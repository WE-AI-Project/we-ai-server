package com.weai.server.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// Common Errors
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400", "Invalid request input."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401", "Authentication is required."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "You do not have permission to access this resource."),
	CONFLICT(HttpStatus.CONFLICT, "COMMON_409", "The request conflicts with existing data."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "The requested resource could not be found."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "The HTTP method is not supported for this endpoint."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON_415", "The content type is not supported."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "An unexpected server error occurred."),

	// Auth & User Errors
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_409_1", "The email address is already in use."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "The requested user could not be found."),
	INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "The password does not match."),
	INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_400_1", "The verification code is invalid."),
	EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_400_2", "The verification code has expired."),
	VERIFICATION_DELIVERY_FAILED(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"AUTH_500_1",
		"Failed to deliver the verification code."
	),
	PASSWORD_FIND_FAILED(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"AUTH_500_2",
		"Failed to process the password recovery request."
	),

	// Project Errors
	PROJECT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_1", "Project name is required."),
	PROJECT_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "PROJECT_400_2", "Project name must be 50 characters or fewer."),
	INVALID_PROJECT_DATE(HttpStatus.BAD_REQUEST, "PROJECT_400_3", "Project deadline cannot be earlier than today."),
	PROJECT_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_4", "Project code is required."),
	INVALID_PROJECT_CODE_FORMAT(HttpStatus.BAD_REQUEST, "PROJECT_400_5", "Project code must be 8 uppercase letters or digits."),
	PROJECT_PATH_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_6", "Project localPath is required."),
	ASSIGNEE_NOT_PROJECT_MEMBER(HttpStatus.BAD_REQUEST, "PROJECT_400_7", "The assignee must be an active member of the project."),
	SCHEDULE_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_8", "Schedule title is required."),
	INVALID_SCHEDULE_DATE(HttpStatus.BAD_REQUEST, "PROJECT_400_9", "Schedule endDate must be the same as or after startDate."),
	SCHEDULE_STATUS_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_10", "Schedule status is required."),
	INVALID_SCHEDULE_STATUS(HttpStatus.BAD_REQUEST, "PROJECT_400_11", "Schedule status is invalid."),
	TECH_STACK_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_12", "Tech stack name is required."),
	TECH_STACK_CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_13", "Tech stack category is required."),
	PROJECT_MEMBER_ROLE_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_14", "Project member role is required."),
	INVALID_PROJECT_MEMBER_ROLE(HttpStatus.BAD_REQUEST, "PROJECT_400_15", "Project member role is invalid."),
	PROJECT_MEMBER_DEPARTMENT_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_16", "Project member department is required."),
	INVALID_PROJECT_MEMBER_DEPARTMENT(HttpStatus.BAD_REQUEST, "PROJECT_400_17", "Project member department is invalid."),
	PROJECT_MEMBER_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "PROJECT_400_18", "The project member is not active."),
	CANNOT_CHANGE_OWN_LEADER_ROLE(HttpStatus.BAD_REQUEST, "PROJECT_400_19", "You cannot change your own leader role."),
	PROJECT_LEADER_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_20", "At least one project leader is required."),
	INVALID_PROJECT_STATUS(HttpStatus.BAD_REQUEST, "PROJECT_400_21", "Project status is invalid."),
	INVALID_PROJECT_REPOSITORY_TYPE(HttpStatus.BAD_REQUEST, "PROJECT_400_22", "Project repository type is invalid."),
	PROJECT_MEMBER_ALREADY_LEFT(HttpStatus.BAD_REQUEST, "PROJECT_400_23", "The project member has already left."),
	CANNOT_LEAVE_LAST_LEADER_PROJECT(HttpStatus.BAD_REQUEST, "PROJECT_400_24", "The last project leader cannot leave the project."),
	CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "PROJECT_400_25", "You cannot kick yourself from the project."),
	PROJECT_ALREADY_ARCHIVED(HttpStatus.BAD_REQUEST, "PROJECT_400_26", "The project is already archived."),
	PROJECT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "PROJECT_400_27", "The project is already deleted."),
	PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_1", "The requested project could not be found."),
	ASSIGNEE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_2", "The requested assignee could not be found."),
	SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_3", "The requested project schedule could not be found."),
	TECH_STACK_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_4", "The requested project tech stack could not be found."),
	PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_5", "The requested project member could not be found."),
	PROJECT_REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_6", "The requested project repository could not be found."),
	PROJECT_COMMIT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_7", "The requested project commit could not be found."),
	PROJECT_COMMIT_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_8", "The requested project commit file could not be found."),
	PROJECT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "PROJECT_403_1", "The project is not active."),
	PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PROJECT_403_2", "You are not an active member of this project."),
	PROJECT_LEADER_ONLY(HttpStatus.FORBIDDEN, "PROJECT_403_3", "Only the project leader can perform this action."),
	ALREADY_JOINED_PROJECT(HttpStatus.CONFLICT, "PROJECT_409_1", "You are already an active member of this project."),
	TECH_STACK_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROJECT_409_2", "The tech stack already exists in this project."),
	PROJECT_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_1", "Failed to generate a project code."),
	PROJECT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_2", "Failed to create the project."),
	PROJECT_JOIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_3", "Failed to join the project."),
	SCHEDULE_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_4", "Failed to create the project schedule."),
	PROJECT_GIT_COMMAND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_5", "Failed to execute the project git command."),
	DAILY_STANDUP_DISMISS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_6", "Failed to save daily standup dismissal."),
	DAILY_STANDUP_SUMMARY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_7", "Failed to retrieve daily standup summary."),
	INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_8", "Failed to generate an invite code."),
	GIT_REPOSITORY_PATH_NOT_FOUND(HttpStatus.BAD_REQUEST, "GIT_400_1", "Project git repository path is required."),
	GIT_REPOSITORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "GIT_400_2", "Git repository could not be found at the project path."),
	GIT_FILE_PATH_REQUIRED(HttpStatus.BAD_REQUEST, "GIT_400_3", "Git file path is required."),
	INVALID_GIT_FILE_PATH(HttpStatus.BAD_REQUEST, "GIT_400_4", "Git file path must be a relative path inside the repository."),
	GIT_STAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GIT_500_1", "Failed to stage the requested files."),
	GIT_UNSTAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GIT_500_2", "Failed to unstage the requested files."),
	GIT_STAGE_ALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GIT_500_3", "Failed to stage all files."),
	GIT_UNSTAGE_ALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GIT_500_4", "Failed to unstage all files."),
	GIT_COMMAND_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GIT_500_5", "Failed to execute the git command."),
	GIT_CHANGED_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "GIT_404_1", "The requested changed file could not be found."),
	GIT_DIFF_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GIT_500_6", "Failed to read the requested file diff."),
	GIT_COMMIT_MESSAGE_REQUIRED(HttpStatus.BAD_REQUEST, "GIT_400_5", "Git commit message is required."),
	GIT_COMMIT_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "GIT_400_6", "Git commit message must be 200 characters or fewer."),
	GIT_COMMIT_DESCRIPTION_TOO_LONG(
		HttpStatus.BAD_REQUEST,
		"GIT_400_7",
		"Git commit description must be 1000 characters or fewer."
	),
	GIT_NO_STAGED_FILES(HttpStatus.BAD_REQUEST, "GIT_400_8", "There are no staged files to commit."),
	GIT_COMMIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GIT_500_7", "Failed to create the git commit."),
	INVALID_GIT_BRANCH_NAME(HttpStatus.BAD_REQUEST, "GIT_400_9", "Git branch name is invalid."),
	GIT_BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "GIT_404_2", "The requested git branch could not be found."),
	INVALID_GIT_GRAPH_LIMIT(HttpStatus.BAD_REQUEST, "GIT_400_10", "Git graph limit must be between 1 and 200."),
	GIT_BRANCH_GRAPH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GIT_500_8", "Failed to retrieve the git branch graph."),

	// QA Query Errors
	INVALID_QA_REPORT_STATUS(HttpStatus.BAD_REQUEST, "QA_400_1", "QA report status is invalid."),
	INVALID_SPRING_PROFILE(HttpStatus.BAD_REQUEST, "ENVIRONMENT_400_1", "Spring profile is invalid."),
	QA_RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "QA_404_1", "The requested QA run could not be found."),
	QA_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "QA_404_2", "The requested QA report could not be found."),
	COMMIT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMIT_404_1", "The requested commit could not be found."),

	// Notification Errors
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404_1", "The requested notification could not be found."),
	NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "NOTIFICATION_403_1", "You cannot access this notification."),
	NOTIFICATION_READ_FAILED(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"NOTIFICATION_500_1",
		"Failed to mark the notification as read."
	),
	NOTIFICATION_DELETE_FAILED(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"NOTIFICATION_500_2",
		"Failed to delete the notification."
	),

	// Chat Errors
	INVALID_CHAT_ROOM_TYPE(HttpStatus.BAD_REQUEST, "CHAT_400_1", "Chat room type is invalid."),
	INVALID_DEPARTMENT(HttpStatus.BAD_REQUEST, "CHAT_400_2", "Department is invalid."),
	CHAT_MESSAGE_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_400_3", "Chat message content is required."),
	CHAT_MESSAGE_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "CHAT_400_4", "Chat message content is too long."),
	INVALID_CHAT_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "CHAT_400_5", "Chat message type is invalid."),
	CHAT_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_400_6", "Chat file is required."),
	CHAT_FILE_EMPTY(HttpStatus.BAD_REQUEST, "CHAT_400_7", "Chat file is empty."),
	CHAT_FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "CHAT_400_8", "Chat file size exceeded."),
	CHAT_FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CHAT_400_9", "Chat file type is not allowed."),
	CHAT_ROOM_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_400_10", "Chat room name is required."),
	CHAT_ROOM_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "CHAT_400_11", "Chat room name must be 50 characters or fewer."),
	CHAT_ROOM_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_400_12", "Chat room type is required."),
	DEPARTMENT_NOT_ALLOWED_FOR_GENERAL_CHAT_ROOM(
		HttpStatus.BAD_REQUEST,
		"CHAT_400_13",
		"Department is not allowed for a general chat room."
	),
	CHAT_ROOM_DEPARTMENT_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_400_14", "Department is required for a department chat room."),
	CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT_403_1", "You cannot access this chat room."),
	CHAT_ROOM_NOT_ACTIVE(HttpStatus.FORBIDDEN, "CHAT_403_2", "The chat room is not active."),
	CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404_1", "The requested chat room could not be found."),
	CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404_2", "The requested chat message could not be found."),
	PROJECT_DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404_3", "The department has no active members in this project."),
	DEPARTMENT_CHAT_ROOM_ALREADY_EXISTS(
		HttpStatus.CONFLICT,
		"CHAT_409_1",
		"An active chat room already exists for this department."
	),
	DEFAULT_CHAT_ROOM_ALREADY_EXISTS(
		HttpStatus.CONFLICT,
		"CHAT_409_2",
		"An active default chat room already exists for this project."
	),
	CHAT_ROOM_ALREADY_EXISTS(
		HttpStatus.CONFLICT,
		"CHAT_409_3",
		"An active chat room with the same name already exists in this project."
	),
	CHAT_FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT_500_1", "Failed to upload the chat file."),

	// Chat Document & Meeting Errors
	DOCUMENT_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_DOCUMENT_400_1", "Document file is required."),
	DOCUMENT_FILE_EMPTY(HttpStatus.BAD_REQUEST, "CHAT_DOCUMENT_400_2", "Document file is empty."),
	DOCUMENT_FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "CHAT_DOCUMENT_400_3", "Document file size exceeded."),
	DOCUMENT_FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CHAT_DOCUMENT_400_4", "Document file type is not allowed."),
	DOCUMENT_TEXT_NOT_EXTRACTED(HttpStatus.BAD_REQUEST, "CHAT_DOCUMENT_400_5", "Document text has not been extracted."),
	MEETING_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_MEETING_400_1", "Meeting title is required."),
	MEETING_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "CHAT_MEETING_400_2", "Meeting has already ended."),
	MEETING_MINUTE_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_MEETING_400_3", "Meeting minute content is required."),
	DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_DOCUMENT_404_1", "The requested document could not be found."),
	DOCUMENT_BRIEFING_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_DOCUMENT_404_2", "The requested document briefing could not be found."),
	MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_MEETING_404_1", "The requested meeting could not be found."),
	MEETING_MINUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_MEETING_404_2", "The requested meeting minute could not be found."),
	MEETING_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "CHAT_MEETING_409_1", "A meeting is already in progress."),
	MEETING_PARTICIPANT_NOT_PROJECT_MEMBER(HttpStatus.BAD_REQUEST, "CHAT_MEETING_400_4", "Meeting participant is not an active project member."),
	DOCUMENT_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT_DOCUMENT_500_1", "Failed to upload the document."),
	DOCUMENT_BRIEFING_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT_DOCUMENT_500_2", "Failed to create the document briefing.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
