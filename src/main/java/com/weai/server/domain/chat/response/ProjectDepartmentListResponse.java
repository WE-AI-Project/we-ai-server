package com.weai.server.domain.chat.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "채팅방 생성용 프로젝트 부서 목록 응답")
public record ProjectDepartmentListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@ArraySchema(schema = @Schema(implementation = DepartmentItem.class))
	List<DepartmentItem> departments
) {

	@Schema(description = "프로젝트 부서 항목")
	public record DepartmentItem(
		@Schema(description = "부서", example = "BACKEND")
		ProjectDepartment department,

		@Schema(description = "해당 부서의 활성 멤버 수", example = "2")
		long memberCount,

		@Schema(description = "해당 부서의 활성 채팅방 존재 여부", example = "true")
		boolean chatRoomExists
	) {
	}
}
