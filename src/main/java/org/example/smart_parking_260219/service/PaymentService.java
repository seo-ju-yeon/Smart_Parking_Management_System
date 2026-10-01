package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.dao.FeePolicyDAO;
import org.example.smart_parking_260219.dao.ParkingDAO;
import org.example.smart_parking_260219.dao.ParkingDAOImpl;
import org.example.smart_parking_260219.dao.ParkingSpotDAO;
import org.example.smart_parking_260219.dao.ParkingSpotDAOImpl;
import org.example.smart_parking_260219.dao.PaymentDAO;
import org.example.smart_parking_260219.dto.PaymentDTO;
import org.example.smart_parking_260219.util.MapperUtil;
import org.example.smart_parking_260219.vo.FeePolicyVO;
import org.example.smart_parking_260219.vo.ParkingVO;
import org.example.smart_parking_260219.vo.PaymentVO;
import org.modelmapper.ModelMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
public enum PaymentService {
    INSTANCE;

    private final PaymentDAO paymentDAO;
    private final FeePolicyDAO feePolicyDAO;
    private final ParkingDAO parkingDAO;
    private final ParkingSpotDAO parkingSpotDAO;
    private final ModelMapper modelMapper;

    private PaymentService() {
        paymentDAO = PaymentDAO.getInstance();
        feePolicyDAO = FeePolicyDAO.getInstance();
        parkingDAO = new ParkingDAOImpl();
        parkingSpotDAO = new ParkingSpotDAOImpl();
        modelMapper = MapperUtil.INSTANCE.getInstance();
    }

    // 결제 저장, 주차 기록 갱신, 주차 공간 반환을 하나의 트랜잭션으로 처리함
    public PaymentDTO completePaymentAndExit(
            int parkingId,
            int paymentType
    ) {
        try {
            Connection connection = DBConnection.INSTANCE.getConnection();
            boolean originalAutoCommit = true;
            boolean transactionStarted = false;
            // 업무 성공 여부가 아니라 커밋·롤백으로 트랜잭션이 끝났는지를 기록함
            boolean transactionCompleted = false;

            try {
                originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                transactionStarted = true;

                PaymentDTO paymentDTO = processPaymentAndExit(
                        connection,
                        parkingId,
                        paymentType
                );

                // 세 상태 변경이 모두 성공한 경우에만 DB에 최종 반영함
                connection.commit();
                transactionCompleted = true;

                return paymentDTO;
            } catch (Exception e) {
                if (transactionStarted) {
                    transactionCompleted = rollback(connection, e);
                }

                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException(
                        "결제 및 출차 처리에 실패했습니다.",
                        e
                );
            } finally {
                // 정상 연결은 close()로 반환하고, 실패 연결은 제거만 하여 중복 정리를 피함
                finishTransaction(
                        connection,
                        originalAutoCommit,
                        transactionCompleted,
                        DBConnection.INSTANCE::evictConnection
                );
            }
        } catch (SQLException e) {
            log.error("결제 트랜잭션 연결 처리 중 오류 발생", e);
            throw new IllegalStateException(
                    "결제 및 출차 처리에 실패했습니다.",
                    e
            );
        }
    }

    // 잠금 조회한 DB 값을 기준으로 계산하고 모든 상태 변경 결과를 확인함
    private PaymentDTO processPaymentAndExit(
            Connection connection,
            int parkingId,
            int paymentType
    ) throws SQLException {
        ParkingVO parkingVO =
                parkingDAO.selectParkingByParkingIdForUpdate(
                        connection,
                        parkingId
                );

        if (parkingVO == null || parkingVO.isPaid()) {
            throw new IllegalStateException(
                    "정산 가능한 주차 기록이 아닙니다."
            );
        }

        int carType = parkingVO.getCarType();
        if (!isValidCarType(carType)) {
            throw new IllegalStateException(
                    "주차 기록의 차량 유형이 유효하지 않습니다."
            );
        }

        if (!isValidPaymentType(paymentType)
                || !isValidPaymentSelection(carType, paymentType)) {
            throw new IllegalArgumentException(
                    "선택할 수 없는 결제 수단입니다."
            );
        }

        FeePolicyVO feePolicyVO =
                feePolicyDAO.selectActivePolicy(connection);
        if (feePolicyVO == null) {
            throw new IllegalStateException(
                    "활성화된 요금 정책이 없습니다."
            );
        }

        // 한 번 확정한 출차 시각을 주차시간과 요금 계산에 함께 사용함
        LocalDateTime exitTime = LocalDateTime.now();
        int totalMinutes = Math.toIntExact(
                Duration.between(
                        parkingVO.getEntryTime(),
                        exitTime
                ).toMinutes()
        );
        int calculatedFee = calculateFeeLogic(
                parkingVO.getEntryTime(),
                exitTime,
                feePolicyVO
        );
        int discountAmount = calculateDiscountLogic(
                calculatedFee,
                carType,
                feePolicyVO
        );
        int finalFee = calculatedFee - discountAmount;

        PaymentVO paymentVO = PaymentVO.builder()
                .parkingId(parkingVO.getParkingId())
                .policyId(feePolicyVO.getPolicyId())
                .carNum(parkingVO.getCarNum())
                .carType(carType)
                .paymentType(paymentType)
                .calculatedFee(calculatedFee)
                .discountAmount(discountAmount)
                .finalFee(finalFee)
                .totalTime(totalMinutes)
                .build();

        int insertedPaymentRows = paymentDAO.insertPayment(connection, paymentVO);
        requireSingleChangedRow(
                insertedPaymentRows,
                "결제 정보가 정상적으로 저장되지 않았습니다."
        );

        int updatedParkingRows =
                parkingDAO.updateParkingForPayment(
                        connection,
                        parkingVO.getParkingId(),
                        carType,
                        exitTime,
                        totalMinutes
                );
        requireSingleChangedRow(
                updatedParkingRows,
                "주차 기록이 정상적으로 갱신되지 않았습니다."
        );

        int updatedParkingSpotRows =
                parkingSpotDAO.updateParkingSpotForExit(
                        connection,
                        parkingVO.getSpaceId(),
                        parkingVO.getCarNum()
                );
        requireSingleChangedRow(
                updatedParkingSpotRows,
                "주차 공간이 정상적으로 반환되지 않았습니다."
        );

        return PaymentDTO.builder()
                .parkingId(parkingVO.getParkingId())
                .policyId(feePolicyVO.getPolicyId())
                .carNum(parkingVO.getCarNum())
                .carType(carType)
                .paymentType(paymentType)
                .calculatedFee(calculatedFee)
                .discountAmount(discountAmount)
                .finalFee(finalFee)
                .totalTime(totalMinutes)
                .build();
    }

