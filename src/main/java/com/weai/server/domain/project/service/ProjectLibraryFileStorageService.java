package com.weai.server.domain.project.service;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 프로젝트 공유 자료실(Shared Library) 문서 파일을 uploads/projects/{projectId}/library 아래에 저장한다. */
@Service
public class ProjectLibraryFileStorageService {

	private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
		"pdf", "md", "txt", "yml", "yaml", "java", "ts", "tsx", "js", "jsx",
		"doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "png", "jpg", "jpeg"
	);

	private final Path uploadRoot;

	public ProjectLibraryFileStorageService(@Value("${chat.document.upload-root:uploads/projects}") String uploadRoot) {
		this.uploadRoot = Paths.get(uploadRoot).toAbsolutePath().normalize();
	}

	public StoredLibraryFile store(Long projectId, MultipartFile file) {
		validateFile(file);

		String originalFileName = normalizeOriginalFileName(file.getOriginalFilename());
		String extension = extractExtension(originalFileName);
		String storedFileName = UUID.randomUUID() + (extension == null ? "" : "." + extension);
		Path libraryDirectory = uploadRoot.resolve(projectId.toString()).resolve("library").normalize();
		Path targetPath = libraryDirectory.resolve(storedFileName).normalize();

		if (!targetPath.startsWith(libraryDirectory)) {
			throw new ApiException(ErrorCode.LIBRARY_UPLOAD_FAILED, "Invalid library file path.");
		}

		try {
			Files.createDirectories(libraryDirectory);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.LIBRARY_UPLOAD_FAILED);
		}

		String fileUrl = "/uploads/projects/%d/library/%s".formatted(projectId, storedFileName);
		return new StoredLibraryFile(
			fileUrl,
			originalFileName,
			storedFileName,
			file.getSize(),
			file.getContentType(),
			extension == null ? "" : extension
		);
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
