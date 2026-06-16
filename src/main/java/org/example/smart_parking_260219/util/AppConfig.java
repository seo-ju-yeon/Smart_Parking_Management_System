package org.example.smart_parking_260219.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 애플리케이션 실행에 필요한 외부 설정값을 읽는 유틸리티 클래스입니다.
 */
public final class AppConfig {

    private static final String CONFIG_FILE = "application.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException(CONFIG_FILE + " 파일을 찾을 수 없습니다.");
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new IllegalStateException(CONFIG_FILE + " 파일을 읽는 중 오류가 발생했습니다.", e);
        }
    }

    private AppConfig() {
    }

    public static String get(String key) {
        String value = System.getenv(toEnvKey(key));

        if (value == null || value.isBlank()) {
            value = PROPERTIES.getProperty(key);
        }

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("필수 설정값이 없습니다: " + key);
        }

        return value.trim();
    }

    private static String toEnvKey(String key) {
        return key.toUpperCase().replace('.', '_').replace('-', '_');
    }
}
