<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerRole" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerVO" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>내 정보 수정</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/my-modify.css">
</head>
<body data-context-path="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/common/menu.jsp" %>

<div class="main-content">
    <div class="container">
        <h2>내 정보 수정</h2>

        <%-- 수정 안내 메시지 표시 --%>
        <div class="message info-message">
            ℹ️ 정보 수정을 위해 이메일 인증이 필요합니다. <br>
            ℹ️ 수정 완료 후 재로그인이 필요합니다.
        </div>

        <%-- 성공 메시지 표시 --%>
        <% String successMsg = (String) session.getAttribute("successMessage");
           if (successMsg != null) { session.removeAttribute("successMessage"); %>
        <div class="message success-message"><%= successMsg %></div>
        <% } %>

        <%-- 수정 실패 메시지 표시 --%>
        <% String error = (String) request.getAttribute("error");
           if (error != null) { %>
        <div class="message error-message"><%= error %></div>
        <% } %>

        <%
            // 세션에서 로그인 관리자 정보 확인
            ManagerVO manager = (ManagerVO) session.getAttribute("loginManager");

            // 최고관리자는 전용 수정 페이지로 이동
            if (manager != null && manager.getRole() == ManagerRole.ADMIN) {
                response.sendRedirect(request.getContextPath() + "/mgr/modify");
                return;
            }

            if (manager != null) {
        %>
        <%-- 본인 수정 정보 전송 --%>
        <form id="modifyForm" action="${pageContext.request.contextPath}/mgr/my_modify" method="post">

            <%-- 아이디는 읽기 전용으로 표시하고 hidden 값으로 전송 --%>
            <div class="form-group">
                <label for="managerIdDisplay">아이디</label>
                <input type="text" id="managerIdDisplay" value="<%= manager.getManagerId() %>" readonly>
                <input type="hidden" name="managerId" value="<%= manager.getManagerId() %>">
                <div class="field-hint">아이디는 변경할 수 없습니다.</div>
            </div>

            <%-- 이름 입력 영역 --%>
            <div class="form-group">
                <label for="name">이름 <span class="required">*</span></label>
                <input type="text" id="name" name="name"
                       value="<%= manager.getManagerName() %>" maxlength="20" required>
                <div class="field-hint">최대 20자</div>
                <div class="field-error" id="nameError"></div>
            </div>

            <%-- 새 비밀번호 입력 영역 --%>
            <div class="form-group">
                <label for="pw">새 비밀번호</label>
                <input type="password" id="pw" name="pw">
                <div class="password-strength" id="passwordStrength"></div>
                <div class="field-hint">변경하지 않으려면 비워두세요 (변경 시 최소 4자 이상)</div>
                <div class="field-error" id="pwError"></div>
            </div>

            <%-- 새 비밀번호 확인 영역 --%>
            <div class="form-group">
                <label for="passwordConfirm">새 비밀번호 확인</label>
                <input type="password" id="passwordConfirm" name="passwordConfirm">
                <div class="field-error" id="passwordConfirmError"></div>
            </div>

            <%-- 이메일 인증 영역 --%>
            <div class="form-group">
                <label for="email">이메일 <span class="required">*</span></label>
                <div class="email-input-row">
                    <input type="email" id="email" name="email"
                           value="<%= manager.getEmail() %>" maxlength="100"
                           placeholder="example@email.com" required class="email-input">
                    <button type="button" id="sendEmailBtn" class="btn btn-secondary send-email-button">인증요청</button>
                </div>
                <div class="field-hint">정보 수정 시 반드시 이메일 인증이 필요합니다.</div>
                <div class="field-error" id="emailError"></div>

                <%-- 인증번호 입력 영역 --%>
                <div id="emailAuthGroup" class="email-auth-group initially-hidden">
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

            <%-- 처리 버튼 영역 --%>
            <div class="btn-group">
                <button type="button" class="btn btn-secondary navigation-button"
                        data-url="${pageContext.request.contextPath}/dashboard">
                    취소
                </button>
                <button type="submit" id="submitBtn" class="btn btn-primary">
                    변경사항 적용
                </button>
            </div>
        </form>

        <% } else { %>
        <div class="message error-message">
            로그인 정보를 불러올 수 없습니다. 다시 로그인해주세요.
        </div>
        <div class="btn-group">
            <button type="button" class="btn btn-secondary navigation-button"
                    data-url="${pageContext.request.contextPath}/login">
                로그인 페이지로
            </button>
        </div>
        <% } %>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/manager/my-modify.js"></script>
</body>
</html>
