package com.weai.server.domain.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weai.server.domain.chat.domain.BriefingStatus;
import com.weai.server.domain.chat.domain.ChatDocument;
import com.weai.server.domain.chat.domain.ChatMeeting;
import com.weai.server.domain.chat.domain.ChatRoom;
import com.weai.server.domain.chat.domain.DocumentBriefing;
import com.weai.server.domain.chat.domain.MeetingMinute;
import com.weai.server.domain.chat.domain.MeetingMinuteStatus;
import com.weai.server.domain.chat.domain.MeetingParticipant;
import com.weai.server.domain.chat.domain.MeetingStatus;
import com.weai.server.domain.chat.repository.ChatDocumentRepository;
import com.weai.server.domain.chat.repository.ChatMeetingRepository;
import com.weai.server.domain.chat.repository.ChatRoomRepository;
import com.weai.server.domain.chat.repository.DocumentBriefingRepository;
import com.weai.server.domain.chat.repository.MeetingMinuteRepository;
import com.weai.server.domain.chat.repository.MeetingParticipantRepository;
import com.weai.server.domain.chat.request.MeetingEndRequest;
import com.weai.server.domain.chat.request.MeetingStartRequest;
import com.weai.server.domain.chat.response.DocumentBriefingListResponse;
import com.weai.server.domain.chat.response.DocumentBriefingListResponse.BriefingSummaryResponse;
import com.weai.server.domain.chat.response.DocumentBriefingResponse;
import com.weai.server.domain.chat.response.DocumentUploadResponse;
import com.weai.server.domain.chat.response.MeetingEndResponse;
import com.weai.server.domain.chat.response.MeetingMinuteListResponse;
import com.weai.server.domain.chat.response.MeetingMinuteListResponse.MeetingMinuteSummaryResponse;
import com.weai.server.domain.chat.response.MeetingStartResponse;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatDocumentMeetingService {

	private static final long MAX_DOCUMENT_FILE_SIZE = 30L * 1024L * 1024L;
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final int MAX_TITLE_LENGTH = 100;
	private static final int MAX_DESCRIPTION_LENGTH = 500;
	private static final int MAX_MINUTE_CONTENT_LENGTH = 10_000;
	private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?。！？])\\s+|\\R+");
	private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of("pdf", "txt", "md", "doc", "docx", "ppt", "pptx");
	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

	private final ProjectService projectService;
	private final UserService userService;
	private final UserRepository userRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatDocumentRepository chatDocumentRepository;
	private final DocumentBriefingRepository documentBriefingRepository;
	private final ChatMeetingRepository chatMeetingRepository;
	private final MeetingMinuteRepository meetingMinuteRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final ObjectMapper objectMapper;
	private final DocumentTextExtractor documentTextExtractor;

	@Value("${chat.document.upload-root:uploads/projects}")
	private String documentUploadRoot;

	@Transactional
	public DocumentUploadResponse uploadDocument(
		String userEmail,
		Long projectId,
		MultipartFile file,
		String description
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		validateDocumentFile(file);
		String originalFileName = normalizeOriginalFileName(file.getOriginalFilename());
		String extension = extractExtension(originalFileName);
		String extractedText = documentTextExtractor.extract(file, extension);
		StoredDocumentFile storedFile = storeDocumentFile(projectId, file, originalFileName, extension);

		ChatDocument document = chatDocumentRepository.save(ChatDocument.uploaded(
			project,
			user,
			storedFile.originalFileName(),
			storedFile.storedFileName(),
			storedFile.fileUrl(),
			storedFile.fileSize(),
			storedFile.fileContentType(),
			storedFile.extension(),
			trimToNull(description),
			extractedText
		));

		return DocumentUploadResponse.from(document);
	}

	@Transactional
	public DocumentBriefingResponse createDocumentBriefing(String userEmail, Long projectId, Long documentId) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		ChatDocument document = getDocument(projectId, documentId);

		DocumentBriefing latestBriefing = documentBriefingRepository.findTopByDocument_IdOrderByCreatedAtDescIdDesc(documentId)
			.orElse(null);
		if (latestBriefing != null && latestBriefing.getStatus() == BriefingStatus.COMPLETED) {
			return toBriefingResponse(latestBriefing);
		}

		String extractedText = trimToNull(document.getExtractedText());
		if (extractedText == null) {
			throw new ApiException(ErrorCode.DOCUMENT_TEXT_NOT_EXTRACTED);
		}

		BriefingDraft draft = createBriefingDraft(extractedText);
		try {
			DocumentBriefing briefing = documentBriefingRepository.save(DocumentBriefing.builder()
				.document(document)
				.project(project)
				.creator(user)
				.summary(draft.summary())
				.keyPoints(toJson(draft.keyPoints()))
				.actionItems(toJson(draft.actionItems()))
				.risks(toJson(draft.risks()))
				.keywords(toJson(draft.keywords()))
				.status(BriefingStatus.COMPLETED)
				.build());
			document.markBriefingCreated();
			return toBriefingResponse(briefing);
		} catch (RuntimeException exception) {
			throw new ApiException(ErrorCode.DOCUMENT_BRIEFING_CREATE_FAILED);
		}
	}

	public DocumentBriefingListResponse getDocumentBriefings(
		String userEmail,
		Long projectId,
		Integer page,
		Integer size,
		String keyword,
		String status
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		Pageable pageable = normalizePageable(page, size);
		Page<DocumentBriefing> briefingPage = documentBriefingRepository.findPageByProjectIdAndFilters(
			projectId,
			trimToNull(keyword),
			parseBriefingStatus(status),
			pageable
		);
		Page<BriefingSummaryResponse> responsePage = new PageImpl<>(
			briefingPage.getContent().stream()
				.map(briefing -> BriefingSummaryResponse.from(briefing, fromJson(briefing.getKeyPoints())))
				.toList(),
			pageable,
			briefingPage.getTotalElements()
		);
		return DocumentBriefingListResponse.from(projectId, responsePage);
	}

	@Transactional
	public MeetingStartResponse startMeeting(String userEmail, Long projectId, MeetingStartRequest request) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		String title = validateTitle(request == null ? null : request.title());
		String description = validateDescription(request == null ? null : request.description());
		ChatRoom chatRoom = resolveChatRoom(projectId, request == null ? null : request.chatRoomId());

		if (chatMeetingRepository.findTopByProject_IdAndStatusOrderByStartedAtDescIdDesc(projectId, MeetingStatus.IN_PROGRESS)
			.isPresent()) {
			throw new ApiException(ErrorCode.MEETING_ALREADY_IN_PROGRESS);
		}

		ChatMeeting meeting = chatMeetingRepository.save(ChatMeeting.start(project, chatRoom, user, title, description));
		meetingParticipantRepository.save(MeetingParticipant.join(meeting, user, meeting.getStartedAt()));
		return MeetingStartResponse.from(meeting);
	}

	@Transactional
	public MeetingEndResponse endMeeting(
		String userEmail,
		Long projectId,
		Long meetingId,
		MeetingEndRequest request
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		ChatMeeting meeting = chatMeetingRepository.findByIdAndProject_Id(meetingId, projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.MEETING_NOT_FOUND));
		if (meeting.getStatus() == MeetingStatus.ENDED) {
			throw new ApiException(ErrorCode.MEETING_ALREADY_ENDED);
		}
		if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
			throw new ApiException(ErrorCode.MEETING_NOT_FOUND);
		}

		String content = validateMinuteContent(request == null ? null : request.content());
		String summary = resolveSummary(request == null ? null : request.summary(), content);
		List<String> actionItems = normalizeStringList(request == null ? null : request.actionItems());
		List<Long> participantIds = resolveParticipantIds(projectId, user.getId(), request == null ? null : request.participants());
		LocalDateTime endedAt = LocalDateTime.now();
		meeting.end(endedAt);

		for (Long participantId : participantIds) {
			if (!meetingParticipantRepository.existsByMeeting_IdAndUser_Id(meeting.getId(), participantId)) {
				User participant = userRepository.findById(participantId)
					.orElseThrow(() -> new ApiException(ErrorCode.MEETING_PARTICIPANT_NOT_PROJECT_MEMBER));
				meetingParticipantRepository.save(MeetingParticipant.join(meeting, participant, endedAt));
			}
		}

		MeetingMinute minute = meetingMinuteRepository.save(MeetingMinute.builder()
			.meeting(meeting)
			.project(project)
			.writer(user)
			.title(meeting.getTitle())
			.content(content)
			.summary(summary)
			.actionItems(toJson(actionItems))
			.status(MeetingMinuteStatus.CREATED)
			.build());

		return MeetingEndResponse.from(meeting, minute, actionItems);
	}

	public MeetingMinuteListResponse getMeetingMinutes(
		String userEmail,
		Long projectId,
		Integer page,
		Integer size,
		String keyword,
		LocalDate startDate,
		LocalDate endDate
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		Pageable pageable = normalizePageable(page, size);
		LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
		LocalDateTime endAt = endDate == null ? null : endDate.atTime(LocalTime.MAX);
		if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "endDate must be the same as or after startDate.");
		}

		Page<MeetingMinute> minutePage = meetingMinuteRepository.findPageByProjectIdAndFilters(
			projectId,
			trimToNull(keyword),
			startAt,
			endAt,
			pageable
		);
		Page<MeetingMinuteSummaryResponse> responsePage = new PageImpl<>(
			minutePage.getContent().stream()
				.map(minute -> MeetingMinuteSummaryResponse.from(
					minute,
					meetingParticipantRepository.countByMeeting_Id(minute.getMeeting().getId())
				))
				.toList(),
			pageable,
			minutePage.getTotalElements()
		);
		return MeetingMinuteListResponse.from(projectId, responsePage);
	}

	private StoredDocumentFile storeDocumentFile(
		Long projectId,
		MultipartFile file,
		String originalFileName,
		String extension
	) {
		String storedFileName = UUID.randomUUID() + "." + extension;
		Path projectDirectory = Paths.get(documentUploadRoot)
			.toAbsolutePath()
			.normalize()
			.resolve(projectId.toString())
			.resolve("documents")
			.normalize();
		Path targetPath = projectDirectory.resolve(storedFileName).normalize();
		if (!targetPath.startsWith(projectDirectory)) {
			throw new ApiException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "Invalid document file path.");
		}

		try {
			Files.createDirectories(projectDirectory);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.DOCUMENT_UPLOAD_FAILED);
		}

		return new StoredDocumentFile(
			originalFileName,
			storedFileName,
			"/uploads/projects/%d/documents/%s".formatted(projectId, storedFileName),
			file.getSize(),
			file.getContentType(),
			extension
		);
	}

	private void validateDocumentFile(MultipartFile file) {
		if (file == null) {
			throw new ApiException(ErrorCode.DOCUMENT_FILE_REQUIRED);
		}
		if (file.isEmpty()) {
			throw new ApiException(ErrorCode.DOCUMENT_FILE_EMPTY);
		}
		if (file.getSize() > MAX_DOCUMENT_FILE_SIZE) {
			throw new ApiException(ErrorCode.DOCUMENT_FILE_SIZE_EXCEEDED);
		}
		String extension = extractExtension(normalizeOriginalFileName(file.getOriginalFilename()));
		if (extension == null || !ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)) {
			throw new ApiException(ErrorCode.DOCUMENT_FILE_TYPE_NOT_ALLOWED);
		}
	}

	private ChatDocument getDocument(Long projectId, Long documentId) {
		return chatDocumentRepository.findByIdAndProject_IdAndDeletedAtIsNull(documentId, projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
	}

	private ChatRoom resolveChatRoom(Long projectId, Long chatRoomId) {
		if (chatRoomId == null) {
			return null;
		}
		ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
			.orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));
		if (!chatRoom.getProject().getId().equals(projectId) || !chatRoom.isActive()) {
			throw new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND);
		}
		return chatRoom;
	}

	private List<Long> resolveParticipantIds(Long projectId, Long currentUserId, List<Long> rawParticipantIds) {
		LinkedHashSet<Long> participantIds = new LinkedHashSet<>();
		participantIds.add(currentUserId);
		if (rawParticipantIds != null) {
			rawParticipantIds.stream()
				.filter(id -> id != null && id > 0)
				.forEach(participantIds::add);
		}
		for (Long participantId : participantIds) {
			if (!projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(
				projectId,
				participantId,
				ProjectMemberStatus.ACTIVE
			)) {
				throw new ApiException(ErrorCode.MEETING_PARTICIPANT_NOT_PROJECT_MEMBER);
			}
		}
		return participantIds.stream().toList();
	}

	private BriefingDraft createBriefingDraft(String text) {
		List<String> sentences = splitSentences(text);
		List<String> keyPoints = sentences.stream().limit(5).toList();
		String summary = sentences.isEmpty()
			? shorten(text, 500)
			: shorten(String.join(" ", sentences.stream().limit(3).toList()), 500);
		List<String> actionItems = sentences.stream()
			.filter(this::looksLikeActionItem)
			.limit(5)
			.toList();
		List<String> risks = sentences.stream()
			.filter(this::looksLikeRisk)
			.limit(5)
			.toList();
		List<String> keywords = extractKeywords(text);
		return new BriefingDraft(
			summary,
			keyPoints.isEmpty() ? List.of(shorten(text, 200)) : keyPoints,
			actionItems,
			risks,
			keywords
		);
	}

	private List<String> splitSentences(String text) {
		List<String> sentences = new ArrayList<>();
		for (String sentence : SENTENCE_SPLIT_PATTERN.split(text)) {
			String normalizedSentence = trimToNull(sentence);
			if (normalizedSentence != null) {
				sentences.add(shorten(normalizedSentence, 300));
			}
		}
		return sentences;
	}

	private boolean looksLikeActionItem(String sentence) {
		String normalized = sentence.toLowerCase(Locale.ROOT);
		return normalized.contains("todo")
			|| normalized.contains("action")
			|| normalized.contains("해야")
			|| normalized.contains("구현")
			|| normalized.contains("추가")
			|| normalized.contains("확인");
	}

	private boolean looksLikeRisk(String sentence) {
		String normalized = sentence.toLowerCase(Locale.ROOT);
		return normalized.contains("risk")
			|| normalized.contains("위험")
			|| normalized.contains("제한")
			|| normalized.contains("문제")
			|| normalized.contains("오류")
			|| normalized.contains("실패");
	}

	private List<String> extractKeywords(String text) {
		LinkedHashSet<String> keywords = new LinkedHashSet<>();
		for (String token : text.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHangul}]+", " ").split("\\s+")) {
			String normalizedToken = trimToNull(token);
			if (normalizedToken != null && normalizedToken.length() >= 2) {
				keywords.add(shorten(normalizedToken, 30));
			}
			if (keywords.size() >= 8) {
				break;
			}
		}
		return keywords.stream().toList();
	}

	private DocumentBriefingResponse toBriefingResponse(DocumentBriefing briefing) {
		return DocumentBriefingResponse.from(
			briefing,
			fromJson(briefing.getKeyPoints()),
			fromJson(briefing.getActionItems()),
			fromJson(briefing.getRisks()),
			fromJson(briefing.getKeywords())
		);
	}

	private BriefingStatus parseBriefingStatus(String rawStatus) {
		String status = trimToNull(rawStatus);
		if (status == null) {
			return null;
		}
		try {
			return BriefingStatus.valueOf(status.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "briefing status is invalid.");
		}
	}

	private Pageable normalizePageable(Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		if (resolvedPage < 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "page must be greater than or equal to 0.");
		}
		if (resolvedSize <= 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "size must be greater than 0.");
		}
		return PageRequest.of(
			resolvedPage,
			Math.min(resolvedSize, MAX_SIZE),
			Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
		);
	}

	private String validateTitle(String rawTitle) {
		String title = trimToNull(rawTitle);
		if (title == null) {
			throw new ApiException(ErrorCode.MEETING_TITLE_REQUIRED);
		}
		if (title.length() > MAX_TITLE_LENGTH) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "title must be 100 characters or fewer.");
		}
		return title;
	}

	private String validateDescription(String rawDescription) {
		String description = trimToNull(rawDescription);
		if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "description must be 500 characters or fewer.");
		}
		return description;
	}

	private String validateMinuteContent(String rawContent) {
		String content = trimToNull(rawContent);
		if (content == null) {
			throw new ApiException(ErrorCode.MEETING_MINUTE_CONTENT_REQUIRED);
		}
		if (content.length() > MAX_MINUTE_CONTENT_LENGTH) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "content must be 10000 characters or fewer.");
		}
		return content;
	}

	private String resolveSummary(String rawSummary, String content) {
		String summary = trimToNull(rawSummary);
		return summary == null ? shorten(content, 500) : shorten(summary, 2000);
	}

	private List<String> normalizeStringList(Collection<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
			.map(this::trimToNull)
			.filter(StringUtils::hasText)
			.map(value -> shorten(value, 300))
			.toList();
	}

	private String toJson(List<String> values) {
		try {
			return objectMapper.writeValueAsString(values == null ? List.of() : values);
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize list data.");
		}
	}

	private List<String> fromJson(String json) {
		String normalizedJson = trimToNull(json);
		if (normalizedJson == null) {
			return List.of();
		}
		try {
			return objectMapper.readValue(normalizedJson, STRING_LIST_TYPE);
		} catch (IOException exception) {
			return List.of();
		}
	}

	private String normalizeOriginalFileName(String originalFileName) {
		String cleanedFileName = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName);
		return cleanedFileName.isBlank() ? "document" : cleanedFileName;
	}

	private String extractExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
			return null;
		}
		return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
	}

	private String shorten(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private record StoredDocumentFile(
		String originalFileName,
		String storedFileName,
		String fileUrl,
		Long fileSize,
		String fileContentType,
		String extension
	) {
	}

	private record BriefingDraft(
		String summary,
		List<String> keyPoints,
		List<String> actionItems,
		List<String> risks,
		List<String> keywords
	) {
	}
}
