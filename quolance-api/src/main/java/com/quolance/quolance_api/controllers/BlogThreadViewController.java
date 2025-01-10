package com.quolance.quolance_api.controllers;

import com.quolance.quolance_api.dtos.blog.BlogThreadViewRequestDto;
import com.quolance.quolance_api.dtos.blog.BlogThreadViewResponseDto;
import com.quolance.quolance_api.dtos.blog.UpdateBlogThreadViewTitleDto;
import com.quolance.quolance_api.services.entity_services.BlogThreadViewService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/blog-thread-views")
@RequiredArgsConstructor
public class BlogThreadViewController {

    private final BlogThreadViewService blogThreadViewService;

    @PostMapping
    @Operation(summary = "Create a blog thread view with an associated blog post")
    public ResponseEntity<BlogThreadViewResponseDto> createThreadView(
            @RequestBody BlogThreadViewRequestDto request) {
        BlogThreadViewResponseDto response = blogThreadViewService.create(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all blog thread views with pagination")
    public ResponseEntity<Page<BlogThreadViewResponseDto>> getAllThreadViews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogThreadViewResponseDto> response = blogThreadViewService.getAll(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a blog thread view by ID")
    public ResponseEntity<BlogThreadViewResponseDto> getThreadViewById(@PathVariable Long id) {
        BlogThreadViewResponseDto response = blogThreadViewService.getBlogThreadById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/blog-post/{blogPostId}")
    @Operation(summary = "Get a blog thread view by BlogPost ID")
    public ResponseEntity<BlogThreadViewResponseDto> getThreadViewByBlogPostId(@PathVariable Long blogPostId) {
        BlogThreadViewResponseDto response = blogThreadViewService.getThreadViewByBlogPostId(blogPostId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update the title of a blog thread view")
    public ResponseEntity<BlogThreadViewResponseDto> updateThreadViewTitle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBlogThreadViewTitleDto request) {
        BlogThreadViewResponseDto response = blogThreadViewService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a blog thread view and its associated blog post")
    public ResponseEntity<Void> deleteThreadView(@PathVariable Long id) {
        blogThreadViewService.delete(id);
        return ResponseEntity.noContent().build(); // Return 204 No Content
    }
}