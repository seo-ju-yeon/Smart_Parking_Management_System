<%@ page import="org.example.smart_parking_260219.dto.FeePolicyDTO" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    FeePolicyDTO policy = (FeePolicyDTO) request.getAttribute("policy");
    String pageNum = request.getParameter("pageNum");
    String items = (request.getParameter("items") != null) ? request.getParameter("items") : "";
    String keyword = (request.getParameter("keyword") != null) ? request.getParameter("keyword") : "";

    if (pageNum == null) pageNum = "1";
%>
<html>
<head>
    <title>요금 정책 상세 조회</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <link rel="stylesheet" href="../CSS/style.css">
    <style>
        .main-content {
            display: flex;
            flex-direction: column;
            align-items: center;    /* 가로축 중앙 정렬 */
            justify-content: flex-start;
            min-height: 100vh;
            padding: 40px 20px;
            background-color: #f8f9fc;
        }
        .detail-card {
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.08);
            padding: 40px;
            margin-top: 30px;

            width: 100%;            /* 모바일 대응 */
            max-width: 900px;       /* [추가] 상세페이지는 리스트보다 조금 좁은 게 더 예쁩니다 */
            margin-left: auto;      /* [추가] 좌우 마진 자동 */
            margin-right: auto;     /* [추가] 좌우 마진 자동 */
        }
        .detail-header {
            border-bottom: 2px solid #f8f9fa;
            margin-bottom: 30px;
            padding-bottom: 20px;
        }
        .detail-header h2 {
            font-weight: 700;
            color: #333;
            margin: 0;
        }
        .info-group {
            display: flex;
            flex-wrap: wrap;
            justify-content: space-between;
        }
        .info-item {
            flex: 0 0 50%; /* 2단 구성 */
            display: flex;
            align-items: center;
            margin-bottom: 25px;
            padding: 10px;
            border-bottom: 1px solid #fcfcfc;
        }
        .info-label {
            width: 150px;
            font-weight: 600;
            color: #6c757d;
            display: flex;
            align-items: center;
        }
        .info-label i {
            margin-right: 10px;
            color: #4e73df;
            width: 20px;
            text-align: center;
        }
        .info-value {
            font-size: 1.1rem;
            color: #495057;
            font-weight: 500;
        }
        .status-badge {
            padding: 8px 16px;
            border-radius: 50px;
            font-weight: 600;
            font-size: 0.9rem;
        }
        .btn-group-custom {
            margin-top: 40px;
            display: flex;
            justify-content: center;
            gap: 15px;
        }
        .btn-custom {
            padding: 12px 30px;
            font-weight: 600;
            border-radius: 8px;
            transition: all 0.3s;
        }
    </style>
</head>
<body>
<!-- Navigation -->
<%@ include file="/WEB-INF/views/common/menu.jsp" %>

<div class="main-content">
    <div class="detail-card">
        <div class="detail-header d-flex justify-content-between align-items-center">
            <h2><i class="fas fa-file-invoice-dollar mr-2"></i>요금 부과 정책 상세</h2>
            <% if (policy.isActive()) { %>
            <span class="status-badge badge-primary"><i class="fas fa-check-circle mr-1"></i>현재 적용 중</span>
            <% } else { %>
            <span class="status-badge badge-secondary"><i class="fas fa-history mr-1"></i>미사용 정책</span>
            <% } %>
        </div>

        <div class="info-group">
            <div class="info-item">
                <div class="info-label"><i class="fas fa-clock"></i>무료 회차</div>
                <div class="info-value"><%=policy.getGracePeriod()%>분</div>
            </div>
            <div class="info-item">
                <div class="info-label"><i class="fas fa-hourglass-start"></i>기본 시간/요금</div>
                <div class="info-value"><%=policy.getDefaultTime()%>분 / <span class="text-primary"><%=String.format("%,d", policy.getDefaultFee())%>원</span></div>
            </div>
            <div class="info-item">
                <div class="info-label"><i class="fas fa-plus-circle"></i>추가 시간/요금</div>
                <div class="info-value"><%=policy.getExtraTime()%>분 / <span class="text-primary"><%=String.format("%,d", policy.getExtraFee())%>원</span></div>
            </div>
            <div class="info-item">
                <div class="info-label"><i class="fas fa-calendar-check"></i>월정액 요금</div>
                <div class="info-value"><%=String.format("%,d", policy.getSubscribedFee())%>원</div>
            </div>
            <div class="info-item">
                <div class="info-label"><i class="fas fa-coins"></i>일일 최대 요금</div>
                <div class="info-value text-danger"><%=String.format("%,d", policy.getMaxDailyFee())%>원</div>
            </div>
            <div class="info-item">
                <div class="info-label"><i class="fas fa-car-side"></i>경차 할인율</div>
                <div class="info-value"><%=policy.getLightDiscount() * 100%>%</div>
            </div>
            <div class="info-item">
                <div class="info-label"><i class="fas fa-wheelchair"></i>장애인 할인율</div>
                <div class="info-value"><%=policy.getDisabledDiscount() * 100%>%</div>
            </div>
            <div class="info-item">
                <div class="info-label"><i class="fas fa-info-circle"></i>정책 상태</div>
                <div class="info-value"><%= policy.isActive() ? "사용 중" : "사용 가능" %></div>
            </div>
        </div>

        <div class="btn-group-custom">
            <% if (!policy.isActive()) { %>
            <button type="button" class="btn btn-success btn-custom"
                    onclick="if(confirm('이 정책을 현재 주차 요금 정책으로 즉시 적용하시겠습니까?'))
                            location.href='${pageContext.request.contextPath}/view/policy/apply?id=<%= policy.getPolicyId()%>&pageNum=<%=pageNum%>&items=<%=items%>&keyword=<%=keyword%>'">
                <i class="fas fa-play mr-1"></i> 정책 적용하기
            </button>
            <% } %>

            <button type="button" class="btn btn-outline-primary btn-custom"
                    onclick="location.href='${pageContext.request.contextPath}/view/policy/list?pageNum=<%=pageNum%>&items=<%=items%>&keyword=<%=keyword%>'">
                <i class="fas fa-list mr-1"></i> 목록으로
            </button>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/JS/menu.js"></script>
<script src="${pageContext.request.contextPath}/JS/function.js"></script>
</body>
</html>
