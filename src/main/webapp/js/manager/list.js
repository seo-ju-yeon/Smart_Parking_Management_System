// 최고관리자 접근 차단 모달 제어
function openAdminModal() {
    document.getElementById('adminModal').classList.add('show');
}

function closeAdminModal() {
    document.getElementById('adminModal').classList.remove('show');
}

// 모달 바깥 영역을 클릭하면 닫음
document.getElementById('adminModal').addEventListener('click', function (e) {
    if (e.target === this) closeAdminModal();
});

// ESC 키로 모달 닫기
document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
        closeAdminModal();
    }
});

document.getElementById('closeAdminModalButton').addEventListener('click', closeAdminModal);

document.querySelectorAll('.open-admin-modal').forEach(link => {
    link.addEventListener('click', function (event) {
        event.preventDefault();
        openAdminModal();
    });
});
