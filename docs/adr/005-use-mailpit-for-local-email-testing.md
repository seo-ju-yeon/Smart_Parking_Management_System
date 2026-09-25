# ADR-005: 로컬 이메일 확인에 Mailpit 사용

- 상태: 승인

## 문제 상황

관리자 로그인, 관리자 등록·수정, 비밀번호 찾기는 이메일 OTP 수신이 필요합니다. 기존 설정은 네이버 SMTP 서버와 실제 계정 인증을 전제로 했기 때문에 로컬 실행자마다 SMTP 계정과 앱 비밀번호를 준비해야 했습니다. 외부 SMTP 상태나 계정 설정에 문제가 생기면 애플리케이션의 OTP 흐름 자체를 확인하기 어려웠습니다.

Flyway의 로컬 시연 계정은 `@smartparking.local` 가상 주소를 사용합니다. 이 주소로는 실제 외부 메일을 받을 수 없으므로, 인증 우회를 다시 추가하지 않으면서 로컬에서 발송 내용과 OTP를 확인할 수단이 필요했습니다.

## 변경 전 흐름

```text
OTP 발송 요청
→ Java 코드가 네이버 SMTP 인증·SSL 설정 사용
→ 실제 SMTP 계정으로 외부 메일 발송
→ 실제 수신함에서 OTP 확인
```

이 흐름은 로컬 기능 확인도 외부 계정과 네트워크에 의존합니다.

## 결정 기준

- 고정 OTP, 인증 생략, OTP 로그 출력 없이 실제 메일 발송 코드를 실행합니다.
- 로컬 시연용 가상 이메일 주소로 발송한 메일도 확인할 수 있어야 합니다.
- 실제 SMTP 계정과 비밀번호를 저장소에 포함하지 않습니다.
- 로컬과 외부 SMTP 환경을 바꿀 때 Java 코드를 수정하지 않습니다.
- 인증, SSL, STARTTLS 사용 여부가 설정 파일에 드러나야 합니다.

## 결정

Docker Compose에 `axllent/mailpit:v1.31.2` 기반 `mailpit` 서비스를 추가합니다. SMTP 포트는 기본 `1025`, 웹 UI 포트는 기본 `8025`를 사용하며 두 포트 모두 `127.0.0.1`에만 바인딩합니다.

로컬 애플리케이션은 다음 설정으로 Mailpit에 메일을 전송합니다.

| 설정 | 로컬 값 | 의미 |
| --- | --- | --- |
| `mail.host` | `localhost` | SMTP 서버 주소 |
| `mail.port` | `1025` | Mailpit SMTP 포트 |
| `mail.from` | `no-reply@smartparking.local` | 화면에 표시할 발신 주소 |
| `mail.auth` | `false` | 로컬 SMTP 인증 사용 안 함 |
| `mail.ssl.enable` | `false` | 로컬 SSL 연결 사용 안 함 |
| `mail.starttls.enable` | `false` | 로컬 STARTTLS 사용 안 함 |
| `mail.debug` | `false` | SMTP 상세 로그 출력 안 함 |

네이버 전용 `NaverEmailConfig`를 제거하고 `SmtpConfig`가 공통 SMTP 속성을 생성하도록 변경합니다. `mail.auth=false`이면 Jakarta Mail에 `Authenticator`를 전달하지 않습니다. `mail.auth=true`인 환경에서만 `mail.username`과 `mail.password`를 읽습니다.

실제 SMTP를 사용할 때는 같은 설정 키를 환경변수 또는 Git에서 제외된 `application.properties`에 제공합니다. `AppConfig`는 `mail.host`를 `MAIL_HOST` 형식의 환경변수로 변환하여 파일 설정보다 먼저 읽습니다. 네이버 SMTP에 필요한 예시값은 `config/naver-smtp.properties.example`에 참고용으로만 보관하며 이 파일은 자동으로 로드하지 않습니다.

## 변경 후 흐름

```text
로컬 OTP 발송 요청
→ MailService가 SmtpConfig에서 Mailpit 설정 조회
→ Jakarta Mail이 localhost:1025로 메일 전송
→ Mailpit이 외부로 발송하지 않고 메일을 보관
→ 사용자가 localhost:8025에서 OTP 확인
→ 기존 서버 OTP 검증 흐름 수행
```

외부 SMTP 환경에서는 설정값만 호스트, 포트, 인증·암호화 값으로 교체하며 `MailService` 코드는 동일하게 사용합니다.

## 검토한 대안

