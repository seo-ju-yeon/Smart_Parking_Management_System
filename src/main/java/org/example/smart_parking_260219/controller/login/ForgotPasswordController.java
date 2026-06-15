package org.example.smart_parking_260219.controller.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dao.ManagerDAO;
import org.example.smart_parking_260219.mail.MailService;
import org.example.smart_parking_260219.service.ValidationService;
import org.example.smart_parking_260219.vo.ManagerVO;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;

/**
 * 비밀번호 찾기 요청을 처리하는 컨트롤러입니다.
 *
 * <p>
 * 아이디 확인, 이메일 인증번호 발송, 인증번호 검증, 임시 비밀번호 발급 과정을 처리합니다.
 * </p>
 *
 * <p>
 * 모든 POST 요청은 JSON 형태로 응답합니다.
 * </p>
 */
@Log4j2
@WebServlet(name = "forgotPasswordController",
        value = {"/forgot-password",
                "/forgot-password/checkId",
                "/forgot-password/sendOtp",
                "/forgot-password/verify"})
public class ForgotPasswordController extends HttpServlet {

    private final ManagerDAO managerDAO = ManagerDAO.getInstance();
    private final ValidationService validationService = new ValidationService();
    private final MailService mailService = new MailService();

    /**
     * 비밀번호 찾기 페이지로 이동합니다.
     *
     * <p>
     * 이미 로그인된 사용자는 대시보드로 이동시킵니다.
     * </p>
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        // 이미 로그인된 상태면 대시보드로 리다이렉트
        HttpSession session = req.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute("fullyAuthenticated"))) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }

        req.getRequestDispatcher("/WEB-INF/views/find_password.jsp").forward(req, resp);
    }

    /**
     * 비밀번호 찾기 관련 POST 요청을 경로별로 처리합니다.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        // 요청 경로에 따라 처리할 기능 분기
        String path = req.getServletPath() +
                (req.getPathInfo() != null ? req.getPathInfo() : "");

        switch (path) {
            case "/forgot-password/checkId":  // 아이디 존재 여부 확인 (JSON)
                checkId(req, resp);
                break;
            case "/forgot-password/sendOtp":  // DB 이메일 일치 확인 + OTP 발송 (JSON)
                sendOtp(req, resp);
                break;
            case "/forgot-password/verify":  // OTP 검증 + 임시 비밀번호 발급 (JSON)
                verifyAndIssue(req, resp);
                break;
            default:
                sendJson(resp, false, "잘못된 요청입니다.");
        }
    }

    /**
     * 입력한 관리자 아이디가 존재하는지 확인합니다.
     */
    private void checkId(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String managerId = req.getParameter("managerId");
        log.info("비밀번호 찾기 - 아이디 조회: {}", managerId);

        // 아이디 입력 여부 확인
        if (managerId == null || managerId.trim().isEmpty()) {
            sendJson(resp, false, "아이디를 입력해주세요.");
            return;
        }

        // 관리자 계정 조회
        ManagerVO manager = managerDAO.selectOne(managerId.trim());

        if (manager == null) {
            log.warn("비밀번호 찾기 - 존재하지 않는 아이디: {}", managerId);
            sendJson(resp, false, "존재하지 않는 아이디입니다.");
            return;
        }

        // 비활성화 계정은 비밀번호 찾기 차단
        if (!manager.isActive()) {
            log.warn("비밀번호 찾기 - 비활성화된 계정: {}", managerId);
            sendJson(resp, false, "비활성화된 계정입니다. 최고 관리자에게 문의하세요.");
            return;
        }

        log.info("비밀번호 찾기 - 아이디 확인 완료: {}", managerId);
        sendJson(resp, true, "아이디가 확인되었습니다.");
    }

