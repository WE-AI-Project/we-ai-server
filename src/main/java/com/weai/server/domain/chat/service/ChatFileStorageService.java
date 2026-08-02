package com.weai.server.domain.chat.service;

import com.weai.server.domain.chat.domain.ChatMessageType;
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

@Service
public class ChatFileStorageService {

	private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
	private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");
	private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx");
	private static final Set<String> ARCHIVE_EXTENSIONS = Set.of("zip");
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/png",
		"image/jpeg",
		"image/gif",
		"image/webp",
		"application/pdf",
		"text/plain",
		"application/msword",
		"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
		"application/vnd.ms-excel",
		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
		"application/vnd.ms-powerpoint",
		"application/vnd.openxmlformats-officedocument.presentationml.presentation",
		"application/zip",
		"application/x-zip-compressed"
	);

	private final Path uploadRoot;

	public ChatFileStorageService(@Value("${chat.file.upload-root:uploads/chat}") String uploadRoot) {
		this.uploadRoot = Paths.get(uploadRoot).toAbsolutePath().normalize();
	}

	public StoredChatFile store(Long projectId, Long chatRoomId, MultipartFile file) {
		validateFile(file);

		String originalFileName = normalizeOriginalFileName(file.getOriginalFilename());
		String extension = extractExtension(originalFileName);
		String storedFileName = UUID.randomUUID() + (extension == null ? "" : "." + extension);
		Path roomDirectory = uploadRoot.resolve(projectId.toString()).resolve(chatRoomId.toString()).normalize();
		Path targetPath = roomDirectory.resolve(storedFileName).normalize();

		if (!targetPath.startsWith(roomDirectory)) {
			throw new ApiException(ErrorCode.CHAT_FILE_UPLOAD_FAILED, "Invalid chat file path.");
		}

		try {
			Files.createDirectories(roomDirectory);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.CHAT_FILE_UPLOAD_FAILED);
		}

		String fileUrl = "/uploads/chat/%d/%d/%s".formatted(projectId, chatRoomId, storedFileName);
		return new StoredChatFile(
			fileUrl,
			originalFileName,
			storedFileName,
			file.getSize(),
			file.getContentType(),
			resolveMessageType(extension, file.getContentType())
		);
	}

	private void validateFile(MultipartFile file) {
		if (file == null) {
			throw new ApiException(ErrorCode.CHAT_FILE_REQUIRED);
		}
		if (file.isEmpty()) {
			throw new ApiException(ErrorCode.CHAT_FILE_EMPTY);
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new ApiException(ErrorCode.CHAT_FILE_SIZE_EXCEEDED);
		}

		String originalFileName = normalizeOriginalFileName(file.getOriginalFilename());
		String extension = extractExtension(originalFileName);
		String contentType = normalizeContentType(file.getContentType());
		if (!isAllowedExtension(extension) && !isAllowedContentType(contentType)) {
			throw new ApiException(ErrorCode.CHAT_FILE_TYPE_NOT_ALLOWED);
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

	private boolean isAllowedExtension(String extension) {
		return extension != null
			&& (IMAGE_EXTENSIONS.contains(extension)
			|| DOCUMENT_EXTENSIONS.contains(extension)
			|| ARCHIVE_EXTENSIONS.contains(extension));
	}

	private boolean isAllowedContentType(String contentType) {
		return contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);
	}

	private String normalizeContentType(String contentType) {
		return contentType == null ? null : contentType.toLowerCase(Locale.ROOT);
	}

	private ChatMessageType resolveMessageType(String extension, String contentType) {
		String normalizedContentType = normalizeContentType(contentType);
		if ((extension != null && IMAGE_EXTENSIONS.contains(extension))
			|| (normalizedContentType != null && normalizedContentType.startsWith("image/"))) {
			return ChatMessageType.IMAGE;
		}
		return ChatMessageType.FILE;
	}

	public record StoredChatFile(
		String fileUrl,
		String originalFileName,
		String storedFileName,
		Long fileSize,
		String fileContentType,
		ChatMessageType messageType
	) {
	}
}
