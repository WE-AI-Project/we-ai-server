package com.weai.server.domain.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weai.server.domain.ai.rag.RagTestSupport.CountingEmbeddingModel;
import com.weai.server.domain.ai.rag.WorkspaceRagIndexer.WorkspaceIndexStatus;
import com.weai.server.domain.project.service.ProjectWorkspaceService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceRagIndexerTest {

	private static final Long PROJECT_ID = 3L;

	@TempDir
	Path workspace;

	private ProjectRagIndexService indexService;
	private WorkspaceRagIndexer indexer;

	@BeforeEach
	void setUp() throws IOException {
		ProjectWorkspaceService workspaceService = mock(ProjectWorkspaceService.class);
		when(workspaceService.resolveProjectDirectory(PROJECT_ID)).thenReturn(workspace);
		indexService = new ProjectRagIndexService(
			new InMemoryEmbeddingStore<>(),
			new CountingEmbeddingModel(),
			RagTestSupport.inMemoryRepository(),
			"test-embed"
		);
		indexer = new WorkspaceRagIndexer(workspaceService, indexService, Runnable::run);

		write("src/Main.java", "public class Main {}");
		write("README.md", "# 프로젝트 소개");
		write(".env", "DB_PASSWORD=hunter2");
		write("node_modules/lib/index.js", "module.exports = 1;");
		write("build/generated.java", "class Generated {}");
		write("package-lock.json", "{}");
		Files.write(workspace.resolve("logo.txt"), new byte[] {'a', 0, 'b'});
		write("big.md", "x".repeat((int) RagSourcePolicy.MAX_FILE_BYTES + 1));
	}

	@Test
	void indexesOnlyEligibleTextFiles() {
		WorkspaceIndexStatus status = indexer.run(PROJECT_ID);

		assertThat(status.state()).isEqualTo("COMPLETED");
		assertThat(indexService.list(PROJECT_ID))
			.extracting(RagDocument::getSource)
			.containsExactly("README.md", "src/Main.java");
		assertThat(indexService.list(PROJECT_ID))
			.allMatch(document -> document.getOrigin() == RagDocumentOrigin.WORKSPACE);
	}

	@Test
	void reuploadSkipsUnchangedFilesAndDropsDeletedOnes() throws IOException {
		indexer.run(PROJECT_ID);
		Files.delete(workspace.resolve("README.md"));

		WorkspaceIndexStatus status = indexer.run(PROJECT_ID);

		assertThat(status.unchangedFiles()).isEqualTo(1);
		assertThat(status.removedFiles()).isEqualTo(1);
		assertThat(status.indexedFiles()).isZero();
		assertThat(indexService.list(PROJECT_ID)).extracting(RagDocument::getSource).containsExactly("src/Main.java");
	}

	@Test
	void doesNotRemoveDocumentsFromOtherOrigins() {
		indexService.index(PROJECT_ID, "notes/meeting.md", "회의 내용", RagDocumentOrigin.MEETING_MINUTE);

		indexer.run(PROJECT_ID);

		assertThat(indexService.list(PROJECT_ID)).extracting(RagDocument::getSource).contains("notes/meeting.md");
	}

	private void write(String relativePath, String content) throws IOException {
		Path file = workspace.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}
}
