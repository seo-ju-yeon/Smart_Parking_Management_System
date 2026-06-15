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
 * 관리자 추가 요청을 처리하는 컨트롤러.
 *
 * <p>기존 ManagerController가 담당하던 /mgr/add GET/POST 기능을 분리한 컨트롤러이다.
 * 화면 호출, 입력값 검증, 중복 ID 검사, 관리자 생성 처리를 모두 담당한다.</p>
 */
@Log4j2
@WebServlet(name = "managerAddController", value = "/mgr/add")
public class ManagerAddController extends HttpServlet {

    // 관리자 추가에 사용하는 DAO
    private final ManagerDAO managerDAO = ManagerDAO.getInstance();

    /**
     * 관리자 추가 화면으로 이동한다.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("=== ManagerAddController doGet() 진입 ===");
        logRequestInfo(request);

        // 관리자 추가 화면은 로긍니한 사용자만 접근
        HttpSession session = getLoginSessionOrRedirect(request, response);
        if (session == null) {
            return;
        }

        log.info("관리자 추가 화면으로 이동");
        request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
    }

    /**
     * 관리자 추가 폼 제출을 처리한다.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("=== ManagerAddController doPost() 진입 ===");
        logRequestInfo(request);

        request.setCharacterEncoding("UTF-8");  // 한글 깨짐 방지

        HttpSession session = getLoginSessionOrRedirect(request, response);
        if (session == null) {
            return;
        }

        log.info("관리자 추가 처리 시작");
        addManager(request, response);  // 관리자 등록
    }

    /**
     * 관리자 추가 처리.
     *
     * <p>입력값 검증 후 신규 관리자 계정을 생성한다. 검증 실패 시 입력했던 값 일부를 request에 다시 담아
     * 추가 화면에서 사용자가 입력 내용을 이어서 확인할 수 있도록 한다.</p>
     */
    private void addManager(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("ManagerAddController.addManager() 진입");

        // 관리자 추가 폼에서 전달된 입력값을 읽음
        String managerId = request.getParameter("id");
        String managerName = request.getParameter("name");
        String password = request.getParameter("pw");
        String passwordConfirm = request.getParameter("passwordConfirm");
        String email = request.getParameter("email");

        log.info("관리자 추가 요청 - ID: {}, 이름: {}, 이메일: {}", managerId, managerName, email);
        log.info("관리자 추가 입력값 확인 - password 입력여부: {}, passwordConfirm 입력여부: {}",
                password != null && !password.trim().isEmpty(),
                passwordConfirm != null && !passwordConfirm.trim().isEmpty());

        // 필수 입력값 검증
        if (managerId == null || managerId.trim().isEmpty() ||
                managerName == null || managerName.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {

            log.warn("필수 입력값 누락");
            request.setAttribute("error", "모든 필드를 입력해주세요.");
            // 검증 실패 시 사용자가 입력한 값을 다시 화면에 표시하기 위헤 request에 보관
            request.setAttribute("managerId", managerId);
            request.setAttribute("managerName", managerName);
            request.setAttribute("email", email);

            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
            return;
        }

        // 비밀번호 일치 확인
        if (!password.equals(passwordConfirm)) {
            log.warn("비밀번호 불일치");
            request.setAttribute("error", "비밀번호가 일치하지 않습니다.");
            request.setAttribute("managerId", managerId);
            request.setAttribute("managerName", managerName);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
            return;
        }

        // 비밀번호 길이 검증
        if (password.length() < 4) {
            log.warn("비밀번호 길이 부족: {}", password.length());
            request.setAttribute("error", "비밀번호는 최소 4자 이상이어야 합니다.");
            request.setAttribute("managerId", managerId);
            request.setAttribute("managerName", managerName);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
            return;
        }

        // 이메일 형식 검증
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            log.warn("잘못된 이메일 형식: {}", email);
            request.setAttribute("error", "올바른 이메일 형식이 아닙니다.");
            request.setAttribute("managerId", managerId);
            request.setAttribute("managerName", managerName);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
            return;
        }

        try {
            // 중복 ID 체크 (신규 관리자 ID가 이미 존재하는지 확인)
            ManagerVO existingManager = managerDAO.selectOne(managerId);
            if (existingManager != null) {
                log.warn("중복된 관리자 ID: {}", managerId);
                request.setAttribute("error", "이미 사용 중인 아이디입니다.");
                request.setAttribute("managerId", managerId);
                request.setAttribute("managerName", managerName);
                request.setAttribute("email", email);
                request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
                return;
            }

            // 비밀번호는 평문으로 전달하고, DAO 계층에서 BCrypt로 암호화
            ManagerVO newManager = ManagerVO.builder()
                    .managerId(managerId)
                    .managerName(managerName)
                    .password(password)
                    .email(email)
                    .active(true)  // 활성상태를 기본값으로
                    .build();

            // DB에 저장
            managerDAO.insertManager(newManager);
            log.info("관리자 추가 성공 - ID: {}, 이름: {}", managerId, managerName);

            // 성공 메시지와 함께 대시보드로 리다이렉트
            HttpSession session = request.getSession();
            session.setAttribute("successMessage", "관리자가 성공적으로 추가되었습니다.");

            log.info("대시보드로 리다이렉트: {}/dashboard", request.getContextPath());
            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (Exception e) {
            log.error("관리자 추가 중 오류 발생", e);
            request.setAttribute("error", "관리자 추가 중 오류가 발생했습니다: " + e.getMessage());
            request.setAttribute("managerId", managerId);
            request.setAttribute("managerName", managerName);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/views/mgr_add.jsp").forward(request, response);
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
