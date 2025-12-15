# 대규모 시스템 서버 인프라 기초

## 📍Scale-Up / Scale-Out
* Scale-Up(수직 확장): 단일 서버의 성능을 향상시키는 것
* Scale-Out(수평 확장): 서버를 추가하여 성능을 향상시키는 것

Scale-Out은 Spring boot 서버 뿐만 아니라 로드 밸런서, 캐시 등에도 광범위하게 적용될 수 있다.

## 📍Load Balancer
트래픽을 라우팅 및 분산하기 위한 도구로 로드 밸런서를 활용할 수 있다. 클라이언트가 로드 밸런서로 요청을 보내면 로드 밸런서가 요청을 적절하게 분산해 서버로 전달한다. 

## 📍시스템 아키텍처의 종류
시스템 아키텍처란 시스템 구조/설계방식을 의미하며 확장성, 유지보수성, 성능 등에 큰 영향을 미친다.

대표적으로는 Monolithic 구조, Microservice 구조가 존재한다.

### 📢 Monolithic Architecture
Monolithic이란 단일의, 일체형의, 하나의 덩어리로 된 이라는 뜻을 가지고 있다.

따라서 모놀리식 아키텍처는 애플리케이션의 모든 기능이 하나로 통합된 아키텍처를 의미한다. 

* 모든 기능이 단일 코드 베이스로 결합된 아키텍처
* 소규모 시스템에서 개발 및 배포가 간단해 많이 채택한다.
* 하지만 특정 부분만 확장하기 어렵고, 변경 사항이 시스템 전체에 영향을 미치며, 대규모 시스템에서 복잡도가 커지고 개발이 어려워질 수 있다.

### 📢 Microservice Architecture
전체 시스템이 작고 독립적인 서비스(Microservice)로 구성된다.

* 시스템이 작고 독립적인 서비스로 구성된다.
* 각 서비스는 단일 기능을 담당하며 독립적인 배포가 가능하다.
* 서비스 단위로 유연한 확장이 가능하다.
* 하지만 서비스 간 복잡한 통신/모니터링이 필요하고, 데이터 일관성/트랜잭션 관리가 어려우며, 개발 난이도도 높다.

### 📢 Service-Oriented Architecture(SOA)
다양한 기능을 서비스 단위로 묶어 재사용 가능하게 하는 아키텍처이다. 여러 시스템을 통합하고 재사용할 수 있도록 기능을 서비스화한다.

* 서비스 단위를 재사용 가능한 형태로 구성한다.
* ESB(Enterprise Service Bus)를 통합 통신이 특징이다.
* 무겁고 복잡해지는 단점이 존재해 이후 마이크로서비스가 등장하는 배경이 된다.

구성요소로는 Service Provider/Consumer/Registry, ESB(Enterprise Service Bus)가 있다.

1. Service Provider
서비스를 제공하는 시스템 (ex. ERP의 "재고 조회 서비스")
2. Service Consumer
서비스를 호출하는 시스템 (ex. 웹 시스템, 모바일 앱 등)
3. Service Registry
서비스 목록을 저장하는 저장소 (ex. 고객 조회 서비스의 WSDL 위치, 인증 방식 등)
4. ESB(Enterprise Service Bus)
서비스 간 메시지의 중심 허브로 변환, 라우팅, 보안 등을 담당한다.

### 📢 Serverless Architecture
서버 관리를 하지 않고 FaaS(Function as a Service)를 활용한다.

* AWS Lambda, Cloud Functions
* 비용이 효율적이지만 장기 실행 작업에는 부적합할 수 있다.

## 📍애플리케이션 내부 구조
### 📢 Layered Architecture
역할에 따라 여러 계층으로 나누어 책임을 분리한다.

* 가장 전통적인 방식 
* 전형적 레이어: <br>
  Presentation → 
  Application/Service → 
  Domain/Business Logic → 
  Persistence/Repository → 
  Infrastructure
  * Presentation (UI, Controller): 사용자 요청을 받고 응답을 반환. 웹 컨트롤러, API 핸들러 등. 
  * Application / Service: 요청을 조정(or 트랜잭션 경계). 여러 도메인 유즈케이스를 조합. 
  * Domain / Business Logic: 핵심 비즈니스 규칙. 엔티티, 도메인 서비스 등. (단순 구현에서는 Service 레이어와 혼용되기도 함)
  * Persistence / Repository: DB 접근을 캡슐화. ORM, DAO 등이 위치. 
  * Infrastructure: 외부 시스템 연동(이메일, 메시지, 파일스토리지), 설정, 공통 라이브러리.
