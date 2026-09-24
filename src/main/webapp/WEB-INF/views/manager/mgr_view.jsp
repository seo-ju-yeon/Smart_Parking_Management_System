<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerRole" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerVO" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>관리자 정보</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/view.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/menu.jsp" %>

<%
    /* 로그인 관리자 정보 확인
     * menu.jsp의 loginManager 변수와 이름이 겹치지 않도록 sessionLoginManager 사용 */
    ManagerVO sessionLoginManager = (ManagerVO) session.getAttribute("loginManager");
    String loginId = (sessionLoginManager != null) ? sessionLoginManager.getManagerId() : "";
%>

<div class="main-content">
    <div class="container">

        <h2>관리자 정보</h2>

        <%-- 성공 메시지 표시 --%>
        <% String successMessage = (String) session.getAttribute("successMessage");
            if (successMessage != null) {
                session.removeAttribute("successMessage"); %>
        <div class="message success-message">
            <%= successMessage %>
        </div>
        <% } %>

        <%-- 에러 메시지 표시 --%>
        <% String error = (String) request.getAttribute("error");
            if (error == null) error = (String) session.getAttribute("error");
            if (error != null) {
                session.removeAttribute("error"); %>
        <div class="message error-message">
            <%= error %>
        </div>
        <% } %>

        <%
            ManagerVO manager = (ManagerVO) request.getAttribute("manager");
            if (manager != null) {

                /* 조회 대상이 본인 계정인지, 최고관리자인지 확인 */
                boolean isSelf      = manager.getManagerId().equals(loginId);
                boolean isAdminRole = manager.getRole() == ManagerRole.ADMIN;

                /* 최고관리자 본인 계정 비활성화 차단 여부 */
                boolean blockDeactivate = isSelf && isAdminRole;
        %>

        <!-- 관리자 정보 영역 -->
        <div class="info-section">
            <div class="info-row">
                <div class="info-label">아이디</div>
                <div class="info-value"><%= manager.getManagerId() %></div>
            </div>
            <div class="info-row">
                <div class="info-label">이름</div>
                <div class="info-value"><%= manager.getManagerName() %></div>
            </div>
            <div class="info-row">
                <div class="info-label">이메일</div>
                <div class="info-value"><%= manager.getEmail() %></div>
            </div>
            <div class="info-row">
                <div class="info-label">계정 상태</div>
                <div class="info-value">
                    <% if (manager.isActive()) { %>
                    <span class="status-badge status-active">활성</span>
                    <% } else { %>
                    <span class="status-badge status-inactive">비활성</span>
                    <% } %>
                </div>
            </div>
        </div>

        <!-- 처리 버튼 영역 -->
        <div class="btn-group">
            <button type="button" class="btn btn-secondary navigation-button"
                    data-url="${pageContext.request.contextPath}/mgr/list">
                돌아가기
            </button>

            <button type="button" class="btn btn-primary flex-button navigation-button"
                    data-url="${pageContext.request.contextPath}/mgr/modify_normal?id=<%= manager.getManagerId() %>">
                정보 수정
            </button>

            <% if (manager.isActive()) { %>
            <%-- 관리자 계정 비활성화 요청 전송 --%>
            <form action="${pageContext.request.contextPath}/mgr/toggleActive" method="post" class="flex-form">
                <input type="hidden" name="managerId" value="<%= manager.getManagerId() %>">
                <input type="hidden" name="active" value="false">
                <button type="submit" class="btn btn-danger full-width-button account-state-button"
                <%-- 최고관리자 본인은 비활성화 제출 차단 --%>
                        <% if (blockDeactivate) { %>
                        data-block-deactivate="true"
                        <% } else { %>
                        data-confirm-message="이 관리자 계정을 비활성화 하시겠습니까?"
                        <% } %>
                >
                    계정 비활성화
                </button>
            </form>
            <% } else { %>
            <%-- 관리자 계정 활성화 요청 전송 --%>
            <form action="${pageContext.request.contextPath}/mgr/toggleActive" method="post" class="flex-form">
                <input type="hidden" name="managerId" value="<%= manager.getManagerId() %>">
                <input type="hidden" name="active" value="true">
                <button type="submit" class="btn btn-success full-width-button account-state-button"
                        data-confirm-message="이 관리자 계정을 활성화 하시겠습니까?">
                    계정 활성화
                </button>
            </form>
            <% } %>
        </div>

        <% } else { %>
        <!-- 조회 결과 없음 -->
        <div class="empty-state">
            <div class="empty-state-icon">👤</div>
            <div class="empty-state-text">조회할 관리자 정보가 없습니다.</div>
            <button type="button" class="btn btn-primary navigation-button"
                    data-url="${pageContext.request.contextPath}/mgr/add">
                관리자 추가
            </button>
        </div>
        <% } %>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/manager/view.js"></script>
</body>
</html>
