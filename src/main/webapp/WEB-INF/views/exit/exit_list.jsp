<%@ page import="org.example.smart_parking_260219.dto.ParkingSpotDTO" %>
<%@ page import="java.util.List" %>
<%@ page import="org.example.smart_parking_260219.service.ParkingSpotService" %>
<%@ page import="org.example.smart_parking_260219.service.MemberService" %>
<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<ParkingSpotDTO> dtoList = (List<ParkingSpotDTO>) request.getAttribute("dtoList");
    // 전체 입차 차량 수
    MemberService memberService = MemberService.INSTANCE;
%>
<html>
<head>
    <title>Title</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/exit/list.css">
</head>
<body>
<!-- Navigation -->
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
  <!-- 제목 -->
  <div id="entry" class="page">
    <h2> 출차 </h2>
   <!-- 내용 -->
    <div class="form-group">
        <div class="text-right">
<%--            <span class="badge badge-success">전체 <%=no%>건</span>--%>
        </div>
        <div>
            <table>
                <tr>
                    <th>
                        주차 구역
                        <a href="${pageContext.request.contextPath}/list?sort=space&amp;order=asc">
                             ▲
                        </a>
                        <a href="${pageContext.request.contextPath}/list?sort=space&amp;order=desc">
                             ▼
                        </a>
                    </th>
                    <th>차량 번호</th>
                    <th>
                        입차 시간
                        <a href="${pageContext.request.contextPath}/list?sort=time&amp;order=asc">
                            ▲
                        </a>
                        <a href="${pageContext.request.contextPath}/list?sort=time&amp;order=desc">
                            ▼
                        </a>
                    </th>
                    <th>
                        구독 여부
                        <a href="${pageContext.request.contextPath}/list?sort=subscribe&amp;order=asc">
                            ▲
                        </a>
                        <a href="${pageContext.request.contextPath}/list?sort=subscribe&amp;order=desc">
                            ▼
                        </a>
                    </th>
                </tr>
                <%
                    for (ParkingSpotDTO parkingSpotDTO : dtoList) {
                        if (!parkingSpotDTO.getEmpty()) {
                %>
                <tr class="click-row"
                    data-url="${pageContext.request.contextPath}/get?id=<%= Encode.forHtmlAttribute(
                                URLEncoder.encode(parkingSpotDTO.getSpaceId(), StandardCharsets.UTF_8)
                    ) %>&amp;carNum=<%= Encode.forHtmlAttribute(
                                URLEncoder.encode(parkingSpotDTO.getCarNum(), StandardCharsets.UTF_8)
                    ) %>">
                    <td>
                        <%=Encode.forHtmlContent(parkingSpotDTO.getSpaceId())%>
                    </td>
                    <td>
                        <%=Encode.forHtmlContent(parkingSpotDTO.getCarNum())%>
                    </td>
                    <td class="time">
                        <%=parkingSpotDTO.getLastUpdate()%>
                    </td>
                    <td class="subscribe">
                        <%
                            String carNum = parkingSpotDTO.getCarNum();
                            MemberDTO memberDTO;
                            try {
                                memberDTO = memberService.getOneMember(carNum);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        %>
                        <%=
                        (memberDTO != null && memberDTO.isSubscribed()) ? "구독중" : "미구독"
                        %>
                    </td>
                </tr>
                <%
                        }
                    }
                %>
            </table>
        </div>
        <hr>
    </div>
  </div>
</div>
    <script src="${pageContext.request.contextPath}/js/common/function.js"></script>
    <script src="${pageContext.request.contextPath}/js/exit/list.js"></script>
</body>
</html>
