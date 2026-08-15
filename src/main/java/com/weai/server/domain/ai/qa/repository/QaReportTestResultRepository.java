package com.weai.server.domain.ai.qa.repository;

import com.weai.server.domain.ai.qa.domain.QaReportTestResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaReportTestResultRepository extends JpaRepository<QaReportTestResult, Long> {

	List<QaReportTestResult> findByQaReport_IdOrderByIdAsc(Long qaReportId);
}
