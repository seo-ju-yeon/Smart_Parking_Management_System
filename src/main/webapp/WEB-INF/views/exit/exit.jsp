<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String space = request.getParameter("id");
    String carNum = request.getParameter("carNum");
    String failInput = request.getParameter("fail");

    String alertMessage = null;
    if ("false".equals(failInput)) alertMessage = "존재하지 않는 차량입니다.";
    if ("nullId".equals(failInput)) alertMessage = "존재하지 않는 구역입니다.";

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
    <title>출차</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <div id="exit" class="page">
        <%-- [버그수정] ../output 상대경로 → contextPath 기준 절대경로 --%>
        <form action="${pageContext.request.contextPath}/output" method="post" class="form-horizontal">
            <input type="hidden" id="exitSpaceId" name="id" value="<%=(space != null) ? space : ""%>">
            <h2>출차</h2>
            <div class="form-group">
                <label>차량 번호</label>
                <input type="text" id="exitCarNum" placeholder="차량번호 8자리" name="carNum"
                       value="<%=(carNum != null) ? carNum : ""%>">
            </div>
            <button>정산</button>
        </form>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/common/function.js"></script>
</body>
</html>
