package kuke.board.view.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class ArticleViewDistributedLockRepository {
    private final StringRedisTemplate redisTemplate;

    // view::article::{article_id}::user::{user_id}::lock
    private static final String KEY_FORMAT = "view::article::%s::user::%s::lock";

    // 락 획득 메서드
    public boolean lock(Long articleId, Long userId, Duration ttl) {
        String key = generateKey(articleId, userId);

        return redisTemplate.opsForValue().setIfAbsent(key, "", ttl); //키가 새로 생성이 되었을 경우만 true 반환(lock get)
    }

    private String generateKey(Long articleId, Long userId) {
        return KEY_FORMAT.formatted(articleId, userId);
    }
}
