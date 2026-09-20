// 외부 JavaScript에서는 JSP 표현식을 직접 사용할 수 없어 body의 data-* 속성에서 경로를 읽는다.
const contextPath = document.body.dataset.contextPath;

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
    location.href = contextPath + '/login';
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

// 서버에서 OTP가 폐기된 경우 화면의 인증 상태도 다시 발급 가능한 상태로 초기화
function resetOtpInputState() {
    stopAuthTimer();

    otpSent = false;

    const emailInput = document.getElementById('inputEmail');
    const otpInput = document.getElementById('inputOtp');
    const otpGroup = document.getElementById('otpGroup');
    const sendBtn = document.getElementById('sendOtpBtn');
    const verifyBtn = document.getElementById('verifyOtpBtn');

    emailInput.readOnly = false;
    otpInput.value = '';
    otpGroup.style.display = 'none';

    sendBtn.disabled = false;
    sendBtn.textContent = '인증요청';

    verifyBtn.disabled = false;
    verifyBtn.textContent = '확인';

    hideFieldError('otp');
}

// 비밀번호 변경 권한이 없거나 만료된 경우 모든 입력 상태를 지우고 첫 단계로 돌아감
function restartForgotPasswordFlow(message) {
    stopAuthTimer();

    // 브라우저가 보관하던 아이디와 OTP 진행 상태를 제거
    foundManagerId = '';
    otpSent = false;

    document.getElementById('inputId').value = '';
    document.getElementById('confirmedId').value = '';

    const emailInput = document.getElementById('inputEmail');
    emailInput.value = '';
    emailInput.readOnly = false;

    document.getElementById('inputOtp').value = '';
    document.getElementById('otpGroup').style.display = 'none';

    // 다시 OTP 인증을 마친 경우 새 비밀번호 입력 폼부터 시작할 수 있도록 복구
    document.getElementById('newPassword').value = '';
    document.getElementById('confirmPassword').value = '';
    document.getElementById('passwordResetForm').style.display = 'block';
    document.getElementById('passwordResetSuccess').style.display = 'none';

    const sendBtn = document.getElementById('sendOtpBtn');
    sendBtn.disabled = false;
    sendBtn.textContent = '인증요청';

    const verifyBtn = document.getElementById('verifyOtpBtn');
    verifyBtn.disabled = false;
    verifyBtn.textContent = '확인';

    const resetBtn = document.getElementById('resetPasswordBtn');
    resetBtn.disabled = false;
    resetBtn.textContent = '비밀번호 변경';

    hideFieldError('id');
    hideFieldError('email');
    hideFieldError('otp');
    hideFieldError('newPassword');
    hideFieldError('confirmPassword');

    // goStep()이 이전 메시지를 지우므로 이동 후 권한 만료 안내를 표시
    goStep(1);
    showMsg(message, 'error');
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
    fetch(contextPath + '/forgot-password/checkId', {
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
    fetch(contextPath + '/forgot-password/sendOtp', {
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

// 인증번호를 검증하고 성공하면 새 비밀번호 입력 단계로 이동
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

    // 서버에서 OTP를 검증하고, 성공 시 세션에 비밀번호 변경 권한을 발급
    fetch(contextPath + '/forgot-password/verify', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: 'managerId=' + encodeURIComponent(foundManagerId)
            + '&email=' + encodeURIComponent(email)
            + '&otp=' + encodeURIComponent(otp)
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                // OTP 입력 단계가 끝났으므로 타이머를 멈추고 새 비밀번호 입력 화면으로 이동
                stopAuthTimer();
                goStep(3);
            } else {
                if (data.resetOtp) {
                    // 서버에서 OTP를 폐기했다면 화면도 새 인증번호를 요청할 수 있게 초기화
                    resetOtpInputState();
                    showMsg(
                        data.message || '새 인증번호를 발급받아주세요.',
                        'error'
                    );
                } else {
                    // 단순 불일치는 OTP 입력 상태를 유지하여 남은 횟수 안에서 다시 입력
                    showFieldError(
                        'otp',
                        data.message || '인증번호가 일치하지 않습니다.'
                    );
                    verifyBtn.disabled = false;
                    verifyBtn.textContent = '확인';
                }
            }
        })
        .catch(() => {
            // 요청 실패 시 사용자가 다시 시도할 수 있도록 버튼을 복구
            showMsg('서버 통신 오류가 발생했습니다.', 'error');
            verifyBtn.disabled = false;
            verifyBtn.textContent = '확인';
        });
}

// OTP 인증으로 발급된 세션 권한을 사용하여 새 비밀번호를 서버에 저장
function submitNewPassword() {
    // 비밀번호는 공백을 임의로 제거하지 않고 입력된 값 그대로 서버에 전달
    const newPassword =
        document.getElementById('newPassword').value;
    const confirmPassword =
        document.getElementById('confirmPassword').value;
    const resetBtn =
        document.getElementById('resetPasswordBtn');

    hideFieldError('newPassword');
    hideFieldError('confirmPassword');
    clearMsg();

    // 클라이언트 검사는 사용자 편의를 위한 것이며 서버에서도 같은 조건을 다시 검사함
    if (!newPassword.trim()) {
        showFieldError(
            'newPassword',
            '새 비밀번호를 입력해주세요.'
        );
        return;
    }

    if (newPassword.length < 4) {
        showFieldError(
            'newPassword',
            '비밀번호는 최소 4자 이상이어야 합니다.'
        );
        return;
    }

    if (!confirmPassword.trim()) {
        showFieldError(
            'confirmPassword',
            '새 비밀번호 확인을 입력해주세요.'
        );
        return;
    }

    if (newPassword !== confirmPassword) {
        showFieldError(
            'confirmPassword',
            '새 비밀번호가 일치하지 않습니다.'
        );
        return;
    }

    // 중복 요청으로 비밀번호가 여러 번 변경되지 않도록 처리 중에는 버튼을 비활성화
    resetBtn.disabled = true;
    resetBtn.textContent = '변경 중...';

    fetch(contextPath + '/forgot-password/reset', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams({
            newPassword: newPassword,
            confirmPassword: confirmPassword
        })
    })
        .then(async response => {
            // fetch는 HTTP 401도 성공한 통신으로 처리하므로 상태 코드와 JSON을 함께 전달
            return {
                status: response.status,
                data: await response.json()
            };
        })
        .then(result => {
            const status = result.status;
            const data = result.data;

            if (status === 401) {
                // 세션 권한이 없거나 만료됐으므로 OTP 인증부터 다시 시작
                restartForgotPasswordFlow(
                    data.message
                    || '이메일 인증부터 다시 진행해주세요.'
                );
                return;
            }

            if (data.success) {
                // 변경이 끝난 비밀번호를 화면에 남기지 않고 완료 안내로 전환
                clearMsg();
                document.getElementById('newPassword').value = '';
                document.getElementById('confirmPassword').value = '';
                document.getElementById('passwordResetForm').style.display = 'none';
                document.getElementById('passwordResetSuccess').style.display = 'block';
            } else {
                // 입력 오류나 일시적인 DB 오류는 현재 권한을 유지한 채 다시 시도하도록 함
                showMsg(
                    data.message || '비밀번호 변경에 실패했습니다.',
                    'error'
                );
                resetBtn.disabled = false;
                resetBtn.textContent = '비밀번호 변경';
            }
        })
        .catch(() => {
            // 네트워크 또는 JSON 처리 오류가 발생해도 버튼을 복구하여 다시 시도할 수 있게 함
            showMsg('서버 통신 오류가 발생했습니다.', 'error');
            resetBtn.disabled = false;
            resetBtn.textContent = '비밀번호 변경';
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
document.getElementById('confirmPassword').addEventListener('keydown', e => {
    if (e.key === 'Enter') submitNewPassword();
});
// 인증번호는 숫자만 입력할 수 있도록 제한
document.getElementById('inputOtp').addEventListener('input', function () {
    this.value = this.value.replace(/[^0-9]/g, '');
});
