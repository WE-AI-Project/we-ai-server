package com.weai.server.domain.project.service;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.storage.ObjectStorageService;
import com.weai.server.global.storage.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 프로젝트 공유 자료실(Shared Library) 문서 파일을 MinIO 오브젝트 스토리지에 저장한다. */
@Service
@RequiredArgsConstructor
public class ProjectLibraryFileStorageService {

	private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
	private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
		"pdf", "md", "txt", "yml", "yaml", "java", "ts", "tsx", "js", "jsx",
		"doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "png", "jpg", "jpeg"
	);

	private final ObjectStorageService objectStorageService;
	private final StorageProperties storageProperties;

	public StoredLibraryFile store(Long projectId, MultipartFile file) {
		validateFile(file);

		String originalFileName = normalizeOriginalFileName(file.getOriginalFilename());
		String extension = extractExtension(originalFileName);
		String storedFileName = UUID.randomUUID() + (extension == null ? "" : "." + extension);
		String objectKey = objectKey(projectId, storedFileName);

		try (InputStream inputStream = file.getInputStream()) {
			objectStorageService.put(bucketFor(extension), objectKey, inputStream, file.getSize(), file.getContentType());
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.LIBRARY_UPLOAD_FAILED);
		}

		String fileUrl = "/api/v1/projects/%d/library/files/%s".formatted(projectId, storedFileName);
		return new StoredLibraryFile(
			fileUrl,
			originalFileName,
			storedFileName,
			file.getSize(),
			file.getContentType(),
			extension == null ? "" : extension
		);
	}

	/** Resolves a previously stored library file's bytes from object storage. */
	public ObjectStorageService.StoredObject resolveStoredFile(Long projectId, String storedFileName) {
		String extension = extractExtension(storedFileName);
		try {
			return objectStorageService.get(bucketFor(extension), objectKey(projectId, storedFileName));
		} catch (ObjectStorageService.ObjectNotFoundException exception) {
			throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "The requested library file could not be found.");
		}
	}

	private String objectKey(Long projectId, String storedFileName) {
		return "library/%d/%s".formatted(projectId, storedFileName);
	}

	private String bucketFor(String extension) {
		return extension != null && IMAGE_EXTENSIONS.contains(extension)
			? storageProperties.getPublicBucket()
			: storageProperties.getPrivateBucket();
	}

	private void validateFile(MultipartFile file) {
		if (file == null) {
			throw new ApiException(ErrorCode.LIBRARY_FILE_REQUIRED);
		}
		if (file.isEmpty()) {
			throw new ApiException(ErrorCode.LIBRARY_FILE_EMPTY);
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new ApiException(ErrorCode.LIBRARY_FILE_SIZE_EXCEEDED);
		}
		String extension = extractExtension(normalizeOriginalFileName(file.getOriginalFilename()));
		if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
			throw new ApiException(ErrorCode.LIBRARY_FILE_TYPE_NOT_ALLOWED);
		}
	}

	private String normalizeOriginalFileName(String originalFileName) {
		String cleanedFileName = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName);
		return cleanedFileName.isBlank() ? "file" : cleanedFileName;
	}

	private String extractExtension(String fileName) {
		if (fileName == null) {
			return null;
		}
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
			return null;
		}
		return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
	}

	public record StoredLibraryFile(
		String fileUrl,
		String originalFileName,
		String storedFileName,
		Long fileSize,
		String fileContentType,
		String extension
	) {
	}
}
