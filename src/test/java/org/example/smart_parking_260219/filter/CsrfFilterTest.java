package org.example.smart_parking_260219.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 웹 서버와 DB 없이 CSRF 토큰 생성, 검증, 요청 차단 동작을 확인함
// 요청 조건을 준비하고 필터를 실행한 뒤 응답과 다음 필터 호출 여부를 확인함
@Log4j2
class CsrfFilterTest {

    private CsrfFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        // 테스트마다 새 요청/응답/세션을 준비해 호출 기록이 서로 섞이지 않게 함
        filter = new CsrfFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);
    }

    @Test
    void loginGetCreatesTokenTest() throws Exception {
        // 로그인 첫 방문에는 세션이 없어도 폼 제출에 사용할 토큰이 필요함
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/login");
        when(request.getSession()).thenReturn(session);

        filter.doFilter(request, response, chain);

        // 생성된 토큰은 미리 알 수 없으므로 세션에 저장된 값을 꺼내 확인함
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).setAttribute(eq("csrfToken"), tokenCaptor.capture());
        String token = tokenCaptor.getValue();

        assertNotNull(token);
        log.info("--- 첫 로그인 화면의 CSRF 토큰 생성: 길이={} ---", token.length());
        assertFalse(token.isBlank());
        // 폼에서 사용할 수 있도록 같은 토큰을 요청 속성에도 전달하는지 확인함
        verify(request).setAttribute("csrfToken", token);
        verify(chain).doFilter(request, response);
    }

    @Test
    void getReusesSessionTokenTest() throws Exception {
        // 세션에 토큰이 이미 있으면 새로 만들지 않고 그대로 요청 속성에 전달해야 함
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("test-token");

        filter.doFilter(request, response, chain);

        log.info("--- 기존 세션 토큰으로 GET 요청 통과 ---");
        verify(request).setAttribute("csrfToken", "test-token");
        verify(session, never()).setAttribute(eq("csrfToken"), any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void staticGetDoesNotCreateSessionTest() throws Exception {
        // 세션이 없는 CSS 요청에서는 새 세션을 만들지 않아야 함
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/css/style.css");

        filter.doFilter(request, response, chain);

        log.info("--- 정적 파일 GET 요청: 새 세션 없이 통과 ---");
        verify(request, never()).getSession();
        verify(chain).doFilter(request, response);
    }

    @Test
    void postWithoutSessionReturns403Test() throws Exception {
        // 세션이 없으면 비교할 기준 토큰도 없으므로 POST를 차단해야 함
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);

        log.info("--- 세션 없는 POST 요청: 403 ---");
        verify(request).setCharacterEncoding("UTF-8");
        // 403 응답 후에는 다음 필터나 컨트롤러로 요청이 넘어가지 않아야 함
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void postWithoutTokenReturns403Test() throws Exception {
        // 세션 토큰은 있지만 요청에 폼 값이나 헤더 토큰이 없는 경우를 확인함
        when(request.getMethod()).thenReturn("POST");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("test-token");

        filter.doFilter(request, response, chain);

        log.info("--- CSRF 토큰 없는 POST 요청: 403 ---");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void postWithWrongTokenReturns403Test() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("test-token");
        // 길이가 같은 다른 토큰으로 실제 값 비교까지 수행하는지 확인함
        when(request.getHeader("X-CSRF-Token")).thenReturn("tEst-token");

        filter.doFilter(request, response, chain);

        log.info("--- 잘못된 CSRF 토큰으로 보낸 POST 요청: 403 ---");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void postWithFormTokenPassesTest() throws Exception {
        // 폼의 _csrf 값이 세션 토큰과 일치하면 요청을 통과시켜야 함
        when(request.getMethod()).thenReturn("POST");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("test-token");
        when(request.getParameter("_csrf")).thenReturn("test-token");

        filter.doFilter(request, response, chain);

        log.info("--- 폼 토큰이 일치하는 POST 요청 통과 ---");
        // POST 후 JSP로 forward할 때 사용할 토큰을 요청 속성에 전달함
        verify(request).setAttribute("csrfToken", "test-token");
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void postWithHeaderTokenPassesTest() throws Exception {
        // fetch 요청은 폼 값 대신 X-CSRF-Token 헤더로 토큰을 전달함
        when(request.getMethod()).thenReturn("POST");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("test-token");
        when(request.getHeader("X-CSRF-Token")).thenReturn("test-token");

        filter.doFilter(request, response, chain);

        log.info("--- 헤더 토큰이 일치하는 POST 요청 통과 ---");
        verify(request).setAttribute("csrfToken", "test-token");
        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void headWithoutTokenPassesTest() throws Exception {
        // HEAD 요청은 토큰 검사 없이 다음 필터로 전달함
        when(request.getMethod()).thenReturn("HEAD");

        filter.doFilter(request, response, chain);

        log.info("--- HEAD 요청: CSRF 토큰 검사 없이 통과 ---");
        verify(request, never()).getSession(false);
        verify(chain).doFilter(request, response);
    }
}
