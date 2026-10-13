package org.example.smart_parking_260219.service;


import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.FeePolicyDTO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// 설정된 DB에 요금 정책을 추가하고 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class FeePolicyServiceTest {

    private FeePolicyService feePolicyService = FeePolicyService.getInstance();

    @Test
    @Disabled("전용 테스트 DB와 검증 코드가 준비될 때까지 실행하지 않음")
    public void addPolicy() {
        FeePolicyDTO dto = FeePolicyDTO.builder()
                .gracePeriod(20)
                .defaultTime(10)
                .defaultFee(1000)
                .extraTime(30)
                .extraFee(1000)
                .lightDiscount(0.3)
                .disabledDiscount(0.5)
                .subscribedFee(100000)
                .maxDailyFee(15000)
                .build();

        feePolicyService.addPolicy(dto);
        log.info("정책 등록 완료");
    }

    @Test
    public void getPolicyList() {
        List<FeePolicyDTO> list = feePolicyService.getPolicyList();

        log.info("정책 목록 개수: {}", list.size());
        for (FeePolicyDTO dto : list) {
            log.info("policy: {}", dto);
        }
    }

    @Test
    public void getPolicy() {
        FeePolicyDTO feePolicyDTO = feePolicyService.getPolicy();

        log.info("정책 상세 조회 결과: {}", feePolicyDTO);

        assertNotNull(feePolicyDTO);
    }

}
