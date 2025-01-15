package com.quolance.quolance_api.unit.controllers;

import com.quolance.quolance_api.controllers.AdminController;
import com.quolance.quolance_api.dtos.project.ProjectDto;
import com.quolance.quolance_api.dtos.PageableRequestDto;
import com.quolance.quolance_api.services.business_workflow.AdminWorkflowService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import com.quolance.quolance_api.util.PaginationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerUnitTest {

    @Mock
    private AdminWorkflowService adminWorkflowService;

    @Mock
    private PaginationUtils paginationUtils;

    @InjectMocks
    private AdminController adminController;

    private ProjectDto projectDto1;
    private ProjectDto projectDto2;

    @BeforeEach
    void setUp() {
        projectDto1 = new ProjectDto();
        projectDto1.setId(1L);
        projectDto2 = new ProjectDto();
        projectDto2.setId(2L);
    }

    @Test
    void getAllPendingProjects_ReturnsPageOfProjects() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setPage(0);
        pageableRequest.setSize(10);
        Pageable pageable = PageRequest.of(0, 10);
        when(paginationUtils.createPageable(pageableRequest)).thenReturn(pageable);

        List<ProjectDto> projectList = Arrays.asList(projectDto1, projectDto2);
        Page<ProjectDto> expectedPage = new PageImpl<>(projectList, pageable, projectList.size());
        when(adminWorkflowService.getAllPendingProjects(pageable)).thenReturn(expectedPage);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedPage);
        verify(adminWorkflowService, times(1)).getAllPendingProjects(pageable);
    }

    @Test
    void getAllPendingProjects_ReturnsEmptyPage_WhenNoProjects() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setPage(0);
        pageableRequest.setSize(10);
        Pageable pageable = PageRequest.of(0, 10);
        when(paginationUtils.createPageable(pageableRequest)).thenReturn(pageable);

        Page<ProjectDto> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(adminWorkflowService.getAllPendingProjects(pageable)).thenReturn(emptyPage);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(emptyPage);
        assertThat(response.getBody().getContent()).isEmpty();
        verify(adminWorkflowService, times(1)).getAllPendingProjects(pageable);
    }

    @Test
    void getAllPendingProjects_WhenUnauthorized_ThrowsAccessDeniedException() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setPage(0);
        pageableRequest.setSize(10);
        Pageable pageable = PageRequest.of(0, 10);
        when(paginationUtils.createPageable(pageableRequest)).thenReturn(pageable);

        when(adminWorkflowService.getAllPendingProjects(pageable))
                .thenThrow(new AccessDeniedException("User is not authorized to view pending projects"));

        assertThatThrownBy(() -> adminController.getAllPendingProjects(pageableRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not authorized to view pending projects");
        verify(adminWorkflowService, times(1)).getAllPendingProjects(pageable);
    }

    @Test
    void getAllPendingProjects_WithCustomPageSize_ReturnCorrectPageSize() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setPage(0);
        pageableRequest.setSize(5);
        Pageable customPageable = PageRequest.of(0, 5);
        when(paginationUtils.createPageable(pageableRequest)).thenReturn(customPageable);

        List<ProjectDto> projectList = Arrays.asList(projectDto1, projectDto2);
        Page<ProjectDto> expectedPage = new PageImpl<>(projectList, customPageable, projectList.size());
        when(adminWorkflowService.getAllPendingProjects(customPageable)).thenReturn(expectedPage);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(pageableRequest);

        assertThat(response.getBody().getSize()).isEqualTo(5);
        verify(adminWorkflowService).getAllPendingProjects(customPageable);
    }

    @Test
    void getAllPendingProjects_WithCustomPageNumber_ReturnsCorrectPage() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setPage(2);
        pageableRequest.setSize(10);
        Pageable customPageable = PageRequest.of(2, 10);
        when(paginationUtils.createPageable(pageableRequest)).thenReturn(customPageable);

        List<ProjectDto> projectList = Arrays.asList(projectDto1, projectDto2);
        Page<ProjectDto> expectedPage = new PageImpl<>(projectList, customPageable, 25);
        when(adminWorkflowService.getAllPendingProjects(customPageable)).thenReturn(expectedPage);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(pageableRequest);

        assertThat(response.getBody().getNumber()).isEqualTo(2);
        assertThat(response.getBody().getTotalPages()).isEqualTo(3);
        verify(adminWorkflowService).getAllPendingProjects(customPageable);
    }

    @Test
    void getAllPendingProjects_WithInvalidPageSize_UsesDefaultPageSize() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setPage(0);
        pageableRequest.setSize(-1);
        Pageable pageable = PageRequest.of(0, 10);
        when(paginationUtils.createPageable(pageableRequest)).thenReturn(pageable);

        List<ProjectDto> projectList = Arrays.asList(projectDto1, projectDto2);
        Page<ProjectDto> expectedPage = new PageImpl<>(projectList, pageable, projectList.size());
        when(adminWorkflowService.getAllPendingProjects(pageable)).thenReturn(expectedPage);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(pageableRequest);

        assertThat(response.getBody().getSize()).isEqualTo(10);
        verify(adminWorkflowService).getAllPendingProjects(pageable);
    }

    @Test
    void approveProject_ReturnsSuccessMessage() {
        Long projectId = 1L;
        doNothing().when(adminWorkflowService).approveProject(projectId);

        ResponseEntity<String> response = adminController.approveProject(projectId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Project approved successfully");
        verify(adminWorkflowService, times(1)).approveProject(projectId);
    }

    @Test
    void approveProject_WhenProjectNotFound_ThrowsApiException() {
        Long projectId = 999L;
        doThrow(new ApiException("Project not found")).when(adminWorkflowService).approveProject(projectId);

        assertThatThrownBy(() -> adminController.approveProject(projectId))
                .isInstanceOf(ApiException.class)
                .hasMessage("Project not found");
        verify(adminWorkflowService, times(1)).approveProject(projectId);
    }

    @Test
    void approveProject_WhenUnauthorized_ThrowsAccessDeniedException() {
        Long projectId = 1L;
        doThrow(new AccessDeniedException("User is not authorized to approve projects"))
                .when(adminWorkflowService).approveProject(projectId);

        assertThatThrownBy(() -> adminController.approveProject(projectId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not authorized to approve projects");
        verify(adminWorkflowService, times(1)).approveProject(projectId);
    }

    @Test
    void rejectProject_ReturnsSuccessMessage() {
        Long projectId = 1L;
        doNothing().when(adminWorkflowService).rejectProject(projectId);

        ResponseEntity<String> response = adminController.rejectProject(projectId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Project rejected successfully");
        verify(adminWorkflowService, times(1)).rejectProject(projectId);
    }

    @Test
    void rejectProject_WhenProjectNotFound_ThrowsApiException() {
        Long projectId = 999L;
        doThrow(new ApiException("Project not found")).when(adminWorkflowService).rejectProject(projectId);

        assertThatThrownBy(() -> adminController.rejectProject(projectId))
                .isInstanceOf(ApiException.class)
                .hasMessage("Project not found");
        verify(adminWorkflowService, times(1)).rejectProject(projectId);
    }

    @Test
    void rejectProject_WhenUnauthorized_ThrowsAccessDeniedException() {
        Long projectId = 1L;
        doThrow(new AccessDeniedException("User is not authorized to reject projects"))
                .when(adminWorkflowService).rejectProject(projectId);

        assertThatThrownBy(() -> adminController.rejectProject(projectId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not authorized to reject projects");
        verify(adminWorkflowService, times(1)).rejectProject(projectId);
    }
}