| 대안 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- |
| 실제 네이버 SMTP만 사용 | 실제 외부 발송까지 한 번에 확인할 수 있음 | 실행자마다 실제 계정과 앱 비밀번호가 필요하고 외부 서비스에 의존함 | 제외 |
| OTP를 로그에 출력 | 별도 메일 서버 없이 번호를 확인할 수 있음 | 인증번호가 로그에 남아 민감정보 로그 제거 결정과 충돌함 | 제외 |
| 로컬에서 고정 OTP 또는 발송 생략 | 시연 절차가 단순함 | ADR-001에서 제거한 인증 우회 경로가 다시 생김 | 제외 |
| Mailpit으로 SMTP 메일 수집 | 실제 발송 코드를 실행하면서 가상 주소의 메일도 웹 UI에서 확인할 수 있음 | 외부 SMTP 공급자의 인증·전송 동작은 검증하지 않음 | 채택 |

## 결과

### 기대 효과

- 실제 SMTP 계정 없이 로그인·관리자 관리·비밀번호 찾기의 이메일 발송 흐름을 확인할 수 있습니다.
- `@smartparking.local` 시연 계정의 OTP를 Mailpit 웹 UI에서 확인할 수 있습니다.
- 고정 OTP나 인증번호 로그 없이 기존 OTP 생성·검증 코드를 그대로 사용합니다.
- 로컬과 외부 SMTP의 차이를 설정값으로 분리하여 메일 공급자를 바꿀 때 Java 코드를 수정하지 않습니다.
- 발신 주소가 SMTP 로그인 아이디와 분리되어 인증을 사용하지 않는 로컬 환경에서도 명시적으로 설정됩니다.

### 감수한 제약

- Mailpit은 메일을 외부 수신자에게 전달하지 않으므로 실제 도메인의 수신 여부를 검증하지 않습니다.
- 로컬 설정은 인증과 암호화를 사용하지 않으므로 로컬 컴퓨터 밖의 SMTP 환경에 그대로 사용하면 안 됩니다.
- 네이버 등 실제 SMTP를 사용하려면 공급자가 요구하는 포트, 인증, SSL 또는 STARTTLS 값을 별도로 설정해야 합니다.
- Mailpit 컨테이너를 제거하면 보관 중인 로컬 메일도 사라집니다.
- 재발송할 때마다 Mailpit에는 별도의 메일이 한 건씩 저장되며, 서버에서는 마지막으로 발급한 OTP 상태를 사용합니다.

## 검증

### 설정 확인

- `docker compose config --quiet`가 오류 없이 종료되어 `mailpit` 서비스와 포트 설정이 유효함을 확인했습니다.
- `SmtpConfig`에서 `mail.auth=false`일 때 SMTP 사용자명과 비밀번호를 읽지 않는 분기를 확인했습니다.
- `NaverEmailConfig` 참조가 남아 있지 않고, 실제 SMTP의 사용자명과 비밀번호 예시는 참고 파일에만 존재함을 재검색했습니다.

### 빌드 확인

- Java 17 환경에서 `./gradlew clean war`가 성공했습니다.
- 이 결과는 `NaverEmailConfig`를 제거하고 `SmtpConfig`로 교체한 뒤에도 코드가 컴파일되고 WAR가 생성된다는 의미이며, 메일 수신과 OTP 동작은 아래 수동 검증으로 별도 확인했습니다.

### 수동 검증

| 시나리오 | 예상 결과 | 확인 결과 |
| --- | --- | --- |
| `demo_admin` 계정으로 로그인 OTP 요청 | Mailpit에 OTP 메일 수신 | 통과 |
| OTP 재발송 3회 | Mailpit에는 요청별 메일이 저장되고 화면 안내는 한 개만 표시 | 통과 |
| Mailpit에서 확인한 OTP 입력 | 2차 인증 완료 후 보호 경로 접근 | 통과 |

수동 검증 결과: **3/3 통과**

## 관련 구현

- [docker-compose.yml](../../docker-compose.yml)
- [SmtpConfig.java](../../src/main/java/org/example/smart_parking_260219/mail/SmtpConfig.java)
- [MailService.java](../../src/main/java/org/example/smart_parking_260219/mail/MailService.java)
- [application.properties.example](../../src/main/resources/application.properties.example)
- [naver-smtp.properties.example](../../config/naver-smtp.properties.example)
- [login_email_otp.jsp](../../src/main/webapp/WEB-INF/views/auth/login_email_otp.jsp)
- [login-email-otp.js](../../src/main/webapp/js/auth/login-email-otp.js)
