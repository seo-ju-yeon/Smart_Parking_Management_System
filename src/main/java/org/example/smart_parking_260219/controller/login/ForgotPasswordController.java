package org.example.smart_parking_260219.controller.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dao.ManagerDAO;
import org.example.smart_parking_260219.service.ValidationService;
import org.example.smart_parking_260219.vo.ManagerVO;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 비밀번호 찾기 요청을 처리하는 컨트롤러입니다.
 *
 * <p>
 * 아이디 확인, 이메일 인증번호 발송, 인증번호 검증, 새 비밀번호 설정 과정을 처리합니다.
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
                "/forgot-password/verify",
                "/forgot-password/reset"})
public class ForgotPasswordController extends HttpServlet {

    private static final int MAX_FORGOT_PASSWORD_OTP_ATTEMPTS = 5;

    // OTP 인증 완료 후 새 비밀번호를 설정할 수 있는 권한의 유효시간(5분)
    private static final long PASSWORD_RESET_VALIDITY_MILLIS =
            5 * 60 * 1000L;

    // OTP 인증을 완료하여 비밀번호 변경이 허용된 관리자 아이디를 저장하는 세션 키
    private static final String PASSWORD_RESET_MANAGER_ID_SESSION_KEY =
            "forgotPasswordResetManagerId";

    // 비밀번호 변경 권한이 발급된 시각을 저장하는 세션 키
    private static final String PASSWORD_RESET_GRANTED_AT_SESSION_KEY =
            "forgotPasswordResetGrantedAt";

    private final ManagerDAO managerDAO = ManagerDAO.getInstance();
    private final ValidationService validationService = new ValidationService();

    /**
     * 비밀번호 찾기 페이지로 이동합니다.
     *
     * <p>
     * 이미 로그인된 사용자는 대시보드로 이동시킵니다.
     * </p>
     */
    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {

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

        // 새로고침하면 진행 중이던 비밀번호 변경 권한도 폐기하고 처음부터 다시 시작
        clearForgotPasswordResetState(session);

        req.getRequestDispatcher(
                "/WEB-INF/views/auth/find_password.jsp"
        ).forward(req, resp);
    }

    /**
     * 비밀번호 찾기 관련 POST 요청을 경로별로 처리합니다.
     */
    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        // 요청 경로에 따라 처리할 기능 분기
        String path =
                req.getServletPath() + (req.getPathInfo() != null ? req.getPathInfo() : "");

