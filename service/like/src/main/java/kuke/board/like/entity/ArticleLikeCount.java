package kuke.board.like.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "article_like_count")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleLikeCount {
    @Id
    private Long articleId; //shard key
    private Long likeCount;
    @Version
    private Long version;

    //데이터가 아예 없을 때 초기화하는 팩토리 메소드
    public static ArticleLikeCount init(Long articleId, Long likeCount) {
        ArticleLikeCount articleLikeCount = new ArticleLikeCount();
        articleLikeCount.articleId = articleId;
        articleLikeCount.likeCount = likeCount;
        articleLikeCount.version = 0L;
        return articleLikeCount;
    }

    //증가 메소드
    public void increase() {
        this.likeCount++;
    }

    //감소
    public void decrease() {
        this.likeCount--;
    }
}
