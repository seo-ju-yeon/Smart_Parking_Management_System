package org.example.smart_parking_260219.controller.parking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.MemberDTO;
import org.example.smart_parking_260219.dto.ParkingDTO;
import org.example.smart_parking_260219.dto.ParkingSpotDTO;
import org.example.smart_parking_260219.service.MemberService;
import org.example.smart_parking_260219.service.ParkingService;
import org.example.smart_parking_260219.service.ParkingSpotService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "parkingInputController", value = "/input")
@Log4j2
public class ParkingInputController extends HttpServlet {
    private final ParkingService parkingService = ParkingService.INSTANCE;
    private final ParkingSpotService parkingSpotService = ParkingSpotService.INSTANCE;
    private final MemberService memberService = MemberService.INSTANCE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("GET /input - 입차 페이지 이동");
        String spaceId = req.getParameter("id");
        String carNum = req.getParameter("carNum");

        req.setAttribute("id", spaceId);
        req.setAttribute("carNum", carNum);

        req.getRequestDispatcher("/WEB-INF/views/entry/entry.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        log.info("POST /input - 입차 처리");

        String carNum = req.getParameter("carNum");
        String spaceId = req.getParameter("spaceId");

        if (carNum == null || carNum.isEmpty() || carNum.length() > 8) {
            req.setAttribute("id", req.getParameter("spaceId"));
            req.setAttribute("fail", "over");
            req.getRequestDispatcher("/WEB-INF/views/entry/entry.jsp").forward(req, resp);
            return;
        }
        if (spaceId == null || spaceId.isEmpty() || spaceId.length() > 4) {
            req.setAttribute("id", req.getParameter("spaceId"));
            req.setAttribute("fail", "nullId");
            req.getRequestDispatcher("/WEB-INF/views/entry/entry.jsp").forward(req, resp);
            return;
        }

        ParkingSpotDTO spotDTO = parkingSpotService.getParkingSpotBySpaceId(spaceId);

        if (spotDTO == null) {
            req.setAttribute("id", spaceId);
            req.setAttribute("fail", "nullId");
            req.getRequestDispatcher("/WEB-INF/views/entry/entry.jsp").forward(req, resp);
            return;
        }

        if (!spotDTO.getEmpty()) {
            req.setAttribute("id", spaceId);
            req.setAttribute("fail", "false");
            req.getRequestDispatcher("/WEB-INF/views/entry/entry.jsp").forward(req, resp);
            return;
        }
        ParkingDTO existingParking = parkingService.getParkingByCarNum(carNum);
        if (existingParking != null && !existingParking.isPaid()) {
            req.setAttribute("id", spaceId);
            req.setAttribute("fail", "already");
            req.getRequestDispatcher("/WEB-INF/views/entry/entry.jsp").forward(req, resp);
            return;
        }

        MemberDTO memberDTO;

        try {
            memberDTO = memberService.getOneMember(carNum);
        } catch (SQLException e) {
            throw new ServletException(
                    "회원 정보 조회 중 오류가 발생했습니다.",
                    e
            );
        }

        // 등록되지 않은 차량은 비회원 입차 화면에서 차량 유형을 입력받음
        if (memberDTO == null) {
            req.setAttribute("id", spaceId);
            req.setAttribute("carNum", carNum);

            req.getRequestDispatcher(
                    "/WEB-INF/views/entry/add_non_member.jsp"
            ).forward(req, resp);
            return;
        }

        // 월정액 회원은 월정액 유형을, 그 외 회원은 등록된 차량 유형을 사용함
        int carType = memberDTO.isSubscribed()
                ? 2
                : memberDTO.getCarType();

        if (!isValidCarType(carType)
                || (!memberDTO.isSubscribed() && carType == 2)) {
            throw new ServletException(
                    "회원의 월정액 상태와 차량 유형이 유효하지 않습니다."
            );
        }

        // 회원 정보에서 확정한 차량 유형을 입차 기록과 함께 저장함
        ParkingSpotDTO parkingSpotDTO = ParkingSpotDTO.builder()
                .carNum(carNum)
                .spaceId(spaceId)
                .build();
        parkingSpotService.modifyInputParkingSpot(parkingSpotDTO);

        ParkingDTO parkingDTO = ParkingDTO.builder()
                .memberId(memberDTO.getMemberId())
                .carNum(carNum)
                .spaceId(spaceId)
                .carType(carType)
                .build();
        parkingService.addParking(parkingDTO);

        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }

    // 입차 기록에 저장할 차량 유형이 정의된 범위인지 확인함
    private boolean isValidCarType(int carType) {
        return carType >= 1 && carType <= 4;
    }
}
