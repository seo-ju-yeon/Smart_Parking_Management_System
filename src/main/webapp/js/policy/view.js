document.querySelectorAll('.navigation-button').forEach(button => {
    button.addEventListener('click', function () {
        window.location.href = this.dataset.url;
    });
});

const applyPolicyButton = document.querySelector('.apply-policy-button');
if (applyPolicyButton) {
    applyPolicyButton.addEventListener('click', function () {
        const message = '이 정책을 현재 주차 요금 정책으로 즉시 적용하시겠습니까?';
        if (window.confirm(message)) {
            window.location.href = this.dataset.url;
        }
    });
}
