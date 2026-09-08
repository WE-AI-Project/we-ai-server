package com.weai.server.domain.project.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.weai.server.domain.project.domain.ProjectDepartment;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectDepartmentStatusResponseTest {

	@Test
	@DisplayName("일정 수가 0개이면 상태는 READY여야 한다")
	void whenScheduleCountIsZero_thenStatusIsReady() {
		ProjectDepartmentStatusResponse.DepartmentStatusItem item =
			new ProjectDepartmentStatusResponse.DepartmentStatusItem(
				ProjectDepartment.BACKEND,
				1L,
				0L,
				0L,
				0L,
				0L,
				0L,
				0,
				ProjectDepartmentStatusResponse.DepartmentStatusItem.resolveStatus(0L, 0, false),
				0L
			);

		assertThat(item.status()).isEqualTo("READY");
		assertThat(item.totalScheduleCount()).isEqualTo(0L);
	}

	@Test
	@DisplayName("진행률이 100%이면 상태는 COMPLETED여야 한다")
	void whenProgressRateIs100_thenStatusIsCompleted() {
		String status = ProjectDepartmentStatusResponse.DepartmentStatusItem.resolveStatus(5L, 100, false);
		assertThat(status).isEqualTo("COMPLETED");
	}

	@Test
	@DisplayName("마감일이 지난 일정이 있으면 상태는 DELAYED여야 한다")
	void whenHasDelayedSchedules_thenStatusIsDelayed() {
		String status = ProjectDepartmentStatusResponse.DepartmentStatusItem.resolveStatus(5L, 50, true);
		assertThat(status).isEqualTo("DELAYED");
	}

	@Test
	@DisplayName("정상 진행 중이면 상태는 IN_PROGRESS여야 한다")
	void whenNormallyProgressing_thenStatusIsInProgress() {
		String status = ProjectDepartmentStatusResponse.DepartmentStatusItem.resolveStatus(5L, 50, false);
		assertThat(status).isEqualTo("IN_PROGRESS");
	}
}
