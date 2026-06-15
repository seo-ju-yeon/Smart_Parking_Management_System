package org.example.smart_parking_260219.controller.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dao.ManagerDAO;
import org.example.smart_parking_260219.vo.ManagerVO;

import java.io.IOException;

/**
 * 관리자 수정 관련 요청을 처리하는 컨트롤러.
 *
 * <p>기존 ManagerController가 담당하던 관리자 수정 화면, 본인 정보 수정, 일반 관리자 정보 수정,
 * 관리자 활성화/비활성화 기능을 분리한 컨트롤러이다.</p>
 */
@Log4j2
@WebServlet(
        name = "managerModifyController",
        value = {"/mgr/modify", "/mgr/modify_normal", "/mgr/my_modify", "/mgr/toggleActive"}
)
public class ManagerModifyController extends HttpServlet {

    // 관리자 수정에 사용하는 DAO
    private final ManagerDAO managerDAO = ManagerDAO.getInstance();

    /**
     * 관리자 수정 관련 화면 요청을 처리한다.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("=== ManagerModifyController doGet() 진입 ===");
        logRequestInfo(request);

        // 관리자 수정 화면은 로그인한 사용자만 접근
        HttpSession session = getLoginSessionOrRedirect(request, response);
        if (session == null) {
            return;
        }

        // 정확 매핑만 사용하므로 servletPath 기준으로 처리 기능을 분기
        String servletPath = request.getServletPath();
        log.info("처리할 servletPath: {}", servletPath);

        switch (servletPath) {
            case "/mgr/modify":
                showManagerModify(request, response, session);
                break;

            case "/mgr/my_modify":
                showMyModify(request, response, session);
                break;

            case "/mgr/modify_normal":
                showModifyNormal(request, response);
                break;

            default:
                log.warn("알 수 없는 GET 경로: {}", servletPath);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }

    /**
     * 관리자 수정 관련 폼 제출 요청을 처리한다.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("=== ManagerModifyController doPost() 진입 ===");
        logRequestInfo(request);

        request.setCharacterEncoding("UTF-8");  // 한글 깨짐 방지

        // 관리자 수정 처리는 로그인한 사용자만 접근
        HttpSession session = getLoginSessionOrRedirect(request, response);
        if (session == null) {
            return;
        }

        // 요청 경로에 따라 수정, 본인 수정, 일반 관리자 수정, 활성화 토글로 분기
        String servletPath = request.getServletPath();
        log.info("처리할 servletPath: {}", servletPath);

        switch (servletPath) {
            case "/mgr/modify":
                log.info("관리자 수정 처리 시작");
                modifyManager(request, response);
                break;

            case "/mgr/my_modify":
                log.info("일반 관리자 본인 정보 수정 처리 시작");
                modifyMyInfo(request, response);
                break;

            case "/mgr/modify_normal":
                log.info("일반 관리자 수정 처리 시작");
                modifyManagerNormal(request, response);
                break;

            case "/mgr/toggleActive":
                log.info("관리자 활성화 토글 처리 시작");
                toggleManagerActive(request, response);
                break;

            default:
                log.warn("알 수 없는 POST 경로: {}", servletPath);
                // 엉뚱 URL 요청 시, 404 Not Found
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }

    /**
     * 최고 관리자 정보 수정 화면을 처리한다.
     */
    private void showManagerModify(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws ServletException, IOException {

        log.info("ManagerModifyController.showManagerModify() 진입");
        log.info("관리자 수정 페이지 처리");

        // 수정 대상 ID를 요청 파라미터에서 확인
        String modifyId = request.getParameter("id");
        log.info("수정할 관리자 ID: {}", modifyId);

        // id가 없으면 현재 로그인한 관리자 정보를 수정 대상으로 사용
        if (modifyId == null || modifyId.trim().isEmpty()) {
            log.info("ID 파라미터가 없어 세션에서 정보를 찾습니다.");
            ManagerVO loginManager = (ManagerVO) session.getAttribute("loginManager");
            if (loginManager != null) {
                modifyId = loginManager.getManagerId();
            }
        }
        log.info("최종 수정할 관리자 ID: {}", modifyId);

        if (modifyId != null && !modifyId.isEmpty()) {
            try {
                ManagerVO manager = managerDAO.selectOne(modifyId);

                if (manager != null) {
                    request.setAttribute("manager", manager);
                    log.info("관리자 데이터 조회 성공: {}", manager.getManagerName());
                } else {
                    log.warn("ID가 {}인 관리자를 찾을 수 없음", modifyId);
                    request.setAttribute("error", "존재하지 않는 관리자입니다.");
                }
            } catch (Exception e) {
                log.error("관리자 조회 중 DB 오류", e);
                request.setAttribute("error", "데이터를 가져오는 중 오류가 발생했습니다.");
            }
        } else {
            log.warn("수정할 ID를 찾을 수 없음 (파라미터X, 세션X)");
            request.setAttribute("error", "수정할 관리자 정보를 특정할 수 없습니다.");
        }

        request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
    }

    /**
     * 일반 관리자 본인 정보 수정 페이지를 처리한다.
     */
    private void showMyModify(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws ServletException, IOException {

        log.info("ManagerModifyController.showMyModify() 진입");
        log.info("일반 관리자 본인 정보 수정 페이지 요청");

        // 본인 수정은 URL 파라미터 대신 세션의 로그인 정보를 기준으로 처리
        ManagerVO myManager = (ManagerVO) session.getAttribute("loginManager");

        if (myManager == null) {
            log.warn("세션에 로그인 정보 없음 - 로그인 페이지로 리다이렉트");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // 순수 ADMIN 계정은 전용 수정 화면을 사용하고, SUPER 계정은 본인 수정도 허용
        if ("ADMIN".equals(myManager.getRole()) && !SuperKeyConfig.SUPER_ROLE.equals(myManager.getRole())) {
            log.warn("ADMIN 계정의 /my_modify 접근 차단 → /mgr/modify 로 리다이렉트");
            response.sendRedirect(request.getContextPath() + "/mgr/modify");
            return;
        }

        try {
            // 세션 정보가 오래되었을 수 있으므로 DB에서 최신 정보를 다시 조회
            ManagerVO freshManager = managerDAO.selectOne(myManager.getManagerId());

            if (freshManager != null) {
                request.setAttribute("manager", freshManager);
                log.info("본인 정보 조회 성공 - ID: {}", freshManager.getManagerId());
            } else {
                log.warn("DB에서 본인 정보를 찾을 수 없음 - ID: {}", myManager.getManagerId());
                request.setAttribute("error", "계정 정보를 불러올 수 없습니다.");
            }
        } catch (Exception e) {
            log.error("본인 정보 조회 중 오류 발생", e);
            request.setAttribute("error", "데이터 조회 중 오류가 발생했습니다.");
        }

        request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
    }

    /**
     * 일반 관리자 정보 수정 페이지를 처리한다.
     */
    private void showModifyNormal(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("ManagerModifyController.showModifyNormal() 진입");
        log.info("일반 관리자 수정 페이지 처리");

        // 수정 대상 ID를 요청 파라미터에서 확인
        String targetId = request.getParameter("id");
        log.info("수정 대상 ID: {}", targetId);

        // id가 없으면 현재 로그인한 관리자 정보를 수정 대상으로 사용
        if (targetId == null || targetId.trim().isEmpty()) {
            log.warn("수정할 ID가 없어 목록으로 돌아갑니다.");
            response.sendRedirect(request.getContextPath() + "/mgr/list");
            return;
        }

        try {
            ManagerVO targetManager = managerDAO.selectOne(targetId);

            if (targetManager != null) {
                request.setAttribute("manager", targetManager);
                log.info("수정 대상 조회 성공: {}", targetManager.getManagerName());
            } else {
                log.warn("ID가 {}인 관리자를 찾을 수 없음", targetId);
                request.setAttribute("error", "존재하지 않는 관리자입니다.");
            }
        } catch (Exception e) {
            log.error("관리자 조회 중 DB 오류", e);
            request.setAttribute("error", "데이터 조회 중 오류 발생");
        }

        request.getRequestDispatcher("/WEB-INF/views/mgr_modify_normal.jsp").forward(request, response);
    }

    /**
     * 관리자 수정 처리.
     *
     * <p>최고 관리자 정보 수정 화면에서 제출된 값을 검증하고 수정한다.</p>
     */
    private void modifyManager(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("ManagerModifyController.modifyManager() 진입");

        // 세션 체크
        HttpSession session = request.getSession(false);

        // 수정 폼에서 전달된 입력값을 읽음
        String managerId = request.getParameter("id");
        String managerName = request.getParameter("name");
        String password = request.getParameter("pw");  // 변경할 새 비밀번호 (비어있을 수 있음)
        String passwordConfirm = request.getParameter("passwordConfirm");  // 확인용 비밀번호
        String email = request.getParameter("email");

        log.info("관리자 정보 수정 요청 - ID: {}, 이름: {}, 이메일: {}", managerId, managerName, email);
        log.info("비밀번호 변경 여부: {}", (password != null && !password.trim().isEmpty() ? "Yes" : "No"));
        log.info("관리자 수정 입력값 확인 - passwordConfirm 입력여부: {}",
                passwordConfirm != null && !passwordConfirm.trim().isEmpty());

        // 필수 입력값이 누락되면 기존 정보를 다시 조회해 수정 화면으로 돌려보냄
        if (managerId == null || managerId.trim().isEmpty() ||
                managerName == null || managerName.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {

            log.warn("필수 입력값 누락");
            request.setAttribute("error", "ID, 이름, 이메일은 필수 입력값입니다.");

            try {
                // 에러 발생 시, 기존 정보를 다시 DB에서 읽어와 화면에 뿌려줘야 입력폼이 유지됨
                ManagerVO manager = managerDAO.selectOne(managerId);
                request.setAttribute("manager", manager);
            } catch (Exception e) {
                log.error("관리자 정보 재조회 실패", e);
            }
            request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
            return;
        }

        // 비밀번호를 입력한 경우에만 비밀번호 변경 검증을 수행
        if (password != null && !password.trim().isEmpty()) {
            // 새 비밀번호 & 확인용 비밀번호가 동일한지 확인
            if (!password.equals(passwordConfirm)) {
                log.warn("비밀번호 불일치");
                request.setAttribute("error", "비밀번호가 일치하지 않습니다.");

                try {
                    // 에러 발생 시, 기존 정보를 다시 DB에서 읽어와 화면에 뿌려줘야 입력폼이 유지됨
                    ManagerVO manager = managerDAO.selectOne(managerId);
                    request.setAttribute("manager", manager);
                } catch (Exception e) {
                    log.error("관리자 정보 재조회 실패", e);
                }

                request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
                return;
            }

            // 비밀번호 길이 검증
            if (password.length() < 4) {
                log.warn("비밀번호 길이 부족: {}", password.length());
                request.setAttribute("error", "비밀번호는 최소 4자 이상이어야 합니다.");

                try {
                    // 에러 발생 시, 기존 정보를 다시 DB에서 읽어와 화면에 뿌려줘야 입력폼이 유지됨
                    ManagerVO manager = managerDAO.selectOne(managerId);
                    request.setAttribute("manager", manager);
                } catch (Exception e) {
                    log.error("관리자 정보 재조회 실패", e);
                }
                request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
                return;
            }
        }

        // 이메일 형식 검증
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            log.warn("잘못된 이메일 형식: {}", email);
            request.setAttribute("error", "올바른 이메일 형식이 아닙니다.");

            try {
                // 에러 발생 시, 기존 정보를 다시 DB에서 읽어와 화면에 뿌려줘야 입력폼이 유지됨
                ManagerVO manager = managerDAO.selectOne(managerId);
                request.setAttribute("manager", manager);
            } catch (Exception e) {
                log.error("관리자 정보 재조회 실패", e);
            }

            request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
            return;
        }

        try {
            // 비밀번호는 평문으로 전달하고, DAO 계층에서 BCrypt로 암호화
            ManagerVO managerVO = ManagerVO.builder()
                    .managerId(managerId)
                    .managerName(managerName)
                    .password(password)
                    .email(email)
                    .build();

            managerDAO.updateManager(managerVO);
            log.info("관리자 정보 수정 완료 - ID: {}", managerId);

            ManagerVO loginManager = (ManagerVO) session.getAttribute("loginManager");

            // 현재 로그인한 본인 정보를 수정한 경우 세션을 종료하고 재로그인을 유도
            if (loginManager != null && managerId.equals(loginManager.getManagerId())) {
                log.info("최고 관리자 정보 수정 - 재로그인 필요");

                session.setAttribute("logoutMessage", "관리자 정보가 변경되었으니 다시 로그인해주세요.");
                session.invalidate();  // 모든 세션 정보 삭제 (로그아웃)
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                // 다른 관리자의 정보를 수정한 경우 상세 조회 화면으로 이동
                log.info("일반 관리자 정보 수정 완료 - ID: {}", managerId);
                session.setAttribute("successMessage", "관리자 정보가 성공적으로 수정되었습니다.");
                response.sendRedirect(request.getContextPath() + "/mgr/view?id=" + managerId);
            }

        } catch (Exception e) {
            log.error("관리자 정보 수정 중 오류 발생", e);
            request.setAttribute("error", "정보 수정 중 오류가 발생했습니다: " + e.getMessage());

            try {
                ManagerVO manager = managerDAO.selectOne(managerId);
                request.setAttribute("manager", manager);
            } catch (Exception ex) {
                log.error("관리자 정보 재조회 실패", ex);
            }

            request.getRequestDispatcher("/WEB-INF/views/mgr_modify.jsp").forward(request, response);
        }
    }

    /**
     * 일반 관리자 정보 수정 처리.
     *
     * <p>ADMIN이 일반 관리자 정보를 수정할 때 사용하는 처리이며, ADMIN 계정에 대한 위조 수정 요청은 차단한다.</p>
     */
    private void modifyManagerNormal(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("ManagerModifyController.modifyManagerNormal() 진입");

        // 일반 관리자 수정 폼에서 전달된 입력값을 읽음
        String managerId = request.getParameter("managerId");
        String managerName = request.getParameter("name");
        String password = request.getParameter("pw");
        String email = request.getParameter("email");

        log.info("수정 요청 수신 - ID: {}, Name: {}, PW입력여부: {}, Email: {}",
                managerId, managerName, (password != null && !password.isEmpty()), email);

        try {
            ManagerVO existing = managerDAO.selectOne(managerId);
            if (existing == null) {
                request.setAttribute("error", "존재하지 않는 관리자입니다.");
                request.getRequestDispatcher("/WEB-INF/views/mgr_modify_normal.jsp").forward(request, response);
                return;
            }

            // POST 위조 요청으로 ADMIN 계정이 수정되지 않도록 차단
            if ("ADMIN".equals(existing.getRole())) {
                log.warn("최고관리자 계정({}) POST 수정 시도 차단", managerId);
                HttpSession sess = request.getSession(false);
                if (sess != null) {
                    sess.setAttribute("error",
                            "최고 관리자 계정은 '최고 관리자 정보 수정' 메뉴를 이용해 주세요.");
                }
                response.sendRedirect(request.getContextPath() + "/mgr/list");
                return;
            }

            // 수정 대상의 고정 정보는 기존 DB 값을 유지하고, 변경 가능한 값만 새 입력값으로 교체
            ManagerVO.ManagerVOBuilder builder = ManagerVO.builder()
                    .managerNo(existing.getManagerNo())
                    .managerId(existing.getManagerId())
                    .managerName(managerName)
                    .email(email)
                    .active(existing.isActive())
                    .role(existing.getRole());

            // 비밀번호를 입력한 경우에만 변경하고, 입력하지 않으면 기존 해시값을 유지
            if (password != null && !password.trim().isEmpty()) {
                log.info("비밀번호 변경을 수행합니다.");
                builder.password(password);
            } else {
                log.info("기존 비밀번호를 유지합니다.");
                builder.password(existing.getPassword());
            }

            managerDAO.updateManager(builder.build());
            log.info("관리자 수정 완료 - ID: {}", managerId);

            // 요청자 역할에 따른 분기 처리
            HttpSession sess = request.getSession(false);
            ManagerVO currentLogin = (sess != null) ? (ManagerVO) sess.getAttribute("loginManager") : null;

            if (currentLogin != null && "NORMAL".equals(currentLogin.getRole())) {
                // 일반 관리자가 본인 정보를 수정한 경우 세션 무효화 후 재로그인 유도
                log.info("일반 관리자 본인 수정 완료 - 세션 무효화 후 로그인 페이지로 이동");

                sess.setAttribute("logoutMessage", "정보가 수정되었습니다. 변경된 정보로 다시 로그인해주세요.");
                sess.invalidate();
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                // ADMIN이 타 관리자 수정한 경우 경우 관리자 목록으로 이동
                log.info("ADMIN에 의한 관리자 수정 완료 - 목록으로 이동");
                request.getSession().setAttribute("successMessage", "정보가 성공적으로 수정되었습니다.");
                response.sendRedirect(request.getContextPath() + "/mgr/list");
            }

        } catch (Exception e) {
            log.error("수정 중 오류 발생", e);
            request.setAttribute("error", "오류가 발생했습니다: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/mgr_modify_normal.jsp").forward(request, response);
        }
    }

    /**
     * 일반 관리자 본인 정보 수정 처리
     *
     * <p>
     * 아이디는 수정할 수 없으며, 세션 ID와 요청 ID가 일치하는 경우에만 이름, 비밀번호, 이메일을 수정한다.
     * 수정 완료 후 일반 관리자는 재로그인을 유도하고, SUPER 계정은 세션을 유지한다.
     * </p>
     */
    private void modifyMyInfo(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("ManagerModifyController.modifyMyInfo() 진입");

        HttpSession session = request.getSession(false);

        // 본인 수정 대산은 세션에 저장된 로그인 관리자이다.
        ManagerVO loginManager = (ManagerVO) session.getAttribute("loginManager");

        if (loginManager == null) {
            log.warn("세션 없음 - 로그인 페이지로 리다이렉트");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // 순수 ADMIN 계정은 전용 수정 화면을 사용하고, SUPER 계정은 본인 수정도 허용
        if ("ADMIN".equals(loginManager.getRole()) && !SuperKeyConfig.SUPER_ROLE.equals(loginManager.getRole())) {
            log.warn("ADMIN이 /my_modify POST 시도 - 차단");
            response.sendRedirect(request.getContextPath() + "/mgr/modify");
            return;
        }

        // hidden 필드의 ID와 세션 ID가 다르면 위조 요청으로 판단
        String sessionId = loginManager.getManagerId();
        String requestedId = request.getParameter("managerId");

        if (!sessionId.equals(requestedId)) {
            log.warn("세션 ID({})와 요청 ID({}) 불일치 - 위조 요청 차단", sessionId, requestedId);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "잘못된 요청입니다.");
            return;
        }

        String managerName = request.getParameter("name");
        String password = request.getParameter("pw");
        String email = request.getParameter("email");

        log.info("본인 정보 수정 요청 - ID: {}, 이름: {}, 이메일: {}, 비밀번호 변경: {}",
                sessionId, managerName, email,
                (password != null && !password.trim().isEmpty() ? "Yes" : "No"));

        // 필수값 검증
        if (managerName == null || managerName.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
            log.warn("필수 입력값 누락");

            request.setAttribute("error", "이름과 이메일은 필수 입력값입니다.");
            ManagerVO fresh = managerDAO.selectOne(sessionId);
            request.setAttribute("manager", fresh);
            request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
            return;
        }

        // 비밀번호 길이 검증 (입력한 경우만)
        if (password != null && !password.trim().isEmpty() && password.length() < 4) {
            log.warn("비밀번호 길이 부족: {}", password.length());

            request.setAttribute("error", "비밀번호는 최소 4자 이상이어야 합니다.");
            ManagerVO fresh = managerDAO.selectOne(sessionId);
            request.setAttribute("manager", fresh);
            request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
            return;
        }

        // 이메일 형식 검증
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            log.warn("잘못된 이메일 형식: {}", email);

            request.setAttribute("error", "올바른 이메일 형식이 아닙니다.");
            ManagerVO fresh = managerDAO.selectOne(sessionId);
            request.setAttribute("manager", fresh);
            request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
            return;
        }

        try {
            // 기존 정보 조회
            ManagerVO existing = managerDAO.selectOne(sessionId);
            if (existing == null) {
                request.setAttribute("error", "존재하지 않는 계정입니다.");
                request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
                return;
            }

            // 아이디, 활성화 상태, 권한은 기존 DB 값을 유지
            ManagerVO.ManagerVOBuilder builder = ManagerVO.builder()
                    .managerNo(existing.getManagerNo())
                    .managerId(sessionId)
                    .managerName(managerName.trim())
                    .email(email.trim())
                    .active(existing.isActive())
                    .role(existing.getRole());

            // 비밀번호를 입력한 경우에만 변경하고, 입력하지 않으면 기존 해시값을 유지
            if (password != null && !password.trim().isEmpty()) {
                log.info("비밀번호 변경 수행 - ID: {}", sessionId);
                builder.password(password);
            } else {
                log.info("비밀번호 유지 - ID: {}", sessionId);
                builder.password(existing.getPassword());
            }

            managerDAO.updateManager(builder.build());
            log.info("본인 정보 수정 완료 - ID: {}", sessionId);

            // SUPER 계정은 시연 흐름을 위해 재로그인 없이 세션을 유지
            if (SuperKeyConfig.SUPER_ROLE.equals(loginManager.getRole())) {
                session.setAttribute("successMessage", "정보가 수정되었습니다.");
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            // 일반 관리자는 정보 수정 후 변경된 정보로 다시 로그인하도록 세션을 종료
            session.setAttribute("logoutMessage", "정보가 수정되었습니다. 변경된 정보로 다시 로그인해주세요.");
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");

        } catch (Exception e) {
            log.error("본인 정보 수정 중 오류 발생 - ID: {}", sessionId, e);
            request.setAttribute("error", "정보 수정 중 오류가 발생했습니다: " + e.getMessage());
            try {
                ManagerVO fresh = managerDAO.selectOne(sessionId);
                request.setAttribute("manager", fresh);
            } catch (Exception ex) {
                log.error("재조회 실패", ex);
            }
            request.getRequestDispatcher("/WEB-INF/views/mgr_my_modify.jsp").forward(request, response);
        }
    }

    /**
     * 관리자 계정 활성화/비활성화.
     *
     * <p>현재 로그인한 본인 계정은 스스로 비활성화할 수 없도록 차단한다.</p>
     */
    private void toggleManagerActive(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("ManagerModifyController.toggleManagerActive() 진입");

        // 본인 계정 비활성화를 막기 위해 현재 로그인한 관리자 ID를 확인
        HttpSession session = request.getSession(false);
        String currentLoginId = null;

        if (session != null) {
            ManagerVO loginVO = (ManagerVO) session.getAttribute("loginManager");
            if (loginVO != null) {
                currentLoginId = loginVO.getManagerId();  // 현재 로그인한 ID
            }
        }

        // 변경 대상 ID와 활성화 상태값을 요청에서 읽음
        String targetId = request.getParameter("managerId");
        String activeStr = request.getParameter("active");
        log.info("관리자 상태 변경 요청 - ID: {}, active: {}, currentLoginId: {}", targetId, activeStr, currentLoginId);

        // 값이 하나라도 없으면 400 에러 처리
        if (targetId == null || activeStr == null) {
            log.warn("필수 파라미터 누락- ID: {}, active: {}", targetId, activeStr);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean active = Boolean.parseBoolean(activeStr);

        // 본인 계정(currentLoginId)은 스스로 비활성화(false) 할 수 없게 방어
        if (!active && targetId.equals(currentLoginId)) {
            log.warn("본인 계정 비활성화 시도 차단 - ID: {}", targetId);

            session.setAttribute("error", "본인 계정은 비활성화할 수 없습니다.");
            response.sendRedirect(request.getContextPath() + "/mgr/view?id=" + targetId);
            return;
        }

        try {
            managerDAO.updateActive(active, targetId);
            log.info("관리자 계정 상태 변경 성공 - ID: {}, 활성화: {}", targetId, active);

            String statusText = active ? "활성화" : "비활성화";
            session.setAttribute("successMessage", targetId + " 계정이 " + statusText + " 되었습니다.");

            response.sendRedirect(request.getContextPath() + "/mgr/view?id=" + targetId);

        } catch (Exception e) {
            log.error("관리자 계정 상태 변경 중 오류 발생", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
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
     * 요청 경로 관련 로그를 일관된 형태로 남긴다.
     */
    private void logRequestInfo(HttpServletRequest request) {
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Context Path: {}", request.getContextPath());
        log.info("Servlet Path: {}", request.getServletPath());
        log.info("Path Info: {}", request.getPathInfo());
    }
}
