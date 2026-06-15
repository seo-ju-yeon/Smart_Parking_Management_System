<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerVO" %>
<%
    // ★ 보안: ADMIN 또는 SUPER만 접근 가능. NORMAL 관리자가 직접 URL로 접근 시 차단
    ManagerVO loginCheck = (ManagerVO) session.getAttribute("loginManager");
    if (loginCheck == null ||
            (!"ADMIN".equals(loginCheck.getRole()) && !"SUPER".equals(loginCheck.getRole()))) {
        response.sendRedirect(request.getContextPath() + "/mgr/my_modify");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>일반 관리자 정보 수정</title>
    <link rel="stylesheet" href="/CSS/style.css">

    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f5f5;
            margin: 0;

            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }

        .main-content {
            width: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
        }

        .container {
            width: 100%;
            max-width: 600px;
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h2 {
            color: #333;
            margin-bottom: 30px;
            padding-bottom: 10px;
            border-bottom: 2px solid #667eea;
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
        .required {
            color: #dc3545;
        }
        input[type="text"],
        input[type="password"],
        input[type="email"] {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
            transition: border-color 0.3s;
        }
        input:focus {
            outline: none;
            border-color: #667eea;
        }
        input:disabled {
            background-color: #e9ecef;
            cursor: not-allowed;
        }
        input.error {
            border-color: #dc3545;
        }
        .field-hint {
            font-size: 12px;
            color: #6c757d;
            margin-top: 4px;
        }
        .field-error {
            font-size: 12px;
            color: #dc3545;
            margin-top: 4px;
            display: none;
        }
        .btn-group {
            display: flex;
            gap: 10px;
            margin-top: 30px;
        }
        .btn {
            flex: 1;
            padding: 12px;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            cursor: pointer;
            transition: all 0.3s;
        }
        .btn-primary {
            background: #667eea;
            color: white;
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
        .message {
            padding: 12px;
            border-radius: 5px;
            margin-bottom: 20px;
            text-align: center;
        }
        .success-message {
            background: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .error-message {
            background: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .info-message {
            background: #d1ecf1;
            color: #0c5460;
            border: 1px solid #bee5eb;
        }
        .password-strength {
            height: 4px;
            border-radius: 2px;
            margin-top: 8px;
            transition: all 0.3s;
        }
        .password-strength.weak {
            background: #dc3545;
            width: 33%;
        }
        .password-strength.medium {
            background: #ffc107;
            width: 66%;
        }
        .password-strength.strong {
            background: #28a745;
            width: 100%;
        }
        .auth-timer {
            font-size: 14px;
            color: #dc3545;
            font-weight: bold;
            margin-top: 6px;
        }
        .auth-timer.expiring {
            animation: blink 0.8s step-start infinite;
        }
        @keyframes blink {
            50% { opacity: 0.3; }
        }
    </style>
</head>
<body>
<!-- Navigation -->
<%@ include file="../main/menu.jsp" %>

<div class="main-content">
    <div class="container">
        <h2>일반 관리자 정보 수정</h2>

        <%-- 안내 메시지 --%>
        <div class="message info-message">
            ℹ️ 정보 수정 시 이메일 인증이 필요합니다.
        </div>

        <%-- 성공 메시지 표시 --%>
        <% String successMessage = (String) session.getAttribute("successMessage");
            if (successMessage != null) {
                session.removeAttribute("successMessage"); %>
        <div class="message success-message">
            <%= successMessage %>
        </div>
        <% } %>

        <%-- 에러 메시지 표시 --%>
        <% String error = (String) request.getAttribute("error");
            if (error != null) { %>
        <div class="message error-message">
            <%= error %>
        </div>
        <% } %>

        <%
            ManagerVO manager = (ManagerVO) request.getAttribute("manager");
            if (manager != null) {
        %>
        <form id="modifyForm" action="${pageContext.request.contextPath}/mgr/modify_normal" method="post">
            <!-- 아이디 -->
            <div class="form-group">
                <label for="id">아이디 <span class="required">*</span></label>
                <input type="text" id="id" name="managerId" value="<%= manager.getManagerId() %>" readonly>
                <div class="field-hint">아이디는 변경할 수 없습니다</div>
                <%--                <input type="hidden" name="managerId" value="<%= manager.getManagerId() %>">--%>
            </div>

            <!-- 이름 -->
            <div class="form-group">
                <label for="name">이름 <span class="required">*</span></label>
                <input type="text" id="name" name="name" value="<%= manager.getManagerName() %>"
                       maxlength="20" required>
                <div class="field-hint">최대 20자</div>
                <div class="field-error" id="nameError"></div>
            </div>

            <!-- 비밀번호 (선택) -->
            <div class="form-group">
                <label for="pw">새 비밀번호</label>
                <input type="password" id="pw" name="pw">
                <div class="password-strength" id="passwordStrength"></div>
                <div class="field-hint">변경하지 않으려면 비워두세요 (변경 시 최소 4자 이상 입력)</div>
                <div class="field-error" id="pwError"></div>
            </div>

            <!-- 비밀번호 확인 -->
            <div class="form-group">
                <label for="passwordConfirm">새 비밀번호 확인</label>
                <input type="password" id="passwordConfirm" name="passwordConfirm">
                <div class="field-error" id="passwordConfirmError"></div>
            </div>

            <!-- 이메일 -->
            <div class="form-group">
                <label for="email">이메일 <span class="required">*</span></label>
                <div style="display: flex; gap: 8px;">
                    <input type="email" id="email" name="email" value="<%= manager.getEmail() %>"
                           maxlength="100" placeholder="example@email.com" required
                           style="flex: 1; margin-bottom: 0;">
                    <button type="button" id="sendEmailBtn" class="btn btn-secondary"
                            style="width: 100px; padding: 0; font-size: 14px; height: 45px;">인증요청</button>
                </div>
                <div class="field-hint">변경된 이메일 인증이 필요합니다</div>
                <div class="field-error" id="emailError"></div>

                <!-- 이메일 인증번호 입력 -->
                <div id="emailAuthGroup" style="margin-top: 12px; display: none;">
                    <div style="display: flex; gap: 8px; align-items: center;">
                        <input type="text" id="authCode" placeholder="인증번호 6자리"
                               maxlength="6" style="flex: 1; margin-bottom: 0;">
                        <button type="button" id="verifyBtn" class="btn btn-primary"
                                style="width: 80px; padding: 0; font-size: 14px; height: 45px; flex-shrink: 0;">확인</button>
                    </div>
                    <div id="authTimer" class="auth-timer" style="display: none;">
                        ⏱ 남은 시간: <span id="authTimeLeft">05:00</span>
                    </div>
                </div>
            </div>

            <!-- 버튼 그룹 -->
            <div class="btn-group">
                <button type="button" class="btn btn-secondary"
                        onclick="location.href='${pageContext.request.contextPath}/mgr/list'">
                    취소
                </button>
                <button type="submit" id="submitBtn" class="btn btn-primary">
                    변경사항 적용
                </button>
            </div>
        </form>
        <% } else { %>
        <div class="message error-message">
            수정할 관리자 정보를 불러올 수 없습니다.
        </div>
        <div class="btn-group">
            <button type="button" class="btn btn-secondary"
                    onclick="location.href='${pageContext.request.contextPath}/mgr/list'">
                목록으로
            </button>
        </div>
        <% } %>
    </div>
</div>

<script>
    // 이메일 인증 완료 플래그
    let isEmailVerified = false;
    const originalEmail = '<%= manager != null ? manager.getEmail() : "" %>';

    const form = document.getElementById('modifyForm');
    const nameInput = document.getElementById('name');
    const pwInput = document.getElementById('pw');
    const passwordConfirmInput = document.getElementById('passwordConfirm');
    const emailInput = document.getElementById('email');
    const submitBtn = document.getElementById('submitBtn');

    // 타이머 관련 변수
    let authTimerInterval = null;
    const authTimerDiv   = document.getElementById('authTimer');
    const authTimeLeft   = document.getElementById('authTimeLeft');

    /* 타이머 시작 (5분 = 300초) */
    function startAuthTimer() {
        if (authTimerInterval) clearInterval(authTimerInterval);

        let timeLeft = 300;
        authTimerDiv.style.display = 'block';
        authTimerDiv.classList.remove('expiring');
        authTimeLeft.textContent = '05:00';

        authTimerInterval = setInterval(function () {
            timeLeft--;

            const minutes = Math.floor(timeLeft / 60);
            const seconds = timeLeft % 60;
            authTimeLeft.textContent =
                String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');

            if (timeLeft <= 60) {
                authTimerDiv.classList.add('expiring');
            }

            if (timeLeft <= 0) {
                clearInterval(authTimerInterval);
                authTimerDiv.style.display = 'none';
                document.getElementById('emailAuthGroup').style.display = 'none';
                document.getElementById('authCode').value = '';
                document.getElementById('sendEmailBtn').disabled = false;
                document.getElementById('sendEmailBtn').textContent = '인증요청';
                alert('인증 시간이 만료되었습니다. 다시 인증번호를 요청해주세요.');
            }
        }, 1000);
    }

    /* 타이머 정지 */
    function stopAuthTimer() {
        if (authTimerInterval) {
            clearInterval(authTimerInterval);
            authTimerInterval = null;
        }
        authTimerDiv.style.display = 'none';
    }

    // 에러 메시지 표시 함수
    function showError(fieldId, message) {
        const errorDiv = document.getElementById(fieldId + 'Error');
        const inputField = document.getElementById(fieldId);
        if (errorDiv) {
            errorDiv.textContent = message;
            errorDiv.style.display = 'block';
        }
        if (inputField) {
            inputField.classList.add('error');
        }
    }

    // 에러 메시지 숨김 함수
    function hideError(fieldId) {
        const errorDiv = document.getElementById(fieldId + 'Error');
        const inputField = document.getElementById(fieldId);
        if (errorDiv) {
            errorDiv.style.display = 'none';
        }
        if (inputField) {
            inputField.classList.remove('error');
        }
    }

    // 비밀번호 강도 체크
    pwInput.addEventListener('input', function() {
        const value = this.value;
        const strengthBar = document.getElementById('passwordStrength');

        if (value.length === 0) {
            strengthBar.className = 'password-strength';
            return;
        }

        let strength = 0;
        if (value.length >= 4) strength++;
        if (value.length >= 8) strength++;
        if (/[a-zA-Z]/.test(value) && /[0-9]/.test(value)) strength++;
        if (/[^a-zA-Z0-9]/.test(value)) strength++;

        strengthBar.className = 'password-strength';
        if (strength <= 2) {
            strengthBar.classList.add('weak');
        } else if (strength === 3) {
            strengthBar.classList.add('medium');
        } else {
            strengthBar.classList.add('strong');
        }
    });

    // 이름 유효성 검사
    nameInput.addEventListener('blur', function() {
        if (this.value.trim().length === 0) {
            showError('name', '이름을 입력해주세요.');
        } else {
            hideError('name');
        }
    });

    // 비밀번호 유효성 검사 (입력된 경우만)
    pwInput.addEventListener('blur', function() {
        const value = this.value;
        if (value.length > 0 && value.length < 4) {
            showError('pw', '비밀번호는 최소 4자 이상이어야 합니다.');
        } else {
            hideError('pw');
        }
    });

    // 비밀번호 확인 검사
    passwordConfirmInput.addEventListener('blur', function() {
        const password = pwInput.value;
        const confirmPassword = this.value;

        // 비밀번호 입력이 있을 때만 확인 검사
        if (password.length > 0) {
            if (confirmPassword.length === 0) {
                showError('passwordConfirm', '비밀번호 확인을 입력해주세요.');
            } else if (password !== confirmPassword) {
                showError('passwordConfirm', '비밀번호가 일치하지 않습니다.');
            } else {
                hideError('passwordConfirm');
            }
        }
    });

    // 이메일 유효성 검사
    emailInput.addEventListener('blur', function() {
        const value = this.value.trim();
        const emailPattern = /^[A-Za-z0-9+_.-]+@(.+)$/;

        if (value.length === 0) {
            showError('email', '이메일을 입력해주세요.');
        } else if (!emailPattern.test(value)) {
            showError('email', '올바른 이메일 형식이 아닙니다.');
        } else {
            hideError('email');
        }
    });

    // 입력 시 에러 메시지 자동 제거
    [nameInput, pwInput, passwordConfirmInput, emailInput].forEach(input => {
        input.addEventListener('input', function() {
            hideError(this.id);
        });
    });

    // 이메일 인증 요청 버튼 클릭 시
    document.getElementById('sendEmailBtn').addEventListener('click', function() {
        const email = document.getElementById('email').value;
        const sendBtn = this;

        if(!email || !email.includes('@')) {
            alert('올바른 이메일을 입력해주세요.');
            return;
        }

        // 버튼 비활성화 (중복 클릭 방지)
        sendBtn.disabled = true;
        sendBtn.textContent = '발송 중...';

        // 서버에 이메일 발송 요청
        fetch('${pageContext.request.contextPath}/auth/sendCode', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: 'email=' + encodeURIComponent(email) + '&purpose=MODIFY_MANAGER'
        })
            .then(response => response.json())
            .then(data => {
                if(data.success) {
                    alert(email + '로 인증번호를 발송했습니다.');
                    // 인증번호 입력창 보이기
                    document.getElementById('emailAuthGroup').style.display = 'block';
                    document.getElementById('authCode').focus();
                    startAuthTimer();
                } else {
                    alert('인증번호 발송 실패: ' + data.message);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('인증번호 발송 중 오류가 발생했습니다.');
            })
            .finally(() => {
                // 버튼 다시 활성화
                sendBtn.disabled = false;
                sendBtn.textContent = '인증요청';
            });
    });

    // 확인 버튼 클릭 시
    document.getElementById('verifyBtn').addEventListener('click', function() {
        const code = document.getElementById('authCode').value;
        const email = document.getElementById('email').value;
        const verifyBtn = this;

        if(code === "") {
            alert('인증번호를 입력해주세요.');
            return;
        }

        // 버튼 비활성화
        verifyBtn.disabled = true;
        verifyBtn.textContent = '확인 중...';

        // 서버에 인증번호 검증 요청
        fetch('${pageContext.request.contextPath}/auth/verify', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: 'email=' + encodeURIComponent(email) + '&code=' + encodeURIComponent(code)
        })
            .then(response => response.json())
            .then(data => {
                if(data.success) {
                    alert('인증이 완료되었습니다.');
                    isEmailVerified = true;
                    document.getElementById('email').readOnly = true; // 이메일 수정 불가
                    verifyBtn.disabled = true; // 확인 버튼 비활성화
                    verifyBtn.textContent = '인증완료';
                    document.getElementById('sendEmailBtn').disabled = true;
                    stopAuthTimer();
                } else {
                    alert('인증 실패: ' + data.message);
                    verifyBtn.disabled = false;
                    verifyBtn.textContent = '확인';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('인증 확인 중 오류가 발생했습니다.');
                verifyBtn.disabled = false;
                verifyBtn.textContent = '확인';
            });
    });

    // 이메일 입력 필드 변경 시 인증 상태 초기화
    emailInput.addEventListener('input', function() {
        const currentEmail = this.value.trim();

        // 이메일이 변경되면 인증 상태 초기화
        if (currentEmail !== originalEmail) {
            if (isEmailVerified) {
                isEmailVerified = false;
                this.readOnly = false;
                document.getElementById('sendEmailBtn').disabled = false;
                document.getElementById('emailAuthGroup').style.display = 'none';
                document.getElementById('authCode').value = '';
                stopAuthTimer();
            }
        } else {
            // 원래 이메일로 돌아가면 인증 불필요
            isEmailVerified = true;
        }

        hideError(this.id);
    });

    // 폼 제출 시 전체 유효성 검사
    form.addEventListener('submit', function(e) {
        let isValid = true;

        // 이름 검사
        if (nameInput.value.trim().length === 0) {
            showError('name', '이름을 입력해주세요.');
            isValid = false;
        }

        // 비밀번호 검사 (입력된 경우만)
        const password = pwInput.value;
        if (password.length > 0) {
            if (password.length < 4) {
                showError('pw', '비밀번호는 최소 4자 이상이어야 합니다.');
                isValid = false;
            }

            // 비밀번호 확인 검사
            if (password !== passwordConfirmInput.value) {
                showError('passwordConfirm', '비밀번호가 일치하지 않습니다.');
                isValid = false;
            }
        }

        // 이메일 검사
        const emailPattern = /^[A-Za-z0-9+_.-]+@(.+)$/;
        if (!emailPattern.test(emailInput.value.trim())) {
            showError('email', '올바른 이메일 형식이 아닙니다.');
            isValid = false;
        }

        // ★ 이메일 변경 여부와 무관하게 반드시 이메일 인증 완료 필요
        if (!isEmailVerified) {
            showError('email', '이메일 인증을 완료해주세요.');
            alert('변경사항을 적용하려면 이메일 인증을 먼저 완료해주세요.');
            isValid = false;
        }

        if (!isValid) {
            e.preventDefault();
            return false;
        }

        stopAuthTimer();
        submitBtn.disabled = true;
        submitBtn.textContent = '적용 중...';
    });

    // ★ 초기 로드 시 이메일 미인증 상태 (반드시 인증 후 제출 가능)
    isEmailVerified = false;
</script>
</body>
</html>
