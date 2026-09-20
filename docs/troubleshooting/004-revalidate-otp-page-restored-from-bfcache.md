# TS-004: 뒤로가기로 이전 OTP 페이지가 복원된 문제

## 증상

로그인 OTP 인증을 완료하거나 인증을 취소해 다른 화면으로 이동한 뒤 브라우저의 뒤로가기를 누르면 이전 OTP 입력 화면이 다시 보였습니다.

세션을 무효화하거나 인증 완료 상태를 저장했더라도 화면이 즉시 복원되었습니다. 복원된 화면이 보인다는 사실만으로 인증 상태가 되살아난 것은 아니지만, 이미 끝난 인증 화면과 입력 상태가 사용자에게 다시 노출되고 현재 서버 상태와 다른 화면이 표시되는 문제가 있었습니다.

## 재현 절차

1. 아이디와 비밀번호 인증을 통과하여 OTP 입력 화면으로 이동합니다.
2. OTP를 발급받아 입력 화면과 타이머가 표시된 상태를 만듭니다.
3. OTP 인증을 완료하거나 취소하여 대시보드 또는 로그인 화면으로 이동합니다.
4. 브라우저의 뒤로가기를 누릅니다.
5. 이전 OTP 화면이 입력 상태와 함께 다시 보이는 것을 확인합니다.

## 관찰한 동작

OTP URL을 주소창에서 직접 요청하면 `LoginController`가 현재 세션을 검사할 수 있습니다. 그러나 뒤로가기에서는 서버 로그와 Network 탭에 새로운 GET 요청이 나타나지 않은 상태로 이전 화면이 복원될 수 있습니다.

이는 브라우저가 BFCache(Back/Forward Cache)에 보관한 페이지의 DOM과 JavaScript 상태를 메모리에서 복원했기 때문입니다. 서버 요청이 없으므로 Servlet과 인증 필터는 화면이 복원되는 시점에 현재 세션을 다시 검사할 기회를 얻지 못합니다.

### 변경 전 흐름

```text
OTP 화면 표시
→ 인증 완료 또는 취소
→ 다른 화면으로 이동
→ 브라우저 뒤로가기
→ 서버 요청 없이 BFCache의 OTP 화면 복원
→ 이전 입력 화면이 다시 보임
```

## 원인

서버의 세션 상태와 브라우저가 메모리에 보관한 화면 상태가 서로 다른 시점의 정보를 가지고 있었습니다.

- 서버: 인증 완료 또는 로그아웃으로 OTP 화면을 다시 표시하면 안 되는 상태
- 브라우저: 이전 OTP 문서와 입력 상태를 BFCache에 보관한 상태

서버의 URL 접근 검사만으로는 새 요청이 발생하지 않는 BFCache 복원을 처리할 수 없었습니다. 또한 일반적인 캐시 방지 헤더만으로 모든 브라우저의 BFCache 복원을 동일하게 통제한다고 가정할 수 없었습니다.

## 검토한 대안

### 1. 서버의 OTP URL 접근 검사만 사용

새로운 GET 요청에는 적용되지만 BFCache가 서버 요청 없이 화면을 복원하면 검사 코드가 실행되지 않습니다.

### 2. 캐시 방지 응답 헤더만 설정

로그인·OTP 응답을 일반 HTTP 캐시에 저장하지 않도록 지시할 수 있습니다. 그러나 뒤로가기 복원 방식은 브라우저마다 다를 수 있으므로 헤더만으로 현재 인증 상태 재확인을 보장하지 않습니다.

### 3. 서버 검사, 캐시 방지 헤더, `pageshow` 처리를 함께 적용

일반적인 재요청은 서버에서 검사하고, BFCache 복원은 브라우저의 `pageshow` 이벤트로 감지하여 서버에 다시 요청할 수 있습니다. 서로 다른 복원 경로를 나누어 처리할 수 있어 이 방법을 적용했습니다.

## 해결

### 1. 로그인·OTP 응답에 캐시 방지 헤더 설정

`LoginController.doGet()`에서 다음 응답 헤더를 설정했습니다.

```text
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
Expires: 0
```

### 2. OTP URL 요청 시 서버 세션 재검사

- 세션 또는 `loginManager`가 없으면 로그인 화면으로 이동합니다.
- `fullyAuthenticated == true`이면 이미 인증이 끝난 상태이므로 대시보드로 이동합니다.
- 1차 인증만 완료된 세션만 OTP 화면을 표시합니다.

### 3. BFCache 복원 시 페이지 다시 요청

