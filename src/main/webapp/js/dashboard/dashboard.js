document.addEventListener("click", e => {
    // 입출차 메뉴 이동 판단
    const slot = e.target.closest(".slot-item");
    if (!slot) return;

    // 주차 구역
    const id = slot.dataset.id;
    // 입차 여부
    const isEmpty = slot.dataset.empty === "true"
    // 차량 번호
    const carNum = slot.dataset.carnum;

    // const contextPath = window.location.pathname.split("/")[1];
    // 주차 구역이 비어있다면 입차, 비어있지 않다면 출차 메뉴로 이동
    location.href = isEmpty
        ? `${contextPath}/input?id=${id}`
        : `${contextPath}/get?id=${id}&carNum=${carNum}`;
});