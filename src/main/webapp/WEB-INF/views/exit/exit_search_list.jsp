<%@ page import="org.example.smart_parking_260219.dto.ParkingDTO" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Controller가 대상 검증을 마친 주차 기록만 화면에 표시함
    ParkingDTO parkingDTO = (ParkingDTO) request.getAttribute("parkingDTO");

    if (parkingDTO == null) {
        request.setAttribute("pageAlertMessage", "주차 중인 차량 정보를 찾을 수 없습니다.");
        request.setAttribute("pageAlertAction", "back");
        request.getRequestDispatcher("/WEB-INF/views/common/alert.jsp")
                .forward(request, response);
        return;
    }

    // DB에 저장된 차량 유형 코드를 화면 표시용 이름으로 변환함
    String carTypeName;
    switch (parkingDTO.getCarType()) {
        case 1:
            carTypeName = "일반";
            break;
        case 2:
            carTypeName = "월정액";
            break;
        case 3:
            carTypeName = "경차";
            break;
        case 4:
            carTypeName = "장애인";
            break;
        default:
            carTypeName = "알 수 없음";
    }
%>
<html>
<head>
    <title>출차</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payment/payment_style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <div id="register" class="page">
        <h2>출차</h2>
        <form action="${pageContext.request.contextPath}/get" method="post" class="form-horizontal">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <%-- 화면에 표시된 값 대신 주차 기록 ID로 결제 대상을 전달함 --%>
            <input type="hidden" name="parkingId" value="<%=parkingDTO.getParkingId()%>">
            <div class="form-group">
                <label>주차 구역</label>
                <input type="text" id="spaceId" placeholder="주차 구역"
                       value="<%=parkingDTO.getSpaceId()%>" readonly>
            </div>
            <div class="form-group">
                <label>차량 번호</label>
                <input type="text" id="regCarNum" placeholder="차량번호 8자리" maxlength="8"
                       value="<%=parkingDTO.getCarNum()%>" readonly>
            </div>
            <div class="form-group">
                <label>차량 타입</label>
                <%-- 입차 시 확정된 차량 유형을 표시하며 정산 요청에는 전송하지 않음 --%>
                <input type="text" id="carType" value="<%=carTypeName%>" readonly>
            </div>
            <div class="form-group">
                <label>입차 시간</label>
                <input type="text" class="time" id="entryTime" placeholder="입차 시간"
                       value="<%=parkingDTO.getEntryTime()%>" readonly>
            </div>
            <button type="submit">정산</button>
        </form>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/common/function.js"></script>
<script src="${pageContext.request.contextPath}/js/exit/search-list.js"></script>
</body>
</html>
