let parkingSlots = [];
for (let i = 1; i <= 20; i++) {
    let slot = {id: 'A' + i, carNum: null, entryTime: null};
    if (i <= 10) {
        slot.entryTime = `\${String(9 + Math.floor(i/2)).padStart(2, '0')}:\${i % 2 == 0 ? '40' : '30'}`;
    }
    parkingSlots.push(slot);
}

function renderParkingGrid() {
    const grid = document.getElementById('parkingGrid');
    if (!grid) return; // grid가 없으면 실행 안 함

    grid.innerHTML = '';

    // 데이터가 있는지 확인
    if (typeof parkingSlots === 'undefined') {
        console.error("parkingSlots 데이터가 정의되지 않았습니다.");
        return;
    }

    parkingSlots.forEach(slot => {
        const div = document.createElement('div');
        // 클래스 설정
        div.className = 'parking-slot' + (slot.carNum ? ' occupied' : '');

        // HTML 구성 (따옴표 방식 사용)
        let html = '<h3>' + slot.id + '</h3>';
        if (slot.carNum) {
            html += '<p>' + slot.carNum + '</p>';
            html += '<p>' + slot.entryTime + '</p>';
        } else {
            html += '<p>공차</p>';
        }

        div.innerHTML = html;
        grid.appendChild(div);
    });
}

// 2. 페이지 로드 시 바로 실행되도록 호출
window.onload = function() {
    renderParkingGrid();
};

let currentPage = 1;
let itemsPerPage = 10;
let isLoggedIn = false;

function login() {
    const id = document.getElementById('loginId').value;
    const pw = document.getElementById('loginPw').value;

    if (id && pw) {
        isLoggedIn = true;
        document.getElementById('loginPage').classList.remove('active');
        document.getElementById('dashboard').classList.add('active');
        document.querySelectorAll('#navMenu li')[0].classList.add('active');
        renderParkingGrid();
        showMessage('로그인 성공!');
    } else {
        showMessage('아이디와 비밀번호를 입력해주세요.');
    }
}

function processEntry() {
    const carNum = document.getElementById('entryCarNum').value;
    if (!carNum) {
        showMessage('차량번호를 입력해주세요.');
        return;
    }

    const emptySlot = parkingSlots.find(slot => !slot.carNum);
    if (emptySlot) {
        emptySlot.carNum = carNum;
        emptySlot.entryTime = new Date().toLocaleTimeString('ko-KR', {hour: '2-digit', minute: '2-digit'});
        showMessage(`${emptySlot.id}에 입차 완료되었습니다.`);
        document.getElementById('entryCarNum').value = '';
    } else {
        showMessage('주차 공간이 없습니다.');
    }
}

function processExit() {
    const carNum = document.getElementById('exitCarNum').value;
    const slot = parkingSlots.find(s => s.carNum === carNum);

    if (slot) {
        slot.carNum = null;
        slot.entryTime = null;
        showMessage('출차 처리가 완료되었습니다.');
        document.getElementById('exitCarNum').value = '';
    } else {
        showMessage('해당 차량을 찾을 수 없습니다.');
    }
}

function registerMember() {
    const carNum = document.getElementById('regCarNum').value;
    const owner = document.getElementById('regOwner').value;
    const carType = document.getElementById('regCarType').value;
    const phone = document.getElementById('regPhone').value;

    if (!carNum || !owner || !carType || !phone) {
        showMessage('모든 정보를 입력해주세요.');
        return;
    }

    if (members.find(m => m.carNum === carNum)) {
        showMessage('이미 등록된 차량번호입니다.');
        return;
    }

    const today = new Date();
    const endDate = new Date(today.setFullYear(today.getFullYear() + 1));

    members.push({
        carNum: carNum,
        owner: owner,
        carType: carType,
        phone: phone,
        startDate: new Date().toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0]
    });

    showMessage('회원 등록이 완료되었습니다.');
    document.getElementById('regCarNum').value = '';
    document.getElementById('regOwner').value = '';
    document.getElementById('regCarType').value = '';
    document.getElementById('regPhone').value = '';
}

function renderMemberTable() {
    const tbody = document.getElementById('memberTableBody');
    tbody.innerHTML = '';

    const start = (currentPage - 1) * itemsPerPage;
    const end = start + itemsPerPage;
    const pageMembers = members.slice(start, end);

    pageMembers.forEach((member, index) => {
        const tr = document.createElement('tr');
        tr.onclick = () => viewMemberDetail(member.carNum);
        tr.innerHTML = `
                    <td>${start + index + 1}</td>
                    <td>${member.carNum}</td>
                    <td>${member.owner}</td>
                    <td>${member.startDate}</td>
                    <td>${member.endDate}</td>
                `;
        tbody.appendChild(tr);
    });

    const totalPages = Math.ceil(members.length / itemsPerPage);
    document.getElementById('pageInfo').textContent = `${currentPage} / ${totalPages}`;
}

