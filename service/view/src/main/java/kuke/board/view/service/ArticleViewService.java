package kuke.board.view.service;

import kuke.board.view.repository.ArticleViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleViewService {
    private final ArticleViewCountRepository articleViewCountRepository;
    private final ArticleViewCountBackUpProcessor articleViewCountBackUpProcessor;
    private static final int BACKUP_BACH_SIZE = 100; //개수 단위 백업, 조회수가 100의 배수가 될 때마다 백업

    public Long increase(Long articleId, Long userId) {
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
