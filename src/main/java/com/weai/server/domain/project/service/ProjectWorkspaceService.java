package com.weai.server.domain.project.service;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.storage.ObjectStorageService;
import com.weai.server.global.storage.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Materializes a project's source files on the central server so features that need real files on
 * disk (build execution, tech-stack detection) work without ever referencing a client's local
 * machine path. The client uploads a zip snapshot of the project; this replaces whatever was
 * previously extracted under {@code {workspaceRoot}/{projectId}} with the new snapshot's contents.
 *
 * This intentionally does not attempt incremental sync - every upload is a full-replace. There is
 * no diffing against the previous snapshot, so a partial/incremental zip would silently leave stale
 * files behind; callers must always upload a complete project snapshot.
 */
@Slf4j
@Service
public class ProjectWorkspaceService {

	private static final long MAX_UPLOAD_SIZE = 200L * 1024 * 1024;
	private static final long MAX_EXTRACTED_SIZE = 1024L * 1024 * 1024;
	private static final int MAX_ENTRY_COUNT = 20_000;
	private static final int COPY_BUFFER_SIZE = 8192;

	private final ObjectStorageService objectStorageService;
	private final StorageProperties storageProperties;
	private final Path workspaceRoot;

	public ProjectWorkspaceService(
		ObjectStorageService objectStorageService,
		StorageProperties storageProperties,
		@Value("${app.workspace.root:${APP_WORKSPACE_ROOT:./workspace}}") String workspaceRoot
	) {
		this.objectStorageService = objectStorageService;
		this.storageProperties = storageProperties;
		this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
	}

	public Path resolveProjectDirectory(Long projectId) {
		return workspaceRoot.resolve(String.valueOf(projectId)).normalize();
	}

	/** Same as {@link #resolveProjectDirectory}, but fails clearly if no snapshot has ever been uploaded. */
	public Path requireProjectDirectory(Long projectId) {
		Path directory = resolveProjectDirectory(projectId);
		if (!Files.isDirectory(directory)) {
			throw new ApiException(ErrorCode.PROJECT_WORKSPACE_NOT_FOUND);
		}
		return directory;
	}

	public ProjectWorkspaceUploadResponse uploadSnapshot(Long projectId, MultipartFile file) {
		validateSnapshotFile(file);
		backupSnapshotToObjectStorage(projectId, file);

		Path targetDirectory = resolveProjectDirectory(projectId);
		int extractedFileCount;
		try {
			replaceDirectory(targetDirectory);
			extractedFileCount = extractZip(file, targetDirectory);
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.WORKSPACE_EXTRACTION_FAILED, exception.getMessage());
		}

