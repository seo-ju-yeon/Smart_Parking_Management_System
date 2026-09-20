const contextPath = document.body.dataset.contextPath;
const forgotPasswordButton = document.getElementById('forgotPasswordBtn');

forgotPasswordButton.addEventListener('click', function () {
    window.location.href = contextPath + '/forgot-password';
});
