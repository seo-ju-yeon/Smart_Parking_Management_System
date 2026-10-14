package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.util.AppConfig;
import org.example.smart_parking_260219.vo.ValidationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB에 이메일 인증 정보를 저장하고 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ValidationDAOTest {

    private final ValidationDAO validationDAO = new ValidationDAO();
    private String testEmail;

    @BeforeEach
    void setUp() {
        // IDE에서 직접 실행해도 개발 DB에 데이터를 넣지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        testEmail = "otp-" + UUID.randomUUID().toString().substring(0, 8) + "@smartparking.local";
        log.info("--- 인증 정보 테스트 주소 준비: email={} ---", testEmail);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testEmail == null) {
            return;
        }

        // 테스트가 만든 이메일의 인증 정보만 직접 삭제함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM validation WHERE email = ?")) {
            statement.setString(1, testEmail);
            int deletedCount = statement.executeUpdate();
            log.info("--- 인증 정보 테스트 데이터 정리: email={}, 삭제 {}건 ---", testEmail, deletedCount);
        }
    }

    @Test
    void insertTest() throws SQLException {
        ValidationVO validation = ValidationVO.builder()
                .stringOTP("123456")
                .email(testEmail)
                .build();
        LocalDateTime beforeInsert = LocalDateTime.now();

        validationDAO.insert(validation);

        // 저장 메서드의 결과는 SQL로 직접 조회해 확인함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT string_otp, expiry_time FROM validation WHERE email = ?")) {
            statement.setString(1, testEmail);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                LocalDateTime expiryTime = result.getTimestamp("expiry_time").toLocalDateTime();
                LocalDateTime afterInsert = LocalDateTime.now();

                log.info("--- 인증 정보 저장 결과: email={}, 만료 시각={} ---", testEmail, expiryTime);
                assertEquals("123456", result.getString("string_otp"));
                assertFalse(expiryTime.isBefore(beforeInsert.plusMinutes(5).minusSeconds(1)));
                assertFalse(expiryTime.isAfter(afterInsert.plusMinutes(5).plusSeconds(1)));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void selectTest() throws SQLException {
        LocalDateTime expiryTime = LocalDateTime.of(2030, 1, 2, 3, 4, 5);
        insertTestValidation("654321", expiryTime);

        ValidationVO found = validationDAO.select(testEmail);

        log.info("--- 인증 정보 조회 결과: {} ---", found);
        assertNotNull(found);
        assertTrue(found.getNo() > 0);
        assertEquals("654321", found.getStringOTP());
        assertEquals(testEmail, found.getEmail());
        assertEquals(expiryTime, found.getExpiryTime());
    }

    @Test
    void selectMissingEmailTest() {
        ValidationVO found = validationDAO.select(testEmail);

        log.info("--- 없는 인증 정보 조회 결과: email={}, 결과={} ---", testEmail, found);
        assertNull(found);
    }

    @Test
    void deleteByEmailTest() throws SQLException {
        insertTestValidation("654321", LocalDateTime.now().plusMinutes(5));

        validationDAO.deleteByEmail(testEmail);

        // 삭제 결과는 SQL로 직접 조회해 확인함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM validation WHERE email = ?")) {
            statement.setString(1, testEmail);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                log.info("--- 인증 정보 삭제 결과: email={}, 남은 정보={}건 ---", testEmail, result.getInt(1));
                assertEquals(0, result.getInt(1));
            }
        }
    }

    // 조회와 삭제 테스트에서 사용할 인증 정보는 SQL로 직접 준비함
    private void insertTestValidation(String otp, LocalDateTime expiryTime) throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO validation (string_otp, email, expiry_time) VALUES (?, ?, ?)")) {
            statement.setString(1, otp);
            statement.setString(2, testEmail);
            statement.setTimestamp(3, Timestamp.valueOf(expiryTime));
            assertEquals(1, statement.executeUpdate());
        }
    }
}
