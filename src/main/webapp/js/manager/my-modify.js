// 외부 JavaScript에서는 JSP 표현식을 직접 사용할 수 없어 body의 data-* 속성에서 경로를 읽는다.
const contextPath = document.body.dataset.contextPath;

const managerModifyForm = document.getElementById('modifyForm');

// 로그인 정보가 없어 수정 폼이 출력되지 않은 경우에는 폼 전용 로직을 실행하지 않는다.
if (managerModifyForm) {

// 인증 상태와 기존 이메일 저장
let isEmailVerified = false;
const originalEmail = document.getElementById('email').value.trim();

// 수정 폼 요소 가져오기
const form                 = managerModifyForm;
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

    fetch(contextPath + '/auth/sendCode', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'email=' + encodeURIComponent(email) + '&purpose=MODIFY_MANAGER'
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

    fetch(contextPath + '/auth/verify', {
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
}

// JSP의 인라인 이동 코드를 대신해 data-url에 지정된 화면으로 이동한다.
document.querySelectorAll('.navigation-button').forEach(button => {
    button.addEventListener('click', function () {
        window.location.href = this.dataset.url;
    });
});
