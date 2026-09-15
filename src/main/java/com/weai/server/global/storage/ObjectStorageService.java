package com.weai.server.global.storage;

import java.io.InputStream;

/**
 * Moves file bytes in and out of a named bucket. Object keys are caller-defined (e.g.
 * {@code chat/{projectId}/{chatRoomId}/{storedFileName}}) - implementations only know how to
 * store/retrieve whatever key they're given. Backed by MinIO in real environments
 * ({@link MinioObjectStorageService}); swapped for an in-memory fake in the {@code test} profile
 * ({@link InMemoryObjectStorageService}) the same way {@code AiConfig} swaps Chroma for an
 * in-memory embedding store, so the test suite doesn't need a live MinIO instance.
 */
public interface ObjectStorageService {

	void put(String bucket, String objectKey, InputStream content, long size, String contentType);

	StoredObject get(String bucket, String objectKey);

	record StoredObject(InputStream content, long size) {
	}

	class ObjectNotFoundException extends RuntimeException {
		public ObjectNotFoundException(String bucket, String objectKey) {
			super("Object " + objectKey + " not found in bucket " + bucket);
		}
	}

	class ObjectStorageException extends RuntimeException {
		public ObjectStorageException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
