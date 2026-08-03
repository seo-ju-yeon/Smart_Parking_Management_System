# Smart Parking Management System

JSP/Servlet 기반의 주차장 관리자용 웹 시스템입니다. 차량 입출차, 회원, 결제, 요금 정책, 통계, 관리자 계정 관리를 하나의 관리자 화면에서 처리할 수 있도록 팀 프로젝트로 구현했습니다.

## 1. 프로젝트 개요

- 기간: 2026.02 ~ 2026.03
- 형태: 5인 팀 프로젝트
- 목표: 주차장 운영자가 차량 입출차, 결제, 회원, 요금 정책, 통계를 웹에서 관리할 수 있는 관리자 시스템 구현
- 담당 역할: 관리자 로그인, 일반 관리자 등록 이메일 확인, ADMIN·SUPER 계정 이메일 OTP 확인, 관리자 계정 관리, 로그인 세션 기반 요청 차단
- 검증 범위: 로컬 개발 환경에서 기능 동작을 확인했으며, 별도 배포와 성능 테스트는 진행하지 않음

### 구현 및 확인한 내용

- ID/PW 확인 후 일반 관리자는 등록 이메일을 확인하고, ADMIN·SUPER 계정은 이메일 OTP를 확인하도록 흐름을 나눴습니다.
- BCrypt 기반 비밀번호 해싱을 적용하고, 이중 해싱으로 인한 로그인 실패 문제를 분석해 수정했습니다.
- 로그인 단계별 세션 값을 확인하면서 `LoginCheckFilter`가 최종 추가 확인 상태까지 검사하지 않는 한계를 정리했습니다.

## 2. 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 17 |
| Backend | JSP/Servlet, Jakarta Servlet |
| Frontend/View | JSP, HTML, CSS, JavaScript |
| Database | MariaDB |
| Data Access | JDBC, HikariCP, PreparedStatement |
| Security | Session, BCrypt, Email OTP |
| Mail | Jakarta Mail, Angus Mail, Naver SMTP |
| Library | Lombok, Log4j2, SLF4J, ModelMapper |
| Build/Runtime | Gradle Wrapper 8.8, WAR Plugin, Tomcat |
| Test | JUnit 5 |

## 3. 프로젝트 전체 기능

- 관리자 로그인/로그아웃
- 일반 관리자 등록 이메일 확인
- ADMIN·SUPER 계정 이메일 OTP 확인
- 관리자 등록, 조회, 수정, 활성화/비활성화
- 회원 및 차량 정보 관리
- 차량 입차/출차 관리
- 주차 요금 계산 및 결제 관리
- 요금 정책 관리
- 주차 현황 및 통계 대시보드

회원, 차량, 요금 정책, 결제·통계 영역은 다른 팀원이 담당했으며, 본인은 관리자 로그인과 추가 확인, 관리자 계정 관리 영역을 담당했습니다.

## 4. 담당 기능

### 관리자 로그인과 추가 확인

- `LoginController`에서 ID/PW와 계정 활성 상태를 확인한 뒤 관리자 권한에 따라 추가 확인 화면으로 분기
- 일반 관리자는 입력 이메일과 등록 이메일이 같은지 확인하고, ADMIN·SUPER 계정은 이메일 OTP까지 확인
- `LoginCheckFilter`에서 `loginManager`가 없는 요청을 로그인 화면으로 이동
- 현재 필터는 `fullyAuthenticated`를 검사하지 않아 추가 확인 완료 여부를 최종 통과 조건으로 사용하지 않음
- 계정 비활성화 상태에서는 로그인할 수 없도록 인증 흐름에 반영

### 관리자 계정 관리

- 관리자 등록, 목록 조회, 상세 조회, 정보 수정, 활성화/비활성화 기능 구현
- 관리자 ID 중복 확인, 이메일 형식 검증, 비밀번호 확인 검증 처리
- 본인 정보 수정 요청에서 세션 ID와 요청 ID가 다르면 차단하고, 로그인 중인 본인 계정의 비활성화를 방지
- 관리자 관련 요청이 늘어나면서 기존 Controller에 있던 조회·등록·수정 기능을 각각의 Controller로 분리

### 이메일/OTP 인증