        switch (path) {
            case "/forgot-password/checkId":  // 아이디 존재 여부 확인 (JSON)
                checkId(req, resp);
                break;
            case "/forgot-password/sendOtp":  // DB 이메일 일치 확인 + OTP 발송 (JSON)
                sendOtp(req, resp);
                break;
            case "/forgot-password/verify":  // OTP 검증 + 비밀번호 변경 권한 발급 (JSON)
                verifyOtpAndGrantReset(req, resp);
                break;
            case "/forgot-password/reset":  // 새 비밀번호 검증 및 변경 (JSON)
                resetPassword(req, resp);
                break;
            default:
                sendJson(resp, false, "잘못된 요청입니다.");
        }
    }

    /**
     * 입력한 관리자 아이디가 존재하는지 확인합니다.
     */
    private void checkId(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {

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
    private void sendOtp(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {

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
        // 새로운 비밀번호 찾기를 시작하므로 이전 OTP 인증으로 받은 변경 권한도 제거
        clearForgotPasswordResetState(existingSession);

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
     * 인증번호를 검증한 뒤 비밀번호 변경 권한을 세션에 발급합니다.
     */
    private void verifyOtpAndGrantReset(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {

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

        // 이전 비밀번호 변경 권한이 남아 있다면 제거하고 새 인증 결과만 저장
        clearForgotPasswordResetState(session);

        // OTP 인증으로 세션 권한이 높아졌으므로 세션 고정 공격 방지를 위해 ID를 교체
        req.changeSessionId();

        // OTP 인증을 완료한 관리자 아이디를 서버 세션에 저장
        // 이후 비밀번호 변경 요청에서는 클라이언트가 보낸 아이디 대신 이 값을 사용
        session.setAttribute(
                PASSWORD_RESET_MANAGER_ID_SESSION_KEY,
                pendingManagerId
        );

        // 비밀번호 변경 권한이 발급된 시간을 저장
        // 이후 /forgot-password/reset 요청에서 5분 만료 여부를 확인
        session.setAttribute(
                PASSWORD_RESET_GRANTED_AT_SESSION_KEY,
                System.currentTimeMillis()
        );

        log.info(
                "비밀번호 찾기 - OTP 인증 완료 및 비밀번호 변경 권한 발급 - ID: {}",
                pendingManagerId
        );

        sendJson(
                resp,
                true,
                "본인 인증이 완료되었습니다. 새 비밀번호를 설정해주세요."
        );
    }

    /**
     * OTP 인증으로 발급된 권한을 확인한 뒤 새 비밀번호로 변경합니다.
     *
     * <p>
     * 관리자 아이디는 요청값으로 받지 않고 OTP 인증 완료 시 세션에 저장한 값을 사용합니다.
     * 비밀번호 입력 오류는 다시 수정할 수 있도록 권한을 유지하고, 권한 만료나 계정 상태
     * 오류가 발생한 경우에는 변경 권한을 삭제합니다.
     * </p>
     */
    private void resetPassword(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {

        // 비밀번호 변경 권한은 기존 세션에만 저장되므로 새 세션을 생성하지 않음
        HttpSession session = req.getSession(false);

        if (session == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            sendJson(
                    resp,
                    false,
                    "비밀번호 변경 권한이 없습니다. 이메일 인증부터 다시 진행해주세요."
            );
            return;
        }

        // 클라이언트가 보낸 아이디가 아니라 OTP 인증 완료 시 서버가 저장한 값을 사용
        String managerId =
                (String) session.getAttribute(
                        PASSWORD_RESET_MANAGER_ID_SESSION_KEY
                );

        Long grantedAt =
                (Long) session.getAttribute(
                        PASSWORD_RESET_GRANTED_AT_SESSION_KEY
                );

        if (managerId == null || grantedAt == null) {
            clearForgotPasswordResetState(session);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            sendJson(
                    resp,
                    false,
                    "비밀번호 변경 권한이 없습니다. 이메일 인증부터 다시 진행해주세요."
            );
            return;
        }

        long elapsedTime = System.currentTimeMillis() - grantedAt;

        // 서버 시간 변경으로 음수가 된 경우도 안전하게 만료로 처리
        if (elapsedTime < 0 || elapsedTime >= PASSWORD_RESET_VALIDITY_MILLIS) {
            clearForgotPasswordResetState(session);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            sendJson(
                    resp,
                    false,
                    "비밀번호 변경 시간이 만료되었습니다. 이메일 인증부터 다시 진행해주세요."
            );
            return;
        }

        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");

        // 입력 오류는 사용자가 바로 수정할 수 있도록 비밀번호 변경 권한을 유지함
        if (newPassword == null || newPassword.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            sendJson(resp, false, "새 비밀번호와 비밀번호 확인을 모두 입력해주세요.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            sendJson(resp, false, "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return;
        }

        // 현재 관리자 등록·수정 기능과 동일한 최소 길이 정책을 서버에서도 검증
        if (newPassword.length() < 4) {
            sendJson(resp, false, "비밀번호는 최소 4자 이상이어야 합니다.");
            return;
        }

        // 권한 발급 후 계정이 삭제되거나 비활성화되었는지 다시 확인
        ManagerVO manager = managerDAO.selectOne(managerId);

        if (manager == null || !manager.isActive()) {
            clearForgotPasswordResetState(session);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            sendJson(
                    resp,
                    false,
                    "유효하지 않은 계정입니다. 이메일 인증부터 다시 진행해주세요."
            );
            return;
        }

        try {
            // ManagerDAO에서 평문 비밀번호를 BCrypt로 해싱한 뒤 저장
            managerDAO.updatePassword(managerId, newPassword);
        } catch (Exception e) {
            // 일시적인 DB 오류라면 권한 유효시간 안에서 다시 시도할 수 있도록 상태를 유지
            log.error("비밀번호 찾기 - 새 비밀번호 저장 실패 - ID: {}", managerId, e);
            sendJson(
                    resp,
                    false,
                    "비밀번호 변경에 실패했습니다. 잠시 후 다시 시도해주세요."
            );
            return;
        }

        // 성공한 변경 권한은 다시 사용할 수 없도록 즉시 삭제
        clearForgotPasswordResetState(session);

        log.info("비밀번호 찾기 - 새 비밀번호 변경 완료 - ID: {}", managerId);
        sendJson(resp, true, "비밀번호가 성공적으로 변경되었습니다.");
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
     * OTP 인증 완료 후 발급된 비밀번호 변경 권한을 세션에서 삭제합니다.
     *
     * <p>
     * {@code forgotPasswordResetManagerId}는 OTP 인증을 완료한 관리자 아이디이고,
     * {@code forgotPasswordResetGrantedAt}은 비밀번호 변경 권한을 받은 시각입니다.
     * OTP 발급 대상과 실패 횟수를 관리하는 상태와는 별개의 정보입니다.
     * </p>
     */
    private void clearForgotPasswordResetState(HttpSession session) {
        if (session == null) {
            return;
        }

        session.removeAttribute(PASSWORD_RESET_MANAGER_ID_SESSION_KEY);
        session.removeAttribute(PASSWORD_RESET_GRANTED_AT_SESSION_KEY);
    }

    /**
     * 일반 JSON 응답을 전송합니다.
     * <p>
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
     * <p>
     * resetOtp가 true이면 클라이언트는 기존 OTP 입력 화면을 초기화하고
     * 사용자가 새로운 인증번호를 요청할 수 있는 상태로 변경합니다.
     */
    private void sendJson(
            HttpServletResponse resp,
            boolean success,
            String message,
            boolean resetOtp
    ) throws IOException {
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
