<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerVO" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>내 정보 수정</title>
    <link rel="stylesheet" href="/CSS/style.css">

    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f5f5;
            margin: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }
        .main-content { width: 100%; display: flex; justify-content: center; align-items: center; }
        .container {
            width: 100%;
            max-width: 600px;
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h2 { color: #333; margin-bottom: 30px; padding-bottom: 10px; border-bottom: 2px solid #667eea; }
        .form-group { margin-bottom: 20px; }
        label { display: block; margin-bottom: 5px; color: #555; font-weight: 500; }
        .required { color: #dc3545; }
        input[type="text"], input[type="password"], input[type="email"] {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
            transition: border-color 0.3s;
        }
        input:focus { outline: none; border-color: #667eea; }
        input:disabled, input[readonly] { background-color: #e9ecef; cursor: not-allowed; }
        input.error { border-color: #dc3545; }
        .field-hint { font-size: 12px; color: #6c757d; margin-top: 4px; }
        .field-error { font-size: 12px; color: #dc3545; margin-top: 4px; display: none; }
        .btn-group { display: flex; gap: 10px; margin-top: 30px; }
        .btn { flex: 1; padding: 12px; border: none; border-radius: 5px; font-size: 16px; cursor: pointer; transition: all 0.3s; }
        .btn-primary { background: #667eea; color: white; }
        .btn-primary:hover { background: #5568d3; }
        .btn-primary:disabled { background: #ccc; cursor: not-allowed; }
        .btn-secondary { background: #6c757d; color: white; }
        .btn-secondary:hover { background: #5a6268; }
        .message { padding: 12px; border-radius: 5px; margin-bottom: 20px; text-align: center; }
        .success-message { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .error-message   { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .info-message    { background: #d1ecf1; color: #0c5460; border: 1px solid #bee5eb; }
        .password-strength { height: 4px; border-radius: 2px; margin-top: 8px; transition: all 0.3s; }
        .password-strength.weak   { background: #dc3545; width: 33%; }
        .password-strength.medium { background: #ffc107; width: 66%; }
        .password-strength.strong { background: #28a745; width: 100%; }
        .auth-timer { font-size: 14px; color: #dc3545; font-weight: bold; margin-top: 6px; }
        .auth-timer.expiring { animation: blink 0.8s step-start infinite; }
        @keyframes blink { 50% { opacity: 0.3; } }
    </style>
</head>
<body>
<%@ include file="../main/menu.jsp" %>

<div class="main-content">
    <div class="container">
        <h2>내 정보 수정</h2>

        <%-- 수정 안내 메시지 표시 --%>
        <div class="message info-message">
            ℹ️ 정보 수정을 위해 이메일 인증이 필요합니다. <br>
            ℹ️ 수정 완료 후 재로그인이 필요합니다.
        </div>

        <%-- 성공 메시지 표시 --%>
        <% String successMsg = (String) session.getAttribute("successMessage");
           if (successMsg != null) { session.removeAttribute("successMessage"); %>
        <div class="message success-message"><%= successMsg %></div>
        <% } %>

        <%-- 수정 실패 메시지 표시 --%>
        <% String error = (String) request.getAttribute("error");
           if (error != null) { %>
        <div class="message error-message"><%= error %></div>
        <% } %>

        <%
            // 세션에서 로그인 관리자 정보 확인
            ManagerVO manager = (ManagerVO) session.getAttribute("loginManager");

            // 최고관리자는 전용 수정 페이지로 이동
            if (manager != null && "ADMIN".equals(manager.getRole())) {
                response.sendRedirect(request.getContextPath() + "/mgr/modify");
                return;
            }

            if (manager != null) {
        %>
        <%-- 본인 수정 정보 전송 --%>
        <form id="modifyForm" action="${pageContext.request.contextPath}/mgr/my_modify" method="post">

            <%-- 아이디는 읽기 전용으로 표시하고 hidden 값으로 전송 --%>
            <div class="form-group">
                <label for="managerIdDisplay">아이디</label>
                <input type="text" id="managerIdDisplay" value="<%= manager.getManagerId() %>" readonly>
                <input type="hidden" name="managerId" value="<%= manager.getManagerId() %>">
                <div class="field-hint">아이디는 변경할 수 없습니다.</div>
            </div>

            <%-- 이름 입력 영역 --%>
            <div class="form-group">
                <label for="name">이름 <span class="required">*</span></label>
                <input type="text" id="name" name="name"
                       value="<%= manager.getManagerName() %>" maxlength="20" required>
                <div class="field-hint">최대 20자</div>
                <div class="field-error" id="nameError"></div>
            </div>

            <%-- 새 비밀번호 입력 영역 --%>
            <div class="form-group">
                <label for="pw">새 비밀번호</label>
                <input type="password" id="pw" name="pw">
                <div class="password-strength" id="passwordStrength"></div>
                <div class="field-hint">변경하지 않으려면 비워두세요 (변경 시 최소 4자 이상)</div>
                <div class="field-error" id="pwError"></div>
            </div>

            <%-- 새 비밀번호 확인 영역 --%>
            <div class="form-group">
                <label for="passwordConfirm">새 비밀번호 확인</label>
                <input type="password" id="passwordConfirm" name="passwordConfirm">
                <div class="field-error" id="passwordConfirmError"></div>
            </div>

            <%-- 이메일 인증 영역 --%>
            <div class="form-group">
                <label for="email">이메일 <span class="required">*</span></label>
                <div style="display: flex; gap: 8px;">
                    <input type="email" id="email" name="email"
                           value="<%= manager.getEmail() %>" maxlength="100"
                           placeholder="example@email.com" required
                           style="flex: 1; margin-bottom: 0;">
                    <button type="button" id="sendEmailBtn" class="btn btn-secondary"
                            style="width: 100px; padding: 0; font-size: 14px; height: 45px;">인증요청</button>
                </div>
                <div class="field-hint">정보 수정 시 반드시 이메일 인증이 필요합니다.</div>
                <div class="field-error" id="emailError"></div>

                <%-- 인증번호 입력 영역 --%>
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

            <%-- 처리 버튼 영역 --%>
            <div class="btn-group">
                <button type="button" class="btn btn-secondary"
                        onclick="location.href='${pageContext.request.contextPath}/dashboard'">
                    취소
                </button>
                <button type="submit" id="submitBtn" class="btn btn-primary">
                    변경사항 적용
                </button>
            </div>
        </form>

        <% } else { %>
        <div class="message error-message">
            로그인 정보를 불러올 수 없습니다. 다시 로그인해주세요.
        </div>
        <div class="btn-group">
            <button type="button" class="btn btn-secondary"
                    onclick="location.href='${pageContext.request.contextPath}/login'">
                로그인 페이지로
            </button>
        </div>
        <% } %>
    </div>
</div>

<script>
    // 인증 상태와 기존 이메일 저장
    let isEmailVerified = false;
    const originalEmail = '<%= manager != null ? manager.getEmail() : "" %>';

    // 수정 폼 요소 가져오기
    const form                 = document.getElementById('modifyForm');
    const nameInput            = document.getElementById('name');
    const pwInput              = document.getElementById('pw');
    const passwordConfirmInput = document.getElementById('passwordConfirm');
    const emailInput           = document.getElementById('email');
    const submitBtn            = document.getElementById('submitBtn');

    // 인증번호 타이머 상태 저장
    let authTimerInterval = null;
    const authTimerDiv    = document.getElementById('authTimer');
    const authTimeLeft    = document.getElementById('authTimeLeft');

    // 인증번호 유효 시간 시작
    function startAuthTimer() {
        if (authTimerInterval) clearInterval(authTimerInterval);
        let timeLeft = 300;
        authTimerDiv.style.display = 'block';
        authTimerDiv.classList.remove('expiring');
        authTimeLeft.textContent = '05:00';

        authTimerInterval = setInterval(function () {
            timeLeft--;
            const m = Math.floor(timeLeft / 60);
            const s = timeLeft % 60;
            authTimeLeft.textContent = String(m).padStart(2,'0') + ':' + String(s).padStart(2,'0');
            if (timeLeft <= 60) authTimerDiv.classList.add('expiring');
            if (timeLeft <= 0) {
                clearInterval(authTimerInterval);
                authTimerDiv.style.display = 'none';
                document.getElementById('emailAuthGroup').style.display = 'none';
                document.getElementById('authCode').value = '';
                document.getElementById('sendEmailBtn').disabled = false;
                document.getElementById('sendEmailBtn').textContent = '인증요청';
                isEmailVerified = false;
                alert('인증 시간이 만료되었습니다. 다시 인증번호를 요청해주세요.');
            }
        }, 1000);
    }

    function stopAuthTimer() {
        if (authTimerInterval) { clearInterval(authTimerInterval); authTimerInterval = null; }
        authTimerDiv.style.display = 'none';
    }

    function showError(fieldId, message) {
        const e = document.getElementById(fieldId + 'Error');
        const i = document.getElementById(fieldId);
        if (e) { e.textContent = message; e.style.display = 'block'; }
        if (i) i.classList.add('error');
    }

    function hideError(fieldId) {
        const e = document.getElementById(fieldId + 'Error');
        const i = document.getElementById(fieldId);
        if (e) e.style.display = 'none';
        if (i) i.classList.remove('error');
    }

    // 비밀번호 강도 표시
    pwInput.addEventListener('input', function () {
        const v = this.value;
        const bar = document.getElementById('passwordStrength');
        if (v.length === 0) { bar.className = 'password-strength'; return; }
        let s = 0;
        if (v.length >= 4) s++;
        if (v.length >= 8) s++;
        if (/[a-zA-Z]/.test(v) && /[0-9]/.test(v)) s++;
        if (/[^a-zA-Z0-9]/.test(v)) s++;
        bar.className = 'password-strength ' + (s <= 2 ? 'weak' : s === 3 ? 'medium' : 'strong');
    });

    // 입력값 유효성 검사
    nameInput.addEventListener('blur', function () {
        if (this.value.trim().length === 0) showError('name', '이름을 입력해주세요.'); else hideError('name');
    });
    pwInput.addEventListener('blur', function () {
        if (this.value.length > 0 && this.value.length < 4)
            showError('pw', '비밀번호는 최소 4자 이상이어야 합니다.'); else hideError('pw');
    });
    passwordConfirmInput.addEventListener('blur', function () {
        const pw = pwInput.value;
        if (pw.length > 0) {
            if (this.value.length === 0)  showError('passwordConfirm', '비밀번호 확인을 입력해주세요.');
            else if (pw !== this.value)   showError('passwordConfirm', '비밀번호가 일치하지 않습니다.');
            else                          hideError('passwordConfirm');
        }
    });
    emailInput.addEventListener('blur', function () {
        const v = this.value.trim();
        if (v.length === 0) showError('email', '이메일을 입력해주세요.');
        else if (!/^[A-Za-z0-9+_.-]+@(.+)$/.test(v)) showError('email', '올바른 이메일 형식이 아닙니다.');
        else hideError('email');
    });
    [nameInput, pwInput, passwordConfirmInput, emailInput].forEach(i => {
        i.addEventListener('input', function () { hideError(this.id); });
    });

    // 이메일 변경 시 인증 상태 초기화
    emailInput.addEventListener('input', function () {
        const cur = this.value.trim();
        if (cur !== originalEmail) {
            isEmailVerified = false;
            this.readOnly = false;
            document.getElementById('sendEmailBtn').disabled = false;
            document.getElementById('emailAuthGroup').style.display = 'none';
            document.getElementById('authCode').value = '';
            stopAuthTimer();
        }
        // 이메일 값과 관계없이 수정 전 인증 필요
        hideError(this.id);
    });

    // 이메일 인증번호 발송 처리
    document.getElementById('sendEmailBtn').addEventListener('click', function () {
        const email = emailInput.value;
        const btn = this;

        if (!email || !email.includes('@')) { alert('올바른 이메일을 입력해주세요.'); return; }

        btn.disabled = true;
        btn.textContent = '발송 중...';

        fetch('${pageContext.request.contextPath}/auth/sendCode', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'email=' + encodeURIComponent(email) + '&purpose=MY_MODIFY'
        })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                alert(email + '로 인증번호를 발송했습니다.');
                document.getElementById('emailAuthGroup').style.display = 'block';
                document.getElementById('authCode').focus();
                startAuthTimer();
            } else {
                alert('인증번호 발송 실패: ' + data.message);
                btn.disabled = false;
                btn.textContent = '인증요청';
            }
        })
        .catch(() => {
            alert('인증번호 발송 중 오류가 발생했습니다.');
            btn.disabled = false;
            btn.textContent = '인증요청';
        });
    });

    // 인증번호 확인 처리
    document.getElementById('verifyBtn').addEventListener('click', function () {
        const code = document.getElementById('authCode').value;
        const email = emailInput.value;
        const btn = this;

        if (code === '') { alert('인증번호를 입력해주세요.'); return; }

        btn.disabled = true;
        btn.textContent = '확인 중...';

        fetch('${pageContext.request.contextPath}/auth/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'email=' + encodeURIComponent(email) + '&code=' + encodeURIComponent(code)
        })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                alert('인증이 완료되었습니다. 이제 정보를 수정하고 제출할 수 있습니다.');
                isEmailVerified = true;
                emailInput.readOnly = true;
                btn.disabled = true;
                btn.textContent = '인증완료';
                document.getElementById('sendEmailBtn').disabled = true;
                stopAuthTimer();
            } else {
                alert('인증 실패: ' + data.message);
                btn.disabled = false;
                btn.textContent = '확인';
            }
        })
        .catch(() => {
            alert('인증 확인 중 오류가 발생했습니다.');
            btn.disabled = false;
            btn.textContent = '확인';
        });
    });

    // 폼 제출 전 입력값 전체 검사
    form.addEventListener('submit', function (e) {
        let ok = true;

        if (nameInput.value.trim().length === 0) { showError('name', '이름을 입력해주세요.'); ok = false; }

        const pw = pwInput.value;
        if (pw.length > 0) {
            if (pw.length < 4) { showError('pw', '비밀번호는 최소 4자 이상이어야 합니다.'); ok = false; }
            if (pw !== passwordConfirmInput.value) { showError('passwordConfirm', '비밀번호가 일치하지 않습니다.'); ok = false; }
        }

        if (!/^[A-Za-z0-9+_.-]+@(.+)$/.test(emailInput.value.trim())) {
            showError('email', '올바른 이메일 형식이 아닙니다.'); ok = false;
        }

        // 이메일 인증 필수
        if (!isEmailVerified) {
            showError('email', '이메일 인증을 완료해주세요.');
            alert('정보를 수정하려면 먼저 이메일 인증을 완료해주세요.');
            ok = false;
        }

        if (!ok) { e.preventDefault(); return false; }

        stopAuthTimer();
        submitBtn.disabled = true;
        submitBtn.textContent = '적용 중...';
    });
</script>
</body>
</html>
