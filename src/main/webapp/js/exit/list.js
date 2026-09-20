// 행 전체를 클릭하면 해당 차량의 출차 메뉴로 이동
document.addEventListener("click", function (e) {
    const row = e.target.closest(".click-row");
    if (!row) return;

    const url = row.dataset.url;
    if (url) {
        location.href = url;
    }
});

// 입차 시간 출력 형식
function formatDateTime(dtStr) {
    if(!dtStr || dtStr === "null" || dtStr === "") return "-";
    return dtStr.replace('T', ' ').substring(0, 16);
}
document.addEventListener("DOMContentLoaded", function() {
    document.querySelectorAll(".time").forEach(td => {
        td.textContent = formatDateTime(td.textContent.trim());
    });
});
