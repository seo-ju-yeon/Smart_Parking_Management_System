package org.example.smart_parking_260219.controller.policy;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.service.FeePolicyService;

import java.io.IOException;

@Log4j2
@WebServlet(name = "FeePolicyApplyController", value = "/view/policy/apply")
public class FeePolicyApplyController extends HttpServlet {
    private final FeePolicyService feePolicyService = FeePolicyService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        String pageNum = req.getParameter("pageNum");
        String items = req.getParameter("items");
        String keyword = req.getParameter("keyword");

        if (idParam != null) {
            try {
                int id = Integer.parseInt(idParam);
                // 서비스에 정책 적용 로직 호출
                feePolicyService.applyPolicy(id);
            } catch (Exception e) {
                log.error("요금 정책 적용 중 오류 발생", e);
            }
        }

        if (pageNum == null || pageNum.isEmpty()) pageNum = "1";
        if (items == null) items = "";
        if (keyword == null) keyword = "";

        // 처리가 끝나면 다시 목록이나 해당 상세페이지로 이동
        resp.sendRedirect("/view/policy/list?pageNum=" + pageNum + "&items=" + items + "&keyword=" + keyword);
    }
}
