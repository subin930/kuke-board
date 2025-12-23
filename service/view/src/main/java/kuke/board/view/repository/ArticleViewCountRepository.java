package kuke.board.view.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleViewCountRepository {
    private final StringRedisTemplate redisTemplate; //redis 의존성 추가 시 자동으로 만들어짐

    //view::article::{article_id}::view_count
    private static final String KEY_FORMAT = "view::article::%s::view_count ";

    public Long read(Long articleId) {
        String result = redisTemplate.opsForValue().get(generateKey(articleId));

        return result == null ? 0L : Long.valueOf(result);
    }

    public Long increase(Long articleId) {
        return redisTemplate.opsForValue().increment(generateKey(articleId)); //증가된 이후의 값 반환
    }

    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }
}
