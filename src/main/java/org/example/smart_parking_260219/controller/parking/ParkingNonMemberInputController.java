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
        log.info("/WEB-INF/views/entry/add_non_member.jsp");

        req.getRequestDispatcher("/WEB-INF/views/entry/add_non_member.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        log.info("/parking/non_member_entry post...");
        String carNum = req.getParameter("carNum");
        String spaceId = req.getParameter("id");
        String carType = req.getParameter("finish");
        String phone = req.getParameter("phone");

        // 주차장 현황 갱신
        ParkingSpotDTO parkingSpotDTO = ParkingSpotDTO.builder()
                .carNum(carNum)
                .spaceId(spaceId)
                .build();
        parkingSpotService.modifyInputParkingSpot(parkingSpotDTO);

        // 주차 기록 추가
        ParkingDTO parkingDTO = ParkingDTO.builder()
                .memberId(0)
                .carNum(carNum)
                .spaceId(spaceId)
                .phone(phone)
                .carType(Integer.parseInt(carType))
                .build();
        parkingService.addParking(parkingDTO);

        // 비회원 입차 정상 처리
        req.getRequestDispatcher("/WEB-INF/views/dashboard/dashboard.jsp").forward(req, resp);
    }
}
