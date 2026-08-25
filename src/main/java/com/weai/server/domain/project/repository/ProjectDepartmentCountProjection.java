package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ProjectDepartment;

public interface ProjectDepartmentCountProjection {

	ProjectDepartment getDepartment();

	long getMemberCount();
}
