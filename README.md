# Smart Parking Management System

JSP/Servlet 기반의 주차장 관리자용 웹 시스템입니다. 차량 입출차, 회원, 결제, 요금 정책, 통계, 관리자 계정 관리를 하나의 관리자 화면에서 처리할 수 있도록 팀 프로젝트로 구현했으며, 현재는 기존 기능과 보안을 보완하여 개인 포트폴리오로 개선하고 있습니다.

## 1. 프로젝트 개요

- 기간: 2026.02 ~ 2026.03
- 형태: 5인 팀 프로젝트
- 목표: 주차장 운영자가 차량 입출차, 결제, 회원, 요금 정책, 통계를 웹에서 관리할 수 있는 관리자 시스템 구현
- 팀 프로젝트 당시 담당 역할: 관리자 로그인과 2차 인증, 관리자 등록 이메일 인증, 비밀번호 찾기, 관리자 계정 관리, 세션 기반 접근 제어
- 검증 범위: 로컬 개발 환경에서 기능 동작을 확인했으며, 별도 배포와 성능 테스트는 진행하지 않음

### 구현 및 확인한 내용

- ID/PW 확인 후 `NORMAL`은 등록 이메일 일치 여부를 확인하고, `ADMIN`은 이메일 OTP를 확인하도록 추가 인증 흐름을 나눴습니다.
- `LoginCheckFilter`는 추가 인증 완료 여부를 검사하고, `AuthorizationFilter`는 인증된 관리자의 역할에 따라 요청 경로를 제한합니다.
- BCrypt 기반 비밀번호 해싱을 적용하고, 이중 해싱으로 인한 로그인 실패 문제를 분석해 수정했습니다.
- Java 17 Toolchain을 적용해 빌드에 사용하는 Java 버전을 고정했습니다.
- JSP와 CSS·JavaScript 파일을 기능별 디렉터리로 분류하고 참조 경로를 통일했습니다.
- 결제 금액을 서버에서 계산하고, 결제 저장·출차 처리·공간 반환을 하나의 JDBC 트랜잭션으로 묶었습니다.

## 2. 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 17 |
| Backend | JSP/Servlet, Jakarta Servlet |
| Frontend/View | JSP, HTML, CSS, JavaScript |
| Database | MariaDB 12.3.3 |
| Database Migration | Flyway 13.7.0 |
| Data Access | JDBC, HikariCP, PreparedStatement |
| Security | Session, BCrypt, Email OTP |
| Logging | Log4j2, SLF4J Bridge |
| Mail | Jakarta Mail API, Angus Mail, Mailpit (로컬), SMTP (외부 환경) |
| Library | Lombok, ModelMapper |
| Build/Runtime | Gradle Wrapper 8.8, WAR Plugin, Tomcat |
| Local Infrastructure | Docker Compose |
| Test | JUnit 5 |

## 3. 프로젝트 전체 기능

- 관리자 로그인/로그아웃 및 권한별 2차 인증
- 관리자 등록·수정 이메일 OTP 인증
- 비밀번호 찾기 OTP 인증 및 새 비밀번호 설정
- 관리자 등록, 조회, 수정, 활성화/비활성화
- 회원 및 차량 정보 관리
- 차량 입차/출차 관리
- 주차 요금 계산 및 결제 관리
- 요금 정책 관리
- 주차 현황 및 통계 대시보드

팀 프로젝트 당시 회원, 차량, 요금 정책, 결제·통계 영역은 다른 팀원이 담당했으며, 본인은 관리자 로그인과 추가 확인, 관리자 계정 관리 영역을 담당했습니다.

이후 개인 포트폴리오 개선 과정에서 기존 결제·출차 흐름을 검토하고, 서버 금액 계산, 단일 트랜잭션 처리와 중복 결제 저장 제한을 추가했습니다. 결제 기능의 최초 구현과 이후 개선 작업을 구분하여 기록합니다.

## 4. 담당 기능

### 관리자 역할 구분

| 역할 | 현재 기능 범위 | 로그인 추가 인증 |
| --- | --- | --- |
| `NORMAL` | 주차·회원·결제 등 일반 운영 기능과 본인 정보 수정 | 등록 이메일 일치 확인 |
| `ADMIN` | 일반 운영 기능, 관리자 계정 관리, 요금 정책 변경과 본인 정보 수정 | 등록 이메일로 발송된 OTP 확인 |

