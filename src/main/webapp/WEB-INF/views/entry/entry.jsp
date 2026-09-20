<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String space = (String) request.getAttribute("id");
    // forward 시에는 fail도 request attribute에서 확인
    String failInput = request.getParameter("fail");
    if (failInput == null) failInput = (String) request.getAttribute("fail");

    String alertMessage = null;
    if ("false".equals(failInput)) alertMessage = "이미 입차된 구역입니다.";
    if ("over".equals(failInput)) alertMessage = "잘못된 형식의 차량 번호입니다.";
    if ("already".equals(failInput)) alertMessage = "이미 입차된 차량입니다.";
    if ("nullId".equals(failInput)) alertMessage = "올바른 주차구역을 지정해주세요.";

    if (alertMessage != null) {
        request.setAttribute("pageAlertMessage", alertMessage);
        request.setAttribute("pageAlertAction", "back");
        request.getRequestDispatcher("/WEB-INF/views/common/alert.jsp")
                .forward(request, response);
        return;
    }
%>
<html>
<head>
    <title>입차</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <div id="entry" class="page">
        <h2>입차</h2>
        <%-- [버그수정] ../input 상대경로 → contextPath 기준 절대경로 --%>
        <form action="${pageContext.request.contextPath}/input" method="post" class="form-horizontal">
            <div class="form-group">
                <label>주차 자리</label>
                <input type="text" id="parkingSlot" placeholder="A1 - A20" name="spaceId"
                       value="<%=(space != null) ? space : ""%>" <%= (space != null) ? "readonly" : "" %>>
                <label>차량 번호</label>
                <input type="text" id="entryCarNum" placeholder="차량번호 8자리" name="carNum">
            </div>
            <button type="submit">입차 등록</button>
        </form>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/common/function.js"></script>
</body>
</html>
