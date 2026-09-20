// JSP가 body의 data-* 속성으로 전달한 경로와 OTP 상태를 외부 JavaScript에서 읽는다.
const pageData = document.body.dataset;
const contextPath = pageData.contextPath;
const initialOtpActive = pageData.otpActive === 'true';
const initialRemainingSeconds = Number(pageData.remainingSeconds);

// 화면 요소 가져오기
const emailInput = document.getElementById('email');
const sendOtpBtn = document.getElementById('sendOtpBtn');
const otpGroup = document.getElementById('otpGroup');
const otpInput = document.getElementById('otp');
const submitBtn = document.getElementById('submitBtn');
const cancelBtn = document.getElementById('cancelBtn');
const otpForm = document.getElementById('otpForm');
const timerDiv = document.getElementById('timer');
const timeLeftSpan = document.getElementById('timeLeft');

// 서버가 전달한 OTP 유효 상태를 화면의 초기 인증 상태로 사용
let isEmailVerified = initialOtpActive;
// 타이머 저장
let timerInterval = null;

// 서버가 계산한 OTP 남은 시간부터 타이머 시작
function startTimer(initialSeconds = 300) {
    clearInterval(timerInterval);

    let timeLeft = initialSeconds;
    timerDiv.style.display = 'block';

    function updateTimerDisplay() {
        const minutes = Math.floor(timeLeft / 60);
        const seconds = timeLeft % 60;

        timeLeftSpan.textContent =
            String(minutes).padStart(2, '0')
            + ":"
            + String(seconds).padStart(2, '0');
    }

    updateTimerDisplay();

    timerInterval = setInterval(function () {
        timeLeft--;
        updateTimerDisplay();

        if (timeLeft <= 0) {
            clearInterval(timerInterval);
            alert(
                '인증 시간이 만료되었습니다. 다시 인증번호를 요청해주세요.'
            );
            resetForm();
        }
    }, 1000);
}

// 인증 상태 초기화
function resetForm() {
    clearInterval(timerInterval);
    timerDiv.style.display = 'none';
    otpGroup.style.display = 'none';
    emailInput.readOnly = false;
    otpInput.value = '';
    isEmailVerified = false;
    sendOtpBtn.textContent = '인증요청';

    // 이메일 임시값 삭제
    sessionStorage.removeItem('loginOtpEmail');
}

// PRG 이후 서버에 유효한 OTP가 있으면 입력 화면과 타이머 복원
if (isEmailVerified) {
    // 상태 복원
    const savedEmail =
        sessionStorage.getItem('loginOtpEmail');

    if (savedEmail) {
        emailInput.value = savedEmail;
        emailInput.readOnly = true;
    }

    otpGroup.style.display = 'block';
    sendOtpBtn.textContent = '재발송';

    startTimer(initialRemainingSeconds);
} else {
    // 임시 이메일 삭제
    sessionStorage.removeItem('loginOtpEmail');
}

// 이메일 형식 검사
emailInput.addEventListener('blur', function () {
    const value = this.value.trim();
    const emailPattern = /^[A-Za-z0-9+_.-]+@(.+)$/;
    const errorDiv = document.getElementById('emailError');

    if (value.length === 0) {
        emailInput.classList.add('error');
        errorDiv.textContent = '이메일을 입력해주세요.';
        errorDiv.style.display = 'block';
        return false;
    } else if (!emailPattern.test(value)) {
        emailInput.classList.add('error');
        errorDiv.textContent = '올바른 이메일 형식이 아닙니다.';
        errorDiv.style.display = 'block';
        return false;
    } else {
        emailInput.classList.remove('error');
        errorDiv.style.display = 'none';
        return true;
    }
});

