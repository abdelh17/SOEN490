package com.quolance.quolance_api.services.business_workflow;

import com.quolance.quolance_api.dtos.project.ProjectDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminWorkflowService {
    Page<ProjectDto> getAllPendingProjects(Pageable pageable);

    void approveProject(Long projectId);

    void rejectProject(Long projectId);
}
