package com.quolance.quolance_api.unit.services.entity_services;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.quolance.quolance_api.dtos.FileDto;
import com.quolance.quolance_api.entities.FileEntity;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.repositories.FileRepository;
import com.quolance.quolance_api.util.exceptions.ApiException;
import com.quolance.quolance_api.services.entity_services.impl.FileServiceImpl;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceUnitTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private FileServiceImpl fileService;

    @Captor
    private ArgumentCaptor<FileEntity> fileEntityCaptor;

    private User mockUser;
    private MultipartFile mockImageFile;
    private MultipartFile mockDocFile;
    private Map<String, Object> mockCloudinaryResponse;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .build();

        mockImageFile = new MockMultipartFile(
                "test-image.jpg",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockDocFile = new MockMultipartFile(
                "test-doc.pdf",
                "test-doc.pdf",
                "application/pdf",
                "test doc content".getBytes()
        );

        mockCloudinaryResponse = new HashMap<>();
        mockCloudinaryResponse.put("secure_url", "https://cloudinary.com/test-url");
    }

    @Test
    void uploadFile_SuccessWithImage() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(mockCloudinaryResponse);
        when(fileRepository.save(any(FileEntity.class)))
                .thenReturn(new FileEntity());

        Map<String, Object> result = fileService.uploadFile(mockImageFile, mockUser);

        verify(uploader).upload(
                any(byte[].class),
                eq(Map.of("resource_type", "auto", "folder", "images"))
        );

        verify(fileRepository).save(fileEntityCaptor.capture());
        FileEntity savedFile = fileEntityCaptor.getValue();

        assertThat(savedFile.getUser()).isEqualTo(mockUser);
        assertThat(savedFile.getFileUrl()).isEqualTo("https://cloudinary.com/test-url");
        assertThat(savedFile.getFileType()).isEqualTo("image/jpeg");
        assertThat(savedFile.getFileName()).isEqualTo("test-image.jpg");
        assertThat(result).isEqualTo(mockCloudinaryResponse);
    }

    @Test
    void uploadFile_SuccessWithDocument() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(mockCloudinaryResponse);
        when(fileRepository.save(any(FileEntity.class)))
                .thenReturn(new FileEntity());

        Map<String, Object> result = fileService.uploadFile(mockDocFile, mockUser);

        verify(uploader).upload(
                any(byte[].class),
                eq(Map.of("resource_type", "auto", "folder", "files"))
        );

        verify(fileRepository).save(fileEntityCaptor.capture());
        FileEntity savedFile = fileEntityCaptor.getValue();

        assertThat(savedFile.getUser()).isEqualTo(mockUser);
        assertThat(savedFile.getFileType()).isEqualTo("application/pdf");
        assertThat(savedFile.getFileName()).isEqualTo("test-doc.pdf");
        assertThat(result).isEqualTo(mockCloudinaryResponse);
    }

    @Test
    void uploadFile_CloudinaryError_ThrowsException() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenThrow(new IOException("Upload failed"));

        assertThatThrownBy(() -> fileService.uploadFile(mockImageFile, mockUser))
                .isInstanceOf(ApiException.class)
                .hasMessage("Error uploading file");

        verify(fileRepository, never()).save(any(FileEntity.class));
    }

    @Test
    void uploadFile_NullContentType_HandledCorrectly() throws IOException {
        MultipartFile mockFileWithNullType = new MockMultipartFile(
                "test-file",
                "test-file",
                null,
                "test content".getBytes()
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(mockCloudinaryResponse);
        when(fileRepository.save(any(FileEntity.class)))
                .thenReturn(new FileEntity());

        Map<String, Object> result = fileService.uploadFile(mockFileWithNullType, mockUser);

        verify(uploader).upload(
                any(byte[].class),
                eq(Map.of("resource_type", "auto", "folder", "files"))
        );

        assertThat(result).isEqualTo(mockCloudinaryResponse);
    }

    @Test
    void getAllFileUploadsByUser_Success() {
        FileEntity file1 = new FileEntity();
        file1.setId(1L);
        file1.setFileName("file1.jpg");
        file1.setFileUrl("url1");
        file1.setUser(mockUser);

        FileEntity file2 = new FileEntity();
        file2.setId(2L);
        file2.setFileName("file2.pdf");
        file2.setFileUrl("url2");
        file2.setUser(mockUser);

        List<FileEntity> files = Arrays.asList(file1, file2);
        Page<FileEntity> filePage = new PageImpl<>(files);

        when(fileRepository.findFileUploadsByUser(eq(mockUser), any(Pageable.class)))
                .thenReturn(filePage);

        Page<FileDto> result = fileService.getAllFileUploadsByUser(mockUser, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getFileName()).isEqualTo("file1.jpg");
        assertThat(result.getContent().get(1).getFileName()).isEqualTo("file2.pdf");
    }

    @Test
    void getAllFileUploadsByUser_WithPaging_Success() {
        FileEntity file1 = new FileEntity();
        file1.setId(1L);
        file1.setFileName("file1.jpg");

        FileEntity file2 = new FileEntity();
        file2.setId(2L);
        file2.setFileName("file2.pdf");

        FileEntity file3 = new FileEntity();
        file3.setId(3L);
        file3.setFileName("file3.doc");

        List<FileEntity> allFiles = Arrays.asList(file1, file2, file3);

        Pageable pageRequest = PageRequest.of(0, 2);
        Page<FileEntity> firstPage = new PageImpl<>(
                allFiles.subList(0, 2),
                pageRequest,
                allFiles.size()
        );

        when(fileRepository.findFileUploadsByUser(eq(mockUser), eq(pageRequest)))
                .thenReturn(firstPage);

        Page<FileDto> result = fileService.getAllFileUploadsByUser(mockUser, pageRequest);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }
}