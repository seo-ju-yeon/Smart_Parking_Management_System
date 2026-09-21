package org.example.smart_parking_260219.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * JavaMail을 이용해 이메일을 발송하는 서비스 클래스입니다.
 *
 * <p>
 * {@link SmtpConfig}에서 제공하는 SMTP 설정과 선택적 인증 정보를 사용하여
 * 일반 텍스트 메일과 HTML 메일을 발송합니다.
 * </p>
 */
public class MailService {

    private final Properties props;
    private final Authenticator authenticator;
    private final String fromAddress;

    /**
     * 현재 실행 환경의 SMTP 설정과 발신자 주소를 초기화합니다.
     */
    public MailService() {
        this.props = SmtpConfig.getProperties();
        this.authenticator = SmtpConfig.getAuthenticator();
        this.fromAddress = SmtpConfig.getFromAddress();
    }

    /**
     * 일반 텍스트 형식의 이메일을 발송합니다.
     *
     * @param title   메일 제목
     * @param body    메일 본문
     * @param toEmail 수신자 이메일 주소
     */
    public void sendMail(String title, String body, String toEmail) {
        /* 메일 발송 : 단순 텍스트 (메일 제목, 메일 내용, 받는 사람) */
        // JavaMail 세션은 실제 네트워크 연결 세션과는 다름. 정보를 담고 있는 객체
        Session session = Session.getInstance(props, authenticator);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(title);
            message.setText(body);

            // Transport.send()는 메일 발송 후 연결을 자동으로 종료
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * HTML 형식의 이메일을 발송합니다.
     *
     * @param title   메일 제목
     * @param body    HTML 형식의 메일 본문
     * @param toEmail 수신자 이메일 주소
     */
    public void sendMailWithHtml(String title, String body, String toEmail) {
        /* 메일 발송 : html 발송 (메일 제목, 메일 내용, 받는 사람) */
        // JavaMail 세션은 실제 네트워크 연결 세션과는 다름. 정보를 담고 있는 객체
        Session session = Session.getInstance(props, authenticator);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(title);

            // HTML 컨텐츠 설정 (한글 깨짐 방지)
            message.setContent(body, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

}
