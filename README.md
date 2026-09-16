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
- Java 17 Toolchain을 적용해 빌드에 사용하는 Java 버전을 고정했습니다.
- JSP와 CSS·JavaScript 파일을 기능별 디렉터리로 분류하고 참조 경로를 통일했습니다.

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
├── index.jsp
├── css
│   ├── auth        # 로그인 화면 스타일
│   ├── common      # 공통 스타일
│   ├── dashboard   # 대시보드 스타일
│   ├── member      # 회원 관리 스타일
│   ├── payment     # 결제 화면 스타일
│   └── statistics  # 통계 화면 스타일
├── js
│   ├── common      # 공통 스크립트
│   ├── dashboard   # 대시보드 스크립트
│   ├── member      # 회원 관리 스크립트
│   └── payment     # 결제 화면 스크립트
└── WEB-INF
    ├── web.xml
    └── views
        ├── auth        # 로그인/이메일 인증 화면
        ├── common      # 공통 메뉴와 레이아웃
        ├── dashboard   # 대시보드
        ├── entry       # 입차 관리
        ├── exit        # 출차 관리
        ├── manager     # 관리자 관리
        ├── member      # 회원 관리
        ├── payment     # 결제 관리
        ├── policy      # 요금 정책 관리
        └── statistics  # 통계 화면
```

JSP는 브라우저에서 직접 접근하지 않도록 `WEB-INF/views` 아래에 두고 기능별 디렉터리로 구분했습니다. CSS와 JavaScript도 기능별로 분류했으며, 디렉터리명과 파일 참조 경로는 소문자로 통일했습니다. 정적 리소스를 참조할 때는 애플리케이션의 배포 경로가 바뀌어도 동작하도록 Context Path를 기준으로 경로를 생성합니다.

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
```

DB 초기 스키마와 샘플 데이터는 `src/main/resources/sql/init.sql`을 참고합니다.

빌드 확인:

```bash
./gradlew clean war
```

생성된 WAR 파일은 `build/libs`에서 확인할 수 있습니다. Gradle Java Toolchain을 Java 17로 고정해 실행 중인 로컬 JDK와 관계없이 같은 Java 버전을 기준으로 컴파일합니다.

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

## 8. 개선한 내용

### Java 17 Toolchain 고정

개발 환경마다 사용하는 JDK 버전이 달라 빌드 결과가 달라지는 문제를 줄이기 위해 Gradle Java Toolchain을 Java 17로 고정했습니다. 프로젝트가 요구하는 Java 버전을 빌드 설정에 명시해 동일한 기준으로 컴파일할 수 있도록 했습니다.

### JSP·CSS·JavaScript 구조 통일

여러 경로에 나뉘어 있던 JSP를 `WEB-INF/views` 아래의 기능별 디렉터리로 정리했습니다. CSS와 JavaScript도 소문자 디렉터리와 기능별 구조로 통일하고, Servlet과 JSP의 화면 이동 경로 및 정적 리소스 참조 경로를 함께 수정했습니다.

## 9. 회고

이 프로젝트를 통해 JSP/Servlet, JDBC, Session이 연결되는 Java 웹 요청 흐름을 경험했습니다. 관리자 로그인과 추가 확인, 비밀번호 해싱, DB 연동을 구현하면서 화면 이동뿐 아니라 서버가 어떤 세션 값을 확인하는지도 중요하다는 점을 배웠습니다.
