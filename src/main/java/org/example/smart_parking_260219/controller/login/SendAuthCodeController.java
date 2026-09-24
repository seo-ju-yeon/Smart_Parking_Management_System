package org.example.smart_parking_260219.controller.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.service.ValidationService;
import org.example.smart_parking_260219.vo.ManagerRole;
import org.example.smart_parking_260219.vo.ManagerVO;

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

        // 로그인 필터에서 확인된 기존 세션 조회
        HttpSession session = req.getSession(false);

        // 관리자 등록과 수정에서 사용하는 이메일 OTP 상태는 로그인 세션에 저장함
        if (session == null) {
            log.warn("세션이 없는 이메일 인증번호 발송 요청");

            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            PrintWriter out = resp.getWriter();
            out.write(
                    "{\"success\": false, \"message\": \"로그인 세션이 만료되었습니다.\"}"
            );
            out.flush();
            return;
        }

        // 세션에 OTP 대상 이메일을 저장하기 전에 빈 값 요청을 차단
        if (email == null || email.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            PrintWriter out = resp.getWriter();
            out.write(
                    "{\"success\": false, \"message\": \"이메일을 입력해주세요.\"}"
            );
            out.flush();
            return;
        }

        // 클라이언트가 전달한 인증 목적을 서버에 정의된 enum 값으로 변환
        ValidationService.Purpose purpose;

        try {
            if (purposeParam == null || purposeParam.isBlank()) {
                throw new IllegalArgumentException("인증 목적 누락");
            }

            purpose = ValidationService.Purpose.valueOf(purposeParam);
        } catch (IllegalArgumentException e) {
            // 정의되지 않은 목적을 관리자 등록으로 임의 처리하지 않고 잘못된 요청으로 거절
            log.warn("유효하지 않은 이메일 인증 목적 요청");

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // 400
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            PrintWriter out = resp.getWriter();
            out.write(
                    "{\"success\": false, \"message\": \"유효하지 않은 인증 목적입니다.\"}"
            );
            out.flush();
            return;
        }

        // 관리자 등록과 관리자 정보 수정 목적만 이 이메일 OTP API에서 처리함
        boolean isManagerAddPurpose =
                purpose == ValidationService.Purpose.ADD_MANAGER;

        boolean isManagerModifyPurpose =
                purpose == ValidationService.Purpose.MODIFY_MANAGER;

        if (!isManagerAddPurpose && !isManagerModifyPurpose) {
            log.warn("이메일 OTP API에서 허용되지 않은 인증 목적 요청");

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            PrintWriter out = resp.getWriter();
            out.write(
                    "{\"success\": false, \"message\": \"유효하지 않은 인증 목적입니다.\"}"
            );
            out.flush();
            return;
        }

        // 역할별 OTP 발송 권한을 검사하기 위해 로그인 관리자 정보를 조회함
        Object loginManagerAttribute =
                session.getAttribute("loginManager");

        if (!(loginManagerAttribute instanceof ManagerVO)) {
            log.warn("로그인 관리자 정보가 없는 이메일 인증번호 발송 요청");

            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            PrintWriter out = resp.getWriter();
            out.write(
                    "{\"success\": false, \"message\": \"로그인 정보가 유효하지 않습니다.\"}"
            );
            out.flush();
            return;
        }

        ManagerVO loginManager =
                (ManagerVO) loginManagerAttribute;

        // 관리자 등록 OTP는 ADMIN 역할만 발송할 수 있음
        if (isManagerAddPurpose
                && loginManager.getRole() != ManagerRole.ADMIN) {

            log.warn(
                    "관리자 등록 OTP 발송 권한 없음 - ID: {}, 역할: {}",
                    loginManager.getManagerId(),
                    loginManager.getRole()
            );

            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            PrintWriter out = resp.getWriter();
            out.write(
                    "{\"success\": false, \"message\": \"관리자 등록 권한이 없습니다.\"}"
            );
            out.flush();
            return;
        }

        try {
            // 새로운 인증번호를 발송하면 이전 OTP와 관리자 등록 인증 상태를 재사용할 수 없도록 초기화
            session.removeAttribute("managerAddPendingEmail");
            session.removeAttribute("managerAddVerifiedEmail");
            session.removeAttribute("authCodePendingEmail");
            session.removeAttribute("authCodeAttemptCount");
            session.removeAttribute("authCodePurpose");

            // DB 저장, 메일 발송, 세션 저장에서 동일한 이메일 값을 사용하도록 앞뒤 공백 제거
            String pendingEmail = email.trim();

            // 이메일과 인증 목적에 맞는 인증번호를 생성하고 발송
            validationService.sendAuthCode(pendingEmail, purpose);

            // 검증 요청에서 사용할 인증 목적은 서버가 승인한 enum 값으로 저장함
            session.setAttribute(
                    "authCodePurpose",
                    purpose.name()
            );

            // 메일 발송까지 성공한 경우에만 검증 대상 이메일과 실패 횟수를 세션에 저장함
            session.setAttribute(
                    "authCodePendingEmail",
                    pendingEmail
            );

            session.setAttribute(
                    "authCodeAttemptCount",
                    0
            );

            log.info("이메일 OTP 검증 상태 생성 - 실패 횟수: 0");

            if (isManagerAddPurpose) {
                // 관리자 등록 최종 POST에서도 확인할 이메일을 별도로 저장
                session.setAttribute(
                        "managerAddPendingEmail",
                        pendingEmail
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
            // 발송에 실패한 OTP에 대한 검증 상태가 남지 않도록 초기화
            session.removeAttribute("authCodePendingEmail");
            session.removeAttribute("authCodeAttemptCount");
            session.removeAttribute("authCodePurpose");
            session.removeAttribute("managerAddPendingEmail");
            session.removeAttribute("managerAddVerifiedEmail");

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
