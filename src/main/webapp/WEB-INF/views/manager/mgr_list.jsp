<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="org.example.smart_parking_260219.dto.ManagerDTO" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerVO" %>

<html>
<head>
    <title>관리자 목록 - 스마트 파킹 시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/list.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>

<%-- 최고관리자 접근 차단 안내 모달 --%>
<div id="adminModal" class="modal-backdrop">
    <div class="modal-box">
        <div class="modal-icon">🔒</div>
        <div class="modal-title">접근 제한</div>
        <div class="modal-desc">
            최고 관리자 계정은 이 화면에서 수정할 수 없습니다.<br>
            좌측 메뉴의 <strong>최고 관리자 정보 수정</strong> 탭을 이용해 주세요.
        </div>
        <button type="button" id="closeAdminModalButton" class="modal-btn">확인</button>
    </div>
</div>

<%-- 슈퍼관리자 접근 차단 안내 모달 --%>
<div id="superModal" class="modal-backdrop">
    <div class="modal-box">
        <div class="modal-icon">🛡️</div>
        <div class="modal-title">수정 불가 계정</div>
        <div class="modal-desc">
            해당 계정은 <strong>시스템의 모든 기능을<br> 조회하기 위해 생성된 전용 계정</strong>입니다.<br><br>
            보안 정책상 이 계정은 수정할 수 없습니다.
        </div>
        <button type="button" id="closeSuperModalButton" class="modal-btn">확인</button>
    </div>
</div>

    <%
    /* 현재 로그인한 관리자 권한 확인 */
    ManagerVO sessionMgr = (ManagerVO) session.getAttribute("loginManager");
    String loginRole = (sessionMgr != null) ? sessionMgr.getRole() : "";
%>

<div class="main-content">
    <div id="entry" class="page">
        <div class="list-title-wrapper">
            <h2 class="list-title">관리자 계정 목록</h2>

        </div>

        <div class="form-group">
            <table class="manager-table">
                <thead>
                <tr>
                    <th>번호</th>
                    <th>아이디</th>
                    <th>이름</th>
                    <th>이메일</th>
                    <th>상태</th>
                </tr>
                </thead>
                <tbody>
                <%
                    // 컨트롤러에서 전달한 관리자 목록 가져옴
                    List<ManagerDTO> managerList = (List<ManagerDTO>) request.getAttribute("managerList");

                    // 페이징 기본값 설정
                    int pageSize = 5;
                    int currentPage = 1;

                    // 요청 파라미터에서 현재 페이지 확인
                    String pageParam = request.getParameter("page");
                    if (pageParam != null) {
                        try {
                            currentPage = Integer.parseInt(pageParam);
                        } catch (NumberFormatException e) {
                            currentPage = 1;
                        }
                    }

                    int totalCount = 0;
                    int totalPages = 0;
                    int startIndex = 0;
                    int endIndex = 0;

                    if (managerList != null && !managerList.isEmpty()) {
                        totalCount = managerList.size();
                        totalPages = (int) Math.ceil((double) totalCount / pageSize);

                        // 현재 페이지 범위 보정
                        if (currentPage < 1) currentPage = 1;
                        if (currentPage > totalPages) currentPage = totalPages;

                        // 현재 페이지에 표시할 목록 범위 계산
                        startIndex = (currentPage - 1) * pageSize;
                        endIndex = Math.min(startIndex + pageSize, totalCount);

                        // 현재 페이지 데이터만 출력
                        for (int i = startIndex; i < endIndex; i++) {
                            ManagerDTO mgr = managerList.get(i);
                %>
                <tr class="<%= ("ADMIN".equals(mgr.getRole()) || "SUPER".equals(mgr.getRole())) ? "row-admin" : "" %>">
                    <td><%= mgr.getManagerNo() %>
                    </td>
                    <td><%= mgr.getManagerId() %>
                    </td>
                    <td>
                        <% if ("ADMIN".equals(mgr.getRole())) { %>
                        <%-- 최고관리자는 상세/수정 진입 대신 안내 모달 표시 --%>
                        <a href="#" class="role-link open-admin-modal">
                            <%= mgr.getManagerName() %>
                        </a>
                        <span class="badge-admin">최고관리자</span>
                        <% } else if ("SUPER".equals(mgr.getRole())) { %>
                        <%-- 슈퍼관리자는 전용 안내 모달 표시 --%>
                        <a href="#" class="role-link open-super-modal">
                            <%= mgr.getManagerName() %>
                        </a>
                        <span class="badge-admin">슈퍼관리자</span>
                        <% } else { %>
                        <%-- 일반관리자는 상세 조회 화면으로 이동 --%>
                        <a href="${pageContext.request.contextPath}/mgr/view?id=<%= mgr.getManagerId() %>"
                           class="normal-manager-link">
                            <%= mgr.getManagerName() %>
                        </a>
                        <% } %>
                    </td>
                    <td><%= mgr.getEmail() %>
                    </td>
                    <td>
                            <span class="<%= mgr.isActive() ? "status-active" : "status-inactive" %>">
                                <%= mgr.isActive() ? "활성" : "비활성" %>
                            </span>
                    </td>
                </tr>
                <%
                    }
                } else {
                %>
                <tr>
                    <td colspan="5" class="empty-manager-row">등록된 관리자가 없습니다.</td>
                </tr>
                <%
                    }
                %>
                </tbody>
            </table>

            <%
                // 페이징 영역 출력
                if (managerList != null && !managerList.isEmpty() && totalPages > 1) {
            %>
            <div class="pagination">
                <!-- 이전 페이지 -->
                <% if (currentPage > 1) { %>
                <a href="?page=<%= currentPage - 1 %>">이전</a>
                <% } else { %>
                <span class="disabled">이전</span>
                <% } %>

                <!-- 페이지 번호 -->
                <%
                    // 현재 페이지 기준 앞뒤 2개까지 표시
                    int startPage = Math.max(1, currentPage - 2);
                    int endPage = Math.min(totalPages, currentPage + 2);

                    // 첫 페이지 바로가기 표시
                    if (startPage > 1) {
                %>
                <a href="?page=1">1</a>
                <% if (startPage > 2) { %>
                <span>...</span>
                <% } %>
                <% } %>

                <!-- 페이지 번호 목록 -->
                <% for (int i = startPage; i <= endPage; i++) { %>
                <% if (i == currentPage) { %>
                <span class="current"><%= i %></span>
                <% } else { %>
                <a href="?page=<%= i %>"><%= i %>
                </a>
                <% } %>
                <% } %>

                <!-- 마지막 페이지 바로가기 표시 -->
                <% if (endPage < totalPages) { %>
                <% if (endPage < totalPages - 1) { %>
                <span>...</span>
                <% } %>
                <a href="?page=<%= totalPages %>"><%= totalPages %>
                </a>
                <% } %>

                <!-- 다음 페이지 -->
                <% if (currentPage < totalPages) { %>
                <a href="?page=<%= currentPage + 1 %>">다음</a>
                <% } else { %>
                <span class="disabled">다음</span>
                <% } %>
            </div>
            <% } %>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/common/function.js"></script>
<script src="${pageContext.request.contextPath}/js/manager/list.js"></script>
</html>
