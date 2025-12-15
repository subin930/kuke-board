# AccessLevel 이해하기
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageLimitCalculator {
    /**
     *
     * @param page: 페이지 번호
     * @param pageSize: 페이지 사이즈
     * @param movablePageCount: 이동 가능한 페이지 번호의 개수
     * @return
     */
    public static Long calculatePageLimit(Long page, Long pageSize, Long movablePageCount) {
        return (((page - 1) / movablePageCount) + 1) * pageSize * movablePageCount + 1;
    }
}

```

위 코드를 작성하던 중 AccessLevel에 대해 궁금증이 생겼다. 따라서 아래에 정리하였다.

<br><br>

## ❓위 코드에서 AccessLevel.PRIVATE로 설정한 이유
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
```
👉 매개변수 없는 생성자를 만드는데,
<br>
👉 접근 제어자를 private로 만들어라

<br>
즉 lombok이 아래 코드를 자동으로 생성해준다.

```java
private PageLimitCalculator() {
    
}
```

<br>

다시 전체 코드를 살펴보면, 이 클래스는 **final class**이고, 모든 메서드가 **static**이며, 상태(필드)가 존재하지 않는다. 
<br>

즉, **유틸리티 클래스**이다. 

유틸리티 클래스는 객체로 만들 필요가 없기 때문에 다음과 같은 사용을 막고자 한다.
```java
new PageLimitCalculator();
```
이와 같은 객체 생성은 의미가 없기 때문이다. 객체를 생성하지 않아도 모든 메서드에 접근할 수 있기 때문에..

<br>

### 🤔그렇다면 생성자를 안쓰면 안될까?
자바에서는 생성자를 하나도 만들지 않으면 컴파일러가 public 기본 생성자를 만들어버린다. 
<br>

즉, 다음과 같이 사용하는게 가능해진다.
```java
new PageLimitCalculator();
```

<br>

따라서 명시적으로 AccessLevel.PRIVATE을 명시해줌으로써, 생성자를 만들고 접근을 private으로 막아버리는 것이다.

## 📍AccessLevel정리
자바 접근 제어자(private / protected / default / public)

### 1️⃣AccessLevel.PRIVATE
* 접근 범위
  * 해당 클래스 내부에서만 접근 가능
  * 같은 패키지 ❌ 
  * 자식 클래스 ❌
* 실제 생성 코드
```java
private ClassName() {}
```
* 언제 사용할까?
  * 유틸리티 클래스
  * 싱글톤 패턴
  * 외부에서 객체 생성을 막고 싶을 때

### 2️⃣AccessLevel.PROTECTED
* 접근 범위
  * 같은 패키지
  * 다른 패키지여도 자식 클래스면 가능
* 실제 생성 코드
```java
protected ClassName() {}
```
* 언제 사용할까?
  * 상속을 염두에 둔 설계
  * 외부에서 직접 생성은 막고 서브 클래스에서는 생성을 허용할 때

### 3️⃣AccessLevel.PACKAGE
* 접근 범위
  * 같은 패키지 안에서만 접근 가능
  * 자식 클래스라도 패키지 다르면 ❌
* 실제 생성 코드
```java
ClassName() {} // 접근제어자 없음 (default)
```
* 언제 사용할까?
  * 패키지를 하나의 모듈로 쓰고 싶을 때
  * 외부 레이어 접근 제한
  * 도메인 보호용

### 4️⃣AccessLevel.PUBLIC
* 접근 범위
  * 어디서든 접근 가능
  * 제한 없음
* 실제 생성 코드
```java
public ClassName() {}
```
* 언제 사용할까?
  * 일반적인 DTO
  * 컨트롤러, 서비스
  * 외부 라이브러리 공개 API 

| AccessLevel | 클래스 내부 | 같은 패키지 | 자식 클래스 | 전체 |
| ----------- | ------ | ------ | ------ | -- |
| PRIVATE     | ⭕      | ❌      | ❌      | ❌  |
| PROTECTED   | ⭕      | ⭕      | ⭕      | ❌  |
| PACKAGE     | ⭕      | ⭕      | ❌      | ❌  |
| PUBLIC      | ⭕      | ⭕      | ⭕      | ⭕  |


## 📍실무에서 자주 쓰는 패턴
### ✅ 유틸 클래스
@NoArgsConstructor(access = AccessLevel.PRIVATE)

### ✅ JPA 엔티티
@NoArgsConstructor(access = AccessLevel.PROTECTED)

### ✅ 내부 구현 숨기기
@NoArgsConstructor(access = AccessLevel.PACKAGE)

### ✅ DTO / API 모델
@NoArgsConstructor(access = AccessLevel.PUBLIC)