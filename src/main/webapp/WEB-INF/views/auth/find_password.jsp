<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>비밀번호 찾기 - 주차장 관리 시스템</title>
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
            background: #f5f5f5;
        }

        .container {
            background: white;
            padding: 40px;
            border-radius: 10px;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
            width: 100%;
            max-width: 420px;
        }

        h2 {
            color: #333;
            margin-bottom: 8px;
            text-align: center;
            font-size: 24px;
        }

        .subtitle {
            color: #888;
            text-align: center;
            font-size: 13px;
            margin-bottom: 28px;
        }

        /* 진행 단계 */
        .steps {
            display: flex;
            justify-content: center;
            align-items: center;
            margin-bottom: 28px;
            gap: 0;
        }

        .step {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 4px;
        }

        .step-circle {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: #e0e0e0;
            color: #999;
            font-weight: bold;
            font-size: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s;
        }

        .step-circle.active {
            background: #dc3545;
            color: white;
        }

        .step-circle.done {
            background: #28a745;
            color: white;
        }

        .step-label {
            font-size: 11px;
            color: #999;
            white-space: nowrap;
        }

        .step-label.active {
            color: #dc3545;
            font-weight: 600;
        }

        .step-label.done {
            color: #28a745;
        }

        .step-line {
            width: 48px;
            height: 2px;
            background: #e0e0e0;
            margin-bottom: 18px;
            transition: background 0.3s;
        }

        .step-line.done {
            background: #28a745;
        }

        /* 입력 폼 */
        .form-group {
            margin-bottom: 18px;
        }

        label {
            display: block;
            margin-bottom: 5px;
            color: #555;
            font-weight: 500;
            font-size: 14px;
        }

        input[type="text"],
        input[type="email"],
        input[type="password"] {
            width: 100%;
            padding: 11px 12px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
            transition: border-color 0.3s;
        }

        input:focus {
            outline: none;
            border-color: #dc3545;
        }

        input:read-only {
            background: #f5f5f5;
            cursor: not-allowed;
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

        .field-hint {
            font-size: 12px;
            color: #888;
            margin-top: 4px;
        }

        /* 이메일 입력과 인증 요청 버튼을 한 줄로 배치 */
        .input-row {
            display: flex;
            gap: 8px;
            align-items: center;
        }

        .input-row input {
            flex: 1;
            margin-bottom: 0;
        }

        /* 인증번호 유효 시간 표시 */
        .auth-timer {
            font-size: 13px;
            color: #dc3545;
            font-weight: bold;
            margin-top: 6px;
        }

        .auth-timer.expiring {
            animation: blink 0.8s step-start infinite;
        }

        @keyframes blink {
            50% {
                opacity: 0.3;
            }
        }

        /* 버튼 */
        .btn {
            padding: 11px 16px;
            border: none;
            border-radius: 5px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
            white-space: nowrap;
        }

        .btn-primary {
            background: #dc3545;
            color: white;
        }

        .btn-primary:hover:not(:disabled) {
            background: #c82333;
        }

        .btn-secondary {
            background: #6c757d;
            color: white;
        }

        .btn-secondary:hover:not(:disabled) {
            background: #5a6268;
        }

        .btn-success {
            background: #28a745;
            color: white;
        }

        .btn-success:hover:not(:disabled) {
            background: #218838;
        }

        .btn:disabled {
            background: #ccc;
            cursor: not-allowed;
        }

        .btn-full {
            width: 100%;
            padding: 12px;
            font-size: 15px;
            margin-top: 8px;
        }

        /* 안내 메시지 */
        .msg {
            padding: 12px;
            border-radius: 5px;
            margin-bottom: 16px;
            text-align: center;
            font-size: 14px;
            display: none;
        }

        .msg.show {
            display: block;
        }

        .msg-error {
            background: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }

        .msg-success {
            background: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }

        .msg-info {
            background: #d1ecf1;
            color: #0c5460;
            border: 1px solid #bee5eb;
        }

        /* 단계별 화면 전환 */
        .panel {
            display: none;
        }

        .panel.active {
            display: block;
        }

        /* 하단 이동 링크 */
        .back-link {
            display: block;
            text-align: center;
            margin-top: 16px;
            color: #888;
            font-size: 13px;
            cursor: pointer;
            text-decoration: underline;
        }

        .back-link:hover {
            color: #555;
        }
    </style>
</head>
<body>
<div class="container">
    <h2>🔑 비밀번호 찾기</h2>
    <p class="subtitle">아이디와 이메일로 본인을 인증해주세요</p>

    <%-- 컨트롤러에서 전달한 오류 메시지가 있으면 표시 --%>
    <% String serverError = (String) request.getAttribute("error");
        if (serverError != null) { %>
    <div class="msg msg-error show"><%= serverError %>
    </div>
    <% } %>

    <!-- 비밀번호 찾기 진행 단계 -->
    <div class="steps">
        <div class="step">
            <div class="step-circle active" id="circle1">1</div>
            <div class="step-label active" id="label1">아이디 입력</div>
        </div>
        <div class="step-line" id="line1"></div>
        <div class="step">
            <div class="step-circle" id="circle2">2</div>
            <div class="step-label" id="label2">이메일 인증</div>
        </div>
        <div class="step-line" id="line2"></div>
        <div class="step">
            <div class="step-circle" id="circle3">3</div>
            <div class="step-label" id="label3">임시 비밀번호</div>
        </div>
    </div>

    <!-- 공통 안내 메시지 -->
    <div id="globalMsg" class="msg"></div>

    <!-- 아이디 확인 영역 -->
    <div class="panel active" id="step1">
        <div class="form-group">
            <label for="inputId">아이디 <span style="color:#dc3545">*</span></label>
            <input type="text" id="inputId" placeholder="등록된 아이디를 입력하세요">
            <div class="field-error" id="idError"></div>
        </div>
        <button class="btn btn-primary btn-full" onclick="submitStep1()">다음</button>
        <span class="back-link" onclick="goLogin()">← 로그인으로 돌아가기</span>
    </div>

    <!-- 이메일 인증 영역 -->
    <div class="panel" id="step2">
        <div class="form-group">
            <label>아이디</label>
            <input type="text" id="confirmedId" readonly>
        </div>

        <div class="form-group">
            <label for="inputEmail">이메일 <span style="color:#dc3545">*</span></label>
            <div class="input-row">
                <input type="email" id="inputEmail" placeholder="등록된 이메일을 입력하세요">
                <button class="btn btn-secondary" id="sendOtpBtn" onclick="sendOtp()">인증요청</button>
            </div>
            <div class="field-hint">데이터베이스에 등록된 이메일과 일치해야 합니다</div>
            <div class="field-error" id="emailError"></div>
            <!-- 인증번호 유효 시간 -->
            <div id="authTimer" class="auth-timer" style="display:none;">
                ⏱ 남은 시간: <span id="authTimeLeft">05:00</span>
            </div>
        </div>

        <!-- 인증번호 발송 후 표시되는 입력 영역 -->
        <div id="otpGroup" style="display:none;">
            <div class="form-group">
                <label for="inputOtp">인증번호 <span style="color:#dc3545">*</span></label>
                <div class="input-row">
                    <input type="text" id="inputOtp" maxlength="6" placeholder="6자리 인증번호">
                    <button class="btn btn-primary" id="verifyOtpBtn" onclick="verifyOtp()">확인</button>
                </div>
                <div class="field-error" id="otpError"></div>
            </div>
        </div>

        <span class="back-link" onclick="goStep(1)">← 아이디 다시 입력</span>
    </div>

    <!-- 임시 비밀번호 발급 완료 영역 -->
    <div class="panel" id="step3">
        <div class="msg msg-success show" style="font-size:15px; line-height:1.8;">
            ✅ 인증이 완료되었습니다.<br>
            등록된 이메일로 <strong>임시 비밀번호</strong>가 발송되었습니다.
        </div>
        <div class="msg msg-info show" style="font-size:13px;">
            🔒 로그인 후 반드시 비밀번호를 변경해주세요.
        </div>
        <button class="btn btn-success btn-full" onclick="goLogin()">로그인 페이지로 이동</button>
    </div>
</div>

<script>
    // 비밀번호 찾기 진행 상태
    // 아이디 확인이 끝난 뒤 이메일 인증 요청에 함께 사용할 값
    let foundManagerId = '';

    // 인증번호 발송 후 이메일이 바뀌면 기존 인증 상태를 초기화하기 위해 사용
    let otpSent = false;

    // 인증번호 제한 시간 타이머를 중복 실행하지 않기 위해 interval ID를 보관
    let authTimerInterval = null;

    // 단계 화면 이동
    function goStep(n) {
        // 현재 보이는 영역을 숨기고 요청한 순서의 영역만 표시
        document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
        document.getElementById('step' + n).classList.add('active');

        updateStepUI(n);
        clearMsg();
    }

    function goLogin() {
        location.href = '${pageContext.request.contextPath}/login';
    }

    function updateStepUI(current) {
        // 현재 영역 이전은 완료, 현재 영역은 활성 상태로 표시
        for (let i = 1; i <= 3; i++) {
            const circle = document.getElementById('circle' + i);
            const label = document.getElementById('label' + i);
            circle.className = 'step-circle' + (i < current ? ' done' : i === current ? ' active' : '');
            label.className = 'step-label' + (i < current ? ' done' : i === current ? ' active' : '');
        }

        // 지나간 구간의 연결선만 완료 색상으로 바꿈
        for (let i = 1; i <= 2; i++) {
            document.getElementById('line' + i).className =
                'step-line' + (i < current ? ' done' : '');
        }
    }

    // 공통 메시지와 필드 오류 표시
    function showMsg(text, type) {
        // type 값에 따라 error, success, info 스타일을 적용
        const el = document.getElementById('globalMsg');
        el.className = 'msg msg-' + type + ' show';
        el.textContent = text;
    }

    function clearMsg() {
        const el = document.getElementById('globalMsg');
        el.className = 'msg';
        el.textContent = '';
    }

    function showFieldError(id, msg) {
        // inputId -> idError처럼 입력 영역별 오류 메시지 요소를 찾음
        const el = document.getElementById(id + 'Error');
        if (el) {
            el.textContent = msg;
            el.style.display = 'block';
        }
    }

    function hideFieldError(id) {
        const el = document.getElementById(id + 'Error');
        if (el) {
            el.style.display = 'none';
            el.textContent = '';
        }
    }

    // 인증번호 유효 시간 관리
    function startAuthTimer() {
        // 재발송 시 기존 타이머가 남아있으면 먼저 정리
        if (authTimerInterval) clearInterval(authTimerInterval);

        let timeLeft = 300;
        const timerDiv = document.getElementById('authTimer');
        const timeSpan = document.getElementById('authTimeLeft');

        timerDiv.style.display = 'block';
        timerDiv.classList.remove('expiring');
        timeSpan.textContent = '05:00';

        authTimerInterval = setInterval(function () {
            timeLeft--;

            const m = Math.floor(timeLeft / 60);
            const s = timeLeft % 60;
            timeSpan.textContent = String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');

            // 만료 1분 전부터 타이머를 깜빡이게 해서 사용자에게 알려줌
            if (timeLeft <= 60) timerDiv.classList.add('expiring');

            if (timeLeft <= 0) {
                clearInterval(authTimerInterval);
                timerDiv.style.display = 'none';

                // 인증 시간이 지나면 OTP 입력 영역과 발송 상태를 초기화
                document.getElementById('otpGroup').style.display = 'none';
                document.getElementById('inputOtp').value = '';
                document.getElementById('sendOtpBtn').disabled = false;
                document.getElementById('sendOtpBtn').textContent = '인증요청';
                otpSent = false;
                showMsg('인증 시간이 만료되었습니다. 다시 인증번호를 요청해주세요.', 'error');
            }
        }, 1000);
    }

    function stopAuthTimer() {
        // 인증 완료 또는 상태 초기화 시 실행 중인 타이머를 종료
        if (authTimerInterval) {
            clearInterval(authTimerInterval);
            authTimerInterval = null;
        }

        document.getElementById('authTimer').style.display = 'none';
    }

    // 아이디 존재 여부 확인
    function submitStep1() {
        const id = document.getElementById('inputId').value.trim();

        hideFieldError('id');
        clearMsg();

        if (!id) {
            showFieldError('id', '아이디를 입력해주세요.');
            return;
        }

        // 입력한 아이디가 DB에 등록되어 있는지 컨트롤러에 확인 요청을 보냄
        fetch('${pageContext.request.contextPath}/forgot-password/checkId', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'managerId=' + encodeURIComponent(id)
        })
            .then(r => r.json())
            .then(data => {
                if (data.success) {
                    // 다음 요청에서 사용할 수 있도록 확인된 아이디를 저장
                    foundManagerId = id;
                    document.getElementById('confirmedId').value = id;
                    goStep(2);
                } else {
                    showFieldError('id', data.message || '존재하지 않는 아이디입니다.');
                }
            })
            .catch(() => showMsg('서버 통신 오류가 발생했습니다.', 'error'));
    }

    // 인증번호 발송
    function sendOtp() {
        const email = document.getElementById('inputEmail').value.trim();
        const sendBtn = document.getElementById('sendOtpBtn');

        hideFieldError('email');
        clearMsg();

        if (!email || !/^[A-Za-z0-9+_.-]+@(.+)$/.test(email)) {
            showFieldError('email', '올바른 이메일을 입력해주세요.');
            return;
        }

        // 중복 클릭을 막기 위해 요청 중에는 버튼을 잠시 비활성화
        sendBtn.disabled = true;
        sendBtn.textContent = '발송 중...';

        // 확인된 아이디와 입력한 이메일이 같은 관리자 정보인지 서버에서 검증한 뒤 OTP를 발송
        fetch('${pageContext.request.contextPath}/forgot-password/sendOtp', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'managerId=' + encodeURIComponent(foundManagerId)
                + '&email=' + encodeURIComponent(email)
        })
            .then(r => r.json())
            .then(data => {
                if (data.success) {
                    otpSent = true;

                    // OTP 발송 후에는 이메일을 고정해 같은 주소 기준으로 인증을 이어감
                    document.getElementById('inputEmail').readOnly = true;
                    document.getElementById('otpGroup').style.display = 'block';
                    document.getElementById('inputOtp').focus();

                    sendBtn.textContent = '재발송';
                    sendBtn.disabled = false;

                    startAuthTimer();
                    showMsg('인증번호가 이메일로 발송되었습니다.', 'success');
                } else {
                    // 서버에서 이메일 불일치 또는 발송 실패를 반환한 경우 다시 입력할 수 있게 함
                    showFieldError('email', data.message || '이메일 발송에 실패했습니다.');
                    sendBtn.disabled = false;
                    sendBtn.textContent = '인증요청';
                }
            })
            .catch(() => {
                // 네트워크 오류나 서버 오류가 나면 버튼 상태를 원래대로 돌림
                showMsg('서버 통신 오류가 발생했습니다.', 'error');
                sendBtn.disabled = false;
                sendBtn.textContent = '인증요청';
            });
    }

    // 인증번호 검증 후 임시 비밀번호 발급
    function verifyOtp() {
        const otp = document.getElementById('inputOtp').value.trim();
        const email = document.getElementById('inputEmail').value.trim();
        const verifyBtn = document.getElementById('verifyOtpBtn');

        hideFieldError('otp');
        clearMsg();

        if (!otp || otp.length !== 6) {
            showFieldError('otp', '6자리 인증번호를 입력해주세요.');
            return;
        }

        // 검증 요청이 중복으로 들어가지 않도록 확인 버튼을 비활성화함
        verifyBtn.disabled = true;
        verifyBtn.textContent = '확인 중...';

        // 서버에서 OTP를 검증하고, 성공 시 임시 비밀번호 발급까지 처리
        fetch('${pageContext.request.contextPath}/forgot-password/verify', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'managerId=' + encodeURIComponent(foundManagerId)
                + '&email=' + encodeURIComponent(email)
                + '&otp=' + encodeURIComponent(otp)
        })
            .then(r => r.json())
            .then(data => {
                if (data.success) {
                    // 인증이 끝났으므로 타이머를 멈추고 완료 화면으로 이동
                    stopAuthTimer();
                    goStep(3);
                } else {
                    showFieldError('otp', data.message || '인증번호가 일치하지 않습니다.');
                    verifyBtn.disabled = false;
                    verifyBtn.textContent = '확인';
                }
            })
            .catch(() => {
                // 요청 실패 시 사용자가 다시 시도할 수 있도록 버튼을 복구
                showMsg('서버 통신 오류가 발생했습니다.', 'error');
                verifyBtn.disabled = false;
                verifyBtn.textContent = '확인';
            });
    }

    // 인증번호 발송 후 이메일을 수정하면 인증 상태를 초기화
    document.getElementById('inputEmail').addEventListener('input', function () {
        if (otpSent) {
            otpSent = false;
            this.readOnly = false;
            document.getElementById('otpGroup').style.display = 'none';
            document.getElementById('inputOtp').value = '';
            document.getElementById('sendOtpBtn').disabled = false;
            document.getElementById('sendOtpBtn').textContent = '인증요청';
            stopAuthTimer();
        }
    });

    // Enter 키로 다음 동작을 실행
    document.getElementById('inputId').addEventListener('keydown', e => {
        if (e.key === 'Enter') submitStep1();
    });
    document.getElementById('inputOtp').addEventListener('keydown', e => {
        if (e.key === 'Enter') verifyOtp();
    });
    // 인증번호는 숫자만 입력할 수 있도록 제한
    document.getElementById('inputOtp').addEventListener('input', function () {
        this.value = this.value.replace(/[^0-9]/g, '');
    });
</script>
</body>
</html>
