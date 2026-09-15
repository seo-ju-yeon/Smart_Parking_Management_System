<%@ page import="org.example.smart_parking_260219.dto.ParkingDTO" %>
<%@ page import="org.example.smart_parking_260219.service.ParkingService" %>
<%@ page import="java.util.Objects" %>
<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%@ page import="org.example.smart_parking_260219.service.MemberService" %>
<%@ page import="java.sql.SQLException" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>출차</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payment/payment_style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<%
    // [버그수정] Controller에서 setAttribute("parkingDTO")로 넘긴 경우 우선 사용
    // 없으면 carNum으로 직접 DB 조회 (다양한 진입 경로 대응)
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

    // parkingDTO가 끝내 null이면 안전하게 뒤로 보냄
    if (parkingDTO == null) {
        out.println("<script>alert('주차 중인 차량 정보를 찾을 수 없습니다.'); history.back();</script>");
        return;
    }
%>
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
            <button type="submit" onclick="registerMember()">정산</button>
        </form>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/common/menu.js"></script>
<script src="${pageContext.request.contextPath}/js/common/function.js"></script>
<script>
    function formatDateTime(dtStr) {
        if(!dtStr || dtStr === "null" || dtStr === "") return "-";
        return dtStr.replace('T', ' ').substring(0, 16);
    }
    document.addEventListener("DOMContentLoaded", function() {
        document.querySelectorAll(".time").forEach(el => {
            el.value = formatDateTime(el.value);
        });
    });
</script>
</body>
</html>
