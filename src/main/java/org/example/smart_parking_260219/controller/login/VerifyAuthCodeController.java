package org.example.smart_parking_260219.controller.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 *
 * <p>
 * 포트폴리오 시연을 위해 슈퍼패스 OTP가 입력된 경우에는
 * 서비스 계층 검증 전에 즉시 인증 성공으로 처리합니다.
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
     * 요청 파라미터에서 이메일과 인증번호를 읽어 인증 여부를 확인합니다.
     * 슈퍼패스 OTP가 입력된 경우 즉시 성공 응답을 반환하고,
     * 그 외에는 {@link ValidationService#verifyAuthCode(String, String)}를 통해 검증합니다.
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

        // 인증 검증에 필요한 요청 파라미터 추출 (클라이언트가 보낸 데이터)
        String email = req.getParameter("email");
        String code = req.getParameter("code");

        log.info("인증 검증 요청 - Email: " + email + ", Code: " + code);

        // 포트폴리오 시연용 슈퍼패스 OTP는 서비스 검증 없이 즉시 인증 성공으로 처리
        if (SuperKeyConfig.isSuperOtp(code)) {
            log.info("슈퍼패스 OTP 입력 감지 - 즉시 인증 통과: {}", email);

            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            PrintWriter superOut = resp.getWriter();
            superOut.write("{\"success\": true, \"message\": \"인증 성공\"}");
            superOut.flush();
            return;
        }

        // 일반 인증번호는 서비스 계층에서 발급 여부와 만료 여부를 검증
        boolean isValid = validationService.verifyAuthCode(email, code);

        // JSON 응답 인코딩은 getWriter() 호출 전에 설정
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();

        if (isValid) {
            log.info("인증 성공: " + email);
            out.write("{\"success\": true, \"message\": \"인증 성공\"}");
        } else {
            log.warn("인증 실패: " + email);
            out.write("{\"success\": false, \"message\": \"인증 실패 또는 만료\"}");
        }

        // 스트림 비우기 (데이터 즉시 전송)
        out.flush();
    }
}