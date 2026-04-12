package com.weai.server.domain.auth.dto.naver;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data // Getter, Setter, toString 등을 모두 포함합니다.
public class NaverTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private String expiresIn;

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
}