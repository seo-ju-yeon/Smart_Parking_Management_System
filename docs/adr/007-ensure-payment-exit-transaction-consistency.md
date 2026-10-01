# ADR-007: 서버 계산과 단일 트랜잭션으로 결제·출차 정합성 개선

- 상태: 승인

## 문제 상황

### 1. 사용자가 보낸 금액을 그대로 저장했습니다

기존 결제 화면은 차량번호와 차량 유형뿐 아니라 할인 전 요금(`calculatedFee`), 할인 금액(`discountAmount`), 최종 결제 금액(`finalFee`)도 서버로 보냈습니다.

`PaymentController`는 요청에서 읽은 금액을 그대로 결제 내역에 저장했습니다. 따라서 사용자가 전송할 값을 바꾸면 화면에 표시된 금액과 다른 금액을 저장할 수 있는 구조였습니다.

### 2. 출차 처리 시 과거 주차 기록까지 변경될 수 있었습니다

주차 기록에는 뒷 4자리가 아닌 전체 차량번호가 저장됩니다. 같은 차량이 다시 방문하면 차량번호는 같지만 입차 시각과 `parkingId`가 다른 기록이 추가됩니다.

예를 들어 `99가9999` 차량이 어제 방문하고 오늘 다시 방문했다면, 같은 차량번호를 가진 주차 기록이 두 건 존재합니다.

기존 정산 대상 조회는 전체 차량번호와 미정산 조건을 사용했습니다. 하지만 출차 상태를 변경하는 UPDATE에는 다음 조건만 있었습니다.

```sql
WHERE car_num = ?
```

따라서 오늘 방문한 차량을 정산하면서 어제의 주차 기록까지 출차 시각과 주차시간 등이 함께 변경될 수 있었습니다. 이번에 정산할 기록 한 건만 변경하도록 `parkingId`로 대상을 지정할 필요가 있었습니다.

### 3. 결제·출차·주차 공간 반환이 따로 저장됐습니다

Controller는 다음 세 작업을 순서대로 호출했습니다.

```text
addPayment()               → 결제 내역 저장
modifyParking()            → 주차 기록을 출차 완료로 변경
modifyOutputParkingSpot()  → 주차 공간을 빈자리로 변경
```

각 DAO가 별도의 DB 연결(`Connection`)을 사용해 변경 내용을 저장했기 때문에, 뒤의 작업이 실패해도 앞에서 이미 저장한 내용을 함께 취소할 수 없었습니다.

예를 들어 결제 내역은 저장됐지만 주차 공간은 여전히 사용 중으로 남을 수 있었습니다. 한 번의 정산을 완료하려면 세 작업이 모두 성공해야 하므로, 중간에 실패하면 앞선 변경도 함께 취소하는 처리가 필요했습니다.

### 4. 같은 주차 기록에 결제를 여러 번 저장할 수 있었습니다

`payment.parking_id`에는 외래키만 설정되어 있었습니다. 외래키는 연결할 주차 기록이 존재하는지는 확인하지만, 그 기록에 결제 내역이 이미 있는지는 제한하지 않습니다.

따라서 같은 `parkingId`를 가진 결제 내역을 여러 건 저장할 수 있었습니다. 한 번의 주차 이용에 결제 내역을 한 건만 저장한다는 규칙을 DB에서도 제한할 필요가 있었습니다.

위 내용은 변경 전 코드와 테이블 구조에서 확인한 문제입니다. 과거 기록 변경이나 결제 일부만 저장되는 상황을 실제로 재현했다는 의미는 아닙니다.

## 변경 전 흐름

```text
Controller가 차량번호·차량 유형·금액을 요청에서 읽음
→ 차량번호로 주차 기록 조회
→ 결제 저장: DAO가 별도 Connection 사용
→ 주차 기록 출차 처리: DAO가 별도 Connection 사용
→ 주차 공간 반환: DAO가 별도 Connection 사용
→ 대시보드 이동
```

## 결정 기준

