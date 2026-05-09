package com.weai.server.domain.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "소셜 로그인 시작 URL 응답")
public record SocialAuthorizationUrlResponse(
	@Schema(description = "OAuth 제공자 이름", example = "naver")
	String provider,

	@Schema(
		description = "브라우저에서 이 URL을 열면 소셜 로그인 절차를 시작할 수 있습니다.",
		example = "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=client-id&state=random-state&redirect_uri=http://localhost/api/v1/auth/naver/callback"
	)
	String authorizationUrl,

	@Schema(description = "state 값이 필요한 제공자에 대해 생성된 검증용 값", example = "8c0905e8-f8c6-4c8c-b487-9389f1d22ad7")
	String state
) {
}
