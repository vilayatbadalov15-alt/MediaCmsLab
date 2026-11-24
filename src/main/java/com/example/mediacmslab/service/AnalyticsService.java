package com.example.mediacmslab.service;

import com.example.mediacmslab.entity.Content;
import com.example.mediacmslab.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final StringRedisTemplate redis;
    private final ContentRepository contents;

    // Keep the counter, ranking and expiry in sync for each accepted view.
    private static final DefaultRedisScript<Long> VIEW = new DefaultRedisScript<>("""
            local total = redis.call('INCR', KEYS[1])
            redis.call('ZINCRBY', KEYS[2], 1, ARGV[1])
            redis.call('EXPIRE', KEYS[2], 172800)
            if ARGV[2] ~= '' then
                redis.call('PFADD', KEYS[3], ARGV[2])
                redis.call('EXPIRE', KEYS[3], 172800)
            end
            return total
            """, Long.class);

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    @Transactional
    public Long recordView(Long id, String email) {
        contents.findPublishedForView(id, Content.Status.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDate date = today();
        return redis.execute(VIEW, List.of("analytics:views:content:" + id,
                "analytics:top:views:" + date, "analytics:dau:" + date), id.toString(), email == null ? "" : email);
    }

    public Set<String> getTopContentToday() {
        return redis.opsForZSet().reverseRange("analytics:top:views:" + today(), 0, 9);
    }

    public Long getDailyActiveUsers() {
        return redis.opsForHyperLogLog().size("analytics:dau:" + today());
    }

    @TransactionalEventListener
    void removeFromRanking(ContentService.Withdrawn event) {
        redis.opsForZSet().remove("analytics:top:views:" + today(), event.id().toString());
        if (event.deleted()) redis.delete("analytics:views:content:" + event.id());
    }
}
