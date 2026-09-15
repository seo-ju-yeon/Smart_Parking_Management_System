<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="org.example.smart_parking_260219.dto.FeePolicyDTO" %>
<%@ page import="org.example.smart_parking_260219.dto.PageResponseDto" %>
<%
    PageResponseDto pageResponseDto = (PageResponseDto) request.getAttribute("pageResponseDto");

    // 2. 에러 방지를 위한 null 체크 및 데이터 할당
    if (pageResponseDto == null) {
        out.print("<script>alert('데이터가 없습니다.'); history.back();</script>");
        return;
    }

    List<FeePolicyDTO> boardList = pageResponseDto.getBoardList();
    int totalCount = pageResponseDto.getTotalCount(); // 전체 게시글 개수
    int pageNum = pageResponseDto.getPageNum(); // 현재 페이지 번호
    int totalPage = pageResponseDto.getTotalPage(); // 전체 페이지 수
    String items = (request.getParameter("items") != null) ? request.getParameter("items") : "";
    String keyword = (request.getParameter("keyword") != null) ? request.getParameter("keyword") : "";
%>

<html>
<head>
    <title>요금 정책 변경 이력</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/style.css">
    <style>
        .main-content {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: flex-start;
            min-height: 100vh;
            padding: 40px 20px;
            background-color: #f8f9fc;
        }

        .list-card {
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.08);
            padding: 30px;
            margin-top: 20px;

            width: 100%;
            max-width: 1100px;
            margin-left: auto;
            margin-right: auto;
        }
        .list-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 25px;
            border-bottom: 2px solid #f8f9fa;
            padding-bottom: 15px;
        }
        .list-header h2 {
            font-weight: 700;
            color: #333;
            margin: 0;
        }
        /* 테이블 스타일 개선 */
        .table {
            border-collapse: separate;
            border-spacing: 0 10px;
        }
        .table thead th {
            border: none;
            color: white;
            font-weight: 600;
            text-transform: uppercase;
            font-size: 0.85rem;
            text-align: center;
        }
        .table tbody tr {
            cursor: pointer; /* 마우스 오버 시 손가락 모양 */
            background-color: #fff;
            box-shadow: 0 2px 5px rgba(0,0,0,0.02);
            transition: transform 0.2s;
        }
        .table tbody tr:hover {
            transform: translateY(-2px);
            background-color: #f8faff;
        }
        .table td {
            vertical-align: middle;
            text-align: center;
            padding: 15px;
            border-top: 1px solid #f1f1f1;
            border-bottom: 1px solid #f1f1f1;
        }
        .table td:first-child { border-left: 1px solid #f1f1f1; border-top-left-radius: 8px; border-bottom-left-radius: 8px; }
        .table td:last-child { border-right: 1px solid #f1f1f1; border-top-right-radius: 8px; border-bottom-right-radius: 8px; }

        .table-responsive {
            width: 100%;
            margin: 0 auto;
        }

        .active-row { background-color: #f0f8ff !important; border: 1px solid #4e73df !important; }
        .policy-link { font-weight: 700; color: #4e73df; text-decoration: none; }
        .policy-link:hover { text-decoration: underline; }

        /* 페이지네이션 스타일 */
        .pagination-container .btn {
            border-radius: 5px;
            margin: 0 2px;
            font-weight: 600;
        }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">

    <div class="list-card">
        <div class="list-header">
            <div>
                <h2><i class="fas fa-history mr-2" style="color: #4e73df;"></i>요금 정책 변경 이력</h2>
                <small class="text-muted">전체 <b class="text-primary"><%=pageResponseDto.getTotalCount()%></b>개의 정책 데이터가 있습니다.</small>
            </div>
            <a href="${pageContext.request.contextPath}/view/policy/add" class="btn btn-primary shadow-sm">
                <i class="fas fa-plus mr-1"></i> 신규 정책 등록
            </a>
        </div>

        <div class="table-responsive">
            <table class="table">
                <thead>
                    <tr>
                        <th>기본 시간/요금</th>
                        <th>추가 시간/요금</th>
                        <th>유예시간</th>
                        <th>경차 할인</th>
                        <th>장애인 할인</th>
                        <th>일일최대</th>
                        <th>등록/수정일</th>
                        <th>상태</th>
                    </tr>
                </thead>
                <tbody>
                <%
                    if (boardList != null && !boardList.isEmpty()) {
                        for (FeePolicyDTO dto : boardList) {
                            String activeClass = dto.isActive() ? "active-row" : "";
                            String detailUrl = request.getContextPath() + "/view/policy?id=" + dto.getPolicyId() + "&pageNum=" + pageNum;
                %>
                <tr class="<%= activeClass %>" onclick="location.href='<%= detailUrl %>'">
                    <td>
                        <i class="far fa-clock mr-1"></i>
                        <%= dto.getDefaultTime() %>분 /
                        <%= (dto.getDefaultFee() != 0) ? String.format("%,d", dto.getDefaultFee()) : "0" %>원
                    </td>
                    <td>
                        <%= dto.getExtraTime() %>분 /
                        <%= (dto.getExtraFee() != 0) ? String.format("%,d", dto.getExtraFee()) : "0" %>원
                    </td>
                    <td><span class="badge badge-light"><%= dto.getGracePeriod() %>분</span></td>

                    <%-- 할인율은 double 연산 시 null 체크 주의 --%>
                    <td><b class="text-info"><%= (int)(dto.getLightDiscount() * 100) %>%</b></td>
                    <td><b class="text-info"><%= (int)(dto.getDisabledDiscount() * 100) %>%</b></td>

                    <td><b class="text-danger"><%= String.format("%,d", dto.getMaxDailyFee()) %>원</b></td>
                    <td class="text-muted" style="font-size: 0.85rem;">
                        <%-- 안전한 날짜 출력 --%>
                        <%= (dto.getModifyDate() != null) ? dto.getModifyDate().toString().replace("T", " ") : "-" %>
                    </td>
                    <td>
                        <% if (dto.isActive()) { %>
                        <span class="badge badge-primary"><i class="fas fa-check mr-1"></i>적용중</span>
                        <% } else { %>
                        <span class="badge badge-secondary">미적용</span>
                        <% } %>
                    </td>
                </tr>
                <%
                    }
                } else {
                %>
                <tr><td colspan="8" class="text-center">데이터가 없습니다.</td></tr>
                <%
                    }
                %>
                </tbody>
            </table>
        </div>

        <div class="pagination-container w-100 text-center" style="margin-top: 30px;">
            <%
                int pagePerBlock = 5;
                int thisBlock = (pageNum - 1) / pagePerBlock + 1;
                int firstPage = (thisBlock - 1) * pagePerBlock + 1;
                int lastPage = Math.min(thisBlock * pagePerBlock, totalPage);
            %>

            <% if (firstPage != 1) {%>
            <a href="${pageContext.request.contextPath}/view/policy/list?pageNum=<%= (firstPage - 1) %>" class="btn btn-sm btn-outline-secondary">이전</a>
            <% } %>

            <%
                for (int i = firstPage; i <= lastPage; i++) {
                    String activeBtn = (pageNum == i) ? "btn-primary" : "btn-outline-secondary";
            %>
            <a href="${pageContext.request.contextPath}/view/policy/list?pageNum=<%=i%>&items=<%=items%>&keyword=<%=keyword%>"
               class="btn btn-sm <%= activeBtn %>">
                <%= i %>
            </a>
            <% } %>

            <% if (lastPage < totalPage) { %>
            <a href="${pageContext.request.contextPath}/view/policy/list?pageNum=<%=(lastPage + 1)%>&items=<%=items%>&keyword=<%=keyword%>"
               class="btn btn-sm btn-outline-secondary">다음</a>
            <% } %>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/JS/menu.js"></script>
<script src="${pageContext.request.contextPath}/JS/function.js"></script>
</body>
</html>