package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.project.response.LibraryResourceResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.web.FileDownloadSupport;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectLibraryServiceTest {

	@Autowired
	private ProjectLibraryService projectLibraryService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void projectMemberCanDownloadUploadedResource() throws Exception {
		TestFixture fixture = createFixture();
		MockMultipartFile file = new MockMultipartFile("file", "guide.md", "text/markdown", "content".getBytes());
		LibraryResourceResponse uploaded = projectLibraryService.upload(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			file,
			"Guide",
			null,
			null
		);
		String storedFileName = uploaded.fileUrl().substring(uploaded.fileUrl().lastIndexOf('/') + 1);

		FileDownloadSupport.DownloadableFile downloaded = projectLibraryService.download(
			fixture.member().getEmail(),
			fixture.project().getId(),
			storedFileName
		);

		assertThat(downloaded.originalFileName()).isEqualTo("guide.md");
		assertThat(Files.exists(downloaded.path())).isTrue();
	}

	@Test
	void nonProjectMemberCannotDownloadResource() {
		TestFixture fixture = createFixture();
		MockMultipartFile file = new MockMultipartFile("file", "guide.md", "text/markdown", "content".getBytes());
		LibraryResourceResponse uploaded = projectLibraryService.upload(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			file,
			"Guide",
			null,
			null
		);
		String storedFileName = uploaded.fileUrl().substring(uploaded.fileUrl().lastIndexOf('/') + 1);

		assertThatThrownBy(() -> projectLibraryService.download(
			fixture.outsider().getEmail(),
			fixture.project().getId(),
			storedFileName
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
	}

	@Test
	void deletedResourceCannotBeDownloadedEvenByAMember() {
		TestFixture fixture = createFixture();
		MockMultipartFile file = new MockMultipartFile("file", "guide.md", "text/markdown", "content".getBytes());
		LibraryResourceResponse uploaded = projectLibraryService.upload(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			file,
			"Guide",
			null,
			null
		);
		String storedFileName = uploaded.fileUrl().substring(uploaded.fileUrl().lastIndexOf('/') + 1);
		projectLibraryService.delete(fixture.leader().getEmail(), fixture.project().getId(), uploaded.id());

		assertThatThrownBy(() -> projectLibraryService.download(
			fixture.member().getEmail(),
			fixture.project().getId(),
			storedFileName
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.LIBRARY_RESOURCE_NOT_FOUND);
	}

	private TestFixture createFixture() {
		String suffix = UUID.randomUUID().toString();
		User leader = userRepository.save(User.create(
			"lib-leader-" + suffix,
			"password",
			"Leader",
			"lib-leader-" + suffix + "@example.com",
			UserRole.USER
		));
		User member = userRepository.save(User.create(
			"lib-member-" + suffix,
			"password",
			"Member",
			"lib-member-" + suffix + "@example.com",
			UserRole.USER
		));
		User outsider = userRepository.save(User.create(
			"lib-outsider-" + suffix,
			"password",
			"Outsider",
			"lib-outsider-" + suffix + "@example.com",
			UserRole.USER
		));
		Project project = projectRepository.save(Project.create(
			"Library Project " + suffix.substring(0, 8),
			"Library service test project",
			suffix.substring(0, 8).toUpperCase(),
			"C:\\WE_AI\\library-test",
			LocalDate.now(),
			LocalDate.now().plusDays(30),
			leader
		));
		projectMemberRepository.save(ProjectMember.leader(project, leader, ProjectDepartment.BACKEND));
		projectMemberRepository.save(ProjectMember.member(project, member, ProjectDepartment.FRONTEND));
		projectMemberRepository.flush();
		return new TestFixture(project, leader, member, outsider);
	}

	private record TestFixture(Project project, User leader, User member, User outsider) {
	}
}
