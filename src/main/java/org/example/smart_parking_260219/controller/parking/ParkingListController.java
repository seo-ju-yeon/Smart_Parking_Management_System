package org.example.smart_parking_260219.controller.parking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.ParkingDTO;
import org.example.smart_parking_260219.service.ParkingService;

import java.io.IOException;

@Log4j2
@WebServlet("/get")
public class  ParkingListController extends HttpServlet {

    private final ParkingService parkingService = ParkingService.INSTANCE;

    // 차량번호와 주차구역이 실제 미정산 주차 기록과 일치하는지 확인함
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("POST /get - 정산 페이지 이동");

        // 화면의 차량번호가 아니라 주차 기록 ID로 결제 대상을 다시 조회함
        String parkingIdParam = req.getParameter("parkingId");
        int parkingId;
        try {
            parkingId = Integer.parseInt(parkingIdParam);
        } catch (NumberFormatException e) {
            log.warn("유효하지 않은 주차 ID로 정산 페이지 이동 요청");
            resp.sendRedirect(req.getContextPath() + "/output");
            return;
        }

        ParkingDTO parkingDTO = parkingService.getByIdParking(parkingId);
        if (parkingDTO == null || parkingDTO.isPaid()) {
            log.warn("정산 가능한 주차 기록을 찾을 수 없습니다. parkingId={}", parkingId);
            resp.sendRedirect(req.getContextPath() + "/output");
            return;
        }

        String carType = req.getParameter("carType");
        req.setAttribute("parkingDTO", parkingDTO);
        req.setAttribute("carType", carType);

        req.getRequestDispatcher("/WEB-INF/views/payment/payment.jsp").forward(req, resp);
    }
}
