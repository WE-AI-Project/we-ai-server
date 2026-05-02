package com.weai.server.domain.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Social login authorization URL response")
public record SocialAuthorizationUrlResponse(
	@Schema(description = "OAuth provider name", example = "naver")
	String provider,

	@Schema(
		description = "Open this URL in a browser to start the social login flow.",
		example = "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=client-id&state=random-state&redirect_uri=http://localhost/api/v1/auth/naver/callback"
	)
	String authorizationUrl,

	@Schema(description = "Generated state value for providers that require it", example = "8c0905e8-f8c6-4c8c-b487-9389f1d22ad7")
	String state
) {
}
