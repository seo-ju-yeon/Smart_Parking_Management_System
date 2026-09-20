// 목록 행 이동과 갱신 제출이 서로 간섭하지 않도록 이벤트를 분리한다.
document.querySelectorAll('.member-row').forEach(row => {
    row.addEventListener('click', function () {
        window.location.href = this.dataset.url;
    });
});

document.querySelectorAll('.member-action-cell').forEach(cell => {
    cell.addEventListener('click', function (event) {
        event.stopPropagation();
    });
});

document.querySelectorAll('.renew-button:not(:disabled)').forEach(button => {
    button.addEventListener('click', function (event) {
        event.stopPropagation();
        if (!window.confirm(this.dataset.carNum + ' 1개월 갱신하시겠습니까?')) {
            event.preventDefault();
        }
    });
});
