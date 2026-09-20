<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>요금 부과 정책 등록</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/policy/add.css">
</head>
<body>
<!-- Navigation -->
<%@ include file="/WEB-INF/views/common/menu.jsp" %>
<div class="main-content">
    <div class="row">
        <div class="col-lg-9 mx-auto">
            <div class="form-card">
                <div class="form-header text-center">
                    <h2><i class="fas fa-plus-circle text-primary mr-2"></i>신규 요금 정책 등록</h2>
                    <p class="text-muted mt-2">새로운 주차 요금 부과 기준을 설정합니다.</p>
                </div>

                <form name="frmFeePolicy" action="${pageContext.request.contextPath}/view/policy/add" method="post">

                    <div class="section-title">
                        <i class="fas fa-stopwatch"></i> 기본 및 추가 설정
                    </div>
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="col-form-label">무료 회차(분)</label>
                            <div class="input-group">
                                <input type="number" name="gracePeriod" class="form-control" value="10" min="0" required>
                                <div class="input-group-append"><span class="input-group-text">분</span></div>
                            </div>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="col-form-label">일일 최대 요금</label>
                            <div class="input-group">
                                <input type="number" name="maxDailyFee" class="form-control" value="50000" min="0" required>
                                <div class="input-group-append"><span class="input-group-text">원</span></div>
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="col-form-label">기본 시간</label>
                            <div class="input-group">
                                <input type="number" name="defaultTime" class="form-control" value="30" min="1" required>
                                <div class="input-group-append"><span class="input-group-text">분</span></div>
                            </div>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="col-form-label">기본 요금</label>
                            <div class="input-group">
                                <input type="number" name="defaultFee" class="form-control" value="2000" min="0" required>
                                <div class="input-group-append"><span class="input-group-text">원</span></div>
                            </div>
                        </div>
                    </div>

                    <div class="row mb-4">
                        <div class="col-md-6 mb-3">
                            <label class="col-form-label">추가 시간</label>
                            <div class="input-group">
                                <input type="number" name="extraTime" class="form-control" value="30" min="1" required>
                                <div class="input-group-append"><span class="input-group-text">분</span></div>
                            </div>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="col-form-label">추가 요금</label>
                            <div class="input-group">
                                <input type="number" name="extraFee" class="form-control" value="1000" min="0" required>
                                <div class="input-group-append"><span class="input-group-text">원</span></div>
                            </div>
                        </div>
                    </div>

                    <hr>

                    <div class="section-title mt-4">
                        <i class="fas fa-percentage"></i> 특별 요금 및 할인율
                    </div>
                    <div class="form-group row mb-3">
                        <label class="col-sm-4 col-form-label">월정액 요금</label>
                        <div class="col-sm-8">
                            <div class="input-group">
                                <input type="number" name="subscribedFee" class="form-control" value="15000" min="0" required>
                                <div class="input-group-append"><span class="input-group-text">원</span></div>
                            </div>
                        </div>
                    </div>

                    <div class="form-group row mb-3">
                        <label class="col-sm-4 col-form-label">경차 할인율</label>
                        <div class="col-sm-8">
                            <div class="input-group">
                                <input type="number" name="lightDiscount" class="form-control" value="30" min="0" max="100" required>
                                <div class="input-group-append"><span class="input-group-text">%</span></div>
                            </div>
                            <small class="text-muted"><i class="fas fa-info-circle mr-1"></i>0~100 사이의 숫자를 입력하세요.</small>
                        </div>
                    </div>

                    <div class="form-group row mb-4">
                        <label class="col-sm-4 col-form-label">장애인 할인율</label>
                        <div class="col-sm-8">
                            <div class="input-group">
                                <input type="number" name="disabledDiscount" class="form-control" value="50" min="0" max="100" required>
                                <div class="input-group-append"><span class="input-group-text">%</span></div>
                            </div>
                            <small class="text-muted"><i class="fas fa-info-circle mr-1"></i>0~100 사이의 숫자를 입력하세요.</small>
                        </div>
                    </div>

                    <input type="hidden" name="isActive" value="true">

                    <div class="text-center mt-5">
                        <button type="submit" class="btn btn-primary btn-custom shadow-sm mr-2">
                            <i class="fas fa-save mr-1"></i> 정책 저장하기
                        </button>
                        <button type="button" class="btn btn-outline-secondary btn-custom"
                                onclick="location.href='${pageContext.request.contextPath}/view/policy/list'">
                            <i class="fas fa-times mr-1"></i> 취소
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/policy/add.js"></script>
</body>
</html>
