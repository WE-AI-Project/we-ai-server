package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.weai.server.domain.project.service.ProjectWorkspaceService.ProjectWorkspaceUploadResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.storage.ObjectStorageService;
import com.weai.server.global.storage.StorageProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ProjectWorkspaceServiceTest {

	private static final Long PROJECT_ID = 1L;

	@TempDir
	Path workspaceRoot;

	private ProjectWorkspaceService service;

	@BeforeEach
	void setUp() {
		service = new ProjectWorkspaceService(
			mock(ObjectStorageService.class),
			new StorageProperties(),
			workspaceRoot.toString()
		);
	}

	@Test
	void extractsAZipIntoTheProjectDirectory() throws IOException {
		byte[] zip = buildZip(entry("README.md", "hello"), entry("src/App.java", "class App {}"));
		MockMultipartFile file = new MockMultipartFile("file", "project.zip", "application/zip", zip);

		ProjectWorkspaceUploadResponse response = service.uploadSnapshot(PROJECT_ID, file);

		Path projectDir = service.resolveProjectDirectory(PROJECT_ID);
		assertThat(response.extractedFileCount()).isEqualTo(2);
		assertThat(Files.readString(projectDir.resolve("README.md"))).isEqualTo("hello");
		assertThat(Files.readString(projectDir.resolve("src/App.java"))).isEqualTo("class App {}");
	}

	@Test
	void replacesPreviousContentsOnReupload() throws IOException {
		service.uploadSnapshot(PROJECT_ID, new MockMultipartFile(
			"file", "first.zip", "application/zip", buildZip(entry("old.txt", "stale"))
		));

		service.uploadSnapshot(PROJECT_ID, new MockMultipartFile(
			"file", "second.zip", "application/zip", buildZip(entry("new.txt", "fresh"))
		));

		Path projectDir = service.resolveProjectDirectory(PROJECT_ID);
		assertThat(Files.exists(projectDir.resolve("old.txt"))).isFalse();
		assertThat(Files.readString(projectDir.resolve("new.txt"))).isEqualTo("fresh");
	}

	@Test
	void rejectsZipSlipEntries() throws IOException {
		byte[] zip = buildZip(entry("../../evil.txt", "pwned"));
		MockMultipartFile file = new MockMultipartFile("file", "evil.zip", "application/zip", zip);

		assertThatThrownBy(() -> service.uploadSnapshot(PROJECT_ID, file))
			.isInstanceOf(ApiException.class)
			.extracting(exception -> ((ApiException) exception).getErrorCode())
			.isEqualTo(ErrorCode.WORKSPACE_EXTRACTION_FAILED);
	}

	@Test
	void rejectsNonZipFiles() {
		MockMultipartFile file = new MockMultipartFile("file", "not-a-zip.txt", "text/plain", "hi".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> service.uploadSnapshot(PROJECT_ID, file))
			.isInstanceOf(ApiException.class)
			.extracting(exception -> ((ApiException) exception).getErrorCode())
			.isEqualTo(ErrorCode.WORKSPACE_FILE_TYPE_NOT_ALLOWED);
	}

	@Test
	void throwsWhenNoSnapshotHasBeenUploadedYet() {
		assertThatThrownBy(() -> service.requireProjectDirectory(PROJECT_ID))
			.isInstanceOf(ApiException.class)
			.extracting(exception -> ((ApiException) exception).getErrorCode())
			.isEqualTo(ErrorCode.PROJECT_WORKSPACE_NOT_FOUND);
	}

	private static Entry entry(String name, String content) {
		return new Entry(name, content);
	}

	private static byte[] buildZip(Entry... entries) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(buffer)) {
			for (Entry entry : entries) {
				zipOutputStream.putNextEntry(new ZipEntry(entry.name()));
				zipOutputStream.write(entry.content().getBytes(StandardCharsets.UTF_8));
				zipOutputStream.closeEntry();
			}
		}
		return buffer.toByteArray();
	}

	private record Entry(String name, String content) {
	}
}
