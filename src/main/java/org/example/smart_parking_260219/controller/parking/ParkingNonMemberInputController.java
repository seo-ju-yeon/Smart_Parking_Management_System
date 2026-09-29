package org.example.smart_parking_260219.controller.parking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.ParkingDTO;
import org.example.smart_parking_260219.dto.ParkingSpotDTO;
import org.example.smart_parking_260219.service.ParkingService;
import org.example.smart_parking_260219.service.ParkingSpotService;

import java.io.IOException;

@WebServlet("/nonMember")
@Log4j2
public class ParkingNonMemberInputController extends HttpServlet {
    private final ParkingService parkingService = ParkingService.INSTANCE;
    private final ParkingSpotService parkingSpotService = ParkingSpotService.INSTANCE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("GET /nonMember - 비회원 입차 화면 이동");

        req.getRequestDispatcher("/WEB-INF/views/entry/add_non_member.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        log.info("POST /nonMember - 비회원 입차 처리");

        String carNum = req.getParameter("carNum");
        String spaceId = req.getParameter("id");
        String phone = req.getParameter("phone");

        if (carNum == null || carNum.isBlank() || carNum.length() > 8
                || spaceId == null || spaceId.isBlank() || spaceId.length() > 4) {
            resp.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "유효하지 않은 입차 요청입니다."
            );
            return;
        }

        int carType;
        try {
            carType = Integer.parseInt(req.getParameter("carType"));
        } catch (NumberFormatException e) {
            resp.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "유효하지 않은 차량 유형입니다."
            );
            return;
        }

        // 비회원은 월정액을 제외한 차량 유형만 선택할 수 있음
        if (!isValidNonMemberCarType(carType)) {
            resp.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "선택할 수 없는 차량 유형입니다."
            );
            return;
        }

        // 화면을 표시한 뒤 공간 상태가 바뀌었을 수 있으므로 POST에서 다시 확인함
        ParkingSpotDTO currentParkingSpot =
                parkingSpotService.getParkingSpotBySpaceId(spaceId);

        if (currentParkingSpot == null
                || !Boolean.TRUE.equals(currentParkingSpot.getEmpty())) {
            resp.sendError(
                    HttpServletResponse.SC_CONFLICT,
                    "현재 사용할 수 없는 주차 공간입니다."
            );
            return;
        }

        ParkingDTO existingParking =
                parkingService.getParkingByCarNum(carNum);

        if (existingParking != null
                && !existingParking.isPaid()) {
            resp.sendError(
                    HttpServletResponse.SC_CONFLICT,
                    "이미 입차된 차량입니다."
            );
            return;
        }

        // 검증이 끝난 차량과 공간을 사용 중 상태로 변경함
        ParkingSpotDTO parkingSpotDTO = ParkingSpotDTO.builder()
                .carNum(carNum)
                .spaceId(spaceId)
                .build();
        parkingSpotService.modifyInputParkingSpot(parkingSpotDTO);

        // 사용자가 선택한 차량 유형을 신규 입차 기록에 저장함
        ParkingDTO parkingDTO = ParkingDTO.builder()
                .memberId(0)
                .carNum(carNum)
                .spaceId(spaceId)
                .phone(phone)
                .carType(carType)
                .build();
        parkingService.addParking(parkingDTO);

        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }

    // 비회원에게 허용되는 일반, 경차, 장애인 유형인지 확인함
    private boolean isValidNonMemberCarType(int carType) {
        return carType == 1 || carType == 3 || carType == 4;
    }
}
