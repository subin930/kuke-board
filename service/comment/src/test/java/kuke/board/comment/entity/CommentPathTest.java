package kuke.board.comment.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class CommentPathTest {
    @Test
    void createChildCommentTest() {
        //00000이 생성되는 상황
        createChildCommentTest(CommentPath.create(""), null, "00000");

        //00000
        //     00000 생성되는 상황
        createChildCommentTest(CommentPath.create("00000"), null, "0000000000");

        //00000
        //00001 생성
        createChildCommentTest(CommentPath.create(""), "00000", "00001");

        //0000z
        //     abcdz
        //          zzzzz
        //               zzzzz
        //     abce0 생성되는 상황
        createChildCommentTest(CommentPath.create("0000z"), "0000zabcdzzzzzzzzzzz", "0000zabce0");
    }

    void createChildCommentTest(CommentPath commentPath, String descendantsTopPath, String expectedChildPath) {
        CommentPath childCommentPath = commentPath.createChildCommentPath(descendantsTopPath);
        assertThat(childCommentPath.getPath()).isEqualTo(expectedChildPath);
    }

    //max depth 예외 테스트 -> 현재 "zzzzzzzzzzzzzzzzzzzzzzzzz"여서 더 이상 하위 댓글 생성이 불가능한 상황
    @Test
    void createChildCommentPathIfMaxDepthTest() {
        //create 메서드 내 IllegalStateException("depth overflowed")에서 예외 발생
        assertThatThrownBy(() ->
                CommentPath.create("zzzzz".repeat(5)).createChildCommentPath(null)
        ).isInstanceOf(IllegalStateException.class);
    }

    //chunk 내에서 overflow 테스트
    @Test
    void createChildCommentPathIfChunkOverflowTest() {
        //increase 메서드 내 throw new IllegalStateException("chunk overflowed")에서 예외 발생
        assertThatThrownBy(() ->
                CommentPath.create("abced").createChildCommentPath("abcdezzzzz")
        ).isInstanceOf(IllegalStateException.class);
    }
}