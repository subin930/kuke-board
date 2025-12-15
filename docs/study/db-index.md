# 데이터베이스 인덱스
데이터를 빠르게 찾기 위한 방법

- 인덱스 관리를 위해 부가적인 쓰기 작업과 공간이 필요함
- 다양한 데이터 특성과 쿼리를 지원하는 다양한 자료 구조가 존재함
    - B+ tree, Hash, LSM tree, R tree, Bitmap
- 관계형 데이터베이스에서는 주로 B+ tree를 이용하여 인덱스를 구성하는데, 이유는 다음과 같다.
    - 데이터가 정렬된 상태로 저장된다.
    - 검색, 삽입, 삭제 연산을 로그 시간에 수행할 수 있다.
    - 트리 구조에서 리프 노드 간 연결되기 때문에 범위 검색이 매우 효율적이다.
- 인덱스를 사용하면, 쓰기 시점에 B+ tree 구조의 정렬된 상태의 데이터가 생성된다. 이미 인덱스로 지정된 칼럼에 대해 정렬된 상태를 가지고 있으므로 조회 시점에 전체 데이터를 정렬하고 필터링할 필요가 없다. (조회 쿼리 빠르게 수행 가능)

## 📍인덱스의 종류

MySQL은 기본 스토리지 엔진으로 InnoDB를 사용한다. 스토리지 엔진이란 DB에서 데이터를 저장 및 관리하는 장치를 의미한다. InnoDB는 테이블마다 Clustered Index를 자동으로 생성하고, 보통은 자동으로 Primary Key를 기준으로 정렬된 Clustered Index를 생성한다. (leaf node에는 행 데이터를 가지고 있음)

### 📢clustered index

- 테이블의 Primary key로 자동 생성됨
- 데이터: 행 데이터를 가지고 있음
- 개수: 테이블 당 1개

### 📢secondary index(non-clustered index)

- 테이블의 칼럼으로 직접 생성함
- 데이터: 데이터에 접근하기 위한 포인터(데이터는 clustered index가 가지고 있기 때문) + 인덱스 칼럼(key) 데이터
- 개수: 테이블 당 여러 개

→ secondary index를 이요한 데이터 조회는 다음과 같이 인덱스 트리를 두 번 탐색한다.

1. Secondary Index에서 데이터에 접근하기 위한 포인터를 찾는다.
2. 찾은 포인터를 바탕으로 Clustered Index에서 데이터를 찾는다.

**그렇다면 Secondary Index에만 접근해서 데이터를 가져올 수는 없을까?**

현재 Secondary Index를 생성할 때 board_id, article_id를 사용했다. 쿼리문을 작성할 때 select *가 아니라 select board_id, article_id로 바꾸어준 뒤 실행한다면 눈에 띄게 조회 속도가 높아지는 것을 알 수 있다.

### 📢covering index

- 인덱스만으로 쿼리의 모든 데이터를 처리할 수 있는 인덱스
- 데이터(Clustered Index)를 읽지 않고, 인덱스(Secondary Index)에 포함된 정보만으로 쿼리 가능한 인덱스