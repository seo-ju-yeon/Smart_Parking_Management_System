package org.example.smart_parking_260219.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 이메일 인증 정보를 전달하기 위한 DTO입니다.
 *
 * <p>
 * 관리자 등록, 관리자 정보 수정, 비밀번호 찾기에서 사용하는
 * 인증번호와 만료 시간을 담습니다.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationDTO {
    private int no;  // 인증 정보 번호
    private String stringOTP;  // 이메일로 발송된 인증번호
    private String email;  // 인증번호를 받을 이메일
    private LocalDateTime expiryTime;  // 인증번호 만료 시간
}
