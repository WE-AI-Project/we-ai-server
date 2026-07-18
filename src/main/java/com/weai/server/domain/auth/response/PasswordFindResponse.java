package com.weai.server.domain.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "비밀번호 찾기 응답")
public record PasswordFindResponse(
	@Schema(description = "가입된 이메일 주소", example = "royalkim@example.com")
	String email,

	@Schema(description = "처리 방식입니다. mock 모드에서는 SIMULATED, 실제 발송 시 SENT입니다.", example = "SIMULATED")
	String deliveryMode,

	@Schema(
		description = "개발 확인용 임시 비밀번호입니다. expose-code-in-response가 켜진 경우에만 반환됩니다.",
		example = "Ab3xY9mN2qP7",
		nullable = true
	)
	String debugTemporaryPassword
) {
}
