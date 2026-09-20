// JSP가 스크립트를 직접 생성하지 않고 안내 내용과 이동 방식만 data-* 속성으로 전달한다.
const pageData = document.body.dataset;

window.alert(pageData.alertMessage);

if (pageData.alertAction === 'redirect' && pageData.alertUrl) {
    window.location.href = pageData.alertUrl;
} else {
    window.history.back();
}
