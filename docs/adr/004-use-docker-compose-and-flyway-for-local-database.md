# ADR-004: Docker Compose와 Flyway로 로컬 데이터베이스 환경 구성

- 상태: 승인

## 문제 상황

기존 로컬 실행 방식은 개발자가 MariaDB를 직접 설치하고 데이터베이스와 접속 계정을 만든 뒤, 하나의 `init.sql`을 수동으로 실행하는 흐름이었습니다. 이 방식은 실행하는 컴퓨터에 설치된 MariaDB 버전과 기존 데이터에 영향을 받으며, SQL이 어느 시점까지 적용됐는지를 저장소만 보고 확인하기 어려웠습니다.

또한 하나의 초기화 파일에 테이블, 애플리케이션 실행에 필요한 기준 데이터, 시연 데이터가 함께 있으면 각 데이터의 적용 범위를 구분하기 어렵습니다. 시연 데이터를 제외해야 하는 환경에서도 초기화 파일 전체를 검토하거나 수정해야 했습니다.

## 변경 전 흐름

```text
MariaDB 직접 설치·실행
→ 사용자가 데이터베이스와 접속 계정 생성
→ init.sql 수동 실행
→ application.properties에 각자 접속 정보 작성
→ 애플리케이션 실행
```

이 흐름에서는 `init.sql`의 실행 여부와 실행 순서를 별도로 기록하지 않았습니다.

## 결정 기준

- 저장소에 명시된 MariaDB 버전으로 빈 데이터베이스를 다시 만들 수 있어야 합니다.
- 스키마, 필수 기준 데이터, 로컬 시연 데이터의 적용 범위를 파일과 경로로 구분합니다.
- SQL 적용 순서와 성공 여부를 데이터베이스에서 확인할 수 있어야 합니다.
- 데이터베이스가 준비된 후에만 마이그레이션을 실행합니다.
- 비밀번호는 추적되는 파일에 실제 값으로 저장하지 않고 예시 파일과 로컬 `.env`를 구분합니다.
- 현재 단계에서는 MariaDB와 Flyway만 컨테이너로 실행하고, 애플리케이션은 로컬 Tomcat에서 실행합니다.

## 결정

Docker Compose에 다음 두 서비스를 구성합니다.

| 서비스 | 이미지 | 책임 |
| --- | --- | --- |
| `db` | `mariadb:12.3.3` | 데이터베이스와 애플리케이션 접속 계정 생성, 데이터 보관 |
| `flyway` | `flyway/flyway:13.7.0` | 버전 마이그레이션과 로컬 시연 데이터 적용 |

`db`의 상태는 MariaDB 이미지가 제공하는 `healthcheck.sh`로 확인합니다. `flyway`는 `db`가 `healthy`가 된 뒤 `migrate`를 한 번 실행하고 종료합니다. 데이터는 `db_data` 이름의 Docker 볼륨에 보관합니다.

데이터베이스 이름과 접속 계정은 Compose의 `MARIADB_DATABASE`, `MARIADB_USER`, `MARIADB_PASSWORD`로 생성합니다. 따라서 Flyway SQL에는 `CREATE DATABASE`, `CREATE USER`, `GRANT`, `USE`를 작성하지 않습니다.

SQL은 적용 목적에 따라 다음과 같이 분리합니다.

| 파일 | 적용 내용 | 적용 범위 |
| --- | --- | --- |
| `V1__create_schema.sql` | 애플리케이션이 사용하는 7개 테이블과 PK·FK·인덱스 | 모든 환경 |
| `V2__insert_required_data.sql` | 주차 공간 20개와 기본 요금 정책 1개 | 모든 환경 |
| `R__seed_local_demo_data.sql` | 로그인 계정, 회원, 주차·결제 시연 데이터 | 로컬 Compose 환경 |

버전 마이그레이션은 `src/main/resources/db/migration`에서 관리합니다. 이미 적용된 `V` 파일은 수정하지 않고, 스키마 변경이 필요하면 다음 번호의 파일을 추가합니다.

로컬 시연 데이터는 `docker/flyway/local`에 반복 마이그레이션으로 둡니다. 고정된 샘플 ID와 UPSERT를 사용하고 날짜는 `CURRENT_DATE`를 기준으로 생성합니다. 이 경로는 로컬 Compose의 `FLYWAY_LOCATIONS`에만 추가합니다.

Compose 설정값은 추적되는 `.env.example`에 키와 예시값만 제공합니다. 실제 로컬 값은 Git에서 제외된 `.env`에 저장합니다. 애플리케이션의 JDBC 접속 정보는 별도로 Git에서 제외된 `application.properties`에 저장합니다.

## 변경 후 흐름

```text
.env.example과 application.properties.example 복사
→ docker compose up -d
→ MariaDB가 데이터베이스·접속 계정 생성
→ healthcheck 통과
→ Flyway가 V1 → V2 → 로컬 R 마이그레이션 적용
→ 로컬 Tomcat에서 애플리케이션 실행
```

