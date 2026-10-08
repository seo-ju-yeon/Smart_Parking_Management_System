<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>비회원 등록</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payment/payment_style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<%
    String space = (String) request.getAttribute("id");
    String carNum = (String) request.getAttribute("carNum");
%>
<div class="main-content">
    <div id="exit" class="page">
        <form action="${pageContext.request.contextPath}/nonMember" method="post" class="form-horizontal">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <%-- 이전 단계에서 확인한 차량과 공간도 최종 POST에서 서버가 다시 검증함 --%>
            <input type="hidden" id="id" name="id" value="<%=(space != null) ? space : ""%>">
            <input type="hidden" id="carNum" name="carNum" value="<%=(carNum != null) ? carNum : ""%>">
            <h2>비회원 입차</h2>
            <div class="form-group">
                <label>연락처</label>
                <input type="text" id="regPhone" name="phone" placeholder="연락처">
            </div>
            <div class="form-group">
                <label>차량 타입</label>
                <label class="radio-item"><input type="radio" name="carType" value="1" checked required>일반</label>
                <label class="radio-item"><input type="radio" name="carType" value="3">경차</label>
                <label class="radio-item"><input type="radio" name="carType" value="4">장애인</label>
            </div>
            <button>입차 등록</button>
        </form>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/common/function.js"></script>
</body>
</html>
