package com.quolance.quolance_api.services.business_workflow;

import com.quolance.quolance_api.dtos.profile.FreelancerProfileDto;
import com.quolance.quolance_api.dtos.application.ApplicationDto;
import com.quolance.quolance_api.dtos.project.ProjectCreateDto;
import com.quolance.quolance_api.dtos.project.ProjectDto;
import com.quolance.quolance_api.dtos.project.ProjectUpdateDto;
import com.quolance.quolance_api.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClientWorkflowService {

    void createProject(ProjectCreateDto projectCreateDto, User client);
    ProjectDto getProject(Long projectId, User client);
    void deleteProject(Long projectId, User client);

    Page<ProjectDto> getAllClientProjects(User client, Pageable pageable);

    Page<ApplicationDto> getAllApplicationsToProject(Long projectId, User client, Pageable pageable);

    ProjectDto updateProject(Long projectId, ProjectUpdateDto projectUpdateDto, User client);
}
