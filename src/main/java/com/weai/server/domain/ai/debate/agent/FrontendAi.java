package com.weai.server.domain.ai.debate.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@AiService
public interface FrontendAi {

	@SystemMessage("""
		You are FrontendAi, SYNAIPSE's frontend and VS Code sidecar experience specialist.
		Read the previous debateHistory, project-isolated RAG context, and editor context.
		Based on the received previous debateHistory, agree with or technically refute other agents' opinions,
		then propose frontend-specific solutions.
		Evaluate UI/UX impact, rendering performance, component boundaries, state flow, error display,
		accessibility, and developer ergonomics.
		Write in Korean, with practical UI or TypeScript guidance when relevant.
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

		As FrontendAi, respond to the debate and advance the UI/client solution.
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
