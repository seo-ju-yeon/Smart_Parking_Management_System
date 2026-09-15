package org.example.smart_parking_260219.controller.data;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.StatisticsDTO;
import org.example.smart_parking_260219.service.StatisticsService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Log4j2
@WebServlet(name = "statisticsController", value = "/statistics/statistics")

public class StatisticsController extends HttpServlet {
    private final StatisticsService statisticsService = StatisticsService.INSTANCE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("StatisticsController get... ");
        try {
            // 1. 파라미터 수집 (targetDate가 없으면 오늘 날짜로 세팅)
            String targetDate = req.getParameter("targetDate");
            if (targetDate == null || targetDate.isEmpty()) {
                targetDate = LocalDate.now().toString(); // "2026-02-12"
            }

            // 2. 연도와 월 (일별 매출 및 차종 통계용)
            // LocalDate.parse를 이용하면 안전하게 연/월을 뽑을 수 있습니다.
            LocalDate date = LocalDate.parse(targetDate);
            int year = date.getYear();
            int month = date.getMonthValue();

            // 3. 서비스 호출하여 가공된 데이터(0 채워진 데이터) 가져오기
            List<StatisticsDTO> hourlySales = statisticsService.getHourlySales(targetDate);
            List<StatisticsDTO> hourlyCounts = statisticsService.getHourlyCount(targetDate);
            List<StatisticsDTO> dailySales = statisticsService.getDailySales(year, month);
            List<StatisticsDTO> carTypeStats = statisticsService.getCarTypeStats(year, month);

            // 일일 매출 총합 계산 (hourlySales 리스트의 value 합산)
            long dayTotal = hourlySales.stream().mapToLong(StatisticsDTO::getValue).sum();
            log.info("dayTotal: " + dayTotal);

            // 월간 매출 총합 계산 (dailySales 리스트의 value 합산)
            long monthTotal = dailySales.stream().mapToLong(StatisticsDTO::getValue).sum();
            log.info("monthTotal: " + monthTotal);

            int monthSubscribedFee = statisticsService.getMonthlySales();
            log.info("monthSubscribedFee: " + monthSubscribedFee);

            // 4. JSP 화면으로 전달하기 위해 request에 setAttribute
            req.setAttribute("targetDate", targetDate);
            req.setAttribute("hourlySales", hourlySales);
            req.setAttribute("hourlyCounts", hourlyCounts);
            req.setAttribute("dailySales", dailySales);
            req.setAttribute("carTypeStats", carTypeStats);
            req.setAttribute("dayTotal", dayTotal);
            req.setAttribute("monthTotal", monthTotal);
            req.setAttribute("monthSubscribedFee", monthSubscribedFee);

            // 5. 결과를 보여줄 JSP로 포워딩
            log.info(req.getRequestURI());
            req.getRequestDispatcher("/WEB-INF/views/statistics/statistics.jsp").forward(req, resp);

        } catch (Exception e) {
            log.error("StatisticsController failed: " + e.getMessage());
            // 에러 발생 시 처리 (예: 에러 페이지로 리다이렉트)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "통계 데이터를 불러올 수 없습니다.");
        }
    }

}


