package org.example.smart_parking_260219.controller.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.PaymentDTO;
import org.example.smart_parking_260219.service.PaymentService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@WebServlet(name = "paymentListController", value = "/payment/payment_list")
public class PaymentListController extends HttpServlet {
    private final PaymentService paymentService = PaymentService.INSTANCE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, IOException {
        // 1. 날짜 파라미터 처리
        String targetDate = req.getParameter("targetDate");
        if (targetDate == null || targetDate.isEmpty()) {
            targetDate = LocalDate.now().toString();
        }

        // 2. 페이지 번호 가져오기 (기본값: 1)
        String pageParam = req.getParameter("page");
        int currentPage = 1;
        if (pageParam != null && !pageParam.isEmpty()) {
            try {
                currentPage = Integer.parseInt(pageParam);
            } catch (NumberFormatException e) {
                currentPage = 1;
            }
        }

        // 3. 페이지당 항목 수
        int itemsPerPage = 10;

        // 4. 해당 날짜 전체 데이터 조회 (Service에서 List<PaymentDTO> 반환)
        List<PaymentDTO> allPayments = paymentService.getPaymentList(targetDate);
        int totalItems = allPayments.size();

        // 5. 전체 페이지 수 계산
        int totalPages = (totalItems > 0) ? (int) Math.ceil((double) totalItems / itemsPerPage) : 1;

        // 6. 페이지 번호 유효성 검사
        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        // 7. 현재 페이지 데이터 추출 (subList 사용)
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        List<PaymentDTO> pagedPayments = (startIndex < totalItems)
                ? allPayments.subList(startIndex, endIndex)
                : new ArrayList<>();

        // 8. 시작 번호 계산 (역순 표시용)
        int startNo = totalItems - (startIndex);

        log.info("target date: " + targetDate
                + "totalItems: " + totalItems
                + "currentPage: " + currentPage
                + "itemsPerPage: " + itemsPerPage
                + "totalPages: " + totalPages
                + " startIndex: " + startIndex
                + " endIndex: " + endIndex);

        // JSP로 데이터 전달
        req.setAttribute("targetDate", targetDate);
        req.setAttribute("paymentList", pagedPayments);
        req.setAttribute("currentPage", currentPage);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("startNo", startNo);

        req.getRequestDispatcher("/WEB-INF/views/payment/payment_list.jsp").forward(req, resp);
    }

}