- 정산 대상은 한 번의 주차 이용 건으로 식별합니다.
- 결제에 저장할 금액은 서버가 조회한 입차 기록과 요금 정책으로 계산합니다.
- 결제 저장, 주차 기록 갱신, 공간 반환은 하나의 커밋·롤백 범위에 포함합니다.
- SQL 실행 자체가 성공했더라도 변경 행 수가 예상과 다르면 실패로 처리합니다.
- 같은 주차 기록에는 결제 내역을 최대 한 건만 허용합니다.
- 현재 Servlet·JDBC 구조에서 HTTP 처리와 업무 처리의 책임을 구분합니다.

## 결정

### 1. parkingId로 주차 이용 건 식별

차량번호는 차량을 구분하지만 이용 건을 구분하지 못합니다. 동일 차량이 출차 후 다시 입차하면 차량번호는 같고 `parkingId`는 새로 생성됩니다. 주차 공간도 여러 차량이 반복해서 사용하므로 정산 식별자로 적합하지 않습니다.

결제 요청은 `parkingId`로 대상을 지정하고, Service가 해당 기록의 존재 여부와 `paid` 상태를 다시 검사합니다. 차량번호, 입차 시각, 차량 유형과 공간 ID는 조회한 기록에서 가져옵니다. 요청 ID 자체가 신뢰할 수 있는 값이 되는 것은 아니므로 숫자 형식·범위 검사와 DB 상태 검사를 함께 수행합니다.

같은 `parkingId`를 주차 기록 잠금, 출차 갱신, `payment.parking_id` 저장에 사용하여 조회 대상과 변경 대상을 일치시킵니다.

### 2. PaymentService를 트랜잭션 경계로 선택

결제 저장, 출차 처리와 공간 반환은 각각의 HTTP 기능이 아니라 함께 성공해야 하는 하나의 업무입니다. 따라서 `PaymentService.completePaymentAndExit()`가 연결 획득부터 `commit()`과 `rollback()` 호출까지 담당하도록 결정했습니다.

Controller에서도 JDBC 트랜잭션을 구현할 수 있습니다. 다만 그렇게 하면 요청 파라미터 파싱·응답 처리와 세 DB 변경의 순서·실패 처리가 같은 클래스에 들어갑니다. 다른 Controller에서 같은 정산 기능을 호출할 때도 트랜잭션 구성을 다시 작성해야 할 수 있습니다. Service에 업무 단위를 두면 호출자는 `parkingId`와 `paymentType`을 전달하고 같은 처리 규칙을 사용할 수 있습니다.

| 계층 | 담당 역할 |
| --- | --- |
| `PaymentController` | 요청값 형식·범위 확인, Service 호출, HTTP 응답과 이동 처리 |
| `PaymentService` | DB 상태 검사, 금액 계산, 세 변경의 실행 순서와 커밋·롤백 관리 |
| DAO | 전달받은 연결로 SQL 실행, 조회 결과와 변경 행 수 반환 |

### 3. 동일한 Connection을 DAO에 전달

JDBC의 로컬 트랜잭션은 `Connection` 단위로 관리됩니다. Service의 연결 A에서 `rollback()`을 호출해도 DAO가 연결 B에서 이미 커밋한 변경을 취소할 수 없습니다. 따라서 Service가 한 번 획득한 연결에서 `setAutoCommit(false)`를 호출하고, 이 연결을 잠금 조회·정책 조회·세 상태 변경에 모두 전달합니다.

트랜잭션용 DAO는 `PreparedStatement`와 `ResultSet`만 닫습니다. 전달받은 연결에서 별도로 `commit()`하거나 연결을 닫지 않습니다. Service가 트랜잭션 종료를 관리하고, `finally`의 `finishTransaction()`에서 연결을 정리합니다. 정상 종료된 연결은 `close()`로 풀에 반환하고, 시작·롤백·설정 복원·반환에 실패한 연결은 재사용하지 않도록 풀에서 제거합니다.

기존의 개별 결제 저장·출차·공간 반환 메서드는 제거했습니다. 정산 화면 표시용 일반 조회와 입차 처리 메서드는 별도 용도로 유지합니다.

### 4. 서버에서 금액을 다시 계산

폼의 `readonly`나 hidden 필드는 사용자가 요청 내용을 변경하는 것을 막지 못합니다. 또한 정산 화면을 연 시점과 결제를 제출한 시점 사이에 시간이 지나면 요금 구간이 바뀔 수 있습니다. 화면에 표시된 예상 금액을 그대로 저장하지 않고 결제 처리 시점에 다시 계산합니다.

