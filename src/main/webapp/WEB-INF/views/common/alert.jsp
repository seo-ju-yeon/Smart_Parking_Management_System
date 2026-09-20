<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String pageAlertMessage = (String) request.getAttribute("pageAlertMessage");
    String pageAlertAction = (String) request.getAttribute("pageAlertAction");
    String pageAlertUrl = (String) request.getAttribute("pageAlertUrl");

    if (pageAlertMessage == null) pageAlertMessage = "요청을 처리할 수 없습니다.";
    if (pageAlertAction == null) pageAlertAction = "back";
    if (pageAlertUrl == null) pageAlertUrl = "";
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>요청 처리 안내</title>
</head>
<body data-alert-message="<%= pageAlertMessage %>"
      data-alert-action="<%= pageAlertAction %>"
      data-alert-url="<%= pageAlertUrl %>">
<script src="${pageContext.request.contextPath}/js/common/page-alert.js"></script>
</body>
</html>
