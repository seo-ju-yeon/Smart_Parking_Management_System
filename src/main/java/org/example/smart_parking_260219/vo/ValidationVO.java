package org.example.smart_parking_260219.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * validation 테이블의 이메일 인증 정보를 담는 VO입니다.
 *
 * <p>
 * DAO에서 인증정보를 저장하거나,
 * DB에서 조회한 인증번호와 만료 시간을 담을 때 사용합니다.
 * </p>
 */
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationVO {
    private int no;  // 인증 정보 번호
    private String stringOTP;  // 이메일로 발송된 인증번호
    private String email;  // 인증번호를 받을 이메일
    private LocalDateTime expiryTime;  // 인증번호 만료 시간
}