- Jakarta Mail 기반 이메일 인증번호 발송
- 로그인 OTP는 Session에 코드, 발송 이메일, 생성 시각을 저장하고 5분 만료 여부를 확인
- 관리자 등록·수정과 비밀번호 찾기의 인증번호는 `validation` 테이블에 저장하고 만료 시각을 확인
- 비밀번호 찾기 흐름에서 관리자 ID와 등록 이메일이 일치하는지 재검증
- 비밀번호는 평문 저장 없이 BCrypt 해시값으로 저장

## 5. 아키텍처 및 구조

### 요청 흐름

```text
Browser / JSP
  -> LoginCheckFilter
  -> Servlet Controller
       ├─ 로그인·일부 관리자 요청 -> DAO 직접 호출
       └─ Service -> DAO

일부 JSP -> Service
DAO -> DBConnection / HikariCP -> MariaDB
Controller / Service -> MailService -> SMTP
```

### 주요 설계 포인트

- JSP/Servlet 기반 요청·응답 처리
- Controller가 DAO를 직접 호출하는 흐름과 Service를 거쳐 DAO를 호출하는 흐름이 함께 존재
- 대시보드와 결제 등 일부 JSP에서 Service를 직접 호출
- HikariCP 기반 DB Connection Pool 구성
- JDBC `PreparedStatement`를 사용해 SQL 파라미터 바인딩
- BCrypt 기반 관리자 비밀번호 해시 검증
- Session 기반 로그인 상태 관리
- Jakarta Mail 기반 이메일/OTP 인증
- 민감 설정값은 `application.properties`로 분리하고, Git에는 예시 파일만 포함

### 프로젝트 구조

```text
src/main/java/org/example/smart_parking_260219
├── connection      # HikariCP 기반 DB 연결
├── controller      # Servlet Controller
├── dao             # JDBC 기반 DB 접근 계층
├── dto             # 화면/서비스 전달 객체
├── filter          # 로그인 접근 제어 필터
├── mail            # 이메일 발송 설정 및 서비스
├── service         # 비즈니스 로직
├── util            # 설정, 비밀번호, 매핑 유틸
└── vo              # DB 매핑 객체

src/main/webapp
├── CSS             # 화면 스타일
├── JS              # 화면 스크립트
└── WEB-INF
    ├── views       # 로그인/인증/관리자 화면
    ├── view        # 주차/결제/통계/요금 정책 화면
    ├── member      # 회원 관리 화면
    └── main        # 공통 메뉴/레이아웃 JSP
```

JSP 화면은 `view`, `views`, `member`, `main` 폴더에 나뉘어 있습니다. 화면 경로 규칙이 일관되지 않아 이후에는 `WEB-INF/views` 아래에서 기능별 폴더로 정리할 필요가 있습니다.

### 주요 테이블

| 테이블 | 역할 |
| --- | --- |
| `manager` | 관리자 계정, 권한, 이메일, 활성화 상태 관리 |
| `validation` | 관리자 등록·수정과 비밀번호 찾기의 이메일 인증번호 및 만료 시간 관리 |
| `member` | 회원 차량 정보 및 월정액 상태 관리 |
| `parking_spot` | 주차 공간 상태 관리 |
| `parking` | 차량 입차/출차 기록 관리 |
| `fee_policy` | 주차 요금 정책 관리 |
| `payment` | 결제 내역 관리 |

## 6. 실행 방법

### 실행 환경

- Java 17
- Tomcat 10.x (Jakarta Servlet 6.0 기준)
- MariaDB
- Gradle Wrapper 8.8

### 로컬 설정

민감정보는 소스 코드에 직접 작성하지 않고 `src/main/resources/application.properties`에서 읽습니다. 최초 실행 시 예시 파일을 복사한 뒤 로컬 환경에 맞게 수정합니다.

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

주요 설정값:

```properties
db.url=jdbc:mariadb://localhost:3306/your_database
db.username=your_db_user
db.password=your_db_password

mail.host=smtp.naver.com
mail.port=465
mail.username=your_email@naver.com
mail.password=your_app_password

super.id=super
super.otp=change-me
super.role=SUPER
```

DB 초기 스키마와 샘플 데이터는 `src/main/resources/sql/init.sql`을 참고합니다.

빌드 확인:

```bash
./gradlew clean build
```

테스트 코드는 JUnit 5 기반으로 작성되어 있으나, 일부 테스트는 로컬 MariaDB와 초기 데이터에 의존하는 통합 테스트 성격이 있습니다.

## 7. 담당 기능 흐름

### 관리자 로그인

