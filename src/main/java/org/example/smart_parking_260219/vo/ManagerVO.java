package org.example.smart_parking_260219.vo;

import lombok.*;

/**
 * manager 테이블의 관리자 정보를 담는 VO입니다.
 *
 * <p>
 * DAO에서 DB 조회 결과를 담거나,
 * DB에 저장할 관리자 정보를 전달할 때 사용합니다.
 * </p>
 */
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ManagerVO {
    private int managerNo;  // 관리자 내부 번호
    private String managerId;  // 로그인 아이디
    private String managerName;  // 관리자 이름
    private String password;  // 관리자 비밀번호
    private String email;  // 이메일 인증에 사용할 주소
    private boolean active;  // 계정 활성화 여부
    private String role;  // 관리자 권한 (ADMIN / NORMAL)
}
