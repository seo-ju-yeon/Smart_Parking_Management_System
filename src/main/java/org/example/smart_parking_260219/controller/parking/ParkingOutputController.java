package org.example.smart_parking_260219.controller.parking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.ParkingSpotDTO;
import org.example.smart_parking_260219.service.ParkingService;
import org.example.smart_parking_260219.service.ParkingSpotService;

import java.io.IOException;
import java.util.List;

@WebServlet("/output")
@Log4j2
public class ParkingOutputController extends HttpServlet {
    private final ParkingService parkingService = ParkingService.INSTANCE;
    private final ParkingSpotService parkingSpotService = ParkingSpotService.INSTANCE;

    // [버그수정] doGet: 대시보드에서 입차 차량 클릭 시 /output?id=A1&carNum=xxx 로 GET 요청이 옴
    // 기존에는 단순히 빈 exit.jsp만 보여줘서 차량 정보가 전달되지 않았음
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("GET /output");
        String carNum = req.getParameter("carNum");
        String spaceId = req.getParameter("id");
        String failInput = req.getParameter("fail");

        // 단순 출차 페이지 접근 (carNum 없음)
        if (carNum == null || carNum.isEmpty()) {
            req.getRequestDispatcher("/WEB-INF/views/exit/exit.jsp").forward(req, resp);
            return;
        }

        // 대시보드에서 입차 차량 클릭 → 차량 정보가 있으면 바로 정산 페이지로
        if (parkingService.getParkingByCarNum(carNum) != null) {
            req.setAttribute("id", spaceId);
            req.setAttribute("carNum", carNum);
            req.getRequestDispatcher("/WEB-INF/views/exit/exit_search_list.jsp").forward(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/output?fail=false");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        log.info("POST /output - 출차 차량 조회");
        String carNum = req.getParameter("carNum");
        // [버그수정] getAttribute → getParameter
        String spaceId = req.getParameter("id");

        List<ParkingSpotDTO> parkingSpotDTOList = parkingSpotService.getAllParkingSpot();
        req.setAttribute("dtoList", parkingSpotDTOList);

        if (parkingService.getParkingByCarNum(carNum) != null) {
            req.setAttribute("id", spaceId);
            req.setAttribute("carNum", carNum);
            req.getRequestDispatcher("/WEB-INF/views/exit/exit_search_list.jsp").forward(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/output?fail=false");
        }
    }
}