// 인증번호 발송 처리
sendOtpBtn.addEventListener('click', function () {
    const email = emailInput.value.trim();
    const emailPattern = /^[A-Za-z0-9+_.-]+@(.+)$/;

    if (!email) {
        alert('이메일을 입력해주세요.');
        emailInput.focus();
        return;
    }

    if (!emailPattern.test(email)) {
        alert('올바른 이메일 형식을 입력해주세요.');
        emailInput.focus();
        return;
    }

    sendOtpBtn.disabled = true;
    sendOtpBtn.textContent = '발송 중...';

    const url = contextPath + '/login/sendLoginOtp';
    const body = 'email=' + encodeURIComponent(email);

    // 입력한 이메일로 로그인 OTP 발송 요청
    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: body
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP error! status: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                // 기존 에러 영역을 성공 메시지 영역으로 재사용
                const errorMessage = document.getElementById('errorMessage');
                if (errorMessage) {
                    errorMessage.className = 'success-message';
                    errorMessage.textContent = email + '로 인증번호를 발송했습니다. 이메일을 확인해주세요.';
                } else {
                    const successDiv = document.createElement('div');
                    successDiv.className = 'success-message';
                    successDiv.textContent = email + '로 인증번호를 발송했습니다. 이메일을 확인해주세요.';
                    otpForm.insertBefore(successDiv, otpForm.firstChild);
                }

                // 리다이렉트 후 이메일 입력값을 복원하기 위해 현재 탭에 임시 저장
                // sessionStorage는 현재 브라우저 탭을 닫으면 제거됨
                sessionStorage.setItem('loginOtpEmail', email);
                emailInput.readOnly = true;
                otpGroup.style.display = 'block';
                otpInput.focus();
                isEmailVerified = true;

                // 인증번호 유효 시간 시작
                startTimer();

                alert(
                    '✅ 이메일로 인증번호가 발송되었습니다!\n\n'
                    + email
                    + '\n\n이메일함을 확인하고 6자리 인증번호를 입력해주세요.\n(스팸함도 확인해주세요)');
            } else {
                // OTP 발송 실패 시 화면 상태 초기화
                resetForm();

                alert(
                    '인증번호 발송 실패: '
                    + (data.message || '알 수 없는 오류')
                );
            }
        })
        .catch(error => {
            // OTP 발송 실패 시 화면 상태 초기화
            resetForm();

            console.error('오류:', error);
            alert(
                '인증번호 발송 중 오류가 발생했습니다.\n\n오류: '
                + error.message);
        })
        .finally(() => {
            sendOtpBtn.disabled = false;
            sendOtpBtn.textContent = isEmailVerified ? '재발송' : '인증요청';
        });
});

// 인증번호는 숫자만 입력 가능
otpInput.addEventListener('input', function (e) {
    this.value = this.value.replace(/[^0-9]/g, '');
});

// OTP 인증 폼 제출 전 최종 검사
otpForm.addEventListener('submit', function (e) {
    const email = emailInput.value.trim();
    const otp = otpInput.value.trim();

    if (!email) {
        e.preventDefault();
        alert('이메일을 입력해주세요.');
        emailInput.focus();
        return false;
    }

    if (!isEmailVerified) {
        e.preventDefault();
        alert('먼저 인증번호를 발송받아주세요.');
        return false;
    }

    if (!otp || otp.length !== 6) {
        e.preventDefault();
        alert('6자리 인증번호를 입력해주세요.');
        otpInput.focus();
        return false;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = '로그인 중...';

    clearInterval(timerInterval);
    return true;
});

// 인증 취소 시 브라우저와 서버의 로그인 상태를 모두 초기화
cancelBtn.addEventListener('click', function () {
    if (confirm('로그인을 취소하시겠습니까?')) {
        clearInterval(timerInterval);
        sessionStorage.removeItem('loginOtpEmail');

        window.location.href = contextPath + '/logout';
    }
});

// 뒤로가기로 BFCache의 인증 화면이 복원되면 서버에 현재 인증 상태를 다시 확인
window.addEventListener('pageshow', function (event) {
    if (event.persisted) {
        window.location.reload();
    }
});
