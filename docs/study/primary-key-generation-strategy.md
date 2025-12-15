# Primary Key 생성 전략
## 📍DB auto_increment
### 👍장점
매우 간단하다. 따라서 보안적인 문제를 크게 고려하지 않거나 단일 DB를 사용하는 상황, 애플리케이션에서 PK의 중복을 직접 구분하는 상황에서 사용하면 좋다.

### 👎단점
분산 데이터베이스 환경에서 PK가 중복될 수 있다.(식별자의 유일성 보장 X)
또한 클라이언트 측에 노출되는 경우 보안 문제가 발생할 수 있다. 예를 들어 회웜가입 직후 pk가 100인 것을 확인했다면 현재 100명의 사용자가 있다는 사실을 유추 가능하다.

→ PK는 데이터베이스 내에서의 식별자로만 사용하고 유니크 인덱스를 애플리케이션의 식별자로 사용할 수도 있다.

## 📍유니크 문자열/숫자
UUID 또는 난수를 생성하여 PK를 지정하는 방식이다. 정렬 데이터 X 랜덤 데이터 O

### 👍장점
키 생성 방식이 매우 간단하다. 

### 👎단점
정렬 데이터가 아니라 랜덤 데이터이기 때문에 성능 저하가 발생할 수 있다. (디스크 순차 I/O (X) / 디스크 랜덤 I/O (O))

## 📍유니크 정렬 문자열
UUID v7, ULID 등의 알고리즘을 활용할 수 있다. (일반적으로 128비트)

### 👍장점
분산 환경에 대한 PK 중복 문제와 보안 문제가 해결된다. 또한 랜덤 데이터가 아니기 때문에 성능 문제도 어느정도 해결된다.

### 👎단점
데이터 크기에 따라서 공간 및 성능 효율이 달라진다. 
Secondary index는 데이터에 접근할 수 있는 포인터를 가진다. 즉 PK를 가지고 있는데, 이때 PK가 크면 클수록 비용이 많이 든다.

## 📍유니크 정렬 숫자
Snowflake, TSID 등의 알고리즘을 활용할 수 있다. (일반적으로 64비트(BIGINT))

### 👍장점
분산 환경에 대한 PK 중복 문제와 보안 문제가 해결된다. 또한 랜덤 데이터가 아니기 때문에 성능 문제도 어느정도 해결된다. 정렬 문자열보다 적은 공간을 사용한다. 

### 👎단점
정렬을 위해 사용되는 비트 수가 제한되어 키 생성을 위한 시간적인 한계가 존재할 수 있다. 이는 정렬 문자열에도 동일하게 발생하지만 비트 수가 많을 수록 제한이 덜할 수 있다.

# Snowflake 알고리즘
트위터가 만든 분산 환경용 시간 기반 고유 ID 생성 알고리즘

## 📍특징

- X(트위터)에서 개발
- 분산 시스템에서 고유한 64비트 ID를 생성하는 알고리즘
- 구조: [1비트][41비트: 타임스탬프][10비트: 노드ID][12비트: 시퀀스 번호]
    - 분산 환경에서도 중복 없이 순차적 ID를 생성하기 위한 규칙
        - 타임 스탬프: 순차성
        - 노드ID + 시퀀스 번호: 고유성 → 시간이 겹치더라도 이 필드 덕분에 고유성이 유지됨
- 유니크, 시간 기반 순차성, 분산 환경에서의 높은 성능

## 📍동작 원리

1. 현재 시간(ms)을 읽음
2. (현재 타임스탬프 == 마지막 타임스탬프)면 시퀀스++ → 시퀀스가 한계면 (같은 ms에서) 대기 또는 오류 처리
3. (타임 스탬프 > 마지막 타임스탬프)면 시퀀스 = 0
4. 비트 위치에 맞춰 (타임스탬프, 노드ID, 시퀀스 번호) 등을 합쳐 64비트 정수로 반환

## 📍주의할 점

- **시계(Clock) 역행 문제**: 노드의 시스템 시간이 뒤로 가면(예: NTP 동기화) ID 충돌/시간 순서 훼손 문제가 발생. 대응책:
    - 물리시간이 역행하면 ID 생성 서비스가 대기(wait) 후 진행.
    - 논리적 모노토닉 카운터나 fallback 노드 사용.
    - 외부 시계 불안정시 별도 보정 로직 필요.
- **시퀀스 오버플로우**: 같은 ms에 허용량을 초과하면 대기하거나 다른 전략(추가 비트, 더 큰 시퀀스)을 써야 함.
- **노드 ID 관리**: 각 워커/데이터센터 ID를 중복 없이 배정/관리하는 운영 부담이 있음(환경변수, 컨피그 관리, Zookeeper 등 사용 가능).
- **비트 설계의 제약**: 초기에 epoch와 비트 수를 잘못 설계하면 향후 확장성(예: 더 많은 노드, 더 긴 수명)에서 제약이 생김.

