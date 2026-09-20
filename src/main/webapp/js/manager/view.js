// 최고관리자 본인 계정 비활성화 차단
function alertAdminCannotDeactivate() {
    alert('최고 관리자 계정은 비활성화할 수 없습니다.\n계정을 비활성화하려면 다른 최고 관리자에게 문의하세요.');
    return false;
}

// 성공 메시지는 3초 후 자동 제거
window.onload = function() {
    // 성공 메시지 요소 가져오기
    const successMsg = document.querySelector('.success-message');
    if (successMsg) {
        setTimeout(() => {
            successMsg.style.transition = 'opacity 0.5s';
            successMsg.style.opacity = '0';
            setTimeout(() => successMsg.remove(), 500);
        }, 3000);
    }
};