    /**
     * 등록된 이메일과 입력한 이메일을 비교한 뒤 인증번호를 발송합니다.
     */
    private void sendOtp(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String managerId = req.getParameter("managerId");
        String inputEmail = req.getParameter("email");

        log.info("비밀번호 찾기 - OTP 발송 요청 - ID: {}, Email: {}", managerId, inputEmail);

        // 필수 입력값 확인
        if (managerId == null || managerId.trim().isEmpty()) {
            sendJson(resp, false, "아이디 정보가 없습니다. 처음부터 다시 시도해주세요.");
            return;
        }
        if (inputEmail == null || inputEmail.trim().isEmpty()) {
            sendJson(resp, false, "이메일을 입력해주세요.");
            return;
        }

        // 관리자 계정 유효성 확인
        ManagerVO manager = managerDAO.selectOne(managerId.trim());
        if (manager == null || !manager.isActive()) {
            sendJson(resp, false, "유효하지 않은 계정입니다.");
            return;
        }

        // DB에 등록된 이메일 확인
        String registeredEmail = manager.getEmail();
        if (registeredEmail == null || registeredEmail.trim().isEmpty()) {
            log.error("비밀번호 찾기 - 등록된 이메일 없음 - ID: {}", managerId);
            sendJson(resp, false, "등록된 이메일 정보가 없습니다. 최고 관리자에게 문의하세요.");
            return;
        }

        // 입력한 이메일과 DB 이메일 비교
        if (!inputEmail.trim().equalsIgnoreCase(registeredEmail.trim())) {
            log.warn("비밀번호 찾기 - 이메일 불일치 - ID: {}, 입력: {}, 등록: {}",
                    managerId, inputEmail, registeredEmail);
            sendJson(resp, false, "등록된 이메일 주소와 일치하지 않습니다.");
            return;
        }

        // 비밀번호 찾기용 인증번호 발송
        try {
            validationService.sendAuthCode(registeredEmail.trim(), ValidationService.Purpose.FORGOT_PASSWORD);
            log.info("비밀번호 찾기 - OTP 발송 완료 - ID: {}", managerId);
            sendJson(resp, true, "인증번호가 이메일로 발송되었습니다.");
        } catch (Exception e) {
            log.error("비밀번호 찾기 - OTP 발송 실패 - ID: {}", managerId, e);
            sendJson(resp, false, "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 인증번호를 검증한 뒤 임시 비밀번호를 발급합니다.
     */
    private void verifyAndIssue(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String managerId = req.getParameter("managerId");
        String inputEmail = req.getParameter("email");
        String inputOtp = req.getParameter("otp");

        log.info("비밀번호 찾기 - OTP 검증 요청 - ID: {}", managerId);

        // 필수 입력값 확인
        if (managerId == null || managerId.trim().isEmpty() ||
                inputEmail == null || inputEmail.trim().isEmpty() ||
                inputOtp == null || inputOtp.trim().isEmpty()) {
            sendJson(resp, false, "필수 정보가 누락되었습니다.");
            return;
        }

        // 관리자 계정과 이메일이 실제로 연결되어 있는지 확인
        ManagerVO managerVO = managerDAO.selectOne(managerId.trim());

        if (managerVO == null || !managerVO.isActive()) {
            sendJson(resp, false, "유효하지 않은 계정입니다.");
            return;
        }

        String registeredEmail = managerVO.getEmail();

        if (registeredEmail == null || registeredEmail.trim().isEmpty()) {
            sendJson(resp, false, "등록된 이메일 정보가 없습니다.");
            return;
        }

        if (!inputEmail.trim().equalsIgnoreCase(registeredEmail.trim())) {
            sendJson(resp, false, "등록된 이메일 주소와 일치하지 않습니다.");
            return;
        }

        // 인증번호 일치 여부와 만료 시간 확인
        boolean otpValid = validationService.verifyAuthCode(inputEmail.trim(), inputOtp.trim());
        if (!otpValid) {
            log.warn("비밀번호 찾기 - OTP 불일치 또는 만료 - ID: {}", managerId);
            sendJson(resp, false, "인증번호가 일치하지 않거나 만료되었습니다.");
            return;
        }

        // 임시 비밀번호 생성
        String tempPassword = generateTempPassword();
        log.info("비밀번호 찾기 - 임시 비밀번호 생성 완료 - ID: {}", managerId);

        // 임시 비밀번호 DB 저장
        try {
            managerDAO.updatePassword(managerId.trim(), tempPassword);
            log.info("비밀번호 찾기 - 임시 비밀번호 DB 저장 완료 - ID: {}", managerId);
        } catch (Exception e) {
            log.error("비밀번호 찾기 - DB 저장 실패 - ID: {}", managerId, e);
            sendJson(resp, false, "임시 비밀번호 저장에 실패했습니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        // 임시 비밀번호 이메일 발송
        try {
            String title = "[Smart Parking] 임시 비밀번호가 발급되었습니다.";
            String body = validationService.buildTempPasswordBody(tempPassword);
            mailService.sendMailWithHtml(title, body, inputEmail.trim());
            log.info("비밀번호 찾기 - 임시 비밀번호 이메일 발송 완료 - ID: {}", managerId);
        } catch (Exception e) {
            // DB 저장은 완료된 상태이므로 이메일 실패만 로그로 기록
            log.error("비밀번호 찾기 - 임시 비밀번호 이메일 발송 실패 - ID: {}", managerId, e);
        }

        sendJson(resp, true, "임시 비밀번호가 이메일로 발송되었습니다.");
    }

    /**
     * 임시 비밀번호를 생성합니다.
     *
     * @return 영문 대소문자와 숫자로 구성된 10자리 임시 비밀번호
     */
    private String generateTempPassword() {
        final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

        // 혼동하기 쉬운 문자(0, O, 1, I, l) 제외
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);

        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * JSON 응답을 전송합니다.
     */
    private void sendJson(HttpServletResponse resp, boolean success, String message)
            throws IOException {
        PrintWriter out = resp.getWriter();

        // JSON 문자열 깨짐 방지를 위해 큰따옴표 이스케이프
        String safeMessage = message.replace("\"", "\\\"");

        out.write("{\"success\":" + success + ",\"message\":\"" + safeMessage + "\"}");
        out.flush();
    }
}