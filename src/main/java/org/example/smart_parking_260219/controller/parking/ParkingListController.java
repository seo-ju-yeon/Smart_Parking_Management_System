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

    // [버그수정] carNum만 setAttribute하고 DB 조회를 하지 않아 parkingDTO가 null → NPE(500)
    // DB에서 parkingDTO 조회 후 setAttribute로 전달하도록 수정
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("GET /get - 출차 차량 조회");
        String carNum = req.getParameter("carNum");
        // int carType = Integer.parseInt(req.getParameter("carType"));
        String spaceId = req.getParameter("id");

        // 차량번호가 공백일때
        if (carNum == null || carNum.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/output");
            return;
        }
        ParkingDTO parkingDTO = parkingService.getParkingByCarNum(carNum);

        // 주소창 내의 차량 번호 파라미터를 변경하는 부정적인 접근 차단
        if (parkingDTO == null) {
            resp.sendRedirect(req.getContextPath() + "/output?fail=false");
            return;
        }

        // 주소창 내에 주차구역 파라미터를 변경하는 부정적인 접근 차단
        if (spaceId == null || !spaceId.equals(parkingDTO.getSpaceId())) {
            resp.sendRedirect(req.getContextPath() + "/output?fail=nullId");
            return;
        }

        // ParkingDTO parkingDTO1 = ParkingDTO.builder().carNum(carNum).carType(carType).build();
        // parkingService.modifyParkingCarType(parkingDTO1);

        req.setAttribute("carNum", carNum);
        req.setAttribute("parkingDTO", parkingDTO);
        req.getRequestDispatcher("/WEB-INF/views/exit/exit_search_list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("POST /get - 정산 페이지 이동");
        String carNum = req.getParameter("carNum");
        req.setAttribute("carNum", carNum);
        String carType = req.getParameter("carType");
        log.info(carType);
        req.setAttribute("carType", carType);
        log.info("carType: {}",carType);



        req.getRequestDispatcher("/WEB-INF/views/payment/payment.jsp").forward(req, resp);
    }
}