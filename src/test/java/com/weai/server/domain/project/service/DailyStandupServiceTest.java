package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectSchedule;
import com.weai.server.domain.project.domain.ProjectSchedulePriority;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.project.repository.ProjectScheduleRepository;
import com.weai.server.domain.project.response.DailyStandupMemberResponse;
import com.weai.server.domain.project.response.DailyStandupSummaryResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyStandupServiceTest {

	@Autowired
	private DailyStandupService dailyStandupService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private ProjectScheduleRepository projectScheduleRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void groupsScheduleItemsPerMemberInsteadOfReturningAnEmptyRoster() {
		Fixture fixture = createFixture();
		projectScheduleRepository.save(schedule(fixture.project(), fixture.backendMember(), "인증 API 구현", ProjectDepartment.BACKEND, ProjectScheduleStatus.DONE));
		projectScheduleRepository.save(schedule(fixture.project(), fixture.frontendMember(), "로그인 화면 퍼블리싱", ProjectDepartment.FRONTEND, ProjectScheduleStatus.IN_PROGRESS));
		projectScheduleRepository.save(schedule(fixture.project(), fixture.frontendMember(), "배포 파이프라인 막힘", ProjectDepartment.FRONTEND, ProjectScheduleStatus.HOLD));

		DailyStandupSummaryResponse response = dailyStandupService.getSummary(fixture.leader().getEmail(), fixture.project().getId());

		// 리더(backendMember, secondBackendMember, frontendMember) + 본인 = 4명 전원이 로스터에 나온다.
		assertThat(response.members()).hasSize(4);

		DailyStandupMemberResponse viewerOwnEntry = findMember(response.members(), fixture.leader());
		// 조회자 본인 항목은 부서가 같아도 "나에게 관련"으로 표시하지 않는다.
		assertThat(viewerOwnEntry.relevantToMe()).isFalse();

		DailyStandupMemberResponse backend = findMember(response.members(), fixture.backendMember());
		assertThat(backend.completed()).extracting("title").containsExactly("인증 API 구현");
		assertThat(backend.inProgress()).isEmpty();
		assertThat(backend.blockers()).isEmpty();
		// 같은 BACKEND 부서 소속이라 관련 있다고 표시한다.
		assertThat(backend.relevantToMe()).isTrue();

		DailyStandupMemberResponse frontend = findMember(response.members(), fixture.frontendMember());
		assertThat(frontend.inProgress()).extracting("title").containsExactly("로그인 화면 퍼블리싱");
		assertThat(frontend.blockers()).extracting("title").containsExactly("배포 파이프라인 막힘");
		// 블로커가 있으면 부서가 달라도 관련 있다고 표시한다.
		assertThat(frontend.relevantToMe()).isTrue();
		assertThat(frontend.relevantReason()).contains("블로커");
		assertThat(frontend.navigatePage()).isEqualTo("Calendar");
	}

	@Test
	void marksSameDepartmentMembersAsRelevantEvenWithoutBlockers() {
		Fixture fixture = createFixture();
		projectScheduleRepository.save(schedule(fixture.project(), fixture.secondBackendMember(), "리팩터링", ProjectDepartment.BACKEND, ProjectScheduleStatus.IN_PROGRESS));

		DailyStandupSummaryResponse response = dailyStandupService.getSummary(fixture.leader().getEmail(), fixture.project().getId());

		DailyStandupMemberResponse sameDept = findMember(response.members(), fixture.secondBackendMember());
		assertThat(sameDept.relevantToMe()).isTrue();
		assertThat(sameDept.relevantReason()).contains("BACKEND");
	}

	private DailyStandupMemberResponse findMember(List<DailyStandupMemberResponse> members, User user) {
		return members.stream()
			.filter(m -> m.name().equals(user.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No member briefing found for " + user.getName()));
	}

	private ProjectSchedule schedule(Project project, User assignee, String title, ProjectDepartment department, ProjectScheduleStatus status) {
		return ProjectSchedule.create(
			project,
			assignee,
			title,
			"desc",
			department,
			LocalDate.now(),
			LocalDate.now().plusDays(3),
			ProjectSchedulePriority.MEDIUM,
			status
		);
	}

	private Fixture createFixture() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User leader = userRepository.save(User.create("standup-leader-" + suffix, "password", "리더", "standup-leader-" + suffix + "@example.com", UserRole.USER));
		User backendMember = userRepository.save(User.create("standup-be-" + suffix, "password", "백엔드팀원", "standup-be-" + suffix + "@example.com", UserRole.USER));
		User secondBackendMember = userRepository.save(User.create("standup-be2-" + suffix, "password", "백엔드팀원2", "standup-be2-" + suffix + "@example.com", UserRole.USER));
		User frontendMember = userRepository.save(User.create("standup-fe-" + suffix, "password", "프론트팀원", "standup-fe-" + suffix + "@example.com", UserRole.USER));

		Project project = projectRepository.save(Project.create(
			"Standup Project " + suffix,
			"Daily standup test project",
			suffix.toUpperCase(),
			"C:\\WE_AI\\standup-test",
			LocalDate.now(),
			LocalDate.now().plusDays(30),
			leader
		));

		projectMemberRepository.save(ProjectMember.leader(project, leader, ProjectDepartment.BACKEND));
		projectMemberRepository.save(ProjectMember.member(project, backendMember, ProjectDepartment.BACKEND));
		projectMemberRepository.save(ProjectMember.member(project, secondBackendMember, ProjectDepartment.BACKEND));
		projectMemberRepository.save(ProjectMember.member(project, frontendMember, ProjectDepartment.FRONTEND));
		projectMemberRepository.flush();

		return new Fixture(project, leader, backendMember, secondBackendMember, frontendMember);
	}

	private record Fixture(Project project, User leader, User backendMember, User secondBackendMember, User frontendMember) {
	}
}
