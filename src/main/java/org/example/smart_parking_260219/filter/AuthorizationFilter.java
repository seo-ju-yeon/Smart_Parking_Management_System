package org.example.smart_parking_260219.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.vo.ManagerVO;

import java.io.IOException;

/**
 * 인증을 완료한 관리자의 역할을 기준으로 요청 경로 접근 권한을 검사하는 필터입니다.
 *
 * <p>
 * 로그인 및 2차 인증 완료 여부는 LoginCheckFilter가 먼저 검사합니다.
 * 이 필터는 인증된 NORMAL과 ADMIN이 각 역할에 허용된 기능만 요청하도록 제한합니다.
 * </p>
 */
@Log4j2
public class AuthorizationFilter implements Filter {

    private static final String ROLE_NORMAL = "NORMAL";
    private static final String ROLE_ADMIN = "ADMIN";

    /**
     * 세션의 관리자 역할과 요청 경로를 비교하여 접근 허용 여부를 판단합니다.
     */
    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        // HTTP 요청 경로, 세션, 상태 코드를 사용하기 위해 HTTP 전용 객체로 변환함
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 애플리케이션 컨텍스트 경로를 제외한 실제 요청 경로를 구함
        String requestUri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = requestUri.substring(contextPath.length());

        // 권한 검사 과정에서 새로운 세션이 생성되지 않도록 기존 세션만 조회함
        HttpSession session = req.getSession(false);

        Object loginManager =
                session == null
                        ? null
                        : session.getAttribute("loginManager");

        boolean fullyAuthenticated =
                session != null
                        && Boolean.TRUE.equals(
                        session.getAttribute("fullyAuthenticated")
                );

        /*
         * 인증이 완료되지 않은 요청은 역할을 판단하지 않고 다음 필터로 전달함.
         * LoginCheckFilter가 먼저 실행되므로 보호 경로의 미인증 요청은 그곳에서 차단됨.
         * 로그인 화면과 정적 리소스처럼 인증 예외인 요청도 이 분기를 통해 통과함.
         */
        if (!(loginManager instanceof ManagerVO) || !fullyAuthenticated) {
            chain.doFilter(request, response);
            return;
        }

        ManagerVO manager = (ManagerVO) loginManager;
        String role = manager.getRole();

        // 로그아웃은 역할값 이상 여부와 관계없이 세션을 종료할 수 있도록 항상 통과시킴
        if ("/logout".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 정의되지 않은 역할은 권한 정책에 포함되지 않으므로 기본적으로 차단함
        if (!ROLE_NORMAL.equals(role) && !ROLE_ADMIN.equals(role)) {
            log.warn(
                    "정의되지 않은 역할의 요청 차단 - ID: {}, 역할: {}, 경로: {}, 메서드: {}",
                    manager.getManagerId(),
                    role,
                    path,
                    req.getMethod()
            );
            deny(resp);
            return;
        }

        // 관리자 계정 관리 또는 요금 정책 변경 경로는 ADMIN만 접근 가능함
        if (isAdminOnlyPath(path) && !ROLE_ADMIN.equals(role)) {
            log.warn(
                    "ADMIN 전용 요청 차단 - ID: {}, 역할: {}, 경로: {}, 메서드: {}",
                    manager.getManagerId(),
                    role,
                    path,
                    req.getMethod()
            );
            deny(resp);
            return;
        }

        // 일반 관리자 본인 정보 수정 경로는 NORMAL만 접근 가능함
        if (isNormalOnlyPath(path) && !ROLE_NORMAL.equals(role)) {
            log.warn(
                    "NORMAL 전용 요청 차단 - ID: {}, 역할: {}, 경로: {}, 메서드: {}",
                    manager.getManagerId(),
                    role,
                    path,
                    req.getMethod()
            );
            deny(resp);
            return;
        }

        // 역할 및 경로 조건을 모두 충족한 요청만 다음 필터 또는 Controller로 전달함
        chain.doFilter(request, response);
    }

    /**
     * ADMIN만 접근할 수 있는 경로인지 확인합니다.
     *
     * <p>
     * /mgr 계열은 기본적으로 ADMIN 전용으로 처리합니다.
     * NORMAL 본인 수정 경로인 /mgr/my_modify만 별도 예외로 분리합니다.
     * 요금 정책 조회는 공통으로 허용하고 추가 및 적용 경로만 ADMIN 전용으로 처리합니다.
     * </p>
     */
    private boolean isAdminOnlyPath(String path) {
        boolean managerAdministrationPath =
                "/mgr".equals(path)
                        || "/mgr/".equals(path)
                        || (path.startsWith("/mgr/")
                        && !"/mgr/my_modify".equals(path));

        boolean policyChangePath =
                "/view/policy/add".equals(path)
                        || "/view/policy/apply".equals(path);

        return managerAdministrationPath || policyChangePath;
    }

    /**
     * NORMAL만 접근할 수 있는 본인 정보 수정 경로인지 확인합니다.
     */
    private boolean isNormalOnlyPath(String path) {
        return "/mgr/my_modify".equals(path);
    }

    /**
     * 인증은 완료했지만 역할 권한이 부족한 요청에 403 응답을 반환합니다.
     */
    private void deny(HttpServletResponse response) throws IOException {
        response.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "해당 기능에 접근할 권한이 없습니다."
        );
    }

}
