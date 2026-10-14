package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.vo.FeePolicyVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// DB 연결 없이 요금과 차량 유형별 할인 계산을 확인함
@Log4j2
class PaymentServiceTest {

    private final PaymentService paymentService = PaymentService.INSTANCE;
    private final LocalDateTime entryTime = LocalDateTime.of(2026, 1, 1, 8, 0);

    @Test
    void gracePeriodTest() {
        FeePolicyVO policy = testPolicy();

        int freeFee = paymentService.calculateFeeLogic(entryTime, entryTime.plusMinutes(10), policy);
        int chargedFee = paymentService.calculateFeeLogic(entryTime, entryTime.plusMinutes(11), policy);

        log.info("--- 무료 시간 경계 요금: 10분={}, 11분={} ---", freeFee, chargedFee);
        assertEquals(0, freeFee);
        assertEquals(2000, chargedFee);
    }

    @Test
    void extraFeeTest() {
        FeePolicyVO policy = testPolicy();

        int baseFee = paymentService.calculateFeeLogic(entryTime, entryTime.plusMinutes(60), policy);
        int firstExtraFee = paymentService.calculateFeeLogic(entryTime, entryTime.plusMinutes(61), policy);
        int secondExtraFee = paymentService.calculateFeeLogic(entryTime, entryTime.plusMinutes(91), policy);

        log.info("--- 추가 시간 요금: 60분={}, 61분={}, 91분={} ---",
                baseFee, firstExtraFee, secondExtraFee);
        assertEquals(2000, baseFee);
        assertEquals(3000, firstExtraFee);
        assertEquals(4000, secondExtraFee);
    }

    @Test
    void dailyMaxFeeTest() {
        FeePolicyVO policy = testPolicy();

        int oneDayFee = paymentService.calculateFeeLogic(entryTime, entryTime.plusDays(1), policy);
        int nextPeriodFee = paymentService.calculateFeeLogic(
                entryTime, entryTime.plusDays(1).plusMinutes(61), policy);

        log.info("--- 24시간 최대 요금: 24시간={}, 24시간 61분={} ---", oneDayFee, nextPeriodFee);
        assertEquals(15000, oneDayFee);
        assertEquals(18000, nextPeriodFee);
    }

    @Test
    void invalidParkingTimeTest() {
        FeePolicyVO policy = testPolicy();

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.calculateFeeLogic(null, entryTime, policy));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.calculateFeeLogic(entryTime, entryTime.minusMinutes(1), policy));

        log.info("--- 요금 계산 입력 검사: 입차 시각 누락과 역순 시각 거부 ---");
    }

    @Test
    void carTypeDiscountTest() {
        FeePolicyVO policy = testPolicy();

        int regularDiscount = paymentService.calculateDiscountLogic(3000, 1, policy);
        int monthlyDiscount = paymentService.calculateDiscountLogic(3000, 2, policy);
        int lightDiscount = paymentService.calculateDiscountLogic(3000, 3, policy);
        int disabledDiscount = paymentService.calculateDiscountLogic(3000, 4, policy);

        log.info("--- 차량 유형별 할인: 일반={}, 월정액={}, 경차={}, 장애인={} ---",
                regularDiscount, monthlyDiscount, lightDiscount, disabledDiscount);
        assertEquals(0, regularDiscount);
        assertEquals(3000, monthlyDiscount);
        assertEquals(900, lightDiscount);
        assertEquals(1500, disabledDiscount);
    }

    @Test
    void invalidDiscountRateTest() {
        FeePolicyVO invalidPolicy = FeePolicyVO.builder()
                .lightDiscount(-0.1)
                .disabledDiscount(1.1)
                .build();

        int lightDiscount = paymentService.calculateDiscountLogic(3000, 3, invalidPolicy);
        int disabledDiscount = paymentService.calculateDiscountLogic(3000, 4, invalidPolicy);

        log.info("--- 잘못된 정책 할인율 대체: 경차={}, 장애인={} ---", lightDiscount, disabledDiscount);
        assertEquals(900, lightDiscount);
        assertEquals(1500, disabledDiscount);
    }

    private FeePolicyVO testPolicy() {
        return FeePolicyVO.builder()
                .gracePeriod(10)
                .defaultTime(60)
                .defaultFee(2000)
                .extraTime(30)
                .extraFee(1000)
                .lightDiscount(0.3)
                .disabledDiscount(0.5)
                .maxDailyFee(15000)
                .build();
    }
}
