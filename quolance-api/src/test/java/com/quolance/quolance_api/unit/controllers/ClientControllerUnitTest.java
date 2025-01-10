package com.quolance.quolance_api.unit.controllers;

import com.quolance.quolance_api.controllers.ClientController;
import com.quolance.quolance_api.dtos.application.ApplicationDto;
import com.quolance.quolance_api.dtos.project.ProjectCreateDto;
import com.quolance.quolance_api.dtos.project.ProjectDto;
import com.quolance.quolance_api.dtos.project.ProjectUpdateDto;
import com.quolance.quolance_api.dtos.PageableRequestDto;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.*;
import com.quolance.quolance_api.services.business_workflow.ApplicationProcessWorkflow;
import com.quolance.quolance_api.services.business_workflow.ClientWorkflowService;
import com.quolance.quolance_api.util.SecurityUtil;
import com.quolance.quolance_api.util.exceptions.ApiException;
import com.quolance.quolance_api.util.PaginationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerUnitTest {

    @Mock
    private ClientWorkflowService clientWorkflowService;

    @Mock
    private ApplicationProcessWorkflow applicationProcessWorkflow;

    @Mock
    private PaginationUtils paginationUtils;

    @InjectMocks
    private ClientController clientController;

    private User mockClient;
    private ProjectCreateDto projectCreateDto;
    private ProjectDto projectDto;
    private ProjectUpdateDto projectUpdateDto;

    @BeforeEach
    void setUp() {
        mockClient = new User();
        mockClient.setId(1L);
        mockClient.setEmail("client@test.com");
        mockClient.setRole(Role.CLIENT);

        projectCreateDto = ProjectCreateDto.builder()
                .title("Test Project")
                .description("Test Description")
                .category(ProjectCategory.APP_DEVELOPMENT)
                .priceRange(PriceRange.LESS_500)
                .experienceLevel(FreelancerExperienceLevel.JUNIOR)
                .expectedDeliveryTime(ExpectedDeliveryTime.FLEXIBLE)
                .expirationDate(LocalDate.now().plusDays(7))
                .build();

        projectDto = ProjectDto.builder()
                .id(1L)
                .title("Test Project")
                .description("Test Description")
                .category(ProjectCategory.APP_DEVELOPMENT)
                .priceRange(PriceRange.LESS_500)
                .experienceLevel(FreelancerExperienceLevel.JUNIOR)
                .expectedDeliveryTime(ExpectedDeliveryTime.FLEXIBLE)
                .build();

        projectUpdateDto = ProjectUpdateDto.builder()
                .title("Updated Project")
                .description("Updated Description")
                .category(ProjectCategory.CONTENT_WRITING)
                .priceRange(PriceRange.MORE_10000)
                .experienceLevel(FreelancerExperienceLevel.EXPERT)
                .expectedDeliveryTime(ExpectedDeliveryTime.IMMEDIATELY)
                .build();
    }

    @Test
    void createProject_ReturnsSuccessMessage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            doNothing().when(clientWorkflowService).createProject(eq(projectCreateDto), any(User.class));

            ResponseEntity<String> response = clientController.createProject(projectCreateDto);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("Project created successfully");
            verify(clientWorkflowService).createProject(eq(projectCreateDto), eq(mockClient));
        }
    }

    @Test
    void createProject_WithNullProjectDto_ThrowsException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            doThrow(new ApiException("Project details cannot be null"))
                    .when(clientWorkflowService).createProject(eq(null), any(User.class));

            assertThatThrownBy(() -> clientController.createProject(null))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Project details cannot be null");
        }
    }

    @Test
    void createProject_WhenUnauthorized_ThrowsAccessDeniedException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            doThrow(new AccessDeniedException("User not authorized to create projects"))
                    .when(clientWorkflowService).createProject(eq(projectCreateDto), any(User.class));

            assertThatThrownBy(() -> clientController.createProject(projectCreateDto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("User not authorized to create projects");
            verify(clientWorkflowService).createProject(eq(projectCreateDto), eq(mockClient));
        }
    }

    @Test
    void getProject_ReturnsProjectDto() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(clientWorkflowService.getProject(eq(1L), any(User.class))).thenReturn(projectDto);

            ResponseEntity<ProjectDto> response = clientController.getProject(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(projectDto);
            verify(clientWorkflowService).getProject(eq(1L), eq(mockClient));
        }
    }

    @Test
    void getProject_WithInvalidId_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(clientWorkflowService.getProject(eq(-1L), any(User.class)))
                    .thenThrow(new ApiException("Invalid project ID"));

            assertThatThrownBy(() -> clientController.getProject(-1L))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Invalid project ID");
            verify(clientWorkflowService).getProject(eq(-1L), eq(mockClient));
        }
    }

    @Test
    void updateProject_ReturnsUpdatedProject() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(clientWorkflowService.updateProject(eq(1L), eq(projectUpdateDto), any(User.class)))
                    .thenReturn(projectDto);

            ResponseEntity<ProjectDto> response = clientController.updateProject(1L, projectUpdateDto);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(projectDto);
            verify(clientWorkflowService).updateProject(eq(1L), eq(projectUpdateDto), eq(mockClient));
        }
    }

    @Test
    void updateProject_WhenUnauthorized_ThrowsAccessDeniedException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(clientWorkflowService.updateProject(eq(1L), eq(projectUpdateDto), any(User.class)))
                    .thenThrow(new AccessDeniedException("User not authorized to update this project"));

            assertThatThrownBy(() -> clientController.updateProject(1L, projectUpdateDto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("User not authorized to update this project");
            verify(clientWorkflowService).updateProject(eq(1L), eq(projectUpdateDto), eq(mockClient));
        }
    }

    @Test
    void deleteProject_ReturnsSuccessMessage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            doNothing().when(clientWorkflowService).deleteProject(eq(1L), any(User.class));

            ResponseEntity<String> response = clientController.deleteProject(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("Project deleted successfully");
            verify(clientWorkflowService).deleteProject(eq(1L), eq(mockClient));
        }
    }

    @Test
    void deleteProject_WhenProjectNotFound_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            doThrow(new ApiException("Project not found"))
                    .when(clientWorkflowService).deleteProject(eq(999L), any(User.class));

            assertThatThrownBy(() -> clientController.deleteProject(999L))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Project not found");
            verify(clientWorkflowService).deleteProject(eq(999L), eq(mockClient));
        }
    }

    @Test
    void getAllClientProjects_ReturnsProjectPage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setSize(10);
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            Page<ProjectDto> projectPage = new PageImpl<>(List.of(projectDto));

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(pageRequest);
            when(clientWorkflowService.getAllClientProjects(eq(mockClient), eq(pageRequest)))
                    .thenReturn(projectPage);

            ResponseEntity<Page<ProjectDto>> response = clientController.getAllClientProjects(pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0)).isEqualTo(projectDto);
            verify(clientWorkflowService).getAllClientProjects(eq(mockClient), eq(pageRequest));
        }
    }

    @Test
    void getAllClientProjects_ReturnsEmptyPage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            Page<ProjectDto> emptyPage = new PageImpl<>(List.of());

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(pageRequest);
            when(clientWorkflowService.getAllClientProjects(eq(mockClient), eq(pageRequest)))
                    .thenReturn(emptyPage);

            ResponseEntity<Page<ProjectDto>> response = clientController.getAllClientProjects(pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).isEmpty();
            verify(clientWorkflowService).getAllClientProjects(eq(mockClient), eq(pageRequest));
        }
    }

    @Test
    void getAllClientProjects_WithCustomPageNumber_ReturnsCorrectPage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setPage(2);
            pageableRequest.setSize(10);
            PageRequest expectedPageRequest = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "id"));
            Page<ProjectDto> projectPage = new PageImpl<>(List.of(projectDto));

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(expectedPageRequest);
            when(clientWorkflowService.getAllClientProjects(eq(mockClient), eq(expectedPageRequest)))
                    .thenReturn(projectPage);

            ResponseEntity<Page<ProjectDto>> response = clientController.getAllClientProjects(pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(projectPage);
            verify(paginationUtils).createPageable(argThat(pr ->
                    pr.getPage() == 2
            ));
        }
    }

    @Test
    void getAllClientProjects_WithCustomSortField_ReturnsSortedResults() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setSortBy("createdDate");
            PageRequest expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdDate"));
            Page<ProjectDto> projectPage = new PageImpl<>(List.of(projectDto));

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(expectedPageRequest);
            when(clientWorkflowService.getAllClientProjects(eq(mockClient), eq(expectedPageRequest)))
                    .thenReturn(projectPage);

            ResponseEntity<Page<ProjectDto>> response = clientController.getAllClientProjects(pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(projectPage);
            verify(paginationUtils).createPageable(argThat(pr ->
                    pr.getSortBy().equals("createdDate")
            ));
        }
    }

    @Test
    void getAllClientProjects_WithInvalidSortDirection_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setSortDirection("invalid");

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest)))
                    .thenThrow(new ApiException("Sort direction must be either 'asc' or 'desc'"));

            assertThatThrownBy(() -> clientController.getAllClientProjects(pageableRequest))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Sort direction must be either 'asc' or 'desc'");
        }
    }

    @Test
    void getAllApplicationsToProject_ReturnsApplicationPage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            ApplicationDto applicationDto = new ApplicationDto();
            Page<ApplicationDto> applicationPage = new PageImpl<>(List.of(applicationDto));

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(pageRequest);
            when(clientWorkflowService.getAllApplicationsToProject(eq(1L), eq(mockClient), eq(pageRequest)))
                    .thenReturn(applicationPage);

            ResponseEntity<Page<ApplicationDto>> response = clientController.getAllApplicationsToProject(1L, pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
            verify(clientWorkflowService).getAllApplicationsToProject(eq(1L), eq(mockClient), eq(pageRequest));
        }
    }

    @Test
    void getAllApplicationsToProject_WhenInvalidPageSize_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setSize(101);

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest)))
                    .thenThrow(new ApiException("Page size must not be greater than 100"));

            assertThatThrownBy(() -> clientController.getAllApplicationsToProject(1L, pageableRequest))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Page size must not be greater than 100");
        }
    }

    @Test
    void getAllApplicationsToProject_WithCustomPageSize_ReturnsCorrectPageSize() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setSize(5);
            PageRequest expectedPageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));
            ApplicationDto applicationDto = new ApplicationDto();
            Page<ApplicationDto> applicationPage = new PageImpl<>(List.of(applicationDto));

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(expectedPageRequest);
            when(clientWorkflowService.getAllApplicationsToProject(eq(1L), eq(mockClient), eq(expectedPageRequest)))
                    .thenReturn(applicationPage);

            ResponseEntity<Page<ApplicationDto>> response = clientController.getAllApplicationsToProject(1L, pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(applicationPage);
            verify(paginationUtils).createPageable(argThat(pr ->
                    pr.getSize() == 5
            ));
        }
    }

    @Test
    void getAllApplicationsToProject_WithAscendingSortOrder_ReturnsSortedResults() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setSortDirection("asc");
            PageRequest expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
            ApplicationDto applicationDto = new ApplicationDto();
            Page<ApplicationDto> applicationPage = new PageImpl<>(List.of(applicationDto));

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(expectedPageRequest);
            when(clientWorkflowService.getAllApplicationsToProject(eq(1L), eq(mockClient), eq(expectedPageRequest)))
                    .thenReturn(applicationPage);

            ResponseEntity<Page<ApplicationDto>> response = clientController.getAllApplicationsToProject(1L, pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(applicationPage);
            verify(paginationUtils).createPageable(argThat(pr ->
                    pr.getSortDirection().equals("asc")
            ));
        }
    }

    @Test
    void getAllApplicationsToProject_WithNegativePageNumber_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setPage(-1);

            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            when(paginationUtils.createPageable(eq(pageableRequest)))
                    .thenThrow(new ApiException("Page number cannot be negative"));

            assertThatThrownBy(() -> clientController.getAllApplicationsToProject(1L, pageableRequest))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Page number cannot be negative");
        }
    }

    @Test
    void selectFreelancer_ReturnsSuccessMessage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            doNothing().when(applicationProcessWorkflow).selectFreelancer(eq(1L), any(User.class));

            ResponseEntity<String> response = clientController.selectFreelancer(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("Freelancer selected successfully");
            verify(applicationProcessWorkflow).selectFreelancer(eq(1L), eq(mockClient));
        }
    }

    @Test
    void selectFreelancer_WhenApplicationNotFound_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            doThrow(new ApiException("Application not found"))
                    .when(applicationProcessWorkflow).selectFreelancer(eq(999L), any(User.class));

            assertThatThrownBy(() -> clientController.selectFreelancer(999L))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Application not found");
            verify(applicationProcessWorkflow).selectFreelancer(eq(999L), eq(mockClient));
        }
    }

    @Test
    void rejectFreelancer_ReturnsSuccessMessage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            doNothing().when(applicationProcessWorkflow).rejectApplication(eq(1L), any(User.class));

            ResponseEntity<String> response = clientController.rejectFreelancer(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("Freelancer rejected successfully");
            verify(applicationProcessWorkflow).rejectApplication(eq(1L), eq(mockClient));
        }
    }

    @Test
    void rejectManyFreelancers_ReturnsSuccessMessage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            List<Long> applicationIds = Arrays.asList(1L, 2L);
            doNothing().when(applicationProcessWorkflow).rejectManyApplications(eq(applicationIds), any(User.class));

            ResponseEntity<String> response = clientController.rejectManyFreelancers(applicationIds);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("All selected freelancers rejected successfully");
            verify(applicationProcessWorkflow).rejectManyApplications(eq(applicationIds), eq(mockClient));
        }
    }

    @Test
    void rejectManyFreelancers_WithEmptyList_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockClient);
            List<Long> emptyList = List.of();
            doThrow(new ApiException("Application IDs list cannot be empty"))
                    .when(applicationProcessWorkflow).rejectManyApplications(eq(emptyList), any(User.class));

            assertThatThrownBy(() -> clientController.rejectManyFreelancers(emptyList))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Application IDs list cannot be empty");
            verify(applicationProcessWorkflow).rejectManyApplications(eq(emptyList), eq(mockClient));
        }
    }
}