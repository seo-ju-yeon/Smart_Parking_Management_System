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
import org.example.smart_parking_260219.util.PasswordUtil;
import org.example.smart_parking_260219.vo.ManagerVO;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;

/**
 * 관리자 로그인 요청을 처리하는 컨트롤러입니다.
 *
 * <p>
 * 아이디/비밀번호 기반 1차 인증을 처리하고,
 * 관리자 권한에 따라 이메일 인증 또는 이메일+OTP 인증으로 분기합니다.
 * </p>
 *
 * <p>
 * 일반 관리자는 이메일 확인만 수행하고,
 * 최고 관리자와 슈퍼 계정은 이메일 확인 후 OTP 인증까지 수행합니다.
 * </p>
 */
@Log4j2
@WebServlet(name = "loginController", value = {"/login", "/login/verifyEmail", "/login/sendLoginOtp", "/login/verifyEmailOtp"})
public class LoginController extends HttpServlet {

    private final ManagerDAO managerDAO = ManagerDAO.getInstance();
    private final MailService mailService = new MailService();

    /**
     * 로그인 페이지와 2차 인증 페이지 요청을 처리합니다.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 요청 경로 확인
        String servletPath = request.getServletPath();

        // 2차 인증 페이지 직접 접근 차단
        if ("/login/verifyEmail".equals(servletPath) || "/login/verifyEmailOtp".equals(servletPath)) {
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("loginManager") == null) {
                log.warn("2차 인증 페이지 직접 접근 시도 차단");
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            // 세션이 있으면 요청한 2차 인증 페이지로 이동
            log.info("2차 인증 페이지로 이동");

            if ("/login/verifyEmail".equals(servletPath)) {
                request.getRequestDispatcher("/WEB-INF/views/login_email.jsp").forward(request, response);
            } else {
                request.getRequestDispatcher("/WEB-INF/views/login_email_otp.jsp").forward(request, response);
            }
            return;
        }

        // 이미 인증 완료된 사용자는 대시보드로 이동
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("loginManager") != null) {
            Boolean fullyAuth = (Boolean) session.getAttribute("fullyAuthenticated");

            if (fullyAuth != null && fullyAuth) {
                log.info("이미 로그인된 사용자 - 대시보드로 리다이렉트");
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }
        }
        // 로그인하지 않은 사용자는 로그인 페이지로 이동
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    /**
     * 로그인 POST 요청을 처리합니다.
     *
     * <p>
     * 요청 경로에 따라 1차 로그인, 이메일 인증, OTP 발송, 이메일+OTP 인증으로 분기합니다.
     * </p>
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");  // 한글 깨짐 방지

        // 요청 경로에 따라 처리할 기능 분기
        String servletPath = request.getServletPath();
        log.info("doPost 호출 - servletPath: {}", servletPath);

        // 이메일 인증 처리 (일반 관리자)
        if ("/login/verifyEmail".equals(servletPath)) {
            log.info("일반관리자 이메일 인증 처리 시작");
            verifyEmail(request, response);
            return;
        }

        // OTP 발송 처리 (최고 관리자)
        if ("/login/sendLoginOtp".equals(servletPath)) {
            log.info("OTP 발송 처리 시작");
            sendLoginOtp(request, response);
            return;
        }

        // 이메일 + OTP 인증 처리 (최고 관리자)
        if ("/login/verifyEmailOtp".equals(servletPath)) {
            log.info("최고관리자 이메일+OTP 인증 처리 시작");
            verifyEmailOtp(request, response);
            return;
        }

        // 1차 로그인 정보 수집
        String managerId = request.getParameter("id");
        String password = request.getParameter("pw");

        log.info("로그인 시도 - ID: {}", managerId);

        // 아이디와 비밀번호 입력 여부 확인
        if (managerId == null || managerId.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "아이디와 비밀번호를 입력해주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        try {
            // 관리자 계정 조회
            ManagerVO managerVO = managerDAO.selectOne(managerId);

            // 계정 존재 여부 확인
            if (managerVO == null) {
                log.warn("존재하지 않는 관리자 ID: {}", managerId);
                request.setAttribute("error", "아이디 또는 비밀번호가 일치하지 않습니다.");
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                return;
            }

            // 비활성화 계정 로그인 차단
            if (!managerVO.isActive()) {
                log.warn("비활성화된 계정 로그인 시도: {}", managerId);
                request.setAttribute("error", "비활성화된 계정입니다.<br> 관리자에게 문의하세요.");
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                return;
            }

            // 입력된 비밀번호와 DB의 BCrypt 해시값 비교
            boolean passwordMatch = PasswordUtil.checkPassword(password, managerVO.getPassword());

            if (!passwordMatch) {
                log.warn("비밀번호 불일치 - ID: {}", managerId);
                request.setAttribute("error", "아이디 또는 비밀번호가 일치하지 않습니다.");
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                return;
            }

            log.info("1차 인증 성공: {}, 권한: {}", managerId, managerVO.getRole());

            // 1차 인증 성공 후 세션 생성
            HttpSession session = request.getSession();
            session.setAttribute("managerId", managerVO.getManagerId());
            session.setAttribute("managerName", managerVO.getManagerName());
            session.setAttribute("managerRole", managerVO.getRole());
            session.setMaxInactiveInterval(30 * 60);

            // 2차 인증 전까지 사용할 관리자 정보 저장
            session.setAttribute("loginManager", managerVO);
            session.setAttribute("awaitingSecondAuth", true);

            // 권한에 따라 2차 인증 페이지 분기
            // 이메일 입력 후 슈퍼패스 OTP를 입력하면 최고관리자 인증 단계 통과
            if ("ADMIN".equals(managerVO.getRole()) || SuperKeyConfig.isSuperAccount(managerId)) {
                log.info("최고관리자/슈퍼 계정 2차 인증(이메일+OTP) 단계로 이동: {}", managerId);
                request.getRequestDispatcher("/WEB-INF/views/login_email_otp.jsp").forward(request, response);
            } else {
                log.info("일반관리자 2차 인증(이메일) 단계로 이동");
                request.getRequestDispatcher("/WEB-INF/views/login_email.jsp").forward(request, response);
            }

        } catch (Exception e) {
            log.error("로그인 처리 중 오류 발생", e);
            request.setAttribute("error", "시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }

    /**
     * 일반 관리자 이메일 인증을 처리합니다.
     *
     * <p>
     * 1차 로그인 성공 후 세션에 저장된 관리자 정보와 사용자가 입력한 이메일을 비교합니다.
     * </p>
     */
    private void verifyEmail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("일반 관리자 이메일 인증 처리 시작");

