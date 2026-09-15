// 1. 회원 삭제 함수
function deleteMember(carNum) {
    if (confirm('정말 삭제하시겠습니까?\n차량번호: ' + carNum)) {
        location.href = '/member/member_delete?carNum=' + encodeURIComponent(carNum);
    }
}