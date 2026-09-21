package org.example.smart_parking_260219.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import org.example.smart_parking_260219.util.AppConfig;

import java.util.Properties;

/**
 * 실행 환경에 맞는 SMTP 연결 설정과 선택적 인증 정보를 제공합니다.
 *
 * <p>로컬에서는 인증과 SSL이 필요 없는 Mailpit을 사용하고,
 * 실제 메일 서버에서는 설정값에 따라 인증과 암호화를 사용할 수 있습니다.</p>
 */
public final class SmtpConfig {

    private SmtpConfig() {
    }

    /**
     * Jakarta Mail이 사용할 SMTP 속성을 생성합니다.
     *
     * @return SMTP 호스트, 포트, 인증 및 암호화 설정
     */
    public static Properties getProperties() {
        String host = AppConfig.get("mail.host");
        String port = AppConfig.get("mail.port");
        boolean authEnabled = getBoolean("mail.auth");
        boolean sslEnabled = getBoolean("mail.ssl.enable");
        boolean startTlsEnabled = getBoolean("mail.starttls.enable");
        boolean debugEnabled = getBoolean("mail.debug");

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.host", host);
        props.setProperty("mail.smtp.port", port);
        props.setProperty("mail.smtp.auth", Boolean.toString(authEnabled));
        props.setProperty("mail.smtp.ssl.enable", Boolean.toString(sslEnabled));
        props.setProperty("mail.smtp.starttls.enable", Boolean.toString(startTlsEnabled));
        props.setProperty("mail.debug", Boolean.toString(debugEnabled));

        if (sslEnabled) {
            props.setProperty("mail.smtp.ssl.trust", host);
        }

        return props;
    }

    /**
     * SMTP 인증을 사용하는 환경에만 인증 객체를 제공합니다.
     *
     * @return 인증을 사용하면 계정 정보가 담긴 객체, 사용하지 않으면 {@code null}
     */
    public static Authenticator getAuthenticator() {
        if (!getBoolean("mail.auth")) {
            return null;
        }

        String username = AppConfig.get("mail.username");
        String password = AppConfig.get("mail.password");

        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
    }

    /**
     * 메일에 표시할 발신자 주소를 반환합니다.
     *
     * @return 발신자 이메일 주소
     */
    public static String getFromAddress() {
        return AppConfig.get("mail.from");
    }

    /**
     * 문자열 설정을 boolean으로 변환하고 잘못된 값은 즉시 차단합니다.
     */
    private static boolean getBoolean(String key) {
        String value = AppConfig.get(key);

        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalStateException(
                    "설정값은 true 또는 false여야 합니다: " + key
            );
        }

        return Boolean.parseBoolean(value);
    }
}
