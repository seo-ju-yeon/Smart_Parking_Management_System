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

        // 관리자 등록을 위한 이메일 인증 요청인지 확인
        boolean isManagerAddPurpose =
                ValidationService.Purpose.ADD_MANAGER.name().equals(purposeParam);

        // 로그인 필터에서 확인된 기존 세션 조회
        HttpSession session = req.getSession(false);

        // 요청받은 인증 목적을 enum으로 변환하고, 값이 없거나 잘못된 경우 기본 목적을 사용
        ValidationService.Purpose purpose;

        try {
            purpose = ValidationService.Purpose.valueOf(purposeParam);
        } catch (Exception e) {
            log.warn("purpose 파라미터 없음 또는 잘못된 값: '{}' → ADD_MANAGER 기본값 사용", purposeParam);
            purpose = ValidationService.Purpose.ADD_MANAGER;
        }

        try {
            // 다른 목적의 이메일 인증을 새로 시작했는데 과거 관리자 등록 인증 상태가 남아있으면 안되므로
            // 목적과 관계없이 먼저 제거함
            if (session != null) {
                // 새로운 이메일 인증 요청이 시작되면 이전 관리자 등록 인증 상태를 초기화
                session.removeAttribute("managerAddPendingEmail");
                session.removeAttribute("managerAddVerifiedEmail");
            }

            // 이메일과 인증 목적에 맞는 인증번호를 생성하고 발송
            validationService.sendAuthCode(email, purpose);

            if (isManagerAddPurpose && session != null) {
                // 관리자 등록을 위해 인증번호를 발송한 이메일을 서버 세션에 저장
                session.setAttribute(
                        "managerAddPendingEmail",
                        email.trim()
                );
            }

            // JSON 응답 인코딩은 getWriter() 호출 전에 설정
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            // 클라이언트에게 성공 메시지 전송 (JSON 포맷)
            PrintWriter out = resp.getWriter();
            out.write("{\"success\": true, \"message\": \"인증코드가 발송되었습니다.\"}");
            out.flush();

            log.info("인증코드 발송 완료");
        } catch (Exception e) {
            log.error("인증코드 발송 실패", e);

            // 발송 실패 시HTTP 상태 코드를 500(서버 에러)와 실패 메시지를 JSON으로 반환
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            // 클라이언트에게 에러 원인 메시지 전송
            PrintWriter out = resp.getWriter();
            out.write("{\"success\": false, \"message\": \"인증코드 발송에 실패했습니다.\"}");
            out.flush();
        }
    }
}
