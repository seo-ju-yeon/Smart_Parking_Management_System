// 외부 JavaScript에서는 JSP 표현식을 직접 사용할 수 없어 body의 data-* 속성에서 경로를 읽는다.
const contextPath = document.body.dataset.contextPath;

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
        window.location.href = contextPath + '/login';
    }
});
