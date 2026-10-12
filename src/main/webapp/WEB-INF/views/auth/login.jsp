<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.owasp.encoder.Encode" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>로그인 - 주차장 관리 시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/login.css">
</head>
<body data-context-path="${pageContext.request.contextPath}">
<div class="login-container">
    <h2>관리자 로그인</h2>

    <%-- 로그아웃 또는 재로그인 안내 메시지 표시 --%>
    <% String logoutMessage = (String) session.getAttribute("logoutMessage");
        if (logoutMessage != null) {
            session.removeAttribute("logoutMessage"); %>
    <div class="logout-message">
        <%= Encode.forHtmlContent(logoutMessage) %>
    </div>
    <% } %>

    <%-- 로그인 실패 메시지 표시 --%>
    <% String error = (String) request.getAttribute("error");
        if (error != null) { %>
    <%-- 기존 오류 문구의 <br> 표시는 줄바꿈으로 유지하고, 문구 전체는 텍스트로 출력 --%>
    <div class="error-message"><%= Encode.forHtmlContent(error.replace("<br>", "\n")) %></div>
    <% } %>

    <%-- 로그인 정보 전송 --%>
    <form action="${pageContext.request.contextPath}/login" method="post">
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <div class="form-group">
            <label for="id">아이디</label>
            <input type="text" id="id" name="id" required autofocus>
        </div>

        <div class="form-group">
            <label for="pw">비밀번호</label>
            <input type="password" id="pw" name="pw" required>
        </div>

        <button type="submit" class="btn-login">로그인</button>
    </form>
    <button type="button" class="btn-forgot" id="forgotPasswordBtn">
        🔑 비밀번호 찾기
    </button>
</div>
<script src="${pageContext.request.contextPath}/js/auth/login.js"></script>
</body>
</html>
