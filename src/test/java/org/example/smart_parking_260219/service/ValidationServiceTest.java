package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.service.ValidationService.VerificationResult;
import org.example.smart_parking_260219.util.AppConfig;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB의 인증 정보로 검증과 폐기 결과를 확인함
@Tag("integration")
@Log4j2
class ValidationServiceTest {

    private final ValidationService validationService = new ValidationService();
    private String testEmail;

    @BeforeEach
    void setUp() {
        // IDE에서 직접 실행해도 개발 DB의 인증 정보를 변경하지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        testEmail = "otp-" + UUID.randomUUID().toString().substring(0, 8)
                + "@smartparking.local";
        log.info("--- 인증 서비스 테스트 준비: email={} ---", testEmail);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testEmail == null) {
            return;
        }

        // 이번 테스트에서 사용한 이메일의 인증 정보만 삭제함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM validation WHERE email = ?")) {
            statement.setString(1, testEmail);
            int deletedCount = statement.executeUpdate();
            log.info("--- 인증 서비스 테스트 정리: email={}, 삭제 {}건 ---", testEmail, deletedCount);
        }
    }

    @Test
    void verifyMissingCodeTest() {
        VerificationResult result = validationService.verifyAuthCode(testEmail, "123456");

        log.info("--- 인증 정보 없음 결과: email={}, 결과={} ---", testEmail, result);
        assertEquals(VerificationResult.NOT_FOUND, result);
    }

    @Test
    void verifyCorrectCodeTest() throws SQLException {
        insertTestValidation("123456", LocalDateTime.now().plusMinutes(10));

        VerificationResult result = validationService.verifyAuthCode(testEmail, "123456");

        log.info("--- 인증번호 일치 결과: email={}, 결과={} ---", testEmail, result);
        assertEquals(VerificationResult.SUCCESS, result);
    }

    @Test
    void verifyWrongCodeTest() throws SQLException {
        insertTestValidation("123456", LocalDateTime.now().plusMinutes(10));

        VerificationResult result = validationService.verifyAuthCode(testEmail, "654321");

        log.info("--- 인증번호 불일치 결과: email={}, 결과={} ---", testEmail, result);
        assertEquals(VerificationResult.INVALID_CODE, result);
    }

    @Test
    void verifyExpiredCodeTest() throws SQLException {
        insertTestValidation("123456", LocalDateTime.now().minusMinutes(10));

        VerificationResult result = validationService.verifyAuthCode(testEmail, "123456");

        log.info("--- 인증번호 만료 결과: email={}, 결과={} ---", testEmail, result);
        assertEquals(VerificationResult.EXPIRED, result);
    }

    @Test
    void invalidateAuthCodeTest() throws SQLException {
        insertTestValidation("123456", LocalDateTime.now().plusMinutes(10));

        validationService.invalidateAuthCode(testEmail);
        VerificationResult result = validationService.verifyAuthCode(testEmail, "123456");

        log.info("--- 인증번호 폐기 결과: email={}, 결과={} ---", testEmail, result);
        assertEquals(VerificationResult.NOT_FOUND, result);
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM validation WHERE email = ?")) {
            statement.setString(1, testEmail);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(0, resultSet.getInt(1));
            }
        }
    }

    // 검증 분기마다 필요한 인증번호와 만료 시각을 직접 준비함
    private void insertTestValidation(String code, LocalDateTime expiryTime) throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO validation (string_otp, email, expiry_time) VALUES (?, ?, ?)")) {
            statement.setString(1, code);
            statement.setString(2, testEmail);
            statement.setTimestamp(3, Timestamp.valueOf(expiryTime));
            assertEquals(1, statement.executeUpdate());
        }
    }
}
