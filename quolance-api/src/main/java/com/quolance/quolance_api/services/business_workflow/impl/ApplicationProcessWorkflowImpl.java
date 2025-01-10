package com.quolance.quolance_api.services.business_workflow.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quolance.quolance_api.entities.Application;
import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ApplicationStatus;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.services.business_workflow.ApplicationProcessWorkflow;
import com.quolance.quolance_api.services.entity_services.ApplicationService;
import com.quolance.quolance_api.services.entity_services.ProjectService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;


@Service
@RequiredArgsConstructor
public class ApplicationProcessWorkflowImpl implements ApplicationProcessWorkflow {

    private final ApplicationService applicationService;
    private final ProjectService projectService;

    @Override
    @Transactional
    public void selectFreelancer(Long applicationId, User client) {
        try {
            Application application = applicationService.getApplicationById(applicationId);
            Project project = application.getProject();

            validateClientProjectOwnership(project, client.getId());
            validateProjectForFreelancerSelection(project);
            validateProjectForConfirmation(project);

            applicationService.updateApplicationStatus(application, ApplicationStatus.ACCEPTED);
            projectService.updateProjectStatus(project, ProjectStatus.CLOSED);
            projectService.updateSelectedFreelancer(project, application.getFreelancer());

            // Reject all other applications using pagination
            int pageSize = 100;
            Pageable pageable = PageRequest.of(0, pageSize);
            Page<Application> page;

            do {
                page = applicationService.getAllApplicationsByProjectId(project.getId(), pageable);
                page.getContent().forEach(app -> {
                    if(!app.getId().equals(applicationId)){
                        applicationService.updateApplicationStatus(app, ApplicationStatus.REJECTED);
                    }
                });
                pageable = page.nextPageable();
            } while(page.hasNext());

        } catch (OptimisticLockException e) {
            handleOptimisticLockException(e);
        }
    }

    @Override
    public void rejectApplication(Long applicationId, User client) {
        try {
            // Fetch the application
            Application application = applicationService.getApplicationById(applicationId);
            Project project = application.getProject();

            validateClientProjectOwnership(project, client.getId());
            validateProjectForFreelancerSelection(project);
            validateProjectForConfirmation(project);

            applicationService.updateApplicationStatus(application, ApplicationStatus.REJECTED);

        } catch (OptimisticLockException e) {
            handleOptimisticLockException(e);
        }
    }

    @Override
    public void rejectManyApplications(List<Long> applicationIds, User client) {
        Map<String, String> combinedResults = new LinkedHashMap<>();

        for (int i = 0; i < applicationIds.size(); i++) {
            Long applicationId = applicationIds.get(i);
            try {
                rejectApplication(applicationId, client);
                combinedResults.put("[Request " + (i + 1) + "] Rejecting application with ID " + applicationId + ": ",
                        "successful");
            } catch (ApiException e) {
                combinedResults.put("[Request " + (i + 1) + "] Rejecting application with ID " + applicationId + ": ",
                        e.getMessage());
            }
        }

        if (!combinedResults.isEmpty()) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_PARTIAL_CONTENT)
                    .message("Some rejection requests could not be processed.")
                    .errors(combinedResults)
                    .build();
        }
    }

    @Override
    public void cancelApplication(Long applicationId, User freelancer) {
        try {
            Application application = applicationService.getApplicationById(applicationId);
            validateFreelancerApplicationOwnership(application, freelancer.getId());
            if (application.getApplicationStatus() == ApplicationStatus.ACCEPTED) {
                throw ApiException.builder()
                        .status(HttpServletResponse.SC_CONFLICT)
                        .message("You cannot cancel an application that has been accepted")
                        .build();
            }
            applicationService.deleteApplication(application);

        } catch (OptimisticLockException e) {
            handleOptimisticLockException(e);
        }
    }


    // ==================== Utility Methods ====================
    private void validateClientProjectOwnership(Project project, Long clientId) {
        if (!project.isOwnedBy(clientId)) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .message("You are not authorized to perform this action on this project")
                    .build();
        }
    }

    private void validateFreelancerApplicationOwnership(Application application, Long freelancerId) {
        if (!application.isOwnedBy(freelancerId)) {
            throw ApiException.builder()
                    .status(403)
                    .message("You are not authorized to perform this action on that application")
                    .build();
        }
    }

    private void validateProjectForFreelancerSelection(Project project) {
        if (project.hasSelectedFreelancer()) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_CONFLICT)
                    .message("A freelancer is already working on this project")
                    .build();
        }
    }

    private void validateProjectForConfirmation(Project project) {
        if (!project.isProjectApproved()) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_CONFLICT)
                    .message("Cannot perform this action, project is either pending or rejected")
                    .build();
        }
    }

    private void handleOptimisticLockException(Exception e) {
        throw ApiException.builder()
                .status(HttpServletResponse.SC_CONFLICT)
                .message("The resource was modified by another user. Please refresh the page and try again.")
                .errors(Map.of("optimisticLock", e.getMessage()))
                .build();
    }

}
