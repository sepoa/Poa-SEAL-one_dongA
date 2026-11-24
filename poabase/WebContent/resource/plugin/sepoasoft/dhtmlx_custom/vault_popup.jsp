<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%@ page import="com.fasterxml.jackson.databind.ObjectMapper" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="sepoa.fw.util.*" %>
<%
	Map<String, String> params = new HashMap<String, String>();
	Enumeration<String> paramKeys = request.getParameterNames();
	while(paramKeys.hasMoreElements()){
	    String key  = paramKeys.nextElement();
	    params.put(key, request.getParameter(key));
	}
	
	ObjectMapper jsonMapper = new ObjectMapper();
    String requestString = jsonMapper.writeValueAsString(params).replaceAll("\'", "\\\\'");
    
    String theme = CommonUtil.getConfig("sepoa.sux.theme", "basic");
    String CONTEXT_NAME = request.getContextPath();
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>FileAttach</title>
<!-- [if lte IE 9]>
	<script src="/js/json2.js"></script>
<![endif] -->
<script>
	window.request=<%= requestString %>
	window.CONTEXT_NAME = "<%=CONTEXT_NAME%>"
</script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/jquery/jquery-1.12.4.min.js"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/dhtmlx.js"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxVault_v25_pro/codebase/dhtmlxvault.js"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/sepoa_scripts.js"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/vault.js"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/common.js"></script>
<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxVault_v25_pro/codebase/dhtmlxvault.css" />
<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/theme/<%=theme%>/sepoabase/vault.css" />
<script>
Sepoa.common.SCREEN_ID = "VAULT";
Sepoa.request = <%= requestString %>;
var myVault;
function init() {
	var moduleTop = SUX.root;
	var info = moduleTop.modalPopupInfo[Sepoa.request.parameterId];
	Sepoa.params = info.params;
	window.selfObj = info.selfObj;
	moduleTop.modalPopupInfo[Sepoa.request.windowParameterId] = null;
	delete moduleTop.modalPopupInfo[Sepoa.request.windowParameterId];
	
	document.getElementById('attachView').style.height = document.documentElement.clientHeight + 'px';
	
	if(myVault == null ){
		var options = $.extend(true, {}, request);
		options.container="attachView";
		options.callback=attach_callback;
		
		myVault = new SepoaDhtmlXVaultObject(options);
		myVault.setProgressMode("eta");
		myVault.setHeight(document.body.offsetHeight);
		
		if(request.limit){
			myVault.setFilesLimit(request.limit); 
		}
		
	}
}

function attach_callback(e, a){
	if(Sepoa.params.callback)
		Sepoa.params.callback(e, a, request);
}
</script>
</head>
<body style="margin:0px;">
<div id="attachView" style="width:100%;">
</div>
</body>
</html>