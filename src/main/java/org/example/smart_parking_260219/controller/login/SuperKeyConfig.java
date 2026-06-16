package org.example.smart_parking_260219.controller.login;

import org.example.smart_parking_260219.util.AppConfig;

/**
 * 포트폴리오 시연용 슈퍼 계정과 슈퍼패스 OTP 설정을 관리하는 클래스입니다.
 *
 * <p>
 * 슈퍼 계정은 데이터베이스에 등록된 계정을 기준으로 하며,
 * 로그인 및 인증 흐름에서 관리자 권한과 일반 사용자 기능을 동시에 확인할 수 있도록 사용합니다.
 * </p>
 *
 * <p>
 * 슈퍼패스 OTP는 실제 발송된 인증번호와 관계없이 인증을 통과시키는 시연용 기능입니다.
 * 운영 환경에서는 제거하거나 외부 설정으로 분리해야 합니다.
 * </p>
 */
public class SuperKeyConfig {

    private SuperKeyConfig() {
    }

    /**
     * 데이터베이스에 실제로 존재하는 슈퍼 계정 아이디입니다.
     */
    public static final String SUPER_ID = AppConfig.get("super.id");

    /**
     * 이메일 인증번호 검증을 우회할 수 있는 시연용 OTP 값입니다.
     */
    public static final String SUPER_OTP = AppConfig.get("super.otp");

    /**
     * 세션에 저장할 슈퍼 계정 역할값입니다.
     */
    public static final String SUPER_ROLE = AppConfig.get("super.role");

    /**
     * 전달받은 관리자 아이디가 슈퍼 계정인지 확인합니다.
     *
     * @param managerId 확인할 관리자 아이디
     * @return 슈퍼 계정이면 true, 아니면 false
     */
    public static boolean isSuperAccount(String managerId) {
        return SUPER_ID.equals(managerId);
    }

    /**
     * 전달받은 OTP가 슈퍼패스 OTP인지 확인합니다.
     *
     * @param inputOtp 사용자가 입력한 OTP
     * @return 슈퍼패스 OTP와 일치하면 true, 아니면 false
     */
    public static boolean isSuperOtp(String inputOtp) {
        return SUPER_OTP.equals(inputOtp);
    }
}
