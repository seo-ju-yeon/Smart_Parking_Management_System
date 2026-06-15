package org.example.smart_parking_260219.controller.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * 로그아웃 요청을 처리하는 컨트롤러입니다.
 *
 * <p>
 * 현재 세션을 무효화한 뒤 로그인 페이지로 이동합니다.
 * </p>
 */
@Log4j2
@WebServlet(name = "logoutController", value = {"/logout"})
public class LogoutController extends HttpServlet {

    /**
     * GET 방식의 로그아웃 요청을 처리합니다.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 기존 세션만 조회
        HttpSession session = request.getSession(false);

        if (session != null) {
            String managerId = (String) session.getAttribute("managerId");
            log.info("로그아웃 처리 - ID: {}", managerId);

            // 로그인 세션 무효화
            session.invalidate();
        }
        // 로그아웃 후 로그인 페이지로 이동
        response.sendRedirect(request.getContextPath() + "/login");
    }

    /**
     * POST 방식의 로그아웃 요청도 GET과 동일하게 처리합니다.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        doGet(request, response);
    }
}
