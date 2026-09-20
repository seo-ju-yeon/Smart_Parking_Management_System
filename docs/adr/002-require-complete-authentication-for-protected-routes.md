# ADR-002: 권한별 추가 확인 완료 전 보호 경로 접근 차단

- 상태: 승인

## 문제 상황

기존 로그인 흐름은 아이디와 비밀번호가 일치한 직후 세션에 `loginManager`를 저장했습니다. 이후 `NORMAL`은 입력 이메일과 등록 이메일의 일치 여부를 확인하고, `ADMIN`과 `SUPER`는 등록 이메일로 발송된 OTP를 확인했습니다.

그러나 기존 `LoginCheckFilter`는 `loginManager`의 존재만 로그인 완료 조건으로 사용했습니다. 따라서 아이디와 비밀번호만 통과한 사용자가 추가 확인 화면을 거치지 않고 `/dashboard` 등의 URL을 직접 요청하면 보호 기능에 접근할 수 있었습니다.

`LoginCheckFilter`는 `@WebFilter("/*")`로 모든 요청에 적용됩니다. 이 문서에서 보호 경로는 로그인, 로그아웃, 비밀번호 찾기, CSS·JavaScript 등 필터의 예외 경로를 제외한 나머지 요청을 의미합니다.

## 변경 전 흐름

```text
아이디·비밀번호 검증 성공
→ 세션에 loginManager 저장
→ 권한별 추가 확인 대기
→ 사용자가 /dashboard 직접 요청
→ 필터가 loginManager 존재 여부만 확인
→ 보호 경로 접근 허용
```

## 결정 기준

- 역할별로 요구되는 추가 확인을 마치기 전에는 보호 경로를 허용하지 않습니다.
- 각 Servlet이 아니라 `/*`에 적용되는 `LoginCheckFilter`에서 같은 접근 조건을 검사합니다.
- 별도 인증 상태 저장소를 추가하지 않고 기존 `HttpSession`에 완료 상태를 저장합니다.
- 로그인과 추가 확인 화면, 비밀번호 찾기, 정적 리소스는 인증 진행 전에도 접근할 수 있도록 필터에서 제외합니다.

## 결정

세션 속성의 의미를 다음과 같이 구분합니다.

| 세션 속성 | 의미 |
| --- | --- |
| `loginManager` | 아이디와 비밀번호 검증을 통과한 관리자 정보 |
| `fullyAuthenticated` | 해당 역할에 필요한 추가 확인까지 완료한 상태 |

보호 경로는 두 속성이 모두 유효할 때만 허용합니다. 필터는 `loginManager != null`과 `fullyAuthenticated == true`를 함께 확인합니다.

새로운 아이디·비밀번호 검증이 성공하면 이전 로그인에서 남았을 수 있는 `fullyAuthenticated`를 먼저 삭제합니다. 이후 `NORMAL`의 이메일 일치 확인 또는 `ADMIN`·`SUPER`의 이메일 OTP 검증이 성공한 경우에만 `fullyAuthenticated`를 `true`로 저장합니다.

로그인과 추가 확인 화면에는 캐시 방지 헤더를 설정합니다. 인증을 마친 사용자가 추가 확인 URL을 다시 요청하면 대시보드로 이동합니다. `ADMIN`·`SUPER`의 OTP 화면은 BFCache로 복원된 경우 `pageshow` 이벤트에서 페이지를 다시 요청하여 서버 상태를 확인합니다.

## 변경 후 흐름

```text
아이디·비밀번호 검증 성공
→ 이전 fullyAuthenticated 삭제
→ 세션에 loginManager 저장
→ 역할별 추가 확인 성공
→ fullyAuthenticated = true 저장
→ 필터가 두 속성을 모두 확인
→ 보호 경로 접근 허용
```

추가 확인이 끝나지 않으면 `fullyAuthenticated`가 없으므로 필터가 요청을 `/login`으로 리다이렉트합니다.

## 검토한 대안

| 대안 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- |
| JSP와 JavaScript에서만 접근 제한 | 화면 요소를 빠르게 숨길 수 있음 | 주소창이나 HTTP 클라이언트로 보호 URL을 직접 요청할 수 있음 | 제외 |
| 각 Servlet에서 완료 상태 확인 | 경로별로 다른 처리가 가능함 | 모든 Servlet에 검사가 반복되고 한 곳이라도 누락되면 해당 경로가 노출됨 | 제외 |
| 공통 필터에서 두 세션 속성 확인 | `/*`에 같은 조건을 적용하고 기존 필터를 확장할 수 있음 | 예외 경로를 추가하거나 변경할 때 목록을 함께 검토해야 함 | 채택 |

## 결과

### 기대 효과

- 아이디와 비밀번호만 확인된 중간 상태로는 보호 경로에 접근할 수 없습니다.
- `NORMAL`, `ADMIN`, `SUPER`가 각자 요구되는 추가 확인을 끝낸 뒤에만 보호 경로를 요청할 수 있습니다.
- 보호 대상 Servlet마다 같은 세션 검사를 반복하지 않고 `LoginCheckFilter`에서 한 번 검사합니다.
- 새로운 로그인 시작 시 이전 `fullyAuthenticated` 값이 재사용되지 않습니다.

### 감수한 제약

- `EXCLUDE_URLS`는 문자열 접두사 비교로 동작하므로 새 공개 경로를 추가할 때 목록을 갱신해야 합니다.
- 새로운 추가 확인 성공 경로를 만들면 그 성공 지점에서 `fullyAuthenticated`를 설정해야 합니다.
- 인증 완료 상태는 `HttpSession`에 저장되므로 세션이 만료되면 다시 로그인해야 합니다.
- BFCache 복원 시 서버 재확인은 현재 `ADMIN`·`SUPER` OTP 화면에만 구현되어 있습니다.

## 검증

### 코드 확인

| 확인 항목 | 변경 전 | 변경 후 |
| --- | --- | --- |
| 필터 적용 범위 | `/*` | `/*` |
| 보호 경로 통과 조건 | `loginManager != null` | `loginManager != null && fullyAuthenticated == true` |
| 새 로그인 시 이전 완료 상태 삭제 | 없음 | `removeAttribute("fullyAuthenticated")` |
| 공통 완료 상태 | 없음 | 이메일 일치 또는 OTP 성공 시 `fullyAuthenticated = true` |
| BFCache 복원 처리 | 없음 | 관리자 OTP 화면에서 페이지 재요청 |

### 빌드 확인

- Java 17 환경에서 `./gradlew clean war`가 성공했습니다.
- 이 결과는 코드가 컴파일되고 WAR가 생성된다는 의미이며, 접근 차단 동작은 아래 수동 검증으로 별도 확인했습니다.

### 수동 검증

| 시나리오 | 예상 결과 | 확인 결과 |
| --- | --- | --- |
| 아이디와 비밀번호만 통과한 뒤 `/dashboard` 직접 요청 | `/login`으로 이동 | 통과 |
| 역할별 추가 확인을 완료한 뒤 보호 경로 요청 | 요청 허용 | 통과 |

수동 검증 결과: **2/2 통과**

## 관련 구현

- [LoginCheckFilter.java](../../src/main/java/org/example/smart_parking_260219/filter/LoginCheckFilter.java)
- [LoginController.java](../../src/main/java/org/example/smart_parking_260219/controller/login/LoginController.java)
- [login_email_otp.jsp](../../src/main/webapp/WEB-INF/views/auth/login_email_otp.jsp)
