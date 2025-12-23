# 동시 쓰기 요청을 데이터 유실/장애 없이 해결하기(비관적 락, 낙관적 락, 비동기 순차 처리) 
## 📍비관적 락(Pessimistic Lock)
1. 비관적 관점: 데이터 접근 시 **항상** 충돌이 발생할 가능성이 있다고 가정한다.
2. 항상 **락을 걸어** 다른 트랜잭션의 접근을 방지한다. 
   * 이때 다른 트랜잭션은 락이 해제되기까지 대기한다.
<br><br>
### 1️⃣ 방법1
update 구문 방법. 데이터베이스에 저장된 데이터 기준으로 UPDATE문을 수행
```mysql
start transaction;

# 좋아요 데이터 삽입
insert into article_like values({article_like_id}, {article_id}, {user_id}, {created_at});

# 좋아요 수 데이터 갱신 (Pessimistic Lock 점유)
update article_like_count set like_count = like_count + 1 where article_id = {article_id};

# Pessimistic Lock 해제
commit;
```
1. 락 점유 관점: 락 점유하는 시간이 **상대적으로** 짧음
2. 애플리케이션 개발 관점: 데이터베이스의 현재 저장된 데이터 기준으로 증감 처리하기 때문에 SQL문을 직접 전송
<br><br>
### 2️⃣ 방법2
select ... for update + update 구문 방법. ArticleLikeCount 객체를 for update 구문으로 조회한 후(락 점유) 엔티티의 카운트를 증/감 시키는 방식
```mysql
start transaction;

# 좋아요 데이터 삽입
insert into article_like values({article_like_id}, {article_id}, {user_id}, {created_at});

#for update 구문으로 데이터 조회 (Pessimisitc Lock 점유)
select * from article_like_count where article_id = {article_id} for update;

#좋아요 수 데이터 갱신 (조회된 데이터 기반으로 새로운 좋아요 수를 만들어줌)
update article_like_count set like_count = {updated_like_count} where article_id = {article_id};

# Pessimistic Lock 해제
commit;
```
* for update: @Lock(LockModeType.PESSIMISTIC_WRITE) 사용
* 증/감: 엔티티에 메서드를 구현
<br>
1. 락 점유 관점: 락 점유 시간이 **상대적으로** 길며, 데이터를 조회한 뒤 중간 과정을 수행해야 하기 때문에 락 해제가 지연될 수 있음.
2. 애플리케이션 개발 관점: JPA 사용 시 엔티티를 이용해 더 객체지향스럽게 개발 가능
<br><br><br>
## 📍낙관적 락
1. 낙관적 관점: 데이터 접근 시에 **항상** 충돌이 발생할 가능성이 없다고 가정한다.
2. 데이터의 변경 여부를 확인하여 충돌을 처리한다. 
    * 데이터가 다른 트랜잭션에 의해 수정되었는지 확인하고, 수정된 내역이 있다면 후 처리를 진행(rollback, 재처리 등등)
3. 충돌 확인 방법: version 칼럼 이용. 레코드 업데이트 시 WHERE 조건에 기존 version을 넣고, version을 증가시킨다. 이때 데이터 변경이 실패한다면 충돌이 있다는 것이다. 
<br><br><br>
## 📍비동기 순차 처리
1. 모든 상황을 실시간으로 처리하고 즉시 응답해줄 필요는 없다는 관점
2. 요청을 대기열에 저장해두고, 이후에 비동기로 순차적으로 처리하는 방식이다.
3. 장점: 락으로 인한 지연이나 실패 케이스가 최소화된다.
4. 단점: 즉시 처리되지 않으므로 사용자 입장에서는 지연될 수 있으며, 큰 비용(시스템 구축 비용 등)이 든다.
