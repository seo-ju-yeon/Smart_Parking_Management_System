package org.example.smart_parking_260219.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 로그인 여부를 확인하여 미인증 사용자의 접근을 제한하는 필터입니다.
 *
 * <p>
 * 모든 요청을 대상으로 동작하지만, 로그인 페이지, 로그아웃, 비밀번호 찾기,
 * 정적 리소스 등 인증 없이 접근해야 하는 경로는 검사에서 제외합니다.
 * </p>
 *
 * <p>
 * 인증이 필요한 요청에서 로그인 세션이 없으면 로그인 페이지로 리다이렉트하고,
 * 인증된 요청은 다음 필터 또는 컨트롤러로 전달합니다.
 * </p>
 */
@Log4j2
public class LoginCheckFilter implements Filter {

    /**
     * 로그인 검사를 제외할 경로 목록입니다.
     */
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
            "/login",
            "/login/verifyEmail", "/login/sendLoginOtp", "/login/verifyEmailOtp",  // 로그인 2차 인증
            "/logout",
            "/resources",
            "/css/",
            "/js/",
            "/forgot-password"  // 비밀번호 찾기
    );

    /**
     * 요청별 로그인 세션을 확인하고 인증되지 않은 요청을 로그인 페이지로 이동시킵니다.
     *
     * @param request  필터에 전달된 요청 객체
     * @param response 필터에 전달된 응답 객체
     * @param chain    다음 필터 또는 대상 서블릿으로 요청을 전달하는 체인
     * @throws IOException      필터 처리 중 입출력 오류가 발생한 경우
     * @throws ServletException 필터 또는 서블릿 처리 중 오류가 발생한 경우
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        log.info("LoginCheckFilter doFilter() called");

        // URI, 세션, 리다이렉트 등 HTTP 전용 기능을 사용하기 위해 HTTP 요청/응답 객체로 변환
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = requestURI.substring(contextPath.length());  // 전체 주소에서 프로젝트 경로 제외

        // 캐시 제어 로직 (세션 없이 뒤로가기 시 보안 강화)
        // 인증 페이지와 정적 리소스를 제외한 화면 요청은 브라우저 캐시를 사용하지 않도록 한다.
        if (!path.startsWith("/login") && !path.startsWith("/password") &&
                !path.endsWith(".css") && !path.endsWith(".js")) {

            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
            resp.setHeader("Pragma", "no-cache"); // HTTP 1.0
            resp.setHeader("Expires", "0"); // Proxies
        }

        // 예외 경로인지 확인 (로그인, CSS 파일 등은 검사 없이 통과)
        if (isExcluded(path)) {
            log.info("제외 경로 - 통과: {}", path);
            chain.doFilter(request, response);
            return;
        }

        // 인증 검사 과정에서 불필요한 세션이 생성되지 않도록 기존 세션만 조회
        HttpSession session = req.getSession(false);

        // loginManager는 아이디, 비밀번호를 이용한 1차 인증 성공 여부를 의미
        boolean hasLoginManager =
                session != null
                        && session.getAttribute("loginManager") != null;

        // 보호된 경로는 이메일 또는 OTP를 포함한 2차 인증까지 완료한 경우에만 허용
        // Boolean.TRUE.equals()를 사용하여 속성이 없거나 false인 경우 안전하게 차단
        boolean isFullyAuthenticated =
                hasLoginManager
                        && Boolean.TRUE.equals(
                        session.getAttribute("fullyAuthenticated")
                );

        if (isFullyAuthenticated) {
            log.info("전체 인증 완료 - 요청 통과: {}", path);
            chain.doFilter(request, response);
            return;
        }

        // 1차 인증만 완료한 요청과 로그인하지 않은 요청을 구분하여 기록
        if (hasLoginManager) {
            log.warn("2차 인증 미완료 요청 차단: {}", path);
        } else {
            log.warn("미인증 요청 차단: {}", path);
        }

        // 인증이 완료되지 않은 사용자는 로그인 페이지에서 인증 절차를 다시 시작
        resp.sendRedirect(contextPath + "/login");
    }

    /**
     * 요청 경로가 로그인 검사 제외 대상인지 확인합니다.
     *
     * @param path 컨텍스트 경로를 제외한 요청 경로
     * @return 제외 대상이면 true, 로그인 검사가 필요하면 false
     */
    private boolean isExcluded(String path) {
        if (path.equals("/")) return false; // 루트(/)는 대시보드로 가야 하므로 검사 대상

        for (String exclude : EXCLUDE_URLS) {
            if (path.startsWith(exclude)) return true;
        }
        return false;
    }
}
