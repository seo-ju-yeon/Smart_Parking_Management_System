# TS-005: 웹 애플리케이션 종료 시 DB 자원이 남은 문제

## 발견 경위

권한 판단 공통화 작업의 최종 검증 과정에서 최신 WAR를 Tomcat에 배포하여 기능을 확인한 뒤, 서버가 정상적으로 종료되는지 로그까지 확인했습니다. 이때 권한 기능과 직접 관련된 오류는 없었지만, Tomcat 종료 로그에서 MariaDB JDBC 드라이버와 `HikariPool-1 housekeeper` 스레드를 애플리케이션이 정리하지 못했다는 경고를 발견했습니다.

이후 IntelliJ에서 실행한 Tomcat에서도 로그인 등 DB를 사용하는 기능을 실행한 뒤 서버를 종료하여 같은 경고가 발생하는 것을 다시 확인했습니다. 반면 DB를 사용하지 않고 종료한 실행에서는 경고가 나타나지 않았습니다. 이를 통해 임시 Tomcat에만 생기는 문제가 아니라, HikariCP가 실제로 생성된 실행에서 드러나는 애플리케이션 자원 생명주기 문제로 범위를 좁혔습니다.

## 증상

DB를 사용한 뒤 Tomcat을 종료하면 다음 두 경고가 발생했습니다.

```text
웹 애플리케이션 [ROOT]이 JDBC 드라이버 [org.mariadb.jdbc.Driver]을 등록했지만,
웹 애플리케이션이 중지될 때 해당 JDBC 드라이버의 등록을 제거하지 못했습니다.

웹 애플리케이션 [ROOT]이 [HikariPool-1 housekeeper]라는 이름의 쓰레드를
시작시킨 것으로 보이지만, 해당 쓰레드를 중지시키지 못했습니다.
```

Tomcat은 메모리 누수를 방지하기 위해 JDBC 드라이버를 강제로 등록 해제했지만, 이는 애플리케이션이 자신이 생성한 자원을 직접 정리한 결과가 아니었습니다.

## 재현 절차

1. IntelliJ에서 Tomcat에 WAR exploded 아티팩트를 배포합니다.
2. 로그인 등 DB 조회가 발생하는 기능을 실행하여 HikariCP를 생성합니다.
3. `HikariPool-1 - Start completed.` 로그 또는 정상적인 DB 기능 동작을 확인합니다.
4. Tomcat을 정상 종료합니다.
5. 종료 로그에서 JDBC 드라이버와 `housekeeper` 스레드 경고를 확인합니다.

DB 기능을 사용하지 않으면 `DBConnection.INSTANCE`가 초기화되지 않아 HikariCP와 관리 스레드도 만들어지지 않습니다. 따라서 이 문제는 반드시 DB를 사용한 뒤 종료해야 재현할 수 있었습니다.

## 관찰한 종료 흐름

기존 `DBConnection`은 enum 싱글턴의 생성자에서 `HikariDataSource`를 생성하고 DAO에 커넥션을 제공했습니다. 그러나 애플리케이션 종료 시 `HikariDataSource.close()`를 호출하거나 JDBC 드라이버 등록을 해제하는 코드가 없었습니다.

```text
애플리케이션에서 최초 DB 사용
→ DBConnection.INSTANCE 초기화
→ HikariDataSource 생성
→ HikariCP 커넥션과 housekeeper 스레드 관리
→ Tomcat 종료 또는 WAR 재배포
→ HikariDataSource.close() 호출 없음
→ MariaDB JDBC 드라이버 등록 해제 없음
→ Tomcat이 남은 자원을 감지하여 경고 출력
```

DAO의 `try-with-resources`가 호출하는 `Connection.close()`는 사용한 커넥션을 HikariCP에 반환합니다. 이는 커넥션 풀 전체와 관리 스레드를 종료하는 `HikariDataSource.close()`와 역할이 다릅니다.

## 원인

HikariCP와 JDBC 드라이버를 웹 애플리케이션이 생성했지만, 해당 자원의 종료 시점을 Tomcat의 웹 애플리케이션 생명주기와 연결하지 않은 것이 원인이었습니다.

