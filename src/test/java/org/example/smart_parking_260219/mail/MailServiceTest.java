package org.example.smart_parking_260219.mail;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

@Log4j2
class MailServiceTest {
    @Test
    void sendMailTest() {
        MailService mailService = new
                MailService();
        String title = "Test Mail";
        String content = "<h1>This is a test email</h1><p>Sent from MailServiceTest</p>";
        String toEmail =
                "rkdtngus2201@naver.com";
        mailService.sendMail(title, content, toEmail);
    }
    @Test
    void sendMailWithHtmlTest() {
        MailService mailService = new
                MailService();
        String title = "Test Mail";
        String content = "<h1>This is a test email</h1><p>Sent from MailServiceTest</p>";
        String toEmail = "rkdtngus2201@naver.com";
        mailService.sendMailWithHtml(title,
                content, toEmail);
    }
}
