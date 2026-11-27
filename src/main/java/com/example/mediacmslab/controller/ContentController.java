package com.example.mediacmslab.controller;

import com.example.mediacmslab.dto.ContentRequest;
import com.example.mediacmslab.dto.ContentResponse;
import com.example.mediacmslab.entity.Content;
import com.example.mediacmslab.service.AnalyticsService;
import com.example.mediacmslab.service.ContentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {
    private final ContentService contentService;
    private final AnalyticsService analytics;

    @GetMapping
    public Page<ContentResponse> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {
        return contentService.getAll(pageable, email(auth), isAdmin(auth));
    }

    @GetMapping("/{id}")
    public ContentResponse getById(@PathVariable Long id, Authentication auth) {
        ContentResponse content = contentService.getById(id, contentService.revision(id));
        if (content.getStatus() != Content.Status.PUBLISHED &&
                !content.getAuthorEmail().equals(email(auth)) && !isAdmin(auth)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return content;
    }

    @SecurityRequirement(name = "jwt")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse create(@Valid @RequestBody ContentRequest request, Authentication auth) {
        return contentService.create(request, auth.getName());
    }

    @SecurityRequirement(name = "jwt")
    @PutMapping("/{id}")
    public ContentResponse update(@PathVariable Long id, @Valid @RequestBody ContentRequest request, Authentication auth) {
        return contentService.update(id, request, auth.getName(), isAdmin(auth));
    }

    @SecurityRequirement(name = "jwt")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        contentService.delete(id, auth.getName(), isAdmin(auth));
    }

    @PostMapping("/{id}/view")
    public Long viewContent(@PathVariable Long id, Authentication auth) {
        return analytics.recordView(id, email(auth));
    }

    private static String email(Authentication auth) {
        return auth == null ? null : auth.getName();
    }

    private static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
