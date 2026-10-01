package com.weai.server.domain.ai.rag;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides which files may enter the shared RAG index and scrubs obvious secrets from config files.
 * Indexed text is fed into LLM prompts and quoted back to every project member, so secret files
 * must never be indexed and credential values in config files are masked.
 */
public final class RagSourcePolicy {

	public static final long MAX_FILE_BYTES = 256 * 1024;

	private static final Set<String> SKIPPED_DIRECTORIES = Set.of(
		".git", ".svn", ".hg", "node_modules", "build", "dist", "out", "target", ".gradle", ".idea",
		".vscode", ".venv", "venv", "__pycache__", ".next", ".nuxt", "coverage", ".turbo", ".cache",
		"vendor", "bin", "obj"
	);

	private static final Set<String> INDEXABLE_EXTENSIONS = Set.of(
		"java", "kt", "kts", "groovy", "gradle", "scala", "ts", "tsx", "js", "jsx", "mjs", "cjs", "vue",
		"svelte", "py", "go", "rs", "rb", "php", "cs", "cpp", "cc", "c", "h", "hpp", "swift", "dart",
		"sql", "md", "mdx", "txt", "rst", "adoc", "yml", "yaml", "json", "xml", "toml", "properties",
		"html", "css", "scss", "sh", "dockerfile"
	);

	private static final Set<String> CONFIG_EXTENSIONS = Set.of(
		"yml", "yaml", "json", "xml", "toml", "properties", "ini", "cfg", "conf"
	);

	private static final Set<String> LOCKFILES = Set.of(
		"package-lock.json", "yarn.lock", "pnpm-lock.yaml", "gradle.lockfile", "cargo.lock", "poetry.lock",
		"composer.lock", "gemfile.lock"
	);

	private static final Pattern SECRET_FILE_NAME = Pattern.compile(
		"(^\\.env(\\..*)?$)|(\\.(pem|key|p12|pfx|jks|keystore|crt|cer|der|asc|gpg)$)|(^id_(rsa|dsa|ecdsa|ed25519))"
			+ "|(^\\.npmrc$)|(^\\.netrc$)|(^\\.git-credentials$)",
		Pattern.CASE_INSENSITIVE
	);

	// e.g. secrets.yml, client_secret.json, credentials.txt - but not JwtSecretProvider.java.
	private static final Pattern SECRET_WORD = Pattern.compile("secret|credential", Pattern.CASE_INSENSITIVE);

	private static final Set<String> SECRET_WORD_EXTENSIONS = Set.of(
		"yml", "yaml", "json", "xml", "toml", "properties", "ini", "cfg", "conf", "txt", "env"
	);

	private static final Pattern CONFIG_SECRET_VALUE = Pattern.compile(
		"(?im)^(\\s*[\"']?[\\w.\\-]*(password|passwd|pwd|secret|token|api[_-]?key|private[_-]?key|access[_-]?key|credential)"
			+ "[\\w.\\-]*[\"']?\\s*[:=]\\s*)(.+)$"
	);

	private RagSourcePolicy() {
	}

	public static boolean isSkippedDirectory(String directoryName) {
		return SKIPPED_DIRECTORIES.contains(directoryName.toLowerCase(Locale.ROOT));
	}

	public static boolean isSecretFile(String path) {
		String name = fileName(path);
		if (SECRET_FILE_NAME.matcher(name).find()) {
			return true;
		}
		String ext = name.contains(".") ? extension(name.toLowerCase(Locale.ROOT)) : "";
		return (ext.isEmpty() || SECRET_WORD_EXTENSIONS.contains(ext)) && SECRET_WORD.matcher(name).find();
	}

	public static boolean isIndexableWorkspaceFile(String path) {
		String name = fileName(path).toLowerCase(Locale.ROOT);
		if (isSecretFile(name) || LOCKFILES.contains(name) || name.endsWith(".min.js") || name.endsWith(".min.css")) {
			return false;
		}
		if (name.equals("dockerfile") || name.equals("readme")) {
			return true;
		}
		return INDEXABLE_EXTENSIONS.contains(extension(name));
	}

	/** Masks credential-looking values in config files; source code is left untouched. */
	public static String redactSecrets(String path, String text) {
		if (!CONFIG_EXTENSIONS.contains(extension(fileName(path).toLowerCase(Locale.ROOT)))) {
			return text;
		}
		Matcher matcher = CONFIG_SECRET_VALUE.matcher(text);
		StringBuilder redacted = new StringBuilder();
		while (matcher.find()) {
			String value = matcher.group(3).trim();
			String replacement = isPlaceholder(value) ? matcher.group(0) : matcher.group(1) + "[REDACTED]";
			matcher.appendReplacement(redacted, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(redacted);
		return redacted.toString();
	}

	// Values like ${DB_PASSWORD} or ${JWT_SECRET:} only reference an env var, so they are safe to keep.
	private static boolean isPlaceholder(String value) {
		String unquoted = value.replaceAll("^[\"']|[\"',]+$", "");
		return unquoted.startsWith("${") || unquoted.isEmpty();
	}

	private static String fileName(String path) {
		String normalized = path.replace('\\', '/');
		int slash = normalized.lastIndexOf('/');
		return slash >= 0 ? normalized.substring(slash + 1) : normalized;
	}

	private static String extension(String name) {
		int dot = name.lastIndexOf('.');
		return dot >= 0 ? name.substring(dot + 1) : name;
	}
}
