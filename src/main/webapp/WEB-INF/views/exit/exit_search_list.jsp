<%@ page import="org.example.smart_parking_260219.dto.ParkingDTO" %>
<%@ page import="org.example.smart_parking_260219.service.ParkingService" %>
<%@ page import="java.util.Objects" %>
<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%@ page import="org.example.smart_parking_260219.service.MemberService" %>
<%@ page import="java.sql.SQLException" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Controller가 전달한 주차 정보를 우선 사용하고, 없으면 차량번호로 조회한다.
    ParkingDTO parkingDTO = (ParkingDTO) request.getAttribute("parkingDTO");
    String carNum = request.getParameter("carNum");
    MemberDTO memberDTO;
    try {
        memberDTO = MemberService.INSTANCE.getOneMember(carNum);
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }

    if (parkingDTO == null) {
        if (carNum == null || carNum.isEmpty()) {
            carNum = (String) request.getAttribute("carNum");
        }
        if (carNum != null && !carNum.isEmpty()) {
            parkingDTO = ParkingService.INSTANCE.getParkingByCarNum(carNum);
        }
    }

    if (parkingDTO == null) {
        request.setAttribute("pageAlertMessage", "주차 중인 차량 정보를 찾을 수 없습니다.");
        request.setAttribute("pageAlertAction", "back");
        request.getRequestDispatcher("/WEB-INF/views/common/alert.jsp")
                .forward(request, response);
        return;
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
            <div class="form-group">
                <label>주차 구역</label>
                <input type="text" id="spaceId" placeholder="주차 구역" name="spaceId"
                       value="<%=parkingDTO.getSpaceId()%>" readonly>
            </div>
            <div class="form-group">
                <label>차량 번호</label>
                <input type="text" id="regCarNum" placeholder="차량번호 8자리" maxlength="8" name="carNum"
                       value="<%=parkingDTO.getCarNum()%>" readonly>
            </div>
            <div class="form-group">
                <label>차량 타입</label>
                <div class="radio-group">
                    <%
                        // 월정액 회원인 경우
                        if (memberDTO != null && Objects.requireNonNull(memberDTO).isSubscribed()) {
                    %>
                    <label class="radio-item"><input type="radio" name="carType" value="2" checked>월정액</label>
                    <%
                    } else {
                    %>
                    <label class="radio-item"><input type="radio" name="carType" value="1" checked>일반</label>
                    <label class="radio-item"><input type="radio" name="carType" value="3">경차</label>
                    <label class="radio-item"><input type="radio" name="carType" value="4">장애인</label>
                    <%
                        }
                    %>
                </div>
            </div>
            <div class="form-group">
                <label>입차 시간</label>
                <input type="text" class="time" id="entryTime" placeholder="입차 시간" name="entryTime"
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
