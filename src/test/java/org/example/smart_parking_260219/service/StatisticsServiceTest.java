package org.example.smart_parking_260219.service;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB의 주차·결제·회원 데이터를 사용해 통계 서비스 결과를 확인함
@Tag("integration")
@Log4j2
class StatisticsServiceTest {

    private final StatisticsService statisticsService = StatisticsService.INSTANCE;
    private final Set<Integer> originalActivePolicyIds = new HashSet<>();
    private final List<Integer> testParkingIds = new ArrayList<>();
    private final List<String> testSpaceIds = new ArrayList<>();
    private final List<Integer> testMemberIds = new ArrayList<>();
    private Integer testPolicyId;
    private LocalDate testDate;
    private int originalSubscribedCount;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB에 테스트 데이터를 넣지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        // 2월의 빈 날짜 채우기와 윤년 마지막 날을 함께 확인할 수 있는 날짜임
        LocalDate newDate = LocalDate.of(
                2104 + 4 * ThreadLocalRandom.current().nextInt(24), 2, 10);
        List<Integer> newParkingIds = new ArrayList<>();
        List<String> newSpaceIds = new ArrayList<>();
        List<Integer> newMemberIds = new ArrayList<>();

        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement active = connection.prepareStatement(
                        "SELECT policy_id FROM fee_policy WHERE is_active = true");
                     ResultSet resultSet = active.executeQuery()) {
                    while (resultSet.next()) {
                        originalActivePolicyIds.add(resultSet.getInt("policy_id"));
                    }
                }

                try (PreparedStatement count = connection.prepareStatement(
                        "SELECT COUNT(*) FROM member WHERE subscribed = true");
                     ResultSet resultSet = count.executeQuery()) {
                    assertTrue(resultSet.next());
                    originalSubscribedCount = resultSet.getInt(1);
                }

                try (PreparedStatement deactivate = connection.prepareStatement(
                        "UPDATE fee_policy SET is_active = false WHERE is_active = true")) {
                    deactivate.executeUpdate();
                }

                int newPolicyId = insertTestPolicy(connection);
                insertParkingAndPayment(connection, newPolicyId, newDate.atTime(9, 15),
                        1, 2500, newParkingIds, newSpaceIds);
                insertParkingAndPayment(connection, newPolicyId, newDate.plusDays(1).atTime(10, 15),
                        3, 1500, newParkingIds, newSpaceIds);
                newMemberIds.add(insertTestMember(connection, true));
                newMemberIds.add(insertTestMember(connection, false));

                connection.commit();
                testPolicyId = newPolicyId;
                testDate = newDate;
                testParkingIds.addAll(newParkingIds);
                testSpaceIds.addAll(newSpaceIds);
                testMemberIds.addAll(newMemberIds);
                log.info("--- 통계 서비스 테스트 준비: 날짜={}, 주차 {}건, 회원 {}명 ---",
                        testDate, testParkingIds.size(), testMemberIds.size());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testPolicyId == null) {
            return;
        }

        // 결제와 주차 데이터를 먼저 삭제하고 원래 활성 정책을 복원함
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement payment = connection.prepareStatement(
                    "DELETE FROM payment WHERE parking_id = ?");
                 PreparedStatement parking = connection.prepareStatement(
                         "DELETE FROM parking WHERE parking_id = ?");
                 PreparedStatement spot = connection.prepareStatement(
                         "DELETE FROM parking_spot WHERE space_id = ?");
                 PreparedStatement member = connection.prepareStatement(
                         "DELETE FROM member WHERE member_id = ?");
                 PreparedStatement policy = connection.prepareStatement(
                         "DELETE FROM fee_policy WHERE policy_id = ?");
                 PreparedStatement restore = connection.prepareStatement(
                         "UPDATE fee_policy SET is_active = true WHERE policy_id = ?")) {
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
                for (int memberId : testMemberIds) {
                    member.setInt(1, memberId);
                    member.executeUpdate();
                }
                policy.setInt(1, testPolicyId);
                policy.executeUpdate();
                for (int policyId : originalActivePolicyIds) {
                    restore.setInt(1, policyId);
                    restore.executeUpdate();
                }

                connection.commit();
                log.info("--- 통계 서비스 테스트 정리: 날짜={}, 활성 정책 복원={} ---",
                        testDate, originalActivePolicyIds);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    @Test
    void getHourlySalesTest() {
        List<StatisticsDTO> sales = statisticsService.getHourlySales(testDate.toString());

        log.info("--- 시간별 매출 결과: 날짜={}, 9시={}, 8시={} ---",
                testDate, sales.get(9).getValue(), sales.get(8).getValue());
        assertEquals(24, sales.size());
        assertEquals("0시", sales.get(0).getLabel());
        assertEquals("23시", sales.get(23).getLabel());
        assertEquals(2500L, sales.get(9).getValue());
        assertEquals(0L, sales.get(8).getValue());
        assertEquals(0L, sales.get(23).getValue());
    }

    @Test
    void getHourlyCountTest() {
        List<StatisticsDTO> counts = statisticsService.getHourlyCount(testDate.toString());

        log.info("--- 시간별 입차 결과: 날짜={}, 9시={}, 10시={} ---",
                testDate, counts.get(9).getValue(), counts.get(10).getValue());
        assertEquals(24, counts.size());
        assertEquals("9시", counts.get(9).getLabel());
        assertEquals(1L, counts.get(9).getValue());
        assertEquals(0L, counts.get(10).getValue());
    }

    @Test
    void getDailySalesTest() {
        List<StatisticsDTO> sales = statisticsService.getDailySales(
                testDate.getYear(), testDate.getMonthValue());

        log.info("--- 윤년 2월 일별 매출: 날짜={}, 10일={}, 11일={} ---",
                testDate, sales.get(9).getValue(), sales.get(10).getValue());
        assertEquals(29, sales.size());
        assertEquals(testDate.withDayOfMonth(1).toString(), sales.get(0).getLabel());
        assertEquals(testDate.withDayOfMonth(29).toString(), sales.get(28).getLabel());
        assertEquals(0L, sales.get(0).getValue());
        assertEquals(2500L, sales.get(9).getValue());
        assertEquals(1500L, sales.get(10).getValue());
    }

    @Test
    void getCarTypeStatsTest() {
        List<StatisticsDTO> stats = statisticsService.getCarTypeStats(
                testDate.getYear(), testDate.getMonthValue());
        Map<String, Long> counts = stats.stream()
                .collect(Collectors.toMap(StatisticsDTO::getLabel, StatisticsDTO::getValue));

        log.info("--- 차종별 이용 결과: 날짜={}, 대수={} ---", testDate, counts);
        assertEquals(Map.of("일반", 1L, "경차", 1L), counts);
    }

    @Test
    void getMonthlySalesTest() throws SQLException {
        int monthlySales = statisticsService.getMonthlySales();

        log.info("--- 월정액 매출 결과: 기존 회원={}, 테스트 구독 회원=1, 매출={} ---",
                originalSubscribedCount, monthlySales);
        assertEquals((originalSubscribedCount + 1) * 75000, monthlySales);
    }

    private int insertTestPolicy(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO fee_policy (subscribed_fee, is_active) VALUES (75000, true)",
                Statement.RETURN_GENERATED_KEYS)) {
            assertEquals(1, statement.executeUpdate());
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getInt(1);
            }
        }
    }

    private void insertParkingAndPayment(Connection connection, int policyId,
                                         LocalDateTime eventTime, int carType, int finalFee,
                                         List<Integer> parkingIds, List<String> spaceIds)
            throws SQLException {
        String spaceId = findUnusedSpaceId(connection, spaceIds);
        String carNum = "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        try (PreparedStatement spot = connection.prepareStatement(
                "INSERT INTO parking_spot (space_id) VALUES (?)")) {
            spot.setString(1, spaceId);
            assertEquals(1, spot.executeUpdate());
        }

        int parkingId;
        try (PreparedStatement parking = connection.prepareStatement(
                "INSERT INTO parking (space_id, car_num, car_type, entry_time) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            parking.setString(1, spaceId);
            parking.setString(2, carNum);
            parking.setInt(3, carType);
            parking.setTimestamp(4, Timestamp.valueOf(eventTime));
            assertEquals(1, parking.executeUpdate());
            try (ResultSet keys = parking.getGeneratedKeys()) {
                assertTrue(keys.next());
                parkingId = keys.getInt(1);
            }
        }

        try (PreparedStatement payment = connection.prepareStatement(
                "INSERT INTO payment (parking_id, policy_id, payment_type, final_fee, payment_date) "
                        + "VALUES (?, ?, 1, ?, ?)")) {
            payment.setInt(1, parkingId);
            payment.setInt(2, policyId);
            payment.setInt(3, finalFee);
            payment.setTimestamp(4, Timestamp.valueOf(eventTime));
            assertEquals(1, payment.executeUpdate());
        }

        parkingIds.add(parkingId);
        spaceIds.add(spaceId);
    }

    private String findUnusedSpaceId(Connection connection, List<String> newSpaceIds)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM parking_spot WHERE space_id = ?")) {
            for (int attempt = 0; attempt < 20; attempt++) {
                String candidate = "T" + UUID.randomUUID().toString().substring(0, 4);
                if (newSpaceIds.contains(candidate)) {
                    continue;
                }
                statement.setString(1, candidate);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return candidate;
                    }
                }
            }
        }
        throw new IllegalStateException("테스트에 사용할 빈 주차 공간 번호를 찾지 못했습니다.");
    }

    private int insertTestMember(Connection connection, boolean subscribed) throws SQLException {
        String carNum = "M" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        LocalDate startDate = LocalDate.now();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO member (car_num, car_type, name, phone, start_date, end_date, subscribed) "
                        + "VALUES (?, 2, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, carNum);
            statement.setString(2, "통계테스트");
            statement.setString(3, "010-1111-2222");
            statement.setObject(4, startDate);
            statement.setObject(5, startDate.plusMonths(1));
            statement.setBoolean(6, subscribed);
            assertEquals(1, statement.executeUpdate());
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getInt(1);
            }
        }
    }
}
