// 최고관리자 접근 차단 모달 제어
function openAdminModal() {
    document.getElementById('adminModal').classList.add('show');
}

function closeAdminModal() {
    document.getElementById('adminModal').classList.remove('show');
}

// 슈퍼관리자 접근 차단 모달 제어
function openSuperModal() {
    document.getElementById('superModal').classList.add('show');
}

function closeSuperModal() {
    document.getElementById('superModal').classList.remove('show');
}

// 모달 바깥 영역을 클릭하면 닫음
document.getElementById('adminModal').addEventListener('click', function (e) {
    if (e.target === this) closeAdminModal();
});
document.getElementById('superModal').addEventListener('click', function (e) {
    if (e.target === this) closeSuperModal();
});
// ESC 키로 모달 닫기
document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') { closeAdminModal(); closeSuperModal(); }
});

document.getElementById('closeAdminModalButton').addEventListener('click', closeAdminModal);
document.getElementById('closeSuperModalButton').addEventListener('click', closeSuperModal);

document.querySelectorAll('.open-admin-modal').forEach(link => {
    link.addEventListener('click', function (event) {
        event.preventDefault();
        openAdminModal();
    });
});

document.querySelectorAll('.open-super-modal').forEach(link => {
    link.addEventListener('click', function (event) {
        event.preventDefault();
        openSuperModal();
    });
});
