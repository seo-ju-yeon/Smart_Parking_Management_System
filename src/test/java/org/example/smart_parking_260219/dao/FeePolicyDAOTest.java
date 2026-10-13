package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.vo.FeePolicyVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// 설정된 DB에 직접 접근하고 데이터를 변경하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class FeePolicyDAOTest {
    private final FeePolicyDAO feePolicyDAO = FeePolicyDAO.getInstance();

    @Test
    public void insertPolicy()  {
        FeePolicyVO feePolicyVo = FeePolicyVO.builder()
                .gracePeriod(10)
                .defaultTime(10)
                .defaultFee(2000)
                .extraTime(30)
                .extraFee(1000)
                .lightDiscount(0.3)
                .disabledDiscount(0.5)
                .subscribedFee(100000)
                .maxDailyFee(15000)
                .isActive(true).build();
        feePolicyDAO.insertPolicy(feePolicyVo);
    }

    @Test
    public void insertPolicyDummy() {
        for (int i = 0; i < 100; i++) {
            FeePolicyVO feePolicyVo = FeePolicyVO.builder()
                    .gracePeriod(10)
                    .defaultTime(10)
                    .defaultFee(2000)
                    .extraTime(30)
                    .extraFee(1000)
                    .lightDiscount(0.3)
                    .disabledDiscount(0.5)
                    .subscribedFee(100000)
                    .maxDailyFee(140000 + i)
                    .isActive(true).build();
            feePolicyDAO.insertPolicy(feePolicyVo);
        }
    }

    @Test
    void selectAllPolicies() {
        List<FeePolicyVO> list = feePolicyDAO.selectAllPolicies();

        log.info("총 정책 개수: {}", list.size());
        for (FeePolicyVO vo : list) {
            log.info("policy: {}", vo);
        }
    }

    @Test
    void selectOnePolicy() {

        FeePolicyVO vo = feePolicyDAO.selectOnePolicy();

        log.info("조회 결과: {}", vo);

        assertNotNull(vo);
        assertTrue(vo.isActive());
    }
}
