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

    private static final int MAX_FORGOT_PASSWORD_OTP_ATTEMPTS = 5;

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
        if (session != null
                && Boolean.TRUE.equals(session.getAttribute("fullyAuthenticated"))) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }

        // 새로고침 후 화면과 서버의 인증 상태가 달라지지 않도록 기존 OTP를 폐기
        invalidateForgotPasswordOtpState(session);

        req.getRequestDispatcher(
                "/WEB-INF/views/auth/find_password.jsp"
        ).forward(req, resp);
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

        log.info("비밀번호 찾기 - OTP 발송 요청 - ID: {}", managerId);

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
            log.warn("비밀번호 찾기 - 이메일 불일치 - ID: {}", managerId);
            sendJson(resp, false, "등록된 이메일 주소와 일치하지 않습니다.");
            return;
        }

        // DB 조회·OTP 발송·세션 저장에서 동일한 값을 사용하도록 정규화
        String pendingManagerId = managerId.trim();
        String pendingEmail = registeredEmail.trim();

        // 재발급 요청이면 이전 OTP와 세션의 발급 상태를 모두 폐기
        HttpSession existingSession = req.getSession(false);
        invalidateForgotPasswordOtpState(existingSession);

        // 비밀번호 찾기용 인증번호 발송
        try {
            validationService.sendAuthCode(
                    pendingEmail,
                    ValidationService.Purpose.FORGOT_PASSWORD
            );

            // 메일 발송까지 성공한 경우에만 비밀번호 찾기 OTP 세션을 생성
            HttpSession session = req.getSession();

            session.setAttribute(
                    "forgotPasswordPendingManagerId",
                    pendingManagerId
            );

            session.setAttribute(
                    "forgotPasswordPendingEmail",
                    pendingEmail
            );

            session.setAttribute(
                    "forgotPasswordOtpAttemptCount",
                    0
            );

            log.info(
                    "비밀번호 찾기 - OTP 발송 완료 - ID: {}",
                    managerId
            );
            sendJson(
                    resp,
                    true,
                    "인증번호가 이메일로 발송되었습니다."
            );
        } catch (Exception e) {
            // DB 저장 후 메일 발송에 실패했을 수 있으므로 사용할 수 없는 OTP를 정리
            try {
                validationService.invalidateAuthCode(pendingEmail);
            } catch (Exception cleanupError) {
                log.error(
                        "비밀번호 찾기 - 발송 실패 OTP 정리 실패 - ID: {}",
                        pendingManagerId,
                        cleanupError
                );
            }

            // 기존 세션이 있다면 비밀번호 찾기 OTP 상태도 제거
            clearForgotPasswordOtpState(
                    req.getSession(false)
            );

            log.error(
                    "비밀번호 찾기 - OTP 발송 실패 - ID: {}",
                    managerId,
                    e
            );
            sendJson(resp,
                    false,
                    "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요."
            );
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

        // OTP 발급 상태가 저장된 기존 세션 조회
        HttpSession session = req.getSession(false);

        if (session == null) {
            log.warn("비밀번호 찾기 - 세션 없이 OTP 검증 요청");

            sendJson(
                    resp,
                    false,
                    "인증번호를 먼저 발급받아주세요.",
                    true
            );
            return;
        }

        // OTP 발급 성공 시 서버가 저장한 관리자 아이디와 이메일 조회
        String pendingManagerId =
                (String) session.getAttribute(
                        "forgotPasswordPendingManagerId"
                );

        String pendingEmail =
                (String) session.getAttribute(
                        "forgotPasswordPendingEmail"
                );

        // 이전 검증 요청까지 누적된 OTP 실패 횟수 조회
        Integer attemptCount =
                (Integer) session.getAttribute(
                        "forgotPasswordOtpAttemptCount"
                );

        int failedAttempts = attemptCount == null ? 0 : attemptCount;

        // 발급 상태가 없으면 클라이언트가 보낸 값만으로 OTP를 검증하지 않음
        if (pendingManagerId == null || pendingEmail == null) {
            log.warn("비밀번호 찾기 - 세션에 OTP 발급 상태 없음");

            sendJson(
                    resp,
                    false,
                    "인증번호를 먼저 발급받아주세요.",
                    true
            );
            return;
        }

        // OTP를 발급받은 계정과 현재 검증 요청의 계정이 같은지 확인
        boolean managerIdMatched =
                pendingManagerId.equals(managerId.trim());

        boolean emailMatched =
                pendingEmail.equalsIgnoreCase(inputEmail.trim());

        if (!managerIdMatched || !emailMatched) {
            log.warn("비밀번호 찾기 - OTP 발급 정보와 검증 요청 불일치");

            sendJson(
                    resp,
                    false,
                    "인증번호를 발급받은 계정 정보와 일치하지 않습니다."
            );
            return;
        }

        // 클라이언트 요청값이 아니라 서버 세션에 저장된 아이디로 계정 조회
        ManagerVO managerVO = managerDAO.selectOne(pendingManagerId);

        if (managerVO == null || !managerVO.isActive()) {
            invalidateForgotPasswordOtpState(session);

            sendJson(resp, false, "유효하지 않은 계정입니다.", true);
            return;
        }

        String registeredEmail = managerVO.getEmail();

        if (registeredEmail == null || registeredEmail.trim().isEmpty()) {
            invalidateForgotPasswordOtpState(session);

            sendJson(resp, false, "등록된 이메일 정보가 없습니다.", true);
            return;
        }

        // OTP 발급 후 DB 이메일이 변경되지 않았는지 다시 확인
        if (!pendingEmail.equalsIgnoreCase(registeredEmail.trim())) {
            log.warn("비밀번호 찾기 - OTP 발급 후 계정 이메일 변경 감지");

            invalidateForgotPasswordOtpState(session);

            sendJson(
                    resp,
                    false,
                    "계정 정보가 변경되었습니다. 인증번호를 다시 발급받아주세요.",
                    true
            );
            return;
        }

        // 클라이언트 이메일이 아니라 서버 세션에 저장된 발급 이메일로 OTP 검증
        ValidationService.VerificationResult verificationResult =
                validationService.verifyAuthCode(
                        pendingEmail,
                        inputOtp.trim()
                );

        // 실제 OTP가 틀린 경우에만 실패 횟수를 증가시킴
        if (verificationResult == ValidationService.VerificationResult.INVALID_CODE) {

            int updatedAttempts = failedAttempts + 1;

            log.warn(
                    "비밀번호 찾기 - OTP 불일치 - 실패 횟수: {}/{}",
                    updatedAttempts,
                    MAX_FORGOT_PASSWORD_OTP_ATTEMPTS
            );

            if (updatedAttempts >= MAX_FORGOT_PASSWORD_OTP_ATTEMPTS) {

                // 실패 제한에 도달한 OTP와 세션의 발급 상태를 함께 폐기
                invalidateForgotPasswordOtpState(session);

                sendJson(
                        resp,
                        false,
                        "인증번호 입력 가능 횟수를 초과했습니다. 새 인증번호를 발급받아주세요.",
                        true
                );
                return;
            }

            // 아직 제한에 도달하지 않았다면 증가한 실패 횟수를 세션에 저장
            session.setAttribute(
                    "forgotPasswordOtpAttemptCount",
                    updatedAttempts
            );

            int remainingAttempts = MAX_FORGOT_PASSWORD_OTP_ATTEMPTS - updatedAttempts;

            sendJson(
                    resp,
                    false,
                    "인증번호가 일치하지 않습니다. 남은 입력 횟수: "
                            + remainingAttempts
                            + "회"
            );
            return;
        }

        // 만료된 OTP는 DB와 세션에서 모두 폐기
        if (verificationResult == ValidationService.VerificationResult.EXPIRED) {
            invalidateForgotPasswordOtpState(session);

            log.warn("비밀번호 찾기 - OTP 만료");

            sendJson(
                    resp,
                    false,
                    "인증번호가 만료되었습니다. 새 인증번호를 발급받아주세요.",
                    true
            );
            return;
        }

        // DB에 인증정보가 없으면 남아 있는 세션 상태도 삭제
        if (verificationResult == ValidationService.VerificationResult.NOT_FOUND) {
            invalidateForgotPasswordOtpState(session);

            log.warn("비밀번호 찾기 - OTP 발급 정보 없음");

            sendJson(
                    resp,
                    false,
                    "사용 가능한 인증번호가 없습니다. 새 인증번호를 발급받아주세요.",
                    true
            );
            return;
        }

        // 정의되지 않은 검증 결과가 추가되더라도 비밀번호 발급으로 넘어가지 않도록 차단
        if (verificationResult != ValidationService.VerificationResult.SUCCESS) {
            sendJson(resp,
                    false,
                    "인증번호 검증에 실패했습니다."
            );
            return;
        }

        // 인증에 성공한 OTP는 다시 사용할 수 없도록 즉시 폐기
        invalidateForgotPasswordOtpState(session);

        // 임시 비밀번호 생성
        String tempPassword = generateTempPassword();
        log.info("비밀번호 찾기 - 임시 비밀번호 생성 완료 - ID: {}", managerId);

        // 임시 비밀번호 DB 저장
        try {
            // OTP를 발급받은 서버 세션의 관리자 아이디를 기준으로 비밀번호를 변경
            managerDAO.updatePassword(
                    pendingManagerId,
                    tempPassword
            );
            log.info("비밀번호 찾기 - 임시 비밀번호 DB 저장 완료 - ID: {}", managerId);
        } catch (Exception e) {
            log.error("비밀번호 찾기 - DB 저장 실패 - ID: {}", managerId, e);
            sendJson(
                    resp,
                    false,
                    "임시 비밀번호 저장에 실패했습니다. 잠시 후 다시 시도해주세요.",
                    true
            );
            return;
        }

        // 임시 비밀번호 이메일 발송
        try {
            String title = "[Smart Parking] 임시 비밀번호가 발급되었습니다.";
            String body = validationService.buildTempPasswordBody(tempPassword);
            // OTP를 발급받은 서버 세션의 이메일로 임시 비밀번호 발송
            mailService.sendMailWithHtml(
                    title,
                    body,
                    pendingEmail
            );
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
     * 비밀번호 찾기 OTP를 DB에서 폐기하고 세션의 발급 상태도 삭제합니다.
     */
    private void invalidateForgotPasswordOtpState(HttpSession session) {

        // clearForgotPasswordOtpState(): 세션 정보만 삭제
        // invalidateForgotPasswordOtpState(): DB의 OTP를 삭제하고 세션 정보도 삭제

        if (session == null) {
            return;
        }

        String pendingEmail =
                (String) session.getAttribute(
                        "forgotPasswordPendingEmail"
                );

        try {
            if (pendingEmail != null && !pendingEmail.isBlank()) {
                validationService.invalidateAuthCode(pendingEmail);
            }
        } catch (Exception e) {
            // DB 정리에 실패하더라도 세션의 인증 상태는 반드시 제거
            log.error("비밀번호 찾기 - OTP 폐기 실패", e);
        } finally {
            clearForgotPasswordOtpState(session);
        }
    }

    /**
     * 비밀번호 찾기 OTP의 발급 대상과 실패 횟수를 세션에서 삭제합니다.
     */
    private void clearForgotPasswordOtpState(HttpSession session) {
        if (session == null) {
            return;
        }

        session.removeAttribute("forgotPasswordPendingManagerId");
        session.removeAttribute("forgotPasswordPendingEmail");
        session.removeAttribute("forgotPasswordOtpAttemptCount");
    }

    /**
     * 일반 JSON 응답을 전송합니다.
     *
     * OTP 화면 초기화가 필요하지 않은 기존 응답에서 사용합니다.
     * resetOtp의 기본값을 false로 지정하여 아래의 실제 응답 생성 메서드에 전달합니다.
     */
    private void sendJson(
            HttpServletResponse resp,
            boolean success,
            String message
    ) throws IOException {
        sendJson(resp, success, message, false);
    }

    /**
     * OTP 화면 초기화 여부를 포함한 JSON 응답을 전송합니다.
     *
     * resetOtp가 true이면 클라이언트는 기존 OTP 입력 화면을 초기화하고
     * 사용자가 새로운 인증번호를 요청할 수 있는 상태로 변경합니다.
     */
    private void sendJson(
            HttpServletResponse resp,
            boolean success,
            String message,
            boolean resetOtp
    ) throws IOException{
        PrintWriter out = resp.getWriter();

        // 메시지에 큰따옴표가 포함되어도 JSON 형식이 깨지지 않도록 이스케이프 처리
        String safeMessage = message.replace("\"", "\\\"");

        // success, 안내 메시지, OTP 화면 초기화 여부를 JSON으로 전달
        out.write(
                "{\"success\":" + success
                        + ",\"message\":\"" + safeMessage + "\""
                        + ",\"resetOtp\":" + resetOtp
                        + "}"
        );
        out.flush();
    }
}
