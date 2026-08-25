package com.weai.server.domain.chat.controller;

import com.weai.server.domain.chat.response.ProjectDepartmentListResponse;
import com.weai.server.domain.chat.service.ChatRoomService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "채팅", description = "프로젝트 채팅 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectDepartmentController {

	private final ChatRoomService chatRoomService;

	@Operation(
		summary = "프로젝트 부서 목록 조회",
		description = "부서 채팅방 생성 시 선택할 수 있는 프로젝트의 실제 부서 목록을 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/departments")
	public ApiResponse<ProjectDepartmentListResponse> getProjectDepartments(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"PROJECT_DEPARTMENT_LIST_SUCCESS",
			"프로젝트 부서 목록 조회에 성공했습니다.",
			chatRoomService.getProjectDepartments(authentication.getName(), projectId)
		);
	}
}
