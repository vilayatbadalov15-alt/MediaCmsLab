package com.example.mediacmslab.controller;

import com.example.mediacmslab.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/top")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Set<String>> getTopContent() {
        return ResponseEntity.ok(analyticsService.getTopContentToday());
    }

    @GetMapping("/dau")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Long> getDailyActiveUsers() {
        return ResponseEntity.ok(analyticsService.getDailyActiveUsers());
    }
}
