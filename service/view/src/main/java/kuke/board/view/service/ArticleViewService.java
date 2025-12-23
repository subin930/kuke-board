package kuke.board.view.service;

import kuke.board.view.repository.ArticleViewCountRepository;
import kuke.board.view.repository.ArticleViewDistributedLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ArticleViewService {
    private final ArticleViewDistributedLockRepository articleViewDistributedLockRepository;
    private final ArticleViewCountRepository articleViewCountRepository;
    private final ArticleViewCountBackUpProcessor articleViewCountBackUpProcessor;

    private static final int BACKUP_BACH_SIZE = 100; //개수 단위 백업, 조회수가 100의 배수가 될 때마다 백업
    private static final Duration TTL = Duration.ofMinutes(10); //10분

    public Long increase(Long articleId, Long userId) {
        if(!articleViewDistributedLockRepository.lock(articleId, userId, TTL)) {
            //락 획득 실패 시 증가X, 현재 조회수 그대로 반환
            return articleViewCountRepository.read(articleId);
        }

        Long count = articleViewCountRepository.increase(articleId);

        if(count % BACKUP_BACH_SIZE == 0){
            articleViewCountBackUpProcessor.backUp(articleId, count);
        }

        return count;
    }

    public Long count(Long articleId) {
        return articleViewCountRepository.read(articleId);
    }
}
