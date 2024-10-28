package com.quolance.quolance_api.services;

import com.quolance.quolance_api.dtos.ApplicationDto;
import com.quolance.quolance_api.dtos.ProjectDto;

import java.util.List;

public interface FreelancerService {

    ApplicationDto submitApplication(ApplicationDto applicationDto);
    List<ApplicationDto> getMyApplications(Long freelancerId);

    List<ProjectDto> getAllAvailableProjects();
    ProjectDto getProjectById(Long id);
}