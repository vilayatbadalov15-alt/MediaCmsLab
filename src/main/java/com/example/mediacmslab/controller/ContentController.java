package com.example.mediacmslab.controller;

import com.example.mediacmslab.dto.ContentRequest;
import com.example.mediacmslab.dto.ContentResponse;
import com.example.mediacmslab.service.ContentService;
import com.example.mediacmslab.service.AnalyticsService;
import com.example.mediacmslab.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping
    public ResponseEntity<Page<ContentResponse>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contentService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<ContentResponse> create(@Valid @RequestBody ContentRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.create(request, email));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<ContentResponse> update(@PathVariable Long id, @Valid @RequestBody ContentRequest request) {
        String email = getCurrentUserEmail();
        boolean isAdmin = isCurrentUserAdmin();
        return ResponseEntity.ok(contentService.update(id, request, email, isAdmin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        boolean isAdmin = isCurrentUserAdmin();
        contentService.delete(id, email, isAdmin);
        return ResponseEntity.noContent().build();
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private final AnalyticsService analyticsService;
    private final RecommendationService recommendationService;

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> viewContent(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        analyticsService.recordView(id, email);
        if (email != null) {
            recommendationService.updateUserInterests(email, id);
        }
        return ResponseEntity.ok().build();
    }
}
