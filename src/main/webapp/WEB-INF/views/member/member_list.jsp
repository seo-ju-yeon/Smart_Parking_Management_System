<%@ page import="java.util.List" %>
<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="org.example.smart_parking_260219.service.MemberService" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    // 구독중인 회원과 만료 회원 가져오기
    MemberService memberService = MemberService.INSTANCE;
    List<MemberDTO> allMembers = memberService.getAllMember();
    long totalSubscribed = allMembers.stream().filter(MemberDTO::isSubscribed).count();
    long totalExpired = allMembers.size() - totalSubscribed;

    // DB의 회원DTO 가져오기
    List<MemberDTO> dtoList = (List<MemberDTO>) request.getAttribute("dtoList");
    Integer currentPage = (Integer) request.getAttribute("currentPage");
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    Integer totalItems = (Integer) request.getAttribute("totalItems");
    Integer startNo = (Integer) request.getAttribute("startNo");
    String sortColumn = (String) request.getAttribute("sortColumn");
    String sortOrder = (String) request.getAttribute("sortOrder");

    if (dtoList == null) dtoList = new java.util.ArrayList<>();
    if (currentPage == null) currentPage = 1;
    if (totalPages == null) totalPages = 1;
    if (totalItems == null) totalItems = 0;
    if (startNo == null) startNo = 0;
    if (sortColumn == null) sortColumn = "memberId";
    if (sortOrder == null) sortOrder = "desc";

    int displayNo = startNo;
    String success = request.getParameter("success");
    String error = request.getParameter("error");
%>
<%!
    // 회원 col 클릭시 정렬
    String getSortUrl(String col, String curCol, String curOrder, int page) {
        String newOrder = col.equals(curCol) && "asc".equals(curOrder) ? "desc" : "asc";
        return "?page=" + page + "&sort=" + col + "&order=" + newOrder;
    }
    String getArrow(String col, String curCol, String curOrder) {
        if (!col.equals(curCol)) return "";
        return "asc".equals(curOrder) ? " ▲" : " ▼";
    }
