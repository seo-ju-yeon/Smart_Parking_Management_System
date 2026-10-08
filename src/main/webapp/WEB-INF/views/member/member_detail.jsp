<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%
  MemberDTO member = (MemberDTO) request.getAttribute("member");
  if (member == null) {
    response.sendRedirect("/member/member_list");
    return;
  }
  String listPage = (String) request.getAttribute("page");
  if (listPage == null || listPage.isEmpty()) listPage = "1";
  String listUrl = "/member/member_list?page=" + listPage;
  String deleteCarNum = member.getCarNum()
          .replace("&", "&amp;")
          .replace("\"", "&quot;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("'", "&#39;");
%>
<html>
<head>
  <title>회원 상세</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/member/detail.css">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
  <div class="container mt-4 member-detail-container">
    <h2 class="mb-4">월정액 회원 상세</h2>

    <!-- 회원 정보 -->
    <div class="card mb-3">
      <div class="card-header bg-primary text-white font-weight-bold">회원 정보</div>
      <div class="card-body">
        <div class="form-group row">
          <label class="col-4 col-form-label font-weight-bold">차량 번호</label>
          <div class="col-8">
            <input type="text" class="form-control bg-light"
                   value="<%= member.getCarNum() %>" readonly>
          </div>
        </div>
        <div class="form-group row">
          <label class="col-4 col-form-label font-weight-bold">차량 종류</label>
          <div class="col-8">
            <input type="text" class="form-control bg-light"
                   value="<%= member.CarTypeText() %>" readonly>
          </div>
        </div>
        <div class="form-group row">
          <label class="col-4 col-form-label font-weight-bold">회원 이름</label>
          <div class="col-8">
            <input type="text" class="form-control bg-light"
                   value="<%= member.getName() %>" readonly>
          </div>
        </div>
        <div class="form-group row">
          <label class="col-4 col-form-label font-weight-bold">전화번호</label>
          <div class="col-8">
            <input type="text" class="form-control bg-light"
                   value="<%= member.getPhone() %>" readonly>
          </div>
        </div>
        <% if (member.getCreateDate() != null) { %>
        <div class="form-group row mb-0">
          <label class="col-4 col-form-label font-weight-bold">등록일</label>
          <div class="col-8">
            <input type="text" class="form-control bg-light"
                   value="<%= member.getCreateDate() %>" readonly>
          </div>
        </div>
        <% } %>
      </div>
    </div>

    <!-- 월정액 정보 (월정액 누적 확인) -->
    <div class="card mb-3">
      <div class="card-header bg-dark text-white font-weight-bold">결제 및 갱신 이력</div>
      <div class="card-body p-0"> <%-- 패딩을 제거하여 테이블이 꽉 차게 설정 --%>
        <table class="table table-hover mb-0 member-history-table">
          <thead class="thead-light">
          <tr>
            <th>결제일</th>
            <th>이용 기간</th>
            <th>결제 금액</th>
          </tr>
          </thead>
          <tbody>
          <%
            java.util.List<org.example.smart_parking_260219.dto.MemberDTO> history =
                    (java.util.List<org.example.smart_parking_260219.dto.MemberDTO>) request.getAttribute("history");
            if (history != null && !history.isEmpty()) {
              for (org.example.smart_parking_260219.dto.MemberDTO h : history) {
          %>
          <tr>
            <td><%= h.getCreateDate() != null ? h.getCreateDate() : "-" %></td>
            <td>
              <small class="text-muted">
                <%= h.getStartDate() != null ? h.getStartDate() : "-" %> ~ <%= h.getEndDate() != null ? h.getEndDate() : "-" %>
              </small>
            </td>
            <td><%= String.format("%,d", h.getSubscribedFee()) %>원</td>
          </tr>
          <%
            }
          } else {
          %>
          <tr>
            <td colspan="3" class="text-center">결제 이력이 없습니다.</td>
          </tr>
          <% } %>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 버튼 -->
    <div class="d-flex mt-3">
      <a href="/member/member_modify?carNum=<%= member.getCarNum() %>&page=<%= listPage %>"
         class="btn btn-warning flex-fill mr-2 text-white">수정</a>
      <a href="<%= listUrl %>"
         class="btn btn-secondary flex-fill mr-2">목록</a>
      <form id="deleteMemberForm" action="${pageContext.request.contextPath}/member/member_delete"
            method="post" class="flex-fill">
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <input type="hidden" name="carNum" value="<%= deleteCarNum %>">
        <button type="submit" class="btn btn-danger w-100">삭제</button>
      </form>
    </div>

  </div>
</div>

<script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<script src="${pageContext.request.contextPath}/js/member/detail.js"></script>
</body>
</html>
