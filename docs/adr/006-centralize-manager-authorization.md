# ADR-006: 관리자 역할 단순화와 권한 검사 공통화

- 상태: 승인

## 문제 상황

시연용 인증 우회를 제거한 뒤 `SUPER`와 `ADMIN` 사이에 실제 기능 차이가 남지 않았습니다. 같은 관리자 기능을 사용하는 두 역할을 계속 유지하면 새로운 기능을 추가할 때 두 역할의 권한을 각각 판단해야 하고, 역할 이름만으로 기능 차이를 설명하기도 어려웠습니다.

인증된 사용자의 역할 판단도 여러 Servlet과 JSP에 나뉘어 있었습니다. `LoginCheckFilter`는 `loginManager`와 `fullyAuthenticated`를 확인했지만, 이는 로그인과 추가 인증 완료 여부만 판단합니다. 인증을 완료한 `NORMAL`이 `/mgr/add`처럼 ADMIN 전용 URL을 직접 요청했을 때 모든 경로를 같은 규칙으로 차단하는 계층은 없었습니다.

또한 URL 접근 권한과 실제 수정 대상 권한은 서로 다른 문제입니다. ADMIN 전용 URL을 통과했더라도 요청 파라미터의 관리자 ID를 그대로 사용하면 본인 수정 화면에서 다른 ADMIN을 지정하거나 NORMAL 수정 화면에서 ADMIN을 대상으로 바꾸는 요청을 별도로 막아야 합니다.

## 변경 전 흐름

```text
요청
→ LoginCheckFilter가 로그인·추가 인증 완료 여부 확인
→ Servlet 또는 JSP마다 역할 판단
→ 역할 검사가 없거나 서로 다르면 요청 파라미터의 대상에 작업 가능
```

## 결정 기준

- 역할은 실제 기능 차이를 표현해야 합니다.
- 로그인 완료 여부와 역할별 기능 권한을 서로 다른 책임으로 구분합니다.
- ADMIN 전용 URL은 한 곳에서 같은 규칙으로 검사합니다.
- JSP의 메뉴 숨김이나 JavaScript 검사를 서버 권한 검사의 근거로 사용하지 않습니다.
- URL 권한을 통과한 뒤에도 Controller가 실제 조회·수정 대상을 다시 확인합니다.
- 필터 실행 순서를 설정 파일에서 확인할 수 있어야 합니다.

## 결정

### 1. 관리자 역할을 두 가지로 단순화

`ManagerRole`은 `NORMAL`, `ADMIN`만 사용합니다. Flyway `V3__simplify_manager_roles.sql`에서 기존 `SUPER` 데이터를 `ADMIN`으로 변경하고, 데이터베이스 역할 제약도 두 값만 허용하도록 변경합니다.

| 역할 | 기능 범위 |
| --- | --- |
| `NORMAL` | 일반 운영 기능과 본인 정보 수정 |
| `ADMIN` | 일반 운영 기능, 관리자 계정 관리, 요금 정책 변경과 본인 정보 수정 |

화면의 “일반 관리자”는 `NORMAL`, “최고 관리자”는 `ADMIN`을 의미합니다.

### 2. 역할별 URL 권한을 AuthorizationFilter에서 검사

`AuthorizationFilter`를 추가하고 `web.xml`에서 `LoginCheckFilter` 다음에 실행되도록 등록합니다.

```text
요청
→ LoginCheckFilter: 로그인·추가 인증 완료 여부 검사
→ AuthorizationFilter: 역할별 URL 접근 권한 검사
→ Controller: 실제 대상 데이터 권한 검사
→ JSP: 허용된 결과 표시
```

적용한 URL 규칙은 다음과 같습니다.

| 대상 경로 | 허용 역할 |
| --- | --- |
| `/mgr`, `/mgr/`, `/mgr/**` | `ADMIN` |
| `/mgr/my_modify` | `NORMAL` |
| `/view/policy/add`, `/view/policy/apply` | `ADMIN` |
| 요금 정책 조회와 일반 운영 경로 | 인증을 완료한 `NORMAL`, `ADMIN` |

