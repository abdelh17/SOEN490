package com.quolance.quolance_api.controllers;

import com.quolance.quolance_api.dtos.project.ProjectDto;
import com.quolance.quolance_api.services.business_workflow.AdminWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminWorkflowService adminWorkflowService;

    @Operation(
            summary = "Get all pending projects"
    )
    @GetMapping("projects/pending/all")
    public ResponseEntity<Page<ProjectDto>> getAllPendingProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(adminWorkflowService.getAllPendingProjects(pageRequest));
    }

    @Operation(
            summary = "Approve a pending project"
    )
    @PostMapping("projects/pending/{projectId}/approve")
    public ResponseEntity<String> approveProject(@PathVariable(name = "projectId") Long projectId) {
        adminWorkflowService.approveProject(projectId);
        // TODO: Notify client (using notification service for example)
        return ResponseEntity.ok("Project approved successfully");
    }

    @Operation(
            summary = "Reject a pending project"
    )
    @PostMapping("projects/pending/{projectId}/reject")
    public ResponseEntity<String> rejectProject(@PathVariable(name = "projectId") Long projectId) {
        adminWorkflowService.rejectProject(projectId);
        // TODO: Notify client + a reason of rejection (using notification service for example)
        return ResponseEntity.ok("Project rejected successfully");
    }

}