- `DBConnection`에는 커넥션 풀을 생성하고 커넥션을 빌려주는 코드만 있었습니다.
- `HikariDataSource.close()`를 호출하는 코드가 없었습니다.
- `ServletContextListener` 또는 그와 같은 종료 처리 구성 요소가 없었습니다.
- MariaDB JDBC 드라이버는 JVM의 `DriverManager`에 등록되었지만 애플리케이션 종료 시 등록을 해제하지 않았습니다.

이 구조는 HikariCP를 처음 도입한 시점부터 존재했으며, 권한 판단 공통화 작업 때문에 새로 발생한 문제는 아니었습니다. 권한 기능을 검증한 뒤 Tomcat 종료 로그까지 확인하면서 기존 문제가 발견된 것입니다.

## 수정하기로 한 이유

Tomcat 프로세스를 완전히 종료하면 운영체제와 JVM이 자원을 회수하므로 즉시 기능 장애나 DB 데이터 손상이 발생한 것은 아닙니다. 또한 Tomcat이 JDBC 드라이버를 강제로 해제하고 있었습니다.

그러나 IntelliJ의 WAR 재배포처럼 Tomcat JVM은 유지한 채 웹 애플리케이션만 교체하는 환경에서는 이전 애플리케이션이 만든 스레드와 JDBC 드라이버가 기존 웹 애플리케이션 클래스 로더를 참조할 수 있습니다. 재배포를 반복하면 이전 커넥션 풀, 클래스 로더 또는 관련 메모리가 정상적으로 회수되지 않을 가능성이 있습니다.

종료 로그에 Tomcat이 메모리 누수 가능성을 명시했고, 수정 범위도 커넥션 풀 종료 메서드와 생명주기 Listener로 제한할 수 있어 해당 자원을 애플리케이션이 직접 정리하도록 수정하기로 했습니다.

## 검토한 대안

### 1. Tomcat의 강제 정리에 의존

별도 코드를 추가하지 않아도 Tomcat이 JDBC 드라이버를 강제로 해제합니다. 하지만 `housekeeper` 스레드를 애플리케이션이 종료하지 않은 상태가 남고, 자원 소유자가 직접 정리하지 않는 구조도 유지되므로 선택하지 않았습니다.

### 2. `HikariDataSource.close()`만 호출

커넥션 풀과 `housekeeper` 스레드는 종료할 수 있습니다. 실제로 이 단계만 적용했을 때 HikariCP 종료 완료 로그와 `housekeeper` 경고 제거를 확인했지만, JDBC 드라이버 등록 해제 경고는 남았습니다.

### 3. 웹 애플리케이션 생명주기에서 풀과 드라이버를 순서대로 정리

`ServletContextListener`를 `web.xml`에 등록하면 Tomcat이 애플리케이션 시작과 종료 시점을 전달합니다. 종료 시 커넥션 풀을 먼저 닫고, 현재 웹 애플리케이션의 클래스 로더가 등록한 JDBC 드라이버만 해제할 수 있으므로 이 방법을 적용했습니다.

## 해결

### 1. 커넥션 풀 종료 메서드 추가

`DBConnection.close()`를 추가하고 이미 종료된 풀에는 `close()`를 다시 호출하지 않도록 확인했습니다.

```text
DBConnection.INSTANCE.close()
→ HikariDataSource.close()
→ 풀에 보관된 커넥션 종료
→ HikariCP 관리 스레드 종료
```

### 2. 웹 애플리케이션 생명주기 Listener 등록

`AppLifecycleListener`가 `ServletContextListener`를 구현하도록 만들고 `web.xml`에 등록했습니다.

- `contextInitialized()`: 애플리케이션 시작 시 `DBConnection.INSTANCE`를 생성하고 참조를 보관합니다.
- `contextDestroyed()`: 애플리케이션 종료 시 보관한 커넥션 풀을 닫습니다.

### 3. 현재 웹 애플리케이션의 JDBC 드라이버만 해제

