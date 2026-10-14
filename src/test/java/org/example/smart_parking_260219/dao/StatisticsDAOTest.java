package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.dto.StatisticsDTO;
import org.example.smart_parking_260219.util.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB에 주차 기록과 결제 내역을 만들고 통계를 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class StatisticsDAOTest {

    private final StatisticsDAO statisticsDAO = StatisticsDAO.getInstance();
    private final List<Integer> testParkingIds = new ArrayList<>();
    private final List<String> testSpaceIds = new ArrayList<>();
    private Integer testPolicyId;
    private LocalDate testDate;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB에 데이터를 넣지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        // 다른 테스트 데이터와 날짜가 겹치지 않도록 미래 날짜를 선택함
        LocalDate newDate = LocalDate.of(
                ThreadLocalRandom.current().nextInt(2100, 2200),
                ThreadLocalRandom.current().nextInt(1, 13),
                10);
        List<Integer> newParkingIds = new ArrayList<>();
        List<String> newSpaceIds = new ArrayList<>();

        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int newPolicyId = insertTestPolicy(connection);

                // 첫날 9시·10시와 다음 날 9시에 각각 다른 결제 내역을 준비함
                for (int index = 0; index < 3; index++) {
                    String spaceId;
                    do {
                        spaceId = "T" + UUID.randomUUID().toString().substring(0, 4);
                    } while (newSpaceIds.contains(spaceId));

                    String carNum = "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                    int carType = index == 1 ? 3 : 1;
                    LocalDate parkingDate = index == 2 ? newDate.plusDays(1) : newDate;
                    int hour = index == 1 ? 10 : 9;
                    int finalFee = index == 0 ? 2500 : index == 1 ? 1500 : 1000;
                    LocalDateTime eventTime = parkingDate.atTime(hour, 15);

                    int parkingId = insertTestParking(connection, spaceId, carNum, carType, eventTime);
                    insertTestPayment(connection, parkingId, newPolicyId, finalFee, eventTime);
                    newSpaceIds.add(spaceId);
                    newParkingIds.add(parkingId);
                }

                connection.commit();
                testPolicyId = newPolicyId;
                testDate = newDate;
                testParkingIds.addAll(newParkingIds);
                testSpaceIds.addAll(newSpaceIds);
                log.info("--- 통계 테스트 데이터 준비: 날짜={}, 주차·결제 {}건 ---", testDate, testParkingIds.size());
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testPolicyId == null) {
            return;
        }

        // 결제를 먼저 삭제한 뒤 주차 기록과 주차면, 정책을 정리함
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement payment = connection.prepareStatement(
                    "DELETE FROM payment WHERE parking_id = ?");
                 PreparedStatement parking = connection.prepareStatement(
                         "DELETE FROM parking WHERE parking_id = ?");
                 PreparedStatement spot = connection.prepareStatement(
                         "DELETE FROM parking_spot WHERE space_id = ?");
                 PreparedStatement policy = connection.prepareStatement(
                         "DELETE FROM fee_policy WHERE policy_id = ?")) {
                for (int parkingId : testParkingIds) {
                    payment.setInt(1, parkingId);
                    payment.executeUpdate();
                    parking.setInt(1, parkingId);
                    parking.executeUpdate();
                }
                for (String spaceId : testSpaceIds) {
                    spot.setString(1, spaceId);
                    spot.executeUpdate();
                }
                policy.setInt(1, testPolicyId);
                policy.executeUpdate();
                connection.commit();
                log.info("--- 통계 테스트 데이터 정리: 날짜={}, 주차·결제 {}건 ---",
                        testDate, testParkingIds.size());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @Test
    void selectHourlySalesByDayTest() {
        List<StatisticsDTO> result = statisticsDAO.selectHourlySalesByDay(testDate.toString());
        Map<String, Long> salesByHour = toMap(result);

        log.info("--- 시간대별 매출 조회 결과: 날짜={}, 매출={} ---", testDate, salesByHour);
        assertEquals(Map.of("9시", 2500L, "10시", 1500L), salesByHour);
    }

    @Test
    void selectHourlyCountByDayTest() {
        List<StatisticsDTO> result = statisticsDAO.selectHourlyCountByDay(testDate.toString());
        Map<String, Long> countByHour = toMap(result);

        log.info("--- 시간대별 입차 조회 결과: 날짜={}, 대수={} ---", testDate, countByHour);
        assertEquals(Map.of("9시", 1L, "10시", 1L), countByHour);
    }

    @Test
    void selectMonthlySalesByYearMonthTest() {
        List<StatisticsDTO> result = statisticsDAO.selectMonthlySalesByYearMonth(
                testDate.getYear(), testDate.getMonthValue());
        Map<String, Long> salesByDay = toMap(result);

        log.info("--- 월별 일간 매출 조회 결과: 기준 월={}, 매출={} ---", testDate.getMonth(), salesByDay);
        assertEquals(Map.of(testDate.toString(), 4000L,
                testDate.plusDays(1).toString(), 1000L), salesByDay);
    }

    @Test
    void selectCarTypeStatsByPeriodTest() {
        List<StatisticsDTO> result = statisticsDAO.selectCarTypeStatsByPeriod(
                testDate.getYear(), testDate.getMonthValue());
        Map<String, Long> countByCarType = toMap(result);

        log.info("--- 차종별 이용 통계 조회 결과: 기준 월={}, 대수={} ---", testDate.getMonth(), countByCarType);
        assertEquals(Map.of("일반", 2L, "경차", 1L), countByCarType);
    }

    // 날짜와 차종별 집계 결과를 기대값과 비교하기 쉬운 형태로 바꿈
    private Map<String, Long> toMap(List<StatisticsDTO> result) {
        return result.stream().collect(Collectors.toMap(StatisticsDTO::getLabel, StatisticsDTO::getValue));
    }

    private int insertTestPolicy(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO fee_policy (is_active) VALUES (FALSE)",
                Statement.RETURN_GENERATED_KEYS)) {
            assertEquals(1, statement.executeUpdate());
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getInt(1);
            }
        }
    }

    private int insertTestParking(Connection connection, String spaceId, String carNum,
                                  int carType, LocalDateTime entryTime) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO parking_spot (space_id) VALUES (?)")) {
            statement.setString(1, spaceId);
            assertEquals(1, statement.executeUpdate());
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO parking (space_id, car_num, car_type, entry_time) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, spaceId);
            statement.setString(2, carNum);
            statement.setInt(3, carType);
            statement.setTimestamp(4, Timestamp.valueOf(entryTime));
            assertEquals(1, statement.executeUpdate());
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getInt(1);
            }
        }
    }

    private void insertTestPayment(Connection connection, int parkingId, int policyId,
                                   int finalFee, LocalDateTime paymentDate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO payment (parking_id, policy_id, payment_type, "
                        + "calculated_fee, discount_amount, final_fee, payment_date) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setInt(1, parkingId);
            statement.setInt(2, policyId);
            statement.setInt(3, 1);
            statement.setInt(4, finalFee);
            statement.setInt(5, 0);
            statement.setInt(6, finalFee);
            statement.setTimestamp(7, Timestamp.valueOf(paymentDate));
            assertEquals(1, statement.executeUpdate());
        }
    }
}
