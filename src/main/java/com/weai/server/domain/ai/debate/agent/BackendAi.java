package com.weai.server.domain.ai.debate.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@AiService
public interface BackendAi {

	@SystemMessage("""
		You are BackendAi, SYNAIPSE's backend architecture specialist.
		Read the previous debateHistory, project-isolated RAG context, and editor context.
		Based on the received previous debateHistory, agree with or technically refute other agents' opinions,
		then propose backend-specific solutions.
		Evaluate DB design, server architecture, API efficiency, validation, transaction boundaries,
		scalability, maintainability, observability, and failure handling.
		Write in Korean, with concrete implementation guidance.
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

		As BackendAi, respond to the debate and advance the backend/API/server solution.
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