%>
<html>
<head>
    <title>월정액 회원 목록</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/member/list.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <div id="memberList" class="page">

        <!-- 알림 메시지 -->
        <% if ("renew".equals(success)) { %>
        <div class="alert alert-success alert-dismissible fade show">
            <strong>완료!</strong> 월정액이 1개월 갱신되었습니다.
            <button type="button" class="close" data-dismiss="alert"><span>&times;</span></button>
        </div>
        <% } else if ("modify".equals(success)) { %>
        <div class="alert alert-success alert-dismissible fade show">
            <strong>완료!</strong> 회원 정보가 수정되었습니다.
            <button type="button" class="close" data-dismiss="alert"><span>&times;</span></button>
        </div>
        <% } else if ("delete".equals(success)) { %>
        <div class="alert alert-success alert-dismissible fade show">
            <strong>완료!</strong> 회원이 삭제되었습니다.
            <button type="button" class="close" data-dismiss="alert"><span>&times;</span></button>
        </div>
        <% } else if ("renewFail".equals(error)) { %>
        <div class="alert alert-danger alert-dismissible fade show">
            <strong>오류!</strong> 갱신에 실패했습니다.
            <button type="button" class="close" data-dismiss="alert"><span>&times;</span></button>
        </div>
        <% } else if ("modifyFail".equals(error)) { %>
        <div class="alert alert-danger alert-dismissible fade show">
            <strong>오류!</strong> 수정에 실패했습니다.
            <button type="button" class="close" data-dismiss="alert"><span>&times;</span></button>
        </div>
        <% } else if ("deleteFail".equals(error)) { %>
        <div class="alert alert-danger alert-dismissible fade show">
            <strong>오류!</strong> 삭제에 실패했습니다.
            <button type="button" class="close" data-dismiss="alert"><span>&times;</span></button>
        </div>
        <% } %>

        <div class="d-flex justify-content-between align-items-center">
            <h2 class="mb-0">월정액 회원 목록</h2>
            <a href="/member/member_add" class="btn btn-primary">회원 등록</a>
        </div>
        <hr class="mt-2 mb-2">
        <div class="text-right mb-3">
            <span class="badge badge-custom badge-warning text-white">
                <i class="fas fa-users"></i>전체 <%=totalItems%>명</span>
            <span class="badge badge-custom badge-success">
                <i class="fas fa-check-circle"></i>구독중 <%=totalSubscribed%>명</span>
            <span class="badge badge-custom badge-secondary">
                <i class="fas fa-exclamation-circle"></i>만료 <%=totalExpired%>명</span>
        </div>

        <table class="table table-hover">
            <thead>
            <tr>
                <%-- 메뉴 선택시 정렬 (번호는 내림차순 우선으로 변경 안됨)--%>
                <th><a href="<%= getSortUrl("memberId", sortColumn, sortOrder, currentPage) %>">
                    번호<span class="sort-icon"><%= getArrow("memberId", sortColumn, sortOrder) %></span></a></th>
                <th><a href="<%= getSortUrl("carNum", sortColumn, sortOrder, currentPage) %>">
                    차량 번호<span class="sort-icon"><%= getArrow("carNum", sortColumn, sortOrder) %></span></a></th>
                <th><a href="<%= getSortUrl("carType", sortColumn, sortOrder, currentPage) %>">
                    차량 종류<span class="sort-icon"><%= getArrow("carType", sortColumn, sortOrder) %></span></a></th>
                <th><a href="<%= getSortUrl("name", sortColumn, sortOrder, currentPage) %>">
                    회원 이름<span class="sort-icon"><%= getArrow("name", sortColumn, sortOrder) %></span></a></th>
                <th><a href="<%= getSortUrl("phone", sortColumn, sortOrder, currentPage) %>">
                    전화번호<span class="sort-icon"><%= getArrow("phone", sortColumn, sortOrder) %></span></a></th>
                <th><a href="<%= getSortUrl("createDate", sortColumn, sortOrder, currentPage) %>">
                    구독 가입일<span class="sort-icon"><%= getArrow("createDate", sortColumn, sortOrder) %></span></a></th>
                <th><a href="<%= getSortUrl("endDate", sortColumn, sortOrder, currentPage) %>">
                    구독 만기일<span class="sort-icon"><%= getArrow("endDate", sortColumn, sortOrder) %></span></a></th>
                <th><a href="<%= getSortUrl("status", sortColumn, sortOrder, currentPage) %>">
                    상태<span class="sort-icon"><%= getArrow("status", sortColumn, sortOrder) %></span></a></th>
                <th>관리</th>
            </tr>
            </thead>
            <tbody>
            <% if (dtoList.isEmpty()) { %>
            <tr><td colspan="9" class="text-center">등록된 회원이 없습니다.</td></tr>
            <% } else {
                for (MemberDTO m : dtoList) {
                    boolean isExpired = !m.isSubscribed();
            %>
            <tr class="<%= isExpired ? "table-secondary" : "" %>" style="cursor: pointer;"
                onclick="location.href='/member/member_detail?carNum=<%= m.getCarNum() %>&page=<%= currentPage %>'">
                <td><%= displayNo-- %></td>
                <td><%= m.getCarNum() %></td>
                <td><%= m.CarTypeText() %></td>
                <td><%= m.getName() %></td>
                <td><%= m.getPhone() %></td>
                <td><%= m.getCreateDate() != null ? m.getCreateDate() : "-" %></td>
                <td><%= m.getEndDate() != null ? m.getEndDate() : "-" %></td>
                <td>
                    <% if (isExpired) { %>
                    <span class="badge badge-secondary">만료</span>
                    <% } else { %>
                    <span class="badge badge-success">구독중</span>
                    <% } %>
                </td>
                <!-- 갱신 버튼: tr 클릭과 분리 -->
                <td onclick="event.stopPropagation()" style="text-align:center; vertical-align:middle;">
                    <% if (isExpired) { %>
                    <form action="/member/member_list" method="post"
                          style="display:inline; margin:0;">
                        <input type="hidden" name="action" value="renew">
                        <input type="hidden" name="carNum" value="<%= m.getCarNum() %>">
                        <button type="submit"
                                onclick="event.stopPropagation(); return confirm('<%= m.getCarNum() %> 1개월 갱신하시겠습니까?')"
                                style="background:#e67e22; color:#fff; border:none; border-radius:4px;
                                           padding:4px 12px; font-size:13px; cursor:pointer; width:60px;">갱신</button>
                    </form>
                    <% } else { %>
                    <button disabled style="background:#2c3e50; color:#fff; border:none; border-radius:4px;
                                       padding:4px 12px; font-size:13px; cursor:not-allowed; width:60px; opacity:0.7;">갱신</button>
                    <% } %>
                </td>
            </tr>
            <% }
            } %>
            </tbody>
        </table>

        <!-- 페이징 -->
        <div class="pagination">
            <% if (currentPage > 1) { %>
            <a href="?page=<%= currentPage - 1 %>&sort=<%= sortColumn %>&order=<%= sortOrder %>">
                <button>◀ 이전</button></a>
            <% } else { %>
            <button disabled>◀ 이전</button>
            <% } %>
            <span id="pageInfo"><%= currentPage %> / <%= totalPages %></span>
            <% if (currentPage < totalPages) { %>
            <a href="?page=<%= currentPage + 1 %>&sort=<%= sortColumn %>&order=<%= sortOrder %>">
                <button>다음 ▶</button></a>
            <% } else { %>
            <button disabled>다음 ▶</button>
            <% } %>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<script src="${pageContext.request.contextPath}/js/common/function.js"></script>
</body>
</html>
