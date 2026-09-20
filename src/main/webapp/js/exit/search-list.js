function formatDateTime(dtStr) {
    if(!dtStr || dtStr === "null" || dtStr === "") return "-";
    return dtStr.replace('T', ' ').substring(0, 16);
}
document.addEventListener("DOMContentLoaded", function() {
    document.querySelectorAll(".time").forEach(el => {
        el.value = formatDateTime(el.value);
    });
});
