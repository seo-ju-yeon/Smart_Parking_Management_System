<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerVO" %>
<%
    // ADMIN 또는 SUPER만 접근 가능
    ManagerVO loginCheck = (ManagerVO) session.getAttribute("loginManager");
    if (loginCheck == null ||
            (!"ADMIN".equals(loginCheck.getRole()) && !"SUPER".equals(loginCheck.getRole()))) {
        response.sendRedirect(request.getContextPath() + "/mgr/my_modify");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>일반 관리자 정보 수정</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/modify-normal.css">
</head>
<body data-context-path="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/common/menu.jsp" %>

<div class="main-content">
    <div class="container">
        <h2>일반 관리자 정보 수정</h2>

        <%-- 수정 안내 메시지 표시 --%>
        <div class="message info-message">
            ℹ️ 정보 수정 시 이메일 인증이 필요합니다.
        </div>

        <%-- 성공 메시지 표시 --%>
        <% String successMessage = (String) session.getAttribute("successMessage");
            if (successMessage != null) {
                session.removeAttribute("successMessage"); %>
        <div class="message success-message">
            <%= successMessage %>
        </div>
        <% } %>

        <%-- 수정 실패 메시지 표시 --%>
        <% String error = (String) request.getAttribute("error");
            if (error != null) { %>
        <div class="message error-message">
            <%= error %>
        </div>
        <% } %>

        <%
            ManagerVO manager = (ManagerVO) request.getAttribute("manager");
            if (manager != null) {
        %>
        <%-- 일반 관리자 수정 정보 전송 --%>
        <form id="modifyForm" action="${pageContext.request.contextPath}/mgr/modify_normal" method="post">
            <!-- 수정 불가 아이디 영역 -->
            <div class="form-group">
                <label for="id">아이디 <span class="required">*</span></label>
                <input type="text" id="id" name="managerId" value="<%= manager.getManagerId() %>" readonly>
                <div class="field-hint">아이디는 변경할 수 없습니다</div>
            </div>

            <!-- 이름 입력 영역 -->
            <div class="form-group">
                <label for="name">이름 <span class="required">*</span></label>
                <input type="text" id="name" name="name" value="<%= manager.getManagerName() %>"
                       maxlength="20" required>
                <div class="field-hint">최대 20자</div>
                <div class="field-error" id="nameError"></div>
            </div>

            <!-- 새 비밀번호 입력 영역 -->
            <div class="form-group">
                <label for="pw">새 비밀번호</label>
                <input type="password" id="pw" name="pw">
                <div class="password-strength" id="passwordStrength"></div>
                <div class="field-hint">변경하지 않으려면 비워두세요 (변경 시 최소 4자 이상 입력)</div>
                <div class="field-error" id="pwError"></div>
            </div>

            <!-- 새 비밀번호 확인 영역 -->
            <div class="form-group">
                <label for="passwordConfirm">새 비밀번호 확인</label>
                <input type="password" id="passwordConfirm" name="passwordConfirm">
                <div class="field-error" id="passwordConfirmError"></div>
            </div>

            <!-- 이메일 인증 영역 -->
            <div class="form-group">
                <label for="email">이메일 <span class="required">*</span></label>
                <div class="email-input-row">
                    <input type="email" id="email" name="email" value="<%= manager.getEmail() %>"
                           maxlength="100" placeholder="example@email.com" required class="email-input">
                    <button type="button" id="sendEmailBtn" class="btn btn-secondary send-email-button">인증요청</button>
                </div>
                <div class="field-hint">변경된 이메일 인증이 필요합니다</div>
                <div class="field-error" id="emailError"></div>

                <!-- 인증번호 입력 영역 -->
                <div id="emailAuthGroup" class="email-auth-group">
                    <div class="auth-code-row">
                        <input type="text" id="authCode" placeholder="인증번호 6자리"
                               maxlength="6" class="auth-code-input">
                        <button type="button" id="verifyBtn" class="btn btn-primary verify-code-button">확인</button>
                    </div>
                    <div id="authTimer" class="auth-timer initially-hidden">
                        ⏱ 남은 시간: <span id="authTimeLeft">05:00</span>
                    </div>
                </div>
            </div>

            <!-- 처리 버튼 영역 -->
            <div class="btn-group">
                <button type="button" class="btn btn-secondary navigation-button"
                        data-url="${pageContext.request.contextPath}/mgr/list">
                    취소
                </button>
                <button type="submit" id="submitBtn" class="btn btn-primary">
                    변경사항 적용
                </button>
            </div>
        </form>
        <% } else { %>
        <div class="message error-message">
            수정할 관리자 정보를 불러올 수 없습니다.
        </div>
        <div class="btn-group">
            <button type="button" class="btn btn-secondary navigation-button"
                    data-url="${pageContext.request.contextPath}/mgr/list">
                목록으로
            </button>
        </div>
        <% } %>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/manager/modify-normal.js"></script>
</body>
</html>
