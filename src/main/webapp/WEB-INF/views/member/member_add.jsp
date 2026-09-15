<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.dto.MemberDTO" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%
    MemberDTO       member         = (MemberDTO)       request.getAttribute("member");
    List<MemberDTO> matchedMembers = (List<MemberDTO>) request.getAttribute("matchedMembers");
    String          step           = (String)          request.getAttribute("step");
    String          searchCarNum   = (String)          request.getAttribute("searchCarNum");
    LocalDate       newStart       = (LocalDate)       request.getAttribute("newStart");
    LocalDate       newEnd         = (LocalDate)       request.getAttribute("newEnd");
    String          error          = request.getParameter("error");

    if (searchCarNum == null) searchCarNum = "";
    if (step == null) step = "search";
%>
<html>
<head>
    <title>회원 등록</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <div class="container mt-4" style="max-width: 500px;">

        <%-- STEP 1: 차량번호 조회 (member_search.jsp 와 동일한 구조) --%>
        <% if ("search".equals(step)) { %>

        <h2 class="mb-4">회원 등록</h2>

        <% if ("fail".equals(error)) { %>
        <div class="alert alert-danger">조회 중 오류가 발생했습니다.</div>
        <% } %>

        <div class="card">
            <div class="card-header bg-primary text-white font-weight-bold">
                차량번호 조회
            </div>
            <div class="card-body">
                <form action="/member/member_add" method="get" onsubmit="return validateCarNum()">
                    <div class="form-group">
                        <label>차량번호 <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" name="carNum" id="carNum"
                               value="<%= searchCarNum %>"
                               placeholder="예: 123가4567">
                        <small class="text-muted">전체 차량번호를 입력해주세요.</small>
                    </div>
                    <div class="d-flex">
                        <button type="submit" class="btn btn-primary flex-fill mr-2">조회</button>
                        <a href="/member/member_list" class="btn btn-secondary flex-fill">목록</a>
                    </div>
                </form>
            </div>
        </div>

        <%-- STEP 2: 신규 회원 등록 폼 (중복 없음 → 바로 이동) --%>
        <% } else if ("register".equals(step)) { %>

        <h2 class="mb-4">신규 회원 등록</h2>

        <div class="alert alert-success">
            <strong>확인 완료!</strong> '<%= searchCarNum %>'은(는) 등록되지 않은 차량입니다.
        </div>

        <div class="card">
            <div class="card-body">
                <form action="/member/member_add" method="post" onsubmit="return validateRegister()">
                    <input type="hidden" name="action" value="register">

                    <div class="form-group">
                        <label>차량 번호</label>
                        <input type="text" class="form-control bg-light" name="carNum"
                               value="<%= searchCarNum %>" readonly>
                    </div>

                    <div class="form-group" hidden="hidden">
                        <label>차량 종류 <span class="text-danger">*</span></label>
                        <select class="form-control" name="carType">
                            <option value="1">일반</option>
                            <option value="2" selected>월정액</option>
                            <option value="3">경차</option>
                            <option value="4">장애인</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>회원 이름 <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" name="name" id="name"
                               placeholder="홍길동" required>
                    </div>

                    <div class="form-group">
                        <label>연락처 <span class="text-danger">*</span></label>
                        <input type="tel" class="form-control" name="phone" id="phone"
                               placeholder="010-1234-5678" required>
                    </div>

                    <div class="card mt-3 mb-3">
                        <div class="card-header bg-info text-white font-weight-bold">월정액 정보</div>
                        <div class="card-body">
                            <div class="form-group">
                                <label>구독 시작일 <span class="text-danger">*</span></label>
                                <input type="date" class="form-control" name="startDate"
                                       id="startDate" required>
                            </div>
                            <div class="form-group">
                                <label>구독 종료일 <span class="text-danger">*</span></label>
                                <input type="date" class="form-control" name="endDate"
                                       id="endDate" required>
                            </div>
                            <div class="form-group mb-0">
                                <label>구독 비용</label>
                                <input type="text" class="form-control bg-light"
                                       value="100,000원" readonly>
                                <small class="text-muted">구독 비용은 고정값입니다.</small>
                            </div>
                        </div>
                    </div>

                    <div class="d-flex">
                        <button type="submit" class="btn btn-primary flex-fill mr-2">등록</button>
                        <a href="/member/member_add" class="btn btn-secondary flex-fill">다시 조회</a>
                    </div>
                </form>
            </div>
        </div>

        <%-- STEP 3: 중복 차량 목록 선택 (member_select.jsp 와 동일한 구조) --%>
        <% } else if ("select".equals(step) && matchedMembers != null) { %>

        <h2 class="mb-4">회원 등록</h2>

        <div class="alert alert-warning">
            '<%= searchCarNum %>'로 검색된 차량이 <%= matchedMembers.size() %>건 있습니다.
            해당 차량을 선택하면 월정액을 갱신합니다.
        </div>

        <div class="list-group mb-3">
            <% for (MemberDTO m : matchedMembers) { %>
            <a href="/member/member_add?step=renew&carNum=<%= m.getCarNum() %>"
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

        <a href="/member/member_add" class="btn btn-secondary btn-block">다시 조회</a>

        <%-- STEP 4: 선택된 기존 회원 갱신 폼 --%>
        <% } else if ("renew".equals(step) && member != null) { %>

        <h2 class="mb-4">월정액 갱신</h2>

        <div class="card mb-3">
            <div class="card-header bg-secondary text-white font-weight-bold">회원 정보</div>
            <div class="card-body">
                <div class="form-group row mb-2">
                    <label class="col-4 col-form-label font-weight-bold">차량번호</label>
                    <div class="col-8">
                        <input type="text" class="form-control bg-light"
                               value="<%= member.getCarNum() %>" readonly>
                    </div>
                </div>
                <div class="form-group row mb-2">
                    <label class="col-4 col-form-label font-weight-bold">이름</label>
                    <div class="col-8">
                        <input type="text" class="form-control bg-light"
                               value="<%= member.getName() %>" readonly>
                    </div>
                </div>
                <div class="form-group row mb-0">
                    <label class="col-4 col-form-label font-weight-bold">구독 상태</label>
                    <div class="col-8 d-flex align-items-center">
                        <% if (member.isSubscribed()) { %>
                        <span class="badge badge-success p-2">구독중 (~<%= member.getEndDate() %>)</span>
                        <% } else { %>
                        <span class="badge badge-secondary p-2">만료</span>
                        <% } %>
                    </div>
                </div>
            </div>
        </div>

        <div class="card mb-3">
            <div class="card-header bg-info text-white font-weight-bold">월정액 1개월 갱신</div>
            <div class="card-body">
                <div class="alert alert-light border mb-3">
                    <div class="row">
                        <div class="col-5 text-center">
                            <small class="text-muted">갱신 시작일</small>
                            <div class="font-weight-bold text-primary"><%= newStart %></div>
                        </div>
                        <div class="col-2 text-center align-self-center text-muted">→</div>
                        <div class="col-5 text-center">
                            <small class="text-muted">갱신 종료일</small>
                            <div class="font-weight-bold text-danger"><%= newEnd %></div>
                        </div>
                    </div>
                </div>
                <div class="form-group row mb-2">
                    <label class="col-4 col-form-label font-weight-bold">구독 비용</label>
                    <div class="col-8">
                        <input type="text" class="form-control bg-light" value="100,000원" readonly>
                    </div>
                </div>
                <small class="text-muted">
                    <% if (member.isSubscribed()) { %>
                    ※ 현재 구독 종료일(<%= member.getEndDate() %>) 다음날부터 1개월 연장됩니다.
                    <% } else { %>
                    ※ 오늘 다음날(<%= newStart %>)부터 1개월 등록됩니다.
                    <% } %>
                </small>
            </div>
        </div>

        <form action="${pageContext.request.contextPath}/member/member_add" method="post">
            <input type="hidden" name="action" value="renew">
            <input type="hidden" name="carNum" value="<%= member.getCarNum() %>">
            <div class="d-flex">
                <button type="submit" class="btn btn-warning flex-fill mr-2 text-white"
                        onclick="return confirm('월정액 1개월을 갱신하시겠습니까?')">
                    갱신
                </button>
                <a href="${pageContext.request.contextPath}/member/member_add" class="btn btn-secondary flex-fill">다시 조회</a>
            </div>
        </form>

        <% } %>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<script src="${pageContext.request.contextPath}/js/common/menu.js"></script>
<script src="${pageContext.request.contextPath}/js/member/add.js"></script>
</body>
</html>
