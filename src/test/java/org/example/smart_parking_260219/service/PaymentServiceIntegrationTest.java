package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.dto.PaymentDTO;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB에서 결제 저장, 주차 기록 갱신, 주차 공간 반환을 함께 확인함
@Tag("integration")
@Log4j2
class PaymentServiceIntegrationTest {

    private final PaymentService paymentService = PaymentService.INSTANCE;
    private final Set<Integer> originalActivePolicyIds = new HashSet<>();
    private Integer testParkingId;
    private Integer testPolicyId;
    private String testSpaceId;
    private String testCarNum;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB의 결제와 주차 기록을 변경하지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        String newSpaceId = findUnusedSpaceId();
        String newCarNum = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 테스트 정책만 활성화하고 결제 대상인 미정산 차량을 준비함
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

                try (PreparedStatement deactivate = connection.prepareStatement(
                        "UPDATE fee_policy SET is_active = false WHERE is_active = true")) {
                    deactivate.executeUpdate();
                }

                int newPolicyId;
                try (PreparedStatement policy = connection.prepareStatement(
                        "INSERT INTO fee_policy (grace_period, default_time, default_fee, "
                                + "extra_time, extra_fee, light_discount, disabled_discount, "
                                + "max_daily_fee, is_active) VALUES (10, 60, 2000, 30, 1000, 0.3, 0.5, 15000, true)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    assertEquals(1, policy.executeUpdate());
                    try (ResultSet keys = policy.getGeneratedKeys()) {
                        assertTrue(keys.next());
                        newPolicyId = keys.getInt(1);
                    }
                }

                try (PreparedStatement spot = connection.prepareStatement(
                        "INSERT INTO parking_spot (space_id, empty, car_num) VALUES (?, false, ?)")) {
                    spot.setString(1, newSpaceId);
                    spot.setString(2, newCarNum);
                    assertEquals(1, spot.executeUpdate());
                }

                int newParkingId;
                try (PreparedStatement parking = connection.prepareStatement(
                        "INSERT INTO parking (space_id, car_num, car_type, entry_time, paid) "
                                + "VALUES (?, ?, 3, ?, false)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    parking.setString(1, newSpaceId);
                    parking.setString(2, newCarNum);
                    parking.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().minusMinutes(75)));
                    assertEquals(1, parking.executeUpdate());
                    try (ResultSet keys = parking.getGeneratedKeys()) {
                        assertTrue(keys.next());
                        newParkingId = keys.getInt(1);
                    }
                }

