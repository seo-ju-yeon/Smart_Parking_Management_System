# ADR-0001: 시연용 인증 우회 제거

- 상태: 승인

## 상황

프로젝트를 쉽게 시연하기 위해 고정 OTP와 특정 관리자 권한을 이용한 인증 우회 기능을 추가했었다.
하지만 이 기능이 코드와 설정에 남아 있으면 인증번호를 받지 않은 사용자도 인증 절차를 통과할 수 있다.
환경 설정이 잘못되거나 우회 값이 노출될 경우 실제 배포 환경에서도 인증이 무력화될 위험이 있다.

## 결정

시연용 고정 OTP, 슈퍼 OTP, 특정 권한에 대한 인증 예외와 관련 설정을 모두 제거한다.
OTP 인증이 필요한 모든 경로에서는 서버가 실제로 발급한 인증번호만 허용한다.
`SUPER` 역할은 권한 구분을 위해 유지하지만 인증 우회 조건으로는 사용하지 않는다.
인증번호와 인증 관련 개인정보도 로그에 기록하지 않는다.

## 검토한 대안

### 1. 개발 환경에서만 우회 기능 활성화

환경별 설정으로 기능을 제한할 수 있지만, 설정 실수로 운영 환경에서 활성화될 가능성이 남는다.

### 2. 시연 전용 관리자 계정만 우회 허용

시연은 편리하지만 해당 계정이나 조건이 노출되면 인증 우회 경로로 악용될 수 있다.

### 3. 인증 우회 기능 완전 제거

시연 편의성은 줄어들지만 배포 환경과 동일한 인증 흐름을 검증할 수 있고 보안 위험을 없앨 수 있다.

## 결과

- `ADMIN`과 `SUPER` 계정 모두 실제로 발급된 이메일 OTP를 검증한다.
- 배포 설정에 따라 우회 기능이 실수로 활성화될 가능성이 사라진다.
- 시연할 때도 실제 이메일 인증이 필요하다.
- 향후 시연 편의성은 인증 우회 대신 Docker Compose, 초기 데이터 구성 및 실행 안내로 제공한다.

## 검증 근거

| 검증 항목 | 변경 전 | 변경 후 |
| --- | ---: | ---: |
| 시연용 인증 설정 클래스 | 1개 | 0개 |
| `super.id`, `super.otp`, `super.role` 설정 키 | 3개 | 0개 |
| 우회 관련 키워드가 등장한 소스·설정 줄 수 | 38줄 | 0줄 |
| Java 17 WAR 빌드 | - | 성공 |

현재 소스와 예시 설정에서 다음 명령의 검색 결과가 0건임을 확인했다.

```bash
grep -RInE \
'SuperKeyConfig|superOtpBypass|isSuperAccount|isSuperOtp|슈퍼패스' \
src/main/java src/main/resources/application.properties.example
```

검증 대상 구현: [`LoginController`](../../src/main/java/org/example/smart_parking_260219/controller/login/LoginController.java), [`ValidationService`](../../src/main/java/org/example/smart_parking_260219/service/ValidationService.java)
