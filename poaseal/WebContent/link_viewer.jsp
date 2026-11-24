<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="sepoa.fw.cache.Cache"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="sepoa.fw.srv.*" %>
<%@page import="sepoa.fw.ses.*" %>
<%@ page import="sepoa.fw.util.*" %>
<%@ page import="sepoa.fw.log.Logger"%>
<%@ page import="sepoa.fw.cfg.*"%>
<%@page import="sepoa.fw.version.SepoaVersion" %>
<%@page import="sepoa.fw.ux.design.DynamicCSS" %>
<%@page import="sepoa.fw.ux.*" %>
<%@ page import="java.util.*"%>
<%@ page import="java.util.Map.Entry"%>
<%@ page import="javax.servlet.RequestDispatcher"%>
<%
	//String CONTEXT_NAME = request.getContextPath();
	SepoaInfo info = SepoaSession.getAllValue(request);
	
	String domain = request.getServerName();
	String houseCode = CommonUtil.getConfig(domain+".house_code");
	Config conf = new Configuration(houseCode);
	SepoaOut so = null;
	
	String redirectUrl = "/link.sux";
	String qs = request.getQueryString();
	redirectUrl = redirectUrl + "?" + qs + "&LINK=QR";
	
	
	HashMap<String, String> headerData = new HashMap<String, String>();
	HashMap<String, Object> allData = new HashMap<String, Object>();
	allData.put(SepoaDataMapper.KEY_HEADER_DATA, headerData);
	Object[] obj = {allData};
	
	String[] qsArr = qs.split("&");
	String linkCode = null;
	for (String qsKv : qsArr) {
		String[] qsKvArr = qsKv.split("=");
		if (qsKvArr.length != 2) continue;
		String key = qsKvArr[0];
		if ("ln".equals(key)) {
			linkCode = qsKvArr[1];
			break;
		}
	}
	headerData.put("LINK_CODE", linkCode);
	headerData.put("ACCESS_URL", request.getRequestURL().toString()+"?"+qs);
	String ip = request.getHeader("X-Forwarded-For");
    if (ip == null) ip = request.getHeader("Proxy-Client-IP");
    if (ip == null) ip = request.getHeader("WL-Proxy-Client-IP");
    if (ip == null) ip = request.getHeader("HTTP_CLIENT_IP");
    if (ip == null) ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    if (ip == null) ip = request.getRemoteAddr();
    headerData.put("ACCESS_IP", ip);
	
	so = ServiceConnector.doService(SepoaSession.getAllValue(request), "sepoa.svc.seal.SepoaReqSealService", "TRANSACTION", "addQR_accessLog", obj);
	if(so.flag){
		request.getRequestDispatcher(redirectUrl).forward(request, response);
	} else {
		out.println("QR 접속 로그 저장을 실패했습니다 : "+ so.message);
	}

%>
<!DOCTYPE html>
<html>
<head>
<script>
</script>
<style>
</style>
</head>
<body>
</body>
</html>