화면에서는 `NORMAL`을 일반 관리자, `ADMIN`을 최고 관리자로 표시합니다. 인증 우회 제거 후 별도 기능이 남지 않은 `SUPER` 역할은 `ADMIN`으로 통합했습니다.

### 관리자 로그인과 추가 확인

- `LoginController`에서 ID/PW와 계정 활성 상태를 확인한 뒤 관리자 권한에 따라 추가 확인 화면으로 분기
- `NORMAL`은 입력 이메일과 등록 이메일이 같은지 확인하고, `ADMIN`은 등록 이메일로 발송된 OTP까지 확인
- `LoginCheckFilter`에서 1차 인증 정보인 `loginManager`와 2차 인증 완료 상태인 `fullyAuthenticated`를 모두 확인
- 1차 인증만 완료한 상태에서 보호된 URL로 직접 접근하면 로그인 화면으로 이동
- 계정 비활성화 상태에서는 로그인할 수 없도록 인증 흐름에 반영

### 역할별 접근 제어

- `web.xml`에서 `LoginCheckFilter` 다음에 `AuthorizationFilter`가 실행되도록 순서를 고정
- `/mgr/**`는 ADMIN 전용으로 처리하되, `/mgr/my_modify`는 NORMAL 본인 수정 경로로 분리
- 요금 정책 조회는 두 역할에 허용하고 `/view/policy/add`, `/view/policy/apply`는 ADMIN만 허용
- JSP의 메뉴 노출 조건은 화면 표시 목적으로만 사용하고, 실제 접근 허용 여부는 필터와 Controller에서 판단
- 요청 파라미터의 관리자 ID를 그대로 신뢰하지 않고 Controller가 세션 사용자, 대상 계정의 존재 여부와 역할을 다시 확인

### 관리자 계정 관리

- 관리자 등록, 목록 조회, 상세 조회, 정보 수정, 활성화/비활성화 기능 구현
- 관리자 ID 중복 확인, 이메일 형식 검증, 비밀번호 확인 검증 처리
- 관리자 등록 최종 요청에서 세션의 인증 완료 이메일과 실제 등록 이메일을 서버가 다시 비교하고, 사용한 인증 상태는 즉시 삭제
- 관리자 수정 OTP는 대상 관리자 ID와 이메일을 Session의 인증 완료 상태에 함께 저장하고, 최종 수정 POST에서 두 값이 일치할 때만 사용
- 본인 정보 수정 요청에서 세션 ID와 요청 ID가 다르면 차단하고, ADMIN이 다른 ADMIN 계정을 수정하거나 로그인 중인 본인 계정을 비활성화하지 못하도록 제한
- 관리자 관련 요청이 늘어나면서 기존 Controller에 있던 조회·등록·수정 기능을 각각의 Controller로 분리

### 이메일/OTP 인증

- Jakarta Mail 기반 이메일 인증번호 발송
- `SecureRandom`을 사용하는 공통 `OtpGenerator`에서 6자리 인증번호 생성
- 로그인 OTP는 Session에 코드, 발송 이메일, 생성 시각과 실패 횟수를 저장하고 5분 만료 여부를 확인
- 관리자 등록·수정과 비밀번호 찾기의 인증번호는 `validation` 테이블에 저장하고, Session의 발급 대상 및 실패 횟수와 함께 검증
- OTP 입력은 최대 5회로 제한하고 성공·만료·횟수 초과 시 인증번호와 관련 상태를 제거
- 비밀번호 찾기에서 관리자 ID와 등록 이메일을 재검증한 뒤, OTP 인증을 마친 사용자가 5분 안에 새 비밀번호를 직접 설정
- 비밀번호는 평문 저장 없이 BCrypt 해시값으로 저장

## 5. 아키텍처 및 구조

### 요청 흐름

```text
Browser / JSP
  -> LoginCheckFilter
  -> AuthorizationFilter
  -> Servlet Controller
       ├─ 로그인·일부 관리자 요청 -> DAO 직접 호출
       └─ Service -> DAO

일부 JSP -> Service
개별 연결을 사용하는 DAO -> DBConnection / HikariCP -> MariaDB
Controller / Service -> MailService -> SMTP
```

결제·출차 처리에서는 DAO마다 연결을 얻지 않습니다. `PaymentService`가 HikariCP에서 Connection 하나를 얻고, 잠금 조회·요금 정책 조회·결제 저장·출차 갱신·공간 반환 DAO에 같은 연결을 전달합니다. Service가 커밋·롤백과 연결 반환을 담당합니다.

