package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dao.ParkingDAO;
import org.example.smart_parking_260219.dao.ParkingDAOImpl;
import org.example.smart_parking_260219.dao.PaymentDAO;
import org.example.smart_parking_260219.dto.PaymentDTO;
import org.example.smart_parking_260219.util.MapperUtil;
import org.example.smart_parking_260219.vo.FeePolicyVO;
import org.example.smart_parking_260219.vo.ParkingVO;
import org.example.smart_parking_260219.vo.PaymentVO;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public enum PaymentService {
    INSTANCE;

    private final PaymentDAO paymentDAO;
    private final ModelMapper modelMapper;
    private final ParkingDAO parkingDAO = new ParkingDAOImpl();

    private PaymentService() {
        paymentDAO = PaymentDAO.getInstance();
        modelMapper = MapperUtil.INSTANCE.getInstance();
    }

    // 결제 등록
    public void addPayment(PaymentDTO paymentDTO) throws Exception {
        // 결제 대상의 존재 여부와 정산 상태를 DB에서 다시 확인함
        ParkingVO parkingVO = parkingDAO.selectParkingByParkingId(paymentDTO.getParkingId());

        if (parkingVO == null || parkingVO.isPaid()) {
            throw new Exception("정산 가능한 주차 기록이 아닙니다.");
        }

        PaymentVO paymentVO = PaymentVO.builder()
                .parkingId(parkingVO.getParkingId())
                .policyId(paymentDTO.getPolicyId())
                .carNum(parkingVO.getCarNum())
                .paymentType(paymentDTO.getPaymentType())
                .calculatedFee(paymentDTO.getCalculatedFee())
                .discountAmount(paymentDTO.getDiscountAmount())
                .finalFee(paymentDTO.getFinalFee())
                .build();

        paymentDAO.insertPayment(paymentVO);
    }

    // 결제 날짜별 목록 조회
    public List<PaymentDTO> getPaymentList(String targetDate) {
        log.info("Service: getPaymentList 호출 - 날짜: " + targetDate);

        List<PaymentVO> paymentVOList = paymentDAO.selectPaymentByDate(targetDate);

        if (paymentVOList == null || paymentVOList.isEmpty()) {
            log.info("해당 날짜에 결제 내역이 없습니다.");
            return Collections.emptyList();
        }

        return paymentVOList.stream()
                .map(vo -> modelMapper.map(vo, PaymentDTO.class))
                .collect(Collectors.toList());
    }

    // 조회가 끝난 주차 시간과 요금 정책을 사용하여 24시간 단위 요금을 계산함
    public int calculateFeeLogic(
            LocalDateTime entryTime,
            LocalDateTime exitTime,
            FeePolicyVO feePolicyVO
    ) {
        if (entryTime == null
                || exitTime == null
                || feePolicyVO == null) {
            throw new IllegalArgumentException(
                    "요금 계산에 필요한 정보가 없습니다."
            );
        }

        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException(
                    "출차 시각은 입차 시각보다 빠를 수 없습니다."
            );
        }

        int totalAccumulatedFee = 0;
        LocalDateTime currentStart = entryTime;

        while (currentStart.plusDays(1).isBefore(exitTime)) {
            LocalDateTime endOfCycle = currentStart.plusDays(1);

            totalAccumulatedFee += calculateSingleDayFee(
                    currentStart,
                    endOfCycle,
                    feePolicyVO
            );

            currentStart = endOfCycle;
        }

        totalAccumulatedFee += calculateSingleDayFee(
                currentStart,
                exitTime,
                feePolicyVO
        );

        log.info(
                "24시간 단위 주차 요금 계산 완료 - 계산 금액: {}",
                totalAccumulatedFee
        );

        return totalAccumulatedFee;
    }

    // 할인 금액 계산
    // [버그수정] DB의 lightDiscount/disabledDiscount 값이 잘못 저장되어 있어도
    //           안전하게 동작하도록 비율을 0.0~1.0 범위로 강제 보정 후 계산
    public int calculateDiscountLogic(int totalFee, int carType, FeePolicyVO policyVO) {
        double discountRate = 0.0;

        if (carType == 2) {
            // 월정액: 100% 무료
            discountRate = 1.0;
        } else if (carType == 3) {
            // 경차: DB 정책값 사용, 단 0~1 범위를 벗어나면 기본값 0.3 적용
            double raw = policyVO.getLightDiscount();
            discountRate = (raw >= 0.0 && raw <= 1.0) ? raw : 0.3;
            log.info("경차 할인율 적용: {}", discountRate);
        } else if (carType == 4) {
            // 장애인: DB 정책값 사용, 단 0~1 범위를 벗어나면 기본값 0.5 적용
            double raw = policyVO.getDisabledDiscount();
            discountRate = (raw >= 0.0 && raw <= 1.0) ? raw : 0.5;
            log.info("장애인 할인율 적용: {}", discountRate);
        }
        // carType == 1 (일반): discountRate = 0.0 → 할인 없음

        int discountAmount = (int) (totalFee * discountRate);
        log.info("carType={}, totalFee={}, discountRate={}, discountAmount={}", carType, totalFee, discountRate, discountAmount);
        return discountAmount;
    }

    // 내부 메서드: 지정된 시간 구간(start ~ end)에 대한 요금 계산
    private int calculateSingleDayFee(LocalDateTime start, LocalDateTime end, FeePolicyVO policyVO) {
        long minutes = java.time.Duration.between(start, end).toMinutes();

        // 무료 구간 이내
        if (minutes <= policyVO.getGracePeriod()) return 0;

        // 기본 요금
        int fee = policyVO.getDefaultFee();

        // 추가 요금 (기본 시간 초과 시)
        if (minutes > policyVO.getDefaultTime()) {
            long extraMinutes = minutes - policyVO.getDefaultTime();
            int units = (int) Math.ceil((double) extraMinutes / policyVO.getExtraTime());
            fee += (units * policyVO.getExtraFee());
        }

        // 일일 최대 요금 제한
        return Math.min(fee, policyVO.getMaxDailyFee());
    }
}
