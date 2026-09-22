package com.weai.server.domain.project.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.weai.server.domain.chat.event.MeetingFileUploadedEvent;
import com.weai.server.domain.chat.response.DocumentUploadResponse;
import com.weai.server.domain.chat.service.ChatDocumentMeetingService;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectLibraryResource;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.LibraryResourceSource;
import com.weai.server.domain.project.repository.ProjectLibraryResourceRepository;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that uploading a chat/meeting document (the only meeting-adjacent file upload path
 * today) automatically mirrors the file into the project's Shared Library, so teammates who
 * never open the chat still see meeting materials there.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MeetingLibrarySyncListenerTest {

	@Autowired
	private ChatDocumentMeetingService chatDocumentMeetingService;

	@Autowired
	private ProjectLibraryResourceRepository projectLibraryResourceRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MeetingLibrarySyncListener meetingLibrarySyncListener;

	@Test
	void uploadingAChatDocumentSyncsItIntoTheSharedLibrary() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User leader = userRepository.save(User.create(
			"meeting-leader-" + suffix,
			"password",
			"Leader",
			"meeting-leader-" + suffix + "@example.com",
			UserRole.USER
		));
		Project project = projectRepository.save(Project.create(
			"Meeting Sync Project " + suffix,
			"Meeting library sync test project",
			suffix.toUpperCase(),
			"C:\\WE_AI\\meeting-sync-test",
			LocalDate.now(),
			LocalDate.now().plusDays(30),
			leader
		));
		projectMemberRepository.save(ProjectMember.leader(project, leader, ProjectDepartment.BACKEND));
		projectMemberRepository.flush();

		MockMultipartFile file = new MockMultipartFile(
			"file", "meeting-notes.md", "text/markdown", "회의록 내용입니다.".getBytes()
		);

		DocumentUploadResponse uploaded = chatDocumentMeetingService.uploadDocument(
			leader.getEmail(),
			project.getId(),
			file,
			"주간 회의 자료"
		);

		List<ProjectLibraryResource> synced = projectLibraryResourceRepository
			.findByProject_IdAndDeletedAtIsNull(project.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
			.getContent();

		assertThat(synced).hasSize(1);
		ProjectLibraryResource resource = synced.get(0);
		assertThat(resource.getSource()).isEqualTo(LibraryResourceSource.MEETING);
		assertThat(resource.getSourceDocumentId()).isEqualTo(uploaded.documentId());
		assertThat(resource.getOriginalFileName()).isEqualTo("meeting-notes.md");
		assertThat(resource.getFileUrl()).isEqualTo(uploaded.fileUrl());
		assertThat(resource.getUploader().getId()).isEqualTo(leader.getId());
	}

	@Test
	void uploadingTheSameDocumentTwiceDoesNotDuplicateTheLibraryEntry() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User leader = userRepository.save(User.create(
			"meeting-leader2-" + suffix,
			"password",
			"Leader",
			"meeting-leader2-" + suffix + "@example.com",
			UserRole.USER
		));
		Project project = projectRepository.save(Project.create(
			"Meeting Sync Project 2 " + suffix,
			"Meeting library sync test project",
			suffix.toUpperCase(),
			"C:\\WE_AI\\meeting-sync-test-2",
			LocalDate.now(),
			LocalDate.now().plusDays(30),
			leader
		));
		projectMemberRepository.save(ProjectMember.leader(project, leader, ProjectDepartment.BACKEND));
		projectMemberRepository.flush();

		MockMultipartFile file = new MockMultipartFile(
			"file", "meeting-notes.md", "text/markdown", "회의록 내용입니다.".getBytes()
		);
		DocumentUploadResponse uploaded = chatDocumentMeetingService.uploadDocument(
			leader.getEmail(), project.getId(), file, null
		);

		// 동일 문서에 대해 이벤트가 재처리되어도(예: 재전송/재시도) 중복 생성되지 않아야 한다.
		long countAfterUpload = projectLibraryResourceRepository.count();
		meetingLibrarySyncListener.handleMeetingFileUploaded(
			new MeetingFileUploadedEvent(project.getId(), uploaded.documentId())
		);

		assertThat(projectLibraryResourceRepository.count()).isEqualTo(countAfterUpload);
	}
}