`PaymentController`는 정산 처리 입력으로 `parkingId`와 `paymentType`을 읽습니다. 요청의 금액·차량 유형은 계산에 사용하지 않습니다. `payment.jsp`의 금액 필드에서도 `name`을 제거했지만, 실제 방어는 서버가 해당 요청값을 읽지 않고 DB 값으로 계산하는 데 있습니다.

Service는 잠금 조회한 `ParkingVO`와 같은 연결로 조회한 활성 `FeePolicyVO`를 계산 메서드에 전달합니다. 출차 시각 `exitTime`을 한 번 생성하여 요금, 총 주차시간, `parking.exit_time` 저장에 공통으로 사용합니다. 계산 메서드 안에서 정책을 다시 조회하거나 DB 상태를 변경하지 않습니다.

Service에서 차량 유형과 결제 수단의 조합도 검사합니다. 월정액 차량은 월정액 결제만, 일반·경차·장애인 차량은 카드 또는 현금 결제만 허용합니다. 계산한 할인 전 요금, 할인 금액, 최종 금액과 적용한 정책 ID를 결제 내역에 저장합니다.

### 5. 잠금·상태 검사와 UNIQUE 제약을 함께 사용

자동 커밋을 끈 뒤 `parking_id = ? FOR UPDATE`로 주차 기록을 조회합니다. 같은 행을 잠금 조회하는 다른 정산 요청은 앞선 트랜잭션의 종료를 기다리게 됩니다. 잠금을 얻은 뒤 `paid`를 검사하고, 이미 정산된 기록이면 결제 저장을 진행하지 않습니다.

이 규칙은 해당 처리 경로를 거치는 요청에 적용됩니다. 다른 코드가 결제 내역을 직접 저장하더라도 중복을 거부하도록 `payment.parking_id`에 `uk_payment_parking_id` UNIQUE 제약을 추가합니다.

| 방어 | 담당하는 규칙 |
| --- | --- |
| `FOR UPDATE`와 `paid` 검사 | 같은 주차 기록의 정산 순서를 제어하고 이미 완료된 정산 요청을 거부합니다. |
| `payment.parking_id` UNIQUE | 하나의 주차 기록을 참조하는 결제 내역이 두 건 이상 저장되지 않도록 제한합니다. |
| 세 변경을 묶은 트랜잭션 | 결제 저장 뒤 출차나 공간 반환이 실패했을 때 앞선 변경도 취소합니다. |

기존 결제 중 `parking_id`가 중복된 그룹이 0건임을 확인한 뒤 `V4__add_unique_payment_parking.sql`을 추가했습니다. 이미 적용된 `V1`을 수정하지 않고 새 마이그레이션으로 제약 변경 이력을 남겼습니다.

중복 INSERT의 거부와 트랜잭션 전체 롤백은 구분합니다. UNIQUE 위반으로 DAO가 `SQLException`을 전달하면 Service가 명시적으로 `rollback()`을 호출합니다. UNIQUE 제약만으로 출차·공간 반환까지 하나의 트랜잭션이 되는 것은 아닙니다.

### 6. 변경 행 수 검사와 성공 안내

다음 세 결과가 각각 1건일 때만 커밋합니다.

| 작업 | 대상 조건 | 필요한 변경 행 수 |
| --- | --- | --- |
| 결제 저장 | 잠금 조회한 주차 기록의 `parkingId` | 1건 |
| 출차 갱신 | 해당 `parking_id`이고 `paid = false` | 1건 |
| 공간 반환 | 해당 `space_id`, 입차 차량번호, `empty = false` 모두 일치 | 1건 |

각 DAO 실행 직후 `requireSingleChangedRow()`로 결과를 검사합니다. `UPDATE`가 0건이어도 SQL 자체는 오류 없이 끝날 수 있으므로, 1건이 아니면 Service가 예외를 발생시켜 롤백 경로로 전달합니다.

서버 요청 전에 표시하던 정산 성공 `alert()`도 제거했습니다. Service가 커밋 후 정상 반환한 경우에만 Controller가 `successMessage`를 세션에 저장합니다. 대시보드는 이를 현재 요청으로 옮기고 세션에서 삭제하여 일회성 메시지로 표시합니다.