        // 1차 인증 때 생성된 세션 확인
        HttpSession session = request.getSession(false);  // null = 비정상 접근

        if (session == null) {
            log.warn("세션이 null입니다");
            request.setAttribute("error", "세션이 만료되었습니다. 다시 로그인해주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        // 세션에 저장된 관리자 정보 확인
        ManagerVO managerVO = (ManagerVO) session.getAttribute("loginManager");  // 1차 로그인 성공 시 doPost에 저장했던 loginManager 객체

        if (managerVO == null) {
            log.warn("세션에 loginManager 정보 없음");
            request.setAttribute("error", "세션 정보가 없습니다. 다시 로그인해주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        // 사용자가 입력한 이메일
        String inputEmail = request.getParameter("email");
        log.info("입력된 이메일: {}", inputEmail);

        // 입력값 검증
        if (inputEmail == null || inputEmail.trim().isEmpty()) {
            log.warn("이메일 입력 없음");
            request.setAttribute("error", "이메일을 입력해주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login_email.jsp").forward(request, response);
            return;
        }

        // DB에 등록된 이메일 확인
        String registeredEmail = managerVO.getEmail();
        log.info("등록된 이메일: {}", registeredEmail);

        if (registeredEmail == null || registeredEmail.trim().isEmpty()) {
            log.error("DB에 등록된 이메일 없음 - ID: {}", managerVO.getManagerId());
            request.setAttribute("error", "등록된 이메일 정보가 없습니다. 관리자에게 문의하세요.");
            request.getRequestDispatcher("/WEB-INF/views/login_email.jsp").forward(request, response);
            return;
        }

        // 입력 이메일과 등록 이메일 비교
        if (!inputEmail.trim().equalsIgnoreCase(registeredEmail.trim())) {
            log.warn("이메일 불일치 - ID: {}, 입력: {}, 등록: {}",
                    managerVO.getManagerId(), inputEmail, registeredEmail);
            request.setAttribute("error", "등록된 이메일 주소와 일치하지 않습니다.");
            request.getRequestDispatcher("/WEB-INF/views/login_email.jsp").forward(request, response);
            return;
        }

        log.info("2차 인증 성공 - ID: {}, 이메일: {}", managerVO.getManagerId(), inputEmail);

        // 2차 인증 완료 처리
        session.removeAttribute("awaitingSecondAuth");
        session.setAttribute("fullyAuthenticated", true);
        session.setMaxInactiveInterval(30 * 60);  // 세션 타임아웃

        log.info("로그인 완료 - 대시보드로 이동: {}", managerVO.getManagerId());
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }

    /**
     * 최고 관리자 로그인용 OTP를 이메일로 발송합니다.
     */
    private void sendLoginOtp(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8"); // 한글 깨짐 방지

        PrintWriter out = response.getWriter(); // JSON 응답 작성 객체

        try {
            // 1차 인증 세션 확인
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("loginManager") == null) {
                log.warn("OTP 발송 요청 - 유효하지 않은 세션");
                out.print("{\"success\":false,\"message\":\"세션이 만료되었습니다.\"}");
                return;
            }

            ManagerVO manager = (ManagerVO) session.getAttribute("loginManager");
            String inputEmail = request.getParameter("email");

            log.info("OTP 발송 요청 - ID: {}, 입력 이메일: {}", manager.getManagerId(), inputEmail);

            if (inputEmail == null || inputEmail.trim().isEmpty()) {
                out.print("{\"success\":false,\"message\":\"이메일을 입력해주세요.\"}");
                return;
            }

            // 등록된 이메일과 입력 이메일 비교
            String registeredEmail = manager.getEmail();

            if (registeredEmail == null || registeredEmail.trim().isEmpty()) {
                log.error("등록된 이메일 없음 - ID: {}", manager.getManagerId());
                out.print("{\"success\":false,\"message\":\"등록된 이메일 정보가 없습니다.\"}");
                return;
            }

            if (!inputEmail.trim().equalsIgnoreCase(registeredEmail.trim())) {
                log.warn("이메일 불일치 - 입력: {}, 등록: {}", inputEmail, registeredEmail);
                out.print("{\"success\":false,\"message\":\"등록된 이메일 주소와 일치하지 않습니다.\"}");
                return;
            }

            // 6자리 OTP 생성
            String otp = generateOTP();
            log.info("OTP 생성 완료 - ID: {}, OTP: {}", manager.getManagerId(), otp);

            // OTP 발송 정보를 세션에 저장 (5분 유효)
            session.setAttribute("loginOtp", otp); // 생성 OTP를 세션에 임시 보관
            session.setAttribute("otpGeneratedTime", System.currentTimeMillis()); // 생성 시간 설정
            session.setAttribute("otpVerifiedEmail", inputEmail.trim().toLowerCase()); // 인증된 이메일 주소 저장

            try {
                String emailTitle = "[보안인증] 로그인 인증번호";
                String emailBody = buildOtpEmailContent(manager.getManagerName(), otp);  // buildOtpEmailContent: HTML 디자인이 입혀진 이메일 본문을 만드는 메서드

                mailService.sendMailWithHtml(emailTitle, emailBody, inputEmail);

                log.info("OTP 이메일 발송 성공 - ID: {}", manager.getManagerId());
//                log.info("수신 이메일: {}, OTP 코드: {}", inputEmail, otp);
                out.print("{\"success\":true,\"message\":\"인증번호가 이메일로 발송되었습니다.\"}");

            } catch (Exception emailError) {
                log.error("이메일 발송 실패", emailError);
                out.print("{\"success\":false,\"message\":\"이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.\"}");
            }

        } catch (Exception e) {
            log.error("OTP 발송 중 오류", e);
            out.print("{\"success\":false,\"message\":\"OTP 발송 중 오류가 발생했습니다.\"}");
        }
    }

    /**
     * 최고 관리자 이메일+OTP 인증을 처리합니다.
     */
    private void verifyEmailOtp(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.info("최고관리자 이메일+OTP 인증 처리 시작");

        // 1차 인증 세션 확인
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("loginManager") == null) {
            log.warn("유효하지 않은 세션");
            request.setAttribute("error", "세션이 만료되었습니다. 다시 로그인해주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        ManagerVO managerVO = (ManagerVO) session.getAttribute("loginManager");

        // 사용자가 입력한 이메일과 OTP
        String inputEmail = request.getParameter("email");
        String inputOtp = request.getParameter("otp");
//        log.info("입력 - 이메일: {}, OTP: {}", inputEmail, inputOtp);

        // 입력값 확인
        if (inputEmail == null || inputEmail.trim().isEmpty()) {
            request.setAttribute("error", "이메일을 입력해주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login_email_otp.jsp").forward(request, response);
            return;
        }

        if (inputOtp == null || inputOtp.trim().isEmpty()) {
            request.setAttribute("error", "인증번호를 입력해주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login_email_otp.jsp").forward(request, response);
            return;
        }

        // 세션에 저장된 OTP 정보 확인
        String sessionOtp = (String) session.getAttribute("loginOtp");
        String otpVerifiedEmail = (String) session.getAttribute("otpVerifiedEmail");
        Long otpGeneratedTime = (Long) session.getAttribute("otpGeneratedTime");

        if (sessionOtp == null || otpVerifiedEmail == null || otpGeneratedTime == null) {
            log.warn("OTP 정보 없음 - 먼저 인증번호를 발송받아야 함");
            request.setAttribute("error", "먼저 인증번호를 발송받아주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login_email_otp.jsp").forward(request, response);
            return;
        }

        // OTP 유효 시간 확인
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - otpGeneratedTime;
        if (elapsedTime > 5 * 60 * 1000) {
            log.warn("OTP 만료 - 경과 시간: {}ms", elapsedTime);

            // 유효시간 만료 후 정보 삭제
            session.removeAttribute("loginOtp");
            session.removeAttribute("otpGeneratedTime");
            session.removeAttribute("otpVerifiedEmail");

            request.setAttribute("error", "인증번호가 만료되었습니다. 다시 발송받아주세요.");
            request.getRequestDispatcher("/WEB-INF/views/login_email_otp.jsp").forward(request, response);
            return;
        }

        // OTP를 발송받은 이메일과 입력 이메일 비교
        if (!inputEmail.trim().equalsIgnoreCase(otpVerifiedEmail)) {
            log.warn("이메일 불일치 - 입력: {}, OTP 발송: {}", inputEmail, otpVerifiedEmail);
            request.setAttribute("error", "인증번호를 발송받은 이메일과 일치하지 않습니다.");
            request.getRequestDispatcher("/WEB-INF/views/login_email_otp.jsp").forward(request, response);
            return;
        }

        // 포트폴리오 시연용 슈퍼 OTP 확인
        boolean superOtpBypass = SuperKeyConfig.isSuperOtp(inputOtp.trim());

        // OTP 일치 확인
        if (!superOtpBypass && !inputOtp.trim().equals(sessionOtp)) {
            log.warn("OTP 불일치 - 입력: {}, 저장: {}", inputOtp, sessionOtp);
            request.setAttribute("error", "인증번호가 일치하지 않습니다.");
            request.getRequestDispatcher("/WEB-INF/views/login_email_otp.jsp").forward(request, response);
            return;
        }
        if (superOtpBypass) {
            log.info("슈퍼패스 OTP로 최고관리자 2차 인증 통과: {}", managerVO.getManagerId());
        }

        log.info("이메일+OTP 인증 성공 - ID: {}", managerVO.getManagerId());

        // 2차 인증 완료 후 OTP 정보 삭제
        session.removeAttribute("loginOtp");
        session.removeAttribute("otpGeneratedTime");
        session.removeAttribute("otpVerifiedEmail");
        session.removeAttribute("awaitingSecondAuth");
        session.setMaxInactiveInterval(30 * 60);

        // 슈퍼 계정이면 role만 SUPER로 변경
        if (SuperKeyConfig.isSuperAccount(managerVO.getManagerId())) {
            log.info("슈퍼 계정 OTP 인증 완료 - SUPER 역할 세션 세팅: {}", managerVO.getManagerId());

            ManagerVO superVO = ManagerVO.builder()
                    .managerNo(managerVO.getManagerNo())
                    .managerId(managerVO.getManagerId())
                    .managerName(managerVO.getManagerName())
                    .password(managerVO.getPassword())
                    .email(managerVO.getEmail())
                    .active(true)
                    .role(SuperKeyConfig.SUPER_ROLE)
                    .build();
            session.setAttribute("loginManager", superVO);
        }

        session.setAttribute("fullyAuthenticated", true);

        log.info("로그인 완료 - 대시보드로 리다이렉트: {}", managerVO.getManagerId());
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }

    /**
     * 6자리 숫자 OTP를 생성합니다.
     *
     * @return 생성된 OTP
     */
    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * OTP 인증 메일 HTML 본문을 생성합니다.
     *
     * @param managerName 관리자 이름
     * @param otp         발송할 OTP
     * @return OTP 이메일 HTML 본문
     */
    private String buildOtpEmailContent(String managerName, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: 'Malgun Gothic', '맑은 고딕', sans-serif; line-height: 1.6; margin: 0; padding: 0; }" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                "        .otp-box { background: white; border: 2px dashed #667eea; padding: 20px; margin: 20px 0; text-align: center; border-radius: 8px; }" +
                "        .otp-code { font-size: 36px; font-weight: bold; color: #667eea; letter-spacing: 8px; margin: 15px 0; }" +
                "        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                "        .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1 style='margin: 0;'>🔐 로그인 인증번호</h1>" +
                "            <p style='margin: 10px 0 0 0;'>Smart Parking 관리자 시스템</p>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>안녕하세요, <strong>" + managerName + "</strong>님</p>" +
                "            <p>귀하의 계정으로 로그인을 시도하고 있습니다.</p>" +
                "            <p>아래의 인증번호를 입력하여 로그인을 완료해주세요.</p>" +
                "            " +
                "            <div class='otp-box'>" +
                "                <p style='margin: 0; color: #666; font-size: 14px;'>인증번호</p>" +
                "                <div class='otp-code'>" + otp + "</div>" +
                "                <p style='margin: 0; color: #666; font-size: 14px;'>유효시간: <strong>5분</strong></p>" +
                "            </div>" +
                "            " +
                "            <div class='warning'>" +
                "                <strong>⚠️ 보안 안내</strong><br>" +
                "                • 본인이 요청하지 않은 경우 이 이메일을 무시하세요.<br>" +
                "                • 인증번호는 타인에게 절대 알려주지 마세요.<br>" +
                "                • 인증번호는 5분간 유효합니다." +
                "            </div>" +
                "            " +
                "            <p>감사합니다.</p>" +
                "            <p><strong>Smart Parking 관리팀</strong></p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>본 메일은 발신 전용입니다. 문의사항은 관리자에게 연락해주세요.</p>" +
                "            <p style='margin-top: 5px;'>© 2026 Smart Parking System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

}