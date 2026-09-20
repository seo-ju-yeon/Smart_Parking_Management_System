        <%@ page import="org.example.smart_parking_260219.dto.ParkingDTO" %>
<%@ page import="org.example.smart_parking_260219.service.ParkingService" %>
<%@ page import="org.example.smart_parking_260219.service.PaymentService" %>
<%@ page import="org.example.smart_parking_260219.util.MapperUtil" %>
<%@ page import="org.example.smart_parking_260219.service.FeePolicyService" %>
<%@ page import="org.example.smart_parking_260219.vo.FeePolicyVO" %>
<%@ page import="org.example.smart_parking_260219.dto.PaymentDTO" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String carNum = request.getParameter("carNum");

    // 1. 이전 페이지(출차화면)에서 보낸 carType을 받음
    String carTypeParam = request.getParameter("carType");

    // 2. 만약 값이 없으면(직접 진입 등) 기존 DB 정보에서 가져옴
    ParkingDTO parkingDTO = null;
    int selectedCarType = (carTypeParam != null) ? Integer.parseInt(carTypeParam) : parkingDTO.getCarType();

    // 2. 만약 Forward로 올 경우
    if (carNum == null || carNum.isEmpty()) {
        carNum = (String) request.getAttribute("carNum");
    }
    // 3. 데이터가 없을 때 DB 조회를 시도하면 에러가 나므로 조건문 처리
    parkingDTO = null;
    PaymentDTO paymentDTO = null;
    int calculatedFee = 0;
    int discountAmount = 0;
    int finalFee = 0;

    if (carNum != null && !carNum.isEmpty() && !"null".equals(carNum)) {
        parkingDTO = ParkingService.INSTANCE.getParkingByCarNum(carNum.trim());
//        if (parkingDTO != null) {
//            // 주차 정보가 있을 때만 실행
//            paymentDTO = PaymentService.INSTANCE.getPayment(parkingDTO.getParkingId());
//        }

        // DB에서 차량을 못 찾았을 경우의 처리
        if (parkingDTO == null) {
            request.setAttribute("pageAlertMessage", "현재 주차 중인 차량이 아닙니다.");
            request.setAttribute("pageAlertAction", "redirect");
            request.setAttribute("pageAlertUrl", request.getContextPath() + "/dashboard");
            request.getRequestDispatcher("/WEB-INF/views/common/alert.jsp")
                    .forward(request, response);
            return;
        }
        calculatedFee = PaymentService.INSTANCE.calculateFeeLogic(parkingDTO);
        discountAmount = PaymentService.INSTANCE.calculateDiscountLogic(calculatedFee, selectedCarType,
                MapperUtil.INSTANCE.getInstance().map(FeePolicyService.getInstance().getPolicy(), FeePolicyVO.class));
        finalFee = calculatedFee - discountAmount;
    } else {
        request.setAttribute("pageAlertMessage", "차량 정보가 없습니다.");
        request.setAttribute("pageAlertAction", "back");
        request.getRequestDispatcher("/WEB-INF/views/common/alert.jsp")
                .forward(request, response);
        return;
    }

    long totalTime = 0;
    if (parkingDTO != null && parkingDTO.getEntryTime() != null) {
        // 현재 시간과 입차 시간의 차이 계산
        java.time.Duration duration = java.time.Duration.between(
                parkingDTO.getEntryTime(),
                java.time.LocalDateTime.now()
        );
        totalTime = duration.toMinutes();
    }

    // 외부 JavaScript가 사용할 시간 값은 실행 코드가 아니라 data-* 속성으로 전달한다.
    String entryTimeStr = (parkingDTO != null && parkingDTO.getEntryTime() != null)
            ? parkingDTO.getEntryTime().toString() : "";
    String exitTimeStr = (parkingDTO != null && parkingDTO.getExitTime() != null)
            ? parkingDTO.getExitTime().toString() : java.time.LocalDateTime.now().toString();
%>
<html>
<head>
    <title>Payment</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payment/payment_style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payment/modal.css">
</head>
<body data-context-path="${pageContext.request.contextPath}"
      data-entry-time="<%=entryTimeStr%>"
      data-exit-time="<%=exitTimeStr%>">
<div id="customModal" class="modal-overlay receipt-modal-overlay">
    <div class="modal-content receipt-modal-content">
        <div id="modalBody" class="modal-body receipt-modal-body">
        </div>
        <div class="modal-footer receipt-modal-footer"></div>
    </div>
