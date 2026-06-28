# Smart Parking Management System

JSP/Servlet 기반의 주차장 관리자용 웹 시스템입니다. 차량 입출차, 회원, 결제, 요금 정책, 통계, 관리자 계정 관리를 하나의 관리자 화면에서 처리할 수 있도록 팀 프로젝트로 구현했습니다.

> GitHub README는 1차 검토용 요약 문서로 구성했습니다. 상세 화면 흐름과 보충 설명은 별도 포트폴리오 문서에서 다룹니다.

## 1. 프로젝트 개요

- 기간: 2026.02
- 인원: 5명
- 형태: JSP/Servlet 기반 팀 프로젝트
- 목표: 주차장 운영자가 차량 입출차, 결제, 회원, 요금 정책, 통계를 웹에서 관리할 수 있는 관리자 시스템 구현
- 담당 역할: 관리자 인증/인가, 이메일/OTP 2차 인증, 관리자 CRUD, 접근 제어

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

## 3. 주요 기능

- 관리자 로그인/로그아웃
- 일반 관리자 이메일 2차 인증
- 최고 관리자 이메일 OTP 인증
- 관리자 등록, 조회, 수정, 활성화/비활성화
- 회원 및 차량 정보 관리
- 차량 입차/출차 관리
- 주차 요금 계산 및 결제 관리
- 요금 정책 관리
- 주차 현황 및 통계 대시보드

## 4. 담당 기능

### 관리자 인증/인가

- `LoginController`에서 ID/PW 기반 1차 인증 후 관리자 권한에 따라 2차 인증 화면으로 분기
- 일반 관리자는 등록 이메일 확인, 최고 관리자는 이메일 OTP 인증까지 수행
- `LoginCheckFilter`를 통해 미인증 사용자의 보호 페이지 접근 제한
- 계정 비활성화 상태에서는 로그인할 수 없도록 인증 흐름에 반영

### 관리자 계정 관리

- 관리자 등록, 목록 조회, 상세 조회, 정보 수정, 활성화/비활성화 기능 구현
- 관리자 ID 중복 확인, 이메일 형식 검증, 비밀번호 확인 검증 처리
- 본인 계정 비활성화 방지 및 요청 ID 위조 방어 로직 적용
- 관리자 기능이 커지면서 조회, 등록, 수정 컨트롤러를 분리해 요청 책임을 명확히 개선

### 이메일/OTP 인증

- Jakarta Mail 기반 이메일 인증번호 발송
- 인증번호 저장/조회/만료 검증을 위한 `validation` 테이블 사용
- 비밀번호 찾기 흐름에서 관리자 ID와 등록 이메일이 일치하는지 재검증
- 비밀번호는 평문 저장 없이 BCrypt 해시값으로 저장

## 5. 아키텍처 및 구조

### 요청 흐름

```text
JSP View
  -> LoginCheckFilter
  -> Servlet Controller
  -> Service
  -> DAO
  -> HikariCP
  -> MariaDB
```

### 주요 설계 포인트

- JSP/Servlet 기반 MVC 구조 적용
- Controller, Service, DAO 계층 분리
- HikariCP 기반 DB Connection Pool 구성
- `PreparedStatement`를 사용해 SQL 실행 안정성 확보
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

초기 팀 개발 과정에서 JSP 화면 폴더가 `view`, `views`, `member`, `main`으로 분산되었습니다. 제출 전에는 화면 경로 변경에 따른 기능 오류를 피하기 위해 현재 구조를 유지했고, 향후에는 `WEB-INF/views` 하위에 기능별 폴더로 통일하는 방향이 적절하다고 판단했습니다.

### 주요 테이블

| 테이블 | 역할 |
| --- | --- |
| `manager` | 관리자 계정, 권한, 이메일, 활성화 상태 관리 |
| `validation` | 이메일 인증번호와 만료 시간 관리 |
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

## 7. 주요 화면

README에는 담당 기능 중심의 대표 화면만 선별하고, 자세한 화면 흐름은 별도 포트폴리오 문서에서 설명합니다.

### 담당 기능 화면

- 로그인 화면
- 일반 관리자 이메일 2차 인증 화면
- 최고 관리자 OTP 인증 화면
- 관리자 목록 화면
- 관리자 등록/수정 화면
- 관리자 상세 및 활성화/비활성화 화면

### 프로젝트 전체 참고 화면

- 대시보드 화면

## 8. 트러블슈팅

### 관리자 비밀번호 이중 암호화로 인한 로그인 실패

관리자 정보 수정 후 올바른 비밀번호를 입력해도 로그인이 실패하는 문제가 있었습니다. 원인은 Controller와 DAO 양쪽에서 BCrypt 해싱이 중복 수행되어, 이미 해싱된 비밀번호가 다시 해싱되는 구조였습니다.

비밀번호 암호화 책임을 DAO 계층으로 통일하고, 기존 BCrypt 해시값은 재해싱하지 않도록 분기하여 해결했습니다. 이를 통해 비밀번호 변경 후에도 정상 로그인할 수 있고, 비밀번호를 변경하지 않는 수정 요청에서는 기존 해시값을 유지할 수 있게 되었습니다.

### 2차 인증 전 보호 페이지 접근 가능성 점검

로그인 흐름을 점검하면서, 필터가 `loginManager` 세션 존재 여부만 확인하면 2차 인증 완료 전에도 보호 페이지 접근 가능성이 있다는 점을 발견했습니다.

이를 개선하기 위해 1차 인증 대기 상태와 최종 인증 완료 상태를 구분하고, 보호 페이지 접근 시 `fullyAuthenticated` 값까지 확인하는 구조가 필요하다고 정리했습니다. 2차 인증 URL은 예외 경로로 두고, 인증 완료 후 OTP 관련 세션 값을 제거하는 방식으로 접근 제어 흐름을 명확히 할 수 있습니다.

## 9. 회고

이 프로젝트를 통해 JSP/Servlet, JDBC, Session 기반 인증 흐름을 직접 구현하며 Java 웹 애플리케이션의 기본 구조를 경험했습니다. 특히 관리자 인증/인가, 비밀번호 해싱, 이메일 OTP, DB 연동 흐름을 구현하면서 업무 시스템에서 보안 흐름과 예외 처리가 중요하다는 점을 배웠습니다.

향후에는 JSP 화면 폴더 구조 일관화, 결제/출차 처리 트랜잭션 적용, 권한 검증 로직 공통화, 테스트 DB 격리 등을 보완하고 싶습니다.
