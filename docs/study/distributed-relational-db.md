# 분산 관계형 데이터베이스 (Distributed Relational Database)
단일 DB를 사용할 때 저장해야 할 데이터와 요청의 수가 많아지면 제일 먼저 고려하는 것이 단순히 데이터베이스 서버의 성능을 높이는 <strong> scale up </strong>일 것이다. 하지만 늘어날수록 scale up으로는 한계가 존재하고, 따라서 데이터 베이스 서버를 여러 대 두는 <strong> scale out </strong>을 고려할 것이다.

여러 데이터베이스 서버에 데이터를 분산할 때 <strong>샤딩</strong>을 이용하고, <br>
이때 샤딩된 각각의 데이터 단위를 <strong>샤드</strong>라고 한다.

* 샤딩: 데이터를 여러 데이터 베이스에 분산하여 저장하는 기술
* 샤드: 샤딩된 각각의 데이터 단위 
## 📍샤딩의 종류
### 📢 수직 샤딩(Vertical Sharding)
데이터를 수직을 분할하는 방식, 즉 <strong>칼럼 단위</strong>로 분할한다.

* 장점: 샤드가 적은 수의 칼럼을 저장하므로 성능, 공간 이점이 생긴다.
* 단점: 데이터의 분리로 인해 조인 및 트랜잭션 관리가 복잡해질 수 있다. 또한 수직으로 분할되므로 수평적 확장에 제한이 존재한다.

### 📢 수평 샤딩(Horizontal Sharding)
데이터를 수평으로 분할하는 방식, 즉 <strong>행 단위</strong>로 분할한다.

* 장점: 샤드에 데이터가 분산 저장되므로 성능, 공간 이점이 생긴다. 또한 수평으로 분할되기 때문에 수평적 확장이 용이하다.
* 단점: 데이터의 분리로 인해 조인 및 트랜잭션 관리가 복잡해질 수 있다.

## 📍샤딩 기법
### 📢 Range-based Sharding(범위 기반 샤딩)

데이터를 특정 값(shard key)의 범위에 따라 분할하는 기법

ex) shard key = article_id → shard1 (article_id = 1 ~ 5000) / shard2 (article_id: 5001 ~ 10000)

- 장점: 범위 데이터 조회에 유리 ex) 100번부터 300번까지의 데이터를 조회한다면 shard1에서 모두 조회 가능
- 단점: 데이터 쏠림 현상 존재 가능 ex) 데이터가 6000개 존재한다면 shard1은 꽉 차지만 shard2에는 1000개의 데이터만 존재

### 📢 Hash-based Sharding(해시 기반 샤딩)

데이터를 특정 값(shard key)의 해시 함수에 따라 분할하는 기법

ex) shard key = article_id & hash_function = article_id % 2라면 → shard1(article_id = 1, 3, 5, …) / shard2(article_id = 2, 4, 6, …)

- shard key와 hash_function의 선정이 매우 중요함. 이에 따라 데이터 쏠림 현상이 발생할 수도 있고 발생하지 않을 수도 있음
- 단점: 범위 데이터 조회에 불리할 수 있음

### 📢 Directory-based Sharding(디렉토리 기반 샤딩)

디렉토리를 이용하여 데이터가 저장된 샤드를 관리하는 기법. 여기서 디렉토리는 매핑 테이블을 의미하며, 매핑 테이블을 이용해 각 데이터가 어디 샤드에 저장되어 있는 지를 관리한다.

- 디렉토리의 관리 비용이 존재할 수 있으나, 데이터 규모에 따라 유연한 관리가 가능함

## 📍물리적 샤드 vs 논리적 샤드
### 물리적 샤드
- 정의: 데이터를 물리적으로 분산한 실제 단위
- 필요성: 대규모 데이터 분산 저장 및 성능 향상

### 논리적 샤드
- 정의: 데이터를 논리적으로 분산한 가상의 단위
- 필요성: 물리적 확장 시 client 변경 없이 유연한 매핑이 가능함

## 📍데이터 복제
데이터 베이스의 장애 등에 대비하여 데이터 복제본을 관리할 수 있다.

Primary(주 데이터베이스)에 데이터를 쓰고, Replica(복제본)에 데이터를 복제한다. Primary/Replica, Leader/Follower, Master/Slave, Main/Standby 등의 개념이 존재한다.

- Synchronous(동기적): 데이터 일관성을 보장하나 쓰기 성능이 저하됨
- Asynchronous(비동기적):  쓰기에 대한 응답이 복제에 관해서는 기다리지 않기 때문에 쓰기 성능이 유지되나 복제본에 최신 데이터가 즉시 반영되지 않을 수 있음




추가적으로 시간이 된다면 Quorum, Split Brain, Multi-Leader, Collision, Consensus, Clock 등의 개념도 학습해보면 좋다.