package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Both scenarios below must fail before any {@code ProcessBuilder} is started, so this test never
 * spawns a real OS process.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectBuildExecutionServiceTest {

	@Autowired
	private ProjectBuildExecutionService projectBuildExecutionService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void nonLeaderMemberCannotExecuteBuildTask() {
		TestFixture fixture = createFixture("C:\\WE_AI\\build-test");

		assertThatThrownBy(() ->
			projectBuildExecutionService.executeTask(fixture.member().getEmail(), fixture.project().getId(), "build")
		)
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_LEADER_ONLY);
	}

	@Test
	void leaderCannotExecuteBuildTaskWhenLocalPathDoesNotExist() {
		TestFixture fixture = createFixture("C:\\this\\path\\definitely\\does\\not\\exist\\on\\this\\machine");

		assertThatThrownBy(() ->
			projectBuildExecutionService.executeTask(fixture.leader().getEmail(), fixture.project().getId(), "build")
		)
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.BUILD_LOCAL_PATH_NOT_FOUND);
	}

	private TestFixture createFixture(String localPath) {
		String suffix = UUID.randomUUID().toString();
		User leader = userRepository.save(User.create(
			"build-leader-" + suffix,
			"password",
			"Build Leader",
			"build-leader-" + suffix + "@example.com",
			UserRole.USER
		));
		User member = userRepository.save(User.create(
			"build-member-" + suffix,
			"password",
			"Build Member",
			"build-member-" + suffix + "@example.com",
			UserRole.USER
		));
		Project project = projectRepository.save(Project.create(
			"Build Test Project " + suffix.substring(0, 8),
			"Build execution service test project",
			suffix.substring(0, 8).toUpperCase(),
			localPath,
			LocalDate.now(),
			LocalDate.now().plusDays(30),
			leader
		));
		projectMemberRepository.save(ProjectMember.leader(project, leader, ProjectDepartment.BACKEND));
		projectMemberRepository.save(ProjectMember.member(project, member, ProjectDepartment.FRONTEND));
		projectMemberRepository.flush();
		return new TestFixture(project, leader, member);
	}

	private record TestFixture(Project project, User leader, User member) {
	}
}
