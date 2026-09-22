package com.weai.server.domain.health.service;

import com.weai.server.domain.health.response.HealthCheckResponse.ComponentStatus;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Runs the real dependency checks behind {@code /api/v1/health}. Unlike
 * {@code AiInfrastructureHealthService} (which checks Ollama/Chroma for the AI subsystem), this
 * covers the core web-tier dependency: the relational database.
 *
 * {@code /api/v1/health/**} is permitAll() (SecurityConfig), so the response body must stay
 * generic - the real exception (which can include connection strings, hostnames, or auth failure
 * detail) is logged server-side instead of being returned to an anonymous caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

	private static final int VALIDATION_TIMEOUT_SECONDS = 3;

	private final DataSource dataSource;

	public ComponentStatus checkDatabase() {
		try (Connection connection = dataSource.getConnection()) {
			if (connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
				return ComponentStatus.up("Database connection is valid.");
			}
			return ComponentStatus.down("Database connection validation failed.");
		} catch (SQLException exception) {
			log.error("Database health check failed", exception);
			return ComponentStatus.down("Database connection failed.");
		}
	}
}
