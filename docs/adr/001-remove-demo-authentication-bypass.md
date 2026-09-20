# ADR-001: 시연용 인증 우회 제거

- 상태: 승인

## 문제 상황

프로젝트 시연 과정에서 후속 기능을 빠르게 확인하기 위해 특정 계정과 고정 OTP를 이용한 인증 우회 기능을 추가했습니다.

`LoginController`, `VerifyAuthCodeController`, `ValidationService`는 입력값이 시연용 조건과 일치하면 실제로 발급된 OTP를 비교하지 않고 인증을 성공시킬 수 있었습니다. 특정 계정은 이메일 발송도 생략할 수 있었습니다. 이 우회 로직이 저장소에 남아 있으면 프로젝트를 다른 환경에서 실행하거나 공유할 때도 동일한 인증 우회 경로가 포함됩니다.

`SUPER`는 관리자 기능 범위를 구분하기 위해 기존부터 사용하던 역할입니다. 이번 결정의 대상은 역할 자체가 아니라 해당 역할과 특정 계정에 연결되어 있던 인증 우회 동작입니다.

## 변경 전 흐름

```text
OTP 발송 또는 검증 요청
→ 시연용 설정에서 특정 계정·고정 OTP 여부 확인
→ 조건이 일치하면 이메일 발송 또는 정상 OTP 비교 생략
→ 인증 성공
```

## 결정 기준

- 공유되는 소스 코드에 고정 인증값과 우회 분기를 남기지 않습니다.
- `ADMIN`과 `SUPER` 모두 실제 발급된 OTP로 인증합니다.
- 관리자 역할과 인증 수단을 분리합니다.
- 시연 편의는 인증 우회가 아니라 실행 안내와 데모 데이터로 제공합니다.

## 결정

시연용 인증 우회 기능을 완전히 제거합니다.

- `SuperKeyConfig`와 `super.id`, `super.otp`, `super.role` 설정을 삭제합니다.
- 고정 OTP를 곧바로 성공 처리하던 분기 3곳을 삭제합니다.
- 특정 계정의 이메일 발송을 생략하던 분기 1곳을 삭제합니다.
- `SUPER` 역할은 유지하되 다른 관리자와 같은 OTP 발급·검증 절차를 적용합니다.

## 변경 후 흐름

```text
OTP 발송 요청
→ 등록 이메일 확인
→ SecureRandom으로 OTP 생성 및 이메일 발송
→ 서버가 발급한 OTP 상태 저장
→ 사용자가 입력한 OTP와 발급한 OTP 비교
→ 일치할 때만 인증 완료
```

## 검토한 대안

| 대안 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- |
| 기존 우회 기능 유지 | 시연 과정이 빠름 | 설정 노출 또는 활성화 실수로 인증이 무력화될 수 있음 | 제외 |
| 개발 환경에서만 우회 허용 | 환경 분리 경험을 보여줄 수 있음 | 프로필과 환경 변수 오설정 위험이 남고 현재 단계에는 구성이 과도함 | 제외 |
| 인증 우회 완전 제거 | 인증 흐름이 단순하며 환경 설정에 의존하지 않음 | 시연에도 실제 이메일 수신이 필요함 | 채택 |

## 결과

### 기대 효과

- 저장소에 있는 고정값만으로 2차 인증을 통과할 수 없습니다.
- `SUPER`가 권한의 의미로만 사용되어 역할과 인증 책임이 분리됩니다.
- 관리자 인증 성공 조건을 실제 OTP 일치 여부로 일관되게 설명할 수 있습니다.

### 감수한 제약

- 시연자는 실제로 OTP를 수신할 수 있는 이메일 환경을 준비해야 합니다.
- 외부 메일 서비스 장애가 발생하면 관리자 2차 인증도 완료할 수 없습니다.

## 검증

### 코드 확인

| 확인 항목 | 변경 전 | 변경 후 |
| --- | ---: | ---: |
| 인증 우회 전용 설정 클래스 | 1개 | 0개 |
| 고정 인증 설정 키 | 3개 | 0개 |
| 고정 OTP 직접 성공 분기 | 3곳 | 0곳 |
| 특정 계정 이메일 발송 생략 분기 | 1곳 | 0곳 |

다음 범위를 재검색하여 우회 설정과 고정 OTP 참조가 남지 않았음을 확인했습니다.

```bash
grep -RInE \
'SuperKeyConfig|superOtpBypass|isSuperAccount|isSuperOtp|슈퍼패스|super\.id|super\.otp|super\.role' \
src/main/java src/main/resources src/main/webapp README.md
```

검색 결과는 0건이었습니다.

### 빌드 확인

- Java 17 환경에서 `./gradlew clean war`가 성공했습니다.
- 이 결과는 인증 우회 설정을 삭제한 뒤에도 코드가 컴파일되고 WAR가 생성된다는 의미이며, 실제 인증 동작은 아래 수동 검증으로 별도 확인했습니다.

### 수동 검증

- 관리자 로그인은 실제 등록 이메일로 발송된 OTP가 일치할 때만 완료되었습니다.
- 잘못된 OTP로는 로그인을 완료할 수 없었습니다.
- `SUPER` 계정도 동일한 2차 인증 절차를 거쳤습니다.

## 관련 구현

- [LoginController.java](../../src/main/java/org/example/smart_parking_260219/controller/login/LoginController.java)
- [VerifyAuthCodeController.java](../../src/main/java/org/example/smart_parking_260219/controller/login/VerifyAuthCodeController.java)
- [ValidationService.java](../../src/main/java/org/example/smart_parking_260219/service/ValidationService.java)
