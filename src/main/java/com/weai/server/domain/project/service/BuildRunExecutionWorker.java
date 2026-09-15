package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.BuildRun;
import com.weai.server.domain.project.repository.BuildRunRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuildRunExecutionWorker {

	private final BuildRunRepository buildRunRepository;
	private final BuildCommandExecutor buildCommandExecutor;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void execute(Long buildRunId, BuildCommand command) {
		BuildRun buildRun = buildRunRepository.findById(buildRunId)
			.orElseThrow(() -> new ApiException(ErrorCode.BUILD_RUN_NOT_FOUND));

		try {
			BuildCommandResult result = buildCommandExecutor.execute(command);
			LocalDateTime finishedAt = LocalDateTime.now();
			if (result.successful()) {
				buildRun.succeed(result.exitCode(), result.output(), normalizeBlank(result.errorOutput()), finishedAt);
			} else {
				buildRun.fail(result.exitCode(), result.output(), normalizeBlank(result.errorOutput()), finishedAt);
			}
		} catch (RuntimeException exception) {
			buildRun.fail(1, "", exception.getMessage(), LocalDateTime.now());
		}
	}

	private String normalizeBlank(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
