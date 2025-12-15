# 효율적인 페이지네이션(Pagination) 구현

## 📍페이징 쿼리
```mysql
select article.article_id, article.title, article.content, article.board_id, article.writer_id, 
       article.created_at, article.modified_at
from (
   select article_id from article 
   where board_id = :boardId
   order by article_id desc
   limit :limit offset :offset
) t left join article on t.article_id = article.article_id
```

이떄 서브쿼리를 이용하지 않고 다음과 같이 sql문을 작성한다면 발생하는 문제를 알아보자. 
```mysql
select * from article
where board_id = :boardId
order by article_id desc
limit :limit offset :offset
```

1. secondary index 스캔 
secondary index(board_id, article_id)를 스캔한다.
2. 각 row마다 clustered index를 접근한다.
각 행마다 clustered index를 접근하는데, 이때 random access 방식으로 접근한다.
3. OFFSET만큼 불필요한 row도 전부 접근한다.
offset = 10000이면? 10000번의 Clustered Index 접근이 발생한다. 

<br>
Secondary → Clustered 이동이 너무 많아 OFFSET이 클수록 성능이 급락한다.

<br>

따라서 secondary index를 활용해 알맞은 데이터의 article_id만 먼저 추출하고 이를 조인해서 데이터를 불러온다.


## 📍페이지 번호 처리
데이터 개수가 매우 많을 때 count(*)를 이용해 전체 데이터 개수를 세고, 이를 기반으로 몇 페이지까지 가능한 지를 카운트하는 방식은 시간이 오래 소요되는 작업일 수 있다.

따라서 만약 현재

1 페이지 당 30개씩의 데이터가 존재하고, 1 ~ 10번 페이지를 띄우고 다음 버튼을 활성화할 지 여부를 판단하는 상황이라면

301개의 데이터 존재 여부만 알면 된다. 전체를 카운트 할 필요가 없어지는 것이다! N개의 데이터 존재 여부만 알면 된다고 할 때 N을 구하는 수식을 공식화하면 다음과 같다.

$N = (((n - 1) / k) + 1) * m * k + 1$
$N = (((n - 1) / k) + 1) * m * k + 1$

- n: 현재 페이지 (n > 0)
- k: 이동 가능한 페이지 개수. 예시에서는 10
- m: 페이지 당 데이터 개수
- (n - 1) / k: 현재 페이지의 앞자리. 이때 해당 연산의 나머지는 버린다. 예를 들어 n = 7일 경우 현재는 0번째 페이지 그룹에 존재하기 때문에 (n-1)/k = 0이다. n = 11일 경우 1번째 페이지 그룹에 존재하기 때문에 (n-1)/k = 1이다.

```mysql
select count(*)
from (
	select article_id from article where board_id = {board_id} limit {limit}
) t
```

