package com.quolance.quolance_api.controllers;

import com.quolance.quolance_api.dtos.profile.FreelancerProfileDto;
import com.quolance.quolance_api.dtos.project.ProjectPublicDto;
import com.quolance.quolance_api.dtos.PageableRequestDto;
import com.quolance.quolance_api.dtos.PageResponseDto;
import com.quolance.quolance_api.dtos.project.ProjectFilterDto;
import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.services.business_workflow.FreelancerWorkflowService;
import com.quolance.quolance_api.services.entity_services.ProjectService;
import com.quolance.quolance_api.util.SecurityUtil;
import com.quolance.quolance_api.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    // FIXME: Reusing FreelancerWorkflowService here since both public and freelancers can view projects THE SAME WAY.
    //  In the future, separate this logic to avoid mixing responsibilities in the service.

    private final FreelancerWorkflowService freelancerWorkflowService;
    private final PaginationUtils paginationUtils;

    @GetMapping("/projects/all")
    @Operation(
            summary = "View all available projects.",
            description = "View all projects (as a guest) that are open or closed and still in the visibility of the public."
    )
    public ResponseEntity<PageResponseDto<ProjectPublicDto>> getAllAvailableProjects(
            @Valid PageableRequestDto pageableRequest,
            @Valid ProjectFilterDto filters) {
        Page<ProjectPublicDto> projectsPage = freelancerWorkflowService.getAllAvailableProjects(
                pageableRequest.toPageRequest(),
                filters
        );
        return ResponseEntity.ok(new PageResponseDto<>(projectsPage));
    }

    @GetMapping("/projects/{projectId}")
    @Operation(
            summary = "View a project.",
            description = "View a project (as a guest) by passing the project id."
    )
    public ResponseEntity<ProjectPublicDto> getProjectById(@PathVariable(name = "projectId") Long projectId) {
        ProjectPublicDto project = freelancerWorkflowService.getProject(projectId);
        return ResponseEntity.ok(project);
    }

    @GetMapping("freelancer/profile/{username}")
    @Operation(
            summary = "View freelancer profile"
    )
    public ResponseEntity<FreelancerProfileDto> getFreelancerProfile(@PathVariable(name = "username") String username) {
        FreelancerProfileDto profile = freelancerWorkflowService.getFreelancerProfile(username);
        return ResponseEntity.ok(profile);
    }

}