</div>
<!-- Navigation -->
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <!-- Content -->
    <div id="register" class="page">
        <h2>정산</h2>
        <form name="payment" action="${pageContext.request.contextPath}/payment/payment" method="post">
            <div class="form-group">
                <input type="hidden" name="carType" value="<%=selectedCarType%>"/>
                <label>차량 번호</label>
                <input type="text" name ="carNum" id="carNum" placeholder="차량번호 8자리" maxlength="8" value="<%=carNum%>">
            </div>
            <div class="form-group">
                <label>결제 타입</label>
                <div class="radio-group">
                    <label class="radio-item"><input type="radio" name="paymentType" value="1"
                        <% if (selectedCarType != 2) {
                out.println("checked");
            } %>>카드</label>
                    <label class="radio-item"><input type="radio" name="paymentType" value="2">현금</label>
                    <label class="radio-item"><input type="radio" name="paymentType" value="3"
                    <% if (selectedCarType == 2) {
                out.println("checked");
            } %>>월정액</label>
                </div>
            </div>
            <div class="form-group">
                <label>총 주차 시간</label>
                <input type="text" id="totalParkingTime" placeholder="총 주차 시간" name="totalTime" value="<%=totalTime%>분">
            </div>
            <div class="form-group">
                <label>할인 전 요금</label>
                <input type="text" name="calculatedFee" id="calculatedFee" placeholder="할인 전 요금" value="<%=calculatedFee%>">
            </div>
            <div class="form-group">
                <label>할인액</label>
                <input type="text" name="discountAmount" id="discountAmount" placeholder="할인액" value="<%=discountAmount%>">
            </div>
            <div class="form-group">
                <label>총 주차 요금</label>
                <input type="text" name="finalFee" id="finalFee" placeholder="총 주차 요금" value="<%=finalFee%>">
            </div>
            <div>
                <input type="checkbox" name="receipt" id="receipt"><label>영수증 출력</label>
            </div>
            <div>
                <button type="button" id="showReceiptButton" class="btn btn-primary">확인</button>
            </div>

        </form>
    </div>
    <!-- 영수증 -->
    <div id="printArea" class="receipt-print-area">
        <div class="receipt-paper">

            <div class="receipt-header">
                <h1 class="receipt-title">영 수 증</h1>
                <span class="receipt-copy-label">(공급받는자용)</span>
            </div>

            <table class="receipt-table supplier-table">
                <tr>
                    <td rowspan="4" class="supplier-label">공급자</td>
                    <td class="business-number-label">사업자번호</td>
                    <td colspan="3" class="business-number-value">123-45-67890</td>
                </tr>
                <tr class="receipt-row">
                    <td class="receipt-label-cell">상 호</td>
                    <td class="receipt-value-cell">스마트 주차장</td>
                    <td class="receipt-label-cell name-label">성명</td>
                    <td class="receipt-right-value">홍길동 (인)</td>
                </tr>
                <tr class="receipt-row">
                    <td class="receipt-label-cell">주 소</td>
                    <td colspan="3" class="receipt-address">대구광역시 중구 중앙대로 123</td>
                </tr>
                <tr class="receipt-row">
                    <td class="receipt-label-cell no-bottom-border">업 태</td>
                    <td class="receipt-value-cell no-bottom-border">서비스</td>
                    <td class="receipt-label-cell no-bottom-border">종목</td>
                    <td class="receipt-value-last">주차장업</td>
                </tr>
            </table>

            <table class="receipt-table summary-table">
                <tr class="summary-row">
                    <td class="summary-label">작성일</td>
                    <td class="summary-date"><%=java.time.LocalDate.now()%></td>
                    <td class="summary-label">합계금액</td>
                    <td class="summary-amount"><span id="p-finalFee"></span></td>
                </tr>
            </table>

            <table class="receipt-table detail-table">
                <thead class="detail-table-head">
                <tr class="detail-header-row">
                    <th class="detail-bordered-cell">항 목</th>
                    <th class="detail-bordered-cell">내 용</th>
                    <th class="detail-bottom-cell">금 액</th>
                </tr>
                </thead>
                <tbody class="detail-table-body">
                <tr class="detail-row">
                    <td class="detail-bordered-cell">차량번호</td>
                    <td class="detail-bordered-cell"><span id="p-carNum"></span></td>
                    <td class="detail-bottom-cell">-</td>
                </tr>
                <tr class="detail-row">
                    <td class="detail-bordered-cell">주차시간</td>
                    <td class="detail-bordered-cell"><span id="p-totalTime"></span></td>
                    <td class="detail-bottom-cell"><span id="p-calcFee"></span></td>
                </tr>
                <tr class="detail-row">
                    <td class="detail-bordered-cell">할인액</td>
                    <td class="detail-bordered-cell">-</td>
                    <td class="detail-bottom-cell">-<span id="p-discount"></span></td>
                </tr>
                </tbody>
                <tfoot>
                <tr class="receipt-total-row">
                    <td colspan="2" class="receipt-total-label">합 계 (VAT포함)</td>
                    <td class="receipt-total-value"><span id="p-finalFee-total"></span></td>
                </tr>
                </tfoot>
            </table>

            <div class="receipt-thanks">감사합니다. 또 이용해 주십시오.</div>

            <div class="receipt-actions">
                <button type="button" class="receipt-action-button receipt-confirm-button">확인</button>
                <button type="button" class="receipt-action-button receipt-cancel-button">취소</button>
            </div>
        </div>
    </div>
    <!-- 영수증 -->
</div>
<script src="${pageContext.request.contextPath}/js/common/function.js"></script>
<script src="${pageContext.request.contextPath}/js/payment/payment.js"></script>
</body>
</html>
