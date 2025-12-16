package kuke.board.comment.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PageLimitCalculator {
    /**
     *
     * @param page: 페이지 번호
     * @param pageSize: 페이지 사이즈
     * @param movablePageCount: 이동 가능한 페이지 번호의 개수
     * @return
     */
    public static Long calculatePageLimit (
        Long page,
        Long pageSize,
        Long movablePageCount
    ) {
        return (((page - 1) / movablePageCount) + 1) * pageSize * movablePageCount + 1;
    }
}
