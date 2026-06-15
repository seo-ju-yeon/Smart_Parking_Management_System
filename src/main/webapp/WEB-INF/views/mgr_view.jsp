<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerVO" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>관리자 정보</title>
    <link rel="stylesheet" href="/CSS/style.css">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f5f5;
            margin: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }
        .main-content {
            width: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
        }
        .container {
            width: 100%;
            max-width: 600px;
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h2 {
            color: #333;
            margin-bottom: 30px;
            padding-bottom: 10px;
            border-bottom: 2px solid #667eea;
        }
        .message {
            padding: 12px;
            border-radius: 5px;
            margin-bottom: 20px;
            text-align: center;
        }
        .success-message {
            background: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .error-message {
            background: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .info-section {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        .info-row {
            display: flex;
            padding: 12px 0;
            border-bottom: 1px solid #e0e0e0;
        }
        .info-row:last-child {
            border-bottom: none;
        }
        .info-label {
            flex: 0 0 150px;
            font-weight: 600;
            color: #555;
        }
        .info-value {
            flex: 1;
            color: #333;
        }
        .status-badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 500;
        }
        .status-active {
            background: #d4edda;
            color: #155724;
        }
        .status-inactive {
            background: #f8d7da;
            color: #721c24;
        }
        .btn-group {
            display: flex;
            gap: 10px;
            margin-top: 30px;
        }
        .btn {
            padding: 12px 24px;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            cursor: pointer;
            transition: all 0.3s;
            text-decoration: none;
            display: inline-block;
            text-align: center;
        }
        .btn-primary {
            background: #667eea;
            color: white;
        }
        .btn-primary:hover {
            background: #5568d3;
        }
        .btn-secondary {
            background: #6c757d;
            color: white;
        }
        .btn-secondary:hover {
            background: #5a6268;
        }
        .btn-danger {
            background: #dc3545;
            color: white;
        }
        .btn-danger:hover {
            background: #c82333;
        }
        .btn-success {
            background: #28a745;
            color: white;
        }
        .btn-success:hover {
            background: #218838;
        }
        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: #999;
        }
        .empty-state-icon {
            font-size: 48px;
            margin-bottom: 20px;
        }
        .empty-state-text {
            font-size: 18px;
            margin-bottom: 30px;
        }
    </style>
</head>
<body>
<%@ include file="../main/menu.jsp" %>

<%
    /* 로그인 관리자 정보 확인
     * menu.jsp의 loginManager 변수와 이름이 겹치지 않도록 sessionLoginManager 사용 */
    ManagerVO sessionLoginManager = (ManagerVO) session.getAttribute("loginManager");
    String loginId   = (sessionLoginManager != null) ? sessionLoginManager.getManagerId() : "";
    String loginRole = (sessionLoginManager != null) ? sessionLoginManager.getRole()      : "";
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
                boolean isAdminRole = "ADMIN".equals(manager.getRole());

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
            <button type="button" class="btn btn-secondary"
                    onclick="location.href='${pageContext.request.contextPath}/mgr/list'">
                돌아가기
            </button>

            <button type="button" class="btn btn-primary" style="flex: 1;"
                    onclick="location.href='${pageContext.request.contextPath}/mgr/modify_normal?id=<%= manager.getManagerId() %>'">
                정보 수정
            </button>

            <% if (manager.isActive()) { %>
            <%-- 관리자 계정 비활성화 요청 전송 --%>
            <form action="${pageContext.request.contextPath}/mgr/toggleActive" method="post" style="flex: 1;">
                <input type="hidden" name="managerId" value="<%= manager.getManagerId() %>">
                <input type="hidden" name="active" value="false">
                <button type="submit" class="btn btn-danger" style="width: 100%;"
                <%-- 최고관리자 본인은 비활성화 제출 차단 --%>
                        <% if (blockDeactivate) { %>
                        onclick="return alertAdminCannotDeactivate();"
                        <% } else { %>
                        onclick="return confirm('이 관리자 계정을 비활성화 하시겠습니까?');"
                        <% } %>
                >
                    계정 비활성화
                </button>
            </form>
            <% } else { %>
            <%-- 관리자 계정 활성화 요청 전송 --%>
            <form action="${pageContext.request.contextPath}/mgr/toggleActive" method="post" style="flex: 1;">
                <input type="hidden" name="managerId" value="<%= manager.getManagerId() %>">
                <input type="hidden" name="active" value="true">
                <button type="submit" class="btn btn-success" style="width: 100%;"
                        onclick="return confirm('이 관리자 계정을 활성화 하시겠습니까?');">
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
            <button type="button" class="btn btn-primary"
                    onclick="location.href='${pageContext.request.contextPath}/mgr/add'">
                관리자 추가
            </button>
        </div>
        <% } %>
    </div>
</div>

<script>
    // 최고관리자 본인 계정 비활성화 차단
    function alertAdminCannotDeactivate() {
        alert('최고 관리자 계정은 비활성화할 수 없습니다.\n계정을 비활성화하려면 다른 최고 관리자에게 문의하세요.');
        return false;
    }

    // 성공 메시지는 3초 후 자동 제거
    window.onload = function() {
        // 성공 메시지 요소 가져오기
        const successMsg = document.querySelector('.success-message');
        if (successMsg) {
            setTimeout(() => {
                successMsg.style.transition = 'opacity 0.5s';
                successMsg.style.opacity = '0';
                setTimeout(() => successMsg.remove(), 500);
            }, 3000);
        }
    };
</script>
</body>
</html>
