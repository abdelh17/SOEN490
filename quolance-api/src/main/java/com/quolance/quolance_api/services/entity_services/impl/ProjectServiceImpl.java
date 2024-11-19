package com.quolance.quolance_api.services.entity_services.impl;

import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.repositories.ProjectRepository;
import com.quolance.quolance_api.services.entity_services.ProjectService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    public void saveProject(Project project) {
        projectRepository.save(project);
    }

    @Override
    public void deleteProject(Project project) {
        projectRepository.delete(project);
    }

    @Override
    @Transactional
    public Project getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() ->
                ApiException.builder()
                        .status(HttpServletResponse.SC_NOT_FOUND)
                        .message("No Project found with ID: " + projectId)
                        .build());
        return project;
    }

    @Override
    @Transactional
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Override
    @Transactional
    public List<Project> getProjectsByStatuses(List<ProjectStatus> projectStatuses) {
        return projectRepository.findProjectsByProjectStatusIn(projectStatuses);
    }

    @Override
    @Transactional
    public List<Project> getProjectsByClientId(Long clientId) {
        return projectRepository.findProjectsByClientId(clientId);
    }

    @Override
    public void updateProjectStatus(Project project, ProjectStatus newStatus) {

        if (project.getProjectStatus() == ProjectStatus.CLOSED) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .message("Project is closed and cannot be updated.")
                    .build();
        }

        if (project.getProjectStatus() == newStatus) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_CONFLICT)
                    .message("Project is already in status: " + newStatus)
                    .build();
        }

        switch (newStatus) {
            case OPEN:
                if (project.getProjectStatus() == ProjectStatus.PENDING) {
                    project.setProjectStatus(ProjectStatus.OPEN);
                    projectRepository.save(project);
                }
                break;

            case CLOSED:
                if (project.getProjectStatus() == ProjectStatus.PENDING) {
                    project.setProjectStatus(ProjectStatus.CLOSED);
                    project.setVisibilityExpirationDate(LocalDate.now().plusDays(3));
                    projectRepository.save(project);

                }
                break;

            case REJECTED:
                if (project.getProjectStatus() == ProjectStatus.PENDING) {
                    project.setProjectStatus(ProjectStatus.REJECTED);
                    projectRepository.save(project);
                }
                break;

            default:
                throw ApiException.builder()
                        .status(HttpServletResponse.SC_BAD_REQUEST)
                        .message("Invalid status update request.")
                        .build();
        }
    }

    @Override
    public void updateSelectedFreelancer(Project project, User freelancer) {
        project.setSelectedFreelancer(freelancer);
        projectRepository.save(project);
    }

}