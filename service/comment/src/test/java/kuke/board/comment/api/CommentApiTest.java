package kuke.board.comment.api;

import kuke.board.comment.service.request.CommentCreateRequest;
import kuke.board.comment.service.response.CommentPageResponse;
import kuke.board.comment.service.response.CommentResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

public class CommentApiTest {
    RestClient restClient = RestClient.create("http://localhost:9001");

    @Test
    void create() {
        CommentResponse response1 = createComment(new CommentCreateRequest(1L, "my comment1", null, 1L));
        CommentResponse response2 = createComment(new CommentCreateRequest(1L, "my comment2", response1.getCommentId(), 1L));
        CommentResponse response3 = createComment(new CommentCreateRequest(1L, "my comment3", response1.getCommentId(), 1L));

        System.out.println("commentId=%s".formatted(response1.getCommentId()));
        System.out.println("\tcommentId=%s".formatted(response2.getCommentId()));
        System.out.println("\tcommentId=%s".formatted(response3.getCommentId()));

//        commentId=258920883265941504
//        commentId=258920884264185856
//        commentId=258920884360654848
    }

    CommentResponse createComment(CommentCreateRequest request) {
        return restClient.post()
                .uri("/v1/comments")
                .body(request)
                .retrieve()
                .body(CommentResponse.class);
    }

    @Test
    void read() {
        CommentResponse response = restClient.get()
                .uri("/v1/comments/%s".formatted(258920883265941504L))
                .retrieve()
                .body(CommentResponse.class);

        System.out.println("response= " + response);
    }

    @Test
    void delete() {
        //        commentId=258920883265941504 -> X
        //          commentId=258920884264185856 -> X
        //          commentId=258920884360654848 -> X

        restClient.delete()
                .uri("/v1/comments/{commentId}", 258920884360654848L)
                .retrieve();

    }

    //목록 조회 - 페이지 방식
    @Test
    void readAll() {
        CommentPageResponse response = restClient.get()
                .uri("/v1/comments?articleId=1&page=1&pageSize=10")
                .retrieve()
                .body(CommentPageResponse.class);
        System.out.println("response.getCommentCount() = " + response.getCommentCount());
        for(CommentResponse comment : response.getComments()) {
            if(!comment.getParentCommentId().equals(comment.getCommentId()))
                System.out.print("\t");
            System.out.println("commentId = " + comment.getCommentId());
        }

        /**
         * 1번 페이지 수행 결과
         * response.getCommentCount() = 101
         * commentId = 258924812877602816
         * 	commentId = 258924812974071810
         * commentId = 258924812877602817
         * 	commentId = 258924812974071813
         * commentId = 258924812877602818
         * 	commentId = 258924812974071835
         * commentId = 258924812877602819
         * 	commentId = 258924812974071809
         * commentId = 258924812877602820
         * 	commentId = 258924812974071811
         */
    }


    //목록 조회 - 무한 스크롤 방식
    @Test
    void readAllInfiniteScroll() {
        List<CommentResponse> responses1 = restClient.get()
                .uri("/v1/comments/infinite-scroll?articleId=1&pageSize=5")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponse>>() {});

        System.out.println("first page");

        for(CommentResponse comment : responses1) {
            if(!comment.getParentCommentId().equals(comment.getCommentId()))
                System.out.print("\t");
            System.out.println("commentId = " + comment.getCommentId());
        }

        Long lastParentCommentId = responses1.getLast().getParentCommentId();
        Long lastCommentId = responses1.getLast().getCommentId();

        List<CommentResponse> responses2 = restClient.get()
                .uri("/v1/comments/infinite-scroll?articleId=1&lastParentCommentId=%s&lastCommentId=%s&pageSize=5".formatted(lastParentCommentId, lastCommentId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponse>>() {});

        System.out.println("second page");

        for(CommentResponse comment : responses2) {
            if(!comment.getParentCommentId().equals(comment.getCommentId()))
                System.out.print("\t");
            System.out.println("commentId = " + comment.getCommentId());
        }

        /** 실행 결과
         * first page
         * commentId = 258924812877602816
         * 	commentId = 258924812974071810
         * commentId = 258924812877602817
         * 	commentId = 258924812974071813
         * commentId = 258924812877602818
         * second page
         * 	commentId = 258924812974071835
         * commentId = 258924812877602819
         * 	commentId = 258924812974071809
         * commentId = 258924812877602820
         * 	commentId = 258924812974071811
         */
    }
    @Getter
    @AllArgsConstructor
    public static class CommentCreateRequest {
        private Long articleId;
        private String content;
        private Long parentCommentId;
        private Long writerId;
    }

}
