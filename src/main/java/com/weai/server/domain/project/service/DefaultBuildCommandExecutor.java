package com.weai.server.domain.project.service;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class DefaultBuildCommandExecutor implements BuildCommandExecutor {

	private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(10);
	private static final int MAX_OUTPUT_LENGTH = 20_000;

	@Override
	public BuildCommandResult execute(BuildCommand command) {
		Process process;
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command.arguments())
				.directory(command.workingDirectory());
			processBuilder.environment().putAll(command.environmentVariables());
			process = processBuilder.start();
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.BUILD_COMMAND_EXECUTION_FAILED, "Failed to start the build command.");
		}

		CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readOutput(process.getInputStream()));
		CompletableFuture<String> errorFuture = CompletableFuture.supplyAsync(() -> readOutput(process.getErrorStream()));

		try {
			boolean finished = process.waitFor(BUILD_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
			if (!finished) {
				process.destroyForcibly();
				outputFuture.cancel(true);
				errorFuture.cancel(true);
				return new BuildCommandResult(124, "", "Build command timed out after " + BUILD_TIMEOUT.toMinutes() + " minutes.");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			outputFuture.cancel(true);
			errorFuture.cancel(true);
			return new BuildCommandResult(130, "", "Build command was interrupted.");
		}

		String output = truncate(joinOutput(outputFuture));
		String errorOutput = truncate(joinOutput(errorFuture));
		return new BuildCommandResult(process.exitValue(), output, errorOutput);
	}

	private String readOutput(InputStream inputStream) {
		try {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	private String joinOutput(CompletableFuture<String> future) {
		try {
			return future.join();
		} catch (CompletionException exception) {
			if (exception.getCause() instanceof UncheckedIOException) {
				throw new ApiException(ErrorCode.BUILD_COMMAND_EXECUTION_FAILED, "Failed to read build command output.");
			}
			throw exception;
		}
	}

	private String truncate(String value) {
		if (value == null || value.length() <= MAX_OUTPUT_LENGTH) {
			return value == null ? "" : value;
		}
		return value.substring(value.length() - MAX_OUTPUT_LENGTH);
	}
}
