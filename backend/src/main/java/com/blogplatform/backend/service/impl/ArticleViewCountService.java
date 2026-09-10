package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.entity.ArticleViewDelta;
import com.blogplatform.backend.mapper.ArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ArticleViewCountService {

    private static final Logger log = LoggerFactory.getLogger(ArticleViewCountService.class);
    private static final String PENDING_KEY = "article:view:pending";

    private static final DefaultRedisScript<List> DRAIN_SCRIPT = new DefaultRedisScript<>("""
            local entries = redis.call('HGETALL', KEYS[1])
            if #entries > 0 then
                redis.call('DEL', KEYS[1])
            end
            return entries
            """, List.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ArticleMapper articleMapper;

    @Value("${article.view.flush-interval-ms:30000}")
    private long flushIntervalMs;

    @Value("${article.view.flush-threshold:100}")
    private long flushThreshold;

    private final AtomicBoolean flushRequested = new AtomicBoolean();
    private volatile long lastFlushTime = System.currentTimeMillis();

    public void recordView(Integer articleId) {
        try {
            Long pendingCount = redisTemplate.opsForHash()
                    .increment(PENDING_KEY, articleId.toString(), 1L);
            if (pendingCount != null && pendingCount >= Math.max(1, flushThreshold)) {
                flushRequested.set(true);
            }
        } catch (DataAccessException ex) {
            log.warn("Redis is unavailable; writing article view directly to MySQL: articleId={}", articleId);
            articleMapper.incrementViewCount(articleId);
        }
    }

    @Scheduled(fixedDelayString = "${article.view.check-interval-ms:1000}")
    public void flushWhenNeeded() {
        long now = System.currentTimeMillis();
        boolean reachedThreshold = flushRequested.getAndSet(false);
        if (!reachedThreshold && now - lastFlushTime < Math.max(1, flushIntervalMs)) return;

        try {
            flushPendingViews();
            lastFlushTime = now;
        } catch (RuntimeException ex) {
            flushRequested.set(true);
            log.error("Failed to flush article views; restored increments will be retried", ex);
        }
    }

    private void flushPendingViews() {
        List<?> entries = redisTemplate.execute(DRAIN_SCRIPT, List.of(PENDING_KEY));
        if (entries == null || entries.isEmpty()) return;

        List<ArticleViewDelta> deltas = toDeltas(entries);
        try {
            articleMapper.incrementViewCounts(deltas);
        } catch (RuntimeException ex) {
            restore(deltas);
            throw ex;
        }
    }

    private List<ArticleViewDelta> toDeltas(List<?> entries) {
        List<ArticleViewDelta> deltas = new ArrayList<>(entries.size() / 2);
        for (int i = 0; i < entries.size(); i += 2) {
            deltas.add(new ArticleViewDelta(
                    Integer.valueOf(asString(entries.get(i))),
                    Long.parseLong(asString(entries.get(i + 1)))
            ));
        }
        return deltas;
    }

    private void restore(List<ArticleViewDelta> deltas) {
        for (ArticleViewDelta delta : deltas) {
            redisTemplate.opsForHash().increment(
                    PENDING_KEY,
                    delta.articleId().toString(),
                    delta.increment()
            );
        }
    }

    private String asString(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return value.toString();
    }
}
