package com.weai.server.domain.ai.backend;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import org.springframework.stereotype.Component;

/**
 * Blocks the custom-AI-backend connection test from being used as an SSRF probe. The caller only
 * needs to be logged in (see {@link AiBackendSettingController#testConnection}), so without this
 * guard any authenticated user could point {@code baseUrl} at a Docker-internal service
 * (mysql/chroma/minio), localhost, or another host on this server's Tailscale mesh, and use the
 * success/HTTP-status/timeout three-way response as a port scanner/fingerprinting oracle.
 */
@Component
public class PrivateNetworkGuard {

	/** @throws IllegalArgumentException if the URL is malformed or resolves to a private/internal address. */
	public void assertPubliclyRoutable(String url) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException("Malformed URL.");
		}

		String host = uri.getHost();
		if (host == null || host.isBlank()) {
			throw new IllegalArgumentException("URL has no host.");
		}

		InetAddress[] addresses;
		try {
			addresses = InetAddress.getAllByName(host);
		} catch (UnknownHostException exception) {
			throw new IllegalArgumentException("Could not resolve host.");
		}

		if (addresses.length == 0) {
			throw new IllegalArgumentException("Could not resolve host.");
		}

		for (InetAddress address : addresses) {
			if (isPrivateOrInternal(address)) {
				throw new IllegalArgumentException("Refusing to connect to a private/internal address.");
			}
		}
	}

	private boolean isPrivateOrInternal(InetAddress address) {
		if (address.isLoopbackAddress()
			|| address.isLinkLocalAddress()
			|| address.isSiteLocalAddress()
			|| address.isAnyLocalAddress()
			|| address.isMulticastAddress()) {
			return true;
		}
		if (address instanceof Inet4Address) {
			return isCarrierGradeNat(address.getAddress());
		}
		if (address instanceof Inet6Address) {
			return isUniqueLocalAddress(address.getAddress());
		}
		return false;
	}

	// 100.64.0.0/10 (RFC 6598 carrier-grade NAT) is not covered by InetAddress#isSiteLocalAddress,
	// but it is exactly the range this project's own Tailscale mesh uses (e.g. the Pi at
	// 100.127.105.105) - left open, this guard would let anyone probe other Tailscale peers.
	private boolean isCarrierGradeNat(byte[] bytes) {
		int first = bytes[0] & 0xFF;
		int second = bytes[1] & 0xFF;
		return first == 100 && second >= 64 && second <= 127;
	}

	// fc00::/7 - IPv6 unique local addresses, the RFC 1918 equivalent for IPv6.
	private boolean isUniqueLocalAddress(byte[] bytes) {
		return (bytes[0] & 0xFE) == 0xFC;
	}
}