1. ID/PW와 계정 활성 상태를 확인합니다.
2. 일반 관리자는 입력 이메일과 등록 이메일이 같은지 확인합니다.
3. ADMIN·SUPER 계정은 등록 이메일로 발송한 OTP를 확인합니다.
4. 추가 확인이 끝나면 `fullyAuthenticated`를 Session에 저장합니다.

### 관리자 계정 관리

- 등록: ID 중복, 이메일 형식, 비밀번호 확인 후 저장
- 조회: 관리자 목록과 상세 정보 조회
- 수정: 새 비밀번호를 입력하면 DAO에서 BCrypt로 해싱하고, 입력하지 않으면 기존 해시 유지
- 상태 변경: 관리자 계정 활성화·비활성화, 로그인 중인 본인 계정 비활성화 차단

## 8. 문제 해결과 현재 한계

### 관리자 비밀번호 이중 암호화로 인한 로그인 실패

**문제 상황:** 관리자 정보를 수정한 뒤 새 비밀번호로 로그인되지 않는 문제가 발생했습니다.

**확인한 원인:** 수정 요청의 비밀번호가 Controller와 DAO를 거치는 과정에서 두 번 해싱될 수 있는 구조였습니다.

**정리한 내용:** Controller에서는 입력값을 확인하고, 비밀번호 해싱은 DAO에서 저장 직전에 처리하도록 정리했습니다. 비밀번호를 변경하지 않은 경우에는 DB에 저장된 기존 해시값을 그대로 사용했습니다.

**현재 한계:** DAO는 문자열 길이와 `$2a$` 접두어로 기존 BCrypt 해시인지 판단합니다. 입력받은 비밀번호와 저장된 해시값을 서로 다른 타입으로 구분하는 구조는 적용하지 않았습니다.

### 추가 확인 완료 여부를 필터에서 확인하지 않는 구조

**확인한 내용:** ID/PW 확인이 끝나면 Session에 `loginManager`와 `awaitingSecondAuth`가 저장되고, 등록 이메일 또는 OTP 확인 뒤 `fullyAuthenticated`가 저장됩니다.

**현재 한계:** `LoginCheckFilter`는 `loginManager`가 있는지만 확인합니다. 이 값은 추가 확인 전에도 저장되기 때문에 URL을 직접 요청하면 추가 확인을 완료하지 않은 상태에서도 보호 요청이 통과할 가능성이 있습니다.

**보완 방향:** 필터에서 `fullyAuthenticated == true`까지 확인하거나, 추가 확인이 끝난 뒤에만 최종 로그인 정보를 Session에 저장하도록 흐름을 나눌 필요가 있습니다.

### 그 밖에 확인한 과제

- 관리자 페이지의 역할 판단이 여러 Servlet과 JSP에 나뉘어 있어 공통 필터나 권한 검사 계층으로 모을 필요가 있습니다.
- 관리자 등록의 이메일 확인 완료 여부는 화면 JavaScript 상태에 의존하므로 최종 POST에서도 서버가 다시 확인해야 합니다.
- 개발 과정에서 후속 기능을 빠르게 확인하려고 넣은 고정 OTP 우회 경로와 인증번호 로그가 현재 코드에 남아 있습니다. 일반 인증 흐름과 개발용 설정을 분리할 필요가 있습니다.
- 비밀번호 찾기는 임시 비밀번호를 DB에 먼저 저장한 뒤 메일을 발송합니다. 메일 발송에 실패하면 비밀번호는 바뀌었지만 사용자는 새 비밀번호를 받지 못할 수 있습니다.
- `validation` 테이블은 인증 목적과 사용 완료 여부, 반복 입력 횟수를 저장하지 않습니다.
- 현재 비밀번호 최소 길이는 4자이므로 길이와 조합 규칙을 보완할 필요가 있습니다.

## 9. 회고

이 프로젝트를 통해 JSP/Servlet, JDBC, Session이 연결되는 Java 웹 요청 흐름을 경험했습니다. 관리자 로그인과 추가 확인, 비밀번호 해싱, DB 연동을 구현하면서 화면 이동뿐 아니라 서버가 어떤 세션 값을 확인하는지도 중요하다는 점을 배웠습니다.

향후에는 JSP 화면 폴더 구조 일관화, 결제/출차 처리 트랜잭션 적용, 권한 검증 로직 공통화, 테스트 DB 격리 등을 보완하고 싶습니다.
