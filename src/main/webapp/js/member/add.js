// DOM 로드 후 실행
document.addEventListener('DOMContentLoaded', function() {
    // 1. 테이블 행 효과
    const rows = document.querySelectorAll('.clickable-row');
    rows.forEach(row => {
        row.addEventListener('mouseenter', function() { this.style.backgroundColor = '#e3f2fd'; });
        row.addEventListener('mouseleave', function() { this.style.backgroundColor = ''; });
    });

    // 2. 차량번호 중복 확인 AJAX (요소가 있을 때만 실행)
    const carNumInput = document.getElementById('carNum');
    if (carNumInput) {
        carNumInput.addEventListener('blur', function() {
            const carNum = this.value.trim();
            const msg = document.getElementById('carNumMsg');
            if (!carNum || !msg) return;

            fetch('/member/check_carnum?carNum=' + encodeURIComponent(carNum))
                .then(response => response.json())
                .then(data => {
                    if (data.exists) {
                        msg.textContent = '이미 등록된 차량번호입니다.';
                        msg.style.color = 'red';
                    } else {
                        msg.textContent = '등록 가능한 차량번호입니다.';
                        msg.style.color = 'green';
                    }
                })
                .catch(err => console.error("Check Error:", err));
        });
    }
});

// STEP 1 검증 함수
function validateCarNum() {
    const carNum = document.getElementById('carNum').value.trim();
    if (!carNum) {
        alert('차량 번호를 입력해주세요.');
        return false;
    }
    return true;
}

// STEP 2 등록 검증 함수
function validateRegister() {
    const name = document.getElementById('name').value.trim();
    const phone = document.getElementById('phone').value.trim();
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;

    if (!name) { alert('이름을 입력해주세요.'); return false; }
    if (!phone) { alert('전화번호를 입력해주세요.'); return false; }

    if (startDate && endDate) {
        if (new Date(startDate) >= new Date(endDate)) {
            alert('종료일은 시작일보다 이후여야 합니다.');
            return false;
        }
    } else {
        alert('구독 기간을 입력해주세요.');
        return false;
    }
    return true;
}