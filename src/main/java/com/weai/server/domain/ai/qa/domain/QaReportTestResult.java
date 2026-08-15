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
	name = "qa_report_test_results",
	indexes = {
		@Index(name = "idx_qa_report_test_results_report", columnList = "qa_report_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class QaReportTestResult extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "qa_report_id", nullable = false)
	private QaReport qaReport;

	@Column(name = "test_name", nullable = false, length = 200)
	private String testName;

	@Enumerated(EnumType.STRING)
	@Column(name = "test_type", nullable = false, length = 30)
	private QaTestType testType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QaTestStatus status;

	@Column(length = 1000)
	private String message;

	@Column(name = "duration_ms")
	private Long durationMs;
}
