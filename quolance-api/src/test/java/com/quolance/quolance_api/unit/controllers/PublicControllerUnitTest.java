package com.quolance.quolance_api.unit;

import com.quolance.quolance_api.controllers.PublicController;
import com.quolance.quolance_api.dtos.PageableRequestDto;
import com.quolance.quolance_api.dtos.project.ProjectPublicDto;
import com.quolance.quolance_api.services.business_workflow.FreelancerWorkflowService;
import com.quolance.quolance_api.util.PaginationUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicControllerUnitTest {

    @Mock
    private FreelancerWorkflowService freelancerWorkflowService;

    @Mock
    private PaginationUtils paginationUtils;


    @InjectMocks
    private PublicController publicController;

    private ProjectPublicDto sampleProject;
    private List<ProjectPublicDto> sampleProjects;

    @BeforeEach
    void setUp() {
        sampleProject = ProjectPublicDto.builder()
                .id(1L)
                .title("Sample Project")
                .description("Sample Description")
                .build();

        sampleProjects = Arrays.asList(
                sampleProject,
                ProjectPublicDto.builder()
                        .id(2L)
                        .title("Another Project")
                        .description("Another Description")
                        .build()
        );

        lenient().when(paginationUtils.createPageable(any(PageableRequestDto.class))).thenReturn(Pageable.unpaged());

    }

    @Test
    void getAllAvailableProjects_ShouldReturnProjectsPage_WhenProjectsExist() {

        Page<ProjectPublicDto> projectPage = new PageImpl<>(sampleProjects);

        when(freelancerWorkflowService.getAllAvailableProjects(any(Pageable.class))).thenReturn(projectPage);

        PageableRequestDto pageableRequest = new PageableRequestDto();
        ResponseEntity<Page<ProjectPublicDto>> response = publicController.getAllAvailableProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getContent().get(0)).isEqualTo(sampleProjects.get(0));
        assertThat(response.getBody().getContent().get(1)).isEqualTo(sampleProjects.get(1));
        verify(freelancerWorkflowService).getAllAvailableProjects(any(Pageable.class));
    }

    @Test
    void getAllAvailableProjects_WithCustomPageSize_ReturnsCorrectPageSize() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setSize(5);
        Pageable customPageable = mock(Pageable.class);
        when(customPageable.getPageSize()).thenReturn(5);
        Page<ProjectPublicDto> projectPage = new PageImpl<>(Collections.singletonList(sampleProject));

        when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(customPageable);
        when(freelancerWorkflowService.getAllAvailableProjects(eq(customPageable)))
                .thenReturn(projectPage);

        ResponseEntity<Page<ProjectPublicDto>> response = publicController.getAllAvailableProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(paginationUtils).createPageable(argThat(pr ->
                pr.getSize() == 5
        ));
        verify(freelancerWorkflowService).getAllAvailableProjects(argThat(p ->
                p.getPageSize() == 5
        ));
    }

    @Test
    void getAllAvailableProjects_WithCustomPage_ReturnsCorrectPage() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setPage(2);
        Pageable customPageable = mock(Pageable.class);
        when(customPageable.getPageNumber()).thenReturn(2);
        Page<ProjectPublicDto> projectPage = new PageImpl<>(Collections.singletonList(sampleProject));

        when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(customPageable);
        when(freelancerWorkflowService.getAllAvailableProjects(eq(customPageable)))
                .thenReturn(projectPage);

        ResponseEntity<Page<ProjectPublicDto>> response = publicController.getAllAvailableProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(paginationUtils).createPageable(argThat(pr ->
                pr.getPage() == 2
        ));
        verify(freelancerWorkflowService).getAllAvailableProjects(argThat(p ->
                p.getPageNumber() == 2
        ));
    }

    @Test
    void getAllAvailableProjects_WithCustomSortField_ReturnsSortedResults() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setSortBy("createdDate");
        pageableRequest.setSortDirection("asc");
        Pageable customPageable = mock(Pageable.class);
        Page<ProjectPublicDto> projectPage = new PageImpl<>(Collections.singletonList(sampleProject));

        when(paginationUtils.createPageable(eq(pageableRequest))).thenReturn(customPageable);
        when(freelancerWorkflowService.getAllAvailableProjects(eq(customPageable)))
                .thenReturn(projectPage);

        ResponseEntity<Page<ProjectPublicDto>> response = publicController.getAllAvailableProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(paginationUtils).createPageable(argThat(pr ->
                pr.getSortBy().equals("createdDate") &&
                        pr.getSortDirection().equals("asc")
        ));
    }

    @Test
    void getAllAvailableProjects_WithInvalidPageNumber_ThrowsApiException() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setPage(-1);

        when(paginationUtils.createPageable(eq(pageableRequest)))
                .thenThrow(new ApiException("Page number cannot be negative"));

        assertThatThrownBy(() -> publicController.getAllAvailableProjects(pageableRequest))
                .isInstanceOf(ApiException.class)
                .hasMessage("Page number cannot be negative");

        verify(paginationUtils).createPageable(eq(pageableRequest));
        verifyNoInteractions(freelancerWorkflowService);
    }

    @Test
    void getAllAvailableProjects_WithInvalidSortDirection_ThrowsApiException() {
        PageableRequestDto pageableRequest = new PageableRequestDto();
        pageableRequest.setSortDirection("invalid");

        when(paginationUtils.createPageable(eq(pageableRequest)))
                .thenThrow(new ApiException("Sort direction must be either 'asc' or 'desc'"));

        assertThatThrownBy(() -> publicController.getAllAvailableProjects(pageableRequest))
                .isInstanceOf(ApiException.class)
                .hasMessage("Sort direction must be either 'asc' or 'desc'");

        verify(paginationUtils).createPageable(eq(pageableRequest));
        verifyNoInteractions(freelancerWorkflowService);
    }

    @Test
    void getAllAvailableProjects_ShouldReturnEmptyPage_WhenNoProjects() {

        Page<ProjectPublicDto> emptyPage = Page.empty();

        when(freelancerWorkflowService.getAllAvailableProjects(any(Pageable.class))).thenReturn(emptyPage);

        PageableRequestDto pageableRequest = new PageableRequestDto();
        ResponseEntity<Page<ProjectPublicDto>> response = publicController.getAllAvailableProjects(pageableRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
        verify(freelancerWorkflowService).getAllAvailableProjects(any(Pageable.class));

    }

    @Test
    void getAllAvailableProjects_ShouldThrowApiException_WhenServiceFails() {
        when(freelancerWorkflowService.getAllAvailableProjects(any(Pageable.class)))
                .thenThrow(ApiException.builder()
                        .message("Failed to fetch projects")
                        .status(500)
                        .build());

        PageableRequestDto pageableRequest = new PageableRequestDto();

        assertThatThrownBy(() -> publicController.getAllAvailableProjects(pageableRequest))
                .isInstanceOf(ApiException.class)
                .hasMessage("Failed to fetch projects");
        verify(freelancerWorkflowService).getAllAvailableProjects(any(Pageable.class));
        verifyNoMoreInteractions(freelancerWorkflowService);

    }

    @Test
    void getProjectById_ShouldReturnProject_WhenProjectExists() {
        when(freelancerWorkflowService.getProject(1L)).thenReturn(sampleProject);

        ResponseEntity<ProjectPublicDto> response = publicController.getProjectById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .isEqualTo(sampleProject);
        verify(freelancerWorkflowService).getProject(1L);
        verifyNoMoreInteractions(freelancerWorkflowService);
    }

    @Test
    void getProjectById_ShouldThrowApiException_WhenProjectNotFound() {
        when(freelancerWorkflowService.getProject(anyLong()))
                .thenThrow(ApiException.builder()
                        .message("Project not found")
                        .status(404)
                        .build());

        assertThatThrownBy(() -> publicController.getProjectById(999L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Project not found");
        verify(freelancerWorkflowService).getProject(999L);
        verifyNoMoreInteractions(freelancerWorkflowService);
    }

    @Test
    void getProjectById_ShouldThrowApiException_WhenServiceFails() {
        when(freelancerWorkflowService.getProject(anyLong()))
                .thenThrow(ApiException.builder()
                        .message("Service unavailable")
                        .status(500)
                        .build());

        assertThatThrownBy(() -> publicController.getProjectById(1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Service unavailable");
        verify(freelancerWorkflowService).getProject(1L);
        verifyNoMoreInteractions(freelancerWorkflowService);
    }
}
