package com.weai.server.domain.ai.rag;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.Locale;

/** Where an indexed RAG document came from. */
public enum RagDocumentOrigin {
	MANUAL,
	VSCODE,
	WORKSPACE,
	CHAT_DOCUMENT,
	MEETING_MINUTE;

	/** Only these may be claimed by an API caller; the rest are set by server-side auto-indexing. */
	public static RagDocumentOrigin fromClient(String raw) {
		if (raw == null || raw.isBlank()) {
			return MANUAL;
		}
		RagDocumentOrigin origin;
		try {
			origin = RagDocumentOrigin.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "origin must be MANUAL or VSCODE.");
		}
		if (origin != MANUAL && origin != VSCODE) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "origin must be MANUAL or VSCODE.");
		}
		return origin;
	}
}
