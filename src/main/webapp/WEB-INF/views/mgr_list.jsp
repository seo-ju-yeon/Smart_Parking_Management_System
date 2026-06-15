<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="org.example.smart_parking_260219.dto.ManagerDTO" %>
<%@ page import="org.example.smart_parking_260219.vo.ManagerVO" %>

<html>
<head>
    <title>관리자 목록 - 스마트 파킹 시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/style.css">
    <style>
        /* 메인 컨텐츠 영역 */
        .main-content {
            padding: 40px;
            background-color: #f4f7f6;
            min-height: 100vh;
        }

        /* 관리자 목록 테이블 */
        .manager-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
        }

        /* 테이블 헤더 강조 */
        .manager-table thead th {
            background-color: #2c3e50;
            color: #ffffff;
            padding: 15px;
            font-size: 1.1em;
            border-bottom: 3px solid #1a252f;
            text-align: center;
        }

        .manager-table tbody td {
            padding: 12px;
            border-bottom: 1px solid #eee;
            text-align: center;
            color: #333;
        }

        /* 행 hover 효과 */
        .manager-table tbody tr:hover {
            background-color: #f8f9fa;
        }

        .btn-add:hover {
            background: #1abc9c;
        }

        .btn-add {
            float: right;
            padding: 8px 15px;
            background: #2c3e50;
            color: white;
            text-decoration: none;
            border-radius: 4px;
        }

        /* 페이징 */
        .pagination {
            display: flex;
            justify-content: center;
            align-items: center;
            margin-top: 30px;
            gap: 5px;
        }

        .pagination a, .pagination span {
            padding: 8px 12px;
            text-decoration: none;
            border: 1px solid #ddd;
            color: #2c3e50;
            border-radius: 4px;
            transition: all 0.3s;
        }

        .pagination a:hover {
            background-color: #2c3e50;
            color: white;
        }

        .pagination .current {
            background-color: #2c3e50;
            color: white;
            font-weight: bold;
        }

        .pagination .disabled {
            color: #ccc;
            cursor: not-allowed;
            border-color: #eee;
        }

        .pagination .disabled:hover {
            background-color: transparent;
            color: #ccc;
        }

        /* 관리자 접근 제한 모달 */
        .modal-backdrop {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.45);
            z-index: 9000;
            justify-content: center;
            align-items: center;
        }

        .modal-backdrop.show {
            display: flex;
        }

        .modal-box {
            background: #fff;
            border-radius: 12px;
            padding: 36px 32px 28px;
            max-width: 420px;
            width: 90%;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
            text-align: center;
            animation: modalIn 0.18s ease;
        }

        @keyframes modalIn {
            from {
                transform: translateY(-20px);
                opacity: 0;
            }
            to {
                transform: translateY(0);
                opacity: 1;
            }
        }

        .modal-icon {
            font-size: 44px;
            margin-bottom: 14px;
        }

        .modal-title {
            font-size: 18px;
            font-weight: 700;
            color: #2c3e50;
            margin-bottom: 10px;
        }

        .modal-desc {
            font-size: 14px;
            color: #555;
            line-height: 1.7;
            margin-bottom: 24px;
        }

        .modal-desc strong {
            color: #667eea;
        }

        .modal-btn {
            display: inline-block;
            padding: 10px 32px;
            background: #2c3e50;
            color: #fff;
            border: none;
            border-radius: 6px;
            font-size: 15px;
            cursor: pointer;
            transition: background 0.2s;
        }

        .modal-btn:hover {
            background: #1a252f;
        }

        /* 특수 관리자 행 강조 */
        .row-admin td {
            background-color: #f0f4ff;
        }

        .badge-admin {
            display: inline-block;
            font-size: 11px;
            background: #667eea;
            color: #fff;
            border-radius: 4px;
            padding: 1px 6px;
            margin-left: 6px;
            vertical-align: middle;
        }
    </style>
</head>
<body>
<%@ include file="../main/menu.jsp" %>

<%-- 최고관리자 접근 차단 안내 모달 --%>
<div id="adminModal" class="modal-backdrop">
    <div class="modal-box">
        <div class="modal-icon">🔒</div>
        <div class="modal-title">접근 제한</div>
        <div class="modal-desc">
            최고 관리자 계정은 이 화면에서 수정할 수 없습니다.<br>
            좌측 메뉴의 <strong>최고 관리자 정보 수정</strong> 탭을 이용해 주세요.
        </div>
        <button class="modal-btn" onclick="closeAdminModal()">확인</button>
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
        <button class="modal-btn" onclick="closeSuperModal()">확인</button>
    </div>
</div>

    <%
    /* 현재 로그인한 관리자 권한 확인 */
    ManagerVO sessionMgr = (ManagerVO) session.getAttribute("loginManager");
    String loginRole = (sessionMgr != null) ? sessionMgr.getRole() : "";
%>

<div class="main-content">
    <div id="entry" class="page">
        <div style="overflow: hidden;">
            <h2 style="display: inline-block;">관리자 계정 목록</h2>

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
                        <a href="javascript:void(0);"
                           onclick="openAdminModal();"
                           style="color: #667eea; font-weight: bold; cursor: pointer;">
                            <%= mgr.getManagerName() %>
                        </a>
                        <span class="badge-admin">최고관리자</span>
                        <% } else if ("SUPER".equals(mgr.getRole())) { %>
                        <%-- 슈퍼관리자는 전용 안내 모달 표시 --%>
                        <a href="javascript:void(0);"
                           onclick="openSuperModal();"
                           style="color: #667eea; font-weight: bold; cursor: pointer;">
                            <%= mgr.getManagerName() %>
                        </a>
                        <span class="badge-admin">슈퍼관리자</span>
                        <% } else { %>
                        <%-- 일반관리자는 상세 조회 화면으로 이동 --%>
                        <a href="${pageContext.request.contextPath}/mgr/view?id=<%= mgr.getManagerId() %>"
                           style="color: #007bff; font-weight: bold;">
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
                    <td colspan="5" style="padding: 30px; color: #999;">등록된 관리자가 없습니다.</td>
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

<script src="${pageContext.request.contextPath}/JS/menu.js"></script>
<script src="${pageContext.request.contextPath}/JS/function.js"></script>
<script>
    // 최고관리자 접근 차단 모달 제어
    function openAdminModal() {
        document.getElementById('adminModal').classList.add('show');
    }

    function closeAdminModal() {
        document.getElementById('adminModal').classList.remove('show');
    }

    // 슈퍼관리자 접근 차단 모달 제어
    function openSuperModal() {
        document.getElementById('superModal').classList.add('show');
    }

    function closeSuperModal() {
        document.getElementById('superModal').classList.remove('show');
    }

    // 모달 바깥 영역을 클릭하면 닫음
    document.getElementById('adminModal').addEventListener('click', function (e) {
        if (e.target === this) closeAdminModal();
    });
    document.getElementById('superModal').addEventListener('click', function (e) {
        if (e.target === this) closeSuperModal();
    });
    // ESC 키로 모달 닫기
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') { closeAdminModal(); closeSuperModal(); }
    });
</script>
</html>
