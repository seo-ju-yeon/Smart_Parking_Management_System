<%@ page import="org.example.smart_parking_260219.service.ParkingService" %>
<%@ page import="org.example.smart_parking_260219.dto.ParkingDTO" %>
<%@ page import="org.example.smart_parking_260219.dto.ParkingSpotDTO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.example.smart_parking_260219.service.ParkingSpotService" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // 미정산(paid=false) 주차 기록만 Map으로 구성 (spaceId → ParkingDTO)
    List<ParkingDTO> parkingDTOList = ParkingService.INSTANCE.getAllParking();
    Map<String, ParkingDTO> activeParkingMap = new HashMap<>();
    for (ParkingDTO dto : parkingDTOList) {
        if (!dto.isPaid()) {
            activeParkingMap.put(dto.getSpaceId(), dto);
        }
    }

    // 전체 주차 공간 목록 (empty 상태 기준)
    List<ParkingSpotDTO> allSpots = ParkingSpotService.INSTANCE.getAllParkingSpot();
    Map<String, ParkingSpotDTO> spotMap = new HashMap<>();
    for (ParkingSpotDTO spot : allSpots) {
        spotMap.put(spot.getSpaceId(), spot);
    }
%>
<html>
<head>
    <title>주차 현황</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard/dashboard.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <div id = "dashboard" class="page">
        <h2>주차 현황</h2>
        <div class="parking-grid">
            <%
                // A1 ~ A20까지 전체 주차 공간 출력
                for (int i = 1; i <= 20; i++) {
                    String id = "A" + i;
                    String carNum = null;
                    String entryTime = null;

                    // parking_spot 테이블의 empty 컬럼을 진실의 원천으로 사용
                    ParkingSpotDTO spot = spotMap.get(id);
                    boolean isEmpty = (spot == null) || spot.getEmpty();

                    // empty = false 이고 미정산 주차 기록이 있을 때만 차량 정보 표시
                    if (!isEmpty) {
                        ParkingDTO activeParking = activeParkingMap.get(id);
                        if (activeParking != null) {
                            carNum = activeParking.getCarNum();
                            entryTime = String.valueOf(activeParking.getEntryTime().toLocalTime());
                        } else {
                            // parking_spot은 occupied인데 활성 주차 기록이 없는 데이터 불일치 상태
                            // 빈 자리로 표시하여 UI 오류 방지
                            isEmpty = true;
                        }
                    }
            %>
            <%-- 입차 여부에 따라 주차 공간 출력 --%>
            <div class="slot-item <%= (isEmpty) ? "empty" : "occupied" %>"
                 data-id="<%= id %>"
                 data-empty="<%= (isEmpty) %>"
                 data-carnum="<%= carNum %>">

                <div class="slot-title"><%= id %></div>

                <% if (isEmpty) { %>
                <div class="slot-status">공차</div>
                <% } else { %>
                <div class="slot-status"><%= carNum %></div>
                <div class="slot-time"><%= entryTime %> 입차</div>
                <% } %>
            </div>
            <% } %>
        </div>
        <%-- 주차 차량 조회 --%>
        <div align="center">
            <form id="searchForm">
                <div class="form-group">
                    <input name="keyword" type="text" class="search-input" placeholder="조회할 차량 번호 입력" value=""/>
                    <button type="button" onclick="selectCarNum()">검색</button>
                    <button type="button" onclick="cancelCarNum()">취소</button>
                </div>
            </form>
        </div>
    </div>
</div>
<script>const contextPath = "${pageContext.request.contextPath}";</script>
<script src="${pageContext.request.contextPath}/js/dashboard/dashboard.js"></script>
<script src="${pageContext.request.contextPath}/js/dashboard/parking-list.js"></script>
</body>
</html>