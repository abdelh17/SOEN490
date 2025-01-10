package com.quolance.quolance_api.services.entity_services;

import com.quolance.quolance_api.entities.Application;
import com.quolance.quolance_api.entities.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;

public interface ApplicationService {

    void saveApplication(Application application);
    void deleteApplication(Application application);
    Application getApplicationById(Long applicationId);
//    void updateApplication(Application application); Cannot update application for now (refer to: https://github.com/abdelh17/Quolance/wiki/Meeting-Minutes#iteration-5

    void updateApplicationStatus(Application application, ApplicationStatus newStatus);

    Application getApplicationByFreelancerIdAndProjectId(Long freelancerId, Long projectId);

    List<Application> getAllApplications();
    Page<Application> getAllApplicationsByFreelancerId(Long freelancerId, Pageable pageable);
    Page<Application> getAllApplicationsByProjectId(Long projectId, Pageable pageable);
}
