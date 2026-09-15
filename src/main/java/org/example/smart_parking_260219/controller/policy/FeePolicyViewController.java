package org.example.smart_parking_260219.controller.policy;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.smart_parking_260219.dto.FeePolicyDTO;
import org.example.smart_parking_260219.service.FeePolicyService;

import java.io.IOException;

@WebServlet(name = "FeePolicyViewController", value = "/view/policy")
public class FeePolicyViewController extends HttpServlet {

    private final FeePolicyService feePolicyService = FeePolicyService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. 목록에서 넘겨준 id 파라미터를 읽어옵니다.
        String policyId = req.getParameter("id");

        if (policyId != null && !policyId.isEmpty()) {
            try {
                int id = Integer.parseInt(policyId);

                // 2. 서비스 계층을 통해 DB에서 해당 id의 정책 상세 정보를 가져옵니다.
                FeePolicyDTO dto = feePolicyService.getPolicyById(id);

                req.setAttribute("policy", dto);
            } catch (Exception e) {
                // 예외 발생 시 로그를 남기거나 에러 페이지로 보냅니다.
                e.printStackTrace();
            }

        }

        req.getRequestDispatcher("/WEB-INF/views/policy/view.jsp").forward(req, resp);
    }

}
