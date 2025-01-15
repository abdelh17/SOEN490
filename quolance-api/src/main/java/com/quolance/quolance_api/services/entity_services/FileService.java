package com.quolance.quolance_api.services.entity_services;

import com.quolance.quolance_api.dtos.FileDto;
import com.quolance.quolance_api.entities.User;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface FileService {

    Map<String, Object> uploadFile(MultipartFile file, User uploadedBy);
    Page<FileDto> getAllFileUploadsByUser(User user, Pageable pageable);
}