package com.quolance.quolance_api.unit.controllers;

import com.quolance.quolance_api.controllers.FileController;
import com.quolance.quolance_api.dtos.FileDto;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.Role;
import com.quolance.quolance_api.services.entity_services.FileService;
import com.quolance.quolance_api.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileControllerUnitTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileController fileController;

    private User mockUser;
    private MultipartFile mockFile;
    private FileDto fileDto1;
    private FileDto fileDto2;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setRole(Role.FREELANCER);

        mockFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        fileDto1 = FileDto.builder()
                .id(1L)
                .fileName("test1.pdf")
                .fileUrl("http://example.com/test1.pdf")
                .build();

        fileDto2 = FileDto.builder()
                .id(2L)
                .fileName("test2.pdf")
                .fileUrl("http://example.com/test2.pdf")
                .build();
    }

    @Test
    void uploadFile_SuccessfulUpload_ReturnsUrl() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockUser);

            Map<String, Object> uploadResult = new HashMap<>();
            uploadResult.put("secure_url", "http://example.com/file.pdf");

            when(fileService.uploadFile(eq(mockFile), any(User.class)))
                    .thenReturn(uploadResult);

            ResponseEntity<String> response = fileController.uploadFile(mockFile);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("http://example.com/file.pdf");
            verify(fileService).uploadFile(eq(mockFile), eq(mockUser));
        }
    }

    @Test
    void uploadFile_InvalidFileType_ReturnsBadRequest() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockUser);

            when(fileService.uploadFile(eq(mockFile), any(User.class)))
                    .thenThrow(new IllegalArgumentException("Invalid file type"));

            ResponseEntity<String> response = fileController.uploadFile(mockFile);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isEqualTo("Invalid file type");
            verify(fileService).uploadFile(eq(mockFile), eq(mockUser));
        }
    }

    @Test
    void getAllUserFiles_ReturnsPageOfFiles() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockUser);

            List<FileDto> files = Arrays.asList(fileDto1, fileDto2);
            Page<FileDto> filePage = new PageImpl<>(files);
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

            when(fileService.getAllFileUploadsByUser(eq(mockUser), eq(pageRequest)))
                    .thenReturn(filePage);

            ResponseEntity<Page<FileDto>> response = fileController.getAllUserFiles(0, 10, "id", "desc");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(2);
            assertThat(response.getBody().getContent()).containsExactly(fileDto1, fileDto2);
            verify(fileService).getAllFileUploadsByUser(eq(mockUser), eq(pageRequest));
        }
    }

    @Test
    void getAllUserFiles_ReturnsEmptyPage_WhenNoFiles() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockUser);

            Page<FileDto> emptyPage = new PageImpl<>(List.of());
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

            when(fileService.getAllFileUploadsByUser(eq(mockUser), eq(pageRequest)))
                    .thenReturn(emptyPage);

            ResponseEntity<Page<FileDto>> response = fileController.getAllUserFiles(0, 10, "id", "desc");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).isEmpty();
            verify(fileService).getAllFileUploadsByUser(eq(mockUser), eq(pageRequest));
        }
    }

    @Test
    void getAllUserFiles_WithCustomPageSize_ReturnsCorrectPageSize() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockUser);

            List<FileDto> files = Arrays.asList(fileDto1, fileDto2);
            Page<FileDto> filePage = new PageImpl<>(files);
            PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));

            when(fileService.getAllFileUploadsByUser(eq(mockUser), eq(pageRequest)))
                    .thenReturn(filePage);

            ResponseEntity<Page<FileDto>> response = fileController.getAllUserFiles(0, 5, "id", "desc");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            verify(fileService).getAllFileUploadsByUser(eq(mockUser), argThat(pr ->
                    pr.getPageSize() == 5
            ));
        }
    }

    @Test
    void getAllUserFiles_WithCustomSortField_ReturnsSortedResults() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockUser);

            List<FileDto> files = Arrays.asList(fileDto1, fileDto2);
            Page<FileDto> filePage = new PageImpl<>(files);
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fileName"));

            when(fileService.getAllFileUploadsByUser(eq(mockUser), eq(pageRequest)))
                    .thenReturn(filePage);

            ResponseEntity<Page<FileDto>> response = fileController.getAllUserFiles(0, 10, "fileName", "desc");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            verify(fileService).getAllFileUploadsByUser(eq(mockUser), argThat(pr ->
                    pr.getSort().getOrderFor("fileName") != null
            ));
        }
    }

    @Test
    void getAllUserFiles_WithAscendingSort_ReturnsSortedResults() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getAuthenticatedUser).thenReturn(mockUser);

            List<FileDto> files = Arrays.asList(fileDto1, fileDto2);
            Page<FileDto> filePage = new PageImpl<>(files);
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));

            when(fileService.getAllFileUploadsByUser(eq(mockUser), eq(pageRequest)))
                    .thenReturn(filePage);

            ResponseEntity<Page<FileDto>> response = fileController.getAllUserFiles(0, 10, "id", "asc");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            verify(fileService).getAllFileUploadsByUser(eq(mockUser), argThat(pr ->
                    pr.getSort().getOrderFor("id").getDirection() == Sort.Direction.ASC
            ));
        }
    }
}