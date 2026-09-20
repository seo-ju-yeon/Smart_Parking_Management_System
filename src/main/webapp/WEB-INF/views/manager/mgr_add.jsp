<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>관리자 추가</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/add.css">
</head>
<body data-context-path="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/common/menu.jsp" %>

<div class="main-content">
    <div class="container">
        <h2>관리자 추가</h2>

        <%-- 관리자 추가 실패 메시지 표시 --%>
        <% String error = (String) request.getAttribute("error");
            if (error != null) { %>
        <div class="message error-message">
            <%= error %>
        </div>
        <% } %>

        <%-- 신규 관리자 정보 전송 --%>
        <form id="managerForm" action="${pageContext.request.contextPath}/mgr/add" method="post">
            <div class="form-group">
                <label for="id">아이디 <span class="required">*</span></label>
                <input type="text" id="id" name="id"
                       value="<%= request.getAttribute("managerId") != null ? request.getAttribute("managerId") : "" %>"
                       maxlength="50"
                       required>
                <div class="field-hint">영문, 숫자 조합 4-50자</div>
                <div class="field-error" id="idError"></div>
            </div>

            <div class="form-group">
                <label for="name">이름 <span class="required">*</span></label>
                <input type="text" id="name" name="name"
                       value="<%= request.getAttribute("managerName") != null ? request.getAttribute("managerName") : "" %>"
                       maxlength="20"
                       required>
                <div class="field-hint">최대 20자</div>
                <div class="field-error" id="nameError"></div>
            </div>

            <div class="form-group">
                <label for="pw">비밀번호 <span class="required">*</span></label>
                <input type="password" id="pw" name="pw" required>
                <div class="password-strength" id="passwordStrength"></div>
                <div class="field-hint">최소 4자 이상 (영문, 숫자, 특수문자 조합 권장)</div>
                <div class="field-error" id="pwError"></div>
            </div>

            <div class="form-group">
                <label for="passwordConfirm">비밀번호 확인 <span class="required">*</span></label>
                <input type="password" id="passwordConfirm" name="passwordConfirm" required>
                <div class="field-error" id="passwordConfirmError"></div>
            </div>

            <div class="form-group">
                <label for="email">이메일 <span class="required">*</span></label>
                <div style="display: flex; gap: 8px;">
                    <input type="email" id="email" name="email"
                           value="<%= request.getAttribute("email") != null ? request.getAttribute("email") : "" %>"
                           maxlength="100"
                           placeholder="example@email.com" required
                           style="flex: 1; margin-bottom: 0;"> <button type="button" id="sendEmailBtn" class="btn btn-secondary"
                                                                       style="width: 100px; padding: 0; font-size: 14px; height: 45px;">인증요청</button>
                </div>
                <div class="field-hint">2차 인증에 사용됩니다</div>
                <div class="field-error" id="emailError"></div>

                <div id="emailAuthGroup" style="margin-top: 12px; display: none;">
                    <div style="display: flex; gap: 8px;">
                        <input type="text" id="authCode" placeholder="인증번호 6자리"
                               maxlength="6" style="flex: 1; margin-bottom: 0;">
                        <button type="button" id="verifyBtn" class="btn btn-primary"
                                style="width: 100px; padding: 0; font-size: 14px; height: 45px;">확인</button>
                    </div>
                    <div class="field-hint" id="authHint">이메일로 발송된 번호를 입력해주세요.</div>
                    <%-- 인증번호 유효 시간 표시 --%>
                    <div id="authTimer" class="auth-timer" style="display: none;">
                        ⏱ 남은 시간: <span id="authTimeLeft">05:00</span>
                    </div>
                </div>
            </div>

            <div class="btn-group">
                <button type="submit" class="btn btn-primary" id="submitBtn">추가하기</button>
                <button type="button" class="btn btn-secondary"
                        onclick="location.href='${pageContext.request.contextPath}/dashboard'">
                    취소
                </button>
            </div>
        </form>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/manager/add.js"></script>
</body>
</html>
