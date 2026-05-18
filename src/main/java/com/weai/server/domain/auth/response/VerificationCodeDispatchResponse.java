package com.weai.server.domain.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weai.server.domain.auth.domain.VerificationCodePurpose;
import com.weai.server.domain.auth.domain.VerificationDeliveryChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Verification code dispatch result")
public record VerificationCodeDispatchResponse(
	@Schema(description = "Verification purpose", example = "EMAIL_LOGIN")
	VerificationCodePurpose purpose,

	@Schema(description = "Delivery channel", example = "EMAIL")
	VerificationDeliveryChannel deliveryChannel,

	@Schema(description = "Delivery target that accepted the request", example = "royalkim@example.com")
	String deliveryTarget,

	@Schema(description = "Delivery mode. SIMULATED is returned when mock delivery is enabled.", example = "SENT")
	String deliveryMode,

	@Schema(description = "Verification code expiration timestamp", example = "2026-05-18T15:05:00")
	LocalDateTime expiresAt,

	@Schema(
		description = "Debug-only verification code. Returned only when expose-code-in-response is enabled.",
		example = "123456",
		nullable = true
	)
	String debugCode
) {
}
