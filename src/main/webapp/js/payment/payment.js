// JSP가 data-* 속성으로 전달한 서버 값을 외부 스크립트에서 읽는다.
const contextPath = document.body.dataset.contextPath;
const entryTime = document.body.dataset.entryTime;
const exitTime = document.body.dataset.exitTime;

// 날짜 포맷팅 함수
function formatDateTime(dtStr) {
    if(!dtStr || dtStr === "null" || dtStr === "") return "-";
    return dtStr.replace('T', ' ').substring(0, 16);
}

function showReceipt() {
    // 체크박스 상태 확인
    const receiptCheckbox = document.getElementById("receipt");
    const form = document.forms['payment'];

    // 필수 데이터 확인 (차량 번호 등)
    const carNum = document.getElementById("carNum").value;
    if (!carNum || carNum === "null") {
        alert("차량 정보가 없습니다.");
        return;
    }

    // 영수증 출력 체크박스가 선택된 경우
    if (receiptCheckbox && receiptCheckbox.checked) {
        // 데이터 채우기 (기존 로직 유지)
        const totalTime = document.getElementById("totalParkingTime").value;
        const calculatedFee = document.getElementById("calculatedFee").value;
        const discountAmount = document.getElementById("discountAmount").value;
        const finalFee = document.getElementById("finalFee").value;

        document.getElementById("p-carNum").innerText = carNum;
        document.getElementById("p-totalTime").innerText = totalTime;
        document.getElementById("p-calcFee").innerText = Number(calculatedFee).toLocaleString();
        document.getElementById("p-discount").innerText = Number(discountAmount).toLocaleString();
        document.getElementById("p-finalFee").innerText = Number(finalFee).toLocaleString() + "원";
        document.getElementById("p-finalFee-total").innerText = Number(finalFee).toLocaleString() + "원";

        // 모달 표시
        const receiptDesign = document.getElementById("printArea").innerHTML;
        document.getElementById("modalBody").innerHTML = receiptDesign;
        document.getElementById("customModal").style.display = "flex";
    }
    // 체크박스가 선택되지 않은 경우 바로 결제(제출)
    else {
        if (confirm("영수증 없이 정산을 진행하시겠습니까?")) {
            alert("정산이 완료되었습니다. 대시보드로 이동합니다.");
            form.submit();
        }
    }
}

// 모달 내 '확인' 버튼 클릭 시 (영수증 출력 후 제출)
function handleConfirm() {
    if (confirm("영수증을 인쇄하시겠습니까?")) {
        printReceipt();
    }

    setTimeout(function() {
        alert("정산이 완료되었습니다. 대시보드로 이동합니다.");
        const form = document.forms['payment'];
        if (form) {
            form.submit();
        }
    }, 500);
}

function closeModal() {
    document.getElementById("customModal").style.display = "none";
}
// 인쇄 실행 함수
function printReceipt() {
    const printWindow = window.open('', '_blank', 'width=450,height=700');
    printWindow.document.write('<html><head><title>영수증 인쇄</title>');
    // 인쇄 창에서도 화면과 같은 영수증 스타일을 사용한다.
    printWindow.document.write('<link rel="stylesheet" href="' + contextPath + '/css/payment/modal.css">');
    printWindow.document.write('</head><body>');
    printWindow.document.write(document.getElementById("printArea").innerHTML);
    printWindow.document.write('</body></html>');

    printWindow.document.close();
    printWindow.focus();

    setTimeout(function() {
        printWindow.print();
        printWindow.close();
    }, 500);
}

document.getElementById('showReceiptButton').addEventListener('click', showReceipt);

// 영수증은 모달에 복제되므로 이벤트 위임으로 복제된 버튼까지 처리한다.
document.addEventListener('click', function (event) {
    if (event.target.closest('.receipt-confirm-button')) {
        handleConfirm();
    }
    if (event.target.closest('.receipt-cancel-button')) {
        closeModal();
    }
});
