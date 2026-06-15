package org.example.smart_parking_260219.controller.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dao.ManagerDAO;
import org.example.smart_parking_260219.dto.ManagerDTO;
import org.example.smart_parking_260219.service.ManagerService;
import org.example.smart_parking_260219.vo.ManagerVO;

import java.io.IOException;
import java.util.List;

/**
 * 관리자 조회 관련 요청을 처리하는 컨트롤러.
 *
 * <p>기존 ManagerController가 담당하던 관리자 목록 조회와 상세 조회 기능을 분리한 컨트롤러이다.
 * ManagerController가 삭제되더라도 /mgr, /mgr/, /mgr/list, /mgr/view 요청을 처리할 수 있도록
 * 기본 경로와 /mgr/* fallback 경로까지 함께 매핑한다.</p>
 */
@Log4j2
@WebServlet(name = "managerViewController", value = {"/mgr", "/mgr/", "/mgr/*", "/mgr/list", "/mgr/view"})
public class ManagerViewController extends HttpServlet {

    // 관리자 상세 조회에 사용하는 DAO
    private final ManagerDAO managerDAO = ManagerDAO.getInstance();

    /**
     * 관리자 목록과 관리자 상세 조회 화면을 처리한다.
     *
     * <p>/mgr 또는 /mgr/로 접근하면 기존 ManagerController와 동일하게 관리자 목록 화면을 보여준다.</p>
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("=== ManagerViewController doGet() 진입 ===");
        logRequestInfo(request);

        // 관리자 조회 화면은 로그인한 사용자만 접근
        HttpSession session = getLoginSessionOrRedirect(request, response);
        if (session == null) {
            return;
        }

        // 정확한 URL 매핑과 /mgr/* fallback 매핑이 함께 있으므로 최종 처리 경로를 한 번 정규화
        String managerPath = resolveManagerPath(request);
        log.info("처리할 managerPath: {}", managerPath);

        switch (managerPath) {
            case "/":
            case "/mgr/list":
            case "/list":
                showManagerList(request, response);
                break;

            case "/mgr/view":
            case "/view":
                showManagerView(request, response, session);
                break;

            default:
                log.warn("알 수 없는 GET 경로: {}", managerPath);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }

    /**
     * 조회 컨트롤러로 들어오는 POST 요청은 기존 ManagerController의 미정의 POST 경로 처리와 동일하게 404로 응답한다.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("=== ManagerViewController doPost() 진입 ===");
        logRequestInfo(request);

        request.setCharacterEncoding("UTF-8");  // 한글 깨짐 방지

        if (getLoginSessionOrRedirect(request, response) == null) {
            return;
        }

        String managerPath = resolveManagerPath(request);
        log.warn("조회 컨트롤러에서 지원하지 않는 POST 경로: {}", managerPath);
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * 관리자 목록 조회 화면을 처리한다.
     */
    private void showManagerList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("ManagerViewController.showManagerList() 진입");
        log.info("관리자 목록 조회 요청 처리 중..");

        try {
            // 서비스 계층에서 관리자 목록을 DTO 형태로 조회
            List<ManagerDTO> dtoList = ManagerService.INSTANCE.getAllManagers();

            // 조회 결과를 목록 JSP 파일에서 반복문을 돌릴 수 있도록 리스트로 전달
            request.setAttribute("managerList", dtoList);
            log.info("목록 조회 완료: {}명", dtoList.size());

        } catch (Exception e) {
            log.error("목록 조회 중 오류 발생", e);
            request.setAttribute("error", "목록을 불러오는 중 오류가 발생했습니다.");
        }