### 주요 설계 포인트

- JSP/Servlet 기반 요청·응답 처리
- Controller가 DAO를 직접 호출하는 흐름과 Service를 거쳐 DAO를 호출하는 흐름이 함께 존재
- 대시보드와 결제 등 일부 JSP에서 Service를 직접 호출
- HikariCP 기반 DB Connection Pool 구성
- 결제·출차는 `PaymentService`를 트랜잭션 경계로 두고 여러 DAO가 동일한 Connection을 사용
- JDBC `PreparedStatement`를 사용해 SQL 파라미터 바인딩
- BCrypt 기반 관리자 비밀번호 해시 검증
- Session 기반 로그인 상태 관리
- 인증 여부와 역할별 경로 권한을 분리한 두 단계 필터 구성
- Controller에서 수정·조회 대상 ID와 역할을 다시 확인
- Jakarta Mail 기반 이메일/OTP 인증
- 민감 설정값은 `application.properties`로 분리하고, Git에는 예시 파일만 포함

### 프로젝트 구조

```text
docs
├── adr             # 기술 의사결정 기록
└── troubleshooting # 문제 원인과 해결 과정

src/main/java/org/example/smart_parking_260219
├── connection      # HikariCP 기반 DB 연결
├── controller      # Servlet Controller
├── dao             # JDBC 기반 DB 접근 계층
├── dto             # 화면/서비스 전달 객체
├── filter          # 로그인 상태와 역할별 경로 접근 제어 필터
├── mail            # 이메일 발송 설정 및 서비스
├── service         # 비즈니스 로직
├── util            # 설정, 비밀번호, OTP 생성, 매핑 유틸
└── vo              # DB 매핑 객체

src/main/webapp
├── index.jsp
├── css
│   ├── auth        # 로그인, OTP, 비밀번호 찾기 스타일
│   ├── common      # 공통 스타일
│   ├── dashboard   # 대시보드 스타일
│   ├── exit        # 출차 화면 스타일
│   ├── manager     # 관리자 계정 관리 스타일
│   ├── member      # 회원 관리 스타일
│   ├── payment     # 결제 화면 스타일
│   ├── policy      # 요금 정책 스타일
│   └── statistics  # 통계 화면 스타일
├── js
│   ├── auth        # 로그인, OTP, 비밀번호 찾기 동작
│   ├── common      # 공통 메뉴와 알림 처리
│   ├── dashboard   # 대시보드 스크립트
│   ├── exit        # 출차 화면 동작
│   ├── manager     # 관리자 계정 관리 동작
│   ├── member      # 회원 관리 스크립트
│   ├── payment     # 결제와 영수증 처리
│   ├── policy      # 요금 정책 화면 동작
│   └── statistics  # 통계 차트와 조회 조건 처리
└── WEB-INF
    ├── web.xml
    └── views
        ├── auth        # 로그인/이메일 인증 화면
        ├── common      # 공통 메뉴와 요청 처리 알림
        ├── dashboard   # 대시보드
        ├── entry       # 입차 관리
        ├── exit        # 출차 관리
        ├── manager     # 관리자 관리
        ├── member      # 회원 관리
        ├── payment     # 결제 관리
        ├── policy      # 요금 정책 관리
        └── statistics  # 통계 화면
```

JSP는 브라우저에서 직접 접근하지 않도록 `WEB-INF/views` 아래에 두고 기능별 디렉터리로 구분했습니다. CSS와 JavaScript도 동일한 기능 단위로 분류했으며, 디렉터리명과 파일 참조 경로는 소문자로 통일했습니다. 정적 리소스를 참조할 때는 애플리케이션의 Context Path를 기준으로 경로를 생성합니다.

JSP에 있던 인라인 스타일과 `onclick`, `onsubmit`, `onchange` 이벤트는 화면별 CSS·JavaScript 파일로 이동했습니다. JSP나 Servlet이 JavaScript 코드를 직접 생성하던 알림 응답은 공통 알림 JSP에 메시지와 이동 정보만 전달하도록 변경했고, 결제 시간과 통계 값처럼 JavaScript에서 필요한 서버 값은 HTML의 `data-*` 속성을 통해 전달합니다.

### 주요 테이블