`/mgr/my_modify`는 `/mgr/**` 규칙의 예외로 먼저 구분합니다. 인증이 완료됐지만 역할이 경로 규칙과 다르면 `403 Forbidden`을 반환합니다.

### 3. Controller에서 실제 대상 권한을 다시 확인

필터는 URL만 알 수 있고 요청이 가리키는 관리자 계정의 존재 여부와 역할은 알 수 없습니다. 따라서 관리자 조회·수정 Controller에는 다음 검사를 유지합니다.

- ADMIN 본인 수정 대상은 요청 ID가 아니라 세션의 로그인 ID로 고정합니다.
- NORMAL 상세 조회와 수정은 DB에서 조회한 대상 역할이 `NORMAL`인지 확인합니다.
- ADMIN이 다른 ADMIN 계정을 NORMAL 수정 경로로 지정하면 차단합니다.
- 계정 활성화 변경은 대상 존재 여부, 대상 역할, `active` 값과 본인 계정 여부를 확인합니다.
- NORMAL 본인 수정은 요청 ID와 세션 ID가 일치하는지 확인합니다.

### 4. 관리자 수정 OTP를 대상 계정과 연결

관리자 수정 화면의 JavaScript 인증 완료 값만으로는 직접 POST 요청을 막을 수 없습니다. OTP 발송 단계에서 수정 대상 관리자 ID를 Session에 저장하고, 검증 성공 시 다음 값을 수정 완료 증명으로 저장합니다.

| 세션 속성 | 의미 |
| --- | --- |
| `managerModifyVerifiedId` | OTP 인증을 완료한 수정 대상 관리자 ID |
| `managerModifyVerifiedEmail` | OTP 인증을 완료한 제출 이메일 |

최종 `/mgr/modify`, `/mgr/modify_normal`, `/mgr/my_modify` POST는 실제 대상 ID와 제출 이메일이 두 값과 모두 일치할 때만 수정합니다. 수정 성공 후에는 두 속성을 삭제하고, 새로운 수정 화면을 열거나 인증 상태가 일치하지 않을 때도 삭제합니다.

JSP에서는 역할에 따른 리다이렉트를 수행하지 않습니다. 역할별 메뉴 노출과 ADMIN 표시처럼 화면 구성을 위한 조건만 유지하며, 실제 권한은 필터와 Controller가 판단합니다.

## 검토한 대안

| 대안 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- |
| `SUPER`와 `ADMIN` 유지 | 기존 데이터와 화면 변경이 적음 | 실제 기능 차이가 없어 모든 권한 검사에 중복 조건이 남음 | 제외 |
| 각 Servlet과 JSP에서 역할 검사 | 화면별로 바로 조건을 추가할 수 있음 | 경로가 늘어날수록 누락과 서로 다른 처리 방식이 발생할 수 있음 | 제외 |
| `LoginCheckFilter`에 역할 검사까지 추가 | 필터 파일이 하나만 필요함 | 인증 완료 여부와 기능 권한 책임이 한 클래스에 섞임 | 제외 |
| 별도 `AuthorizationFilter`와 Controller 대상 검증 사용 | URL 규칙을 공통화하면서 실제 대상도 변경 직전에 확인할 수 있음 | URL 규칙과 Controller 검사를 함께 관리해야 함 | 채택 |

## 결과

### 기대 효과

- 현재 기능과 일치하지 않는 `SUPER` 역할 분기를 제거했습니다.
- 로그인 완료 여부와 역할별 접근 권한의 책임이 서로 다른 필터로 구분됩니다.
- ADMIN 전용 경로가 추가되면 `AuthorizationFilter`의 경로 규칙에서 확인할 수 있습니다.
- URL의 ID나 폼의 hidden 값을 바꿔도 Controller가 세션 사용자와 DB 대상 역할을 다시 확인합니다.
- 관리자 수정 OTP 완료 상태를 다른 관리자나 다른 이메일의 수정에 사용할 수 없습니다.

