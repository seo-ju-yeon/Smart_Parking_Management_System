<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>2차 인증 - 이메일 확인</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/login-email.css">
</head>
<body data-context-path="${pageContext.request.contextPath}">
<div class="auth-container">
    <h2>🔐 2차 인증</h2>
    <p class="subtitle">등록된 이메일 주소를 입력해주세요</p>

    <div class="info-box">
        📧 회원가입 시 등록한 이메일 주소를 정확히 입력해주세요.
    </div>

    <%-- 이메일 인증 실패 메시지 표시 --%>
    <% String error = (String) request.getAttribute("error");
        if (error != null && !error.isEmpty()) { %>
    <div class="error-message">
        <%= error %>
    </div>
    <% } %>

    <%-- 이메일 인증 정보 전송 --%>
    <form id="emailForm" action="${pageContext.request.contextPath}/login/verifyEmail" method="post">
        <div class="form-group">
            <label for="email">이메일 주소</label>
            <input type="email" id="email" name="email" placeholder="example@email.com" required autofocus>
            <div class="field-error" id="emailError"></div>
        </div>

        <button type="submit" class="btn btn-primary" id="submitBtn">확인</button>
        <button type="button" class="btn btn-secondary" id="cancelBtn">취소</button>
    </form>
</div>

<script src="${pageContext.request.contextPath}/js/auth/login-email.js"></script>
</body>
</html>
