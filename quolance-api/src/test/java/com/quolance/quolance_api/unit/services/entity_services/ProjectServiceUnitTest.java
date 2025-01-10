package com.quolance.quolance_api.services.entity_services.impl;

import com.quolance.quolance_api.dtos.project.ProjectUpdateDto;
import com.quolance.quolance_api.entities.Project;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.ProjectCategory;
import com.quolance.quolance_api.entities.enums.FreelancerExperienceLevel;
import com.quolance.quolance_api.entities.enums.PriceRange;
import com.quolance.quolance_api.entities.enums.ExpectedDeliveryTime;
import com.quolance.quolance_api.entities.enums.ProjectStatus;
import com.quolance.quolance_api.repositories.ProjectRepository;
import com.quolance.quolance_api.util.exceptions.ApiException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceUnitTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Captor
    private ArgumentCaptor<Project> projectCaptor;

    private Project mockProject;
    private User mockFreelancer;
    private ProjectUpdateDto mockUpdateDto;

    @BeforeEach
    void setUp() {
        mockProject = Project.builder()
                .id(1L)
                .title("Test Project")
                .description("Test Description")
                .projectStatus(ProjectStatus.PENDING)
                .category(ProjectCategory.WEB_DEVELOPMENT)
                .priceRange(PriceRange.BETWEEN_1000_5000)
                .expectedDeliveryTime(ExpectedDeliveryTime.WITHIN_A_MONTH)
                .experienceLevel(FreelancerExperienceLevel.INTERMEDIATE)
                .build();

        mockFreelancer = User.builder()
                .id(1L)
                .build();

        mockUpdateDto = new ProjectUpdateDto();
        mockUpdateDto.setTitle("Updated Title");
        mockUpdateDto.setDescription("Updated Description");
        mockUpdateDto.setCategory(ProjectCategory.APP_DEVELOPMENT);
        mockUpdateDto.setPriceRange(PriceRange.BETWEEN_5000_10000);
        mockUpdateDto.setExperienceLevel(FreelancerExperienceLevel.EXPERT);
        mockUpdateDto.setExpectedDeliveryTime(ExpectedDeliveryTime.WITHIN_A_WEEK);
    }

    @Test
    void saveProject_Success() {
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        projectService.saveProject(mockProject);

        verify(projectRepository).save(mockProject);
    }

    @Test
    void deleteProject_Success() {
        projectService.deleteProject(mockProject);

        verify(projectRepository).delete(mockProject);
    }

    @Test
    void getProjectById_Success() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));

        Project result = projectService.getProjectById(1L);

        assertThat(result).isEqualTo(mockProject);
    }

    @Test
    void getProjectById_NotFound_ThrowsException() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(1L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 404)
                .hasMessage("No Project found with ID: 1");
    }

    @Test
    void getAllProjects_Success() {
        List<Project> projects = Arrays.asList(mockProject);
        when(projectRepository.findAll()).thenReturn(projects);

        List<Project> result = projectService.getAllProjects();

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(mockProject);
    }

    @Test
    void getProjectsByStatuses_Success() {
        List<Project> projects = List.of(mockProject);
        List<ProjectStatus> statuses = Arrays.asList(ProjectStatus.PENDING, ProjectStatus.OPEN);
        Page<Project> expectedPage = new PageImpl<>(projects);

        when(projectRepository.findProjectsByProjectStatusIn(eq(statuses), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Project> result = projectService.getProjectsByStatuses(statuses, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).containsExactly(mockProject);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(projectRepository).findProjectsByProjectStatusIn(eq(statuses), any(Pageable.class));
    }

    @Test
    void getProjectsByStatuses_WithPaging_Success() {
        Project mockProject2 = Project.builder()
                .id(2L)
                .title("Test Project 2")
                .projectStatus(ProjectStatus.OPEN)
                .build();
        Project mockProject3 = Project.builder()
                .id(3L)
                .title("Test Project 3")
                .projectStatus(ProjectStatus.OPEN)
                .build();

        List<ProjectStatus> statuses = List.of(ProjectStatus.OPEN);
        List<Project> allProjects = Arrays.asList(mockProject, mockProject2, mockProject3);

        Pageable pageRequest = PageRequest.of(0, 2);
        Page<Project> expectedPage = new PageImpl<>(
                allProjects.subList(0, 2),
                pageRequest,
                allProjects.size()
        );

        when(projectRepository.findProjectsByProjectStatusIn(eq(statuses), eq(pageRequest)))
                .thenReturn(expectedPage);

        Page<Project> result = projectService.getProjectsByStatuses(statuses, pageRequest);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        verify(projectRepository).findProjectsByProjectStatusIn(eq(statuses), eq(pageRequest));
    }

    @Test
    void getProjectsByClientId_Success() {
        List<Project> projects = List.of(mockProject);
        Page<Project> expectedPage = new PageImpl<>(projects);

        when(projectRepository.findProjectsByClientId(eq(1L), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Project> result = projectService.getProjectsByClientId(1L, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).containsExactly(mockProject);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(projectRepository).findProjectsByClientId(eq(1L), any(Pageable.class));
    }

    @Test
    void getProjectsByClientId_WithMultiplePages_Success() {
        Project mockProject2 = Project.builder()
                .id(2L)
                .title("Test Project 2")
                .build();

        Project mockProject3 = Project.builder()
                .id(3L)
                .title("Test Project 3")
                .build();

        List<Project> allProjects = Arrays.asList(mockProject, mockProject2, mockProject3);

        Pageable firstPageRequest = PageRequest.of(0, 2);
        Page<Project> firstPage = new PageImpl<>(
                allProjects.subList(0, 2),
                firstPageRequest,
                allProjects.size()
        );

        when(projectRepository.findProjectsByClientId(eq(1L), eq(firstPageRequest)))
                .thenReturn(firstPage);

        Page<Project> result = projectService.getProjectsByClientId(1L, firstPageRequest);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();

        Pageable secondPageRequest = PageRequest.of(1, 2);
        Page<Project> secondPage = new PageImpl<>(
                allProjects.subList(2, 3),
                secondPageRequest,
                allProjects.size()
        );

        when(projectRepository.findProjectsByClientId(eq(1L), eq(secondPageRequest)))
                .thenReturn(secondPage);

        result = projectService.getProjectsByClientId(1L, secondPageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();

        verify(projectRepository, times(2)).findProjectsByClientId(eq(1L), any(Pageable.class));
    }

    @Test
    void updateProjectStatus_Success_ToOpen() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        projectService.updateProjectStatus(mockProject, ProjectStatus.OPEN);

        verify(projectRepository).save(projectCaptor.capture());
        Project savedProject = projectCaptor.getValue();
        assertThat(savedProject.getProjectStatus()).isEqualTo(ProjectStatus.OPEN);
    }

    @Test
    void updateProjectStatus_Success_ToClosed() {
        mockProject.setProjectStatus(ProjectStatus.OPEN);
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        projectService.updateProjectStatus(mockProject, ProjectStatus.CLOSED);

        verify(projectRepository).save(projectCaptor.capture());
        Project savedProject = projectCaptor.getValue();
        assertThat(savedProject.getProjectStatus()).isEqualTo(ProjectStatus.CLOSED);
        assertThat(savedProject.getVisibilityExpirationDate()).isEqualTo(LocalDate.now().plusDays(3));
    }

    @Test
    void updateProjectStatus_Success_ToRejected() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        projectService.updateProjectStatus(mockProject, ProjectStatus.REJECTED);

        verify(projectRepository).save(projectCaptor.capture());
        Project savedProject = projectCaptor.getValue();
        assertThat(savedProject.getProjectStatus()).isEqualTo(ProjectStatus.REJECTED);
    }

    @Test
    void updateProjectStatus_FromClosed_ThrowsException() {
        mockProject.setProjectStatus(ProjectStatus.CLOSED);

        assertThatThrownBy(() -> projectService.updateProjectStatus(mockProject, ProjectStatus.OPEN))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 403)
                .hasMessage("Project is closed and cannot be updated.");
    }

    @Test
    void updateProjectStatus_SameStatus_ThrowsException() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);

        assertThatThrownBy(() -> projectService.updateProjectStatus(mockProject, ProjectStatus.PENDING))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 409)
                .hasMessage("Project is already in status: PENDING");
    }

    @Test
    void updateSelectedFreelancer_Success() {
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        projectService.updateSelectedFreelancer(mockProject, mockFreelancer);

        verify(projectRepository).save(projectCaptor.capture());
        Project savedProject = projectCaptor.getValue();
        assertThat(savedProject.getSelectedFreelancer()).isEqualTo(mockFreelancer);
    }

    @Test
    void updateProject_Success() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        projectService.updateProject(mockProject, mockUpdateDto);

        verify(projectRepository).save(projectCaptor.capture());
        Project savedProject = projectCaptor.getValue();
        assertThat(savedProject.getTitle()).isEqualTo("Updated Title");
        assertThat(savedProject.getDescription()).isEqualTo("Updated Description");
        assertThat(savedProject.getCategory()).isEqualTo(ProjectCategory.APP_DEVELOPMENT);
        assertThat(savedProject.getPriceRange()).isEqualTo(PriceRange.BETWEEN_5000_10000);
        assertThat(savedProject.getExperienceLevel()).isEqualTo(FreelancerExperienceLevel.EXPERT);
        assertThat(savedProject.getExpectedDeliveryTime()).isEqualTo(ExpectedDeliveryTime.WITHIN_A_WEEK);
    }

    @Test
    void updateProject_NotPending_ThrowsException() {
        mockProject.setProjectStatus(ProjectStatus.OPEN);

        assertThatThrownBy(() -> projectService.updateProject(mockProject, mockUpdateDto))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 403)
                .hasMessage("Project can only be updated when in PENDING state");
    }

    @Test
    void updateProject_NullDto_ThrowsException() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);

        assertThatThrownBy(() -> projectService.updateProject(mockProject, null))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 400)
                .hasMessage("Update data cannot be null");
    }

    @Test
    void updateProject_EmptyTitle_ThrowsException() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);
        mockUpdateDto.setTitle(" ");

        assertThatThrownBy(() -> projectService.updateProject(mockProject, mockUpdateDto))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 400)
                .hasMessage("Title cannot be empty");
    }

    @Test
    void updateProject_EmptyDescription_ThrowsException() {
        mockProject.setProjectStatus(ProjectStatus.PENDING);
        mockUpdateDto.setDescription(" ");

        assertThatThrownBy(() -> projectService.updateProject(mockProject, mockUpdateDto))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 400)
                .hasMessage("Description cannot be empty");
    }
}