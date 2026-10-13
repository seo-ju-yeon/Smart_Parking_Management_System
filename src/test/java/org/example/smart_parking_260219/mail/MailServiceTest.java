package org.example.smart_parking_260219.mail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Properties;

// 로컬 Mailpit 기본 설정에서만 발송하므로 별도 통합 테스트로 분류함
@Tag("mail-integration")
class MailServiceTest {

    @BeforeAll
    static void requireLocalMailpit() {
        // 현재 SMTP 설정 읽음
        Properties smtp = SmtpConfig.getProperties();
        String host = smtp.getProperty("mail.smtp.host");
        // 내 컴퓨터를 가리키는 주소만 허용
        boolean localHost = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);

        // Mailpit 기본 포트와 인증, 안호화 설정을 확인하고 다르면 발송 전에 중단함
        if (!localHost
                || !"1025".equals(smtp.getProperty("mail.smtp.port"))
                || !"false".equals(smtp.getProperty("mail.smtp.auth"))
                || !"false".equals(smtp.getProperty("mail.smtp.ssl.enable"))
                || !"false".equals(smtp.getProperty("mail.smtp.starttls.enable"))) {
            throw new IllegalStateException("메일 테스트는 로컬 Mailpit SMTP 설정에서만 실행할 수 있음");
        }
    }

    @Test
    void sendMailTest() {
        MailService mailService = new MailService();
        String title = "Test Mail";
        String content = "<h1>This is a test email</h1><p>Sent from MailServiceTest</p>";
        String toEmail = "mail-test@smartparking.local";
        mailService.sendMail(title, content, toEmail);
    }

    @Test
    void sendMailWithHtmlTest() {
        MailService mailService = new MailService();
        String title = "Test Mail";
        String content = "<h1>This is a test email</h1><p>Sent from MailServiceTest</p>";
        String toEmail = "mail-test@smartparking.local";
        mailService.sendMailWithHtml(title, content, toEmail);
    }
}
