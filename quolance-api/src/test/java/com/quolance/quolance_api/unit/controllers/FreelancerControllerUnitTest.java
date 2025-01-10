package com.quolance.quolance_api.unit;

import com.quolance.quolance_api.controllers.FreelancerController;
import com.quolance.quolance_api.dtos.PageableRequestDto;
import com.quolance.quolance_api.dtos.application.ApplicationCreateDto;
import com.quolance.quolance_api.dtos.application.ApplicationDto;
import com.quolance.quolance_api.dtos.project.ProjectPublicDto;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ApplicationStatus;
import com.quolance.quolance_api.entities.enums.Role;
import com.quolance.quolance_api.services.business_workflow.ApplicationProcessWorkflow;
import com.quolance.quolance_api.services.business_workflow.FreelancerWorkflowService;
import com.quolance.quolance_api.util.PaginationUtils;
import com.quolance.quolance_api.util.SecurityUtil;
import com.quolance.quolance_api.util.exceptions.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreelancerControllerUnitTest {

    @Mock
    private FreelancerWorkflowService freelancerWorkflowService;

    @Mock
    private ApplicationProcessWorkflow applicationProcessWorkflow;

    @InjectMocks
    private FreelancerController freelancerController;

    @Mock
    private PaginationUtils paginationUtils;

    private User mockFreelancer;
    private ApplicationCreateDto applicationCreateDto;
    private ApplicationDto applicationDto;
    private ProjectPublicDto projectPublicDto;

    @BeforeEach
    void setUp() {
        mockFreelancer = new User();
        mockFreelancer.setId(1L);
        mockFreelancer.setEmail("freelancer@test.com");
        mockFreelancer.setRole(Role.FREELANCER);

        applicationCreateDto = new ApplicationCreateDto(1L);

        applicationDto = ApplicationDto.builder()
                .id(1L)
                .freelancerId(1L)
                .projectId(1L)
                .status(ApplicationStatus.APPLIED)
                .build();

        projectPublicDto = new ProjectPublicDto();
        projectPublicDto.setId(1L);
        projectPublicDto.setTitle("Test Project");

        lenient().when(paginationUtils.createPageable(any(PageableRequestDto.class))).thenReturn(Pageable.unpaged());

    }

    @Test
    void applyToProject_CallsService() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            doNothing().when(freelancerWorkflowService).submitApplication(eq(applicationCreateDto), any(User.class));

            freelancerController.applyToProject(applicationCreateDto);

            verify(freelancerWorkflowService).submitApplication(eq(applicationCreateDto), eq(mockFreelancer));
        }
    }

    @Test
    void applyToProject_WithNullDto_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            doThrow(new ApiException("Application details cannot be null"))
                    .when(freelancerWorkflowService).submitApplication(eq(null), any(User.class));

            assertThatThrownBy(() -> freelancerController.applyToProject(null))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Application details cannot be null");
        }
    }

    @Test
    void applyToProject_WhenUnauthorized_ThrowsAccessDeniedException() {

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            doThrow(new AccessDeniedException("User not authorized to submit applications"))
                    .when(freelancerWorkflowService).submitApplication(eq(applicationCreateDto), any(User.class));

            assertThatThrownBy(() -> freelancerController.applyToProject(applicationCreateDto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("User not authorized to submit applications");
        }
    }

    @Test
    void getApplication_ReturnsApplicationDto() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            when(freelancerWorkflowService.getApplication(eq(1L), any(User.class))).thenReturn(applicationDto);

            ResponseEntity<ApplicationDto> response = freelancerController.getApplication(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(applicationDto);
            assertThat(response.getBody().getStatus()).isEqualTo(ApplicationStatus.APPLIED);
            verify(freelancerWorkflowService).getApplication(eq(1L), eq(mockFreelancer));
        }
    }

    @Test
    void getApplication_WithInvalidId_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            when(freelancerWorkflowService.getApplication(eq(-1L), any(User.class)))
                    .thenThrow(new ApiException("Invalid application ID"));

            assertThatThrownBy(() -> freelancerController.getApplication(-1L))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Invalid application ID");
        }
    }

    @Test
    void deleteApplication_ReturnsSuccessMessage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            doNothing().when(applicationProcessWorkflow).cancelApplication(eq(1L), any(User.class));

            ResponseEntity<String> response = freelancerController.deleteApplication(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("Application deleted successfully.");
            verify(applicationProcessWorkflow).cancelApplication(eq(1L), eq(mockFreelancer));
        }
    }

    @Test
    void deleteApplication_WithInvalidId_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            doThrow(new ApiException("Application not found"))
                    .when(applicationProcessWorkflow).cancelApplication(eq(999L), any(User.class));

            assertThatThrownBy(() -> freelancerController.deleteApplication(999L))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Application not found");
        }
    }

    @Test
    void getAllFreelancerApplications_ReturnsApplicationPage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            PageableRequestDto pageableRequest = new PageableRequestDto();
            Page<ApplicationDto> applicationsPage = new PageImpl<>(List.of(applicationDto));

            when(freelancerWorkflowService.getAllFreelancerApplications(eq(mockFreelancer), any(Pageable.class)))
                    .thenReturn(applicationsPage);

            ResponseEntity<Page<ApplicationDto>> response = freelancerController.getAllFreelancerApplications(pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0)).isEqualTo(applicationDto);
            assertThat(response.getBody().getContent().get(0).getStatus()).isEqualTo(ApplicationStatus.APPLIED);
            verify(freelancerWorkflowService).getAllFreelancerApplications(eq(mockFreelancer), any(Pageable.class));
        }
    }

    @Test
    void getAllFreelancerApplications_ReturnsEmptyPage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            PageableRequestDto pageableRequest = new PageableRequestDto();
            Page<ApplicationDto> emptyPage = Page.empty();

            when(freelancerWorkflowService.getAllFreelancerApplications(eq(mockFreelancer), any(Pageable.class)))
                    .thenReturn(emptyPage);

            ResponseEntity<Page<ApplicationDto>> response = freelancerController.getAllFreelancerApplications(pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).isEmpty();
            verify(freelancerWorkflowService).getAllFreelancerApplications(eq(mockFreelancer), any(Pageable.class));
        }
    }

    @Test
    void getAllFreelancerApplications_WithInvalidPageSize_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);
            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setSize(101);

            when(paginationUtils.createPageable(eq(pageableRequest)))
                    .thenThrow(new ApiException("Page size must not be greater than 100"));

            assertThatThrownBy(() -> freelancerController.getAllFreelancerApplications(pageableRequest))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Page size must not be greater than 100");
        }
    }

    @Test
    void getAllFreelancerApplications_WithCustomPage_ReturnsCorrectPage() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);

            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setPage(2);
            Pageable customPageable = mock(Pageable.class);
            when(customPageable.getPageNumber()).thenReturn(2);

            when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(customPageable);
            when(freelancerWorkflowService.getAllFreelancerApplications(eq(mockFreelancer), eq(customPageable)))
                    .thenReturn(new PageImpl<>(List.of(applicationDto)));

            ResponseEntity<Page<ApplicationDto>> response = freelancerController.getAllFreelancerApplications(pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(paginationUtils).createPageable(argThat(pr ->
                    pr.getPage() == 2
            ));
            verify(freelancerWorkflowService).getAllFreelancerApplications(
                    eq(mockFreelancer),
                    argThat(p -> p.getPageNumber() == 2)
            );
        }
    }

    @Test
    void getAllFreelancerApplications_WithCustomSortDirection_ReturnsSortedResults() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);

            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setSortDirection("asc");
            Pageable customPageable = mock(Pageable.class);

            when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(customPageable);
            when(freelancerWorkflowService.getAllFreelancerApplications(eq(mockFreelancer), eq(customPageable)))
                    .thenReturn(new PageImpl<>(List.of(applicationDto)));

            ResponseEntity<Page<ApplicationDto>> response = freelancerController.getAllFreelancerApplications(pageableRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(paginationUtils).createPageable(argThat(pr ->
                    pr.getSortDirection().equals("asc")
            ));
        }
    }

    @Test
    void getAllFreelancerApplications_WithZeroPageSize_ThrowsApiException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockFreelancer);

            PageableRequestDto pageableRequest = new PageableRequestDto();
            pageableRequest.setSize(0);

            when(paginationUtils.createPageable(eq(pageableRequest)))
                    .thenThrow(new ApiException("Page size must be greater than zero"));

            assertThatThrownBy(() -> freelancerController.getAllFreelancerApplications(pageableRequest))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Page size must be greater than zero");
        }
    }

    @Test
    void getAllAvailableProjects_ReturnsProjectPage() {
        Page<ProjectPublicDto> projectPage = new PageImpl<>(Collections.singletonList(projectPublicDto));

        when(freelancerWorkflowService.getAllAvailableProjects(any(Pageable.class))).thenReturn(projectPage);

        PageableRequestDto pageableRequest = new PageableRequestDto();
        ResponseEntity<Page<ProjectPublicDto>> response = freelancerController.getAllAvailableProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0)).isEqualTo(projectPublicDto);
        verify(freelancerWorkflowService).getAllAvailableProjects(any(Pageable.class));
    }

    @Test
    void getAllAvailableProjects_ReturnsEmptyPage() {

        Page<ProjectPublicDto> emptyPage = Page.empty();

        when(freelancerWorkflowService.getAllAvailableProjects(any(Pageable.class))).thenReturn(emptyPage);

        PageableRequestDto pageableRequest = new PageableRequestDto();
        ResponseEntity<Page<ProjectPublicDto>> response = freelancerController.getAllAvailableProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
        verify(freelancerWorkflowService).getAllAvailableProjects(any(Pageable.class));
    }

    @Test
    void getAllAvailableProjects_WithCustomPageSize_ReturnsCorrectPageSize() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setSize(5);
        Pageable customPageable = mock(Pageable.class);
        when(customPageable.getPageSize()).thenReturn(5);

        when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(customPageable);
        when(freelancerWorkflowService.getAllAvailableProjects(eq(customPageable)))
                .thenReturn(new PageImpl<>(List.of(projectPublicDto)));

        ResponseEntity<Page<ProjectPublicDto>> response = freelancerController.getAllAvailableProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paginationUtils).createPageable(argThat(pr ->
                pr.getSize() == 5
        ));
        verify(freelancerWorkflowService).getAllAvailableProjects(argThat(p ->
                p.getPageSize() == 5
        ));
    }

    @Test
    void getAllAvailableProjects_WithCustomSortField_ReturnsSortedResults() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setSortBy("createdDate");
        pageableRequest.setSortDirection("asc");
        Pageable customPageable = mock(Pageable.class);

        when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(customPageable);
        when(freelancerWorkflowService.getAllAvailableProjects(eq(customPageable)))
                .thenReturn(new PageImpl<>(List.of(projectPublicDto)));

        ResponseEntity<Page<ProjectPublicDto>> response = freelancerController.getAllAvailableProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paginationUtils).createPageable(argThat(pr ->
                pr.getSortBy().equals("createdDate") &&
                        pr.getSortDirection().equals("asc")
        ));
    }

    @Test
    void getProjectById_ReturnsProject() {
        when(freelancerWorkflowService.getProject(1L)).thenReturn(projectPublicDto);

        ResponseEntity<ProjectPublicDto> response = freelancerController.getProjectById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(projectPublicDto);
        verify(freelancerWorkflowService).getProject(1L);
    }

    @Test
    void getProjectById_WithInvalidId_ThrowsApiException() {
        when(freelancerWorkflowService.getProject(-1L))
                .thenThrow(new ApiException("Invalid project ID"));

        assertThatThrownBy(() -> freelancerController.getProjectById(-1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid project ID");
        verify(freelancerWorkflowService).getProject(-1L);
    }
}