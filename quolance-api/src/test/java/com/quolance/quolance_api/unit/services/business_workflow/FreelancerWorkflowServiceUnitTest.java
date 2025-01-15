package com.quolance.quolance_api.unit.services.business_workflow;

import com.quolance.quolance_api.dtos.application.ApplicationCreateDto;
import com.quolance.quolance_api.dtos.application.ApplicationDto;
import com.quolance.quolance_api.dtos.project.ProjectFilterDto;
import com.quolance.quolance_api.dtos.project.ProjectPublicDto;
import com.quolance.quolance_api.entities.Application;
import com.quolance.quolance_api.entities.Profile;
import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ApplicationStatus;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.services.business_workflow.impl.FreelancerWorkflowServiceImpl;
import com.quolance.quolance_api.services.entity_services.ApplicationService;
import com.quolance.quolance_api.services.entity_services.ProjectService;
import com.quolance.quolance_api.services.business_workflow.impl.FreelancerWorkflowServiceImpl;
import com.quolance.quolance_api.util.PaginationUtils;
import com.quolance.quolance_api.util.exceptions.ApiException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreelancerWorkflowServiceTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private FreelancerWorkflowServiceImpl freelancerWorkflowService;

    @Captor
    private ArgumentCaptor<Application> applicationCaptor;

    private User mockFreelancer;
    private Project mockProject;
    private Application mockApplication;
    private ApplicationCreateDto applicationCreateDto;

    @BeforeEach
    void setUp() {
        mockFreelancer = User.builder()
                .id(1L)
                .profile(new Profile())
                .build();

        mockProject = Project.builder()
                .id(1L)
                .projectStatus(ProjectStatus.OPEN)
                .visibilityExpirationDate(LocalDate.now().plusDays(7))
                .build();

        mockApplication = Application.builder()
                .id(1L)
                .project(mockProject)
                .freelancer(mockFreelancer)
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        applicationCreateDto = new ApplicationCreateDto(mockProject.getId());
    }

    @Test
    void submitApplication_Success() {
        when(projectService.getProjectById(1L)).thenReturn(mockProject);
        when(applicationService.getApplicationByFreelancerIdAndProjectId(1L, 1L)).thenReturn(null);

        freelancerWorkflowService.submitApplication(applicationCreateDto, mockFreelancer);

        verify(applicationService).saveApplication(applicationCaptor.capture());
        Application savedApplication = applicationCaptor.getValue();
        assertThat(savedApplication.getFreelancer()).isEqualTo(mockFreelancer);
        assertThat(savedApplication.getProject()).isEqualTo(mockProject);
    }

    @Test
    void submitApplication_AlreadyApplied_ThrowsException() {
        when(applicationService.getApplicationByFreelancerIdAndProjectId(1L, 1L)).thenReturn(mockApplication);

        assertThatThrownBy(() -> freelancerWorkflowService.submitApplication(applicationCreateDto, mockFreelancer))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("You have already applied to this project.");
    }

    @Test
    void submitApplication_ProjectNotApproved_ThrowsException() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);
        when(projectService.getProjectById(1L)).thenReturn(mockProject);
        when(applicationService.getApplicationByFreelancerIdAndProjectId(1L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> freelancerWorkflowService.submitApplication(applicationCreateDto, mockFreelancer))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("You can't apply to this project.");
    }

    @Test
    void submitApplication_OptimisticLockException_ThrowsException() {
        when(projectService.getProjectById(1L)).thenReturn(mockProject);
        when(applicationService.getApplicationByFreelancerIdAndProjectId(1L, 1L)).thenReturn(null);
        doThrow(new OptimisticLockException("Version mismatch")).when(applicationService).saveApplication(any());

        assertThatThrownBy(() -> freelancerWorkflowService.submitApplication(applicationCreateDto, mockFreelancer))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("The ressource was modified by another user. Please refresh the page and try again.");
    }

    @Test
    void getApplication_Success() {
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        ApplicationDto result = freelancerWorkflowService.getApplication(1L, mockFreelancer);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(mockApplication.getId());
    }

    @Test
    void getApplication_NotOwner_ThrowsException() {
        User wrongFreelancer = User.builder().id(999L).build();
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        assertThatThrownBy(() -> freelancerWorkflowService.getApplication(1L, wrongFreelancer))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_FORBIDDEN)
                .hasMessage("You are not authorized to view this application.");
    }

    @Test
    void deleteApplication_Success() {
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        freelancerWorkflowService.deleteApplication(1L, mockFreelancer);

        verify(applicationService).deleteApplication(mockApplication);
    }

    @Test
    void getAllFreelancerApplications_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> applicationPage = new PageImpl<>(List.of(mockApplication), pageable, 1);

        when(applicationService.getAllApplicationsByFreelancerId(1L, pageable))
                .thenReturn(applicationPage);

        Page<ApplicationDto> results = freelancerWorkflowService.getAllFreelancerApplications(mockFreelancer, pageable);

        assertThat(results).hasSize(1);
        assertThat(results.getContent().get(0).getId()).isEqualTo(mockApplication.getId());
    }

    @Test
    void getAllFreelancerApplications_NoApplications_ReturnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> emptyPage = Page.empty(pageable);
        when(applicationService.getAllApplicationsByFreelancerId(1L, pageable))
                .thenReturn(emptyPage);

        Page<ApplicationDto> results = freelancerWorkflowService.getAllFreelancerApplications(mockFreelancer, pageable);

        assertThat(results).isEmpty();
    }

    @Test
    void getAllAvailableProjects_FiltersExpiredClosedProjects() {
        Project expiredProject = Project.builder()
                .id(2L)
                .projectStatus(ProjectStatus.CLOSED)
                .visibilityExpirationDate(LocalDate.now().minusDays(1))
                .build();

        Project activeProject = Project.builder()
                .id(3L)
                .projectStatus(ProjectStatus.OPEN)
                .visibilityExpirationDate(LocalDate.now().plusDays(7))
                .build();

        Project closedButVisibleProject = Project.builder()
                .id(4L)
                .projectStatus(ProjectStatus.CLOSED)
                .visibilityExpirationDate(LocalDate.now().plusDays(2))
                .build();

        List<Project> mockProjects = List.of(activeProject, closedButVisibleProject);
        Page<Project> mockPage = new PageImpl<>(mockProjects);
        Pageable pageable = PageRequest.of(0, 10);
        ProjectFilterDto filters = new ProjectFilterDto();

        when(projectService.findAllWithFilters(any(Specification.class), eq(pageable))).thenReturn(mockPage);
        Page<ProjectPublicDto> results = freelancerWorkflowService.getAllAvailableProjects(pageable, filters);

        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent().stream().map(ProjectPublicDto::getId))
                .containsExactlyInAnyOrder(activeProject.getId(), closedButVisibleProject.getId());
        verify(projectService).findAllWithFilters(any(Specification.class), eq(pageable));
    }

    @Test
    void getProject_Success() {
        when(projectService.getProjectById(1L)).thenReturn(mockProject);

        ProjectPublicDto result = freelancerWorkflowService.getProject(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void getProject_PendingStatus_ThrowsException() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);
        when(projectService.getProjectById(1L)).thenReturn(mockProject);

        assertThatThrownBy(() -> freelancerWorkflowService.getProject(1L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("Project is not yet approved.");
    }

    @Test
    void getProject_ExpiredVisibility_ThrowsException() {
        mockProject.setVisibilityExpirationDate(LocalDate.now().minusDays(1));
        when(projectService.getProjectById(1L)).thenReturn(mockProject);

        assertThatThrownBy(() -> freelancerWorkflowService.getProject(1L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("Project visibility has expired.");
    }
}