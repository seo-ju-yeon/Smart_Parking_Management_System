// Navigation
document.querySelectorAll('#navMenu li').forEach(item => {
    item.addEventListener('click', function() {
        if (!isLoggedIn && this.dataset.page !== 'login') return;
        document.querySelectorAll('#navMenu li').forEach(li => li.classList.remove('active'));
        this.classList.add('active');
        showPage(this.dataset.page);
    });
});

function showPage(pageId) {
    document.querySelectorAll('.page').forEach(page => page.classList.remove('active'));
    document.getElementById(pageId).classList.add('active');

    if (pageId === 'dashboard') renderParkingGrid();
    if (pageId === 'memberList') renderMemberTable();
}
