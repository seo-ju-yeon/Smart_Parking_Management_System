package org.example.smart_parking_260219.mail;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * JavaMail을 이용해 이메일을 발송하는 서비스 클래스입니다.
 *
 * <p>
 * {@link NaverEmailConfig}에서 제공하는 SMTP 설정과 인증 정보를 사용하여
 * 일반 텍스트 메일, HTML 메일, 인증번호 메일을 발송합니다.
 * </p>
 */
public class MailService {

    private final Properties props;
    private final NaverEmailConfig.SimpleAuthenticator authenticator;

    /**
     * 네이버 SMTP 설정과 인증 객체를 초기화합니다.
     */
    public MailService() {
        this.props = NaverEmailConfig.getProperties();
        this.authenticator = new NaverEmailConfig.SimpleAuthenticator();
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
            message.setFrom(new InternetAddress(props.getProperty("mail.username")));
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
            message.setFrom(new InternetAddress(props.getProperty("mail.username")));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(title);
             message.setText(body);

            // HTML 컨텐츠 설정 (한글 깨짐 방지)
            message.setContent(body, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 회원가입 인증번호가 포함된 HTML 이메일을 발송합니다.
     *
     * <p>
     * 메서드 내부에서 6자리 숫자 인증번호를 생성하고,
     * 해당 번호를 HTML 본문에 포함해 수신자에게 전송합니다.
     * </p>
     *
     * @param toEmail 수신자 이메일 주소
     */
    public void sendMailWithHtmlForAuth(String toEmail) {
        /* 메일 발송 : html 발송, 인증 번호 (메일 제목, 메일 내용, 받는 사람) */
        // JavaMail 세션은 실제 네트워크 연결 세션과는 다름. 정보를 담고 있는 객체
        Session session = Session.getInstance(props, authenticator);

        String title = "회원가입 인증 번호 입니다.";
        String body = "<h1>회원가입 인증 번호 입니다." + generateAuthCode() + "</h1><p>";

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(props.getProperty("mail.username")));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(title);

            // 인증번호 메일은 HTML 형식으로 발송
            message.setContent(body, "text/html; charset=UTF-8");

            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 6자리 숫자 인증번호를 생성합니다.
     *
     * @return 0부터 9까지의 숫자로 구성된 6자리 인증번호
     */
    private String generateAuthCode() {
        int codeLength = 6;
        StringBuilder authCode = new StringBuilder();

        for (int i = 0; i < codeLength; i++) {
            int digit = (int) (Math.random() * 10);
            authCode.append(digit);
        }
        return authCode.toString();
    }
}