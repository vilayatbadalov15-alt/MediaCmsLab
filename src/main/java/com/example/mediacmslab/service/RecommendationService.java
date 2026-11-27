package com.example.mediacmslab.service;

import com.example.mediacmslab.dto.ContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final AnalyticsService analytics;
    private final ContentService contents;

    public List<ContentResponse> getRecommendations() {
        var ids = analytics.getTopContentToday().stream().map(Long::valueOf).toList();
        return contents.getPublishedInOrder(ids);
    }
}
