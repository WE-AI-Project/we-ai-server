package com.weai.server.domain.ai.debate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "VS Code editor context for multi-agent AI debate")
public record EditorContextDto(
	@NotNull(message = "projectId is required.")
	@Schema(description = "Workspace/project id linked to the current VS Code folder", example = "1")
	Long projectId,

	@NotBlank(message = "fileName is required.")
	@Schema(description = "Current editor file name", example = "src/app/components/ChatPage.tsx")
	String fileName,

	@NotBlank(message = "currentCodeSnippet is required.")
	@Schema(description = "Selected code or nearby code around the cursor")
	String currentCodeSnippet,

	@NotNull(message = "cursorLine is required.")
	@Min(value = 1, message = "cursorLine must be greater than or equal to 1.")
	@Schema(description = "1-based cursor line number", example = "42")
	Integer cursorLine,

	@NotBlank(message = "userQuery is required.")
	@Schema(description = "Developer's question for the AI agent team", example = "Why does this component re-render too often?")
	String userQuery
) {
}
