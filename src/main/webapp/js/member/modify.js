// 2. 폼 유효성 검사 (오타 수정 버전)
function validateForm() {
    const nameInput = document.getElementById('name');
    const phoneInput = document.getElementById('phone');
    const actionInput = document.getElementById('subscribeAction');

    if (!nameInput || !phoneInput) return true;

    const name = nameInput.value.trim();
    const phone = phoneInput.value.trim();
    const action = actionInput ? actionInput.value : '';

    if (!name) { alert('이름을 입력해주세요.'); return false; }

    // '콜' 대신 'phone'으로 확실히 수정
    const phonePattern = /^[0-9]{2,3}-[0-9]{3,4}-[0-9]{4}$/;
    if (!phonePattern.test(phone)) {
        alert('올바른 전화번호 형식이 아닙니다.\n예: 010-1234-5678');
        return false;
    }

    if (action === 'add' || action === 'extend') {
        const startDate = document.getElementById('startDate').value;
        const endDate = document.getElementById('endDate').value;
        if (!startDate || !endDate) { alert('기간을 입력해주세요.'); return false; }
        if (new Date(startDate) >= new Date(endDate)) {
            alert('종료일은 시작일보다 이후여야 합니다.');
            return false;
        }
    }
    return confirm('진행하시겠습니까?');
}

// 3. 날짜 필드 토글
function toggleDateFields() {
    const actionInput = document.getElementById('subscribeAction');
    const dateFields = document.getElementById('dateFields');
    if (actionInput && dateFields) {
        const action = actionInput.value;
        dateFields.style.display = (action === 'add' || action === 'extend') ? 'block' : 'none';
    }
}