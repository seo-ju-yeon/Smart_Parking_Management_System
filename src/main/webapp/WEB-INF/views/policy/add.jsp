<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>요금 부과 정책 등록</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/style.css">

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
        .form-card {
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.08);
            padding: 50px;
            margin-top: 30px;
            margin-bottom: 50px;

            width: 100%;
            max-width: 1050px;
            margin-left: auto;
            margin-right: auto;
        }
        .form-header {
            border-bottom: 2px solid #f8f9fa;
            margin-bottom: 30px;
            padding-bottom: 20px;
        }
        .form-header h2 {
            font-weight: 700;
            color: #333;
            margin: 0;
        }
        .section-title {
            font-size: 1.1rem;
            font-weight: 700;
            color: #4e73df;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
        }
        .section-title i {
            margin-right: 10px;
        }
        .col-form-label {
            font-weight: 600;
            color: #555;
            display: flex;
            align-items: center;
            padding-right: 20px;
        }
        .form-control {
            border-radius: 8px;
            border: 1px solid #d1d3e2;
            padding: 10px 15px;
            transition: all 0.2s;
        }
        .form-control:focus {
            border-color: #4e73df;
            box-shadow: 0 0 0 0.2rem rgba(78, 115, 223, 0.25);
        }
        .input-group-text {
            background-color: #f8f9fa;
            border-radius: 0 8px 8px 0 !important;
        }
        .btn-custom {
            padding: 12px 35px;
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
<script>
    // 유효성 검사
    function registerMember() {
        if(confirm("이 설정으로 새로운 요금 정책을 등록하시겠습니까?")) {
            return true;
        }
        return false;
    }
</script>
</body>
</html>
