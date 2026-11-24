package com.example.mediacmslab.service;

import com.example.mediacmslab.dto.ContentResponse;
import com.example.mediacmslab.entity.Content;
import com.example.mediacmslab.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final AnalyticsService analytics;
    private final ContentRepository contents;

    @Transactional(readOnly = true)
    public List<ContentResponse> getRecommendations() {
        var ids = analytics.getTopContentToday().stream().map(Long::valueOf).toList();
        var byId = contents.findAllById(ids).stream()
                .filter(content -> content.getStatus() == Content.Status.PUBLISHED)
                .collect(Collectors.toMap(Content::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(ContentService::mapToResponse).toList();
    }
}
