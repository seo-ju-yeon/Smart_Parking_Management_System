<%@ page import="java.util.List" %>
<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  List<MemberDTO> matchedMembers = (List<MemberDTO>) request.getAttribute("matchedMembers");
  String searchCarNum = (String) request.getAttribute("searchCarNum");

  if (matchedMembers == null || matchedMembers.isEmpty()) {
    response.sendRedirect("/member/member_search");
    return;
  }
%>
<html>
<%-- 회원 조회시 차량번호 뒷 4자리 동일시 선택 화면--%>
<head>
  <title>회원 선택</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
  <div class="container mt-4" style="max-width: 600px;">
    <h2 class="mb-4">회원 선택</h2>

    <div class="alert alert-info">
      '<%= searchCarNum %>'로 검색된 회원이 <%= matchedMembers.size() %>건 있습니다. 선택해주세요.
    </div>

    <div class="list-group">
      <% for (MemberDTO m : matchedMembers) { %>
      <a href="/member/member_detail?carNum=<%= m.getCarNum() %>"
         class="list-group-item list-group-item-action">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <h6 class="mb-1 font-weight-bold"><%= m.getCarNum() %></h6>
            <small class="text-muted"><%= m.getName() %> · <%= m.getPhone() %></small>
          </div>
          <div class="text-right">
            <span class="badge badge-primary"><%= m.CarTypeText() %></span>
            <% if (m.isSubscribed()) { %>
            <span class="badge badge-success">구독중</span>
            <% } else { %>
            <span class="badge badge-secondary">만료</span>
            <% } %>
          </div>
        </div>
      </a>
      <% } %>
    </div>

    <div class="mt-3">
      <a href="/member/member_search" class="btn btn-secondary btn-block">다시 검색</a>
    </div>
  </div>
</div>

<script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
</body>
</html>
