package kuke.board.comment.service;

import kuke.board.comment.entity.Comment;
import kuke.board.comment.repository.CommentRepository;
import kuke.board.comment.service.request.CommentCreateRequest;
import kuke.board.comment.service.response.CommentPageResponse;
import kuke.board.comment.service.response.CommentResponse;
import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final Snowflake snowflake = new Snowflake();
    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponse create(CommentCreateRequest request) {
        Comment parent = findParent(request);

        Comment comment = commentRepository.save(
                Comment.create(
                        snowflake.nextId(),
                        request.getContent(),
                        parent == null ? null : parent.getCommentId(),
                        request.getArticleId(),
                        request.getWriterId()
                )
        );

        return CommentResponse.from(comment);
    }

    private Comment findParent(CommentCreateRequest request) {
        Long parentCommentId = request.getParentCommentId();

        if(parentCommentId == null){
            //상위 댓글이 없는 경우
            return null;
        }

        return commentRepository.findById(parentCommentId)
                .filter(not(Comment::getDeleted))
                .filter(Comment::isRoot)
                .orElseThrow();
    }

    public CommentResponse read(Long commentId) {
        return CommentResponse.from(
                commentRepository.findById(commentId).orElseThrow()
        );
    }

    @Transactional
    public void delete(Long commentId) {
        commentRepository.findById(commentId)
                .filter(not(Comment::getDeleted))
                .ifPresent(comment -> {
                    if (hasChildren(comment)) {
                        //하위 댓글 존재: 칼럼만 deleted = true로 변경
                        comment.delete();
                    } else {
                        //실제로 삭제
                        delete(comment);
                    }
                });
    }

    private boolean hasChildren(Comment comment) {
        //limit를 2로 설정: 본인 댓글 1개 + 하위 댓글 1개 / 즉, 2개 이상 있으면 하위 댓글이 존재하는 것이기 떄문
        return commentRepository.countBy(comment.getArticleId(), comment.getCommentId(), 2L) == 2;
    }

    private void delete(Comment comment) {
        commentRepository.delete(comment);

        if(!comment.isRoot()) {
            //상위 댓글들을 재귀적으로 삭제
            commentRepository.findById(comment.getParentCommentId())
                    .filter(Comment::getDeleted)
                    .filter(not(this::hasChildren))
                    .ifPresent(this::delete);
        }
    }

    //페이지 방식 댓글 목록 조회
    public CommentPageResponse readAll(
            Long articleId,
            Long page,
            Long pageSize
    ) {
        return CommentPageResponse.of(
                commentRepository.findAll(articleId, (page - 1) * pageSize, pageSize).stream()
                        .map(CommentResponse::from)
                        .toList(),
                commentRepository.count(articleId, PageLimitCalculator.calculatePageLimit(page, pageSize, 10L))
        );
    }

    //무한 스크롤 방식 댓글 목록 조회
    public List<CommentResponse> InfiniteScroll(
            Long articleId,
            Long lastParentCommentId,
            Long lastCommentId,
            Long limit
    ) {
        List<Comment> comments = (lastParentCommentId == null && lastCommentId == null) ?
                commentRepository.findAllInfiniteScroll(articleId, limit) :
                commentRepository.findAllInfiniteScrll(articleId, lastParentCommentId, lastCommentId, limit);
        return comments.stream()
                .map(CommentResponse::from)
                .toList();
    }
}
