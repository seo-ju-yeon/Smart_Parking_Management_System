<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>로그인 - 주차장 관리 시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/login.css">
</head>
<body>
<div class="login-container">
    <h2>관리자 로그인</h2>

    <%-- 로그아웃 또는 재로그인 안내 메시지 표시 --%>
    <% String logoutMessage = (String) session.getAttribute("logoutMessage");
        if (logoutMessage != null) {
            session.removeAttribute("logoutMessage"); %>
    <div class="logout-message">
        <%= logoutMessage %>
    </div>
    <% } %>

    <%-- 로그인 실패 메시지 표시 --%>
    <% String error = (String) request.getAttribute("error");
        if (error != null) { %>
    <div class="error-message">
        <%= error %>
    </div>
    <% } %>

    <%-- 로그인 정보 전송 --%>
    <form action="${pageContext.request.contextPath}/login" method="post">
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
    <button type="button" class="btn-forgot"
            onclick="location.href='${pageContext.request.contextPath}/forgot-password'">
        🔑 비밀번호 찾기
    </button>
</div>
</body>
</html>
