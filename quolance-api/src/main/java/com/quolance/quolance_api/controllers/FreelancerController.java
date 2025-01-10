package com.quolance.quolance_api.controllers;

import com.quolance.quolance_api.dtos.PageableRequestDto;
import com.quolance.quolance_api.dtos.application.ApplicationCreateDto;
import com.quolance.quolance_api.dtos.application.ApplicationDto;
import com.quolance.quolance_api.dtos.profile.FreelancerProfileDto;
import com.quolance.quolance_api.dtos.profile.UpdateFreelancerProfileDto;
import com.quolance.quolance_api.dtos.project.ProjectPublicDto;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.services.business_workflow.ApplicationProcessWorkflow;
import com.quolance.quolance_api.services.business_workflow.FreelancerWorkflowService;
import com.quolance.quolance_api.util.PaginationUtils;
import com.quolance.quolance_api.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/freelancer")
@RequiredArgsConstructor
public class FreelancerController {

    private final FreelancerWorkflowService freelancerWorkflowService;
    private final ApplicationProcessWorkflow applicationProcessWorkflow;
    private final PaginationUtils paginationUtils;

    @PostMapping("/submit-application")
    @Operation(
            summary = "Create a new application on a project.",
            description = "Create a new application on a project by passing an ApplicationCreateDto"
    )
    public void applyToProject(@RequestBody ApplicationCreateDto applicationCreateDto) {
        User freelancer = SecurityUtil.getAuthenticatedUser();
        freelancerWorkflowService.submitApplication(applicationCreateDto, freelancer);
    }

    @GetMapping("/applications/{applicationId}")
    @Operation(
            summary = "View an application.",
            description = "View an application by passing the application id."
    )
    public ResponseEntity<ApplicationDto> getApplication(@PathVariable(name = "applicationId") Long applicationId) {
        User freelancer = SecurityUtil.getAuthenticatedUser();
        ApplicationDto application = freelancerWorkflowService.getApplication(applicationId, freelancer);
        return ResponseEntity.ok(application);
    }

    @DeleteMapping("/applications/{applicationId}")
    @Operation(
            summary = "Delete an application.",
            description = "Delete an application by passing the application id."
    )
    public ResponseEntity<String> deleteApplication(@PathVariable(name = "applicationId") Long applicationId) {
        User freelancer = SecurityUtil.getAuthenticatedUser();
        applicationProcessWorkflow.cancelApplication(applicationId, freelancer);
        return ResponseEntity.ok("Application deleted successfully.");
    }

    @GetMapping("/applications/all")
    @Operation(
            summary = "View all the application of a freelancer.",
            description = " "
    )
    public ResponseEntity<Page<ApplicationDto>> getAllFreelancerApplications(
            @Valid PageableRequestDto pageableRequest) {
        User freelancer = SecurityUtil.getAuthenticatedUser();
        Page<ApplicationDto> applications = freelancerWorkflowService.getAllFreelancerApplications(
                freelancer,
                paginationUtils.createPageable(pageableRequest)
                );
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/projects/all")
    @Operation(
            summary = "View all available projects.",
            description = "View all projects that are open or closed and still in the visibility of the freelancer."
    )
    public ResponseEntity<Page<ProjectPublicDto>> getAllAvailableProjects(
            @Valid PageableRequestDto pageableRequest) {
            Page<ProjectPublicDto> availableProjectsPage = freelancerWorkflowService.getAllAvailableProjects(
                    paginationUtils.createPageable(pageableRequest)
            );
        return ResponseEntity.ok(availableProjectsPage);
    }

    @GetMapping("/projects/{projectId}")
    @Operation(
            summary = "View a project.",
            description = "View a project by passing the project id."
    )
    public ResponseEntity<ProjectPublicDto> getProjectById(@PathVariable(name = "projectId") Long projectId) {
        ProjectPublicDto project = freelancerWorkflowService.getProject(projectId);
        return ResponseEntity.ok(project);
    }

    @PutMapping("/profile")
    @Operation(
            summary = "Update freelancer profile",
            description = "Update freelancer profile information including personal details and preferences"
    )
    public ResponseEntity<String> updateFreelancerProfile(@RequestBody UpdateFreelancerProfileDto updateFreelancerProfileDto) {
        User freelancer = SecurityUtil.getAuthenticatedUser();
        freelancerWorkflowService.updateFreelancerProfile(updateFreelancerProfileDto, freelancer);
        return ResponseEntity.ok("Profile updated successfully");
    }

    @PostMapping("/profile/picture")
    @Operation(
            summary = "Upload profile picture",
            description = "Upload a profile picture for the authenticated freelancer"
    )
    public ResponseEntity<String> uploadProfilePicture(@RequestParam("photo") MultipartFile photo) {
        User freelancer = SecurityUtil.getAuthenticatedUser();
        freelancerWorkflowService.uploadProfilePicture(photo, freelancer);
        return ResponseEntity.ok("Profile picture uploaded successfully");
    }

}