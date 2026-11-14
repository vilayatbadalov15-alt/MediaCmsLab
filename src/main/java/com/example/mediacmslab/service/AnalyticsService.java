package com.example.mediacmslab.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final StringRedisTemplate redisTemplate;

    private String getDateKey() {
        return LocalDate.now().toString(); // формат YYYY-MM-DD
    }

    public void recordView(Long contentId, String userEmail) {
        String today = getDateKey();

        redisTemplate.opsForValue().increment("analytics:views:content:" + contentId);

        redisTemplate.opsForZSet().incrementScore("analytics:top:views:" + today, contentId.toString(), 1);

        String uniqueUserKey = (userEmail != null) ? userEmail : "anonymous_" + System.currentTimeMillis();
        redisTemplate.opsForHyperLogLog().add("analytics:dau:" + today, uniqueUserKey);
    }

    public Long getViews(Long contentId) {
        String val = redisTemplate.opsForValue().get("analytics:views:content:" + contentId);
        return val != null ? Long.parseLong(val) : 0L;
    }

    public Set<String> getTopContentToday() {
        String today = getDateKey();
        return redisTemplate.opsForZSet().reverseRange("analytics:top:views:" + today, 0, 9);
    }

    public Long getDailyActiveUsers() {
        String today = getDateKey();
        return redisTemplate.opsForHyperLogLog().size("analytics:dau:" + today);
    }
}
