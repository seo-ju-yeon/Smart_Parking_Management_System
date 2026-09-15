function validateSearch() {
    const carNum = document.getElementById('carNum').value.trim();
    if (!carNum) {
        alert('차량번호를 입력해주세요.');
        return false;
    }
    if (!/^\d{4}$/.test(carNum)) {
        alert('숫자 4자리를 입력해주세요.');
        return false;
    }
    return true;
}

