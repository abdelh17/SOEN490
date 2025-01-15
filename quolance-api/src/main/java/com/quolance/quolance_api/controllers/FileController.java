package com.quolance.quolance_api.controllers;

import com.quolance.quolance_api.dtos.FileDto;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.services.entity_services.FileService;
import com.quolance.quolance_api.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(
            summary = "Upload a file",
            description = "Uploads a file to the server and returns the upload result."
    )
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file){
        try {
            User uploadedBy = SecurityUtil.getAuthenticatedUser();
            Map<String, Object> uploadResult = fileService.uploadFile(file,uploadedBy);
            return ResponseEntity.ok(uploadResult.get("secure_url").toString());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid file type");
        }
    }

    @GetMapping("/all")
    @Operation(
            summary = "Get All User Files",
            description = "Returns all files uploaded by the authenticated user with pagination support."
    )
    public ResponseEntity<Page<FileDto>> getAllUserFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ){
        User user = SecurityUtil.getAuthenticatedUser();
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<FileDto> files = fileService.getAllFileUploadsByUser(user, pageRequest);
        return ResponseEntity.ok(files);
    }
}