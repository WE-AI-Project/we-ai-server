package com.weai.server.domain.ai.qa.domain;

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
	name = "qa_report_issues",
	indexes = {
		@Index(name = "idx_qa_report_issues_report", columnList = "qa_report_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class QaReportIssue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "qa_report_id", nullable = false)
	private QaReport qaReport;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QaIssueSeverity severity;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 2000)
	private String description;

	@Column(name = "file_path", length = 500)
	private String filePath;

	@Column(name = "line_number")
	private Integer lineNumber;

	@Column(length = 2000)
	private String suggestion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QaIssueStatus status;
}
