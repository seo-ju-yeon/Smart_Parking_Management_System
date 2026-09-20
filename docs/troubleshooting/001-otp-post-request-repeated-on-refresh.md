# TS-001: OTP 결과 화면 새로고침으로 실패 횟수가 다시 증가한 문제

## 증상

로그인 OTP를 잘못 입력했을 때 화면에 표시되는 남은 입력 횟수가 즉시 갱신되지 않았습니다. 결과 화면을 새로고침하면 남은 횟수가 표시되었지만, OTP를 다시 입력하지 않았는데도 새로고침할 때마다 실패 횟수가 추가로 증가했습니다.

## 재현 절차

1. 최고 관리자 계정으로 아이디와 비밀번호 인증을 완료합니다.
2. DB에 등록된 이메일로 로그인 OTP를 발급받습니다.
3. 실제 OTP와 다른 값을 한 번 제출합니다.
4. OTP 결과 화면에서 남은 입력 횟수를 확인합니다.
5. 값을 다시 제출하지 않고 브라우저를 새로고침합니다.
6. 새로고침할 때마다 남은 입력 횟수가 다시 감소하는 것을 확인합니다.

## 관찰한 요청 흐름

OTP 불일치 분기는 실패 횟수를 변경한 뒤 `RequestDispatcher.forward()`로 OTP JSP에 요청을 내부 전달하고 있었습니다.

```text
POST /login/verifyEmailOtp
→ 실패 횟수 증가
→ 서버 내부에서 login_email_otp.jsp로 forward
→ 200 HTML 응답
→ 브라우저 새로고침
→ 직전 POST /login/verifyEmailOtp 재전송
→ 실패 횟수 추가 증가
```

`forward()`는 서버 내부에서 같은 요청과 응답을 JSP로 전달하므로 브라우저에 새로운 GET 요청이 생기지 않습니다. 브라우저가 받은 화면은 POST 요청의 응답으로 남아 있었고, 새로고침 시 해당 POST가 다시 전송되었습니다.

## 원인

실패 횟수를 변경하는 POST 요청과 결과 화면 표시를 하나의 요청에서 처리한 것이 원인이었습니다. OTP 불일치 응답을 `forward()`로 반환했기 때문에 새로고침이 화면 조회가 아니라 OTP 검증 POST의 재요청으로 동작했습니다.

`forward()` 자체가 잘못된 것은 아니지만, 이 경우에는 POST가 실패 횟수라는 서버 상태를 변경하므로 새로고침에 의한 재전송을 분리할 필요가 있었습니다.

## 검토한 대안

### 1. 기존 `forward()` 유지

코드 변경은 없지만 새로고침으로 직전 POST가 다시 전송되는 문제를 해결하지 못합니다.

### 2. JavaScript로 새로고침 제한

브라우저 화면에서 일부 동작을 제한할 수는 있지만 서버는 재전송된 POST를 정상 요청과 구분할 수 없습니다. 서버 상태 변경 문제를 클라이언트 동작에만 의존하게 되므로 선택하지 않았습니다.

### 3. OTP 불일치 처리에 PRG 적용

POST에서는 OTP 검증과 실패 횟수 변경만 수행하고, 결과 화면은 리다이렉트된 GET에서 표시할 수 있습니다. 새로고침하면 GET만 반복되므로 실패 횟수가 다시 변경되지 않습니다.

## 해결

실패 횟수가 증가하는 OTP 불일치 분기에 PRG(Post/Redirect/Get) 패턴을 적용했습니다.

1. POST에서 OTP를 검증하고 실패 횟수를 한 번 증가시킵니다.
2. 오류 메시지를 세션의 `loginOtpFlashError`에 저장합니다.
3. `sendRedirect()`로 `/login/verifyEmailOtp`에 이동합니다.
4. GET에서 `loginOtpFlashError`를 request 속성으로 옮깁니다.
5. 같은 메시지가 다시 표시되지 않도록 세션 속성을 즉시 삭제합니다.
6. GET이 세션의 OTP 상태와 남은 유효 시간을 조회하여 JSP에 전달합니다.

### 변경 후 요청 흐름

```text
POST /login/verifyEmailOtp
→ 실패 횟수 증가
→ 오류 메시지를 세션에 임시 저장
→ 302 Location: /login/verifyEmailOtp
→ GET /login/verifyEmailOtp
→ 메시지를 request로 이동하고 세션에서 삭제
→ 200 HTML 응답
→ 브라우저 새로고침
→ GET /login/verifyEmailOtp만 재전송
```

OTP를 다섯 번 잘못 입력했을 때 현재 OTP를 폐기하는 제한도 같은 작업에서 추가했지만, 이는 POST 중복 전송 문제의 직접적인 해결책은 아닙니다. PRG 적용 후에도 재시도 제한이 정상적으로 동작하는지 확인하는 회귀 검증 항목으로 구분했습니다.

## 검증

### 코드 확인

- `LoginController`의 OTP 불일치 분기는 `redirectToLoginOtpWithError()`를 호출하여 메시지를 저장하고 리다이렉트합니다.
- OTP 화면 GET 처리는 `loginOtpFlashError`를 request로 옮긴 뒤 세션에서 삭제합니다.
- 실패 횟수 증가는 OTP 검증 POST의 불일치 분기에서만 수행합니다.
- `login_email_otp.jsp`는 GET에서 전달받은 OTP 활성 상태와 남은 시간을 사용하여 입력 영역과 타이머를 복원합니다.

관련 구현:

- [`LoginController`](../../src/main/java/org/example/smart_parking_260219/controller/login/LoginController.java)
- [`login_email_otp.jsp`](../../src/main/webapp/WEB-INF/views/auth/login_email_otp.jsp)

### 수동 검증

| 검증 항목 | 확인 결과 |
| --- | --- |
| 잘못된 OTP를 한 번 제출 | 남은 입력 횟수가 1회 감소했습니다. |
| 결과 화면을 새로고침 | 남은 입력 횟수가 추가로 감소하지 않았습니다. |
| 브라우저 Network 탭 확인 | `POST → 302 → GET` 순서를 확인했습니다. |
| OTP를 다섯 번 잘못 제출 | 기존 OTP가 폐기되고 입력 영역이 초기화되었습니다. |

수동 검증 결과는 **4개 항목 모두 통과**했습니다.

## 결과

- OTP 실패 횟수는 사용자가 OTP 검증 POST를 실제로 제출했을 때만 증가합니다.
- 결과 화면을 새로고침하면 GET만 다시 요청되므로 실패 횟수가 추가로 증가하지 않습니다.
- 오류 메시지는 리다이렉트된 GET에서 한 번 사용된 뒤 세션에서 삭제됩니다.
- 다섯 번 실패하면 해당 OTP 상태가 삭제되어 새 OTP를 발급받아야 합니다.

## 배운 점

POST 처리 후 `forward()`와 `redirect()` 중 무엇을 사용할지는 단순한 화면 이동 방식의 차이가 아닙니다. POST가 서버 상태를 변경하고 사용자가 결과 화면을 새로고침할 수 있다면, PRG를 적용하여 상태 변경 요청과 결과 조회 요청을 분리해야 동일한 POST가 반복되는 문제를 방지할 수 있습니다.
