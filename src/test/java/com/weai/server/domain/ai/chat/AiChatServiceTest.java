package com.weai.server.domain.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weai.server.domain.ai.backend.AiBackendResolver;
import com.weai.server.domain.ai.backend.DynamicAiChatModelFactory;
import com.weai.server.domain.ai.backend.ResolvedAiBackend;
import com.weai.server.domain.ai.rag.ProjectFactsService;
import com.weai.server.domain.ai.rag.ProjectRagRetriever;
import com.weai.server.domain.ai.rag.ThinkingLevel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiChatServiceTest {

	private static final String FACTS = """
		<project_facts source="SynAIpse database">
		[Members] (ACTIVE only, 1 total)
		- 김민혁 | role=MEMBER(팀원) | department=BACKEND(백엔드)
		</project_facts>""";

	private final List<ChatMessage> captured = new ArrayList<>();
	private ProjectRagRetriever retriever;
	private AiChatService service;

	@BeforeEach
	void setUp() {
		retriever = mock(ProjectRagRetriever.class);
		ProjectFactsService factsService = mock(ProjectFactsService.class);
		when(factsService.buildFactsBlock(23L)).thenReturn(FACTS);
		AiBackendResolver resolver = mock(AiBackendResolver.class);
		when(resolver.resolve(any(), eq(23L))).thenReturn(ResolvedAiBackend.DEFAULT);

		ChatModel capturingModel = new ChatModel() {
			@Override
			public dev.langchain4j.model.chat.response.ChatResponse doChat(ChatRequest request) {
				captured.clear();
				captured.addAll(request.messages());
				return dev.langchain4j.model.chat.response.ChatResponse.builder()
					.aiMessage(AiMessage.from("김민혁님이 백엔드 담당입니다."))
					.build();
			}
		};

		service = new AiChatService(
			capturingModel,
			retriever,
			factsService,
			resolver,
			mock(DynamicAiChatModelFactory.class),
			"llama3.1"
		);
	}

	@Test
	void factsBlockIsInThePromptWhenNoDocumentsMatch() {
		when(retriever.retrieve(eq(23L), anyString(), any(ThinkingLevel.class))).thenReturn(List.of());

		service.chat(1L, 23L, "백엔드 담당자 누구야?");

		String userPrompt = userPrompt();
		assertThat(userPrompt).contains(FACTS);
		assertThat(userPrompt).contains("(none found for this question)");
		assertThat(systemPrompt()).contains("<project_facts source=\"SynAIpse database\">");
	}

	@Test
	void factsBlockIsInThePromptAlongsideDocumentContext() {
		when(retriever.retrieve(eq(23L), anyString(), any(ThinkingLevel.class)))
			.thenReturn(List.of("| 김민혁 | 백엔드/서버 | 로그인·프로젝트 생성 API, 배포 |"));

		ChatResponse response = service.chat(1L, 23L, "백엔드 담당자 누구야?");

		String userPrompt = userPrompt();
		assertThat(userPrompt).contains(FACTS);
		assertThat(userPrompt).contains("<project_document_context>");
		// 팩트 블록이 문서 컨텍스트보다 먼저 온다.
		assertThat(userPrompt.indexOf("<project_facts")).isLessThan(userPrompt.indexOf("<project_document_context>"));
		assertThat(response.answer()).contains("김민혁");
	}

	private String userPrompt() {
		return captured.stream()
			.filter(UserMessage.class::isInstance)
			.map(message -> ((UserMessage) message).singleText())
			.findFirst()
			.orElseThrow();
	}

	private String systemPrompt() {
		return captured.stream()
			.filter(SystemMessage.class::isInstance)
			.map(message -> ((SystemMessage) message).text())
			.findFirst()
			.orElseThrow();
	}
}