		log.info(
			"Uploaded workspace snapshot for projectId={}: {} entries extracted to {}",
			projectId, extractedFileCount, targetDirectory
		);
		return new ProjectWorkspaceUploadResponse(projectId, targetDirectory.toString(), extractedFileCount, LocalDateTime.now());
	}

	private void validateSnapshotFile(MultipartFile file) {
		if (file == null) {
			throw new ApiException(ErrorCode.WORKSPACE_FILE_REQUIRED);
		}
		if (file.isEmpty()) {
			throw new ApiException(ErrorCode.WORKSPACE_FILE_EMPTY);
		}
		if (file.getSize() > MAX_UPLOAD_SIZE) {
			throw new ApiException(ErrorCode.WORKSPACE_FILE_SIZE_EXCEEDED);
		}

		String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
		String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
		boolean zipContentType = contentType.equals("application/zip") || contentType.equals("application/x-zip-compressed");
		if (!fileName.endsWith(".zip") && !zipContentType) {
			throw new ApiException(ErrorCode.WORKSPACE_FILE_TYPE_NOT_ALLOWED);
		}
	}

	// 백업 저장 실패는 추출 자체를 막지 않는다 - 빌드 실행/스택감지에 실제로 필요한 건 디스크에
	// 펼쳐진 파일들이지 MinIO 백업본이 아니다. 백업은 감사/복구용 부가 기능이다.
	// RuntimeException도 함께 잡아야 한다 - ObjectStorageService.put()은 실패 시 IOException이
	// 아니라 ObjectStorageException(RuntimeException)을 던지므로, IOException만 잡으면 MinIO가
	// 잠깐 불안정하기만 해도 핵심 기능인 워크스페이스 추출 자체가 막혀버린다.
	private void backupSnapshotToObjectStorage(Long projectId, MultipartFile file) {
		String objectKey = "workspaces/%d/snapshot.zip".formatted(projectId);
		try (InputStream inputStream = file.getInputStream()) {
			objectStorageService.put(storageProperties.getPrivateBucket(), objectKey, inputStream, file.getSize(), file.getContentType());
		} catch (IOException | RuntimeException exception) {
			log.warn("Failed to back up workspace snapshot to object storage for projectId={}", projectId, exception);
		}
	}

	private void replaceDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			deleteRecursively(directory);
		}
		Files.createDirectories(directory);
	}

	private void deleteRecursively(Path root) throws IOException {
		try (Stream<Path> paths = Files.walk(root)) {
			try {
				paths.sorted(Comparator.reverseOrder()).forEach(path -> {
					try {
						Files.delete(path);
					} catch (IOException exception) {
						throw new UncheckedIOException(exception);
					}
				});
			} catch (UncheckedIOException exception) {
				throw exception.getCause();
			}
		}
	}

	private int extractZip(MultipartFile file, Path targetDirectory) throws IOException {
		int extractedEntryCount = 0;
		long totalExtractedBytes = 0L;

		try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				extractedEntryCount++;
				if (extractedEntryCount > MAX_ENTRY_COUNT) {
					throw new ApiException(ErrorCode.WORKSPACE_SNAPSHOT_TOO_LARGE, "Too many entries in the workspace snapshot.");
				}

				// PowerShell's Compress-Archive (and other Windows zip tools) write entry names with
				// backslash separators, which the ZIP spec never allows - ZipEntry.isDirectory() only
				// checks for a trailing '/', so a "src\main\" entry is misread as a file. That silently
				// creates an empty stub file where a directory belongs, which then collides with the
				// real subdirectory on the very next entry. Normalizing to '/' first fixes detection
				// for both directory entries and nested file paths regardless of which OS zipped them.
				String normalizedEntryName = entry.getName().replace('\\', '/');
				Path resolvedPath = resolveZipEntryPath(targetDirectory, normalizedEntryName);
				if (entry.isDirectory() || normalizedEntryName.endsWith("/")) {
					Files.createDirectories(resolvedPath);
					continue;
				}

				Files.createDirectories(resolvedPath.getParent());
				totalExtractedBytes += copyWithLimit(zipInputStream, resolvedPath, MAX_EXTRACTED_SIZE - totalExtractedBytes);
			}
		}

		return extractedEntryCount;
	}

	/**
	 * "Zip slip" guard: a malicious entry name like {@code ../../etc/passwd} or an absolute path
	 * must never be allowed to resolve to a location outside the project's own workspace directory.
	 */
	private Path resolveZipEntryPath(Path targetDirectory, String entryName) {
		Path resolved = targetDirectory.resolve(entryName).normalize();
		if (!resolved.startsWith(targetDirectory)) {
			throw new ApiException(ErrorCode.WORKSPACE_EXTRACTION_FAILED, "Unsafe zip entry path: " + entryName);
		}
		return resolved;
	}

	// zip bomb guard: aborts the moment the cumulative uncompressed size across all entries would
	// exceed the budget, rather than trusting the (attacker-controlled, sometimes absent) uncompressed
	// size field on the zip entry itself.
	private long copyWithLimit(InputStream inputStream, Path target, long remainingBudget) throws IOException {
		if (remainingBudget <= 0) {
			throw new ApiException(ErrorCode.WORKSPACE_SNAPSHOT_TOO_LARGE, "Workspace snapshot exceeds the extracted size limit.");
		}

		try (OutputStream outputStream = Files.newOutputStream(target)) {
			byte[] buffer = new byte[COPY_BUFFER_SIZE];
			long written = 0L;
			int read;
			while ((read = inputStream.read(buffer)) != -1) {
				written += read;
				if (written > remainingBudget) {
					throw new ApiException(ErrorCode.WORKSPACE_SNAPSHOT_TOO_LARGE, "Workspace snapshot exceeds the extracted size limit.");
				}
				outputStream.write(buffer, 0, read);
			}
			return written;
		}
	}

	public record ProjectWorkspaceUploadResponse(
		Long projectId,
		String workspacePath,
		int extractedFileCount,
		LocalDateTime extractedAt
	) {
	}
}
