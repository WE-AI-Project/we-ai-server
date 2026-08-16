package com.weai.server.domain.chat.domain;

import com.weai.server.domain.project.domain.Project;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "document_briefings",
	indexes = {
		@Index(name = "idx_document_briefings_project_status", columnList = "project_id,status"),
		@Index(name = "idx_document_briefings_document_created", columnList = "document_id,created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DocumentBriefing extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "briefing_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private ChatDocument document;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creator_id", nullable = false)
	private User creator;

	@Column(nullable = false, length = 2000)
	private String summary;

	@Column(name = "key_points", columnDefinition = "LONGTEXT")
	private String keyPoints;

	@Column(name = "action_items", columnDefinition = "LONGTEXT")
	private String actionItems;

	@Column(name = "risks", columnDefinition = "LONGTEXT")
	private String risks;

	@Column(name = "keywords", columnDefinition = "LONGTEXT")
	private String keywords;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BriefingStatus status;
}