| 테이블 | 역할 |
| --- | --- |
| `manager` | 관리자 계정, 권한, 이메일, 활성화 상태 관리 |
| `validation` | 관리자 등록·수정과 비밀번호 찾기의 이메일 인증번호 및 만료 시간 관리 |
| `member` | 회원 차량 정보 및 월정액 상태 관리 |
| `parking_spot` | 주차 공간 상태 관리 |
| `parking` | 차량 입차/출차 기록 관리 |
| `fee_policy` | 주차 요금 정책 관리 |
| `payment` | 결제 내역 관리, `parking_id` UNIQUE 제약으로 주차 이용 건당 결제 최대 한 건 허용 |

## 6. 실행 방법

### 실행 환경

- Java 17
- Tomcat 10.x (Jakarta Servlet 6.0 기준)
- Docker Desktop 및 Docker Compose
- MariaDB 12.3.3, Flyway 13.7.0 (Docker Compose로 실행)
- Gradle Wrapper 8.8

### 로컬 설정

먼저 Docker Compose 환경변수 예시를 복사하고 MariaDB와 Flyway를 실행합니다.

```bash
cp .env.example .env
docker compose up -d
docker compose ps -a
```

다음 상태로 표시되면 로컬 인프라가 정상적으로 준비된 것입니다.

| 서비스 | 정상 상태 | 역할 |
| --- | --- | --- |
| `db` | `Up (healthy)` | MariaDB 실행 |
| `flyway` | `Exited (0)` | 마이그레이션 적용 후 정상 종료 |
| `mailpit` | `Up` | 로컬 SMTP와 메일 확인 화면 제공 |

기본 DB 포트는 3306이며, 충돌하는 경우 `.env`의 `DB_PORT`와 아래 JDBC URL의 포트를 같은 값으로 변경합니다.

애플리케이션의 민감정보는 소스 코드에 직접 작성하지 않고 `src/main/resources/application.properties`에서 읽습니다. 예시 파일을 복사한 뒤 로컬 환경에 맞게 수정합니다.

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

주요 설정값:

```properties
db.url=jdbc:mariadb://localhost:3306/smart_parking_team2
db.username=parking_app
db.password=change-me-local-app-password

mail.host=localhost
mail.port=1025
mail.from=no-reply@smartparking.local
mail.auth=false
mail.ssl.enable=false
mail.starttls.enable=false
mail.debug=false
```

`db.password`는 `.env`의 `MARIADB_PASSWORD`와 동일한 값으로 설정합니다.

### 로컬 시연 계정

로컬 Compose 환경에서는 Flyway가 다음 시연 계정을 자동으로 생성합니다. 이 계정과 비밀번호는 로컬 기능 확인만을 위한 값이며 다른 환경의 초기 데이터에는 포함하지 않습니다.

| 아이디 | 비밀번호 | 역할 | 추가 확인 방법 |
| --- | --- | --- | --- |
| `demo_normal` | `normal1234` | `NORMAL` | `demo-normal@smartparking.local` 입력값과 등록 이메일 일치 확인 |
| `demo_admin` | `admin1234` | `ADMIN` | `demo-admin@smartparking.local`로 발송된 OTP를 Mailpit에서 확인 |

`demo_admin`으로 로그인한 뒤 OTP가 발송되면 다음 순서로 확인합니다.

1. 브라우저에서 `http://localhost:8025`에 접속합니다.
2. Mailpit 수신함에서 가장 최근 OTP 메일을 엽니다.
3. 메일의 6자리 인증번호를 애플리케이션에 입력합니다.

Mailpit은 메일을 실제 외부 주소로 전달하지 않고 로컬에서 보관합니다. 재발송하면 요청한 횟수만큼 메일이 저장되므로 가장 최근에 발급된 인증번호를 사용합니다. 관리자 등록·수정과 비밀번호 찾기에서 발송되는 메일도 같은 수신함에서 확인할 수 있습니다.

### 실제 SMTP 설정

실제 SMTP 발송이 필요한 환경에서는 `config/naver-smtp.properties.example`을 참고합니다. 이 파일은 참고용이며 애플리케이션이 자동으로 읽지 않습니다. 필요한 값을 Git에서 제외된 `application.properties`에 옮기거나 다음 환경변수로 제공합니다.

```text
MAIL_HOST
MAIL_PORT
MAIL_FROM
MAIL_AUTH
MAIL_SSL_ENABLE
MAIL_STARTTLS_ENABLE
MAIL_DEBUG
MAIL_USERNAME
MAIL_PASSWORD
```

