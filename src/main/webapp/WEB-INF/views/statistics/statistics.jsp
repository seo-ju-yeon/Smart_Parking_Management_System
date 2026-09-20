<%@ page import="org.example.smart_parking_260219.dto.StatisticsDTO" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // 1. 숫자 포맷터 설정
    java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");

    // 2. 데이터 가져오기 (Null 방어 포함)
    List<StatisticsDTO> hSalesList = (List<StatisticsDTO>) request.getAttribute("hourlySales");
    List<StatisticsDTO> hCountsList = (List<StatisticsDTO>) request.getAttribute("hourlyCounts");
    List<StatisticsDTO> dSalesList = (List<StatisticsDTO>) request.getAttribute("dailySales");
    List<StatisticsDTO> carTypeStatsList = (List<StatisticsDTO>) request.getAttribute("carTypeStats");
    int monthSubscribedFee = (int) request.getAttribute("monthSubscribedFee");

    if (hSalesList == null) hSalesList = java.util.Collections.emptyList();
    if (hCountsList == null) hCountsList = java.util.Collections.emptyList();
    if (dSalesList == null) dSalesList = java.util.Collections.emptyList();
    if (carTypeStatsList == null) carTypeStatsList = java.util.Collections.emptyList();

    // 3. 일 총 매출 직접 계산 (hSalesList 순회)
    long daySum = 0;
    if (hSalesList != null) {
        for (StatisticsDTO dto : hSalesList) {
            daySum += dto.getValue();
        }
    }
    String formattedDayTotal = df.format(daySum);

    // 4. 월 총 매출 직접 계산 (dSalesList 순회)
    long monthSum = 0;
    if (dSalesList != null) {
        for (StatisticsDTO dto : dSalesList) {
            monthSum += dto.getValue();
        }
    }
    String formattedMonthParking = df.format(monthSum);

    long MonthTotal = monthSum + monthSubscribedFee;
    String formattedMonthTotal = df.format(MonthTotal);
%>
<html>
<head>
    <title>매출 통계 - 주차장 관리 시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/statistics/statistics_style.css">
    <%-- chart.js CDN --%>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body data-context-path="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/common/menu.jsp" %>

<div class="main-content">
    <div id="statistics" class="page">
        <h2>매출 및 이용 통계</h2>

        <div class="section-card control-section">
            <form action="${pageContext.request.contextPath}/payment/payment_list" method="get" class="control-bar" id="dateForm">
                <div class="selector-box">
                    <div class="btn-label">조회 기준일</div>
                    <input type="date" id="statisticsDate" name="targetDate" value="${targetDate}">
                    <button type="submit" class="btn-search">조회</button>
                </div>
            </form>
        </div>

        <div class="stats-wrapper">
            <div class="section-card">
                <div class="control-bar">
                    <div class="btn-label">시간대별 통계(일별) (${targetDate})</div>
                    <div class="total-price-tag">일 총 매출: <%= formattedDayTotal %>원</div>
                </div>
                <div class="chart-area">
                    <canvas id="hourlyChart"></canvas>
                </div>
            </div>

            <div class="section-card">
                <div class="control-bar">
                    <div class="btn-label">월별 매출 현황</div>
                    <div class="total-price-tag">월 구독료 총 매출: <%= monthSubscribedFee %>원</div>
                    <div class="total-price-tag">월 주차비 총 매출: <%= formattedMonthParking %>원</div>
                    <div class="total-price-tag">월 총 매출: <%= formattedMonthTotal %>원</div>
                </div>
                <div class="chart-area">
                    <canvas id="dailySalesChart"></canvas>
                </div>
            </div>

            <div class="section-card">
                <div class="control-bar">
                    <div class="btn-label">차종별 이용 비중(월별)</div>
                </div>
                <div class="chart-area">
                    <canvas id="carTypeChart"></canvas>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- 서버 데이터는 실행 가능한 JavaScript 문자열 대신 숨김 DOM의 data-* 속성으로 전달한다. --%>
<div id="statisticsData" hidden>
    <div data-series="hourly-sales">
        <% for (StatisticsDTO dto : hSalesList) { %>
        <span data-label="<%= dto.getLabel() %>" data-value="<%= dto.getValue() %>"></span>
        <% } %>
    </div>
    <div data-series="hourly-counts">
        <% for (StatisticsDTO dto : hCountsList) { %>
        <span data-label="<%= dto.getLabel() %>" data-value="<%= dto.getValue() %>"></span>
        <% } %>
    </div>
    <div data-series="daily-sales">
        <% for (StatisticsDTO dto : dSalesList) { %>
        <span data-label="<%= dto.getLabel() %>" data-value="<%= dto.getValue() %>"></span>
        <% } %>
    </div>
    <div data-series="car-types">
        <% for (StatisticsDTO dto : carTypeStatsList) { %>
        <span data-label="<%= dto.getLabel() %>" data-value="<%= dto.getValue() %>"></span>
        <% } %>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/statistics/statistics.js"></script>
</body>
</html>
