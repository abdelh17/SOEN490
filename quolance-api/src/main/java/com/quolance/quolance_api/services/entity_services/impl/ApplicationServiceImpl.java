package com.quolance.quolance_api.services.entity_services.impl;
import com.quolance.quolance_api.entities.Application;
import com.quolance.quolance_api.entities.enums.ApplicationStatus;
import com.quolance.quolance_api.repositories.ApplicationRepository;
import com.quolance.quolance_api.services.entity_services.ApplicationService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;


    @Override
    public void saveApplication(Application application) {
        applicationRepository.save(application);
    }

    @Override
    public void deleteApplication(Application application) {
        applicationRepository.delete(application);
    }

    @Override
    public Application getApplicationById(Long applicationId) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(() ->
                ApiException.builder()
                        .status(HttpServletResponse.SC_NOT_FOUND)
                        .message("No Application found with ID: " + applicationId)
                        .build());
        return application;
    }

    @Override
    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    @Override
    public Application getApplicationByFreelancerIdAndProjectId(Long freelancerId, Long projectId) {
        return applicationRepository.findApplicationByFreelancerIdAndProjectId(freelancerId, projectId);
    }

    @Override
    public Page<Application> getAllApplicationsByFreelancerId(Long freelancerId, Pageable pageable) {
        return applicationRepository.findApplicationsByFreelancerId(freelancerId, pageable);
    }

    @Override
    public Page<Application> getAllApplicationsByProjectId(Long projectId, Pageable pageable) {
        return applicationRepository.findApplicationsByProjectId(projectId, pageable);
    }

    @Override
    public void updateApplicationStatus(Application application, ApplicationStatus newStatus) {

        // Rule: Cannot update status if it is already the newStatus
        if (application.getApplicationStatus() == newStatus) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_CONFLICT)
                    .message("Application is already in status: " + newStatus)
                    .build();
        }

        // Rule: Cannot update status if it is REJECTED
        if (application.getApplicationStatus() == ApplicationStatus.REJECTED) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .message("Application is rejected and cannot be updated.")
                    .build();
        }

        // Rule: Cannot update status if it is ACCEPTED
        if (application.getApplicationStatus() == ApplicationStatus.ACCEPTED) {
            throw ApiException.builder()
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .message("Application is accepted and cannot be updated.")
                    .build();
        }

        // Update logic based on the new status
        switch (newStatus) {
            case ACCEPTED:
                if (application.getApplicationStatus() == ApplicationStatus.APPLIED) {
                    application.setApplicationStatus(ApplicationStatus.ACCEPTED);
                    applicationRepository.save(application);
                } else {
                    throw ApiException.builder()
                            .status(HttpServletResponse.SC_BAD_REQUEST)
                            .message("Invalid status transition to ACCEPTED.")
                            .build();
                }
                break;

            case REJECTED:
                if (application.getApplicationStatus() == ApplicationStatus.APPLIED) {
                    application.setApplicationStatus(ApplicationStatus.REJECTED);
                    applicationRepository.save(application);
                } else {
                    throw ApiException.builder()
                            .status(HttpServletResponse.SC_BAD_REQUEST)
                            .message("Invalid status transition to REJECTED.")
                            .build();
                }
                break;

            default:
                throw ApiException.builder()
                        .status(HttpServletResponse.SC_BAD_REQUEST)
                        .message("Invalid status update request.")
                        .build();
        }
    }

}
