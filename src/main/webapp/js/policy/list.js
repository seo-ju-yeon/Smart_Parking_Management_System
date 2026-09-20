// 정책 행에 저장된 상세 URL로 이동한다.
document.querySelectorAll('.policy-row').forEach(row => {
    row.addEventListener('click', function () {
        window.location.href = this.dataset.url;
    });
});
