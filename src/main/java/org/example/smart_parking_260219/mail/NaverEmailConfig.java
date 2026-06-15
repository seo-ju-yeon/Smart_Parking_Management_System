package org.example.smart_parking_260219.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import java.util.Properties;

/**
 * 네이버 SMTP 서버를 이용한 이메일 발송 설정을 제공하는 클래스입니다.
 *
 * <p>
 * 메일 발송에 필요한 SMTP 호스트, 포트, 인증 계정, SSL/TLS 설정을 관리합니다.
 * {@link MailService} 등 메일 발송 클래스에서 세션 생성 시 이 설정을 사용합니다.
 * </p>
 */
public class NaverEmailConfig {

    /** 네이버 SMTP SSL 포트 (smtp.port) */
    private final String port = "465";

    /** 네이버 SMTP 서버 호스트 */
    private static final String host = "smtp.naver.com";

    /** 발신자 이메일 계정입니다. */
    private static final String username = "wndus6110@naver.com";

    /** SMTP 인증에 사용할 비밀번호 또는 애플리케이션 비밀번호입니다. */
    private static final String password = "1EV12JZMHMGR";

    /**
     * SMTP 서버 인증 정보를 제공하는 인증 객체입니다.
     *
     * <p>
     * JavaMail 세션이 SMTP 서버에 로그인할 때 발신자 계정과 비밀번호를 전달합니다.
     * </p>
     */
    public static class SimpleAuthenticator extends Authenticator {

        /**
         * SMTP 인증에 사용할 계정 정보를 반환합니다.
         *
         * @return 발신자 이메일 계정과 비밀번호를 담은 인증 정보
         */
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            // 서버에 인증 정보를 담은 객체를 반환
            return new PasswordAuthentication(username, password);
        }
    }

    /**
     * 네이버 SMTP 연결에 필요한 메일 설정값을 생성합니다.
     *
     * <p>
     * SMTP 연결에필요한 세부 프로토콜 및 보안 설정을 Properties 객체에 담아 반환합니다.
     * </p>
     *
     * @return SMTP 연결 설정이 담긴 Properties 객체
     */
    public static Properties getProperties() {
        Properties props = new Properties();

        props.put("mail.username", username);
        props.put("mail.host", host);
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.debug", "true");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", "true");

        return props;
    }
}
