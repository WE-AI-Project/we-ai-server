package com.weai.server.domain.project.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weai.server.domain.project.request.ExecuteBuildTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the {@code @PreAuthorize("hasRole('ADMIN')")} guard added to the system build
 * endpoints actually blocks non-admin callers - these endpoints run arbitrary Gradle/Maven tasks
 * against the server's own source tree, so the URL-pattern-only restriction in SecurityConfig
 * (`/api/v1/build/**` -> hasRole(ADMIN)) needed a method-level backstop, and that backstop needs
 * its own regression test rather than trusting it silently works.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectBuildControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void nonAdminCannotListSystemBuildTasks() throws Exception {
		mockMvc.perform(get("/api/v1/build/tasks")
				.with(user("regular-user").authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCanListSystemBuildTasks() throws Exception {
		mockMvc.perform(get("/api/v1/build/tasks")
				.with(user("admin-user").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}

	@Test
	void nonAdminCannotExecuteSystemBuildTask() throws Exception {
		mockMvc.perform(post("/api/v1/build/execute")
				.with(user("regular-user").authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ExecuteBuildTaskRequest("build"))))
			.andExpect(status().isForbidden());
	}
}
