package com.example.mediacmslab.controller;

import com.example.mediacmslab.dto.ContentResponse;
import com.example.mediacmslab.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/me")
    public ResponseEntity<List<ContentResponse>> getMyRecommendations() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        var contents = recommendationService.getRecommendations(email);

        return ResponseEntity.ok(List.of()); // Заглушка, мы вернемся к этому
    }
}
