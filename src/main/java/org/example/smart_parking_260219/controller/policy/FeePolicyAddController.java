package org.example.smart_parking_260219.controller.policy;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.FeePolicyDTO;
import org.example.smart_parking_260219.service.FeePolicyService;

import java.io.IOException;

import static java.lang.Integer.parseInt;

@Log4j2
@WebServlet("/view/policy/add")
public class FeePolicyAddController extends HttpServlet {

    private final FeePolicyService feePolicyService = FeePolicyService.getInstance();

    // 등록 폼
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/policy/add.jsp").forward(req, resp);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 1. 파라미터 수집 및 DTO 생성
            FeePolicyDTO feePolicyDTO = FeePolicyDTO.builder()
                    .gracePeriod(parseInt(req.getParameter("gracePeriod")))
                    .defaultTime(parseInt(req.getParameter("defaultTime")))
                    .defaultFee(parseInt(req.getParameter("defaultFee")))
                    .extraTime(parseInt(req.getParameter("extraTime")))
                    .extraFee(parseInt(req.getParameter("extraFee")))
                    .lightDiscount(Double.parseDouble(req.getParameter("lightDiscount")))
                    .disabledDiscount(Double.parseDouble(req.getParameter("disabledDiscount")))
                    .subscribedFee(parseInt(req.getParameter("subscribedFee")))
                    .maxDailyFee(parseInt(req.getParameter("maxDailyFee")))
                    .isActive(Boolean.parseBoolean(req.getParameter("isActive")))
                    .build();

            // 2. 서비스 계층 호출 (등록 실행)
            feePolicyService.addPolicy(feePolicyDTO);

            // 3. 성공 시 목록 페이지로 이동
            resp.sendRedirect("/view/policy/list");

        } catch (Exception e) {
            log.error("요금 정책 등록 중 오류 발생", e);
            // 4. 실패 시 에러 메시지와 함께 다시 등록 폼으로 이동
            resp.sendRedirect("/view/policy/add?error=fail");
        }
    }
}
