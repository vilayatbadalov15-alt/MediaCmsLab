package com.example.mediacmslab.service;

import com.example.mediacmslab.entity.Content;
import com.example.mediacmslab.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final StringRedisTemplate redis;
    private final ContentRepository contents;
    private static final String PREFIX = "{analytics}:";
    private static final long UNKNOWN = -1;
    private static final long HIDDEN = -2;

    // A cache miss never increments. Only a successful SQL check can prime the marker.
    private static final DefaultRedisScript<Long> VIEW = new DefaultRedisScript<>("""
            local visible = redis.call('GET', KEYS[4])
            if visible == '0' then return -2 end
            if not visible then
                if ARGV[3] ~= 'verified' then return -1 end
                redis.call('SET', KEYS[4], '1', 'EX', 30)
            end
            local total = redis.call('INCR', KEYS[1])
            redis.call('ZINCRBY', KEYS[2], 1, ARGV[1])
            redis.call('EXPIRE', KEYS[2], 172800)
            if ARGV[2] ~= '' then
                redis.call('PFADD', KEYS[3], ARGV[2])
                redis.call('EXPIRE', KEYS[3], 172800)
            end
            return total
            """, Long.class);

    private static final DefaultRedisScript<Long> WITHDRAW = new DefaultRedisScript<>("""
            redis.call('SET', KEYS[4], '0', 'EX', 30)
            redis.call('ZREM', KEYS[2], ARGV[1])
            if ARGV[2] == 'true' then redis.call('DEL', KEYS[1]) end
            return 1
            """, Long.class);

    private static String today() {
        return LocalDate.now(ZoneOffset.UTC).toString();
    }

    private static List<String> keys(Long id) {
        String date = today();
        return List.of(PREFIX + "views:content:" + id, PREFIX + "top:views:" + date,
                PREFIX + "dau:" + date, PREFIX + "published:" + id);
    }

    public Long recordView(Long id, String email) {
        var keys = keys(id);
        String reader = email == null ? "" : email;
        long total = analytics(() -> redis.execute(VIEW, keys, id.toString(), reader, ""));
        if (total == UNKNOWN && contents.existsByIdAndStatus(id, Content.Status.PUBLISHED)) {
            total = analytics(() -> redis.execute(VIEW, keys, id.toString(), reader, "verified"));
        }
        if (total == UNKNOWN || total == HIDDEN) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return total;
    }

    public Set<String> getTopContentToday() {
        return analytics(() -> redis.opsForZSet().reverseRange(PREFIX + "top:views:" + today(), 0, 9));
    }

    public Long getDailyActiveUsers() {
        return analytics(() -> redis.opsForHyperLogLog().size(PREFIX + "dau:" + today()));
    }

    private static <T> T analytics(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Аналитика временно недоступна", error);
        }
    }

    @TransactionalEventListener
    void updateVisibility(ContentService.VisibilityChanged event) {
        var keys = keys(event.id());
        try {
            if (event.published()) {
                redis.delete(keys.get(3));
            } else {
                redis.execute(WITHDRAW, keys, event.id().toString(), String.valueOf(event.deleted()));
            }
        } catch (DataAccessException error) {
            log.warn("Redis unavailable after content {} changed visibility", event.id(), error);
        }
    }
}
