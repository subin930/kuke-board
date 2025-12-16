# 무한 depth 댓글 설계 및 구현하기
최대 depth 크기가 정해진 댓글 테이블 설계는 간단하다. 단순히 테이블에 칼럼을 depth 크기만큼 생성해주면 된다. 이를 **인접 리스트(Adjacency list) 방식**이라고 한다. 
<br>

하지만 댓글의 depth가 얼마나 될 지 모르는, 무한 depth 댓글에 대해서는 칼럼을 무한대로 늘려줄 수 없다. 따라서 이는 인접 리스트 방식이 아니라 **경로 열거(Path enumeration) 방식**으로 구현해주어야 한다. 

## 📍무한 depth path 칼럼 설계
| 댓글1 |     |     |
|--|-----|-----|
|  | 댓글2 |     |
|  |     | 댓글6 |
|  | 댓글4 |     |
|  |     | 댓글5 |
|  |     | 댓글6 |
| 댓글3 |     |     |
|  |     | 댓글7 |

다음과 같은 댓글 목록이 있다고 가정하자. 댓글들의 경로를 살펴보면 <br><br>

parent_comment_id = 1 <br>
comment_id = 1

<br><br>
parent_comment_id = 1 > 2<br>
comment_id = 2

<br><br>
parent_comment_id = 1 > 2 > 6<br>
comment_id = 6

<br><br>
parent_comment_id = 1 > 4<br>
comment_id = 4

<br><br>
parent_comment_id = 1 > 4 > 5<br>
comment_id = 5

<br><br>
parent_comment_id = 3<br>
comment_id = 3

<br><br>
parent_comment_id = 3 > 7<br>
comment_id = 7

위와 같다. 데이터를 살펴보면 이러한 경로에 의해 정렬 순서가 명확한 것을 알 수 있다. 즉, 경로 정보로 인덱스를 생성하면 된다. 


<br><br>
따라서 다음과 같이 설계한다. <br>

<div style="font-size:large">
**XXXXX(1depth) | XXXXX(2depth) | XXXXX(3depth) | XXXXX(4depth) | XXXXX(5depth)**
</div>

- 각 depth 별로 5개의 문자열로 경로 정보를 저장한다.
  - 1 depth는 5개의 문자로, 2 depth는 10개의 문자로, 3 depth는 15개의 문자로, N depth는 (N * 5)개의 문자로
- 문자열로 모든 상위 댓글에서 각 댓글까지의 경로를 저장하는 것이다.
- 만약 각 자리에 0~9의 수가 할당된다면 각 depth 당 $10^5 = 100,000(10만개, 00000~99999)$로 범위가 제한된다.
- 따라서 각 자리에 0~9, A~Z, a~z를 할당한다. 
  - 각 depth 당 $62^5=916,132,843(9억)$개로 범위가 제한된다. 


<br><br>

## 📍데이터 베이스 collation
path를 문자열로 저장한 후 정렬하기 위해서는 데이터 베이스에서 제공하는 collation 설정을 적극적으로 이용해야 한다. <br><br>

collation은 문자열을 정렬하고 비교하는 방법을 정의하는 설정이다. 
- 데이터베이스/테이블/칼럼별로 설정할 수 있다.
- 대소문자 구분, 악센트 구분, 언어별 정렬 순서 등을 지정할 수 있다.


<br><br>
아마 강의대로 진행했다면 `utf8mb4_0900_ai_ci`가 설정되어 있을 것이다.
![img.png](img.png)

<br>
하지만 해당 설정은 대소문자를 비구분하기 때문에 적합하지 않다. 따라서 `utf8mb4_bin` 설정을 사용해 대소문자의 순서를 구분하도록 설정한다. 
collation 설정은 테이블 별로도 가능하지만 칼럼 별로 설정도 가능하기 때문에 path 칼럼에만 설정해주면 된다. 

```mysql
create table comment_v2 (
    comment_idbigintnot null primary key,
    content varchar(3000) not null,
    article_idbigintnot null,
    writer_idbigintnot null,
    path varchar(25) character set utf8mb4 collate utf8mb4_bin not null,
    deleted bool not null,
    created_atdatetime not null
);
```

<br><br> 
이후 path에 인덱스를 생성하여 정렬 데이터를 관리한다. path에 depth와 시간 오름차순이 반영되어 있으므로 (늦게 생성될수록 path가 커짐) path만 관리하면 된다. 
```mysql
create unique index idx_article_id_pathon comment_v2(
    article_idasc, path asc
);
```
** path는 독립적인 경로이므로 unique index로 생성한다. 

