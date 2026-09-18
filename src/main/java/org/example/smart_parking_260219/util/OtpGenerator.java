package org.example.smart_parking_260219.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * 인증 흐름에서 사용하는 6자리 OTP를 생성합니다.
 */
public final class OtpGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_BOUND = 1_000_000;

    private OtpGenerator() {
        // 인스턴스를 만들지 않고 공통 생성 메서드만 사용
    }

    /**
     * 000000부터 999999까지의 값을 항상 6자리 문자열로 반환합니다.
     */
    public static String generateSixDigitCode() {
        int number = SECURE_RANDOM.nextInt(OTP_BOUND);
        return String.format(Locale.ROOT, "%06d", number);
    }
}