`login_email_otp.jsp`의 `pageshow` 이벤트에서 `event.persisted`가 `true`이면 `window.location.reload()`를 호출합니다. 브라우저가 메모리에서 화면을 복원했더라도 새 GET 요청을 발생시켜 서버가 현재 세션 상태를 다시 판단하도록 했습니다.

### 4. 인증 취소 시 로그아웃 처리

취소 버튼은 단순히 로그인 화면으로 이동하지 않고 `/logout`을 요청합니다. `LogoutController`가 현재 세션을 무효화한 뒤 로그인 화면으로 이동하므로 1차 인증 상태와 OTP 상태가 남지 않습니다.

### 변경 후 흐름

```text
브라우저 뒤로가기
→ BFCache의 OTP 화면 복원
→ pageshow(event.persisted == true)
→ window.location.reload()
→ GET /login/verifyEmailOtp
→ LoginController가 현재 세션 확인
→ 인증 완료 상태이면 /dashboard로 이동
→ 세션이 없으면 /login으로 이동
```

## 검증

### 코드 확인

| 확인 항목 | 변경 전 | 변경 후 |
| --- | --- | --- |
| 로그인·OTP 응답 캐시 헤더 | 별도 설정 없음 | `no-store`, `no-cache`, `must-revalidate` 설정 |
| 인증 완료 후 OTP URL 재접근 | OTP 화면 표시 가능 | `/dashboard`로 이동 |
| BFCache 복원 감지 | 없음 | `pageshow`의 `event.persisted` 확인 |
| BFCache 복원 후 서버 확인 | 없음 | `window.location.reload()`로 GET 재요청 |
| 인증 취소 | 로그인 화면으로 이동 | `/logout`에서 세션 무효화 후 이동 |

관련 구현:

- [`LoginController`](../../src/main/java/org/example/smart_parking_260219/controller/login/LoginController.java)
- [`LogoutController`](../../src/main/java/org/example/smart_parking_260219/controller/login/LogoutController.java)
- [`login_email_otp.jsp`](../../src/main/webapp/WEB-INF/views/auth/login_email_otp.jsp)

### 수동 검증

| 검증 항목 | 확인 결과 |
| --- | --- |
| 수정 전 OTP 화면에서 이동 후 뒤로가기 | 이전 OTP 화면이 복원되는 현상을 확인했습니다. |
| 인증 취소 후 OTP URL 직접 접근 | 로그인 화면으로 이동하는 것을 확인했습니다. |
| 인증 완료 후 OTP URL 직접 접근 | 접근이 차단되는 것을 확인했습니다. |
| 변경 후 뒤로가기로 BFCache 복원 | 코드 적용 후 별도 수동 검증 결과를 기록하지 않았습니다. |

마지막 항목은 확인 결과를 과장하지 않기 위해 통과로 표시하지 않았습니다. Chrome DevTools에서 Network 탭의 요청 기록을 유지한 상태로 뒤로가기를 수행하고, `/login/verifyEmailOtp` GET 재요청과 이후 리다이렉트를 확인할 필요가 있습니다.

## 결과

- 새로 요청된 OTP URL은 서버가 현재 세션과 인증 완료 상태를 기준으로 처리합니다.
- BFCache로 OTP 화면이 복원되면 JavaScript가 페이지를 다시 요청하여 서버 검사를 실행합니다.
- 인증 취소 시 세션을 무효화하여 이전 로그인과 OTP 상태가 유지되지 않도록 했습니다.
- 현재 문서에서는 수정 후 BFCache 동작을 수동 검증했다고 주장하지 않습니다.

## 남은 한계

- `pageshow`를 이용한 BFCache 재검사는 현재 `ADMIN`·`SUPER`가 사용하는 로그인 OTP JSP에만 적용되어 있습니다.
- JavaScript가 실행되지 않는 환경에서는 `pageshow` 재요청이 동작하지 않습니다. 이 경우에도 복원된 화면에서 서버 기능을 사용하려면 새로운 요청이 필요하며, 서버는 해당 요청에서 세션과 인증 상태를 다시 검사해야 합니다.
- 브라우저별 BFCache 적용 조건이 다를 수 있으므로 지원 브라우저에서 수동 검증이 필요합니다.

## 배운 점

브라우저에 이전 인증 화면이 보인다는 현상과 서버 인증 상태가 실제로 복원되는 것은 서로 다른 문제입니다. 서버 접근 제어는 반드시 유지하되, BFCache처럼 서버 요청 없이 화면을 복원하는 브라우저 동작도 별도로 고려해야 사용자에게 현재 인증 상태와 일치하는 화면을 보여줄 수 있습니다.
