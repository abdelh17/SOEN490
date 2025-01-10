package com.quolance.quolance_api.services.business_workflow.impl;

import com.quolance.quolance_api.dtos.project.ProjectDto;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.services.business_workflow.AdminWorkflowService;
import com.quolance.quolance_api.services.entity_services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminWorkflowServiceImpl implements AdminWorkflowService {
    private final ProjectService projectService;

    @Override
    public Page<ProjectDto> getAllPendingProjects(Pageable pageable) {
        return projectService.getProjectsByStatuses(List.of(ProjectStatus.PENDING), pageable)
                .map(ProjectDto::fromEntity);
    }

    @Override
    public void approveProject(Long projectId) {
        projectService.updateProjectStatus(projectService.getProjectById(projectId), ProjectStatus.OPEN);
    }

    @Override
    public void rejectProject(Long projectId) {
        projectService.updateProjectStatus(projectService.getProjectById(projectId), ProjectStatus.REJECTED);
    }
}