이후 같은 볼륨에서는 Flyway가 `flyway_schema_history`를 확인하여 이미 성공한 버전 마이그레이션을 다시 실행하지 않습니다.

## 검토한 대안

| 대안 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- |
| 로컬 MariaDB와 `init.sql` 수동 실행 유지 | 별도 컨테이너 설정이 필요 없음 | 버전·기존 데이터·수동 실행 순서에 따라 결과가 달라질 수 있고 적용 이력이 없음 | 제외 |
| MariaDB 초기화 디렉터리에 SQL 연결 | 컨테이너 최초 실행 시 SQL을 자동 적용할 수 있음 | 새 볼륨에서만 실행되며 버전별 적용 이력과 변경 순서를 관리하지 않음 | 제외 |
| 애플리케이션 또는 Gradle에서 Flyway 실행 | 애플리케이션 실행이나 빌드와 마이그레이션을 연결할 수 있음 | 현재 WAR 실행 구조에 Flyway 의존성과 실행 책임이 추가됨 | 제외 |
| Compose의 별도 Flyway 컨테이너 사용 | 애플리케이션 코드와 분리하고 이미지와 SQL만으로 실행 순서를 재현할 수 있음 | Compose 실행이 선행되어야 하며 애플리케이션 JDBC 포트를 별도로 맞춰야 함 | 채택 |

## 결과

### 기대 효과

- MariaDB와 Flyway 버전, 데이터베이스 이름, 문자셋, 시간대 설정을 Compose 파일에서 확인할 수 있습니다.
- 빈 볼륨에서도 정해진 순서로 같은 스키마와 기준 데이터를 생성합니다.
- `flyway_schema_history`에서 적용된 파일, 체크섬, 성공 여부를 확인할 수 있습니다.
- 애플리케이션 실행에 필요한 기준 데이터와 로컬 시연 데이터의 경로가 분리됩니다.
- 로컬 시연 계정과 통계 데이터가 자동으로 준비되어 저장소를 받은 사람이 별도 SQL을 조합할 필요가 없습니다.

### 감수한 제약

- 애플리케이션은 아직 컨테이너에 포함되지 않으므로 로컬 Tomcat 설정과 WAR 배포가 별도로 필요합니다.
- `.env`의 `DB_PORT`와 `application.properties`의 JDBC 포트가 다르면 애플리케이션이 DB에 연결할 수 없습니다.
- `docker compose down -v`를 실행하면 로컬 DB 볼륨과 그 안의 변경 데이터가 삭제됩니다.
- 로컬 Compose 실행에는 시연 데이터가 자동 적용되므로 시연 데이터가 필요 없는 환경에서는 로컬 Flyway 경로를 포함하지 않아야 합니다.
- 이미 적용된 버전 마이그레이션을 수정하면 체크섬 검증이 실패하므로 새로운 버전 파일을 추가해야 합니다.

## 검증

### 설정 확인

- `docker compose config --quiet`가 오류 없이 종료되어 Compose 구문과 환경변수 치환이 유효함을 확인했습니다.
- MariaDB 컨테이너가 `healthy`로 전환된 뒤 Flyway 컨테이너가 실행되도록 `depends_on.condition`을 설정했습니다.
- Flyway 로그에서 마이그레이션 3개가 검증됐고, 빈 스키마에 V1, V2, 반복 마이그레이션이 적용된 뒤 현재 버전이 `2`로 기록됐습니다.

### 빈 볼륨 재생성 확인

다음 순서로 기존 컨테이너와 볼륨을 제거한 뒤 새 데이터베이스를 생성했습니다.

```bash
docker compose down -v
docker compose up -d
docker compose ps -a
docker compose logs flyway
```

확인 결과 MariaDB는 `healthy`, Flyway는 `Exited (0)`이었으며, 기존 `init.sql` 없이 Flyway만으로 스키마와 데이터가 생성됐습니다.

### 초기 데이터 확인

| 확인 항목 | 생성 건수 |
| --- | ---: |
| 주차 공간 | 20건 |
| 기본 요금 정책 | 1건 |
| 로컬 관리자 계정 | 2건 |
| 가상 회원 | 12건 |
| 정산 완료 주차·결제 | 각 16건 |
| 현재 주차 중 차량 | 6건 |

### 애플리케이션 확인

- Java 17 환경에서 `./gradlew clean war`가 성공했습니다.
- 새 볼륨에 생성된 데이터베이스로 애플리케이션을 실행하여 로그인과 데이터 조회·변경 기능이 동작함을 수동 검증했습니다.

## 관련 구현

- [docker-compose.yml](../../docker-compose.yml)
- [.env.example](../../.env.example)
- [V1__create_schema.sql](../../src/main/resources/db/migration/V1__create_schema.sql)
- [V2__insert_required_data.sql](../../src/main/resources/db/migration/V2__insert_required_data.sql)
- [R__seed_local_demo_data.sql](../../docker/flyway/local/R__seed_local_demo_data.sql)
- [application.properties.example](../../src/main/resources/application.properties.example)
