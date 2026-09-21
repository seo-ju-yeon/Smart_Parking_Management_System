package org.example.smart_parking_260219.controller.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.ParkingDTO;
import org.example.smart_parking_260219.dto.ParkingSpotDTO;
import org.example.smart_parking_260219.dto.PaymentDTO;
import org.example.smart_parking_260219.service.FeePolicyService;
import org.example.smart_parking_260219.service.ParkingService;
import org.example.smart_parking_260219.service.ParkingSpotService;
import org.example.smart_parking_260219.service.PaymentService;

import java.io.IOException;

@Log4j2
@WebServlet(name = "paymentController", value = "/payment/payment")

public class PaymentController extends HttpServlet {
    private final PaymentService paymentService = PaymentService.INSTANCE;
    private final ParkingService parkingService = ParkingService.INSTANCE;
    private final FeePolicyService feePolicyService = FeePolicyService.getInstance();
    private final ParkingSpotService parkingSpotService = ParkingSpotService.INSTANCE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("/payment get...");

        // /WEB-INF -> jsp 파일 위치 옮긴 다음 추가
        req.getRequestDispatcher("/WEB-INF/views/payment/payment.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("/payment post start...");

        try {
            String carNum = req.getParameter("carNum");
            int carType = Integer.parseInt(req.getParameter("carType"));
            int paymentType = Integer.parseInt(req.getParameter("paymentType"));
            int calculatedFee = Integer.parseInt(req.getParameter("calculatedFee"));
            int discountAmount = Integer.parseInt(req.getParameter("discountAmount"));
            int finalFee = Integer.parseInt(req.getParameter("finalFee"));

            // [중요] 상태 변경 전에 미리 ID를 확보해야 합니다.
            var parkingDTO = parkingService.getParkingByCarNum(carNum);
            if (parkingDTO == null) {
                log.warn("결제 처리 대상 주차 기록을 찾을 수 없습니다.");
                resp.sendRedirect(req.getContextPath() + "/dashboard");
                return;
            }

            // 결제 정보 저장
            PaymentDTO paymentDTO = PaymentDTO.builder()
                    .carNum(carNum)
                    .carType(carType)
                    .parkingId(parkingDTO.getParkingId())
                    .policyId(feePolicyService.getPolicy().getPolicyId())
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



            // 순서 주의: 결제 내역을 먼저, 주차 상태 변경.
            paymentService.addPayment(paymentDTO);
            parkingService.modifyParking(parkingDTO1);
            parkingSpotService.modifyOutputParkingSpot(parkingSpotDTO);

            log.info("결제 및 출차 처리 완료 - 차량번호: {}", carNum);

            // 이동할 때 ContextPath를 포함한 올바른 URL로 이동
            resp.sendRedirect(req.getContextPath() + "/dashboard"); // 대시보드 URL로 수정

        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생", e);
            throw new ServletException(e);
        }
    }
}
