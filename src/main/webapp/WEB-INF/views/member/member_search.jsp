<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  String error        = request.getParameter("error");
  String searchCarNum = (String) request.getAttribute("searchCarNum");
  if (searchCarNum == null) searchCarNum = "";
%>
<html>
<head>
  <title>회원 검색</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
  <div class="container mt-4" style="max-width: 500px;">
    <h2 class="mb-4">회원 검색</h2>

    <% if ("notFound".equals(error)) { %>
    <div class="alert alert-warning">
      '<%= searchCarNum %>'로 검색된 회원이 없습니다.
    </div>
    <% } else if ("fail".equals(error)) { %>
    <div class="alert alert-danger">검색 중 오류가 발생했습니다.</div>
    <% } %>

    <div class="card">
      <div class="card-header bg-primary text-white font-weight-bold">
        차량번호 뒤 4자리 검색
      </div>
      <div class="card-body">
        <form action="/member/member_search" method="get"
              onsubmit="return validateSearch()">
          <div class="form-group">
            <label>차량번호 뒤 4자리 <span class="text-danger">*</span></label>
            <input type="text" class="form-control" name="carNum" id="carNum"
                   value="<%= searchCarNum %>"
                   placeholder="예: 4567" maxlength="4">
            <small class="text-muted">숫자 4자리를 입력해주세요.</small>
          </div>
          <div class="d-flex">
            <button type="submit" class="btn btn-primary flex-fill mr-2">검색</button>
            <a href="/member/member_list" class="btn btn-secondary flex-fill">목록</a>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<script src="${pageContext.request.contextPath}/js/common/menu.js"></script>
<script src="../JS/member/search.js"></script>
</body>
</html>
