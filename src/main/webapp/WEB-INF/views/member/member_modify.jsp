<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%@ page import="java.time.LocalDate" %>
<%
    MemberDTO member = (MemberDTO) request.getAttribute("member");
    LocalDate newStart = (LocalDate) request.getAttribute("newStart");
    LocalDate newEnd = (LocalDate) request.getAttribute("newEnd");

    if (member == null) {
        response.sendRedirect("/member/member_list");
        return;
    }
    String modifyError = request.getParameter("error");
    String listPage = (String) request.getAttribute("page");
    if (listPage == null || listPage.isEmpty()) listPage = "1";
    String listUrl = "/member/member_list?page=" + listPage;

    // JSP 스크립틀릿에서 미리 계산 (value 속성 내 따옴표 충돌 방지)
    String startDateVal = (member.getStartDate() != null) ? member.getStartDate().toString() : "";
    String endDateVal   = (member.getEndDate()   != null) ? member.getEndDate().toString()   : "";
    String startDateDisplay = (member.getStartDate() != null) ? member.getStartDate().toString() : "-";
    String endDateDisplay   = (member.getEndDate()   != null) ? member.getEndDate().toString()   : "-";
    String feeDisplay = String.format("%,d", member.getSubscribedFee()) + "원";
    String renewConfirmMsg = member.getCarNum() + " 1개월 갱신하시겠습니까?\\n("
            + newStart + " ~ " + newEnd + ")";
%>
<html>
<head>
    <title>회원 수정</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/member/modify.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <div class="container mt-4 member-modify-container">
        <h2 class="mb-4">회원 수정</h2>

        <% if ("modifyFail".equals(modifyError)) { %>
        <div class="alert alert-danger">수정 중 오류가 발생했습니다.</div>
        <% } %>

        <!-- 회원 정보 수정 폼 -->
        <form id="memberModifyForm" action="/member/member_modify" method="post">
            <input type="hidden" name="action"    value="modify">
            <input type="hidden" name="memberId"  value="<%= member.getMemberId() %>">
            <input type="hidden" name="carNum"    value="<%= member.getCarNum() %>">
            <input type="hidden" name="page"      value="<%= listPage %>">
            <input type="hidden" name="startDate" value="<%= startDateVal %>">
            <input type="hidden" name="endDate"   value="<%= endDateVal %>">

            <div class="card mb-3">
                <div class="card-header bg-primary text-white font-weight-bold">회원 정보 수정</div>
                <div class="card-body">
                    <div class="form-group">
                        <label>차량 번호</label>
                        <input type="text" class="form-control bg-light"
                               value="<%= member.getCarNum() %>" readonly>
                        <small class="text-muted">차량번호는 수정할 수 없습니다.</small>
                    </div>
                    <div class="form-group">
                        <label>회원 이름 <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="name" name="name"
                               value="<%= member.getName() %>" required>
                    </div>
                    <div class="form-group mb-0">
                        <label>전화번호 <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="phone" name="phone"
                               value="<%= member.getPhone() %>" required>
                    </div>
                </div>
            </div>

            <div class="d-flex mb-4">
                <button type="submit" class="btn btn-primary flex-fill mr-2">수정 완료</button>
                <a href="<%= listUrl %>" class="btn btn-secondary flex-fill">취소</a>
            </div>
        </form>

        <!-- 월정액 현황 + 1개월 갱신 -->
        <div class="card mb-3">
            <div class="card-header bg-info text-white font-weight-bold">월정액 현황</div>
            <div class="card-body">
                <div class="form-group row mb-2">
                    <label class="col-4 col-form-label font-weight-bold">구독 상태</label>
                    <div class="col-8 d-flex align-items-center">
                        <% if (member.isSubscribed()) { %>
                        <span class="badge badge-success p-2">구독중</span>
                        <% } else { %>
                        <span class="badge badge-secondary p-2">만료</span>
                        <% } %>
                    </div>
                </div>
                <div class="form-group row mb-2">
                    <label class="col-4 col-form-label font-weight-bold">시작일</label>
                    <div class="col-8">
                        <input type="text" class="form-control bg-light"
                               value="<%= startDateDisplay %>" readonly>
                    </div>
                </div>
                <div class="form-group row mb-2">
                    <label class="col-4 col-form-label font-weight-bold">종료일</label>
                    <div class="col-8">
                        <input type="text" class="form-control bg-light"
                               value="<%= endDateDisplay %>" readonly>
                    </div>
                </div>
                <div class="form-group row mb-3">
                    <label class="col-4 col-form-label font-weight-bold">구독 비용</label>
                    <div class="col-8">
                        <input type="text" class="form-control bg-light"
                               value="<%= feeDisplay %>" readonly>
                    </div>
                </div>

                <!-- 갱신 예상 날짜 안내 -->
                <div class="alert alert-light border mb-3">
                    <small class="text-muted d-block mb-2">갱신 후 예상 구독 기간</small>
                    <div class="d-flex justify-content-around">
                        <div class="text-center">
                            <small class="text-muted">시작일</small>
                            <div class="font-weight-bold text-primary"><%= newStart %></div>
                        </div>
                        <div class="align-self-center text-muted">→</div>
                        <div class="text-center">
                            <small class="text-muted">종료일</small>
                            <div class="font-weight-bold text-danger"><%= newEnd %></div>
                        </div>
                    </div>
                </div>

                <!-- 1개월 갱신 폼 -->
                <form action="/member/member_modify" method="post">
                    <input type="hidden" name="action" value="renew">
                    <input type="hidden" name="carNum" value="<%= member.getCarNum() %>">
                    <input type="hidden" name="page"   value="<%= listPage %>">
                    <button type="submit" id="renewSubscriptionButton"
                            class="btn btn-warning btn-block text-white"
                            data-confirm-message="<%= renewConfirmMsg %>">
                        1개월 갱신
                    </button>
                </form>
            </div>
        </div>

    </div>
</div>

<script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<script src="${pageContext.request.contextPath}/js/member/modify.js"></script>
</body>
</html>
