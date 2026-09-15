package com.weai.server.global.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Test-only stand-in for {@link MinioObjectStorageService} so the Spring test suite doesn't need a live MinIO instance. */
@Service
@ConditionalOnProperty(name = "storage.minio.enabled", havingValue = "false")
public class InMemoryObjectStorageService implements ObjectStorageService {

	private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

	@Override
	public void put(String bucket, String objectKey, InputStream content, long size, String contentType) {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			content.transferTo(buffer);
			objects.put(key(bucket, objectKey), buffer.toByteArray());
		} catch (IOException exception) {
			throw new ObjectStorageException("Failed to buffer object " + objectKey + " for bucket " + bucket, exception);
		}
	}

	@Override
	public StoredObject get(String bucket, String objectKey) {
		byte[] data = objects.get(key(bucket, objectKey));
		if (data == null) {
			throw new ObjectNotFoundException(bucket, objectKey);
		}
		return new StoredObject(new ByteArrayInputStream(data), data.length);
	}

	private String key(String bucket, String objectKey) {
		return bucket + "/" + objectKey;
	}
}
