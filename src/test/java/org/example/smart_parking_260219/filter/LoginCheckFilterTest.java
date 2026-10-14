package org.example.smart_parking_260219.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 공개 경로와 로그인, 2차 인증 상태에 따라 요청을 통과시키거나 리다이렉트하는지 확인함
// 각 테스트는 요청 경로와 세션을 준비한 뒤 필터 실행 결과와 다음 필터 호출 여부를 확인함
@Log4j2
class LoginCheckFilterTest {

    private LoginCheckFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        filter = new LoginCheckFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);

        // 실제 서버 없이 앱이 /parking 아래에 배포된 상황을 가정함
        // 요청 경로에서 /parking을 제외해 로그인 여부를 검사하고, 차단 시 /parking/login으로 보내는지 확인함
        when(request.getContextPath()).thenReturn("/parking");
    }

    @Test
    void loginPagePassesTest() throws Exception {
        when(request.getRequestURI()).thenReturn("/parking/login");

        filter.doFilter(request, response, chain);

        log.info("--- 로그인 화면 요청: 인증 검사 없이 통과 ---");
        verify(request, never()).getSession(false);
        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void loginOtpPathPassesTest() throws Exception {
        // 2차 인증을 완료하기 위한 요청은 인증 전에도 접근할 수 있어야 함
        when(request.getRequestURI()).thenReturn("/parking/login/verifyEmailOtp");

        filter.doFilter(request, response, chain);

        log.info("--- 로그인 OTP 경로 요청: 인증 검사 없이 통과 ---");
        verify(request, never()).getSession(false);
        verify(chain).doFilter(request, response);
    }

    @Test
    void cssPathPassesTest() throws Exception {
        when(request.getRequestURI()).thenReturn("/parking/css/style.css");

        filter.doFilter(request, response, chain);

        log.info("--- CSS 경로 요청: 로그인 없이 통과 ---");
        verify(request, never()).getSession(false);
        verify(chain).doFilter(request, response);
    }

    @Test
    void jsPathPassesTest() throws Exception {
        when(request.getRequestURI()).thenReturn("/parking/js/common/menu.js");

        filter.doFilter(request, response, chain);

        log.info("--- JavaScript 경로 요청: 로그인 없이 통과 ---");
        verify(request, never()).getSession(false);
        verify(chain).doFilter(request, response);
    }

    @Test
    void similarLoginPathRedirectsTest() throws Exception {
        // /login으로 시작하더라도 공개 경로 목록에 없는 주소는 보호해야 함
        when(request.getRequestURI()).thenReturn("/parking/login/other");

        filter.doFilter(request, response, chain);

        log.info("--- 공개 목록에 없는 로그인 유사 경로: /parking/login으로 이동 ---");
        verify(response).sendRedirect("/parking/login");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void withoutSessionRedirectsTest() throws Exception {
        when(request.getRequestURI()).thenReturn("/parking/dashboard");

        filter.doFilter(request, response, chain);

        // 비로그인 요청은 새 세션을 만들지 않고 앱 경로를 포함한 로그인 주소로 이동함
        log.info("--- 세션 없는 대시보드 요청: /parking/login으로 이동 ---");
        verify(request).getSession(false);
        verify(request, never()).getSession();
        verify(response).sendRedirect("/parking/login");
        verify(chain, never()).doFilter(request, response);

        // 보호 화면을 브라우저에 저장하지 않도록 응답 헤더도 설정함
        verify(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setHeader("Expires", "0");
    }

    @Test
    void firstAuthOnlyRedirectsTest() throws Exception {
        when(request.getRequestURI()).thenReturn("/parking/dashboard");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("loginManager")).thenReturn(new Object());
        when(session.getAttribute("fullyAuthenticated")).thenReturn(Boolean.FALSE);

        filter.doFilter(request, response, chain);

        // 아이디·비밀번호 인증만으로는 보호된 화면에 접근할 수 없음
        log.info("--- 1차 인증만 완료한 요청: /parking/login으로 이동 ---");
        verify(response).sendRedirect("/parking/login");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void secondAuthOnlyRedirectsTest() throws Exception {
        when(request.getRequestURI()).thenReturn("/parking/dashboard");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("fullyAuthenticated")).thenReturn(Boolean.TRUE);

        filter.doFilter(request, response, chain);

        // 2차 인증 표시만 있고 로그인 관리자 정보가 없을 때도 접근을 막음
        log.info("--- 로그인 관리자 정보 없는 요청: /parking/login으로 이동 ---");
        verify(response).sendRedirect("/parking/login");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void fullyAuthenticatedPassesTest() throws Exception {
        when(request.getRequestURI()).thenReturn("/parking/dashboard");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("loginManager")).thenReturn(new Object());
        when(session.getAttribute("fullyAuthenticated")).thenReturn(Boolean.TRUE);

        filter.doFilter(request, response, chain);

        log.info("--- 1·2차 인증을 모두 완료한 요청: 대시보드 통과 ---");
        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
    }
}