환경변수는 `application.properties`보다 우선합니다. 실제 SMTP 계정과 앱 비밀번호는 저장소에 커밋하지 않으며, 설정 변경 후에는 애플리케이션을 다시 시작합니다.

### 기존 데이터베이스에 새 마이그레이션 적용

데이터베이스 스키마와 필수 기준 데이터는 `src/main/resources/db/migration`의 Flyway 버전 마이그레이션으로 관리합니다. 로컬 시연 데이터는 `docker/flyway/local`의 반복 마이그레이션으로 분리했으며, 로컬 Docker Compose 실행 시 함께 적용됩니다.

이미 적용한 버전 SQL은 수정하지 않고 새 버전 파일을 추가합니다. 기존 DB에 `V4__add_unique_payment_parking.sql`을 처음 적용하기 전에는 MariaDB의 `smart_parking_team2`에서 다음 조회로 중복 결제를 확인합니다.

```sql
SELECT parking_id, COUNT(*) AS payment_count
FROM payment
GROUP BY parking_id
HAVING COUNT(*) > 1;
```

결과가 0건이면 중복된 `parking_id` 그룹이 없다는 뜻입니다. 결과가 있다면 V4의 UNIQUE 제약 추가가 실패하므로, 해당 데이터의 원인과 정리 방법을 먼저 확인해야 합니다.

기존 볼륨을 삭제하지 않고 새 마이그레이션을 적용하려면 다음 명령을 사용합니다.

```bash
docker compose run --rm flyway
```

명령의 정상 종료와 실행 출력의 마이그레이션 적용 성공 또는 최신 상태 안내를 확인합니다. V4가 이미 적용된 DB에는 같은 버전이 다시 적용되지 않습니다. 실패한 경우 아래의 전체 초기화로 넘기지 말고 오류 원인을 먼저 확인합니다.

이 명령은 볼륨을 삭제하지 않지만 적용 SQL에 따라 데이터는 변경될 수 있으므로, 보존할 데이터는 먼저 백업합니다. 로컬 시연 SQL도 파일 내용이 변경되면 다시 실행되어 시연 데이터를 갱신할 수 있습니다.

### 로컬 데이터베이스 초기화

Flyway가 빈 데이터베이스를 처음부터 다시 구성하는 과정을 확인하려면 다음 명령을 사용합니다.

> `docker compose down -v`는 `db_data` 볼륨과 로컬에서 추가·수정한 데이터를 모두 삭제합니다. 보존할 데이터가 없는지 확인한 후 실행해야 합니다.

```bash
docker compose down -v
docker compose up -d
docker compose ps -a
docker compose logs flyway
```

재생성 후 `db`가 `healthy`, `flyway`가 `Exited (0)`인지 다시 확인합니다.

### 빌드와 검증 범위

```bash
./gradlew clean war
```

생성된 WAR 파일은 `build/libs`에서 확인할 수 있습니다. Gradle Java Toolchain을 Java 17로 고정해 실행 중인 로컬 JDK와 관계없이 같은 Java 버전을 기준으로 컴파일합니다.

Java 17 WAR 빌드 성공과 주요 인증 흐름의 수동 검증을 확인했습니다. 결제·출차 개선은 로컬 환경에서 다음 항목을 수동 검증했습니다.

- 정상 결제: 결제 내역 1건, 주차 기록의 `paid = true`, 주차 공간 반환 확인
- 강제 실패: 공간 반환 조건을 불일치시킨 뒤 결제 내역 0건, `paid = false`, `exit_time = NULL`로 롤백 확인 후 원래 값 복구 및 재결제 확인
- 중복 저장: V4 적용 후 같은 `parking_id`의 직접 INSERT가 UNIQUE 제약으로 거부되는 것을 확인

`./gradlew clean war`는 애플리케이션 컴파일과 WAR 생성을 확인하는 명령이며, 테스트 코드 컴파일·JUnit 실행이나 JSP 실행 결과까지 검증하지는 않습니다. 위 수동 검증은 동시 결제 요청이나 모든 요금 구간의 자동 회귀 검증을 의미하지 않습니다.

테스트 코드는 JUnit 5 기반이지만 일부는 로컬 MariaDB와 초기 데이터에 의존합니다. 또한 기존 테스트 3곳에 제거된 개별 출차 처리 메서드 호출이 남아 있어 수정이 필요합니다. 자동 테스트 정비와 회귀 검증은 아직 완료하지 않았습니다.

