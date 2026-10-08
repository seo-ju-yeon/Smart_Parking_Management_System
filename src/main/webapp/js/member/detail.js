const deleteMemberForm = document.getElementById('deleteMemberForm');
deleteMemberForm.addEventListener('submit', function (event) {
    const carNum = this.elements.carNum.value;
    if (!window.confirm('정말 삭제하시겠습니까?\n차량번호: ' + carNum)) {
        event.preventDefault();
    }
});