function changePage(direction) {
    const totalPages = Math.ceil(members.length / itemsPerPage);
    currentPage += direction;
    if (currentPage < 1) currentPage = 1;
    if (currentPage > totalPages) currentPage = totalPages;
    renderMemberTable();
}

function searchMember() {
    const searchNum = document.getElementById('searchCarNum').value.trim();
    if (!searchNum) {
        showMessage('차량번호를 입력해주세요.');
        return;
    }

    const last4 = searchNum.slice(-4);
    const matches = members.filter(m => m.carNum.includes(last4));

    const result = document.getElementById('searchResult');

    if (matches.length === 0) {
        showMessage('등록된 회원이 없습니다.');
        result.innerHTML = '';
    } else if (matches.length === 1) {
        showMemberDetail(matches[0]);
    } else {
        result.innerHTML = '<div class="member-list"><h3>다수 회원</h3></div>';
        const list = result.querySelector('.member-list');
        matches.forEach(member => {
            const div = document.createElement('div');
            div.className = 'member-item';
            div.innerHTML = `차량 번호: ${member.carNum} | 소유주 전화번호: ${member.phone}`;
            div.onclick = () => showMemberDetail(member);
            list.appendChild(div);
        });
    }
}

function showMemberDetail(member) {
    const result = document.getElementById('searchResult');
    result.innerHTML = `
                <div class="member-detail">
                    <div class="form-group">
                        <label>차량 번호</label>
                        <input type="text" value="${member.carNum}" readonly>
                    </div>
                    <div class="form-group">
                        <label>차종</label>
                        <input type="text" id="editCarType" value="${member.carType}">
                    </div>
                    <div class="form-group">
                        <label>소유주 이름</label>
                        <input type="text" id="editOwner" value="${member.owner}">
                    </div>
                    <div class="form-group">
                        <label>소유주 전화번호</label>
                        <input type="text" id="editPhone" value="${member.phone}">
                    </div>
                    <div class="form-group">
                        <label>구독 시작일</label>
                        <input type="date" id="editStartDate" value="${member.startDate}">
                    </div>
                    <div class="form-group">
                        <label>구독 만료일</label>
                        <input type="date" id="editEndDate" value="${member.endDate}">
                    </div>
                    <div class="detail-buttons">
                        <button onclick="updateMember('${member.carNum}')">수정</button>
                        <button class="secondary" onclick="extendMembership('${member.carNum}')">추가</button>
                        <button class="danger" onclick="confirmDelete('${member.carNum}')">삭제</button>
                    </div>
                </div>
            `;
}

function viewMemberDetail(carNum) {
    const member = members.find(m => m.carNum === carNum);
    if (member) {
        showPage('memberSearch');
        document.getElementById('searchCarNum').value = carNum;
        showMemberDetail(member);
    }
}

function updateMember(carNum) {
    const member = members.find(m => m.carNum === carNum);
    if (member) {
        member.carType = document.getElementById('editCarType').value;
        member.owner = document.getElementById('editOwner').value;
        member.phone = document.getElementById('editPhone').value;
        member.startDate = document.getElementById('editStartDate').value;
        member.endDate = document.getElementById('editEndDate').value;
        showMessage('회원 정보가 수정되었습니다.');
    }
}

function extendMembership(carNum) {
    const member = members.find(m => m.carNum === carNum);
    if (member) {
        const endDate = new Date(member.endDate);
        endDate.setMonth(endDate.getMonth() + 1);
        member.endDate = endDate.toISOString().split('T')[0];
        document.getElementById('editEndDate').value = member.endDate;
        showMessage('구독 기간이 1개월 연장되었습니다.');
    }
}

let deleteTarget = null;
function confirmDelete(carNum) {
    deleteTarget = carNum;
    document.getElementById('confirmMessage').textContent = '회원 정보를 삭제하시겠습니까?';
    document.getElementById('confirmModal').classList.add('active');
}

function confirmAction(confirmed) {
    document.getElementById('confirmModal').classList.remove('active');
    if (confirmed && deleteTarget) {
        const index = members.findIndex(m => m.carNum === deleteTarget);
        if (index > -1) {
            members.splice(index, 1);
            showMessage('회원 정보가 삭제되었습니다.');
            document.getElementById('searchResult').innerHTML = '';
            document.getElementById('searchCarNum').value = '';
        }
    }
    deleteTarget = null;
}

function savePricing() {
    showMessage('요금 정책이 저장되었습니다.');
}

function showMessage(message) {
    document.getElementById('modalMessage').textContent = message;
    document.getElementById('modal').classList.add('active');
}

function closeModal() {
    document.getElementById('modal').classList.remove('active');
}

// Initialize
renderParkingGrid();