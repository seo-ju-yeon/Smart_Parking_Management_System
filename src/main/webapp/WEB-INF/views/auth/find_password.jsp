<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>비밀번호 찾기 - 주차장 관리 시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/find-password.css">
</head>
<body data-context-path="${pageContext.request.contextPath}">
<div class="container">
    <h2>🔑 비밀번호 찾기</h2>
    <p class="subtitle">아이디와 이메일로 본인을 인증해주세요</p>

    <%-- 컨트롤러에서 전달한 오류 메시지가 있으면 표시 --%>
    <% String serverError = (String) request.getAttribute("error");
        if (serverError != null) { %>
    <div class="msg msg-error show"><%= serverError %>
    </div>
    <% } %>

    <!-- 비밀번호 찾기 진행 단계 -->
    <div class="steps">
        <div class="step">
            <div class="step-circle active" id="circle1">1</div>
            <div class="step-label active" id="label1">아이디 입력</div>
        </div>
        <div class="step-line" id="line1"></div>
        <div class="step">
            <div class="step-circle" id="circle2">2</div>
            <div class="step-label" id="label2">이메일 인증</div>
        </div>
        <div class="step-line" id="line2"></div>
        <div class="step">
            <div class="step-circle" id="circle3">3</div>
            <div class="step-label" id="label3">비밀번호 변경</div>
        </div>
    </div>

    <!-- 공통 안내 메시지 -->
    <div id="globalMsg" class="msg"></div>

    <!-- 아이디 확인 영역 -->
    <div class="panel active" id="step1">
        <div class="form-group">
            <label for="inputId">아이디 <span style="color:#dc3545">*</span></label>
            <input type="text" id="inputId" placeholder="등록된 아이디를 입력하세요">
            <div class="field-error" id="idError"></div>
        </div>
        <button class="btn btn-primary btn-full" onclick="submitStep1()">다음</button>
        <span class="back-link" onclick="goLogin()">← 로그인으로 돌아가기</span>
    </div>

    <!-- 이메일 인증 영역 -->
    <div class="panel" id="step2">
        <div class="form-group">
            <label>아이디</label>
            <input type="text" id="confirmedId" readonly>
        </div>

        <div class="form-group">
            <label for="inputEmail">이메일 <span style="color:#dc3545">*</span></label>
            <div class="input-row">
                <input type="email" id="inputEmail" placeholder="등록된 이메일을 입력하세요">
                <button class="btn btn-secondary" id="sendOtpBtn" onclick="sendOtp()">인증요청</button>
            </div>
            <div class="field-hint">데이터베이스에 등록된 이메일과 일치해야 합니다</div>
            <div class="field-error" id="emailError"></div>
            <!-- 인증번호 유효 시간 -->
            <div id="authTimer" class="auth-timer" style="display:none;">
                ⏱ 남은 시간: <span id="authTimeLeft">05:00</span>
            </div>
        </div>

        <!-- 인증번호 발송 후 표시되는 입력 영역 -->
        <div id="otpGroup" style="display:none;">
            <div class="form-group">
                <label for="inputOtp">인증번호 <span style="color:#dc3545">*</span></label>
                <div class="input-row">
                    <input type="text" id="inputOtp" maxlength="6" placeholder="6자리 인증번호">
                    <button class="btn btn-primary" id="verifyOtpBtn" onclick="verifyOtp()">확인</button>
                </div>
                <div class="field-error" id="otpError"></div>
            </div>
        </div>

        <span class="back-link" onclick="goStep(1)">← 아이디 다시 입력</span>
    </div>

    <!-- OTP 인증 후 새 비밀번호를 설정하는 영역 -->
    <div class="panel" id="step3">
        <!-- 비밀번호 변경 전 입력 폼 -->
        <div id="passwordResetForm">
            <div class="form-group">
                <label for="newPassword">
                    새 비밀번호 <span style="color:#dc3545">*</span>
                </label>
                <input
                        type="password"
                        id="newPassword"
                        autocomplete="new-password"
                        placeholder="새 비밀번호를 입력하세요"
                >
                <div class="field-hint">최소 4자 이상 입력해주세요</div>
                <div class="field-error" id="newPasswordError"></div>
            </div>

            <div class="form-group">
                <label for="confirmPassword">
                    새 비밀번호 확인 <span style="color:#dc3545">*</span>
                </label>
                <input
                        type="password"
                        id="confirmPassword"
                        autocomplete="new-password"
                        placeholder="새 비밀번호를 다시 입력하세요"
                >
                <div class="field-error" id="confirmPasswordError"></div>
            </div>

            <button
                    type="button"
                    class="btn btn-primary btn-full"
                    id="resetPasswordBtn"
                    onclick="submitNewPassword()"
            >
                비밀번호 변경
            </button>
        </div>

        <!-- 비밀번호 변경 성공 후 표시할 결과 영역 -->
        <div id="passwordResetSuccess" style="display:none;">
            <div class="msg msg-success show" style="font-size:15px; line-height:1.8;">
                ✅ 비밀번호가 변경되었습니다.<br>
                새 비밀번호로 로그인해주세요.
            </div>
            <button
                    type="button"
                    class="btn btn-success btn-full"
                    onclick="goLogin()"
            >
                로그인 페이지로 이동
            </button>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth/find-password.js"></script>
</body>
</html>