### 7. 롤백 실패 후에는 자동 커밋을 복원하지 않음

문서와 코드를 대조하면서, `rollback()`이 실패해도 `finally`에서 기존 자동 커밋 설정을 복원하는 경로를 확인했습니다. JDBC 명세상 진행 중인 트랜잭션에서 `setAutoCommit(true)`로 모드를 변경하면 해당 트랜잭션이 커밋됩니다. 따라서 미완료 변경이 남은 연결에 설정 복원을 시도하지 않도록 보완했습니다. 실제 DB 장애로 이 상황을 재현한 것은 아닙니다.

- `commit()` 또는 `rollback()`이 성공한 경우에만 `transactionCompleted`를 `true`로 설정합니다.
- 종료가 확인된 연결만 원래 자동 커밋 설정으로 복원합니다.
- 롤백이 실패하면 설정 복원을 건너뛰고 `DBConnection.evictConnection()`으로 해당 연결을 HikariCP에서 제거합니다. 트랜잭션 시작, 설정 복원 또는 연결 반환이 실패한 경우에도 제거합니다.
- 제거한 연결에는 `close()`를 다시 호출하지 않습니다. 정상 연결의 반환과 실패 연결의 제거를 구분하여 풀의 연결 정리를 중복 실행하지 않습니다.
- 롤백 예외는 최초 처리 예외의 `suppressed` 예외에 보관하여 원래 실패 원인과 함께 확인할 수 있게 합니다.

이는 불확실한 연결의 재사용과 자동 커밋 복원을 막는 처리입니다. DB와 통신이 끊긴 경우까지 최종 저장 결과를 알아낼 수 있다는 의미는 아닙니다. 특히 커밋 요청 후 응답을 받지 못했다면 DB에 반영됐는지는 별도 조회가 필요합니다.

