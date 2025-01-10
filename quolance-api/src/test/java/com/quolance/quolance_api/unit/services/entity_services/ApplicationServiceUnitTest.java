package com.quolance.quolance_api.services.entity_services.impl;

import com.quolance.quolance_api.entities.Application;
import com.quolance.quolance_api.entities.enums.ApplicationStatus;
import com.quolance.quolance_api.repositories.ApplicationRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceUnitTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Captor
    private ArgumentCaptor<Application> applicationCaptor;

    private Application mockApplication;

    @BeforeEach
    void setUp() {
        mockApplication = Application.builder()
                .id(1L)
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();
    }

    @Test
    void saveApplication_Success() {
        when(applicationRepository.save(any(Application.class))).thenReturn(mockApplication);

        applicationService.saveApplication(mockApplication);

        verify(applicationRepository).save(mockApplication);
    }

    @Test
    void deleteApplication_Success() {
        applicationService.deleteApplication(mockApplication);

        verify(applicationRepository).delete(mockApplication);
    }

    @Test
    void getApplicationById_Success() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(mockApplication));

        Application result = applicationService.getApplicationById(1L);

        assertThat(result).isEqualTo(mockApplication);
        verify(applicationRepository).findById(1L);
    }

    @Test
    void getApplicationById_NotFound_ThrowsException() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(1L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 404)
                .hasMessage("No Application found with ID: 1");
    }

    @Test
    void getAllApplications_Success() {
        List<Application> applications = Arrays.asList(mockApplication);
        when(applicationRepository.findAll()).thenReturn(applications);

        List<Application> result = applicationService.getAllApplications();

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(mockApplication);
    }

    @Test
    void getApplicationByFreelancerIdAndProjectId_Success() {
        when(applicationRepository.findApplicationByFreelancerIdAndProjectId(1L, 1L))
                .thenReturn(mockApplication);

        Application result = applicationService.getApplicationByFreelancerIdAndProjectId(1L, 1L);

        assertThat(result).isEqualTo(mockApplication);
    }

    @Test
    void getAllApplicationsByFreelancerId_Success() {
        List<Application> applications = List.of(mockApplication);
        Page<Application> expectedPage = new PageImpl<>(applications);

        when(applicationRepository.findApplicationsByFreelancerId(eq(1L), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Application> result = applicationService.getAllApplicationsByFreelancerId(1L, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).containsExactly(mockApplication);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(applicationRepository).findApplicationsByFreelancerId(eq(1L), any(Pageable.class));
    }

    @Test
    void getAllApplicationsByFreelancerId_WithPaging_Success() {
        Application mockApplication2 = Application.builder()
                .id(2L)
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        List<Application> applications = Arrays.asList(mockApplication, mockApplication2);
        Pageable pageRequest = org.springframework.data.domain.PageRequest.of(0, 1);
        Page<Application> expectedPage = new PageImpl<>(List.of(mockApplication), pageRequest, 2);

        when(applicationRepository.findApplicationsByFreelancerId(eq(1L), eq(pageRequest)))
                .thenReturn(expectedPage);

        Page<Application> result = applicationService.getAllApplicationsByFreelancerId(1L, pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(mockApplication);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        verify(applicationRepository).findApplicationsByFreelancerId(eq(1L), eq(pageRequest));
    }

    @Test
    void getAllApplicationsByProjectId_Success() {
        List<Application> applications = List.of(mockApplication);
        Page<Application> expectedPage = new PageImpl<>(applications);

        when(applicationRepository.findApplicationsByProjectId(eq(1L), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Application> result = applicationService.getAllApplicationsByProjectId(1L, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).containsExactly(mockApplication);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(applicationRepository).findApplicationsByProjectId(eq(1L), any(Pageable.class));
    }

    @Test
    void getAllApplicationsByProjectId_WithPaging_Success() {
        Application mockApplication2 = Application.builder()
                .id(2L)
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        Application mockApplication3 = Application.builder()
                .id(3L)
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        List<Application> allApplications = Arrays.asList(mockApplication, mockApplication2, mockApplication3);

        Pageable firstPageRequest = org.springframework.data.domain.PageRequest.of(0, 2);
        Page<Application> firstPage = new PageImpl<>(
                allApplications.subList(0, 2),
                firstPageRequest,
                allApplications.size()
        );

        when(applicationRepository.findApplicationsByProjectId(eq(1L), eq(firstPageRequest)))
                .thenReturn(firstPage);

        Page<Application> result = applicationService.getAllApplicationsByProjectId(1L, firstPageRequest);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        verify(applicationRepository).findApplicationsByProjectId(eq(1L), eq(firstPageRequest));

        Pageable secondPageRequest = org.springframework.data.domain.PageRequest.of(1, 2);
        Page<Application> secondPage = new PageImpl<>(
                allApplications.subList(2, 3),
                secondPageRequest,
                allApplications.size()
        );

        when(applicationRepository.findApplicationsByProjectId(eq(1L), eq(secondPageRequest)))
                .thenReturn(secondPage);

        result = applicationService.getAllApplicationsByProjectId(1L, secondPageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        verify(applicationRepository).findApplicationsByProjectId(eq(1L), eq(secondPageRequest));
    }

    @Test
    void updateApplicationStatus_Success_ToAccepted() {
        when(applicationRepository.save(any(Application.class))).thenReturn(mockApplication);

        applicationService.updateApplicationStatus(mockApplication, ApplicationStatus.ACCEPTED);

        verify(applicationRepository).save(applicationCaptor.capture());
        Application savedApplication = applicationCaptor.getValue();
        assertThat(savedApplication.getApplicationStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void updateApplicationStatus_Success_ToRejected() {
        when(applicationRepository.save(any(Application.class))).thenReturn(mockApplication);

        applicationService.updateApplicationStatus(mockApplication, ApplicationStatus.REJECTED);

        verify(applicationRepository).save(applicationCaptor.capture());
        Application savedApplication = applicationCaptor.getValue();
        assertThat(savedApplication.getApplicationStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void updateApplicationStatus_SameStatus_ThrowsException() {
        mockApplication.setApplicationStatus(ApplicationStatus.ACCEPTED);

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(mockApplication, ApplicationStatus.ACCEPTED))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 409)
                .hasMessage("Application is already in status: ACCEPTED");
    }

    @Test
    void updateApplicationStatus_FromRejected_ThrowsException() {
        mockApplication.setApplicationStatus(ApplicationStatus.REJECTED);

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(mockApplication, ApplicationStatus.ACCEPTED))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 403)
                .hasMessage("Application is rejected and cannot be updated.");
    }

    @Test
    void updateApplicationStatus_FromAccepted_ThrowsException() {
        mockApplication.setApplicationStatus(ApplicationStatus.ACCEPTED);

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(mockApplication, ApplicationStatus.REJECTED))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 403)
                .hasMessage("Application is accepted and cannot be updated.");
    }
}