<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>2차 인증 - 이메일 확인</title>
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

        .auth-container {
            background: white;
            padding: 40px;
            border-radius: 10px;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
            width: 100%;
            max-width: 400px;
        }

        h2 {
            color: #333;
            margin-bottom: 10px;
            text-align: center;
            font-size: 24px;
        }

        .subtitle {
            color: #666;
            text-align: center;
            margin-bottom: 30px;
            font-size: 14px;
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

        input[type="email"] {
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

        input.error {
            border-color: #dc3545;
        }

        .field-error {
            font-size: 12px;
            color: #dc3545;
            margin-top: 4px;
            display: none;
        }

        .btn {
            width: 100%;
            padding: 12px;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.3s;
        }

        .btn-primary {
            background: #667eea;
            color: white;
            margin-bottom: 10px;
        }

        .btn-primary:hover {
            background: #5568d3;
        }

        .btn-primary:disabled {
            background: #ccc;
            cursor: not-allowed;
        }

        .btn-secondary {
            background: #6c757d;
            color: white;
        }

        .btn-secondary:hover {
            background: #5a6268;
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

        .info-box {
            background: #d1ecf1;
            color: #0c5460;
            padding: 12px;
            border-radius: 5px;
            margin-bottom: 20px;
            border: 1px solid #bee5eb;
            font-size: 14px;
        }
    </style>
</head>
<body>
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

<script>
    // 화면 요소 가져오기
    const emailInput = document.getElementById('email');
    const submitBtn = document.getElementById('submitBtn');
    const cancelBtn = document.getElementById('cancelBtn');
    const emailForm = document.getElementById('emailForm');

    // 이메일 형식 검사
    emailInput.addEventListener('blur', function () {
        const value = this.value.trim();
        const emailPattern = /^[A-Za-z0-9+_.-]+@(.+)$/;
        const errorDiv = document.getElementById('emailError');

        if (value.length === 0) {
            emailInput.classList.add('error');
            errorDiv.textContent = '이메일을 입력해주세요.';
            errorDiv.style.display = 'block';
        } else if (!emailPattern.test(value)) {
            emailInput.classList.add('error');
            errorDiv.textContent = '올바른 이메일 형식이 아닙니다.';
            errorDiv.style.display = 'block';
        } else {
            emailInput.classList.remove('error');
            errorDiv.style.display = 'none';
        }
    });

    // 이메일 인증 폼 제출 전 최종 검사
    emailForm.addEventListener('submit', function (e) {
        const emailPattern = /^[A-Za-z0-9+_.-]+@(.+)$/;

        if (!emailInput.value.trim()) {
            e.preventDefault();
            alert('이메일을 입력해주세요.');
            emailInput.focus();
            return false;
        }

        if (!emailPattern.test(emailInput.value.trim())) {
            e.preventDefault();
            alert('올바른 이메일 형식을 입력해주세요.');
            emailInput.focus();
            return false;
        }

        // 중복 제출 방지
        submitBtn.disabled = true;
        submitBtn.textContent = '확인 중...';

        return true;
    });

    // 인증 취소 시 로그인 화면으로 이동
    cancelBtn.addEventListener('click', function () {
        if (confirm('로그인을 취소하시겠습니까?')) {
            window.location.href = '${pageContext.request.contextPath}/login';
        }
    });
</script>
</body>
</html>
