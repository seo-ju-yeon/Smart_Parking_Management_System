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
 * 대시보드 페이지 요청을 처리하는 컨트롤러입니다.
 *
 * <p>
 * 로그인한 관리자만 대시보드에 접근할 수 있도록 세션 정보를 확인합니다.
 * </p>
 */
@Log4j2
@WebServlet(name = "dashboardController", value = {"/dashboard"})
public class DashboardController extends HttpServlet {

    /**
     * 대시보드 페이지 요청을 처리합니다.
     *
     * @param request  클라이언트 요청 객체
     * @param response 서버 응답 객체
     * @throws ServletException 서블릿 처리 중 문제가 발생한 경우
     * @throws IOException      입출력 처리 중 문제가 발생한 경우
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("=== DashboardController doGet() 호출 ===");

        // 기존 세션만 조회
        HttpSession session = request.getSession(false);

        // 세션과 로그인 정보를 확인하여 미인증 사용자의 접근 차단
        if (session != null) {
            log.info("세션 존재: {}", session.getId());

            Object loginManager = session.getAttribute("loginManager");
            Object managerId = session.getAttribute("managerId");

            log.info("loginManager: {}", loginManager);
            log.info("managerId: {}", managerId);

            if (loginManager == null) {
                log.info("미인증 대시보드 접근 요청 - 로그인 페이지로 리다이렉트");
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
        } else {
            log.info("세션 없음 - 로그인 페이지로 리다이렉트");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // 인증된 사용자는 대시보드 화면으로 이동
        log.info("대시보드 페이지로 포워딩");
        request.getRequestDispatcher("/WEB-INF/view/dashboard/dashboard.jsp").forward(request, response);
    }
}
