package org.example.smart_parking_260219.controller.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.FeePolicyDTO;
import org.example.smart_parking_260219.dto.ParkingDTO;
import org.example.smart_parking_260219.dto.ParkingSpotDTO;
import org.example.smart_parking_260219.dto.PaymentDTO;
import org.example.smart_parking_260219.service.FeePolicyService;
import org.example.smart_parking_260219.service.ParkingService;
import org.example.smart_parking_260219.service.ParkingSpotService;
import org.example.smart_parking_260219.service.PaymentService;
import org.example.smart_parking_260219.util.MapperUtil;
import org.example.smart_parking_260219.vo.FeePolicyVO;

import java.io.IOException;
import java.time.LocalDateTime;

@Log4j2
@WebServlet(name = "paymentController", value = "/payment/payment")

public class PaymentController extends HttpServlet {
    private final PaymentService paymentService = PaymentService.INSTANCE;
    private final ParkingService parkingService = ParkingService.INSTANCE;
    private final FeePolicyService feePolicyService = FeePolicyService.getInstance();
    private final ParkingSpotService parkingSpotService = ParkingSpotService.INSTANCE;

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {
        // 정산 화면은 출차 대상 검증을 거친 요청으로만 진입하도록 함
        resp.sendRedirect(req.getContextPath() + "/output");
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {
        log.info("/payment post start...");

        int parkingId;
        int carType;
        int paymentType;

        try {
            parkingId = Integer.parseInt(req.getParameter("parkingId"));
            carType = Integer.parseInt(req.getParameter("carType"));
            paymentType = Integer.parseInt(req.getParameter("paymentType"));
        } catch (NumberFormatException e) {
            log.warn("숫자 형식이 아닌 결제 요청값 수신");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "유효하지 않은 결제 요청입니다.");
            return;
        }

        // 정의되지 않은 차량 유형과 결제 수단은 상태 변경 전에 차단함
        if (parkingId <= 0
                || !isValidCarType(carType)
                || !isValidPaymentType(paymentType)
                || !isValidPaymentSelection(carType, paymentType)) {
            log.warn("허용 범위를 벗어난 결제 요청값 수신");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "유효하지 않은 결제 요청입니다.");
            return;
        }

        try {
            // 요청의 표시값을 신뢰하지 않고 주차 ID로 결제 대상을 다시 조회함
            ParkingDTO parkingDTO = parkingService.getByIdParking(parkingId);
            if (parkingDTO == null || parkingDTO.isPaid()) {
                log.warn("결제 처리 대상 주차 기록을 찾을 수 없습니다.");
                resp.sendRedirect(req.getContextPath() + "/dashboard");
                return;
            }
            String carNum = parkingDTO.getCarNum();

            FeePolicyDTO feePolicyDTO = feePolicyService.getPolicy();
            if (feePolicyDTO == null) {
                throw new IllegalStateException("활성화된 요금 정책이 없습니다.");
            }

            FeePolicyVO feePolicyVO = MapperUtil.INSTANCE.getInstance()
                    .map(feePolicyDTO, FeePolicyVO.class);

            // 서버에서 출차 시각을 확정하고 조회한 주차 기록과 정책으로 요금을 계산함
            LocalDateTime exitTime = LocalDateTime.now();
            int calculatedFee = paymentService.calculateFeeLogic(
                    parkingDTO.getEntryTime(),
                    exitTime,
                    feePolicyVO
            );
            int discountAmount = paymentService.calculateDiscountLogic(
                    calculatedFee,
                    carType,
                    feePolicyVO
            );
            int finalFee = calculatedFee - discountAmount;

            // 서버에서 계산한 금액으로 결제 정보를 생성함
            PaymentDTO paymentDTO = PaymentDTO.builder()
                    .carNum(carNum)
                    .carType(carType)
                    .parkingId(parkingId)
                    .policyId(feePolicyDTO.getPolicyId())
                    .paymentType(paymentType)
                    .calculatedFee(calculatedFee)
                    .discountAmount(discountAmount)
                    .finalFee(finalFee)
                    .totalTime(parkingDTO.getTotalTime())
                    .build();

            ParkingSpotDTO parkingSpotDTO = ParkingSpotDTO.builder()
                    .carNum(carNum).build();

            ParkingDTO parkingDTO1 = ParkingDTO.builder()
                    .carNum(carNum)
                    .carType(carType) // 화면에서 선택한 타입 반영
                    .build();

            paymentService.addPayment(paymentDTO);
            parkingService.modifyParking(parkingDTO1);
            parkingSpotService.modifyOutputParkingSpot(parkingSpotDTO);

            log.info("결제 및 출차 처리 완료 - 차량번호: {}", carNum);

            resp.sendRedirect(req.getContextPath() + "/dashboard");

        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생", e);
            throw new ServletException(e);
        }
    }

    // 차량 유형이 일반(1), 월정액(2), 경차(3), 장애인(4) 중 하나인지 확인함
    private boolean isValidCarType(int carType) {
        return carType >= 1 && carType <= 4;
    }

    // 결제 수단이 카드(1), 현금(2), 월정액(3) 중 하나인지 확인함
    private boolean isValidPaymentType(int paymentType) {
        return paymentType >= 1 && paymentType <= 3;
    }

    // 월정액 차량은 월정액 결제만, 그 외 차량은 카드 또는 현금 결제만 허용함
    private boolean isValidPaymentSelection(int carType, int paymentType) {
        if (carType == 2) {
            return paymentType == 3;
        }
        return paymentType == 1 || paymentType == 2;
    }
}
