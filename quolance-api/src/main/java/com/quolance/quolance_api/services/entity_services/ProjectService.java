package com.quolance.quolance_api.services.entity_services;


import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.dtos.project.ProjectUpdateDto;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectService {

    void saveProject(Project project);
    void deleteProject(Project project);
    Project getProjectById(Long projectId);
    void updateProject(Project existingProject, ProjectUpdateDto updatedProject);

    List<Project> getAllProjects();
    Page<Project> getProjectsByStatuses(List<ProjectStatus> projectStatuses, Pageable pageable);
    Page<Project> getProjectsByClientId(Long clientId, Pageable pageable);

    void updateProjectStatus(Project project, ProjectStatus newStatus);
    void updateSelectedFreelancer(Project project, User freelancer);
}
