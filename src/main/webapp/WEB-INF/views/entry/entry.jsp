<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>입차</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<%
    String space = (String) request.getAttribute("id");
    // [버그수정] forward 시에는 fail도 getAttribute로 받아야 함
    String failInput = request.getParameter("fail");
    if (failInput == null) failInput = (String) request.getAttribute("fail");

    if ("false".equals(failInput)) {
        out.println("<script>alert('이미 입차된 구역입니다.'); history.back();</script>");
    }
    if ("over".equals(failInput)) {
        out.println("<script>alert('잘못된 형식의 차량 번호입니다.'); history.back();</script>");
    }
    if ("already".equals(failInput)) {
        out.println("<script>alert('이미 입차된 차량입니다.'); history.back();</script>");
    }
    if ("nullId".equals(failInput)) {
        out.println("<script>alert('올바른 주차구역을 지정해주세요.'); history.back();</script>");
    }
%>
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
            <button onclick="processEntry()">입차 등록</button>
        </form>
    </div>
</div>
<script src="${pageContext.request.contextPath}/JS/menu.js"></script>
<script src="${pageContext.request.contextPath}/JS/function.js"></script>
</body>
</html>