                connection.commit();
                testSpaceId = newSpaceId;
                testCarNum = newCarNum;
                testPolicyId = newPolicyId;
                testParkingId = newParkingId;
                log.info("--- 결제 서비스 테스트 준비: parkingId={}, policyId={}, spaceId={} ---",
                        testParkingId, testPolicyId, testSpaceId);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testParkingId == null) {
            return;
        }

        // 외래 키 순서대로 테스트 데이터를 삭제하고 원래 활성 정책을 복원함
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement payment = connection.prepareStatement(
                    "DELETE FROM payment WHERE parking_id = ?");
                 PreparedStatement parking = connection.prepareStatement(
                         "DELETE FROM parking WHERE parking_id = ?");
                 PreparedStatement spot = connection.prepareStatement(
                         "DELETE FROM parking_spot WHERE space_id = ?");
                 PreparedStatement policy = connection.prepareStatement(
                         "DELETE FROM fee_policy WHERE policy_id = ?");
                 PreparedStatement restore = connection.prepareStatement(
                         "UPDATE fee_policy SET is_active = true WHERE policy_id = ?")) {
                payment.setInt(1, testParkingId);
                payment.executeUpdate();
                parking.setInt(1, testParkingId);
                parking.executeUpdate();
                spot.setString(1, testSpaceId);
                spot.executeUpdate();
                policy.setInt(1, testPolicyId);
                policy.executeUpdate();

                for (int policyId : originalActivePolicyIds) {
                    restore.setInt(1, policyId);
                    restore.executeUpdate();
                }

                connection.commit();
                log.info("--- 결제 서비스 테스트 정리: parkingId={}, 활성 정책 복원={} ---",
                        testParkingId, originalActivePolicyIds);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    @Test
    void paymentAndExitTest() throws SQLException {
        PaymentDTO payment = paymentService.completePaymentAndExit(testParkingId, 1);

        log.info("--- 결제 및 출차 결과: parkingId={}, 결제={} ---", testParkingId, payment);
        assertEquals(testParkingId.intValue(), payment.getParkingId());
        assertEquals(testPolicyId.intValue(), payment.getPolicyId());
        assertEquals(testCarNum, payment.getCarNum());
        assertEquals(3, payment.getCarType());
        assertEquals(1, payment.getPaymentType());
        assertEquals(3000, payment.getCalculatedFee());
        assertEquals(900, payment.getDiscountAmount());
        assertEquals(2100, payment.getFinalFee());
        assertTrue(payment.getTotalTime() >= 75);

        LocalDate paymentDate;
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            assertPaymentSaved(connection);
            assertParkingState(connection, true, payment.getTotalTime());
            assertSpotState(connection, true, null);
            paymentDate = readStoredPaymentDate(connection);
        }

        List<PaymentDTO> payments = paymentService.getPaymentList(paymentDate.toString());
        PaymentDTO found = payments.stream()
                .filter(item -> item.getParkingId() == testParkingId)
                .findFirst()
                .orElse(null);
        log.info("--- 결제 목록 조회 결과: parkingId={}, 결제={} ---", testParkingId, found);
        assertNotNull(found);
        assertEquals(2100, found.getFinalFee());
    }

    @Test
    void paymentRollbackTest() throws SQLException {
        // 주차 기록과 주차 공간의 차량번호를 다르게 만들어 마지막 갱신만 실패시킴
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE parking_spot SET car_num = ? WHERE space_id = ?")) {
            statement.setString(1, "OTHER-CAR");
            statement.setString(2, testSpaceId);
            assertEquals(1, statement.executeUpdate());
        }

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> paymentService.completePaymentAndExit(testParkingId, 1));
        log.info("--- 출차 실패 후 롤백 결과: parkingId={}, 오류={} ---",
                testParkingId, exception.getMessage());

        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            assertNoPayment(connection);
            assertParkingState(connection, false, 0);
            assertSpotState(connection, false, "OTHER-CAR");
        }
    }

    @Test
    void duplicatePaymentTest() throws SQLException {
        paymentService.completePaymentAndExit(testParkingId, 1);

        assertThrows(IllegalStateException.class,
                () -> paymentService.completePaymentAndExit(testParkingId, 1));

        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            assertPaymentSaved(connection);
            assertSpotState(connection, true, null);
        }
        log.info("--- 중복 결제 거부 결과: parkingId={}, 결제 1건 유지 ---", testParkingId);
    }

    private String findUnusedSpaceId() throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM parking_spot WHERE space_id = ?")) {
            for (int attempt = 0; attempt < 20; attempt++) {
                String candidate = "T" + UUID.randomUUID().toString().substring(0, 4);
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

    private void assertPaymentSaved(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT policy_id, payment_type, calculated_fee, discount_amount, final_fee "
                        + "FROM payment WHERE parking_id = ?")) {
            statement.setInt(1, testParkingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(testPolicyId.intValue(), resultSet.getInt("policy_id"));
                assertEquals(1, resultSet.getInt("payment_type"));
                assertEquals(3000, resultSet.getInt("calculated_fee"));
                assertEquals(900, resultSet.getInt("discount_amount"));
                assertEquals(2100, resultSet.getInt("final_fee"));
                assertFalse(resultSet.next());
            }
        }
    }

    private void assertNoPayment(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM payment WHERE parking_id = ?")) {
            statement.setInt(1, testParkingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(0, resultSet.getInt(1));
            }
        }
    }

    private LocalDate readStoredPaymentDate(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT payment_date FROM payment WHERE parking_id = ?")) {
            statement.setInt(1, testParkingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getTimestamp("payment_date").toLocalDateTime().toLocalDate();
            }
        }
    }

    private void assertParkingState(Connection connection, boolean expectedPaid, int expectedTotalTime)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT paid, exit_time, total_time FROM parking WHERE parking_id = ?")) {
            statement.setInt(1, testParkingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(expectedPaid, resultSet.getBoolean("paid"));
                assertEquals(expectedTotalTime, resultSet.getInt("total_time"));
                if (expectedPaid) {
                    assertNotNull(resultSet.getTimestamp("exit_time"));
                } else {
                    assertNull(resultSet.getTimestamp("exit_time"));
                }
            }
        }
    }

    private void assertSpotState(Connection connection, boolean expectedEmpty, String expectedCarNum)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT empty, car_num FROM parking_spot WHERE space_id = ?")) {
            statement.setString(1, testSpaceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(expectedEmpty, resultSet.getBoolean("empty"));
                assertEquals(expectedCarNum, resultSet.getString("car_num"));
            }
        }
    }
}
