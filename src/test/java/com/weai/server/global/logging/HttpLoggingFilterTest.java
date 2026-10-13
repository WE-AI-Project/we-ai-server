package com.weai.server.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpLoggingFilterTest {

	private final HttpLoggingFilter filter = new HttpLoggingFilter();

	@Test
	void excludesServerLogSseStreamFromResponseCaching() {
		MockHttpServletRequest request = new MockHttpServletRequest(
			"GET", "/api/v1/projects/3/server-logs/stream"
		);

		assertThat(filter.shouldNotFilter(request)).isTrue();
	}

	@Test
	void keepsOrdinaryServerLogRequestsInHttpLogging() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects/3/server-logs");

		assertThat(filter.shouldNotFilter(request)).isFalse();
	}

	@Test
	void addsProjectIdFromProjectApiPathToMdcAndCleansItAfterRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects/37/server-logs");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (servletRequest, servletResponse) ->
			assertThat(MDC.get(ProjectLogContext.PROJECT_ID_KEY)).isEqualTo("37"));

		assertThat(MDC.get(ProjectLogContext.PROJECT_ID_KEY)).isNull();
	}

	@Test
	void masksTokenQueryParametersInLoggedUris() {
		assertThat(HttpLoggingFilter.maskQueryString("token=eyJhbGciOi.abc.def&level=INFO"))
			.isEqualTo("token=***&level=INFO");
		assertThat(HttpLoggingFilter.maskQueryString("level=INFO&accessToken=eyJ.x.y"))
			.isEqualTo("level=INFO&accessToken=***");
		assertThat(HttpLoggingFilter.maskQueryString("service=git-upload-pack"))
			.isEqualTo("service=git-upload-pack");
		assertThat(HttpLoggingFilter.maskQueryString("mytoken=keep"))
			.isEqualTo("mytoken=keep");
	}

	@Test
	void doesNotBufferSyncHubGitRequestBodies() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/synchub/3.git/git-receive-pack");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (servletRequest, servletResponse) ->
			assertThat(servletRequest).isSameAs(request));
	}
}
