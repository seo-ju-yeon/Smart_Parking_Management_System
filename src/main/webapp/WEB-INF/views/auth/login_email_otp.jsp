<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    Boolean loginOtpActiveAttribute =
            // loginOtpActive: 현재 다시 입력할 수 있는 OTP가 있는지
            (Boolean) request.getAttribute("loginOtpActive");

    boolean loginOtpActive =
            Boolean.TRUE.equals(loginOtpActiveAttribute);

    Integer remainingSecondsAttribute =
            (Integer) request.getAttribute(
                    "loginOtpRemainingSeconds"
            );

    // initialRemainingSeconds: 서버가 계산한 실제 OTP 남은 시간
    int initialRemainingSeconds =
            remainingSecondsAttribute == null
                    ? 0
                    : remainingSecondsAttribute;
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>2차 인증 - 이메일 OTP</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/login-email-otp.css">
</head>
<body data-context-path="${pageContext.request.contextPath}"
      data-otp-active="<%= loginOtpActive %>"
      data-remaining-seconds="<%= initialRemainingSeconds %>">
<div class="auth-container">
    <h2>🔐 2차 인증 <span class="admin-badge">최고관리자</span></h2>
    <p class="subtitle">이메일 인증 후 OTP를 입력해주세요</p>

    <div class="info-box">
        🔒 보안을 위해 이메일 인증과 OTP 확인이 필요합니다.
    </div>

    <%-- OTP 인증 실패 메시지 표시 --%>
    <% String error = (String) request.getAttribute("error");
        if (error != null && !error.isEmpty()) { %>
    <div class="error-message" id="errorMessage">
        <%= error %>
    </div>
    <% } %>

    <%-- 이메일과 OTP 인증 정보 전송 --%>
    <form id="otpForm" action="${pageContext.request.contextPath}/login/verifyEmailOtp" method="post">
        <!-- 이메일 입력 및 인증번호 발송 영역 -->
        <div class="form-group">
            <label for="email">이메일 주소</label>
            <div class="email-input-group">
                <input type="email" id="email" name="email" placeholder="example@email.com" required>
                <button type="button" id="sendOtpBtn" class="btn btn-secondary">인증요청</button>
            </div>
            <div class="field-hint">데이터베이스에 등록된 이메일 주소를 입력하세요</div>
            <div class="field-error" id="emailError"></div>
        </div>

        <!-- 인증번호 발송 후 표시되는 입력 영역 -->
        <div id="otpGroup" class="<%= loginOtpActive ? "is-visible" : "" %>">
            <div class="form-group">
                <label for="otp">인증번호</label>
                <input type="text" id="otp" name="otp" maxlength="6" placeholder="6자리 인증번호" autocomplete="off">
                <div class="field-hint">이메일로 전송된 6자리 인증번호를 입력하세요</div>
                <div class="timer" id="timer">남은 시간: <span id="timeLeft">05:00</span></div>
                <div class="field-error" id="otpError"></div>
            </div>

            <button type="submit" class="btn btn-primary" id="submitBtn">로그인</button>
        </div>

        <button type="button" class="btn btn-secondary" id="cancelBtn">취소</button>
    </form>
</div>

<script src="${pageContext.request.contextPath}/js/auth/login-email-otp.js"></script>
</body>
</html>
