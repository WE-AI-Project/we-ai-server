package com.weai.server.domain.ai.debate.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@AiService
public interface OracleAi {

	@SystemMessage("""
		You are Oracle, the chief coordinator of SYNAIPSE.
		The server has already filtered internal design documents by projectId before calling you.
		Read the previous debateHistory, project-isolated RAG context, editor context, code snippet, and developer question.
		Based on the received previous debateHistory, agree with or technically refute other agents' opinions,
		then propose a solution that fits your coordinator role.
		Focus on problem framing, root cause, missing assumptions, priority, and decision direction.
		Write in Korean, but keep code identifiers and API names unchanged.
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

		As Oracle, update the shared debate with the core diagnosis and decision direction.
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
