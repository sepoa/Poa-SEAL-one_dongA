<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.net.URLEncoder"%>
<%
	String context = request.getContextPath();
	String houseCode = request.getParameter("house_code");
	String query = request.getQueryString();

	String target = context + "/entraIdSSO";
	if (query != null && !query.trim().isEmpty()) {
		target += "?" + query;
	} else if (houseCode != null && !houseCode.trim().isEmpty()) {
		target += "?house_code=" + URLEncoder.encode(houseCode, "UTF-8");
	}

	response.sendRedirect(target);
%>
