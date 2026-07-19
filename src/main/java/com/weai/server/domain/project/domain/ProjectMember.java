package com.weai.server.domain.project.domain;

import com.weai.server.domain.user.domain.User;
import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "project_members",
	indexes = {
		@Index(name = "idx_project_members_project_user", columnList = "project_id, user_id", unique = true)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectMember extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectMemberRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProjectDepartment department;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectMemberStatus status;

	@Column(name = "joined_at", nullable = false)
	private LocalDateTime joinedAt;

	public static ProjectMember leader(Project project, User user, ProjectDepartment department) {
		return create(project, user, ProjectMemberRole.LEADER, department);
	}

	public static ProjectMember member(Project project, User user, ProjectDepartment department) {
		return create(project, user, ProjectMemberRole.MEMBER, department);
	}

	private static ProjectMember create(
		Project project,
		User user,
		ProjectMemberRole role,
		ProjectDepartment department
	) {
		return ProjectMember.builder()
			.project(project)
			.user(user)
			.role(role)
			.department(department)
			.status(ProjectMemberStatus.ACTIVE)
			.joinedAt(LocalDateTime.now())
			.build();
	}

	public boolean isActive() {
		return status == ProjectMemberStatus.ACTIVE;
	}

	public boolean isLeader() {
		return role == ProjectMemberRole.LEADER;
	}

	public boolean isKicked() {
		return status == ProjectMemberStatus.KICKED;
	}

	public void changeRole(ProjectMemberRole role) {
		this.role = role;
	}

	public void changeDepartment(ProjectDepartment department) {
		this.department = department;
	}

	public void leave() {
		this.status = ProjectMemberStatus.LEFT;
	}

	public void kick() {
		this.status = ProjectMemberStatus.KICKED;
	}

	public void reactivate(ProjectDepartment department) {
		this.role = ProjectMemberRole.MEMBER;
		this.department = department;
		this.status = ProjectMemberStatus.ACTIVE;
		this.joinedAt = LocalDateTime.now();
	}
}
