<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>로그인 - 주차장 관리 시스템</title>
    <link rel="stylesheet" href="/CSS/style.css">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }

        .login-container {
            background: white;
            padding: 40px;
            border-radius: 10px;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
            width: 100%;
            max-width: 400px;
        }

        h2 {
            color: #333;
            margin-bottom: 30px;
            text-align: center;
            font-size: 28px;
        }

        .form-group {
            margin-bottom: 20px;
        }

        label {
            display: block;
            margin-bottom: 5px;
            color: #555;
            font-weight: 500;
        }

        input[type="text"],
        input[type="password"] {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
            transition: border-color 0.3s;
        }

        input:focus {
            outline: none;
            border-color: #667eea;
        }

        .btn-login {
            width: 100%;
            padding: 12px;
            background: #667eea;
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.3s;
        }

        .btn-login:hover {
            background: #5568d3;
        }

        .error-message {
            background: #f8d7da;
            color: #721c24;
            padding: 12px;
            border-radius: 5px;
            margin-bottom: 20px;
            border: 1px solid #f5c6cb;
            text-align: center;
        }

        .checkbox-group {
            margin-top: 15px;
            margin-bottom: 20px;
        }

        .checkbox-group label {
            display: flex;
            align-items: center;
            font-weight: normal;
            cursor: pointer;
        }

        .checkbox-group input[type="checkbox"] {
            margin-right: 8px;
            cursor: pointer;
        }

        .btn-forgot {
            width: 100%;
            padding: 10px;
            background: transparent;
            color: #667eea;
            border: 1px solid #667eea;
            border-radius: 5px;
            font-size: 14px;
            cursor: pointer;
            margin-top: 10px;
            transition: all 0.3s;
        }

        .btn-forgot:hover {
            background: #667eea;
            color: white;
        }

        .logout-message {
            background: #d1ecf1;
            color: #0c5460;
            padding: 12px;
            border-radius: 5px;
            margin-bottom: 20px;
            border: 1px solid #bee5eb;
            text-align: center;
            font-size: 14px;
        }
    </style>
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
