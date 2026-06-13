package com.weai.server.domain.ai.rag;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProjectRagContextService {

	private final ProjectRagRetriever projectRagRetriever;

	public ProjectRagContextService(ProjectRagRetriever projectRagRetriever) {
		this.projectRagRetriever = projectRagRetriever;
	}

	public ProjectRagContext retrieve(Long projectId, String query) {
		if (projectId == null || !StringUtils.hasText(query)) {
			return new ProjectRagContext(projectId, List.of());
		}
		return new ProjectRagContext(projectId, projectRagRetriever.retrieve(projectId, query));
	}
}
