package org.example.smart_parking_260219.controller.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.service.ValidationService;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 이메일 인증번호 검증 요청을 처리하는 컨트롤러입니다.
 *
 * <p>
 * 클라이언트가 전달한 이메일과 인증번호를 검증하고,
 * 인증 성공 여부를 JSON 형태로 응답합니다.
 * </p>
 */
@Log4j2
@WebServlet(name = "verifyAuthCodeController", value = {"/auth/verify"})
public class VerifyAuthCodeController extends HttpServlet {

    private static final int MAX_AUTH_CODE_ATTEMPTS = 5;

    private final ValidationService validationService = new ValidationService();

    /**
     * 이메일 인증번호 검증 POST 요청을 처리합니다.
     *
     * <p>
     * 요청 파라미터에서 이메일과 인증번호를 읽고,
     * ValidationService를 통해 발급 여부와 만료 여부를 검증합니다.
     * </p>
     *
     * @param req  클라이언트 요청 객체
     * @param resp 서버 응답 객체
     * @throws ServletException 서블릿 처리 중 문제가 발생한 경우
     * @throws IOException      응답 작성 중 입출력 문제가 발생한 경우
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");  // 한글 깨짐 방지

        // 이후 모든 분기에서 JSON을 반환할 수 있도록 응답 형식을 먼저 설정
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();

        // 인증 검증에 필요한 요청 파라미터 추출
        String email = req.getParameter("email");
        String code = req.getParameter("code");

        // 공용 이메일 OTP 상태가 저장된 기존 로그인 세션 조회
        HttpSession session = req.getSession(false);

        if (session == null) {
            log.warn("세션이 없는 이메일 인증번호 검증 요청");

            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write(
                    "{\"success\": false, \"message\": \"로그인 세션이 만료되었습니다.\"}"
            );
            out.flush();
            return;
        }

        // OTP 발송 시 저장한 이메일과 현재까지의 실패 횟수 조회
        String authCodePendingEmail =
                (String) session.getAttribute("authCodePendingEmail");

        Integer attemptCount =
                (Integer) session.getAttribute("authCodeAttemptCount");

        int failedAttempts =
                attemptCount == null ? 0 : attemptCount;

        // 정상적으로 발송된 OTP 상태가 없으면 DB 검증을 진행하지 않음
        if (authCodePendingEmail == null) {
            log.warn("세션에 이메일 OTP 발송 상태 없음");

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(
                    "{\"success\": false, \"message\": \"인증번호를 먼저 발급받아주세요.\"}"
            );
            out.flush();
            return;
        }

        if (email == null || email.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(
                    "{\"success\": false, \"message\": \"이메일을 입력해주세요.\"}"
            );
            out.flush();
            return;
        }

        // 클라이언트가 이메일을 바꿔 다른 이메일의 OTP를 검증하지 못하도록 세션 값과 비교
        if (!authCodePendingEmail.equalsIgnoreCase(email.trim())) {
            log.warn("인증번호 발송 이메일과 검증 요청 이메일 불일치");

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(
                    "{\"success\": false, \"message\": \"인증번호를 발급받은 이메일과 일치하지 않습니다.\"}"
            );
            out.flush();
            return;
        }

        // 값이 없는 요청은 OTP 추측 실패로 세지 않고 입력 오류로 처리
        if (code == null || code.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(
                    "{\"success\": false, \"message\": \"인증번호를 입력해주세요.\"}"
            );
            out.flush();
            return;
        }

        // 관리자 등록 최종 POST 검증에 사용하는 별도 이메일 상태 조회
        String managerAddPendingEmail =
                (String) session.getAttribute("managerAddPendingEmail");

        log.info("인증 검증 요청");

        // 요청 이메일 대신 서버 세션에 저장된 발송 이메일을 사용하여 DB의 OTP 검증
        ValidationService.VerificationResult verificationResult =
                validationService.verifyAuthCode(
                        authCodePendingEmail,
                        code.trim()
                );

        if (verificationResult == ValidationService.VerificationResult.SUCCESS) {
            // 관리자 등록 인증인 경우 발송 이메일과 검증 이메일이 같은 상태인지 확인
            boolean isManagerAddEmailMatched =
                    managerAddPendingEmail != null
                            && managerAddPendingEmail.equalsIgnoreCase(
                                    authCodePendingEmail
                            );

            // 인증에 사용된 OTP는 다시 사용할 수 없도록 DB에서 즉시 삭제
            validationService.invalidateAuthCode(
                    authCodePendingEmail
            );

            // 공용 OTP 상태만 삭제하고 관리자 등록 완료 증명은 최종 등록 POST까지 유지
            clearCommonEmailOtpState(session);

            if (isManagerAddEmailMatched) {
                // 관리자 등록 POST에서 다시 검사할 인증 완료 이메일 저장
                session.setAttribute(
                        "managerAddVerifiedEmail",
                        managerAddPendingEmail
                );

                // 발송 대기 상태는 인증 완료 상태로 전환되었으므로 삭제
                session.removeAttribute("managerAddPendingEmail");

                log.info("관리자 등록 이메일 인증 완료 상태 저장");
            }

            log.info("인증 성공 - 사용한 인증번호 폐기 완료");
            out.write("{\"success\": true, \"message\": \"인증 성공\"}");

        } else if (verificationResult
                == ValidationService.VerificationResult.INVALID_CODE) {

            int updatedAttempts = failedAttempts + 1;

            log.warn(
                    "인증번호 불일치 - 실패 횟수: {}/{}",
                    updatedAttempts,
                    MAX_AUTH_CODE_ATTEMPTS
            );

            if (updatedAttempts >= MAX_AUTH_CODE_ATTEMPTS) {
                // 최대 실패 횟수에 도달한 OTP는 DB와 세션에서 모두 폐기
                validationService.invalidateAuthCode(
                        authCodePendingEmail
                );
                clearEmailOtpState(session);

                out.write(
                        "{\"success\": false, \"message\": \"인증번호 입력 가능 횟수를 초과했습니다. 새 인증번호를 발급받아주세요.\"}"
                );
            } else {
                // 제한에 도달하지 않았다면 증가한 횟수를 다음 요청에서도 사용할 수 있도록 저장
                session.setAttribute(
                        "authCodeAttemptCount",
                        updatedAttempts
                );

                int remainingAttempts =
                        MAX_AUTH_CODE_ATTEMPTS - updatedAttempts;

                out.write(
                        "{\"success\": false, \"message\": \"인증번호가 일치하지 않습니다. 남은 입력 횟수: "
                                + remainingAttempts
                                + "회\"}"
                );
            }

        } else if (verificationResult
                == ValidationService.VerificationResult.EXPIRED) {

            // 만료된 OTP는 DB와 세션에서 모두 삭제하여 이후 요청에서 재사용하지 못하게 함
            validationService.invalidateAuthCode(
                    authCodePendingEmail
            );
            clearEmailOtpState(session);

            log.warn("인증번호 만료 - 인증정보 폐기 완료");
            out.write(
                    "{\"success\": false, \"message\": \"인증번호가 만료되었습니다. 다시 발급받아주세요.\"}"
            );

        } else {
            // DB에는 OTP가 없지만 세션에 발송 상태가 남은 경우 세션도 함께 정리
            clearEmailOtpState(session);

            log.warn("발급된 인증정보 없음 - 세션 인증 상태 초기화");
            out.write(
                    "{\"success\": false, \"message\": \"발급된 인증번호가 없습니다. 인증번호를 먼저 발급받아주세요.\"}"
            );
        }

        // 스트림 비우기 (데이터 즉시 전송)
        out.flush();
    }

    /**
     * 공용 이메일 OTP의 발송 이메일과 실패 횟수를 삭제합니다.
     * 관리자 등록 인증 완료 상태는 최종 등록 POST에서 사용하므로 유지합니다.
     */
    private void clearCommonEmailOtpState(HttpSession session) {
        session.removeAttribute("authCodePendingEmail");
        session.removeAttribute("authCodeAttemptCount");
    }

    /**
     * 공용 이메일 OTP와 관리자 등록 인증 상태를 모두 삭제합니다.
     * 실패 횟수 초과, 만료, DB 인증정보 없음처럼 인증 흐름 전체를 무효화할 때 사용합니다.
     */
    private void clearEmailOtpState(HttpSession session) {
        clearCommonEmailOtpState(session);
        session.removeAttribute("managerAddPendingEmail");
        session.removeAttribute("managerAddVerifiedEmail");
    }
}
