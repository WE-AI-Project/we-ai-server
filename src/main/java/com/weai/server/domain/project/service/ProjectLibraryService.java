package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.LibraryResourceCategory;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectLibraryResource;
import com.weai.server.domain.project.repository.ProjectLibraryResourceRepository;
import com.weai.server.domain.project.response.LibraryResourceListResponse;
import com.weai.server.domain.project.response.LibraryResourceResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProjectLibraryService {

	private final ProjectLibraryResourceRepository projectLibraryResourceRepository;
	private final ProjectLibraryFileStorageService projectLibraryFileStorageService;
	private final ProjectService projectService;
	private final UserService userService;

	@Transactional
	public LibraryResourceResponse upload(
		String userEmail,
		Long projectId,
		MultipartFile file,
		String title,
		LibraryResourceCategory category,
		String description
	) {
		if (!StringUtils.hasText(title)) {
			throw new ApiException(ErrorCode.LIBRARY_TITLE_REQUIRED);
		}

		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());

		ProjectLibraryFileStorageService.StoredLibraryFile storedFile = projectLibraryFileStorageService.store(projectId, file);

		ProjectLibraryResource resource = projectLibraryResourceRepository.save(ProjectLibraryResource.uploaded(
			project,
			user,
			title.trim(),
			category == null ? LibraryResourceCategory.DOCS : category,
			trimToNull(description),
			storedFile.originalFileName(),
			storedFile.storedFileName(),
			storedFile.fileUrl(),
			storedFile.fileSize(),
			storedFile.fileContentType(),
			storedFile.extension()
		));

		return LibraryResourceResponse.from(resource);
	}

	@Transactional(readOnly = true)
	public LibraryResourceListResponse getResources(
		String userEmail,
		Long projectId,
		Integer page,
		Integer size,
		LibraryResourceCategory category
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());

		PageRequest pageRequest = PageRequest.of(
			page == null ? 0 : Math.max(0, page),
			size == null ? 20 : Math.min(Math.max(1, size), 100),
			Sort.by(Sort.Direction.DESC, "createdAt")
		);

		Page<ProjectLibraryResource> resources = category == null
			? projectLibraryResourceRepository.findByProject_IdAndDeletedAtIsNull(projectId, pageRequest)
			: projectLibraryResourceRepository.findByProject_IdAndCategoryAndDeletedAtIsNull(projectId, category, pageRequest);

		return LibraryResourceListResponse.from(projectId, resources);
	}

	@Transactional
	public LibraryResourceResponse view(String userEmail, Long projectId, Long resourceId) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());

		ProjectLibraryResource resource = getResource(projectId, resourceId);
		resource.increaseViewCount();

		return LibraryResourceResponse.from(resource);
	}

	@Transactional
	public void delete(String userEmail, Long projectId, Long resourceId) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());

		ProjectLibraryResource resource = getResource(projectId, resourceId);
		resource.delete(LocalDateTime.now());
	}

	private ProjectLibraryResource getResource(Long projectId, Long resourceId) {
		return projectLibraryResourceRepository.findByIdAndProject_IdAndDeletedAtIsNull(resourceId, projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.LIBRARY_RESOURCE_NOT_FOUND));
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