### 감수한 제약

- 문자열 기반 URL 규칙이므로 관리자 전용 경로를 추가할 때 `AuthorizationFilter`도 함께 검토해야 합니다.
- URL 권한과 대상 데이터 권한이 다른 계층에 있으므로 두 검사의 목적을 구분해서 유지해야 합니다.
- 권한 규칙을 데이터베이스에서 동적으로 관리하지 않고 코드에 고정합니다.
- 세션 기반 OTP 인증 상태이므로 세션이 만료되면 다시 인증해야 합니다.

## 검증

### 코드 확인

- `ManagerRole` enum이 `NORMAL`, `ADMIN` 두 값만 포함하는 것을 확인했습니다.
- Java와 JSP에서 `ManagerRole.SUPER` 참조가 남지 않았음을 재검색했습니다.
- `web.xml`에서 `LoginCheckFilter`, `AuthorizationFilter` 순서로 `/*`에 매핑된 것을 확인했습니다.
- 관리자 수정 세 화면의 OTP 발송·검증 요청이 대상 `managerId`를 함께 전송하는 것을 확인했습니다.
- 관리자 수정 최종 POST 세 경로가 인증 완료 ID와 이메일을 검사하는 것을 확인했습니다.

### 빌드 확인

- Java 17 환경에서 `./gradlew clean war`가 성공했습니다.
- 이 결과는 역할 enum, 필터, Controller와 JSP 변경 후에도 코드가 컴파일되고 WAR가 생성된다는 의미이며, 접근 권한과 OTP 동작은 아래 수동 검증으로 별도 확인했습니다.

### 수동 검증

| 시나리오 | 예상 결과 | 확인 결과 |
| --- | --- | --- |
| `NORMAL`이 ADMIN 전용 관리자 경로 직접 요청 | `403 Forbidden` | 통과 |
| `ADMIN`이 NORMAL 본인 수정 경로 직접 요청 | `403 Forbidden` | 통과 |
| 관리자 조회·수정 요청의 대상 ID 또는 역할 변조 | 요청 차단 | 통과 |
| OTP 인증 없이 NORMAL 관리자 수정 POST | 수정 화면에서 인증 요구 | 통과 |
| 발송 대상과 다른 관리자 ID로 OTP 검증 | `400 Bad Request` | 통과 |
| 올바른 대상 ID·이메일·OTP 검증 후 수정 | 수정 처리 후 관리자 목록으로 이동 | 통과 |
| 사용한 수정 인증 상태로 다시 수정 POST | 인증 요구로 차단 | 통과 |

수동 검증 결과: **7/7 통과**

## 관련 구현

- [ManagerRole.java](../../src/main/java/org/example/smart_parking_260219/vo/ManagerRole.java)
- [AuthorizationFilter.java](../../src/main/java/org/example/smart_parking_260219/filter/AuthorizationFilter.java)
- [LoginCheckFilter.java](../../src/main/java/org/example/smart_parking_260219/filter/LoginCheckFilter.java)
- [web.xml](../../src/main/webapp/WEB-INF/web.xml)
- [ManagerViewController.java](../../src/main/java/org/example/smart_parking_260219/controller/login/ManagerViewController.java)
- [ManagerModifyController.java](../../src/main/java/org/example/smart_parking_260219/controller/login/ManagerModifyController.java)
- [SendAuthCodeController.java](../../src/main/java/org/example/smart_parking_260219/controller/login/SendAuthCodeController.java)
- [VerifyAuthCodeController.java](../../src/main/java/org/example/smart_parking_260219/controller/login/VerifyAuthCodeController.java)
- [V3__simplify_manager_roles.sql](../../src/main/resources/db/migration/V3__simplify_manager_roles.sql)
