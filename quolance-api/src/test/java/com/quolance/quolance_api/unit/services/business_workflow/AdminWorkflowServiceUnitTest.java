package com.quolance.quolance_api.services.business_workflow.impl;

import com.quolance.quolance_api.dtos.project.ProjectDto;
import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ProjectCategory;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.services.entity_services.ProjectService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminWorkflowServiceUnitTest {

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private AdminWorkflowServiceImpl adminWorkflowService;

    private Project mockProject1;
    private Project mockProject2;
    private User mockClient;

    @BeforeEach
    void setUp() {
        mockClient = User.builder()
                .id(1L)
                .email("client@test.com")
                .build();

        mockProject1 = Project.builder()
                .id(1L)
                .title("Test Project 1")
                .description("Test Description 1")
                .projectStatus(ProjectStatus.PENDING)
                .category(ProjectCategory.WEB_DEVELOPMENT)
                .expirationDate(LocalDate.now().plusDays(30))
                .client(mockClient)
                .build();

        mockProject2 = Project.builder()
                .id(2L)
                .title("Test Project 2")
                .description("Test Description 2")
                .projectStatus(ProjectStatus.PENDING)
                .category(ProjectCategory.WEB_DEVELOPMENT)
                .expirationDate(LocalDate.now().plusDays(30))
                .client(mockClient)
                .build();
    }

    @Test
    void getAllPendingProjects_ReturnsPageOfProjects() {
        List<Project> mockProjects = Arrays.asList(mockProject1, mockProject2);
        Page<Project> mockProjectPage = new PageImpl<>(mockProjects);

        when(projectService.getProjectsByStatuses(eq(Collections.singletonList(ProjectStatus.PENDING)), any(Pageable.class)))
                .thenReturn(mockProjectPage);

        Page<ProjectDto> result = adminWorkflowService.getAllPendingProjects(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(mockProject1.getId());
        assertThat(result.getContent().get(0).getTitle()).isEqualTo(mockProject1.getTitle());
        assertThat(result.getContent().get(1).getId()).isEqualTo(mockProject2.getId());
        assertThat(result.getContent().get(1).getTitle()).isEqualTo(mockProject2.getTitle());
        verify(projectService).getProjectsByStatuses(eq(Collections.singletonList(ProjectStatus.PENDING)), any(Pageable.class));
    }

    @Test
    void getAllPendingProjects_WithPaging_ReturnsCorrectPage() {
        List<Project> allProjects = Arrays.asList(
                mockProject1,
                mockProject2,
                Project.builder().id(3L).title("Test Project 3").description("Test Description 3")
                        .projectStatus(ProjectStatus.PENDING).client(mockClient).build(),
                Project.builder().id(4L).title("Test Project 4").description("Test Description 4")
                        .projectStatus(ProjectStatus.PENDING).client(mockClient).build(),
                Project.builder().id(5L).title("Test Project 5").description("Test Description 5")
                        .projectStatus(ProjectStatus.PENDING).client(mockClient).build()
        );

        Pageable pageRequest = org.springframework.data.domain.PageRequest.of(1, 2);

        Page<Project> expectedPage = new PageImpl<>(
                allProjects.subList(2, 4),
                pageRequest,
                allProjects.size()
        );

        when(projectService.getProjectsByStatuses(eq(Collections.singletonList(ProjectStatus.PENDING)), eq(pageRequest)))
                .thenReturn(expectedPage);

        Page<ProjectDto> result = adminWorkflowService.getAllPendingProjects(pageRequest);

        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(3L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(4L);

        verify(projectService).getProjectsByStatuses(eq(Collections.singletonList(ProjectStatus.PENDING)), eq(pageRequest));
    }

    @Test
    void getAllPendingProjects_WithLastPage_ReturnsPartialPage() {
        List<Project> lastPageProjects = Collections.singletonList(mockProject1);
        Pageable pageRequest = org.springframework.data.domain.PageRequest.of(2, 2);

        Page<Project> expectedPage = new PageImpl<>(
                lastPageProjects,
                pageRequest,
                5
        );

        when(projectService.getProjectsByStatuses(eq(Collections.singletonList(ProjectStatus.PENDING)), eq(pageRequest)))
                .thenReturn(expectedPage);

        Page<ProjectDto> result = adminWorkflowService.getAllPendingProjects(pageRequest);

        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(mockProject1.getId());

        verify(projectService).getProjectsByStatuses(eq(Collections.singletonList(ProjectStatus.PENDING)), eq(pageRequest));
    }

    @Test
    void getAllPendingProjects_WhenNoProjects_ReturnsEmptyPage() {
        Page<Project> emptyPage = Page.empty();
        when(projectService.getProjectsByStatuses(eq(Collections.singletonList(ProjectStatus.PENDING)), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<ProjectDto> result = adminWorkflowService.getAllPendingProjects(Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(projectService).getProjectsByStatuses(eq(Collections.singletonList(ProjectStatus.PENDING)), any(Pageable.class));
    }

    @Test
    void approveProject_Success() {
        when(projectService.getProjectById(1L)).thenReturn(mockProject1);
        doNothing().when(projectService).updateProjectStatus(any(Project.class), eq(ProjectStatus.OPEN));

        adminWorkflowService.approveProject(1L);

        verify(projectService).getProjectById(1L);
        verify(projectService).updateProjectStatus(mockProject1, ProjectStatus.OPEN);
    }

    @Test
    void approveProject_WithInvalidId_ThrowsApiException() {
        when(projectService.getProjectById(999L))
                .thenThrow(new ApiException("Project not found"));

        assertThatThrownBy(() -> adminWorkflowService.approveProject(999L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Project not found");
        verify(projectService).getProjectById(999L);
        verify(projectService, never()).updateProjectStatus(any(), any());
    }

    @Test
    void rejectProject_Success() {
        when(projectService.getProjectById(1L)).thenReturn(mockProject1);
        doNothing().when(projectService).updateProjectStatus(any(Project.class), eq(ProjectStatus.REJECTED));

        adminWorkflowService.rejectProject(1L);

        verify(projectService).getProjectById(1L);
        verify(projectService).updateProjectStatus(mockProject1, ProjectStatus.REJECTED);
    }

    @Test
    void rejectProject_WithInvalidId_ThrowsApiException() {
        when(projectService.getProjectById(999L))
                .thenThrow(new ApiException("Project not found"));

        assertThatThrownBy(() -> adminWorkflowService.rejectProject(999L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Project not found");
        verify(projectService).getProjectById(999L);
        verify(projectService, never()).updateProjectStatus(any(), any());
    }
}