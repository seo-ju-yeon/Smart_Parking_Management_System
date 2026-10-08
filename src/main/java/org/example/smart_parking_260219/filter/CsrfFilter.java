package org.example.smart_parking_260219.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 세션에 저장한 토큰과 상태 변경 요청의 토큰을 비교합니다.
 */
public class CsrfFilter implements Filter {

    private static final String TOKEN_ATTRIBUTE = "csrfToken";
    private static final String TOKEN_PARAMETER = "_csrf";
    private static final String TOKEN_HEADER = "X-CSRF-Token";
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String method = req.getMethod();

        if ("GET".equals(method)) {
            HttpSession session = req.getSession(false);

            // 로그인 전 화면에서도 토큰이 필요하지만 정적 파일 조회로 세션을 만들지는 않음
            if (session == null && isInitialPage(req.getServletPath())) {
                session = req.getSession();
            }

            if (session != null) {
                req.setAttribute(TOKEN_ATTRIBUTE, getOrCreateToken(session));
            }

            chain.doFilter(request, response);
            return;
        }

        if ("HEAD".equals(method) || "OPTIONS".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 토큰을 폼 파라미터에서 읽으면 본문이 파싱되므로 다른 한글 입력값보다 먼저 설정함
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());

        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(TOKEN_ATTRIBUTE);

        if (expected == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String supplied = req.getHeader(TOKEN_HEADER);

        if (supplied == null) {
            supplied = req.getParameter(TOKEN_PARAMETER);
        }

        if (supplied == null || supplied.length() != expected.length() || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // POST 실패 후 JSP로 다시 forward하는 화면도 같은 토큰을 사용할 수 있습니다.
        req.setAttribute(TOKEN_ATTRIBUTE, expected);
        chain.doFilter(request, response);
    }

    private boolean isInitialPage(String path) {
        return "/login".equals(path) || "/forgot-password".equals(path);
    }

    private String getOrCreateToken(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute(TOKEN_ATTRIBUTE);

            if (token == null) {
                byte[] randomBytes = new byte[TOKEN_BYTES];
                SECURE_RANDOM.nextBytes(randomBytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
                session.setAttribute(TOKEN_ATTRIBUTE, token);
            }

            return token;
        }
    }
}
