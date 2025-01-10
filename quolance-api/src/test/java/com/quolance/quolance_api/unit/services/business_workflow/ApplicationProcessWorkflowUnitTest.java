package com.quolance.quolance_api.services.business_workflow.impl;

import com.quolance.quolance_api.entities.Application;
import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ApplicationStatus;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.services.entity_services.ApplicationService;
import com.quolance.quolance_api.services.entity_services.ProjectService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletResponse;
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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationProcessWorkflowUnitTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ApplicationProcessWorkflowImpl applicationProcessWorkflow;

    private User mockClient;
    private User mockFreelancer;
    private Project mockProject;
    private Application mockApplication;
    private Application mockApplication2;

    @BeforeEach
    void setUp() {
        mockClient = User.builder()
                .id(1L)
                .email("client@test.com")
                .build();

        mockFreelancer = User.builder()
                .id(2L)
                .email("freelancer@test.com")
                .build();

        mockProject = Project.builder()
                .id(1L)
                .client(mockClient)
                .projectStatus(ProjectStatus.OPEN)
                .build();

        mockApplication = Application.builder()
                .id(1L)
                .project(mockProject)
                .freelancer(mockFreelancer)
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        mockApplication2 = Application.builder()
                .id(2L)
                .project(mockProject)
                .freelancer(User.builder().id(3L).build())
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();
    }

    @Test
    void selectFreelancer_Success() {
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        Pageable firstPageable = PageRequest.of(0, 100);
        Page<Application> firstPage = new PageImpl<>(
                Arrays.asList(mockApplication, mockApplication2),
                firstPageable,
                2
        );

        when(applicationService.getAllApplicationsByProjectId(eq(1L), any(Pageable.class)))
                .thenReturn(firstPage);

        applicationProcessWorkflow.selectFreelancer(1L, mockClient);

        verify(applicationService).updateApplicationStatus(mockApplication, ApplicationStatus.ACCEPTED);
        verify(applicationService).updateApplicationStatus(mockApplication2, ApplicationStatus.REJECTED);

        verify(projectService).updateProjectStatus(mockProject, ProjectStatus.CLOSED);
        verify(projectService).updateSelectedFreelancer(mockProject, mockFreelancer);

        verify(applicationService).getAllApplicationsByProjectId(eq(1L), eq(PageRequest.of(0, 100)));
    }

    @Test
    void selectFreelancer_WithMultiplePages_Success() {
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        Application mockApplication3 = Application.builder()
                .id(3L)
                .project(mockProject)
                .freelancer(User.builder().id(4L).build())
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        Application mockApplication4 = Application.builder()
                .id(4L)
                .project(mockProject)
                .freelancer(User.builder().id(5L).build())
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        Pageable firstPageable = PageRequest.of(0, 2);
        Page<Application> firstPage = new PageImpl<>(
                Arrays.asList(mockApplication, mockApplication2),
                firstPageable,
                4
        );

        Pageable secondPageable = PageRequest.of(1, 2);
        Page<Application> secondPage = new PageImpl<>(
                Arrays.asList(mockApplication3, mockApplication4),
                secondPageable,
                4
        );

        when(applicationService.getAllApplicationsByProjectId(eq(1L), any(Pageable.class)))
                .thenReturn(firstPage)
                .thenReturn(secondPage);

        applicationProcessWorkflow.selectFreelancer(1L, mockClient);

        verify(applicationService).updateApplicationStatus(mockApplication, ApplicationStatus.ACCEPTED);

        verify(applicationService).updateApplicationStatus(mockApplication2, ApplicationStatus.REJECTED);
        verify(applicationService).updateApplicationStatus(mockApplication3, ApplicationStatus.REJECTED);
        verify(applicationService).updateApplicationStatus(mockApplication4, ApplicationStatus.REJECTED);

        verify(projectService).updateProjectStatus(mockProject, ProjectStatus.CLOSED);
        verify(projectService).updateSelectedFreelancer(mockProject, mockFreelancer);

        verify(applicationService).getAllApplicationsByProjectId(eq(1L), eq(PageRequest.of(0, 100)));
    }

    @Test
    void selectFreelancer_WhenNotProjectOwner_ThrowsApiException() {
        User wrongClient = User.builder().id(999L).build();
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        assertThatThrownBy(() -> applicationProcessWorkflow.selectFreelancer(1L, wrongClient))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_FORBIDDEN)
                .hasMessage("You are not authorized to perform this action on this project");
    }

    @Test
    void selectFreelancer_WhenProjectNotOpen_ThrowsApiException() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        assertThatThrownBy(() -> applicationProcessWorkflow.selectFreelancer(1L, mockClient))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("Cannot perform this action, project is either pending or rejected");
    }

    @Test
    void selectFreelancer_WhenFreelancerAlreadySelected_ThrowsApiException() {
        mockProject.setSelectedFreelancer(mockFreelancer);
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        assertThatThrownBy(() -> applicationProcessWorkflow.selectFreelancer(1L, mockClient))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("A freelancer is already working on this project");
    }

    @Test
    void selectFreelancer_WhenOptimisticLockException_ThrowsApiException() {
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);
        doThrow(new OptimisticLockException("Resource was updated")).when(applicationService)
                .updateApplicationStatus(any(), any());

        assertThatThrownBy(() -> applicationProcessWorkflow.selectFreelancer(1L, mockClient))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("The resource was modified by another user. Please refresh the page and try again.");
    }

    @Test
    void rejectApplication_Success() {
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        applicationProcessWorkflow.rejectApplication(1L, mockClient);

        verify(applicationService).updateApplicationStatus(mockApplication, ApplicationStatus.REJECTED);
    }

    @Test
    void rejectManyApplications_AllSuccessful() {
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);
        when(applicationService.getApplicationById(2L)).thenReturn(mockApplication2);

        List<Long> applicationIds = Arrays.asList(1L, 2L);
        assertThatThrownBy(() -> applicationProcessWorkflow.rejectManyApplications(applicationIds, mockClient))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_PARTIAL_CONTENT);

        verify(applicationService, times(1)).updateApplicationStatus(mockApplication, ApplicationStatus.REJECTED);
        verify(applicationService, times(1)).updateApplicationStatus(mockApplication2, ApplicationStatus.REJECTED);
    }

    @Test
    void cancelApplication_Success() {
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        applicationProcessWorkflow.cancelApplication(1L, mockFreelancer);

        verify(applicationService).deleteApplication(mockApplication);
    }

    @Test
    void cancelApplication_WhenNotOwner_ThrowsApiException() {
        User wrongFreelancer = User.builder().id(999L).build();
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        assertThatThrownBy(() -> applicationProcessWorkflow.cancelApplication(1L, wrongFreelancer))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 403)
                .hasMessage("You are not authorized to perform this action on that application");
    }

    @Test
    void cancelApplication_WhenAlreadyAccepted_ThrowsApiException() {
        mockApplication.setApplicationStatus(ApplicationStatus.ACCEPTED);
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);

        assertThatThrownBy(() -> applicationProcessWorkflow.cancelApplication(1L, mockFreelancer))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("You cannot cancel an application that has been accepted");
    }

    @Test
    void cancelApplication_WhenOptimisticLockException_ThrowsApiException() {
        when(applicationService.getApplicationById(1L)).thenReturn(mockApplication);
        doThrow(new OptimisticLockException("Resource was updated")).when(applicationService)
                .deleteApplication(any());

        assertThatThrownBy(() -> applicationProcessWorkflow.cancelApplication(1L, mockFreelancer))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpServletResponse.SC_CONFLICT)
                .hasMessage("The resource was modified by another user. Please refresh the page and try again.");
    }
}