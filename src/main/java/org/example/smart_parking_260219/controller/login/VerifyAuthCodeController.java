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

        // 인증 검증에 필요한 요청 파라미터 추출
        String email = req.getParameter("email");
        String code = req.getParameter("code");

        // 현재 세션에서 관리자 등록 목적으로 인증번호를 발송한 이메일 조회
        HttpSession session = req.getSession(false);
        String managerAddPendingEmail =
                session == null ? null : (String) session.getAttribute("managerAddPendingEmail");

        log.info("인증 검증 요청");

        // 일반 인증번호는 서비스 계층에서 발급 여부와 만료 여부를 검증
        ValidationService.VerificationResult verificationResult =
                validationService.verifyAuthCode(email, code);

        // JSON 응답 인코딩은 getWriter() 호출 전에 설정
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();

        if (verificationResult == ValidationService.VerificationResult.SUCCESS) {
            // 인증번호를 발송한 이메일과 현재 검증한 이메일이 같은지 확인
            boolean isManagerAddEmailMatched =
                    session != null
                            && managerAddPendingEmail != null
                            && email != null
                            && managerAddPendingEmail.equalsIgnoreCase(email.trim());

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

            log.info("인증 성공");
            out.write("{\"success\": true, \"message\": \"인증 성공\"}");

        } else if (verificationResult
                == ValidationService.VerificationResult.INVALID_CODE) {

            log.warn("인증번호 불일치");
            out.write(
                    "{\"success\": false, \"message\": \"인증번호가 일치하지 않습니다.\"}"
            );

        } else if (verificationResult
                == ValidationService.VerificationResult.EXPIRED) {

            log.warn("인증번호 만료");
            out.write(
                    "{\"success\": false, \"message\": \"인증번호가 만료되었습니다. 다시 발급받아주세요.\"}"
            );

        } else {
            log.warn("발급된 인증정보 없음");
            out.write(
                    "{\"success\": false, \"message\": \"발급된 인증번호가 없습니다. 인증번호를 먼저 발급받아주세요.\"}"
            );
        }

        // 스트림 비우기 (데이터 즉시 전송)
        out.flush();
    }
}
