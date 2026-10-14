package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.mail.SmtpConfig;
import org.example.smart_parking_260219.service.ValidationService.Purpose;
import org.example.smart_parking_260219.service.ValidationService.VerificationResult;
import org.example.smart_parking_260219.util.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 전용 테스트 DB와 로컬 Mailpit에서만 인증 메일 발송을 확인함
@Tag("mail-integration")
@Log4j2
class ValidationServiceMailIntegrationTest {

    private final ValidationService validationService = new ValidationService();
    private String testEmail;

    @BeforeAll
    static void requireTestDbAndLocalMailpit() {
        // IDE에서 직접 실행해도 개발 DB와 외부 SMTP로 요청을 보내지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        Properties smtp = SmtpConfig.getProperties();
        String host = smtp.getProperty("mail.smtp.host");
        boolean localHost = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
        assertTrue(localHost);
        assertEquals("1025", smtp.getProperty("mail.smtp.port"));
        assertEquals("false", smtp.getProperty("mail.smtp.auth"));
        assertEquals("false", smtp.getProperty("mail.smtp.ssl.enable"));
        assertEquals("false", smtp.getProperty("mail.smtp.starttls.enable"));
    }

    @BeforeEach
    void setUp() {
        testEmail = "otp-mail-" + UUID.randomUUID().toString().substring(0, 8)
                + "@smartparking.local";
        log.info("--- 인증 메일 테스트 준비: email={} ---", testEmail);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testEmail == null) {
            return;
        }

        // 테스트 DB의 인증 정보만 삭제함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM validation WHERE email = ?")) {
            statement.setString(1, testEmail);
            int deletedCount = statement.executeUpdate();
            log.info("--- 인증 메일 테스트 정리: email={}, 삭제 {}건 ---",
                    testEmail, deletedCount);
        }
    }

    @Test
    void resendAuthCodeTest() throws Exception {
        // 첫 호출은 기본 용도, 두 번째 호출은 비밀번호 찾기 용도를 사용함
        String firstCode = validationService.sendAuthCode(testEmail);
        assertTrue(firstCode.matches("[0-9]{6}"));
        assertEquals(VerificationResult.SUCCESS,
                validationService.verifyAuthCode(testEmail, firstCode));

        String newCode = validationService.sendAuthCode(testEmail, Purpose.FORGOT_PASSWORD);
        assertTrue(newCode.matches("[0-9]{6}"));
        assertEquals(VerificationResult.SUCCESS,
                validationService.verifyAuthCode(testEmail, newCode));

        // 재발송 뒤에는 현재 번호 한 건만 DB에 남아야 함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT string_otp FROM validation WHERE email = ?")) {
            statement.setString(1, testEmail);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(newCode, resultSet.getString("string_otp"));
                assertFalse(resultSet.next());
            }
        }

        log.info("--- 인증 메일 재발송 결과: email={}, 현재 코드 1건 유지 ---", testEmail);
    }
}
