package org.example.smart_parking_260219.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.vo.ManagerRole;
import org.example.smart_parking_260219.vo.ManagerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 인증을 마친 관리자가 역할에 맞는 경로만 이용할 수 있는지 확인함
// 요청 경로와 세션을 준비한 뒤 403 응답 또는 다음 필터 호출 여부를 확인함
@Log4j2
class AuthorizationFilterTest {

    private AuthorizationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        filter = new AuthorizationFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);

        // 실제 서버 없이 앱이 /parking 아래에 배포된 상황을 가정함
        // /parking/mgr/list에서 /parking을 제외한 /mgr/list로 권한을 검사하는지 확인함
        when(request.getContextPath()).thenReturn("/parking");
    }

    @Test
    void withoutLoginManagerSkipsRoleCheckTest() throws Exception {
        // 인증 전 요청은 앞에서 실행되는 LoginCheckFilter가 차단하므로 여기서는 역할을 검사하지 않음
        when(request.getRequestURI()).thenReturn("/parking/mgr/list");

        filter.doFilter(request, response, chain);

        log.info("--- 로그인 관리자 정보 없는 요청: 역할 검사 없이 통과 ---");
        verify(request).getSession(false);
        verify(request, never()).getSession();
        verify(chain).doFilter(request, response);
    }

    @Test
    void withoutFullAuthenticationSkipsRoleCheckTest() throws Exception {
        // 1차 인증 정보만 있는 요청도 LoginCheckFilter가 먼저 차단함
        when(request.getRequestURI()).thenReturn("/parking/mgr/list");
        when(request.getSession(false)).thenReturn(session);
        ManagerVO manager = ManagerVO.builder()
                .managerId("test-manager")
                .role(ManagerRole.NORMAL)
                .build();
        when(session.getAttribute("loginManager")).thenReturn(manager);
        when(session.getAttribute("fullyAuthenticated")).thenReturn(Boolean.FALSE);

        filter.doFilter(request, response, chain);

        log.info("--- 2차 인증 미완료 요청: 역할 검사 없이 통과 ---");
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingRoleReturns403Test() throws Exception {
        authenticatedManager(null);
        when(request.getRequestURI()).thenReturn("/parking/dashboard");

        filter.doFilter(request, response, chain);

        log.info("--- 역할 정보 없는 관리자 요청: 403 ---");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "해당 기능에 접근할 권한이 없습니다.");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void normalManagerAdminPathReturns403Test() throws Exception {
        authenticatedManager(ManagerRole.NORMAL);
        when(request.getRequestURI()).thenReturn("/parking/mgr/list");

        filter.doFilter(request, response, chain);

        log.info("--- NORMAL의 관리자 목록 요청: 403 ---");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "해당 기능에 접근할 권한이 없습니다.");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void normalManagerPolicyAddReturns403Test() throws Exception {
        authenticatedManager(ManagerRole.NORMAL);
        when(request.getRequestURI()).thenReturn("/parking/view/policy/add");

        filter.doFilter(request, response, chain);

        log.info("--- NORMAL의 요금 정책 추가 요청: 403 ---");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "해당 기능에 접근할 권한이 없습니다.");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void normalManagerPolicyApplyReturns403Test() throws Exception {
        authenticatedManager(ManagerRole.NORMAL);
        when(request.getRequestURI()).thenReturn("/parking/view/policy/apply");

        filter.doFilter(request, response, chain);

        log.info("--- NORMAL의 요금 정책 적용 요청: 403 ---");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "해당 기능에 접근할 권한이 없습니다.");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void adminManagerAdminPathPassesTest() throws Exception {
        authenticatedManager(ManagerRole.ADMIN);
        when(request.getRequestURI()).thenReturn("/parking/mgr/list");

        filter.doFilter(request, response, chain);

        log.info("--- ADMIN의 관리자 목록 요청: 통과 ---");
        verify(chain).doFilter(request, response);
    }

    @Test
    void adminManagerPolicyApplyPassesTest() throws Exception {
        authenticatedManager(ManagerRole.ADMIN);
        when(request.getRequestURI()).thenReturn("/parking/view/policy/apply");

        filter.doFilter(request, response, chain);

        log.info("--- ADMIN의 요금 정책 적용 요청: 통과 ---");
        verify(chain).doFilter(request, response);
    }

    @Test
    void adminManagerNormalPathReturns403Test() throws Exception {
        authenticatedManager(ManagerRole.ADMIN);
        when(request.getRequestURI()).thenReturn("/parking/mgr/my_modify");

        filter.doFilter(request, response, chain);

        log.info("--- ADMIN의 NORMAL 본인 수정 요청: 403 ---");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "해당 기능에 접근할 권한이 없습니다.");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void normalManagerOwnPathPassesTest() throws Exception {
        authenticatedManager(ManagerRole.NORMAL);
        when(request.getRequestURI()).thenReturn("/parking/mgr/my_modify");

        filter.doFilter(request, response, chain);

        log.info("--- NORMAL의 본인 수정 요청: 통과 ---");
        verify(chain).doFilter(request, response);
    }

    @Test
    void normalManagerPolicyListPassesTest() throws Exception {
        authenticatedManager(ManagerRole.NORMAL);
        when(request.getRequestURI()).thenReturn("/parking/view/policy/list");

        filter.doFilter(request, response, chain);

        log.info("--- NORMAL의 요금 정책 조회 요청: 통과 ---");
        verify(chain).doFilter(request, response);
    }

    @Test
    void logoutPassesWithMissingRoleTest() throws Exception {
        // 역할 정보가 없어도 로그아웃하여 세션을 종료할 수 있어야 함
        authenticatedManager(null);
        when(request.getRequestURI()).thenReturn("/parking/logout");

        filter.doFilter(request, response, chain);

        log.info("--- 역할 정보 없는 관리자의 로그아웃 요청: 통과 ---");
        verify(chain).doFilter(request, response);
    }

    private void authenticatedManager(ManagerRole role) {
        // 로그인과 2차 인증을 마친 관리자를 세션에 준비함
        ManagerVO manager = ManagerVO.builder()
                .managerId("test-manager")
                .role(role)
                .build();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("loginManager")).thenReturn(manager);
        when(session.getAttribute("fullyAuthenticated")).thenReturn(Boolean.TRUE);
    }
}
