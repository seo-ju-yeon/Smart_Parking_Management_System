<%@ page import="org.example.smart_parking_260219.dto.StatisticsDTO" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // 1. 숫자 포맷터 설정
    java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");

    // 2. 데이터 가져오기 (Null 방어 포함)
    List<StatisticsDTO> hSalesList = (List<StatisticsDTO>) request.getAttribute("hourlySales");
    List<StatisticsDTO> dSalesList = (List<StatisticsDTO>) request.getAttribute("dailySales");
    int monthSubscribedFee = (int) request.getAttribute("monthSubscribedFee");

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
                    <input type="date" name="targetDate" value="${targetDate}" onchange="changeStatisticsDate(this)">
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

<script>
    // 1. 서버에서 넘어온 데이터를 JS 배열로 변환
    <%
        List<StatisticsDTO> hSales = (List<StatisticsDTO>) request.getAttribute("hourlySales");
        List<StatisticsDTO> hCounts = (List<StatisticsDTO>) request.getAttribute("hourlyCounts");
        List<StatisticsDTO> dSales = (List<StatisticsDTO>) request.getAttribute("dailySales");
        List<StatisticsDTO> cStats = (List<StatisticsDTO>) request.getAttribute("carTypeStats");
    %>

    // 시간대별 데이터 - 0시~23시까지의 매출 및 입차 대수
    const hourlyLabels = [
        <% for(StatisticsDTO dto : hSales) { %> "<%= dto.getLabel() %>", <% } %>
    ];
    const hourlySalesData = [
        <% for(StatisticsDTO dto : hSales) { %> <%= dto.getValue() %>, <% } %>
    ];
    const hourlyCountData = [
        <% for(StatisticsDTO dto : hCounts) { %> <%= dto.getValue() %>, <% } %>
    ];

    // 일별 매출 데이터 - 해당 월의 1일~말일까지의 일간 합계
    const dailyLabels = [
        <% for(StatisticsDTO dto : dSales) { %>
        "<%= dto.getLabel().substring(8) %>일", // "2026-02-01" 형태에서 일자만 추출
        <% } %>
    ];
    const dailyData = [
        <% for(StatisticsDTO dto : dSales) { %> <%= dto.getValue() %>, <% } %>
    ];

    // 차종별 데이터 - 일반, 경차, 장애인 등 차종별 카운트
    const carLabels = [
        <% for(StatisticsDTO dto : cStats) { %> "<%= dto.getLabel() %>", <% } %>
    ];
    const carData = [
        <% for(StatisticsDTO dto : cStats) { %> <%= dto.getValue() %>, <% } %>
    ];
</script>

<script src="${pageContext.request.contextPath}/js/statistics/statistics.js"></script>
</body>
</html>
