package com.weai.server.domain.ai.debate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Selectable SYNAIPSE AI agent")
public enum AiAgentType {
	ORACLE("Oracle", "Chief coordinator", "llama3.1"),
	BACKEND("Backend", "Server/API specialist", "qwen2.5-coder"),
	FRONTEND("Frontend", "UI/UX specialist", "llama3.1"),
	INSPECTOR("Inspector", "QA/Security inspector", "qwen2.5-coder");

	private final String displayName;
	private final String role;
	private final String model;

	AiAgentType(String displayName, String role, String model) {
		this.displayName = displayName;
		this.role = role;
		this.model = model;
	}

	public String displayName() {
		return displayName;
	}

	public String role() {
		return role;
	}

	public String model() {
		return model;
	}
}
