package com.weai.server.domain.ai.debate.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@AiService
public interface InspectorAi {

	@SystemMessage("""
		You are InspectorAi, SYNAIPSE's strict QA and security inspector.
		Read the previous agents' debateHistory, project-isolated RAG context, and the code.
		Find security vulnerabilities, null or exception risks, validation gaps, race conditions,
		broken API contracts, and realistic bugs.
		Rule: 앞선 에이전트들의 토론 기록을 읽고, 완벽한 결론이 도출되었다고 판단되면
		답변 마지막에 반드시 [토론 종료] 라는 키워드를 포함해라. 아직 부족하다면 추가 논의점을 던져라.
		Write in Korean and be direct.
		""")
	@UserMessage("""
		Round: {{round}}
		Project ID: {{projectId}}
		File: {{fileName}}
		Cursor line: {{cursorLine}}
		Developer question:
		{{userQuery}}

		This code belongs to [Project ID: {{projectId}}].
		Related internal design documents for this project:
		{{ragContext}}

		Current code snippet:
		```text
		{{currentCodeSnippet}}
		```

		Previous debateHistory:
		{{debateHistory}}

		As InspectorAi, inspect the debate. If the conclusion is complete, end with [토론 종료].
		""")
	String debate(
		@V("round") int round,
		@V("projectId") Long projectId,
		@V("fileName") String fileName,
		@V("currentCodeSnippet") String currentCodeSnippet,
		@V("cursorLine") Integer cursorLine,
		@V("userQuery") String userQuery,
		@V("ragContext") String ragContext,
		@V("debateHistory") String debateHistory
	);
}
