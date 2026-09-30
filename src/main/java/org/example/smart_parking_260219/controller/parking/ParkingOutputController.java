package org.example.smart_parking_260219.controller.parking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.smart_parking_260219.dto.ParkingDTO;
import org.example.smart_parking_260219.service.ParkingService;

import java.io.IOException;

@WebServlet("/output")
public class ParkingOutputController extends HttpServlet {
    private final ParkingService parkingService = ParkingService.INSTANCE;

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {
        String carNum = req.getParameter("carNum");

        // 차량번호가 없는 GET 요청은 출차 차량 검색 화면을 표시함
        if (carNum == null || carNum.isEmpty()) {
            req.getRequestDispatcher("/WEB-INF/views/exit/exit.jsp")
                    .forward(req, resp);
            return;
        }

        forwardParkingDetails(req, resp, carNum);
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException, ServletException {
        String carNum = req.getParameter("carNum");

        forwardParkingDetails(req, resp, carNum);
    }

    // 차량번호로 조회한 DB 주차 기록 전체를 출차 확인 화면에 전달함
    private void forwardParkingDetails(
            HttpServletRequest req,
            HttpServletResponse resp,
            String carNum
    ) throws ServletException, IOException {
        ParkingDTO parkingDTO =
                parkingService.getParkingByCarNum(carNum);

        if (parkingDTO == null) {
            resp.sendRedirect(req.getContextPath() + "/output?fail=false");
            return;
        }

        req.setAttribute("parkingDTO", parkingDTO);
        req.getRequestDispatcher(
                "/WEB-INF/views/exit/exit_search_list.jsp"
        ).forward(req, resp);
    }
}
