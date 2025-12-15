# 테스트 데이터 생성하는 코드 작성하기 
## 📍주요 개념 정리
### 1️⃣ Entity Manger
JPA에서 DB와 직접 대화하는 객체로, `persist`, `find`, `remove` 같은 실제 SQL을 만드는 주체이다. EntityManager의 메서드들은 SQL을 **바로 실행하는 것이 아니라** **영속성 컨텍스트(1차 캐시)**의 상태를 조작한다.

<br>
영속성 컨텍스트란 트랜잭션 단위로 존재하는 엔티티 저장소로, DB보다 앞단에 있는 메모리 캐시이다.

```
[ EntityManager ]
        ↓
[ Persistence Context (1차 캐시) ]
        ↓
[ Database ]
```
| 메서드     | 실제 역할                      |
| ------- |----------------------------|
| persist | INSERT 예약                  |
| find    | 1차 캐시 조회 → 없으면 DB에서 SELECT |
| remove  | DELETE 예약                  |
| flush   | 예약된 SQL 즉시 실행              |
| commit  | flush + 트랜잭션 종료            |

<br><br>

### 2️⃣ Transaction Template
스레드 단위로 트랜잭션을 보장해준다. 
<br><br>
**🤔`@Transactional`을 사용하지 않는 이유는?**<br>
멀티스레드 환경에서는 @Transactional이 거의 무력화되기 때문이다. <br>
`@Transactional`은 AOP + 프록시이기 때문에 다른 스레드에서 실행되면 트랜잭션이 전파되지 않는다. <br>
만약 다음과 같이 작성했다고 가정해보자.
```java
@Test
@Transactional
void initialize() throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    for(int i = 0; i < EXECUTE_COUNT; i++) {
        executorService.submit(() -> {
            insert();
            latch.countDown();
            System.out.println("latch.getCount() = " + latch.getCount());
        });
    }

    latch.await();
    executorService.shutdown();
}

```

```text
[ 프록시 객체 ]
    ↓ (트랜잭션 시작)
[ 실제 메서드 ]
    ↓ (트랜잭션 커밋/롤백)
```

이 경우, initialize 메서드에 프록시 객체가 생성되고, 프록시 객체에 의해 트랜잭션이 실행된다. 트랜잭션은 ThreadLocal에 실제로 저장된다.<br>
이 트랜잭션 내부에서 새로운 스레드를 생성하고, 새로운 스레드 내부에서 데이터 생성 메서드를 실행하지만 <br>
트랜잭션이 ThreadLocal 기반이기 때문에 트랜잭션은 새 스레드로 전파되지 않는다

<br>
👉 AOP 프록시는 스레드를 타고 넘어가지 않는다. 

<br><br>
**🤔그렇다면 insert 메서드에 `@Transactional`을 명시해주면 안되나요?**
```java
@Transactional
void insert() {
    Comment prev = null;
    for (int i = 0; i < BULK_INSERT_SIZE; i++) {
        entityManager.persist(...);
    }
}
```
```java
executorService.submit(() -> {
    insert();
});
```
insert 메서드에 `@Transactional`을 명시해주고, initialize 메서드에서 insert 메서드를 호출한다면, 이는 같은 클래스 내부 메서드이기 때문에 자기 자신 호출(self-invocation)이 발생한다. Spring AOP는 자기 자신 호출을 가로채지 못하기 때문에 프록시가 적용되지 않는다. 따라서 트랜잭션이 제대로 적용되지 않는다. 

> Spring 프록시는 “스프링 컨테이너가 관리하는 Bean을
다른 Bean이 호출할 때, 그 호출 경로에 끼어든다.”

<br>

<small>📢**initialize()가 외부호출인 이유**<br>@SpringBootTest JUnit이 Spring 컨텍스트에서 Bean을 꺼내서 initialize()를 호출하기 때문</small>


