// 공통 사이드바 메뉴의 시계, 확인창, 관리자 하위 메뉴를 처리합니다.

function updateClock() {
    const clockElement = document.getElementById('liveClock');
    if (!clockElement) {
        return;
    }

    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');

    clockElement.textContent =
        `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

// 메뉴 이동 전에 사용자 확인이 필요한 동작을 공통 함수로 처리합니다.
function confirmAddManager() {
    return window.confirm('관리자 추가 페이지로 이동하시겠습니까?');
}

function confirmLogout() {
    return window.confirm('로그아웃을 하시겠습니까?');
}

function toggleDropdown() {
    const dropdown = document.getElementById('adminSubMenu');
    if (dropdown) {
        dropdown.classList.toggle('show');
    }
}

const dropdownButton = document.querySelector('.dropbtn');
if (dropdownButton) {
    dropdownButton.addEventListener('click', function (event) {
        event.preventDefault();
        toggleDropdown();
    });
}

const addManagerLink = document.querySelector('.confirm-add-manager');
if (addManagerLink) {
    addManagerLink.addEventListener('click', function (event) {
        if (!confirmAddManager()) event.preventDefault();
    });
}

const logoutLink = document.querySelector('.confirm-logout');
if (logoutLink) {
    logoutLink.addEventListener('click', function (event) {
        if (!confirmLogout()) event.preventDefault();
    });
}

// 기존 공통 기능에서 호출하는 화면 전환 함수는 호환성을 위해 유지합니다.
function showPage(pageId) {
    const targetPage = document.getElementById(pageId);
    if (!targetPage) {
        return;
    }

    document.querySelectorAll('.page')
        .forEach(page => page.classList.remove('active'));
    targetPage.classList.add('active');

    if (pageId === 'dashboard' && typeof renderParkingGrid === 'function') {
        renderParkingGrid();
    }
    if (pageId === 'memberList' && typeof renderMemberTable === 'function') {
        renderMemberTable();
    }
}

// 관리자 메뉴 밖을 클릭하면 열린 하위 메뉴를 닫습니다.
window.addEventListener('click', function (event) {
    if (event.target.matches('.dropbtn')) {
        return;
    }

    document.querySelectorAll('.dropdown-content.show')
        .forEach(dropdown => dropdown.classList.remove('show'));
});

// 페이지가 준비될 때까지 기다리지 않아도 메뉴 요소는 이미 출력된 상태입니다.
updateClock();
window.setInterval(updateClock, 1000);
