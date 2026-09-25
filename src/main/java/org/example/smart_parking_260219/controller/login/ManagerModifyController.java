package org.example.smart_parking_260219.controller.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dao.ManagerDAO;
import org.example.smart_parking_260219.vo.ManagerRole;
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
                showModifyNormal(request, response, session);
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
    private void showManagerModify(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) throws ServletException, IOException {

        log.info("ManagerModifyController.showManagerModify() 진입");

        // ADMIN 본인 수정 대상은 요청 파라미터가 아니라 로그인 세션에서 결정함
        Object loginManagerAttribute =
                session.getAttribute("loginManager");

        if (!(loginManagerAttribute instanceof ManagerVO)) {
            // ManagerVO가 아니면 로그인 정보가 정상적이지 않다고 판단
            log.warn("로그인 관리자 정보가 없는 ADMIN 수정 화면 요청");

            response.sendRedirect(
                    request.getContextPath() + "/login"
            );
            return;
        }

        ManagerVO loginManager =
                (ManagerVO) loginManagerAttribute;

        // 필터를 통과했더라도 ADMIN 역할인지 다시 확인함
        if (loginManager.getRole() != ManagerRole.ADMIN) {
            log.warn(
                    "ADMIN 수정 화면 접근 권한 없음 - ID: {}, 역할: {}",
                    loginManager.getManagerId(),
                    loginManager.getRole()
            );

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "ADMIN 계정만 접근할 수 있습니다."
            );
            return;
        }

        // 새 수정 화면에서는 이전에 완료한 수정 OTP 인증을 재사용하지 않음
        clearManagerModifyEmailVerification(session);

        // 요청의 id를 사용하지 않고 현재 로그인한 ADMIN ID로 수정 대상을 고정함
        String managerId =
                loginManager.getManagerId();

        try {
            // 세션 정보가 오래되었을 수 있으므로 DB에서 최신 정보를 다시 조회함
            ManagerVO manager =
                    managerDAO.selectOne(managerId);

            if (manager == null) {
                log.warn(
                        "로그인한 관리자 계정을 찾을 수 없음 - ID: {}",
                        managerId
                );

                invalidateSessionWithMessage(
                        request,
                        session,
                        "계정 정보를 확인할 수 없습니다. 다시 로그인해주세요."
                );

                response.sendRedirect(
                        request.getContextPath() + "/login"
                );
                return;
            }

            // DB의 최신 역할도 ADMIN인지 확인함
            if (manager.getRole() != ManagerRole.ADMIN) {
                log.warn(
                        "세션과 DB의 관리자 역할 불일치 - ID: {}, DB 역할: {}",
                        managerId,
                        manager.getRole()
                );

                invalidateSessionWithMessage(
                        request,
                        session,
                        "계정 권한이 변경되었습니다. 다시 로그인해주세요."
                );

                response.sendRedirect(
                        request.getContextPath() + "/login"
                );
                return;
            }

            request.setAttribute(
                    "manager",
                    manager
            );

            log.info(
                    "ADMIN 본인 수정 정보 조회 완료 - ID: {}",
                    managerId
            );

        } catch (Exception e) {
            log.error(
                    "ADMIN 본인 정보 조회 중 오류 발생 - ID: {}",
                    managerId,
                    e
            );

            request.setAttribute(
                    "error",
                    "관리자 정보를 가져오는 중 오류가 발생했습니다."
            );
        }

        request.getRequestDispatcher(
                "/WEB-INF/views/manager/mgr_modify.jsp"
        ).forward(request, response);
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

        // ADMIN은 관리자 전용 수정 화면을 사용
        if (myManager.getRole() == ManagerRole.ADMIN) {
            log.warn("ADMIN 계정의 /my_modify 접근 차단 → /mgr/modify 로 리다이렉트");
            response.sendRedirect(request.getContextPath() + "/mgr/modify");
            return;
        }

        // 새 수정 화면에서는 이전에 완료한 수정 OTP 인증을 재사용하지 않음
        clearManagerModifyEmailVerification(session);

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

        request.getRequestDispatcher("/WEB-INF/views/manager/mgr_my_modify.jsp").forward(request, response);
    }

    /**
     * 일반 관리자 정보 수정 페이지를 처리한다.
     */
    private void showModifyNormal(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    )
            throws ServletException, IOException {

        log.info("ManagerModifyController.showModifyNormal() 진입");

        // 새 수정 화면에서는 이전에 완료한 수정 OTP 인증을 재사용하지 않음
        clearManagerModifyEmailVerification(session);

        // 요청 ID는 화면에 표시할 수정 대상을 찾는 용도로만 사용함
        String targetId = request.getParameter("id");

        if (targetId == null || targetId.trim().isEmpty()) {
            log.warn("수정할 ID가 없어 목록으로 돌아갑니다.");
            response.sendRedirect(request.getContextPath() + "/mgr/list");
            return;
        }

        targetId = targetId.trim();

        try {
            ManagerVO targetManager = managerDAO.selectOne(targetId);

            if (targetManager == null) {
                log.warn("수정 대상 관리자를 찾을 수 없음 - ID: {}", targetId);
                request.getSession().setAttribute(
                        "error",
                        "존재하지 않는 관리자입니다."
                );
                response.sendRedirect(request.getContextPath() + "/mgr/list");
                return;
            }

            // 이 화면은 NORMAL 계정 수정 전용이므로 ADMIN 계정은 대상으로 허용하지 않음
            if (targetManager.getRole() != ManagerRole.NORMAL) {
                log.warn(
                        "일반 관리자 수정 화면의 대상 역할 불일치 - ID: {}, 역할: {}",
                        targetId,
                        targetManager.getRole()
                );
                request.getSession().setAttribute(
                        "error",
                        "일반 관리자 계정만 이 화면에서 수정할 수 있습니다."
                );
                response.sendRedirect(request.getContextPath() + "/mgr/list");
                return;
            }

            // 존재 여부와 역할을 모두 확인한 대상만 수정 화면에 전달함
            request.setAttribute("manager", targetManager);
            log.info("NORMAL 수정 대상 조회 완료 - ID: {}", targetId);

        } catch (Exception e) {
            log.error("NORMAL 수정 대상 조회 중 오류 발생 - ID: {}", targetId, e);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "관리자 정보를 확인하는 중 오류가 발생했습니다."
            );
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/manager/mgr_modify_normal.jsp").forward(request, response);
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

        // 수정 대상은 세션의 로그인 ADMIN을 기준으로 결정함
        Object loginManagerAttribute =
                session == null
                        ? null
                        : session.getAttribute("loginManager");

        if (!(loginManagerAttribute instanceof ManagerVO)) {
            log.warn("로그인 관리자 정보가 없는 ADMIN 수정 요청");

            response.sendRedirect(
                    request.getContextPath() + "/login"
            );
            return;
        }

        ManagerVO loginManager
                = (ManagerVO) loginManagerAttribute;

        // 필터를 우회한 직접 요청에 대비하여 ADMIN 역할을 다시 확인함
        if (loginManager.getRole() != ManagerRole.ADMIN) {
            log.warn(
                    "ADMIN 본인 수정 권한 없음 - ID: {}, 역할: {}",
                    loginManager.getManagerId(),
                    loginManager.getRole()
            );

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "ADMIN 계정만 접근할 수 있습니다."
            );
            return;
        }

        // 요청 ID는 위조 여부 확인에만 사용함
        String requestManagerId = request.getParameter("id");

        // 실제 수정 대상은 세션의 로그인 ADMIN ID로 고정함
        String managerId = loginManager.getManagerId();

        if (!managerId.equals(requestManagerId)) {
            log.warn(
                    "ADMIN 본인 수정 대상 불일치 - 세션 ID: {}, 요청 ID: {}",
                    managerId,
                    requestManagerId
            );

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "본인 계정만 수정할 수 있습니다."
            );
            return;
        }

        // 로그인 이후 계정이 삭제되거나 역할이 변경되지 않았는지 DB에서 다시 확인함
        try {
            ManagerVO currentManager =
                    managerDAO.selectOne(managerId);

            if (currentManager == null) {
                log.warn(
                        "수정 대상 ADMIN 계정을 찾을 수 없음 - ID: {}",
                        managerId
                );

                invalidateSessionWithMessage(
                        request,
                        session,
                        "계정 정보를 확인할 수 없습니다. 다시 로그인해주세요."
                );

                response.sendRedirect(
                        request.getContextPath() + "/login"
                );
                return;
            }

            if (currentManager.getRole() != ManagerRole.ADMIN) {
                log.warn(
                        "ADMIN 수정 요청의 DB 역할 불일치 - ID: {}, 역할: {}",
                        managerId,
                        currentManager.getRole()
                );

                invalidateSessionWithMessage(
                        request,
                        session,
                        "계정 권한이 변경되었습니다. 다시 로그인해주세요."
                );

                response.sendRedirect(
                        request.getContextPath() + "/login"
                );
                return;
            }

        } catch (Exception e) {
            log.error(
                    "ADMIN 수정 대상 확인 중 오류 발생 - ID: {}",
                    managerId,
                    e
            );

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "관리자 정보를 확인하는 중 오류가 발생했습니다."
            );
            return;
        }

        // 수정할 입력값을 읽음
        String managerName = request.getParameter("name");
        String password = request.getParameter("pw");  // 변경할 새 비밀번호 (비어있을 수 있음)
        String passwordConfirm = request.getParameter("passwordConfirm");  // 확인용 비밀번호
        String email = request.getParameter("email");

        log.info("관리자 정보 수정 요청 - ID: {}", managerId);
        log.info("비밀번호 변경 여부: {}", (password != null && !password.trim().isEmpty() ? "Yes" : "No"));
        log.info("관리자 수정 입력값 확인 - passwordConfirm 입력여부: {}",
                passwordConfirm != null && !passwordConfirm.trim().isEmpty());

        // 필수 입력값이 누락되면 기존 정보를 다시 조회해 수정 화면으로 돌려보냄
        if (managerName == null || managerName.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {

            log.warn("필수 입력값 누락");
            request.setAttribute("error", "이름과 이메일은 필수 입력값입니다.");

            try {
                // 에러 발생 시, 기존 정보를 다시 DB에서 읽어와 화면에 뿌려줘야 입력폼이 유지됨
                ManagerVO manager = managerDAO.selectOne(managerId);
                request.setAttribute("manager", manager);
            } catch (Exception e) {
                log.error("관리자 정보 재조회 실패", e);
            }
            request.getRequestDispatcher("/WEB-INF/views/manager/mgr_modify.jsp").forward(request, response);
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

                request.getRequestDispatcher("/WEB-INF/views/manager/mgr_modify.jsp").forward(request, response);
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
                request.getRequestDispatcher("/WEB-INF/views/manager/mgr_modify.jsp").forward(request, response);
                return;
            }
        }

        // 이메일 형식 검증
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            log.warn("잘못된 이메일 형식 - ID: {}", managerId);
            request.setAttribute("error", "올바른 이메일 형식이 아닙니다.");

            try {
                // 에러 발생 시, 기존 정보를 다시 DB에서 읽어와 화면에 뿌려줘야 입력폼이 유지됨
                ManagerVO manager = managerDAO.selectOne(managerId);
                request.setAttribute("manager", manager);
            } catch (Exception e) {
                log.error("관리자 정보 재조회 실패", e);
            }

            request.getRequestDispatcher("/WEB-INF/views/manager/mgr_modify.jsp").forward(request, response);
            return;
        }

        // JavaScript 검사를 우회한 직접 POST라도 서버의 OTP 인증 완료 상태가 없으면 수정하지 않음
        if (!isManagerModifyEmailVerified(
                session,
                managerId,
                email
        )) {
            log.warn(
                    "ADMIN 본인 수정 차단 - OTP 인증 상태 불일치, 대상 ID: {}",
                    managerId
            );

            clearManagerModifyEmailVerification(session);
            request.setAttribute(
                    "error",
                    "이메일 인증 정보가 확인되지 않습니다. 다시 인증해주세요."
            );

            try {
                request.setAttribute(
                        "manager",
                        managerDAO.selectOne(managerId)
                );
            } catch (Exception e) {
                log.error("ADMIN 정보 재조회 실패 - ID: {}", managerId, e);
            }

            request.getRequestDispatcher(
                    "/WEB-INF/views/manager/mgr_modify.jsp"
            ).forward(request, response);
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
            log.info("ADMIN 본인 정보 수정 완료 - ID: {}", managerId);

            // 사용이 끝난 수정 OTP 인증 상태를 삭제하여 다음 수정에 재사용하지 못하게 함
            clearManagerModifyEmailVerification(session);

            // 이름, 이메일 또는 비밀번호가 변경됐으므로 기존 인증 세션을 종료함
            invalidateSessionWithMessage(
                    request,
                    session,
                    "관리자 정보가 변경되었으니 다시 로그인해주세요."
            );

            response.sendRedirect(
                    request.getContextPath() + "/login"
            );

        } catch (Exception e) {
            log.error("관리자 정보 수정 중 오류 발생", e);
            request.setAttribute("error", "정보 수정 중 오류가 발생했습니다.");

            try {
                ManagerVO manager = managerDAO.selectOne(managerId);
                request.setAttribute("manager", manager);
            } catch (Exception ex) {
                log.error("관리자 정보 재조회 실패", ex);
            }

            request.getRequestDispatcher("/WEB-INF/views/manager/mgr_modify.jsp").forward(request, response);
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

        // 경로 권한은 필터에서 확인하지만 실제 변경 직전에도 요청자가 ADMIN인지 확인함
        HttpSession session = request.getSession(false);
        Object loginManagerAttribute =
                session == null
                        ? null
                        : session.getAttribute("loginManager");

        if (!(loginManagerAttribute instanceof ManagerVO)) {
            log.warn("로그인 관리자 정보가 없는 NORMAL 수정 요청");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        ManagerVO loginManager =
                (ManagerVO) loginManagerAttribute;

        if (loginManager.getRole() != ManagerRole.ADMIN) {
            log.warn(
                    "NORMAL 수정 요청 권한 없음 - 요청자 ID: {}, 역할: {}",
                    loginManager.getManagerId(),
                    loginManager.getRole()
            );
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "일반 관리자 정보를 수정할 권한이 없습니다."
            );
            return;
        }

        // 일반 관리자 수정 폼에서 전달된 입력값을 읽음
        String managerId = request.getParameter("managerId");
        String managerName = request.getParameter("name");
        String password = request.getParameter("pw");
        String email = request.getParameter("email");

        if (managerId == null || managerId.trim().isEmpty()) {
            log.warn("NORMAL 수정 대상 ID 누락");
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "수정할 관리자 ID가 필요합니다."
            );
            return;
        }

        managerId = managerId.trim();

        log.info("수정 요청 수신 - ID: {}, 비밀번호 입력 여부: {}",
                managerId, password != null && !password.isEmpty());

        try {
            ManagerVO existing = managerDAO.selectOne(managerId);

            if (existing == null) {
                log.warn("NORMAL 수정 대상을 찾을 수 없음 - ID: {}", managerId);
                session.setAttribute("error", "존재하지 않는 관리자입니다.");
                response.sendRedirect(request.getContextPath() + "/mgr/list");
                return;
            }

            // hidden 필드가 변조되더라도 NORMAL이 아닌 계정은 수정하지 않음
            if (existing.getRole() != ManagerRole.NORMAL) {
                log.warn(
                        "NORMAL 수정 대상 역할 불일치 - ID: {}, 역할: {}",
                        managerId,
                        existing.getRole()
                );
                session.setAttribute(
                        "error",
                        "일반 관리자 계정만 이 화면에서 수정할 수 있습니다."
                );
                response.sendRedirect(request.getContextPath() + "/mgr/list");
                return;
            }

            // 발급 대상 ID와 인증 이메일이 현재 수정 요청과 모두 일치해야 함
            if (!isManagerModifyEmailVerified(
                    session,
                    managerId,
                    email
            )) {
                log.warn(
                        "NORMAL 수정 차단 - OTP 인증 상태 불일치, 대상 ID: {}",
                        managerId
                );

                clearManagerModifyEmailVerification(session);
                request.setAttribute(
                        "error",
                        "이메일 인증 정보가 확인되지 않습니다. 다시 인증해주세요."
                );
                request.setAttribute("manager", existing);
                request.getRequestDispatcher(
                        "/WEB-INF/views/manager/mgr_modify_normal.jsp"
                ).forward(request, response);
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
            log.info(
                    "ADMIN에 의한 NORMAL 정보 수정 완료 - 대상 ID: {}",
                    managerId
            );

            // 한 번 사용한 수정 OTP 인증 상태는 성공 직후 삭제함
            clearManagerModifyEmailVerification(session);

            // 이 경로는 ADMIN 전용이므로 성공 후 관리자 목록으로 이동함
            session.setAttribute(
                    "successMessage",
                    "정보가 성공적으로 수정되었습니다."
            );
            response.sendRedirect(request.getContextPath() + "/mgr/list");

        } catch (Exception e) {
            log.error("NORMAL 정보 수정 중 오류 발생 - ID: {}", managerId, e);
            session.setAttribute(
                    "error",
                    "정보 수정 중 오류가 발생했습니다."
            );
            response.sendRedirect(request.getContextPath() + "/mgr/list");
        }
    }

    /**
     * 일반 관리자 본인 정보 수정 처리
     *
     * <p>
     * 아이디는 수정할 수 없으며, 세션 ID와 요청 ID가 일치하는 경우에만 이름, 비밀번호, 이메일을 수정한다.
     * 수정 완료 후 변경된 정보로 다시 로그인하도록 세션을 종료한다.
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

        // ADMIN은 관리자 전용 수정 화면을 사용
        if (loginManager.getRole() == ManagerRole.ADMIN) {
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

        log.info("본인 정보 수정 요청 - ID: {}, 비밀번호 변경: {}",
                sessionId, password != null && !password.trim().isEmpty());

        // 필수값 검증
        if (managerName == null || managerName.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
            log.warn("필수 입력값 누락");

            request.setAttribute("error", "이름과 이메일은 필수 입력값입니다.");
            ManagerVO fresh = managerDAO.selectOne(sessionId);
            request.setAttribute("manager", fresh);
            request.getRequestDispatcher("/WEB-INF/views/manager/mgr_my_modify.jsp").forward(request, response);
            return;
        }

        // 비밀번호 길이 검증 (입력한 경우만)
        if (password != null && !password.trim().isEmpty() && password.length() < 4) {
            log.warn("비밀번호 길이 부족: {}", password.length());

            request.setAttribute("error", "비밀번호는 최소 4자 이상이어야 합니다.");
            ManagerVO fresh = managerDAO.selectOne(sessionId);
            request.setAttribute("manager", fresh);
            request.getRequestDispatcher("/WEB-INF/views/manager/mgr_my_modify.jsp").forward(request, response);
            return;
        }

        // 이메일 형식 검증
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            log.warn("잘못된 이메일 형식 - ID: {}", sessionId);

            request.setAttribute("error", "올바른 이메일 형식이 아닙니다.");
            ManagerVO fresh = managerDAO.selectOne(sessionId);
            request.setAttribute("manager", fresh);
            request.getRequestDispatcher("/WEB-INF/views/manager/mgr_my_modify.jsp").forward(request, response);
            return;
        }

        try {
            // 기존 정보 조회
            ManagerVO existing = managerDAO.selectOne(sessionId);
            if (existing == null) {
                request.setAttribute("error", "존재하지 않는 계정입니다.");
                request.getRequestDispatcher("/WEB-INF/views/manager/mgr_my_modify.jsp").forward(request, response);
                return;
            }

            // NORMAL 본인 수정도 실제 대상 ID와 제출 이메일의 OTP 인증을 서버에서 확인함
            if (!isManagerModifyEmailVerified(
                    session,
                    sessionId,
                    email
            )) {
                log.warn(
                        "NORMAL 본인 수정 차단 - OTP 인증 상태 불일치, 대상 ID: {}",
                        sessionId
                );

                clearManagerModifyEmailVerification(session);
                request.setAttribute(
                        "error",
                        "이메일 인증 정보가 확인되지 않습니다. 다시 인증해주세요."
                );
                request.setAttribute("manager", existing);
                request.getRequestDispatcher(
                        "/WEB-INF/views/manager/mgr_my_modify.jsp"
                ).forward(request, response);
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

            // 사용이 끝난 수정 OTP 인증 상태를 삭제하여 다음 수정에 재사용하지 못하게 함
            clearManagerModifyEmailVerification(session);

            // 변경된 정보로 다시 로그인하도록 기존 인증 세션을 종료
            invalidateSessionWithMessage(
                    request,
                    session,
                    "정보가 수정되었습니다. 변경된 정보로 다시 로그인해주세요."
            );
            response.sendRedirect(request.getContextPath() + "/login");

        } catch (Exception e) {
            log.error("본인 정보 수정 중 오류 발생 - ID: {}", sessionId, e);
            request.setAttribute("error", "정보 수정 중 오류가 발생했습니다.");
            try {
                ManagerVO fresh = managerDAO.selectOne(sessionId);
                request.setAttribute("manager", fresh);
            } catch (Exception ex) {
                log.error("재조회 실패", ex);
            }
            request.getRequestDispatcher("/WEB-INF/views/manager/mgr_my_modify.jsp").forward(request, response);
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

        // 경로 권한은 필터에서 확인하지만 상태 변경 직전에도 요청자가 ADMIN인지 확인함
        HttpSession session = request.getSession(false);
        Object loginManagerAttribute =
                session == null
                        ? null
                        : session.getAttribute("loginManager");

        if (!(loginManagerAttribute instanceof ManagerVO)) {
            log.warn("로그인 관리자 정보가 없는 계정 상태 변경 요청");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        ManagerVO loginManager =
                (ManagerVO) loginManagerAttribute;

        if (loginManager.getRole() != ManagerRole.ADMIN) {
            log.warn(
                    "계정 상태 변경 권한 없음 - 요청자 ID: {}, 역할: {}",
                    loginManager.getManagerId(),
                    loginManager.getRole()
            );
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "관리자 계정 상태를 변경할 권한이 없습니다."
            );
            return;
        }

        // 변경 대상 ID와 활성화 상태값을 요청에서 읽음
        String targetId = request.getParameter("managerId");
        String activeStr = request.getParameter("active");

        if (targetId == null || targetId.trim().isEmpty()
                || activeStr == null || activeStr.trim().isEmpty()) {
            log.warn("필수 파라미터 누락- ID: {}, active: {}", targetId, activeStr);
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "변경 대상과 활성화 상태가 필요합니다."
            );
            return;
        }

        targetId = targetId.trim();
        activeStr = activeStr.trim();

        // Boolean.parseBoolean은 잘못된 문자열도 false로 처리하므로 true와 false만 허용함
        if (!"true".equalsIgnoreCase(activeStr)
                && !"false".equalsIgnoreCase(activeStr)) {
            log.warn("잘못된 active 값으로 상태 변경 요청 - 값: {}", activeStr);
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "활성화 상태 값이 올바르지 않습니다."
            );
            return;
        }

        boolean active = Boolean.parseBoolean(activeStr);

        try {
            ManagerVO targetManager = managerDAO.selectOne(targetId);

            if (targetManager == null) {
                log.warn("상태 변경 대상 관리자를 찾을 수 없음 - ID: {}", targetId);
                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        "존재하지 않는 관리자입니다."
                );
                return;
            }

            // 상태 변경은 NORMAL 계정에만 허용하여 ADMIN 계정 잠금을 방지함
            if (targetManager.getRole() != ManagerRole.NORMAL) {
                log.warn(
                        "상태 변경 대상 역할 불일치 - ID: {}, 역할: {}",
                        targetId,
                        targetManager.getRole()
                );
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "일반 관리자 계정만 활성화 상태를 변경할 수 있습니다."
                );
                return;
            }

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

        if (session == null
                || session.getAttribute("loginManager") == null) {

            log.warn("미인증 요청 - 로그인 페이지로 리다이렉트");
            response.sendRedirect(
                    request.getContextPath() + "/login"
            );
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

    // 최종 수정 POST의 대상 ID와 이메일이 OTP 인증 완료 상태와 같은지 확인함
    private boolean isManagerModifyEmailVerified(
            HttpSession session,
            String targetManagerId,
            String submittedEmail
    ) {
        if (session == null
                || targetManagerId == null
                || submittedEmail == null) {
            return false;
        }

        String verifiedManagerId =
                (String) session.getAttribute(
                        "managerModifyVerifiedId"
                );

        String verifiedEmail =
                (String) session.getAttribute(
                        "managerModifyVerifiedEmail"
                );

        return targetManagerId.equals(verifiedManagerId)
                && verifiedEmail != null
                && verifiedEmail.equalsIgnoreCase(
                        submittedEmail.trim()
                );
    }

    // 관리자 수정 OTP의 발송 대상과 인증 완료 상태를 모두 삭제함
    private void clearManagerModifyEmailVerification(
            HttpSession session
    ) {
        if (session == null) {
            return;
        }

        session.removeAttribute("managerModifyPendingId");
        session.removeAttribute("managerModifyVerifiedId");
        session.removeAttribute("managerModifyVerifiedEmail");
    }


    /**
     * 기존 로그인 세션을 종료하고 로그인 화면에 표시할 메시지를
     * 새로운 비인증 세션에 저장한다.
     */
    private void invalidateSessionWithMessage(
            HttpServletRequest request,
            HttpSession session,
            String message
    ) {
        session.invalidate();

        HttpSession messageSession = request.getSession(true);
        messageSession.setAttribute("logoutMessage", message);
    }
}
