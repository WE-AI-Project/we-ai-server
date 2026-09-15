package com.weai.server.global.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * Shared response-building logic for the authenticated file-download endpoints (chat attachments,
 * chat documents, shared library resources). These used to be served by an unauthenticated static
 * resource handler under {@code /uploads/**}; each domain now streams its own file through a
 * project-membership-checked controller method instead, and shares this response shape.
 */
public final class FileDownloadSupport {

	private FileDownloadSupport() {
	}

	public static ResponseEntity<Resource> asAttachment(DownloadableFile file) {
		return asAttachment(file.path(), file.originalFileName(), file.contentType());
	}

	public static ResponseEntity<Resource> asAttachment(Path filePath, String originalFileName, String contentType) {
		Resource resource = new FileSystemResource(filePath);
		MediaType mediaType = StringUtils.hasText(contentType)
			? MediaType.parseMediaType(contentType)
			: MediaType.APPLICATION_OCTET_STREAM;
		ContentDisposition contentDisposition = ContentDisposition.attachment()
			.filename(StringUtils.hasText(originalFileName) ? originalFileName : filePath.getFileName().toString(), StandardCharsets.UTF_8)
			.build();

		return ResponseEntity.ok()
			.contentType(mediaType)
			.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
			.body(resource);
	}

	/** What a domain service resolves before handing off to {@link #asAttachment(DownloadableFile)}. */
	public record DownloadableFile(Path path, String originalFileName, String contentType) {
	}
}
