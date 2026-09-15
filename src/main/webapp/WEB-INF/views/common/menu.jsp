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
    <div style="padding: 15px; margin-bottom: 1px; background-color: #1a252f; border-radius: 4px; text-align: center;">
        관리자 : <span style="font-weight: bold; color: #e0e0e0;"><%= mName %></span> 님
    </div>
    <div id="liveClock"
         style="font-size: 0.9em; background-color: #1a252f; color: #95a5a6; letter-spacing: 1px; text-align: center">
        0000-00-00 00:00:00
    </div>
    <%
    } else {
    %>
    <div style="padding: 10px; margin-bottom: 10px; color: #ff0000; text-align: center; font-size: 0.85em;">
        로그인 정보가 없습니다.
    </div>
    <%
        }
    %>

    <ul id="navMenu">

        <%-- ADMIN 또는 SUPER: 관리자 메뉴 토글 표시 --%>
        <% if ("ADMIN".equals(mRole) || "SUPER".equals(mRole)) { %>
        <li class="dropdown">
            <a href="javascript:void(0);" class="dropbtn" onclick="toggleDropdown()">관리자 메뉴 ▼</a>
            <div id="adminSubMenu" class="dropdown-content">
                <a href="${pageContext.request.contextPath}/mgr/add" onclick="return confirmAddManager();">일반 관리자 추가</a>
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
        <li><a href="${pageContext.request.contextPath}/logout" onclick="return confirmLogout();">로그아웃</a></li>
    </ul>
</nav>

<script>
    <%-- 실시간 시계 --%>

    function updateClock() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        const timeString = year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;
        const clockElement = document.getElementById("liveClock");
        if (clockElement) {
            clockElement.innerText = timeString;
        }
    }

    setInterval(updateClock, 1000); // 1초마다 updateClock 함수 실행
    window.onload = updateClock;  // 페이지 로드 시 즉시 실행 (1초 대기 방지)

    <%-- 관리자 추가 함수 --%>

    function confirmAddManager() {
        // confirm() 확인을 누르면 true -> href 경로로 이동
        // confirm() 취소를 누르면 false -> 이동 취소 (현재 화면 유지)
        return confirm("관리자 추가 페이지로 이동하시겠습니까?");
    }

    <%-- 로그아웃 함수 --%>

    function confirmLogout() {
        if (confirm("로그아웃을 하시겠습니까?")) {  // confirm()은 확인을 누르면 true, 취소를 누르면 false를 반환
            ${pageContext.request.contextPath}  // '확인' 클릭 시: true가 반환되어 href 주소(/logout)로 이동
            return true;
        } else {
            // '취소' 클릭 시 아무런 동작도 하지 않고 현재 페이지에 머묾
            return false;
        }
    }

    <%-- 토글 --%>

    function toggleDropdown() {
        const dropdown = document.getElementById("adminSubMenu");
        // 'show' 클래스가 있으면 제거하고, 없으면 추가함
        dropdown.classList.toggle("show");
    }

    <%-- 메뉴 외부 클릭 시 드롭다운 닫기 --%>
    window.onclick = function (event) {
        if (!event.target.matches('.dropbtn')) {
            const dropdowns = document.getElementsByClassName("dropdown-content");
            for (let i = 0; i < dropdowns.length; i++) {
                const openDropdown = dropdowns[i];
                if (openDropdown.classList.contains('show')) {
                    openDropdown.classList.remove('show');
                }
            }
        }
    }
</script>
<style>
    /* 드롭다운 컨테이너 */
    .dropdown-content {
        display: none; /* 기본적으로 숨김 */
        background-color: #2c3e50; /* 사이드바보다 약간 밝거나 어두운 색상 */
        padding-left: 15px; /* 들여쓰기로 계층 구조 표시 */
    }

    /* 드롭다운 내부 링크 */
    .dropdown-content a {
        font-size: 0.9em;
        padding: 10px;
        color: #bdc3c7;
        text-decoration: none;
        display: block;
    }

    .dropdown-content a:hover {
        color: #ffffff;
    }

    /* 토글 시 보여줄 클래스 */
    .show {
        display: block;
    }
</style>