package com.example.mediacmslab.controller;

import com.example.mediacmslab.dto.ContentResponse;
import com.example.mediacmslab.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendations;

    @GetMapping({"/api/recommendations/top", "/api/recommendations/me"})
    public List<ContentResponse> top() {
        return recommendations.getRecommendations();
    }
}
