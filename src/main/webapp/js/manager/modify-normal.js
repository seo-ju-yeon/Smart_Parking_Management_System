// 외부 JavaScript에서는 JSP 표현식을 직접 사용할 수 없어 body의 data-* 속성에서 경로를 읽는다.
const contextPath = document.body.dataset.contextPath;

// 관리자 수정 OTP 인증 상태 저장
let isEmailVerified = false;

// 수정 폼 요소 가져오기
const form = document.getElementById('modifyForm');
const managerId = form.elements.managerId.value;
const nameInput = document.getElementById('name');
const pwInput = document.getElementById('pw');
const passwordConfirmInput = document.getElementById('passwordConfirm');
const emailInput = document.getElementById('email');
const submitBtn = document.getElementById('submitBtn');

// 인증번호 타이머 상태 저장
let authTimerInterval = null;
const authTimerDiv   = document.getElementById('authTimer');
const authTimeLeft   = document.getElementById('authTimeLeft');

// 인증번호 유효 시간 시작
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

// 인증번호 타이머 정지
function stopAuthTimer() {
    if (authTimerInterval) {
        clearInterval(authTimerInterval);
        authTimerInterval = null;
    }
    authTimerDiv.style.display = 'none';
}

// 필드 오류 메시지 표시
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

// 필드 오류 메시지 숨김
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

// 비밀번호 강도 표시
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

// 이름 입력 여부 검사
nameInput.addEventListener('blur', function() {
    if (this.value.trim().length === 0) {
        showError('name', '이름을 입력해주세요.');
    } else {
        hideError('name');
    }
});

// 새 비밀번호를 입력한 경우만 길이 검사
pwInput.addEventListener('blur', function() {
    const value = this.value;
    if (value.length > 0 && value.length < 4) {
        showError('pw', '비밀번호는 최소 4자 이상이어야 합니다.');
    } else {
        hideError('pw');
    }
});

// 새 비밀번호 확인값 검사
passwordConfirmInput.addEventListener('blur', function() {
    const password = pwInput.value;
    const confirmPassword = this.value;

    // 새 비밀번호를 입력한 경우만 확인값 검사
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

// 이메일 형식 검사
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

// 입력값 변경 시 필드 오류 제거
[nameInput, pwInput, passwordConfirmInput, emailInput].forEach(input => {
    input.addEventListener('input', function() {
        hideError(this.id);
    });
});

// 이메일 인증번호 발송 처리
document.getElementById('sendEmailBtn').addEventListener('click', function() {
    const email = document.getElementById('email').value;
    const sendBtn = this;

    if(!email || !email.includes('@')) {
        alert('올바른 이메일을 입력해주세요.');
        return;
    }

    // 중복 클릭 방지
    sendBtn.disabled = true;
    sendBtn.textContent = '발송 중...';

    // 서버에 이메일 인증번호 발송 요청
    fetch(contextPath + '/auth/sendCode', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'email=' + encodeURIComponent(email)
            + '&purpose=MODIFY_MANAGER'
            + '&managerId=' + encodeURIComponent(managerId)
    })
        .then(response => response.json())
        .then(data => {
            if(data.success) {
                alert(email + '로 인증번호를 발송했습니다.');
                // 인증번호 입력 영역 표시
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
            // 요청 완료 후 버튼 복구
            sendBtn.disabled = false;
            sendBtn.textContent = '인증요청';
        });
});

// 인증번호 확인 처리
document.getElementById('verifyBtn').addEventListener('click', function() {
    const code = document.getElementById('authCode').value;
    const email = document.getElementById('email').value;
    const verifyBtn = this;

    if(code === "") {
        alert('인증번호를 입력해주세요.');
        return;
    }

    // 중복 확인 방지
    verifyBtn.disabled = true;
    verifyBtn.textContent = '확인 중...';

    // 서버에 인증번호 검증 요청
    fetch(contextPath + '/auth/verify', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'email=' + encodeURIComponent(email)
            + '&code=' + encodeURIComponent(code)
            + '&managerId=' + encodeURIComponent(managerId)
    })
        .then(response => response.json())
        .then(data => {
            if(data.success) {
                alert('인증이 완료되었습니다.');
                isEmailVerified = true;
                document.getElementById('email').readOnly = true;
                verifyBtn.disabled = true;
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

// 이메일 변경 시 인증 상태 초기화
emailInput.addEventListener('input', function() {
    // 기존 이메일과 같은 값이어도 수정 전에는 새 OTP 인증이 필요함
    isEmailVerified = false;
    this.readOnly = false;
    document.getElementById('sendEmailBtn').disabled = false;
    document.getElementById('emailAuthGroup').style.display = 'none';
    document.getElementById('authCode').value = '';
    stopAuthTimer();

    hideError(this.id);
});

// 폼 제출 전 입력값 전체 검사
form.addEventListener('submit', function(e) {
    let isValid = true;

    // 이름 검사
    if (nameInput.value.trim().length === 0) {
        showError('name', '이름을 입력해주세요.');
        isValid = false;
    }

    // 새 비밀번호를 입력한 경우만 검사
    const password = pwInput.value;
    if (password.length > 0) {
        if (password.length < 4) {
            showError('pw', '비밀번호는 최소 4자 이상이어야 합니다.');
            isValid = false;
        }

        // 새 비밀번호 확인값 검사
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

    // 이메일 변경 여부와 무관하게 인증 필수
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

// 초기 로드 시 이메일 미인증 상태로 시작
isEmailVerified = false;

document.querySelectorAll('.navigation-button').forEach(button => {
    button.addEventListener('click', function () {
        window.location.href = this.dataset.url;
    });
});