## 7. 담당 기능 흐름

### 관리자 로그인

1. ID/PW와 계정 활성 상태를 확인합니다.
2. 일반 관리자는 입력 이메일과 등록 이메일이 같은지 확인합니다.
3. ADMIN 계정은 등록 이메일로 발송한 OTP를 확인합니다.
4. 추가 확인이 끝나면 `fullyAuthenticated`를 Session에 저장합니다.
5. `LoginCheckFilter`는 `loginManager`와 `fullyAuthenticated`가 모두 유효한 요청만 보호 경로로 전달합니다.
6. `AuthorizationFilter`는 역할에 허용된 경로만 Controller로 전달합니다.

### 관리자 등록 이메일 인증

1. 서버가 인증 목적과 이메일을 확인한 뒤 OTP를 발송합니다.
2. 발송 대상 이메일과 실패 횟수를 Session에 저장합니다.
3. 인증 성공 시 OTP를 폐기하고 인증 완료 이메일을 Session에 남깁니다.
4. 최종 관리자 등록 POST에서 인증 완료 이메일과 등록 요청 이메일을 다시 비교합니다.
5. 등록이 완료되면 사용한 인증 상태를 삭제하여 재사용을 막습니다.

### 관리자 수정 이메일 인증

1. 서버가 로그인 관리자에게 수정 대상 계정을 변경할 권한이 있는지 확인합니다.
2. OTP 발송 이메일과 수정 대상 관리자 ID를 Session에 함께 저장합니다.
3. OTP 검증 요청의 이메일과 관리자 ID가 발송 단계의 값과 모두 일치하는지 확인합니다.
4. 최종 수정 POST에서 인증 완료 이메일·관리자 ID와 실제 제출값을 다시 비교합니다.
5. 수정 성공 또는 인증 상태 불일치 시 관련 상태를 삭제하여 다른 계정이나 다음 수정에 재사용하지 못하게 합니다.

### 비밀번호 찾기

1. 관리자 ID, 계정 활성 상태와 등록 이메일을 확인한 뒤 OTP를 발송합니다.
2. OTP 검증이 완료되면 Session에 5분 동안 유효한 비밀번호 변경 권한을 부여합니다.
3. 비밀번호 변경 요청은 클라이언트가 전달한 관리자 ID 대신 Session의 인증된 관리자 ID를 사용합니다.
4. 사용자가 입력한 새 비밀번호를 BCrypt로 해싱해 저장하고, 사용한 변경 권한은 즉시 삭제합니다.

### 관리자 계정 관리

- 등록: ID 중복, 이메일 형식, 비밀번호 확인 후 저장
- 조회: 관리자 목록과 상세 정보 조회
- 수정: 새 비밀번호를 입력하면 DAO에서 BCrypt로 해싱하고, 입력하지 않으면 기존 해시 유지
- 상태 변경: 관리자 계정 활성화·비활성화, 로그인 중인 본인 계정 비활성화 차단

## 8. 개선한 내용

### Java 17 Toolchain 고정

개발 환경마다 사용하는 JDK 버전이 달라 빌드 결과가 달라지는 문제를 줄이기 위해 Gradle Java Toolchain을 Java 17로 고정했습니다. 프로젝트가 요구하는 Java 버전을 빌드 설정에 명시해 동일한 기준으로 컴파일할 수 있도록 했습니다.

### JSP와 정적 리소스 구조 통일

여러 경로에 나뉘어 있던 JSP를 `WEB-INF/views` 아래의 기능별 디렉터리로 정리했습니다. CSS와 JavaScript도 소문자 디렉터리와 기능별 구조로 통일하고, Servlet과 JSP의 화면 이동 경로 및 정적 리소스 참조 경로를 함께 수정했습니다.

- JSP는 서버 데이터 출력과 화면 구조를 담당합니다.
- CSS는 화면별 스타일을 담당하며 JSP의 인라인 `style` 속성을 사용하지 않습니다.
- JavaScript는 폼 검증, 버튼 이벤트, 화면 이동과 모달 동작을 담당합니다.
- Servlet과 JSP는 실행 가능한 `<script>` 문자열을 응답으로 직접 생성하지 않고 공통 알림 화면에 표시할 값만 전달합니다.
- 화면별 JavaScript가 필요한 서버 값은 `data-*` 속성에서 읽습니다.

