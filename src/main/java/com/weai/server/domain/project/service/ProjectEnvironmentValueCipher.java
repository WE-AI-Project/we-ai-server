package com.weai.server.domain.project.service;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProjectEnvironmentValueCipher {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final String VERSION = "v1";
	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH_BITS = 128;

	private final String masterKey;
	private final SecureRandom secureRandom = new SecureRandom();

	public ProjectEnvironmentValueCipher(
		@Value("${synaipse.environment.master-key:${SYNAIPSE_ENV_MASTER_KEY:}}") String masterKey
	) {
		this.masterKey = masterKey;
	}

	public String encrypt(String value) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
			return VERSION + ":" + encode(iv) + ":" + encode(ciphertext);
		} catch (GeneralSecurityException | IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.ENVIRONMENT_VARIABLE_ENCRYPT_FAILED);
		}
	}

	public String decrypt(String encryptedValue) {
		try {
			String[] segments = encryptedValue == null ? new String[0] : encryptedValue.split(":", -1);
			if (segments.length != 3 || !VERSION.equals(segments[0])) {
				throw new IllegalArgumentException("Unsupported encrypted value format.");
			}
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(
				Cipher.DECRYPT_MODE,
				secretKey(),
				new GCMParameterSpec(TAG_LENGTH_BITS, Base64.getDecoder().decode(segments[1]))
			);
			return new String(cipher.doFinal(Base64.getDecoder().decode(segments[2])), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.ENVIRONMENT_VARIABLE_DECRYPT_FAILED);
		}
	}

	private SecretKeySpec secretKey() {
		byte[] key = Base64.getDecoder().decode(masterKey == null ? "" : masterKey.trim());
		if (key.length != 32) {
			throw new IllegalArgumentException("Master key must be a Base64-encoded 32-byte value.");
		}
		return new SecretKeySpec(key, "AES");
	}

	private String encode(byte[] value) {
		return Base64.getEncoder().encodeToString(value);
	}
}
