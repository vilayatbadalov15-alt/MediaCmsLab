package com.example.mediacmslab.service;

import com.example.mediacmslab.entity.Content;
import com.example.mediacmslab.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final StringRedisTemplate redisTemplate;
    private final ContentRepository contentRepository;

    public void updateUserInterests(String userEmail, Long contentId) {
        Content content = contentRepository.findById(contentId).orElse(null);
        if (content == null) return;

        for (var tag : content.getTags()) {
            redisTemplate.opsForZSet().incrementScore("rec:user:" + userEmail + ":tags", tag.getName(), 1.0);
        }
    }

    public List<Content> getRecommendations(String userEmail) {
        Set<String> topTags = redisTemplate.opsForZSet().reverseRange("rec:user:" + userEmail + ":tags", 0, 2);

        if (topTags == null || topTags.isEmpty()) {
            return contentRepository.findAll().stream()
                    .filter(c -> c.getStatus() == Content.Status.PUBLISHED)
                    .limit(5)
                    .collect(Collectors.toList());
        }

        return contentRepository.findAll().stream()
                .filter(c -> c.getStatus() == Content.Status.PUBLISHED)
                .filter(c -> c.getTags().stream().anyMatch(tag -> topTags.contains(tag.getName())))
                .limit(10)
                .collect(Collectors.toList());
    }
}