        request.getRequestDispatcher("/WEB-INF/views/mgr_list.jsp").forward(request, response);
    }

    /**
     * 관리자 상세 조회 화면을 처리한다.
     *
     * <p>id 파라미터가 없으면 현재 로그인한 관리자 ID를 기준으로 조회한다.</p>
     */
    private void showManagerView(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws ServletException, IOException {

        log.info("ManagerViewController.showManagerView() 진입");
        log.info("관리자 상세 조회 화면 처리");

        // 상세 조회 대상 관리자 ID를 요청 파라미터에서 확인
        String viewId = request.getParameter("id");
        log.info("조회할 관리자 ID: {}", viewId);

        // id 파라미터가 없으면 현재 로그인한 관리자 정보를 기본 조회 대상으로 사용
        if (viewId == null || viewId.trim().isEmpty()) {
            log.info("ID 파라미터가 없어 세션에서 정보를 찾습니다.");
            ManagerVO loginManager = (ManagerVO) session.getAttribute("loginManager");
            if (loginManager != null) {
                viewId = loginManager.getManagerId();
            }
        }
        log.info("최종 조회할 관리자 ID: {}", viewId);

        if (viewId != null && !viewId.isEmpty()) {
            try {
                ManagerVO manager = managerDAO.selectOne(viewId);

                if (manager != null) {
                    // 최고 관리자 계정은 별도 수정 메뉴를 사용하도록 상세 조회 화면 접근을 제한함
                    if ("ADMIN".equals(manager.getRole())) {
                        log.warn("최고관리자 계정({}) view 접근 차단 → 목록으로 리다이렉트", viewId);
                        session.setAttribute("error",
                                "최고 관리자 계정은 '최고 관리자 정보 수정' 메뉴를 이용해 주세요.");
                        response.sendRedirect(request.getContextPath() + "/mgr/list");
                        return;
                    }

                    // 조회 결과를 상세 JSP에서 사용할 수 있도록 전달
                    request.setAttribute("manager", manager);
                    log.info("관리자 데이터 조회 성공: {}", manager.getManagerName());
                } else {
                    log.warn("ID가 {}인 관리자를 찾을 수 없음", viewId);
                    request.setAttribute("error", "존재하지 않는 관리자입니다.");
                }
            } catch (Exception e) {
                log.error("관리자 조회 중 DB 오류", e);
                request.setAttribute("error", "데이터를 가져오는 중 오류가 발생했습니다.");
            }
        } else {
            log.warn("조회할 ID를 찾을 수 없음 (파라미터X, 세션X)");
            request.setAttribute("error", "조회할 관리자 정보를 특정할 수 없습니다.");
        }

        request.getRequestDispatcher("/WEB-INF/views/mgr_view.jsp").forward(request, response);
    }

    /**
     * 로그인 세션을 확인하고, 미인증 요청이면 로그인 페이지로 이동시킨다.
     *
     * @return 인증된 세션. 미인증이면 null.
     */
    private HttpSession getLoginSessionOrRedirect(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            log.info("세션 ID: {}", session.getId());
            Object loginManager = session.getAttribute("loginManager");
            log.info("loginManager: {}", loginManager);
        } else {
            log.warn("세션이 없음");
        }

        if (session == null || session.getAttribute("loginManager") == null) {
            log.warn("미인증 요청 - 로그인 페이지로 리다이렉트");
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        return session;
    }

    /**
     * /mgr 계열 요청의 실제 처리 경로를 계산한다.
     *
     * <p>정확 매핑(/mgr/list, /mgr/view)으로 들어온 요청은 servletPath를 그대로 사용하고,
     * /mgr/* fallback으로 들어온 요청은 pathInfo를 사용한다. 기존 ManagerController와 동일하게
     * /mgr, /mgr/ 요청은 관리자 목록으로 처리할 수 있도록 "/"로 정규화한다.</p>
     */
    private String resolveManagerPath(HttpServletRequest request) {

        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();

        log.info("경로 정규화 입력 - servletPath: {}, pathInfo: {}", servletPath, pathInfo);

        // /mgr/* fallback 매핑은 pathInfo를 실제 처리 경로로 사용 (/mgr/add/test -> pathInfo=/add/test)
        if ("/mgr".equals(servletPath) && pathInfo != null) {
            if (pathInfo.isEmpty() || "/".equals(pathInfo)) {
                return "/";
            }
            return pathInfo;
        }

        // /mgr, /mgr/ 요청은 관리자 목록 경로로 정규화
        if ("/mgr".equals(servletPath) || "/mgr/".equals(servletPath)) {
            return "/";
        }

        return servletPath;
    }

    /**
     * 요청 경로 관련 로그를 일관된 형태로 남긴다.
     */
    private void logRequestInfo(HttpServletRequest request) {
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Context Path: {}", request.getContextPath());
        log.info("Servlet Path: {}", request.getServletPath());
        log.info("Path Info: {}", request.getPathInfo());
    }
}
