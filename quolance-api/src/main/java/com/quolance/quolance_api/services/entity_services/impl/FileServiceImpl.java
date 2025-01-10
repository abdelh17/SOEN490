package com.quolance.quolance_api.services.entity_services.impl;

import com.cloudinary.Cloudinary;
import com.quolance.quolance_api.dtos.FileDto;
import com.quolance.quolance_api.entities.FileEntity;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.repositories.FileRepository;
import com.quolance.quolance_api.services.entity_services.FileService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final Cloudinary cloudinary;
    private final FileRepository fileRepository;

    @Override
    public Map<String, Object> uploadFile(MultipartFile file, User uploadedBy) {

        Map<String, Object> uploadResult ;
        String fileType = file.getContentType();
        String folder = (fileType != null ? fileType.startsWith("image") : false) ? "images" : "files";

        try {
            uploadResult = cloudinary.uploader().upload(file.getBytes(), Map.of(
                    "resource_type", "auto",
                    "folder", folder
            ));

            // save to database
            FileEntity fileEntity = new FileEntity();
            fileEntity.setUser(uploadedBy);
            fileEntity.setFileUrl((String) uploadResult.get("secure_url"));
            fileEntity.setFileType(file.getContentType());
            fileEntity.setFileName(file.getOriginalFilename());

            fileRepository.save(fileEntity);

        } catch (IOException e) {
            throw new ApiException("Error uploading file");
        }

        return  uploadResult;
    }

    @Override
    public Page<FileDto> getAllFileUploadsByUser(User user, Pageable pageable) {
        Page<FileEntity> files = fileRepository.findFileUploadsByUser(user, pageable);
        return files.map(FileDto::fromEntity);
    }
}
