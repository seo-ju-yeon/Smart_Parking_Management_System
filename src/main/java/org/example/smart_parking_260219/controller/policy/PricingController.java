package org.example.smart_parking_260219.controller.policy;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.smart_parking_260219.dto.FeePolicyDTO;
import org.example.smart_parking_260219.service.FeePolicyService;

import java.io.IOException;

@WebServlet(name = "PricingController", value = "/policy/pricing")
public class PricingController extends HttpServlet {

    private final FeePolicyService feePolicyService = FeePolicyService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. DB에서 현재 활성화된(isActive=true) 정책 가져오기
        FeePolicyDTO dto = feePolicyService.getPolicy(); // 서비스에 구현된 getPolicy() 호출

        // 2. 데이터를 request에 담기
        req.setAttribute("policy", dto);

        // 3. pricing.jsp로 이동 (WEB-INF 안에 있다면 경로 확인)
        req.getRequestDispatcher("/WEB-INF/views/policy/pricing.jsp").forward(req, resp);
    }
}