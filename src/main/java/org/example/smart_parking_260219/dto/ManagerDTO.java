package org.example.smart_parking_260219.dto;

import lombok.*;

/**
 * 관리자 정보를 화면과 서비스 계층 사이에서 전달하기 위한 DTO입니다.
 *
 * <p>
 * 관리자 등록, 로그인, 관리자 목록 조회, 관리자 정보 수정에서 사용됩니다.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ManagerDTO {
    private int managerNo;  // 관리자 내부 번호
    private String managerId;  // 로그인 아이디
    private String managerName;  // 관리자 이름
    private String password;  // 관리자 비밀번호
    private String email;  // 이메일 인증에 사용할 주소
    private boolean active;  // 계정 활성화 여부
    private String role;  // 관리자 권한 (ADMIN / NORMAL)
}
