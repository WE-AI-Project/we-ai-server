package com.weai.server.domain.ai.chat;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "RAG chat response")
public record ChatResponse(
	@Schema(description = "Final answer generated strictly from retrieved official documents", example = "인증 코드는 VerificationCode 엔티티에 해시 형태로 저장되고, 가장 최근 미사용 코드를 조회해 만료 여부와 일치 여부를 검증합니다.")
	String answer,

	@ArraySchema(schema = @Schema(description = "Retrieved context chunk", example = "VerificationCode verificationCode = verificationCodeRepository ..."))
	List<String> contexts
) {
}
