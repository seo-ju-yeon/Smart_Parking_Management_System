const form = document.getElementById("searchForm");
const keywordInput = document.querySelector("input[name='keyword']");
const slots = document.querySelectorAll(".slot-item");

// 차 번호 조회
function selectCarNum() {
    const keyword = keywordInput.value.trim();
    if (keyword === "") {
        slot.forEach(slot => {
            if (slot.classList.contains("occupied")) {
                slot.style.display = "";
            } else {
                slot.style.display = "none";
            }
        });
        return;
    }
    slots.forEach(slot => {

        // 주차 구역이 비어있다면 숨기기
        if (!slot.classList.contains("occupied")) {
            slot.style.display = "none";
            return;
        }

        // 조회할 차량이 아니라면 숨기기
        const carNum = slot.dataset.carnum;
        if (!carNum) {
            slot.style.display = "none";
            return;
        }

        // 차량 번호 내에 검색 항목이 포함되어 있다면 출력, 아니라면 숨기기
        if (carNum.includes(keyword)) {
            slot.style.display = "";
        } else {
            slot.style.display = "none";
        }
    })
}

// 취소 버튼 클릭시 조회 리셋
function cancelCarNum() {
    keywordInput.value = "";
    slots.forEach(slot => {
        slot.style.display = "";
    });
}