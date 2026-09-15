package com.weai.server.global.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.minio.enabled", havingValue = "true", matchIfMissing = true)
public class MinioObjectStorageService implements ObjectStorageService {

	private final MinioClient minioClient;

	@Override
	public void put(String bucket, String objectKey, InputStream content, long size, String contentType) {
		try {
			minioClient.putObject(PutObjectArgs.builder()
				.bucket(bucket)
				.object(objectKey)
				.stream(content, size, -1)
				.contentType(contentType)
				.build());
		} catch (Exception exception) {
			throw new ObjectStorageException("Failed to upload object " + objectKey + " to bucket " + bucket, exception);
		}
	}

	@Override
	public StoredObject get(String bucket, String objectKey) {
		try {
			StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
				.bucket(bucket)
				.object(objectKey)
				.build());
			InputStream content = minioClient.getObject(GetObjectArgs.builder()
				.bucket(bucket)
				.object(objectKey)
				.build());
			return new StoredObject(content, stat.size());
		} catch (ErrorResponseException exception) {
			if ("NoSuchKey".equals(exception.errorResponse().code())) {
				throw new ObjectNotFoundException(bucket, objectKey);
			}
			throw new ObjectStorageException("Failed to read object " + objectKey + " from bucket " + bucket, exception);
		} catch (Exception exception) {
			throw new ObjectStorageException("Failed to read object " + objectKey + " from bucket " + bucket, exception);
		}
	}
}
