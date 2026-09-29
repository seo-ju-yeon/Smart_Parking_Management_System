package org.example.smart_parking_260219.controller.parking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.FeePolicyDTO;
import org.example.smart_parking_260219.dto.ParkingDTO;
import org.example.smart_parking_260219.service.FeePolicyService;
import org.example.smart_parking_260219.service.ParkingService;
import org.example.smart_parking_260219.service.PaymentService;
import org.example.smart_parking_260219.util.MapperUtil;
import org.example.smart_parking_260219.vo.FeePolicyVO;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Log4j2
@WebServlet("/get")
public class ParkingListController extends HttpServlet {

    private final ParkingService parkingService = ParkingService.INSTANCE;
    private final PaymentService paymentService = PaymentService.INSTANCE;
    private final FeePolicyService feePolicyService = FeePolicyService.getInstance();

    // 차량번호와 주차구역이 실제 미정산 주차 기록과 일치하는지 확인함
    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {
        log.info("GET /get - 출차 차량 조회");
        String carNum = req.getParameter("carNum");
        String spaceId = req.getParameter("id");

        // 차량번호가 없으면 출차 조회 화면으로 돌아감
        if (carNum == null || carNum.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/output");
            return;
        }
        ParkingDTO parkingDTO = parkingService.getParkingByCarNum(carNum);

        // 요청한 차량번호에 해당하는 미정산 주차 기록이 없으면 접근을 중단함
        if (parkingDTO == null) {
            resp.sendRedirect(req.getContextPath() + "/output?fail=false");
            return;
        }

        // 요청한 주차구역과 DB의 실제 주차구역이 다르면 접근을 중단함
        if (spaceId == null || !spaceId.equals(parkingDTO.getSpaceId())) {
            resp.sendRedirect(req.getContextPath() + "/output?fail=nullId");
            return;
        }

        req.setAttribute("carNum", carNum);
        req.setAttribute("parkingDTO", parkingDTO);
        req.getRequestDispatcher("/WEB-INF/views/exit/exit_search_list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {
        log.info("POST /get - 정산 페이지 이동");

        // 화면에 표시된 값이 아니라 주차 기록 ID로 정산 대상을 다시 조회함
        int parkingId;
        try {
            parkingId = Integer.parseInt(req.getParameter("parkingId"));
        } catch (NumberFormatException e) {
            log.warn("숫자 형식이 아닌 정산 요청값 수신");
            resp.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "유효하지 않은 정산 요청입니다."
            );
            return;
        }

        if (parkingId <= 0) {
            log.warn("허용 범위를 벗어난 정산 요청값 수신");
            resp.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "유효하지 않은 정산 요청입니다."
            );
            return;
        }

        ParkingDTO parkingDTO = parkingService.getByIdParking(parkingId);
        if (parkingDTO == null || parkingDTO.isPaid()) {
            log.warn("정산 가능한 주차 기록을 찾을 수 없습니다. parkingId={}", parkingId);
            resp.sendRedirect(req.getContextPath() + "/output");
            return;
        }

        // 예상 할인 계산에도 입차 시 DB에 저장된 차량 유형을 사용함
        int carType = parkingDTO.getCarType();
        if (!isValidCarType(carType)) {
            throw new ServletException(
                    "주차 기록의 차량 유형이 유효하지 않습니다."
            );
        }

        FeePolicyDTO feePolicyDTO = feePolicyService.getPolicy();
        if (feePolicyDTO == null) {
            throw new ServletException(
                    "활성화된 요금 정책이 없습니다."
            );
        }

        FeePolicyVO feePolicyVO = MapperUtil.INSTANCE.getInstance()
                .map(feePolicyDTO, FeePolicyVO.class);

        // 예상 요금과 주차시간이 같은 시각을 기준으로 계산되도록 한 번만 생성함
        LocalDateTime previewExitTime = LocalDateTime.now();
        int calculatedFee = paymentService.calculateFeeLogic(
                parkingDTO.getEntryTime(),
                previewExitTime,
                feePolicyVO
        );
        int discountAmount = paymentService.calculateDiscountLogic(
                calculatedFee,
                carType,
                feePolicyVO
        );
        int finalFee = calculatedFee - discountAmount;
        long totalTime = Duration.between(
                parkingDTO.getEntryTime(),
                previewExitTime
        ).toMinutes();

        // 정산 화면은 서버가 계산한 예상 금액과 주차시간만 표시함
        req.setAttribute("parkingDTO", parkingDTO);
        // payment.jsp 전환이 끝날 때까지 DB에서 조회한 차량 유형을 전달함
        req.setAttribute("carType", carType);
        req.setAttribute("calculatedFee", calculatedFee);
        req.setAttribute("discountAmount", discountAmount);
        req.setAttribute("finalFee", finalFee);
        req.setAttribute("totalTime", totalTime);
        req.setAttribute("previewExitTime", previewExitTime);

        req.getRequestDispatcher("/WEB-INF/views/payment/payment.jsp").forward(req, resp);
    }

    // 차량 유형이 일반(1), 월정액(2), 경차(3), 장애인(4) 중 하나인지 확인함
    private boolean isValidCarType(int carType) {
        return carType >= 1 && carType <= 4;
    }
}
