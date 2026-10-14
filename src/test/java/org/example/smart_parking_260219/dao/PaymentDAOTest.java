package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.util.AppConfig;
import org.example.smart_parking_260219.vo.PaymentVO;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB에 결제 내역을 추가하고 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class PaymentDAOTest {

    private final PaymentDAO paymentDAO = PaymentDAO.getInstance();
    private Integer testParkingId;
    private Integer testPolicyId;
    private String testSpaceId;
    private String testCarNum;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB에 데이터를 넣지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        String newSpaceId = "T" + UUID.randomUUID().toString().substring(0, 4);
        String newCarNum = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 결제의 외래 키가 참조할 주차 기록과 요금 정책을 함께 준비함
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO parking_spot (space_id) VALUES (?)")) {
                    statement.setString(1, newSpaceId);
                    assertEquals(1, statement.executeUpdate());
                }

                int newParkingId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO parking (space_id, car_num, car_type, total_time) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, newSpaceId);
                    statement.setString(2, newCarNum);
                    statement.setInt(3, 3);
                    statement.setInt(4, 75);
                    assertEquals(1, statement.executeUpdate());
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        assertTrue(keys.next());
                        newParkingId = keys.getInt(1);
                    }
                }

                int newPolicyId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO fee_policy (is_active) VALUES (FALSE)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    assertEquals(1, statement.executeUpdate());
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        assertTrue(keys.next());
                        newPolicyId = keys.getInt(1);
                    }
                }

                connection.commit();
                testSpaceId = newSpaceId;
                testCarNum = newCarNum;
                testParkingId = newParkingId;
                testPolicyId = newPolicyId;
                log.info("--- 결제 테스트 데이터 준비: parkingId={}, policyId={}, carNum={} ---",
                        testParkingId, testPolicyId, testCarNum);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testParkingId == null) {
            return;
        }

        // 참조하는 결제 내역부터 삭제한 뒤 주차 기록과 주차면, 정책을 정리함
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
                payment.setInt(1, testParkingId);
                int deletedPayments = payment.executeUpdate();
                parking.setInt(1, testParkingId);
                parking.executeUpdate();
                spot.setString(1, testSpaceId);
                spot.executeUpdate();
                policy.setInt(1, testPolicyId);
                policy.executeUpdate();
                connection.commit();
                log.info("--- 결제 테스트 데이터 정리: parkingId={}, 결제 {}건 ---",
                        testParkingId, deletedPayments);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @Test
    void insertPaymentTest() throws SQLException {
        PaymentVO payment = PaymentVO.builder()
                .parkingId(testParkingId)
                .policyId(testPolicyId)
                .paymentType(1)
                .calculatedFee(3000)
                .discountAmount(500)
                .finalFee(2500)
                .build();

        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            int insertedCount = paymentDAO.insertPayment(connection, payment);

            // 저장 결과는 같은 DB 연결에서 직접 조회해 확인함
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM payment WHERE parking_id = ?")) {
                statement.setInt(1, testParkingId);
                try (ResultSet result = statement.executeQuery()) {
                    assertEquals(1, insertedCount);
                    assertTrue(result.next());
                    log.info("--- 결제 저장 결과: parkingId={}, finalFee={} ---",
                            result.getInt("parking_id"), result.getInt("final_fee"));
                    assertEquals(testPolicyId.intValue(), result.getInt("policy_id"));
                    assertEquals(1, result.getInt("payment_type"));
                    assertEquals(3000, result.getInt("calculated_fee"));
                    assertEquals(500, result.getInt("discount_amount"));
                    assertEquals(2500, result.getInt("final_fee"));
                    assertNotNull(result.getTimestamp("payment_date"));
                    assertFalse(result.next());
                }
            }
        }
    }

    @Test
    void insertPaymentRollbackTest() throws SQLException {
        PaymentVO payment = PaymentVO.builder()
                .parkingId(testParkingId)
                .policyId(testPolicyId)
                .paymentType(2)
                .calculatedFee(2000)
                .discountAmount(0)
                .finalFee(2000)
                .build();

        // DAO가 전달받은 연결을 커밋하지 않아 호출자가 롤백할 수 있는지 확인함
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            connection.setAutoCommit(false);
            assertEquals(1, paymentDAO.insertPayment(connection, payment));
            connection.rollback();
        }

        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM payment WHERE parking_id = ?")) {
            statement.setInt(1, testParkingId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                log.info("--- 결제 롤백 결과: parkingId={}, 남은 결제={} ---",
                        testParkingId, result.getInt(1));
                assertEquals(0, result.getInt(1));
            }
        }
    }

    @Test
    void selectAllPaymentsTest() throws SQLException {
        int paymentId = insertTestPayment(LocalDateTime.of(2026, 2, 12, 14, 30));

        List<PaymentVO> payments = paymentDAO.selectAllPayments();
        PaymentVO found = payments.stream()
                .filter(payment -> payment.getPaymentId() == paymentId)
                .findFirst()
                .orElse(null);

        log.info("--- 전체 결제 조회 결과: paymentId={}, 결제={} ---", paymentId, found);
        assertNotNull(found);
        assertEquals(testParkingId.intValue(), found.getParkingId());
        assertEquals(testPolicyId.intValue(), found.getPolicyId());
        assertEquals(testCarNum, found.getCarNum());
        assertEquals(3, found.getCarType());
        assertEquals(75, found.getTotalTime());
        assertEquals(1, found.getPaymentType());
        assertEquals(3000, found.getCalculatedFee());
        assertEquals(500, found.getDiscountAmount());
        assertEquals(2500, found.getFinalFee());
        assertEquals(LocalDateTime.of(2026, 2, 12, 14, 30), found.getPaymentDate());
    }

    @Test
    void selectPaymentByDateTest() throws SQLException {
        LocalDate targetDate = LocalDate.of(2026, 2, 12);
        int paymentId = insertTestPayment(targetDate.atTime(14, 30));

        List<PaymentVO> matching = paymentDAO.selectPaymentByDate(targetDate.toString());
        PaymentVO found = matching.stream()
                .filter(payment -> payment.getPaymentId() == paymentId)
                .findFirst()
                .orElse(null);
        List<PaymentVO> otherDay = paymentDAO.selectPaymentByDate(targetDate.plusDays(1).toString());
        boolean foundOnOtherDay = otherDay.stream()
                .anyMatch(payment -> payment.getPaymentId() == paymentId);

        log.info("--- 날짜별 결제 조회 결과: date={}, paymentId={}, 결제={} ---",
                targetDate, paymentId, found);
        assertNotNull(found);
        assertEquals(testCarNum, found.getCarNum());
        assertEquals(2500, found.getFinalFee());
        assertEquals(targetDate, found.getPaymentDate().toLocalDate());
        assertFalse(foundOnOtherDay);
    }

    // 조회 테스트가 사용할 결제 내역은 DAO 저장 메서드와 별개로 직접 준비함
    private int insertTestPayment(LocalDateTime paymentDate) throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO payment "
                             + "(parking_id, policy_id, payment_type, calculated_fee, "
                             + "discount_amount, final_fee, payment_date) VALUES (?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, testParkingId);
            statement.setInt(2, testPolicyId);
            statement.setInt(3, 1);
            statement.setInt(4, 3000);
            statement.setInt(5, 500);
            statement.setInt(6, 2500);
            statement.setTimestamp(7, Timestamp.valueOf(paymentDate));
            assertEquals(1, statement.executeUpdate());
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getInt(1);
            }
        }
    }
}
