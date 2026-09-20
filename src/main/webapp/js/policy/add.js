// 유효성 검사
function registerMember() {
    if(confirm("이 설정으로 새로운 요금 정책을 등록하시겠습니까?")) {
        return true;
    }
    return false;
}

document.getElementById('cancelPolicyButton').addEventListener('click', function () {
    window.location.href = this.dataset.url;
});