근거: [Java 17 Connection 명세](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/Connection.html), [HikariCP 5.0.1의 연결 제거 구현](https://github.com/brettwooldridge/HikariCP/blob/HikariCP-5.0.1/src/main/java/com/zaxxer/hikari/HikariDataSource.java).

## 변경 후 흐름

```text
Controller가 parkingId·paymentType의 형식과 범위 확인
→ PaymentService가 Connection 획득 후 자동 커밋 비활성화
→ 주차 기록 FOR UPDATE 조회와 정산 상태 검사
→ DB 차량 유형·결제 수단 조합 검사
→ 같은 Connection으로 활성 요금 정책 조회
→ 출차 시각 1회 생성과 서버 금액 계산
→ 결제 저장 → 1건 확인
→ 출차 기록 갱신 → 1건 확인
→ 주차 공간 반환 → 1건 확인
→ 모두 성공: commit → 연결 정리 → 대시보드 성공 메시지
→ 처리 중 예외: rollback 시도 → 연결 정리 → 오류 전달

연결 정리
  commit 또는 rollback 성공: 설정 복원 후 close()로 반환
  rollback 실패: 설정을 복원하지 않고 연결을 풀에서 제거
  설정 복원 또는 반환 실패: 해당 연결을 풀에서 제거
```

## 검토한 대안

| 대안 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- |
| 기존 개별 저장 흐름 유지 | 수정 범위가 작습니다. | 다른 연결에서 반영한 변경을 한 번의 롤백으로 취소할 수 없습니다. | 제외 |
| Controller에서 트랜잭션 관리 | 현재 요청 경로에 바로 적용할 수 있습니다. | HTTP 처리와 업무 단위의 변경 순서·복구 책임이 함께 들어갑니다. | 제외 |
| Service에서 단일 Connection 관리 | 호출 경로와 관계없이 같은 정산 업무와 트랜잭션을 사용할 수 있습니다. | JDBC의 연결·커밋·롤백 처리를 직접 작성해야 합니다. | 채택 |
| 화면의 금액 사용 | 결제 제출 시 계산을 반복하지 않습니다. | 요청값을 변조할 수 있고 화면 조회 후 경과 시간도 반영하지 못합니다. | 제외 |
| 잠금 없는 paid 검사만 사용 | 조회와 조건문으로 구현할 수 있습니다. | 동시에 실행된 두 요청이 모두 미정산 상태를 읽을 수 있습니다. | 제외 |
| UNIQUE 제약만 추가 | DB에서 중복 결제 저장을 거부합니다. | 결제·출차·공간 반환의 부분 반영은 해결하지 못합니다. | 단독 사용 제외 |
| 단일 트랜잭션에 잠금·상태 검사와 UNIQUE 적용 | 업무 처리 순서와 DB 중복 저장 규칙을 함께 관리합니다. | 잠금 대기와 제약 변경을 함께 고려해야 합니다. | 채택 |

## 결과

### 기대 효과

- 요청의 금액·할인 유형 대신 DB 기록과 서버 시각으로 최종 금액을 계산합니다.
- 출차 갱신 대상을 차량번호가 아니라 잠금 조회한 주차 기록 한 건으로 지정합니다.
- 공간 반환이 0건인 경우도 실패로 인식하고, 앞서 수행한 결제·주차 기록 변경에 롤백을 호출합니다.
- 동일한 `parkingId`로 결제를 추가 저장하는 요청은 DB 제약에서도 거부합니다.
- 정산 성공 메시지는 서버 처리 결과가 반환된 후에만 생성합니다.

### 감수한 제약

- 같은 주차 기록을 처리하는 요청은 행 잠금이 해제될 때까지 대기할 수 있습니다. 잠금 대기시간이나 처리 성능은 이번 수동 검증에서 측정하지 않았습니다.
- UNIQUE 제약은 주차 이용 건당 결제 내역 최대 한 건이라는 현재 규칙을 반영합니다. 분할 결제나 복수 결제 이력을 도입하면 테이블 구조와 제약을 다시 검토해야 합니다.
- 화면 조회 후 시간이 지나거나 활성 정책이 바뀌면 결제 시 계산한 금액이 화면의 예상 금액과 달라질 수 있습니다. 현재 영수증 인쇄도 제출 전 예상값을 사용하므로, 저장된 최종 결제 내역을 기준으로 출력하는 기능은 별도 보완 대상입니다.
- 이 결정의 트랜잭션 범위는 결제·출차·공간 반환입니다. 입차의 기록 저장·공간 점유 처리나 기존 불일치 데이터의 복구까지 포함하지 않습니다.

## 검증

### 코드 확인

- `PaymentController`가 `completePaymentAndExit(parkingId, paymentType)`를 호출하고, DAO나 DB 연결을 직접 사용하지 않는 것을 확인했습니다.
- Service가 하나의 연결을 획득하여 자동 커밋을 끄고 잠금 조회·활성 정책 조회·세 상태 변경에 전달하는 것을 확인했습니다.
- 트랜잭션용 DAO가 전달받은 연결을 닫지 않고 SQL 자원만 정리하는 것을 확인했습니다.
- `exitTime`을 한 번 생성하여 요금 계산, 주차시간 계산, 출차 시각 저장에 사용하는 것을 확인했습니다.
- 세 상태 변경 직후 각각 변경 행 수를 검사하고, 예외 처리에서 `rollback()`을 호출하는 것을 확인했습니다.
- 커밋·롤백 성공 여부로 연결 설정 복원을 구분하고, 롤백 실패 시 해당 연결을 풀에서 제거하는 것을 확인했습니다.
- `src/main`에서 기존 개별 결제·출차·공간 반환 메서드와 호출이 제거된 것을 확인했습니다.
- 결제 POST가 금액과 `carType` 요청값을 읽지 않으며, 해당 JSP 금액 필드에 전송용 `name`이 없는 것을 확인했습니다.
- `payment.js`의 제출 전 성공 알림 두 곳이 제거되고 Controller의 Service 성공 분기에서만 메시지를 저장하는 것을 확인했습니다.

### 빌드 확인

- 기능 변경 후와 V4 적용 후 Java 17 Toolchain을 사용하는 `./gradlew clean war`가 성공했습니다.
- 연결 정리 보완과 임시 테스트 설정 제거 후에도 Gradle 실행 JDK를 17로 지정하여 `./gradlew clean war --offline` 빌드 성공을 확인했습니다.
- 이 명령은 애플리케이션 컴파일과 WAR 생성을 확인하며, 테스트 코드 컴파일·JUnit 실행이나 JSP 실행 결과까지 검증하지는 않습니다.
- 문서 작성 시 기존 `ParkingServiceTest.java`, `CarParkServiceTest.java`, `ParkingSpotDAOImplTest.java`에 제거된 개별 처리 메서드 호출이 남아 있음을 확인했습니다. 해당 테스트의 수정과 자동 검증은 완료 결과에 포함하지 않습니다.

### 수동 검증

아래 결과는 개발 중 로컬 MariaDB와 브라우저에서 수행한 검증을 기준으로 기록했습니다. 신규 결제는 당시 SQL 출력에서 확인한 값을 아래에 요약했습니다. 나머지는 수동 검증 메모이며 SQL 출력 원문을 이 문서에 첨부하지 않았습니다. 재확인용 조회 SQL과 실제로 확인한 결과를 구분했습니다.

| 시나리오 | 확인 결과 | 근거 |
| --- | --- | --- |
| 공간 차량번호를 주차 기록과 다른 값으로 바꾼 뒤 정산 | 공간 반환 실패 예외와 HTTP 500 발생, 결제 0건, `paid = 0`, `exit_time = NULL`, `total_time = 0` 확인 | 오류 화면과 롤백 후 DB 상태를 확인한 수동 검증 메모 |
| 공간 차량번호 복구 후 같은 차량 재결제 | 결제·출차·공간 반환 정상 처리 확인 | 복구 후 수동 검증 결과 |
| V4 적용과 제약·이력 조회 | `uk_payment_parking_id`의 `Non_unique = 0`, Flyway 버전 4 성공 확인 | Flyway 및 DB 조회 후 남긴 수동 검증 메모 |
| 기존 결제의 parkingId로 직접 중복 INSERT | UNIQUE 위반으로 저장 거부, 검증용 트랜잭션 종료 후 기존 결제 1건 유지 | 중복 저장 거부 수동 검증 결과 |
| V4 적용 후 신규 차량 최초 결제 | `parking_id = 2111`, `payment_id = 3021`, 결제 1건, `paid = 1`, 주차시간 1분, 공간 반환 확인 | 아래에 요약한 당시 SQL 조회 결과 |

강제 실패용 공간 차량번호는 결제 트랜잭션 시작 전에 별도로 반영한 값입니다. 따라서 결제 롤백이 이 값을 원래 차량번호로 되돌리는 것은 아닙니다. 롤백 후 DB 상태를 확인하고 공간 차량번호를 직접 복구한 뒤 재결제하여 정상 처리를 확인했습니다. HTTP 500만으로 롤백 성공을 판단하지 않았습니다.

#### 신규 결제의 SQL 출력에서 확인한 값

다음은 당시 출력의 요약이며, 이번 문서 보강 과정에서 DB 조회를 다시 실행한 결과는 아닙니다.

```text
parking_id / payment_id : 2111 / 3021
payment_count          : 1
paid                   : 1
exit_time              : 2026-10-01 11:47:44
total_time             : 1
calculated_fee         : 0
discount_amount        : 0
final_fee              : 0
spot_empty             : 1
spot_car_num           : NULL
```

신규 결제 `2111`의 SQL 출력에서 할인 전 요금·할인 금액·최종 금액은 모두 0원이었습니다. 주차시간 1분으로 무료 회차 구간에 해당하는 결과이며, 이 한 건으로 유료 구간이나 모든 할인 계산이 검증됐다고 판단하지 않습니다. ID는 검증 당시 로컬 데이터의 식별값입니다.

#### 재확인용 조회 SQL

`smart_parking_team2`에 접속한 뒤 실행합니다. `2111`은 위 검증 당시의 ID이므로 새 검증에서는 방금 입차한 기록의 ID로 바꿉니다. DB에 다시 접속하면 사용자 변수가 초기화되므로 `SET`부터 실행합니다.

```sql
SET @verification_parking_id = 2111;

SELECT
    p.parking_id, p.car_num, p.exit_time, p.total_time, p.paid,
    pay.payment_id, pay.calculated_fee, pay.discount_amount, pay.final_fee,
    ps.empty AS spot_empty, ps.car_num AS spot_car_num,
    (SELECT COUNT(*) FROM payment
     WHERE parking_id = p.parking_id) AS payment_count
FROM parking p
LEFT JOIN payment pay ON pay.parking_id = p.parking_id
JOIN parking_spot ps ON ps.space_id = p.space_id
WHERE p.parking_id = @verification_parking_id;
```

`LEFT JOIN`을 사용하여 결제가 0건인 롤백 결과도 조회합니다. 공간 상태는 과거 이력이 아닌 현재 값이므로 다른 차량이 같은 공간을 사용하기 전에 확인합니다.

| 확인 시점 | 확인할 값 |
| --- | --- |
| 정상 결제 직후 | `payment_count = 1`, `paid = 1`, 출차 시각 저장, `spot_empty = 1`, `spot_car_num = NULL` |
| 공간 불일치로 실패한 직후 | `payment_count = 0`, `paid = 0`, 출차 시각·주차시간이 정산 전 값과 같음, 공간은 실패 유도 직전 상태 유지 |

아래 조회는 중복 결제 데이터, UNIQUE 인덱스와 V4 적용 이력을 확인합니다. 인덱스 행이 존재하고 `Non_unique = 0`, `Column_name = parking_id`인지 확인합니다. Flyway는 버전 `4` 행의 `success = 1`을 확인합니다.

```sql
SELECT parking_id, COUNT(*) AS payment_count
FROM payment
GROUP BY parking_id
HAVING COUNT(*) > 1;

SHOW INDEX FROM payment WHERE Key_name = 'uk_payment_parking_id';

SELECT version, description, success
FROM flyway_schema_history
WHERE version = '4';
```

첫 조회의 0건은 현재 중복 데이터가 없다는 뜻입니다. 이것만으로 UNIQUE 제약이 적용됐거나 중복 INSERT가 실제로 거부됐다고 판단하지 않습니다.

직접 실행한 중복 INSERT는 DB 제약을 검증한 것입니다. 이는 애플리케이션에 동시 결제 요청을 보내는 시험이나 UNIQUE 위반을 Service에서 발생시킨 통합 시험을 대신하지 않습니다. 동시 요청, 잠금 시간 초과, DB 통신 단절과 롤백 자체 실패 상황은 이번 수동 검증 범위에 포함하지 않았습니다.

### 남은 검증

- 연결 정리 보완 과정에서 사용한 임시 테스트와 별도 Gradle 실행 설정은 이번 브랜치에 포함하지 않습니다. 연결 정리와 결제 흐름의 자동 회귀검증은 웹 보안 작업 이후 테스트 전용 브랜치에서 기존 테스트와 함께 정비할 예정입니다.
- 위 수동 검증은 연결 정리 보완 전 기록입니다. 이번 보완 후 브라우저에서 정상 결제와 강제 실패 롤백을 다시 실행한 결과는 포함하지 않았습니다.

## 관련 구현

- [PaymentService.java](../../src/main/java/org/example/smart_parking_260219/service/PaymentService.java)
- [DBConnection.java](../../src/main/java/org/example/smart_parking_260219/connection/DBConnection.java)
- [PaymentController.java](../../src/main/java/org/example/smart_parking_260219/controller/payment/PaymentController.java)
- [PaymentDAO.java](../../src/main/java/org/example/smart_parking_260219/dao/PaymentDAO.java)
- [ParkingDAOImpl.java](../../src/main/java/org/example/smart_parking_260219/dao/ParkingDAOImpl.java)
- [ParkingSpotDAOImpl.java](../../src/main/java/org/example/smart_parking_260219/dao/ParkingSpotDAOImpl.java)
- [FeePolicyDAO.java](../../src/main/java/org/example/smart_parking_260219/dao/FeePolicyDAO.java)
- [ParkingListController.java](../../src/main/java/org/example/smart_parking_260219/controller/parking/ParkingListController.java)
- [payment.jsp](../../src/main/webapp/WEB-INF/views/payment/payment.jsp)
- [payment.js](../../src/main/webapp/js/payment/payment.js)
- [DashboardController.java](../../src/main/java/org/example/smart_parking_260219/controller/login/DashboardController.java)
- [V4__add_unique_payment_parking.sql](../../src/main/resources/db/migration/V4__add_unique_payment_parking.sql)