### 인증 및 보안 개선

기존 팀 프로젝트를 개인 포트폴리오로 개선하면서 인증 흐름을 다음과 같이 보완했습니다.

- 시연 편의를 위해 존재했던 고정 OTP와 인증 우회 기능을 제거했습니다.
- 별도 기능이 없어진 `SUPER` 역할을 `ADMIN`으로 통합하고 역할 값을 `NORMAL`, `ADMIN`으로 단순화했습니다.
- 아이디와 비밀번호 인증뿐만 아니라 권한별 2차 인증까지 완료해야 보호된 관리자 기능에 접근할 수 있도록 변경했습니다.
- 인증 완료 여부는 `LoginCheckFilter`, 역할별 URL 접근은 `AuthorizationFilter`, 실제 수정 대상 권한은 Controller가 확인하도록 책임을 구분했습니다.
- 관리자 등록 시 JavaScript의 인증 결과만 신뢰하지 않고, 최종 등록 요청에서도 서버가 인증된 이메일인지 다시 확인합니다.
- 관리자 수정 시 OTP 인증 결과를 대상 관리자 ID와 이메일에 연결하고 최종 POST에서 다시 확인합니다.
- OTP 생성에 `SecureRandom`을 사용하여 예측 가능성을 낮췄습니다.
- OTP 입력은 최대 5회로 제한하며, 인증 성공·만료·횟수 초과 시 기존 OTP와 관련 상태를 제거합니다.
- 로그인, 관리자 등록·수정, 비밀번호 찾기의 OTP 발급 대상과 실패 횟수를 각각의 서버 상태로 관리합니다.
- 비밀번호 찾기 과정에서 임시 비밀번호를 이메일로 발송하는 대신, OTP 인증을 마친 사용자가 제한된 시간 안에 새 비밀번호를 직접 설정하도록 변경했습니다.

### 결제·출차 정합성 개선

기존에는 Controller가 결제 저장, 출차 갱신과 공간 반환을 각각 호출하고 각 DAO가 별도 연결을 사용했습니다. 뒤의 작업이 실패하면 앞서 반영한 변경을 함께 취소할 수 없어 다음과 같이 변경했습니다.

- 차량번호 대신 `parkingId`로 한 번의 주차 이용 건을 식별합니다.
- 요청의 금액·차량 유형을 사용하지 않고 DB의 입차 기록과 활성 요금 정책으로 최종 금액을 계산합니다. 한 번 생성한 출차 시각을 요금·주차시간 계산과 출차 기록 저장에 함께 사용합니다.
- `PaymentService`가 동일한 Connection으로 결제 저장·출차 갱신·공간 반환을 실행합니다. 각 변경 행 수가 1건일 때만 커밋하고, 처리 중 예외가 발생하면 롤백을 호출합니다.
- `FOR UPDATE`로 주차 기록을 잠금 조회한 뒤 정산 상태를 검사합니다. 별도로 `payment.parking_id`에 UNIQUE 제약을 추가하여 같은 이용 건의 결제 내역이 중복 저장되지 않도록 제한합니다.
- 서버 처리 전에 표시하던 성공 알림을 제거하고, 커밋 성공 후에만 대시보드에 일회성 정산 완료 메시지를 표시합니다.

이번 트랜잭션 적용 범위는 결제·출차·공간 반환이며, 입차 처리와 기존 불일치 데이터 복구는 포함하지 않습니다. 화면의 예상 금액과 최종 저장 금액은 제출 시점에 따라 달라질 수 있습니다.

자세한 설계 판단과 문제 해결 과정은 아래 문서에서 확인할 수 있습니다.

- [기술 의사결정 기록](docs/adr/README.md)
- [트러블슈팅 기록](docs/troubleshooting/README.md)

## 9. 회고

이 프로젝트를 통해 JSP/Servlet, JDBC, Session이 연결되는 Java 웹 요청 흐름을 경험했습니다. 기존 팀 프로젝트를 다시 검토하면서 화면의 JavaScript 검증만으로는 인증을 보장할 수 없으며, 서버가 인증 대상·실패 횟수·완료 여부와 사용 시점을 직접 확인해야 한다는 점을 배웠습니다. 또한 PRG 패턴과 일회성 세션 상태를 적용하며 요청 방식과 상태 관리가 사용자 경험과 보안에 함께 영향을 준다는 점을 확인했습니다.
