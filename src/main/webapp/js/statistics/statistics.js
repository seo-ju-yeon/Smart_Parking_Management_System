// 외부 JavaScript에서는 JSP 표현식을 직접 사용할 수 없어 body의 data-* 속성에서 경로를 읽는다.
const contextPath = document.body.dataset.contextPath;

// 서버가 숨김 DOM에 기록한 차트 데이터를 숫자 배열로 변환한다.
function readSeries(seriesName) {
    return Array.from(
        document.querySelectorAll('[data-series="' + seriesName + '"] [data-value]')
    ).map(item => ({
        label: item.dataset.label,
        value: Number(item.dataset.value)
    }));
}

const hourlySalesSeries = readSeries('hourly-sales');
const hourlyCountSeries = readSeries('hourly-counts');
const dailySalesSeries = readSeries('daily-sales');
const carTypeSeries = readSeries('car-types');

const hourlyLabels = hourlySalesSeries.map(item => item.label);
const hourlySalesData = hourlySalesSeries.map(item => item.value);
const hourlyCountData = hourlyCountSeries.map(item => item.value);
const dailyLabels = dailySalesSeries.map(item => item.label.substring(8) + '일');
const dailyData = dailySalesSeries.map(item => item.value);
const carLabels = carTypeSeries.map(item => item.label);
const carData = carTypeSeries.map(item => item.value);

window.onload = function() {
    renderHourlyChart();
    renderDailyChart();
    renderCarTypeChart();
};

// [차트 1] 시간대별 매출(선) + 입차량(막대) 복합 차트
function renderHourlyChart() {
    const ctx = document.getElementById('hourlyChart').getContext('2d');
    new Chart(ctx, {
        data: {
            labels: hourlyLabels,
            datasets: [
                {
                    type: 'line',
                    label: '매출액 (원)',
                    data: hourlySalesData,
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    yAxisID: 'y-sales',
                    tension: 0.4,
                    fill: true
                },
                {
                    type: 'bar',
                    label: '입차량 (대)',
                    data: hourlyCountData,
                    backgroundColor: '#3b82f6',
                    yAxisID: 'y-counts',
                    borderRadius: 5
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                'y-sales': {
                    type: 'linear',
                    position: 'right',
                    title: { display: true, text: '원' },
                    beginAtZero: true,      // 0부터 시작
                    suggestedMin: 0,        // 데이터가 없어도 최소 0
                    suggestedMax: 100000    // 데이터가 없어도 y축이 최소 100,000원까지는 보이도록 설정
                },
                'y-counts': {
                    type: 'linear',
                    position: 'left',
                    title: { display: true, text: '대' },
                    beginAtZero: true,
                    suggestedMin: 0,
                    suggestedMax: 10        // 데이터가 없어도 y축이 최소 10대까지는 보이도록 설정
                }
            }
        }
    });
}

// [차트 2] 일별 매출 차트 (막대)
function renderDailyChart() {
    const ctx = document.getElementById('dailySalesChart').getContext('2d');
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: dailyLabels,
            datasets: [{
                label: '일 매출액',
                data: dailyData,
                backgroundColor: '#10b981',
                borderRadius: 4
            }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

// [차트 3] 차종별 비중 (도넛)
function renderCarTypeChart() {
    const ctx = document.getElementById('carTypeChart').getContext('2d');
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: carLabels,
            datasets: [{
                data: carData,
                backgroundColor: ['#f59e0b', '#6366f1', '#ec4899', '#8b5cf6', '#94a3b8']
            }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

// 캘린더에서 날짜 선택 시 날짜 변경
function changeStatisticsDate(input) {
    const form = input.form;
    // 날짜를 바꿀 때만 잠시 목적지를 '통계'로 변경해서 전송
    form.action = contextPath + '/statistics/statistics';
    form.submit();
}

document.getElementById('statisticsDate').addEventListener('change', function () {
    changeStatisticsDate(this);
});
