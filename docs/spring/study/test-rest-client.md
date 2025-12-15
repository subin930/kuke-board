# 테스트 클래스에서 API 테스트하기 
Spring Boot 3 이상부터는 RestTemplate 대신 RestClient를 사용할 수 있다.
```java
RestClient restClient = RestClient.create("http://localhost:9000");
```
* base URL을 한 번 설정해두면 테스트 코드에서 실제 HTTP 요청을 보낼 수 있다
* 로컬 서버를 띄운 상태에서 API 동작을 빠르게 검증하기 좋다

<br/><br/>
## 📍RestClient에서 retrieve(), body() 이해하기
강의에서 반복해서 get/post/put 등의 요청을 보내고 retrieve().body()를 붙여주는 것을 보고 의문이 들었다.
<br />

### retrieve() - HTTP 응답을 가져오는 단계
- HTTP 요청을 실제로 전송
- 서버로부터 Response(Status + Header + Body) 수신
- 아직 body를 변환하지는 않음
- 4xx / 5xx 응답 시 예외 발생
  - HttpClientErrorException
  - HttpServerErrorException

요약하자면 **요청을 보내고, 응답을 받아올 준비를 하는 단계**이다.

### body() - 응답 Body를 객체로 변환
```
.body(ArticleResponse.class)
```
- 응답 Body(JSON)를 지정한 타입으로 역직렬화 
- Jackson(ObjectMapper)을 사용
- 응답 Body가 없으면 null 반환
- 타입 불일치 시 변환 예외 발생

```json
{
  "articleId": 1,
  "title": "hi"
}
```

⬇️

```
ArticleResponse
```

이때 **ParameterizedTypeReference**를 이용해 제네릭 응답을 처리할 수 있다.
```java
.body(new ParameterizedTypeReference<List<ArticleResponse>>() {})
```
<br/>
자바의 제네릭 타입 소거(Generic Type Erasure, 컴파일러가 제네릭 타입의 타입 파라미터 정보 제거) 때문에
List<ArticleResponse>.class 처럼 쓰는게 불가능하다. 
즉, 리스트라는 것은 알지만 리스트 안에 뭐가 들어있는지 모르는 것이다.

<br />
이런 경우에 ParameterizedTypeReference를 이용해 익명 클래스로 타입 정보를 보존한다. 

```java
class MyType extends ParameterizedTypeReference<List<ArticleResponse>> {}
```
<small>익명 클래스를 만드는 문법으로, List 안에 ArticleResponse가 들어있다는 정보가 적힌다.</small>

## 📍 RestClient에서 자주 쓰는 메서드들
### HTTP 메서드

```java
restClient.get();
restClient.post();
restClient.put();
restClient.delete();
restClient.patch();
```

<br />

### uri()
```java
.uri("/v1/articles/{id}", id)
```


- PathVariable 치환
- 가독성 좋음

```java
.uri("/v1/articles?id={id}&size={size}", id, size)
```

<br />

### body() — 요청 Body 설정 (POST / PUT)
```java
.body(new ArticleCreateRequest(...))
```
- 요청 Body(JSON) 직렬화
- Content-Type 자동 설정

<br />

### header()
```java
.header("Authorization", "Bearer token")
```
<br />

### headers()
```java
.headers(headers -> {
headers.add("Authorization", "Bearer token");
headers.add("Custom-Header", "value");
})
```
<br />

### retrieve()
```java
.retrieve()
```
- 응답 수신 
- 이후 body() / toEntity() 호출 가능

<br />

### toEntity() — 상태 코드까지 필요할 때
```java
ResponseEntity<ArticleResponse> response =
    restClient.get()
        .uri("/v1/articles/{id}", id)
        .retrieve()
        .toEntity(ArticleResponse.class);
```
✔ status
✔ headers
✔ body

### onStatus() — 에러 커스터마이징
```java
.retrieve()
.onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
throw new IllegalArgumentException("잘못된 요청");
})

.onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
throw new IllegalStateException("서버 오류");
})
```

<br />

### exchange() — 가장 로우레벨 
```java
restClient.get()
    .uri("/v1/articles/{id}", id)
    .exchange((req, res) -> {
        if (res.getStatusCode().is2xxSuccessful()) {
            return res.bodyTo(ArticleResponse.class);
        }
        return null;
    });
```
- 상태 코드, 헤더, 바디 직접 제어
- 테스트나 특수 케이스에서만 사용