package kuke.board.comment.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentPath {
    private String path;

    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final int DEPTH_CHUNK_SIZE = 5; //1 DEPTH 당 5
    private static final int MAX_DEPTH = 5;

    //MIN_CHUNK = "00000"
    private static final String MIN_CHUNK = String.valueOf(CHARSET.charAt(0)).repeat(5);
    //MAX_CHUNK = "zzzzz"
    private static final String MAX_CHUNK = String.valueOf(CHARSET.charAt(CHARSET.length() - 1)).repeat(5);

    public static CommentPath create(String path) {
        //오버 플로우 감지
        if (isDepthOverflowed(path)) {
            throw new IllegalStateException("depth overflowed");
        }

        CommentPath commentPath = new CommentPath();
        commentPath.path = path;

        return commentPath;
    }

    private static boolean isDepthOverflowed(String path) {
        return calDepth(path) > MAX_DEPTH;
    }

    //path의 depth 계산 ex) 현재 path = '0000000000' -> depth = 10 / 5 = 2
    private static int calDepth(String path) {
        return path.length() / DEPTH_CHUNK_SIZE;
    }

    //현재 path의 depth 계산
    public int getDepth() {
        return calDepth(path);
    }

    //root인지 확인하는 메서드
    public boolean isRoot() {
        return calDepth(path) == 1;
    }

    //현재 path의 parent path 반환
    public String getParentPath() {
        return path.substring(0, path.length() - DEPTH_CHUNK_SIZE);
    }

    //현재 path의 하위 댓글의 path 생성
    public CommentPath createChildCommentPath(String descendantsTopPath) {
        if(descendantsTopPath == null) {
            //하위 댓글이 현재 0개
            return CommentPath.create(path + MIN_CHUNK);
        }

        String childrenTopPath = findChildrenTopPath(descendantsTopPath);
        return CommentPath.create(increase(childrenTopPath));
    }

    private String findChildrenTopPath(String descendantsTopPath) {
        return descendantsTopPath.substring(0, (getDepth() + 1) * DEPTH_CHUNK_SIZE);
    }

    private String increase(String path) {
        //path에서 가장 마지막 문자 5개를 자름
        String lastChunk = path.substring(path.length() - DEPTH_CHUNK_SIZE);

        if(isChunkOverflowed(lastChunk)) {
            throw new IllegalStateException("chunk overflowed");
        }

        int charsetLength = CHARSET.length();

        //숫자로 변환
        int value = 0;
        for(char ch : lastChunk.toCharArray()) {
            value = value * charsetLength + CHARSET.indexOf(ch);
        }

        //increase
        value += 1;

        //문자로 재변환
        String result = "";
        for(int i = 0; i < DEPTH_CHUNK_SIZE; ++i) {
            result = CHARSET.charAt(value % charsetLength) + result;
            value /= charsetLength;
        }

        return path.substring(0, path.length() - DEPTH_CHUNK_SIZE) + result;
    }

    private boolean isChunkOverflowed(String lastChunk) {
        return MAX_CHUNK.equals(lastChunk);
    }

}
