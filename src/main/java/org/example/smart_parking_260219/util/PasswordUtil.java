package org.example.smart_parking_260219.util;

import org.mindrot.jbcrypt.BCrypt;
import lombok.extern.log4j.Log4j2;

/**
 * 비밀번호 암호화와 검증을 처리하는 유틸 클래스입니다.
 *
 * <p>BCrypt를 사용해서 비밀번호를 해싱하고, 로그인 시 입력한 비밀번호와
 * DB에 저장된 해시값을 비교합니다.</p>
 */
@Log4j2
public class PasswordUtil {

    /** BCrypt 해싱 강도입니다. */
    private static final int WORK_FACTOR = 12;

    private PasswordUtil() {
        // 유틸 클래스 인스턴스화 방지
    }

    /**
     * 평문 비밀번호를 BCrypt 해시값으로 변환합니다.
     *
     * @param plainPassword 사용자가 입력한 평문 비밀번호
     * @return BCrypt로 암호화된 비밀번호
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 null이거나 빈 문자열일 수 없습니다.");
        }

        try {
            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
            log.debug("비밀번호 해싱 완료");
            return hashedPassword;
        } catch (Exception e) {
            log.error("비밀번호 해싱 중 오류 발생", e);
            throw new RuntimeException("비밀번호 암호화에 실패했습니다.", e);
        }
    }

    /**
     * 평문 비밀번호와 저장된 BCrypt 해시값이 일치하는지 확인합니다.
     *
     * @param plainPassword  사용자가 입력한 평문 비밀번호
     * @param hashedPassword DB에 저장된 BCrypt 해시값
     * @return 비밀번호가 일치하면 true, 아니면 false
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            log.warn("비밀번호 검증 실패: 입력값 없음");
            return false;
        }

        if (hashedPassword == null || hashedPassword.trim().isEmpty()) {
            log.warn("비밀번호 검증 실패: 저장된 해시값 없음");
            return false;
        }

        try {
            boolean matches = BCrypt.checkpw(plainPassword, hashedPassword);
            log.debug("비밀번호 검증 결과: {}", matches ? "일치" : "불일치");
            return matches;
        } catch (Exception e) {
            log.error("비밀번호 검증 중 오류 발생", e);
            return false;
        }
    }

    /**
     * BCrypt 해시값에서 work factor를 추출합니다.
     *
     * @param hashedPassword BCrypt 해시값
     * @return 추출한 work factor, 확인할 수 없으면 -1
     */
    public static int getWorkFactor(String hashedPassword) {
        if (hashedPassword == null || hashedPassword.length() < 7) {
            return -1;
        }

        try {
            // BCrypt 해시 형식은 $2a$12$... 처럼 구성된다.
            String[] parts = hashedPassword.split("\\$");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2]);
            }
        } catch (Exception e) {
            log.warn("Work factor 추출 실패", e);
        }
        return -1;
    }
}
