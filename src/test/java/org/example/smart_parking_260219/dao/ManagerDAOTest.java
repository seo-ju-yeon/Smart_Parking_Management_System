package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.vo.ManagerVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// 설정된 DB의 기존 데이터에 의존하고 값을 변경하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ManagerDAOTest {

    private ManagerDAO managerDAO = ManagerDAO.getInstance();

    @Test
    @Disabled("전용 테스트 DB와 검증 코드가 준비될 때까지 실행하지 않음")
    public void insertManagerTest() {
        ManagerVO vo = ManagerVO.builder()
                .managerId("test03")
                .managerName("테스터03")
                .password("3333")
                .email("test03@naver.com")
                .build();
        managerDAO.insertManager(vo);
        log.info("관리자 등록 완료 {}", vo);
    }

    @Test
    public void selectOneTest() {
        String managerId = "test02";
        ManagerVO vo = managerDAO.selectOne(managerId);

        log.info("관리자 아이디 조회 결과: {}", vo);
        Assertions.assertNotNull(vo);
        Assertions.assertEquals(managerId, vo.getManagerId());
    }

    @Test
    public void selectAllTest() {
        log.info("=== 전체 관리자 목록 조회 테스트 ===");

        java.util.List<ManagerVO> list = managerDAO.selectAll();

        Assertions.assertNotNull(list, "조회된 리스트 객체가 null입니다.");

        // 사전 등록된 관리자가 최소 1명 있다는 전제로 검증함
        Assertions.assertFalse(list.isEmpty(), "조회된 관리자 목록이 비어있습니다.");

        list.forEach(vo -> log.info("조회된 관리자: {}", vo));
        log.info("총 관리자 수: {}", list.size());
    }

    @Test
    @Disabled("전용 테스트 DB와 검증 코드가 준비될 때까지 실행하지 않음")
    public void updateActiveTest() {
        String managerId = "test01";
        boolean targetStatus = false;

        managerDAO.updateActive(targetStatus, managerId);

        ManagerVO updatedVo = managerDAO.selectOne(managerId);

        Assertions.assertNotNull(updatedVo, "조회된 관리자 정보가 없습니다.");
        Assertions.assertEquals(targetStatus, updatedVo.isActive(), "활성화 상태가 정상적으로 변경되지 않았습니다.");

        log.info("수정 완료 후 데이터: {}", updatedVo);
    }
}
