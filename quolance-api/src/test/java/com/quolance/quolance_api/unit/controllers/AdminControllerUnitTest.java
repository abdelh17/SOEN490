package com.quolance.quolance_api.unit;

import com.quolance.quolance_api.controllers.AdminController;
import com.quolance.quolance_api.dtos.project.ProjectDto;
import com.quolance.quolance_api.services.business_workflow.AdminWorkflowService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Disabled;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerUnitTest {

    @Mock
    private AdminWorkflowService adminWorkflowService;

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
        List<ProjectDto> projectList = Arrays.asList(projectDto1, projectDto2);
        Page<ProjectDto> expectedProjects = new org.springframework.data.domain.PageImpl<>(projectList);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

        when(adminWorkflowService.getAllPendingProjects(pageRequest)).thenReturn(expectedProjects);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(0, 10, "id", "desc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedProjects);
        verify(adminWorkflowService, times(1)).getAllPendingProjects(pageRequest);
    }

    @Test
    void getAllPendingProjects_ReturnsEmptyPage_WhenNoProjects() {
        Page<ProjectDto> emptyPage = new org.springframework.data.domain.PageImpl<>(List.of());
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

        when(adminWorkflowService.getAllPendingProjects(pageRequest)).thenReturn(emptyPage);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(0, 10, "id", "desc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isEmpty();
        verify(adminWorkflowService, times(1)).getAllPendingProjects(pageRequest);
    }

    @Test
    void getAllPendingProjects_WhenUnauthorized_ThrowsAccessDeniedException() {
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

        when(adminWorkflowService.getAllPendingProjects(pageRequest))
                .thenThrow(new AccessDeniedException("User is not authorized to view pending projects"));

        assertThatThrownBy(() -> adminController.getAllPendingProjects(0, 10, "id", "desc"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not authorized to view pending projects");
        verify(adminWorkflowService, times(1)).getAllPendingProjects(pageRequest);
    }

    @Test
    void getAllPendingProjects_WithCustomPageSize_ReturnsCorrectPageSize() {
        int customPageSize = 5;
        List<ProjectDto> projectList = Arrays.asList(projectDto1, projectDto2);
        Page<ProjectDto> expectedProjects = new org.springframework.data.domain.PageImpl<>(projectList);
        PageRequest pageRequest = PageRequest.of(0, customPageSize, Sort.by(Sort.Direction.DESC, "id"));

        when(adminWorkflowService.getAllPendingProjects(pageRequest)).thenReturn(expectedProjects);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(0, customPageSize, "id", "desc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedProjects);
        verify(adminWorkflowService).getAllPendingProjects(any(PageRequest.class));
        verify(adminWorkflowService).getAllPendingProjects(argThat(pr ->
                pr.getPageSize() == customPageSize
        ));
    }

    @Test
    void getAllPendingProjects_WithCustomPage_ReturnsCorrectPage() {
        int customPage = 2;
        List<ProjectDto> projectList = Arrays.asList(projectDto1, projectDto2);
        Page<ProjectDto> expectedProjects = new org.springframework.data.domain.PageImpl<>(projectList);
        PageRequest pageRequest = PageRequest.of(customPage, 10, Sort.by(Sort.Direction.DESC, "id"));

        when(adminWorkflowService.getAllPendingProjects(pageRequest)).thenReturn(expectedProjects);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(customPage, 10, "id", "desc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedProjects);
        verify(adminWorkflowService).getAllPendingProjects(any(PageRequest.class));
        verify(adminWorkflowService).getAllPendingProjects(argThat(pr ->
                pr.getPageNumber() == customPage
        ));
    }

    @Test
    void getAllPendingProjects_WithCustomSortField_ReturnsSortedResults() {
        String customSortField = "createdDate";
        List<ProjectDto> projectList = Arrays.asList(projectDto1, projectDto2);
        Page<ProjectDto> expectedProjects = new org.springframework.data.domain.PageImpl<>(projectList);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, customSortField));

        when(adminWorkflowService.getAllPendingProjects(pageRequest)).thenReturn(expectedProjects);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(0, 10, customSortField, "desc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedProjects);
        verify(adminWorkflowService).getAllPendingProjects(any(PageRequest.class));
        verify(adminWorkflowService).getAllPendingProjects(argThat(pr ->
                pr.getSort().getOrderFor(customSortField) != null
        ));
    }

    @Test
    void getAllPendingProjects_WithAscendingSort_ReturnsSortedResults() {
        List<ProjectDto> projectList = Arrays.asList(projectDto1, projectDto2);
        Page<ProjectDto> expectedProjects = new org.springframework.data.domain.PageImpl<>(projectList);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));

        when(adminWorkflowService.getAllPendingProjects(pageRequest)).thenReturn(expectedProjects);

        ResponseEntity<Page<ProjectDto>> response = adminController.getAllPendingProjects(0, 10, "id", "asc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedProjects);
        verify(adminWorkflowService).getAllPendingProjects(any(PageRequest.class));
        verify(adminWorkflowService).getAllPendingProjects(argThat(pr ->
                pr.getSort().getOrderFor("id").getDirection() == Sort.Direction.ASC
        ));
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