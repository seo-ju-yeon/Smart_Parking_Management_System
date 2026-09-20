# TS-002: 관리자 등록 성공 메시지가 다음 화면에 남은 문제

## 증상

관리자를 정상적으로 등록하면 대시보드로 이동했지만 성공 메시지가 표시되지 않았습니다. 이후 관리자 추가 화면에 다시 접근하면 이전 등록의 성공 메시지가 해당 화면에 표시되었고, 화면을 새로고침한 뒤에야 사라졌습니다.

## 재현 절차

1. 관리자 추가 화면에서 새 관리자 정보를 입력하고 등록합니다.
2. 등록 성공 후 대시보드로 이동합니다.
3. 대시보드에 등록 성공 메시지가 표시되지 않는 것을 확인합니다.
4. 관리자 추가 화면으로 다시 이동합니다.
5. 이전 등록의 성공 메시지가 관리자 추가 화면에 표시되는 것을 확인합니다.

## 관찰한 상태 흐름

```text
ManagerAddController
→ 세션에 successMessage 저장
→ /dashboard로 리다이렉트
→ DashboardController가 successMessage를 읽지 않음
→ 세션에 successMessage 유지
→ 이후 mgr_add.jsp가 세션 메시지를 읽고 삭제
```

메시지를 생성한 위치는 `ManagerAddController`, 등록 후 이동하는 화면은 대시보드, 메시지를 읽는 위치는 관리자 추가 JSP였습니다. 메시지의 생성 위치, 리다이렉트 대상, 소비 위치가 서로 일치하지 않았습니다.

## 원인

`successMessage`를 세션에 저장했지만 등록 직후 이동하는 `DashboardController`에는 이를 request로 옮기고 삭제하는 코드가 없었습니다. 반면 `mgr_add.jsp`에는 세션 메시지를 읽고 삭제하는 코드가 있어서, 세션에 남은 메시지가 다음 관리자 추가 요청에서 뒤늦게 소비되었습니다.

세션은 여러 요청에 걸쳐 유지되므로 명시적으로 소비하지 않은 값은 화면이 바뀌어도 자동으로 사라지지 않습니다.

## 검토한 대안

### 1. 등록 성공 후 관리자 추가 화면으로 이동

`/mgr/add`로 리다이렉트하면 기존 JSP에서 메시지를 바로 소비할 수 있습니다. 하지만 관리자를 연속해서 등록할 필요가 없고, 등록 결과를 확인한 뒤 대시보드로 이동하는 현재 흐름을 유지하기로 했기 때문에 선택하지 않았습니다.

### 2. JavaScript `alert()`로 표시

화면에 메시지를 표시하는 방식은 바꿀 수 있지만, 세션에 저장된 `successMessage`를 어느 요청에서 삭제할 것인지에 대한 처리는 여전히 필요합니다. 기존의 페이지 내 메시지 표시 방식도 유지할 수 없으므로 선택하지 않았습니다.

### 3. 대시보드에서 Flash Message 소비

등록 직후 실제로 도착하는 대시보드 요청에서 메시지를 request로 옮기고 세션에서 삭제할 수 있습니다. 메시지의 소비 위치가 리다이렉트 대상과 일치하므로 이 방법을 선택했습니다.

## 해결

1. `ManagerAddController`는 등록 성공 시 세션에 `successMessage`를 저장하고 `/dashboard`로 리다이렉트합니다.
2. `DashboardController`는 세션의 `successMessage`를 현재 request 속성으로 옮깁니다.
3. request로 옮긴 직후 세션의 `successMessage`를 삭제합니다.
4. `dashboard.jsp`는 세션이 아니라 request의 메시지만 표시합니다.
5. `mgr_add.jsp`에 있던 성공 메시지 소비 코드는 제거합니다.

### 변경 후 상태 흐름

```text
ManagerAddController
→ 세션에 successMessage 저장
→ /dashboard로 리다이렉트
→ DashboardController가 메시지를 request로 이동
→ 세션에서 successMessage 삭제
→ dashboard.jsp가 현재 요청의 메시지를 한 번 표시
```

## 검증

### 코드 확인

| 확인 항목 | 변경 전 | 변경 후 |
| --- | --- | --- |
| 메시지 생성 위치 | `ManagerAddController` | 동일 |
| 등록 성공 후 이동 위치 | `/dashboard` | 동일 |
| 메시지 소비 위치 | `mgr_add.jsp` | `DashboardController` |
| JSP가 조회하는 범위 | 관리자 추가 JSP가 세션 조회 | 대시보드 JSP가 request 조회 |
| 대시보드 요청 후 세션 상태 | `successMessage` 유지 | `removeAttribute()`로 삭제 |

현재 구현에서 다음 경로를 확인했습니다.

- `ManagerAddController`가 `successMessage`를 세션에 저장한 뒤 `/dashboard`로 리다이렉트합니다.
- `DashboardController`가 메시지를 request로 옮긴 직후 세션에서 삭제합니다.
- `dashboard.jsp`는 request에 전달된 메시지만 표시합니다.
- `mgr_add.jsp`에는 세션의 `successMessage`를 읽는 코드가 남아 있지 않습니다.

관련 구현:

- [`ManagerAddController`](../../src/main/java/org/example/smart_parking_260219/controller/login/ManagerAddController.java)
- [`DashboardController`](../../src/main/java/org/example/smart_parking_260219/controller/login/DashboardController.java)
- [`dashboard.jsp`](../../src/main/webapp/WEB-INF/views/dashboard/dashboard.jsp)
- [`mgr_add.jsp`](../../src/main/webapp/WEB-INF/views/manager/mgr_add.jsp)

### 수동 검증

문제 수정 전에는 관리자 등록 후 관리자 추가 화면에 다시 접근했을 때 이전 성공 메시지가 표시되는 현상을 재현했습니다. 변경 후 동작은 위 코드 경로를 기준으로 확인했으며, 별도의 수동 검증 횟수는 기록하지 않았습니다.

## 결과

- 성공 메시지를 소비하는 위치가 등록 후 도착하는 대시보드로 이동했습니다.
- 첫 번째 대시보드 요청에서 세션 메시지가 삭제되고 현재 request에만 전달됩니다.
- 같은 세션으로 다른 화면에 이동하더라도 이전 성공 메시지를 다시 읽을 세션 속성이 남지 않습니다.

## 배운 점

Flash Message는 세션에 저장하는 것만으로 완성되지 않습니다. 메시지를 생성한 요청의 다음 이동 경로를 기준으로 어느 컨트롤러가 메시지를 소비하고 언제 세션에서 삭제할지 함께 정해야 이전 작업의 메시지가 다른 화면에 노출되는 문제를 방지할 수 있습니다.
