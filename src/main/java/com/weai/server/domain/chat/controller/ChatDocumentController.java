package com.weai.server.domain.chat.controller;

import com.weai.server.domain.chat.response.DocumentBriefingListResponse;
import com.weai.server.domain.chat.response.DocumentBriefingResponse;
import com.weai.server.domain.chat.response.DocumentUploadResponse;
import com.weai.server.domain.chat.service.ChatDocumentMeetingService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chat Document", description = "채팅 문서 업로드와 문서 브리핑 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/chat")
public class ChatDocumentController {

	private final ChatDocumentMeetingService chatDocumentMeetingService;

	@Operation(summary = "문서 업로드", description = "프로젝트 채팅 문서 탭에서 사용할 문서를 multipart/form-data 방식으로 업로드합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.DOCUMENT_FILE_REQUIRED,
		ErrorCode.DOCUMENT_FILE_EMPTY,
		ErrorCode.DOCUMENT_FILE_SIZE_EXCEEDED,
		ErrorCode.DOCUMENT_FILE_TYPE_NOT_ALLOWED,
		ErrorCode.DOCUMENT_UPLOAD_FAILED
	})
	@PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<DocumentUploadResponse> uploadDocument(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "업로드할 문서 파일") @RequestParam(required = false) MultipartFile file,
		@Parameter(description = "문서 설명") @RequestParam(required = false) String description
	) {
		return ApiResponse.success(
			"DOCUMENT_UPLOAD_SUCCESS",
			"문서가 업로드되었습니다.",
			chatDocumentMeetingService.uploadDocument(authentication.getName(), projectId, file, description)
		);
	}

	@Operation(summary = "문서 브리핑 생성", description = "업로드된 문서의 추출 텍스트를 기반으로 문서 브리핑을 생성합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.DOCUMENT_NOT_FOUND,
		ErrorCode.DOCUMENT_TEXT_NOT_EXTRACTED,
		ErrorCode.DOCUMENT_BRIEFING_CREATE_FAILED
	})
	@PostMapping("/documents/{documentId}/briefing")
	public ApiResponse<DocumentBriefingResponse> createDocumentBriefing(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "문서 ID") @PathVariable Long documentId
	) {
		return ApiResponse.success(
			"DOCUMENT_BRIEFING_CREATE_SUCCESS",
			"문서 브리핑이 생성되었습니다.",
			chatDocumentMeetingService.createDocumentBriefing(authentication.getName(), projectId, documentId)
		);
	}

	@Operation(summary = "문서 브리핑 목록 조회", description = "프로젝트에 생성된 문서 브리핑 목록을 생성일 내림차순으로 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/document-briefings")
	public ApiResponse<DocumentBriefingListResponse> getDocumentBriefings(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
		@Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
		@Parameter(description = "문서명 또는 요약 검색어") @RequestParam(required = false) String keyword,
		@Parameter(description = "브리핑 상태") @RequestParam(required = false) String status
	) {
		return ApiResponse.success(
			"DOCUMENT_BRIEFING_LIST_SUCCESS",
			"문서 브리핑 목록 조회에 성공했습니다.",
			chatDocumentMeetingService.getDocumentBriefings(authentication.getName(), projectId, page, size, keyword, status)
		);
	}
}