<br><br>
**다시 돌아와서**<br>
TransactionTemplate은 현재 실행 중인 스레드에서 직접 트랜잭션을 시작한다. 따라서 쓰레드 세이프하다. 
```java
transactionTemplate.executeWithoutResult(status -> {
    // 여기가 하나의 트랜잭션
});

```
위와 같이 Transaction Template을 활용해 작성하면
1. 트랜잭션 시작
2. 콜백 코드 실행
3. 예외 없으면 commit
4. 예외 발생 시 rollback
이 일어나면서 스레드 단위로 트랜잭션이 보장된다. 

<br><br>

### 3️⃣ CountDownLatch
모든 스레드가 끝날 때까지 기다리는 장치이다. 
```java
CountDownLatch latch = new CountDownLatch(EXECUTE_COUNT);
```
카운트가 0이 될 때까지 await()이 블로킹된다. 

<br><br>

## 📍예제 코드 살펴보기
이번 프로젝트에서 활용한 테스트 데이터 생성 코드는 다음과 같다.
```java
@SpringBootTest
public class DataInitializer {
    @PersistenceContext
    EntityManager entityManager;
    @Autowired
    TransactionTemplate transactionTemplate;
    Snowflake snowflake = new Snowflake();
    CountDownLatch latch = new CountDownLatch(EXECUTE_COUNT); //6000번 실행됨을 기다림

    static final int BULK_INSERT_SIZE = 2000; //한번에 2000번씩 실행
    static final int EXECUTE_COUNT = 6000; //총 6000번 반복

    @Test
    void initialize() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for(int i = 0; i < EXECUTE_COUNT; i++) {
            executorService.submit(() -> {
                insert();
                latch.countDown();
                System.out.println("latch.getCount() = " + latch.getCount());
            });
        }

        latch.await();
        executorService.shutdown();
    }

    void insert() {
        transactionTemplate.executeWithoutResult(status -> {
            Comment prev = null;
            for(int i = 0; i < BULK_INSERT_SIZE; i++) {
                Comment comment = Comment.create(
                        snowflake.nextId(),
                        "content",
                        i % 2 == 0 ? null : prev.getCommentId(),
                        1L,
                        1L
                );

                prev = comment;
                entityManager.persist(comment);
            }
        });
    }
}

```

<br><br>

### 1️⃣ EntityManager의 역할
```java
entityManager.persist(comment);
```
- `persist()`호출 시점에는 바로 INSERT SQL이 나가지 않고, 영속성 컨텍스트에 등록된다.
- 트랜잭션이 커밋되는 시점에 INSERT SQL이 한 번에 flush된다. 
- 본래 EntityManager은 스레드 세이프하지 않지만 이 코드가 안전한 이유는 스프링이 트랜잭션마다 EntityManager를 스레드 바인딩하기 때문이다. 
    ```java
    transactionTemplate.executeWithoutResult(...)
    ```
    따라서 이 블록 안에서는 각 스레드/트랜잭션마다 서로 다른 EntityManager 프록시가 사용된다. 

<br><br>

### 2️⃣ Transaction Template의 역할
```java
void insert() {
    transactionTemplate.executeWithoutResult(status -> {
        // 댓글 2000개 생성
    });
}
```
- `insert()`를 한 번 호출한다 
- = 댓글 2000개를 하나의 트랜잭션으로 저장한다
- = 총 6000번 실행된다.
- = 총 1200만 row가 생성된다. 

<br><br>

### 3️⃣CountDownLatch의 역할
```java
for (int i = 0; i < EXECUTE_COUNT; i++) {
    executorService.submit(() -> {
        insert();
        latch.countDown();
    });
}
```
- 초기 EXECUTION_COUNT는 6000이고, 작업 하나가 끝날 때마다 `countDown()`이 실행된다. 이는 남은 작업 수를 의미한다. 
```java
latch.await();
```
- 메인 테스트 스레드가 여기서 멈춘다
- 6000개의 작업이 전부 끝나면
  - latch = 0
  - await() 해제
  - 테스트 종료 

<br><br>

### 🧠 전체 흐름 정리
1. 테스트 시작
2. 스레드 풀 생성
3. 6000개 작업 제출
4. 각 작업에서
   - 트랜잭션 시작
   - 댓글 2000개 persist
   - 커밋
   - 작업 완료 시 마다 `latch.countDown()`
5. 모든 작업이 종료되면 테스트 코드가 종료된다. 