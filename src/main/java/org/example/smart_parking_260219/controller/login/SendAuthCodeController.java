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
 * 이메일 인증번호 발송 요청을 처리하는 컨트롤러입니다.
 *
 * <p>
 * 요청으로 받은 이메일과 인증 목적을 기준으로 인증번호를 발송하고,
 * 처리 결과를 JSON 형태로 응답합니다.
 * </p>
 */
@WebServlet(name = "sendAuthCodeController", value = {"/auth/sendCode"})
@Log4j2
public class SendAuthCodeController extends HttpServlet {

    private final ValidationService validationService = new ValidationService();

    /**
     * 인증정보 발송 POST 요청을 처리합니다.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");  // 한글 깨짐 방지

        // 인증정보 발송에 필요한 요청 파라미터 추출
        String email = req.getParameter("email");
        String purposeParam = req.getParameter("purpose");

        // 요청받은 인증 목적을 enum으로 변환하고, 값이 없거나 잘못된 경우 기본 목적을 사용
        ValidationService.Purpose purpose;
        try {
            purpose = ValidationService.Purpose.valueOf(purposeParam);
        } catch (Exception e) {
            log.warn("purpose 파라미터 없음 또는 잘못된 값: '{}' → ADD_MANAGER 기본값 사용", purposeParam);
            purpose = ValidationService.Purpose.ADD_MANAGER;
        }

        try {
            // 이메일과 인증 목적에 맞는 인증번호를 생성하고 발송한다.
            validationService.sendAuthCode(email, purpose);

            // JSON 응답 인코딩은 getWriter() 호출 전에 설정
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            // 클라이언트에게 성공 메시지 전송 (JSON 포맷)
            PrintWriter out = resp.getWriter();
            out.write("{\"success\": true, \"message\": \"인증코드가 발송되었습니다.\"}");
            out.flush();

            log.info("인증코드 발송 완료: " + email);
        } catch (Exception e) {
            log.error("인증코드 발송 실패: " + email, e);

            // 발송 실패 시HTTP 상태 코드를 500(서버 에러)와 실패 메시지를 JSON으로 반환
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            // 클라이언트에게 에러 원인 메시지 전송
            PrintWriter out = resp.getWriter();
            out.write("{\"success\": false, \"message\": \"발송 실패: " + e.getMessage() + "\"}");
            out.flush();
        }
    }
}