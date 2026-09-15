package com.weai.server.global.web;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
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
 * project-membership-checked controller method instead, and shares this response shape. The bytes
 * themselves come from MinIO via {@link com.weai.server.global.storage.ObjectStorageService}.
 */
public final class FileDownloadSupport {

	private FileDownloadSupport() {
	}

	public static ResponseEntity<Resource> asAttachment(DownloadableFile file) {
		MediaType mediaType = StringUtils.hasText(file.contentType())
			? MediaType.parseMediaType(file.contentType())
			: MediaType.APPLICATION_OCTET_STREAM;
		String fileName = StringUtils.hasText(file.originalFileName()) ? file.originalFileName() : "file";
		ContentDisposition contentDisposition = ContentDisposition.attachment()
			.filename(fileName, StandardCharsets.UTF_8)
			.build();
		Resource resource = new InputStreamResource(file.content()) {
			@Override
			public long contentLength() {
				return file.size();
			}
		};

		return ResponseEntity.ok()
			.contentType(mediaType)
			.contentLength(file.size())
			.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
			.body(resource);
	}

	/** What a domain service resolves before handing off to {@link #asAttachment(DownloadableFile)}. */
	public record DownloadableFile(InputStream content, long size, String originalFileName, String contentType) {
	}
}