    // 결제 날짜별 목록 조회
    public List<PaymentDTO> getPaymentList(String targetDate) {
        List<PaymentVO> paymentVOList = paymentDAO.selectPaymentByDate(targetDate);

        if (paymentVOList == null || paymentVOList.isEmpty()) {
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

    // 정책 할인율이 유효 범위를 벗어나면 차량 유형별 기본 할인율을 사용함
    public int calculateDiscountLogic(int totalFee, int carType, FeePolicyVO policyVO) {
        double discountRate = 0.0;

        if (carType == 2) {
            // 월정액 차량은 전액 할인함
            discountRate = 1.0;
        } else if (carType == 3) {
            // 유효한 경차 할인율이 없으면 기본 할인율 30%를 사용함
            double raw = policyVO.getLightDiscount();
            discountRate = (raw >= 0.0 && raw <= 1.0) ? raw : 0.3;
        } else if (carType == 4) {
            // 유효한 장애인 할인율이 없으면 기본 할인율 50%를 사용함
            double raw = policyVO.getDisabledDiscount();
            discountRate = (raw >= 0.0 && raw <= 1.0) ? raw : 0.5;
        }

        int discountAmount = (int) (totalFee * discountRate);
        return discountAmount;
    }

    // 지정된 24시간 이내 구간의 요금을 계산함
    private int calculateSingleDayFee(LocalDateTime start, LocalDateTime end, FeePolicyVO policyVO) {
        long minutes = Duration.between(start, end).toMinutes();

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

    // 각 상태 변경은 정확히 한 행에 적용되어야 정상 처리로 판단함
    private void requireSingleChangedRow(
            int changedRows,
            String message
    ) {
        if (changedRows != 1) {
            throw new IllegalStateException(message);
        }
    }

    // 롤백 성공 여부를 반환하며, 실패 원인은 최초 처리 예외에 함께 보관함
    static boolean rollback(
            Connection connection,
            Exception cause
    ) {
        try {
            connection.rollback();
            return true;
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            log.error("결제 트랜잭션 롤백 중 오류 발생", rollbackException);
            return false;
        }
    }

    // 커밋 또는 롤백이 끝난 연결만 설정을 복원하고, 실패 연결은 풀에서 제거함
    // 제거 동작을 전달받아 실제 DB 없이도 예외 처리 순서를 검증할 수 있게 함
    static void finishTransaction(
            Connection connection,
            boolean originalAutoCommit,
            boolean transactionCompleted,
            Consumer<Connection> evictConnection
    ) {
        if (!transactionCompleted) {
            // 시작 또는 롤백 실패 후에는 남은 변경 여부를 확신할 수 없어 설정을 복원하지 않음
            evictConnection.accept(connection);
            return;
        }

        try {
            connection.setAutoCommit(originalAutoCommit);
            connection.close();
        } catch (SQLException e) {
            log.warn("결제 트랜잭션 Connection 설정 복원 또는 반환 실패", e);
            // 설정 복원이나 반환에 실패한 연결도 다음 요청에서 재사용하지 않도록 제거함
            evictConnection.accept(connection);
        }
    }

    // 차량 유형이 일반, 월정액, 경차, 장애인 중 하나인지 확인함
    private boolean isValidCarType(int carType) {
        return carType >= 1 && carType <= 4;
    }

    // 결제 수단이 카드, 현금, 월정액 중 하나인지 확인함
    private boolean isValidPaymentType(int paymentType) {
        return paymentType >= 1 && paymentType <= 3;
    }

    // 월정액 차량과 일반 결제 차량에 허용된 결제 수단을 구분함
    private boolean isValidPaymentSelection(
            int carType,
            int paymentType
    ) {
        if (carType == 2) {
            return paymentType == 3;
        }
        return paymentType == 1 || paymentType == 2;
    }
}
