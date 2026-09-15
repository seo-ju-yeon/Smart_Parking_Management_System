<%@ page import="java.time.LocalDate" %>
<%@ page import="org.example.smart_parking_260219.dto.PaymentDTO" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String targetDate = (String) request.getAttribute("targetDate");
    List<PaymentDTO> paymentList = (List<PaymentDTO>) request.getAttribute("paymentList");
%>
<html>
<head>
    <title>정산 목록</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <div id="entry" class="page">
        <h2> <%=targetDate%> 정산 목록 </h2>
        <div class="form-group">
            <div class="table-container">
                <table>
                    <thead>
                    <tr>
                        <th>번호</th>
                        <th>차량 번호</th>
                        <th>차량 종류</th>
                        <th>총 주차 시간</th>
                        <th>할인 금액</th>
                        <th>결제 금액</th>
                    </tr>
                    </thead>
                    <tbody>
                    <%
                        if (paymentList == null || paymentList.isEmpty()) {
                    %>
                    <tr>
                        <td colspan="6" style="text-align: center;">정산 내역이 없습니다.</td>
                    </tr>
                    <%
                    } else {
                        int rowNum = 1;
                        for (PaymentDTO paymentDTO : paymentList) {
                            // 루프 안에서 각 행마다 이름을 판별합니다.
                            String typeName = "";

                            int rawType = paymentDTO.getCarType();

                            if (rawType == 1) {
                                typeName = "일반";
                            } else if (rawType == 2) {
                                typeName = "월정액";
                            } else if (rawType == 3) {
                                typeName = "경차";
                            } else if (rawType == 4) {
                                typeName = "장애인";
                            } else {
                                typeName = "기타(" + rawType + ")"; // 값이 다른 경우 확인용
                            }
                    %>
                    <tr>
                        <td><%= rowNum++ %></td>
                        <td><%= paymentDTO.getCarNum() %></td>
                        <td><span class="badge"><%= typeName %></span></td>
                        <td><%= paymentDTO.getTotalTime()%>분</td>
                        <td><%= paymentDTO.getDiscountAmount() %>원</td>
                        <td><strong><%= paymentDTO.getFinalFee() %>원</strong></td>
                    </tr>
                    <%
                            }
                        }
                    %>
                    </tbody>
                </table>
            </div>
            <div class="section-card control-section">
                <form action="${pageContext.request.contextPath}/statistics/statistics" method="get" class="control-bar" id="dateForm">
                    <div class="selector-box">
                        <button type="submit" class="btn-search">뒤로가기</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
</body>
</html>