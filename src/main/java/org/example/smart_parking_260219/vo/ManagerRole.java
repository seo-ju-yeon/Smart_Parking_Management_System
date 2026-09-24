package org.example.smart_parking_260219.vo;

import java.util.Locale;

/**
 * 관리자 계정에서 사용할 수 있는 역할을 정의합니다.
 */
public enum ManagerRole {
    NORMAL,
    ADMIN;

    /**
     * DB에서 조회한 문자열 역할값을 ManagerRole로 변환합니다.
     *
     * @param value DB에 저장된 역할값
     * @return 변환된 관리자 역할
     * @throws IllegalArgumentException 역할값이 비어 있거나 정의되지 않은 경우
     */
    public static ManagerRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("관리자 역할값이 비어 있습니다.");
        }

        return ManagerRole.valueOf(
                value.trim().toUpperCase(Locale.ROOT)
        );
    }
}