```java
package kuke.board.common.snowflake;

import java.util.random.RandomGenerator;

public class Snowflake {
	private static final int UNUSED_BITS = 1;
	private static final int EPOCH_BITS = 41; //timestamp
	private static final int NODE_ID_BITS = 10; //node_id
	private static final int SEQUENCE_BITS = 12; //sequence num

	private static final long maxNodeId = (1L << NODE_ID_BITS) - 1;
	private static final long maxSequence = (1L << SEQUENCE_BITS) - 1;

	private final long nodeId = RandomGenerator.getDefault().nextLong(maxNodeId + 1);
	// UTC = 2024-01-01T00:00:00Z
	private final long startTimeMillis = 1704067200000L;

	private long lastTimeMillis = startTimeMillis;
	private long sequence = 0L;

	public synchronized long nextId() {
		long currentTimeMillis = System.currentTimeMillis();

		if (currentTimeMillis < lastTimeMillis) {
			throw new IllegalStateException("Invalid Time");
		}

		if (currentTimeMillis == lastTimeMillis) {
			sequence = (sequence + 1) & maxSequence;
			if (sequence == 0) {
				currentTimeMillis = waitNextMillis(currentTimeMillis);
			}
		} else {
			sequence = 0;
		}

		lastTimeMillis = currentTimeMillis;

		return ((currentTimeMillis - startTimeMillis) << (NODE_ID_BITS + SEQUENCE_BITS))
				| (nodeId << SEQUENCE_BITS)
				| sequence;
	}

	private long waitNextMillis(long currentTimestamp) {
		while (currentTimestamp <= lastTimeMillis) {
			currentTimestamp = System.currentTimeMillis();
		}
		return currentTimestamp;
	}
}

```

## 📍알고리즘 

### 최대 노드 ID/ Sequence 계산

```java
	private static final long maxNodeId = (1L << NODE_ID_BITS) - 1;
```

10비트로 만들 수 있는 최대 노드 수 = 1023

```java
	private static final long maxSequence = (1L << SEQUENCE_BITS) - 1;
```

12비트 시퀀스의 최대값 = 4095

<br>

### 랜덤 노드 ID 생성

```java
private final long nodeId = RandomGenerator.getDefault().nextLong(maxNodeId + 1);
```

0~1023 사이의 랜덤 nodeId를 하나 생성해서 고정한다. 서버마다 nodeId가 달라야 충돌이 생기지 않는다.

<br>

### Epoch 설정

이 ID 시스템의 시작 기준 시간(Epoch)

```java
	// UTC = 2024-01-01T00:00:00Z
	private final long startTimeMillis = 1704067200000L;
```

<br>

### 상태값

```java
	private long lastTimeMillis = startTimeMillis;

```

마지막으로 생성한 ID의 시간(ms)

→ 시퀀스 증가/초기화/시계 역행 검사를 위해 필요.

```java
	private long sequence = 0L;

```

같은 millisecond 안에서 ID가 여러개 생성될 때 증가하는 시퀀스 번호.

<br>

### nextId 메서드

```java
	public synchronized long nextId() {
		long currentTimeMillis = System.currentTimeMillis();

		if (currentTimeMillis < lastTimeMillis) {
			throw new IllegalStateException("Invalid Time");
		}

		if (currentTimeMillis == lastTimeMillis) {
			sequence = (sequence + 1) & maxSequence;
			if (sequence == 0) {
				currentTimeMillis = waitNextMillis(currentTimeMillis);
			}
		} else {
			sequence = 0;
		}

		lastTimeMillis = currentTimeMillis;

		return ((currentTimeMillis - startTimeMillis) << (NODE_ID_BITS + SEQUENCE_BITS))
				| (nodeId << SEQUENCE_BITS)
				| sequence;
	}
```

1. 서버 시간이 뒤로 가면 ID 순서가 깨지고, 충돌 위험이 있어 예외 발생.
2. 같은 밀리초에 여러 ID 생성한 경우

```java
			sequence = (sequence + 1) & maxSequence;
			if (sequence == 0) {
				currentTimeMillis = waitNextMillis(currentTimeMillis);
			}
```

직전 생성 타임스탬프가 같다면 이전 시퀀스에서 +1.

```java
	private long waitNextMillis(long currentTimestamp) {
		while (currentTimestamp <= lastTimeMillis) {
			currentTimestamp = System.currentTimeMillis();
		}
		return currentTimestamp;
	}
```

이때 maxSequnce를 넘어가면 0이되고, 다음 millisecond가 될 때까지 기다렸다가 타임스탬프를 갱신한다.

3. 새로운 ms라면 시퀀스를 0으로 설정한다. 
4. 마지막 생성 시간 업데이트

```java
		lastTimeMillis = currentTimeMillis;
```

5. 최종 비트 조합
```java
		return ((currentTimeMillis - startTimeMillis) << (NODE_ID_BITS + SEQUENCE_BITS))
				| (nodeId << SEQUENCE_BITS)
				| sequence;
```