package com.weai.server.domain.health.service;

import com.weai.server.domain.health.response.HealthCheckResponse.ComponentStatus;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Runs the real dependency checks behind {@code /api/v1/health}. Unlike
 * {@code AiInfrastructureHealthService} (which checks Ollama/Chroma for the AI subsystem), this
 * covers the core web-tier dependency: the relational database.
 */
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
			return ComponentStatus.down(exception.getClass().getSimpleName() + ": " + exception.getMessage());
		}
	}
}
