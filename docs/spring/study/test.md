# 스프링부트에서의 테스트 
# 📍테스트 기본 개념: JUnit? Mockito?
| 이름          | 한 줄 설명             |
| ----------- | ------------------ |
| **JUnit**   | **테스트를 실행해주는 엔진**  |
| **Mockito** | **가짜 객체 만들어주는 도구** |

<br><br><br>

## 📢JUnit이란?
`@Test`가 붙은 메서드를 찾아서 순서대로 실행해주고, 성공/실패 여부를 판단해준다.
<br><br>

## 📢Mockito란?
가짜 배우와 같다. 예를 들어 아래 상황을 가정해보자.
```
CommentService
 └── CommentRepository (DB 접근)
```
서비스를 테스트할 때 DB까지 실제로 연결하면 느려지고, 복잡해진다. 따라서 레포지토리를 가짜로 만드는데, 이 역할을 Mockito가 해준다.

<br><br>

### 1️⃣ 핵심 개념
Mockito에서 알아야할 핵심 개념은 다음과 같다. 
- **Mock** 
가짜 객체 <br>
👉 실제 구현 ❌ <br>
👉 껍데기 객체 ⭕
```java
@Mock
CommentRepository commentRepository;
```
<br>

- **Stub**
행동 정의 <br>
👉 “이렇게 호출되면 이렇게 행동해라”
```java
given(commentRepository.findById(1L))
        .willReturn(Optional.of(comment));
```
<br>

- **Verify**
검증 <br>
👉 “진짜 이 메서드가 호출됐냐?”
```java
verify(comment).delete();
```
<br><br>
### 2️⃣ 사용 방법
사용 방법은 다음과 같다.
1. `@ExtendWith(MockitoExtension.class)` 명시
```java
@ExtendWith(MockitoExtension.class)
class CommentServiceTest { }
```
JUnit에게 Mockito를 사용하겠다고 말해주는 부분이다. 해당 어노테이션이 명시되어 있지 않으면 @Mock이 만들어지지 않고, @InjectMocks가 제대로 실행되지 않는다. 

<br>

2. `@Mock`로 가짜 클래스 만들기 
```java
@Mock
CommentRepository commentRepository;
```
"이 클래스를 가짜로 만들어줘"

<br>

3. `@InjectMocks`
```java
@InjectMocks
CommentService commentService;
```
"가짜들을 이 클래스에 주입해줘"
<br><br>
즉 다음과 같이 작성하면,
```java
class CommentServiceTest {
    @InjectMocks
    CommentService commentService;

    @Mock
    CommentRepository commentRepository;
}
```
내부에서는 다음과 같이 동작한다.
```java
commentService = new CommentService(commentRepository);
```
가짜로 만든 레포지토리를 주입해 가짜 서비스를 만드는 것이다. 


<br><br>

### 3️⃣ `@Mock` vs `@MockBean` 비교

| 구분 | @Mock | @MockBean |
|----|------|----------|
| 사용 위치 | 순수 Mockito 테스트 | Spring Context 테스트 |
| 스프링 빈 등록 | ❌ | ⭕ |
| 사용 예 | Service 단위 테스트 | @WebMvcTest, @SpringBootTest |

- Mockito 단독 → @Mock
- Spring이 뜨면 → @MockBean

<br><br><br>

# 📍테스트 종류
테스트는 크게 세 종류로 구분 가능하다.

| 분류                        | 질문                            |
| ------------------------- | ----------------------------- |
| 단위 테스트 (Unit Test)        | “이 클래스 **혼자** 잘 동작하나?”        |
| 통합 테스트 (Integration Test) | “여러 컴포넌트가 **같이** 잘 동작하나?”     |
| E2E / API 테스트             | “사용자 관점에서 **실제로 요청하면** 잘 되나?” |

<br><br><br>

# 📍테스트 어노테이션
## 📢통합 테스트
실제 운영 환경에서 사용될 클래스들을 통합하여 테스트하는 것이다. 단위 테스트와 같이 **기능 검증**을 위한 것이 아니라 스프링 프레임워크에서 전체적으로 플로우가 제대로 동작하는지 검증하기 위해 사용된다.
<br><br>

### **@SpringBootTest**
* 스프링부트 애플리케이션 테스트에 필요한 거의 모든 Bean 로딩
* 느림
* JUnit4는 `@RunWith(SpringRunner.class)`를 명시해주어야 한다. JUnit5는 해당되지 않는다. 
<br> 
`@SpringBootTest`는 `properties`, `webEnvironment` 등의 옵션이 존재한다. 
<br>
* `@SpringBootTest`는 기본 선택지가 아니다  → **"진짜 통합이 필요한 경우"**에만 사용한다.

## 📢단위 테스트
### 1️⃣ **@WebMvcTest**
* Controller 전용
* Service는 @MockBean 이용

<br>

### 2️⃣ **@WebFluxTest**
* WebFlux Controller 전용

<br>

### 3️⃣ **@DataJpaTest**
* JPA + Repository 전용
* 기본 rollback
* Mockito ❌
<br> @DataJpaTest는 실제 JPA 동작을 검증하는 테스트이므로
  Mocking은 테스트 신뢰도를 떨어뜨린다.


<br>

### 4️⃣ **@JsonTest**
* 직렬화/역직렬화
* DTO 검증용

<br>

### 5️⃣ **@RestClientTest**
* RestTemplate / WebClient 테스트
* 외부 API Mock

# 📍사용 방법 총 정리

| 목적             | 어노테이션                      |
| -------------- |----------------------------|
| Controller 테스트 | @WebMvcTest    <br>Controller 테스트에서는 HTTP 요청/응답, 상태 코드, JSON 구조만 검증한다. <br>비즈니스 로직은 검증하지 않는다.<br/>|
| Service 로직     | Mockito + JUnit (스프링을 띄우지 않는다) |
| JPA 검증         | @DataJpaTest               |
| JSON 검증        | @JsonTest                  |
| 외부 API         | @RestClientTest            |
| 전체 플로우         | @SpringBootTest            |

<br><br><br>

# 📍테스트 네이밍 컨벤션
`methodName_condition_expectedResult`

예) <br>
* delete_hasChildren_markDeleted
* delete_noChildren_deleteImmediately
