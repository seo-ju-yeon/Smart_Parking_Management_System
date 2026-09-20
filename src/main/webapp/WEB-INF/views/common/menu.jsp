<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerVO" %>
<nav>
    <h1>주차장 관리 시스템</h1>

    <%
        // 세션에서 "loginManager" 객체를 가져옴 -> Controller에서 session.setAttribute("loginManager", manager)
        Object loginManager = session.getAttribute("loginManager");
        String mName = (String) session.getAttribute("managerName");
        // mRole을 초기화할 때 null 방지를 위해 빈 문자열로 시작
        String mRole = "";

        // 세션에서 꺼낸 객체가 ManagerVO 타입인지 확인하고 캐스팅
        if (loginManager instanceof ManagerVO) {
            ManagerVO vo = (ManagerVO) loginManager;
            mRole = vo.getRole();
        }

        if (loginManager != null && mName != null) {
    %>
    <div class="sidebar-manager-info">
        관리자 : <span class="sidebar-manager-name"><%= mName %></span> 님
    </div>
    <div id="liveClock" class="sidebar-live-clock">
        0000-00-00 00:00:00
    </div>
    <%
    } else {
    %>
    <div class="sidebar-login-missing">
        로그인 정보가 없습니다.
    </div>
    <%
        }
    %>

    <ul id="navMenu">

        <%-- ADMIN 또는 SUPER: 관리자 메뉴 토글 표시 --%>
        <% if ("ADMIN".equals(mRole) || "SUPER".equals(mRole)) { %>
        <li class="dropdown">
            <a href="#" class="dropbtn">관리자 메뉴 ▼</a>
            <div id="adminSubMenu" class="dropdown-content">
                <a href="${pageContext.request.contextPath}/mgr/add" class="confirm-add-manager">일반 관리자 추가</a>
                <a href="${pageContext.request.contextPath}/mgr/list">관리자 목록 & 수정</a>
                <a href="${pageContext.request.contextPath}/mgr/modify">최고 관리자 정보 수정</a>
            </div>
        </li>
        <% } %>

        <%-- NORMAL 또는 SUPER: 내 정보 수정 메뉴 표시 --%>
        <% if ("NORMAL".equals(mRole) || "SUPER".equals(mRole)) { %>
        <li><a href="${pageContext.request.contextPath}/mgr/my_modify">내 정보 수정</a></li>
        <% } %>
        <li><a href="${pageContext.request.contextPath}/dashboard">주차 현황</a></li>
        <li><a href="${pageContext.request.contextPath}/input">입차</a></li>
        <li><a href="${pageContext.request.contextPath}/list">출차</a></li>
        <li><a href="${pageContext.request.contextPath}/member/member_add">회원 등록</a></li>
        <li><a href="${pageContext.request.contextPath}/member/member_list">회원 목록</a></li>
        <li><a href="${pageContext.request.contextPath}/member/member_search">회원 조회</a></li>
        <li><a href="${pageContext.request.contextPath}/view/policy/list">요금 부과 정책</a></li>
        <li><a href="${pageContext.request.contextPath}/statistics/statistics">매출 통계</a></li>
        <li><a href="${pageContext.request.contextPath}/logout" class="confirm-logout">로그아웃</a></li>
    </ul>
</nav>
<script src="${pageContext.request.contextPath}/js/common/menu.js"></script>
