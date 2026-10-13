package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.ManagerDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// 설정된 DB에 관리자를 추가하고 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ManagerServiceTest {

    private final ManagerService managerService = ManagerService.INSTANCE;

    @Test
    @Disabled("전용 테스트 DB와 검증 코드가 준비될 때까지 실행하지 않음")
    public void testAddManager() {
        ManagerDTO managerDTO = ManagerDTO.builder()
                .managerId("testAdmin")
                .managerName("테스트관리자")
                .password("1234")
                .email("test@parking.com")
                .build();

        log.info("등록 시도 데이터: {}", managerDTO);

        managerService.addManager(managerDTO);
    }

    @Test
    public void testIsAuthSuccess() {
        String id = "testAdmin";
        String pw = "1234";

        // 사전 등록된 활성 관리자 계정을 전제로 검증함
        boolean result = managerService.isAuth(id, pw);

        log.info("정상 로그인 확인 결과: {}", result);
        Assertions.assertTrue(result);
    }

    @Test
    public void testIsAuthFailPassword() {
        String id = "testAdmin";
        String pw = "wrong_password";

        boolean result = managerService.isAuth(id, pw);

        log.info("비밀번호 틀림 인증 결과: {}", result);
        Assertions.assertFalse(result);
    }

    @Test
    public void testIsAuthFailId() {
        String id = "non_existent_id";
        String pw = "1234";

        boolean result = managerService.isAuth(id, pw);

        log.info("존재하지 않는 아이디 인증 결과: {}", result);
        Assertions.assertFalse(result);
    }

    @Test
    public void testIsAuthDeactivateAccount() {
        // 비활성 상태로 준비된 기존 관리자 계정을 사용함
        String id = "Test01";
        String pw = "1234";

        boolean result = managerService.isAuth(id, pw);

        log.info("비활성화 계정 인증 결과: " + result);
        Assertions.assertFalse(result, "비활성화 계정 = false(0)");
    }

    @Test
    public void testGetAllManagers() {
        log.info("=== 전체 관리자 목록 조회(Service) 테스트 ===");

        java.util.List<ManagerDTO> dtoList = managerService.getAllManagers();

        Assertions.assertNotNull(dtoList);

        // 사전 등록된 관리자가 최소 1명 있다는 전제로 검증함
        Assertions.assertTrue(dtoList.size() > 0, "조회된 관리자 DTO 목록이 비어있습니다.");

        dtoList.forEach(dto -> log.info("조회된 DTO: {}", dto));
        log.info("총 관리자 수: {}", dtoList.size());
    }
}
