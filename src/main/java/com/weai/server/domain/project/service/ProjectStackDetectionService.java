package com.weai.server.domain.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weai.server.domain.project.domain.ProjectTechStackCategory;
import com.weai.server.domain.project.response.ProjectStackDetectResponse;
import com.weai.server.domain.project.response.ProjectStackDetectResponse.DetectedTechStack;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectStackDetectionService {

	private static final int MAX_DEPTH = 4;
	private static final long MAX_FILE_SIZE = 1024 * 1024;
	private static final Set<String> SKIPPED_DIRECTORIES = Set.of(
		".git", ".gradle", ".idea", ".vscode", ".venv", "venv", "node_modules",
		"build", "dist", "out", "target", "coverage", ".next"
	);
	private static final Set<String> INTERESTING_FILES = Set.of(
		"package.json", "pom.xml", "build.gradle", "build.gradle.kts", "gradle-wrapper.properties",
		"requirements.txt", "pyproject.toml", "pipfile", "poetry.lock", "setup.py",
		"dockerfile", "compose.yml", "compose.yaml", "docker-compose.yml", "docker-compose.yaml",
		"tsconfig.json", "vite.config.js", "vite.config.ts", "vite.config.mjs",
		"tailwind.config.js", "tailwind.config.ts", "application.yml", "application.yaml",
		"application.properties", "go.mod", "cargo.toml"
	);

	private final ObjectMapper objectMapper;

	public ProjectStackDetectResponse detect(String rawPath) {
		Path root = resolveRoot(rawPath);
		Map<String, DetectedTechStack> detected = new LinkedHashMap<>();
		List<String> detectedFiles = new ArrayList<>();

		try {
			Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), MAX_DEPTH, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
					if (!directory.equals(root) && isSkippedDirectory(directory)) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) {
					if (attributes.isRegularFile() && isInteresting(path)) {
						detectedFiles.add(normalizeRelative(root, path));
						inspectFile(path, detected);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "Failed to scan localPath: " + exception.getMessage());
		}
		detectedFiles.sort(Comparator.naturalOrder());

		List<DetectedTechStack> techStacks = List.copyOf(detected.values());
		List<String> stack = techStacks.stream()
			.map(item -> item.version() == null || item.version().isBlank() ? item.name() : item.name() + " " + item.version())
			.toList();

		return new ProjectStackDetectResponse(
			root.toString(),
			stack,
			summarize(techStacks, ProjectTechStackCategory.BACKEND, ProjectTechStackCategory.FRONTEND),
			summarize(techStacks, ProjectTechStackCategory.LANGUAGE),
			summarize(techStacks, ProjectTechStackCategory.BUILD_TOOL),
			techStacks,
			List.copyOf(detectedFiles)
		);
	}

	private Path resolveRoot(String rawPath) {
		try {
			Path root = Path.of(rawPath.trim()).toAbsolutePath().normalize();
			if (!Files.exists(root)) {
				throw new ApiException(ErrorCode.INVALID_INPUT, "localPath does not exist: " + root);
			}
			if (!Files.isDirectory(root)) {
				throw new ApiException(ErrorCode.INVALID_INPUT, "localPath must be a directory: " + root);
			}
			if (!Files.isReadable(root)) {
				throw new ApiException(ErrorCode.INVALID_INPUT, "localPath is not readable: " + root);
			}
			return root;
		} catch (ApiException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "localPath is invalid.");
		}
	}

	private boolean isSkippedDirectory(Path path) {
		Path name = path.getFileName();
		return name != null && SKIPPED_DIRECTORIES.contains(name.toString().toLowerCase(Locale.ROOT));
	}

	private boolean isInteresting(Path path) {
		Path name = path.getFileName();
		return name != null && INTERESTING_FILES.contains(name.toString().toLowerCase(Locale.ROOT));
	}

	private void inspectFile(Path path, Map<String, DetectedTechStack> detected) {
		String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
		String content = readSmallText(path);
		if (content == null) {
			return;
		}

		switch (fileName) {
			case "package.json" -> inspectPackageJson(content, detected);
			case "build.gradle", "build.gradle.kts" -> inspectGradle(content, detected);
			case "gradle-wrapper.properties" -> {
				add(detected, "Gradle", capture(content, "gradle-([0-9.]+)-(?:bin|all)\\.zip"), ProjectTechStackCategory.BUILD_TOOL);
			}
			case "pom.xml" -> inspectMaven(content, detected);
			case "requirements.txt", "pyproject.toml", "pipfile", "poetry.lock", "setup.py" -> inspectPython(fileName, content, detected);
			case "dockerfile", "compose.yml", "compose.yaml", "docker-compose.yml", "docker-compose.yaml" -> inspectDocker(content, detected);
			case "tsconfig.json" -> add(detected, "TypeScript", null, ProjectTechStackCategory.LANGUAGE);
			case "go.mod" -> add(detected, "Go", capture(content, "(?m)^go\\s+([0-9.]+)"), ProjectTechStackCategory.LANGUAGE);
			case "cargo.toml" -> {
				add(detected, "Rust", null, ProjectTechStackCategory.LANGUAGE);
				add(detected, "Cargo", null, ProjectTechStackCategory.BUILD_TOOL);
			}
			default -> inspectConfiguration(fileName, content, detected);
		}
	}

	private void inspectPackageJson(String content, Map<String, DetectedTechStack> detected) {
		try {
			JsonNode root = objectMapper.readTree(content);
			Map<String, String> packages = new LinkedHashMap<>();
			collectPackages(root.path("dependencies"), packages);
			collectPackages(root.path("devDependencies"), packages);
			String nodeVersion = cleanVersion(root.path("engines").path("node").asText(null));
			add(detected, "Node.js", nodeVersion, ProjectTechStackCategory.LANGUAGE);
			addPackage(packages, detected, "react", "React", ProjectTechStackCategory.FRONTEND);
			addPackage(packages, detected, "next", "Next.js", ProjectTechStackCategory.FRONTEND);
			addPackage(packages, detected, "vue", "Vue", ProjectTechStackCategory.FRONTEND);
			addPackage(packages, detected, "@angular/core", "Angular", ProjectTechStackCategory.FRONTEND);
			addPackage(packages, detected, "typescript", "TypeScript", ProjectTechStackCategory.LANGUAGE);
			addPackage(packages, detected, "vite", "Vite", ProjectTechStackCategory.BUILD_TOOL);
			addPackage(packages, detected, "tailwindcss", "Tailwind CSS", ProjectTechStackCategory.FRONTEND);
			addPackage(packages, detected, "express", "Express", ProjectTechStackCategory.BACKEND);
			addPackage(packages, detected, "@nestjs/core", "NestJS", ProjectTechStackCategory.BACKEND);
			addPackage(packages, detected, "prisma", "Prisma", ProjectTechStackCategory.DATABASE);
			addPackage(packages, detected, "mongoose", "MongoDB", ProjectTechStackCategory.DATABASE);
		} catch (IOException ignored) {
			add(detected, "Node.js", null, ProjectTechStackCategory.LANGUAGE);
		}
	}

	private void inspectGradle(String content, Map<String, DetectedTechStack> detected) {
		add(detected, "Gradle", null, ProjectTechStackCategory.BUILD_TOOL);
		if (contains(content, "org.springframework.boot", "spring-boot")) {
			add(detected, "Spring Boot", capture(content, "org\\.springframework\\.boot['\"]?\\s+version\\s+['\"]([^'\"]+)"), ProjectTechStackCategory.BACKEND);
		}
		if (contains(content, "java", "JavaLanguageVersion")) {
			String version = capture(content, "JavaLanguageVersion\\.of\\((\\d+)\\)");
			if (version == null) {
				version = capture(content, "sourceCompatibility\\s*=\\s*['\"]?(?:JavaVersion\\.VERSION_)?([0-9_]+)");
			}
			add(detected, "Java", version == null ? null : version.replace('_', '.'), ProjectTechStackCategory.LANGUAGE);
		}
		inspectDatabases(content, detected);
		if (contains(content, "langchain4j")) {
			add(detected, "LangChain4j", capture(content, "langchain4j[^:]*:([0-9.]+)"), ProjectTechStackCategory.AI);
		}
	}

	private void inspectMaven(String content, Map<String, DetectedTechStack> detected) {
		add(detected, "Maven", null, ProjectTechStackCategory.BUILD_TOOL);
		if (contains(content, "spring-boot")) {
			add(detected, "Spring Boot", capture(content, "<spring-boot.version>([^<]+)"), ProjectTechStackCategory.BACKEND);
		}
		String javaVersion = capture(content, "<(?:java.version|maven.compiler.source)>([^<]+)");
		add(detected, "Java", javaVersion, ProjectTechStackCategory.LANGUAGE);
		inspectDatabases(content, detected);
	}

	private void inspectPython(String fileName, String content, Map<String, DetectedTechStack> detected) {
		add(detected, "Python", capture(content, "python(?:_requires|\\s*=)\\s*['\"]?[^0-9]*([0-9.]+)"), ProjectTechStackCategory.LANGUAGE);
		add(detected, fileName.equals("pyproject.toml") && contains(content, "[tool.poetry") ? "Poetry" : "pip", null, ProjectTechStackCategory.BUILD_TOOL);
		addPythonPackage(content, detected, "fastapi", "FastAPI");
		addPythonPackage(content, detected, "django", "Django");
		addPythonPackage(content, detected, "flask", "Flask");
		if (contains(content, "langchain")) {
			add(detected, "LangChain", capture(content, "(?im)langchain[^0-9\\r\\n]*([0-9.]+)"), ProjectTechStackCategory.AI);
		}
		inspectDatabases(content, detected);
	}

	private void inspectDocker(String content, Map<String, DetectedTechStack> detected) {
		add(detected, "Docker", null, ProjectTechStackCategory.DEVOPS);
		inspectDatabases(content, detected);
		if (contains(content, "chromadb/chroma", "chroma:")) {
			add(detected, "ChromaDB", null, ProjectTechStackCategory.DATABASE);
		}
		if (contains(content, "minio/minio")) {
			add(detected, "MinIO", null, ProjectTechStackCategory.DEVOPS);
		}
	}

	private void inspectConfiguration(String fileName, String content, Map<String, DetectedTechStack> detected) {
		if (fileName.startsWith("vite.config")) {
			add(detected, "Vite", null, ProjectTechStackCategory.BUILD_TOOL);
		}
		if (fileName.startsWith("tailwind.config")) {
			add(detected, "Tailwind CSS", null, ProjectTechStackCategory.FRONTEND);
		}
		inspectDatabases(content, detected);
	}

	private void inspectDatabases(String content, Map<String, DetectedTechStack> detected) {
		if (contains(content, "mysql")) add(detected, "MySQL", null, ProjectTechStackCategory.DATABASE);
		if (contains(content, "postgresql", "postgres:")) add(detected, "PostgreSQL", null, ProjectTechStackCategory.DATABASE);
		if (contains(content, "redis")) add(detected, "Redis", null, ProjectTechStackCategory.DATABASE);
		if (contains(content, "mongodb", "mongo:")) add(detected, "MongoDB", null, ProjectTechStackCategory.DATABASE);
	}

	private void collectPackages(JsonNode node, Map<String, String> packages) {
		if (node.isObject()) {
			node.fields().forEachRemaining(entry -> packages.put(entry.getKey(), cleanVersion(entry.getValue().asText())));
		}
	}

	private void addPackage(Map<String, String> packages, Map<String, DetectedTechStack> detected, String packageName, String displayName, ProjectTechStackCategory category) {
		if (packages.containsKey(packageName)) {
			add(detected, displayName, packages.get(packageName), category);
		}
	}

	private void addPythonPackage(String content, Map<String, DetectedTechStack> detected, String packageName, String displayName) {
		if (contains(content, packageName)) {
			add(detected, displayName, capture(content, "(?im)" + Pattern.quote(packageName) + "[^0-9\\r\\n]*([0-9.]+)"), ProjectTechStackCategory.BACKEND);
		}
	}

	private void add(Map<String, DetectedTechStack> detected, String name, String version, ProjectTechStackCategory category) {
		String key = name.toLowerCase(Locale.ROOT);
		DetectedTechStack previous = detected.get(key);
		String normalizedVersion = cleanVersion(version);
		if (previous == null || (previous.version() == null && normalizedVersion != null)) {
			detected.put(key, new DetectedTechStack(name, normalizedVersion, category, true));
		}
	}

	private String summarize(List<DetectedTechStack> stacks, ProjectTechStackCategory... categories) {
		Set<ProjectTechStackCategory> accepted = Set.of(categories);
		String value = stacks.stream().filter(stack -> accepted.contains(stack.category())).map(DetectedTechStack::name).distinct().reduce((left, right) -> left + " / " + right).orElse("Not detected");
		return value;
	}

	private String readSmallText(Path path) {
		try {
			if (Files.size(path) > MAX_FILE_SIZE) return null;
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException ignored) {
			return null;
		}
	}

	private String normalizeRelative(Path root, Path path) {
		return root.relativize(path).toString().replace('\\', '/');
	}

	private boolean contains(String content, String... values) {
		String lower = content.toLowerCase(Locale.ROOT);
		for (String value : values) if (lower.contains(value.toLowerCase(Locale.ROOT))) return true;
		return false;
	}

	private String capture(String content, String regex) {
		Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(content);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	private String cleanVersion(String version) {
		if (version == null) return null;
		String cleaned = version.trim().replaceFirst("^[~^<>=v\\s]+", "");
		Matcher matcher = Pattern.compile("[0-9]+(?:\\.[0-9]+){0,3}").matcher(cleaned);
		return matcher.find() ? matcher.group() : null;
	}
}
