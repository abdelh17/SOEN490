package com.quolance.quolance_api.services.entity_services.impl;

import com.quolance.quolance_api.dtos.project.ProjectUpdateDto;
import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.repositories.ProjectRepository;
import com.quolance.quolance_api.services.entity_services.ProjectService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Project getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() ->
                ApiException.builder()
                        .status(HttpServletResponse.SC_NOT_FOUND)
                        .message("No Project found with ID: " + projectId)
                        .build());
        return project;
    }

    @Override
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Override
    public Page<Project> getProjectsByClientId(Long clientId, Pageable pageable) {
        return projectRepository.findProjectsByClientId(clientId, pageable);
    }

    @Override
    public void updateProjectStatus(Project project, ProjectStatus newStatus) {

        // TODO: Handle updating open projects
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
                if (project.getProjectStatus() == ProjectStatus.PENDING || project.getProjectStatus() == ProjectStatus.OPEN) {
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

    @Override
    public void updateProject(Project existingProject, ProjectUpdateDto updateDto) {
        // First validate update DTO
        validateUpdateDto(updateDto);

        // Validate project status
        if (existingProject.getProjectStatus() != ProjectStatus.PENDING) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .message("Project can only be updated when in PENDING state")
                    .build();
        }

        // Proceed with update
        updateProjectFields(existingProject, updateDto);
        projectRepository.save(existingProject);
    }

    private void validateUpdateDto(ProjectUpdateDto updateDto) {
        if (updateDto == null) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_BAD_REQUEST)
                    .message("Update data cannot be null")
                    .build();
        }

        // Validate title
        if (updateDto.getTitle() != null && updateDto.getTitle().trim().isEmpty()) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_BAD_REQUEST)
                    .message("Title cannot be empty")
                    .build();
        }

        // Validate description
        if (updateDto.getDescription() != null && updateDto.getDescription().trim().isEmpty()) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_BAD_REQUEST)
                    .message("Description cannot be empty")
                    .build();
        }

        // Validate category
        if (updateDto.getCategory() != null && updateDto.getCategory().toString().trim().isEmpty()) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_BAD_REQUEST)
                    .message("Category cannot be empty")
                    .build();
        }

        // Validate priceRange
        if (updateDto.getPriceRange() != null && updateDto.getPriceRange().toString().trim().isEmpty()) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_BAD_REQUEST)
                    .message("Price range cannot be empty")
                    .build();
        }
    }

    private void updateProjectFields(Project project, ProjectUpdateDto updateDto) {
        if (updateDto.getTitle() != null) {
            project.setTitle(updateDto.getTitle().trim());
        }

        if (updateDto.getDescription() != null) {
            project.setDescription(updateDto.getDescription().trim());
        }

        if (updateDto.getCategory() != null) {
            project.setCategory(updateDto.getCategory());
        }

        if (updateDto.getPriceRange() != null) {
            project.setPriceRange(updateDto.getPriceRange());
        }

        if (updateDto.getExpectedDeliveryTime() != null) {
            project.setExpectedDeliveryTime(updateDto.getExpectedDeliveryTime());
        }

        if (updateDto.getExperienceLevel() != null) {
            project.setExperienceLevel(updateDto.getExperienceLevel());
        }
    }

    @Override
    public Page<Project> getProjectsByStatuses(List<ProjectStatus> projectStatuses, Pageable pageable) {
        return projectRepository.findProjectsByProjectStatusIn(projectStatuses, pageable);
    }
}