* 테스트 단위가 비교적 직관적(Controller, Service, Repository 분리)
* 레이어 간 호출이 복잡해지면 강한 결합으로 변할 수 있음. (예: Presentation이 Repository 직접 호출)
* 언제 사용? 
  * 프로젝트 초기 단계거나 팀이 작고 복잡도가 낮은 경우.
  * 표준 CRUD 중심 앱(관리자 페이지, 내부툴 등).
```angular2html
/src
  /presentation (controllers, dto)
  /service      (app services, transaction scripts)
  /domain       (entities, domain services, value objects)
  /repository   (jpa repositories, mappers)
  /infra        (email, cache, external clients)

```

## 📢 Hexagonal Architecture
애플리케이션(도메인 핵심)을 중앙에 두고, 외부와의 모든 상호작용을 포트(포트: 인터페이스)와 어댑터(구현)로 분리하는 아키텍처 

* 목적: 도메인을 외부 기술에 의존시키지 않는 것
* Core (Domain / Application): 비즈니스 규칙과 포트(인터페이스)를 정의.
* Ports: 애플리케이션이 외부와 통신하는 추상 인터페이스(예: UserRepository, PaymentGatewayPort).
* Adapters: 구체 구현체 (DB용 어댑터, REST 클라이언트 어댑터, Messaging 어댑터). 포트를 구현하여 외부와 연결.
* Application Service (Use Cases): 포트를 통해 외부 접근을 추상적으로 수행.
* 도메인이 외부 기술(특정 DB, 메시지 큐, 프레임워크)에 종속되지 않음.
* 테스트 용이: 포트(인터페이스)를 목으로 교체하여 Pure Domain 테스트 가능.
* 인프라 교체가 쉬움(예: DB 교체, 외부 API 버전 교체).
* 초기 구조 설계에 신경 써야 하고 추상화가 과해지면 오히려 복잡해질 수 있음.
* 언제 사용? 
  * 도메인 규칙이 복잡하고 기술 변화(데이터베이스, 메시징, 외부 API 등) 가능성이 높은 시스템. 
  * 장기 유지보수, 테스트가 중요한 프로젝트.

```angular2html
       [Adapter: Web] 
            |
 [Adapter: CLI] - [Application / Domain Core] - [Adapter: DB]
            |
       [Adapter: Queue]

```

```angular2html
/src
  /core
    /domain (entities, domain services)
    /usecase (application services - 포트 사용)
    /port (repository ports, external service ports)
  /adapters
    /persistence (jpa, sql implementations of ports)
    /web (controllers -> translate to usecases)
    /messaging (queue producers/consumers implementing ports)
  /config (wiring: 어떤 adapter가 어떤 port를 구현할지 바인딩)

```

## 📍도커(Docker)
도커는 애플리케이션을 컨테이너라는 격리된 환경에서 실행할 수 있게 해주는 플랫폼이다.

* 프로그램은 이미지(실행파일)로 패키징되고, 컨테이너(실행된 프로세스)로 실행된다!
  * 이미지: 애플리케이션 실행을 위한 템플릿
  * 컨테이너: 실행된 이미지, 독립적이고 격리된 실행 환경
* Docker Hub: 도커 이미지의 중앙 저장소(레지스트리)
  * 이미지 다운로드 또는 업로드

### 📢 기본 명령어
- docker images: 이미지 목록 조회
- docker pull: Docker hub로부터 이미지 다운로드
- docker ps: 실행 중인 컨테이너 목록 조회
    - -a 옵션: 종료된 컨테이너 포함 목록 조회
- docker run: 주어진 이미지를 기반으로 컨테이너 실행, 필요한 경우 이미지를 자동으로 pull
- docker start: 실행 중인 컨테이너 종료
- docker exec: 실행 중인 컨테이너 내부에서 명령어 실행
- docker rm: 컨테이너 삭제
    - -f 옵션: 컨테이너 강제 삭제
- docker rmi: 이미지 삭제