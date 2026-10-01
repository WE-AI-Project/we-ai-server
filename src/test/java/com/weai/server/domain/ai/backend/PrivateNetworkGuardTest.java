package com.weai.server.domain.ai.backend;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PrivateNetworkGuardTest {

	private final PrivateNetworkGuard guard = new PrivateNetworkGuard();

	@ParameterizedTest
	@ValueSource(strings = {
		"http://127.0.0.1:8080/api/tags",
		"http://localhost:11434/api/tags",
		"http://10.0.0.5:8000",
		"http://172.17.0.1:3306",
		"http://192.168.1.1",
		"http://169.254.169.254/latest/meta-data/",
		// this project's own Tailscale mesh (100.64.0.0/10, RFC 6598 CGNAT) - not covered by
		// InetAddress#isSiteLocalAddress, so it needs its own explicit check.
		"http://100.127.105.105:22",
		"http://[::1]:8080",
		"http://0.0.0.0:80"
	})
	void rejectsPrivateAndInternalAddresses(String url) {
		assertThatThrownBy(() -> guard.assertPubliclyRoutable(url))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsUnresolvableHost() {
		assertThatThrownBy(() -> guard.assertPubliclyRoutable("http://this-host-does-not-exist.invalid/"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsMalformedUrl() {
		assertThatThrownBy(() -> guard.assertPubliclyRoutable("not a url"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void allowsAPubliclyRoutableAddress() {
		// 1.1.1.1 (Cloudflare DNS) is a stable, well-known public address - used only to prove the
		// guard doesn't reject everything, no network call is actually made here.
		assertThatCode(() -> guard.assertPubliclyRoutable("https://1.1.1.1/"))
			.doesNotThrowAnyException();
	}
}