커넥션 풀을 종료한 뒤 `DriverManager.getDrivers()`로 등록된 드라이버를 조회합니다. 드라이버 클래스의 클래스 로더가 `AppLifecycleListener`의 클래스 로더와 같은 경우에만 `DriverManager.deregisterDriver()`를 호출합니다.

이 조건을 둔 이유는 하나의 Tomcat에서 여러 웹 애플리케이션이 실행될 때 다른 애플리케이션이나 Tomcat이 관리하는 드라이버까지 해제하지 않기 위해서입니다.

### 변경 후 종료 흐름

```text
Tomcat이 웹 애플리케이션 종료 시작
→ AppLifecycleListener.contextDestroyed() 호출
→ HikariDataSource.close()
→ HikariCP 커넥션과 housekeeper 스레드 종료
→ 현재 웹 애플리케이션이 등록한 JDBC 드라이버 해제
→ Tomcat 종료
```

## 검증

### 코드 확인

- `DBConnection.close()`가 `HikariDataSource.isClosed()`를 확인한 뒤 풀을 종료합니다.
- `AppLifecycleListener.contextDestroyed()`가 JDBC 드라이버보다 커넥션 풀을 먼저 종료합니다.
- JDBC 드라이버는 현재 웹 애플리케이션의 클래스 로더가 등록한 경우에만 해제합니다.
- `web.xml`에 `AppLifecycleListener`가 등록되어 있습니다.

관련 구현:

- [`DBConnection`](../../src/main/java/org/example/smart_parking_260219/connection/DBConnection.java)
- [`AppLifecycleListener`](../../src/main/java/org/example/smart_parking_260219/listener/AppLifecycleListener.java)
- [`web.xml`](../../src/main/webapp/WEB-INF/web.xml)

### 수동 검증

동일하게 DB를 사용한 뒤 Tomcat을 정상 종료하는 방식으로 수정 전, 중간 수정 후, 최종 수정 후 로그를 각각 확인했습니다.

| 검증 단계 | HikariCP 종료 로그 | `housekeeper` 경고 | JDBC 드라이버 경고 |
| --- | --- | --- | --- |
| 수정 전 | 없음 | 발생 | 발생 |
| `HikariDataSource.close()` 적용 후 | `Shutdown completed` 확인 | 발생하지 않음 | 발생 |
| JDBC 드라이버 해제 적용 후 | `Shutdown completed` 확인 | 발생하지 않음 | 발생하지 않음 |

최종 종료 로그에서는 다음 순서를 확인했습니다.

```text
HikariPool-1 - Shutdown initiated...
HikariPool-1 - Shutdown completed.
프로토콜 핸들러 [http-nio-8080] 중지
프로토콜 핸들러 [http-nio-8080] 소멸
```

## 결과

- 웹 애플리케이션 종료 시 HikariCP가 `Shutdown completed` 상태로 종료됩니다.
- `HikariPool-1 housekeeper` 스레드가 남았다는 Tomcat 경고가 사라졌습니다.
- MariaDB JDBC 드라이버를 Tomcat이 강제로 해제했다는 경고가 사라졌습니다.
- 개별 DAO의 `Connection.close()`와 애플리케이션 전체의 `HikariDataSource.close()` 책임을 구분했습니다.

## 남은 한계

- 종료 동작은 IntelliJ의 Tomcat에서 한 차례씩 수동 검증했으며 자동 회귀 테스트는 없습니다.

## 배운 점

요청마다 사용하는 `Connection`을 닫는 것과 애플리케이션이 소유한 커넥션 풀을 종료하는 것은 서로 다른 책임입니다. Servlet과 Filter가 개별 HTTP 요청을 처리한다면, `ServletContextListener`는 웹 애플리케이션 전체의 시작과 종료에 맞춰 공유 자원을 준비하고 정리할 수 있습니다.

기능 검증이 끝났더라도 서버 종료 로그까지 확인해야 실행 중에는 드러나지 않는 자원 생명주기 문제를 발견할 수 있습니다. 또한 컨테이너가 자원을 강제로 정리해 준다는 사실을 정상 종료로 간주하지 않고, 애플리케이션이 생성한 자원을 직접 정리했는지 확인해야 합니다.
