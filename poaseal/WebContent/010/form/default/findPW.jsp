<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="sepoa.fw.ses.SepoaSession" %>
<%@ page import="sepoa.fw.util.JSPUtil" %>
<%@ page import="sepoa.fw.cfg.*" %>
<%@ page import="sepoa.fw.cache.Cache" %>
<%@ page import="sepoa.fw.util.*" %>
<%@ page import="sepoa.fw.srv.*" %>
<!DOCTYPE HTML>
<%
	String AssetVersion = ""+System.currentTimeMillis();
	String CONTEXT_NAME = request.getContextPath();
%>
<html>
<head>
	<title>Find Password</title>
	<meta charset="utf-8" />
	<script src="<%=CONTEXT_NAME%>/resource/plugin/jquery/jquery-1.12.4.min.js?<%=AssetVersion%>"></script>
	
	<link rel="stylesheet" type="text/css" href="css/findPW.css?<%=AssetVersion%>" />
	
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.polyfill.js?v=<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.utils.js?v=<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.default.js?v=<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/dhtmlx.js?<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxVault_v25_pro/codebase/dhtmlxvault.js?<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/window.js?v=<%=AssetVersion%>"></script>
	
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/fonts/font_roboto/roboto.css?<%=AssetVersion%>" />
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/skins/web/dhtmlx.css?<%=AssetVersion%>" />
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/skins/material/dhtmlx.css?<%=AssetVersion%>" />
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/dhtmlx.css?<%=AssetVersion%>" />
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/window.css?v=<%=AssetVersion%>"/>
	
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/sepoa_scripts.js?<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/common.js?<%=AssetVersion%>"></script>
	
	<script type="text/javascript">
		function doUpdate() {
			var userId = document.getElementById("user_id").value;
			var email = document.getElementById("email").value;
	        if (userId.length < 1) {
	            Alert(Sepoa.cache.text.get('FINDPW.INSERT_ID'));
	            return false;
	        } else if (email.length < 1) {
	        	Alert(Sepoa.cache.text.get('FINDPW.INSERT_EMAIL'));
	            return false;
	        } else {
	        	dataOutput.setValue('USER_ID', userId);
	        	dataOutput.setValue('EMAIL', email);
	            doServiceAjax('sepoa.svc.common.FindPw', 'TRANSACTION', 'doSendPWResetLink', null, function(so) {
	                if (so.status != 1) {
	                    Alert(so.message);
	                    return;
	                } else {
	                    Alert(so.message);
	                    window.selfObj.close();
	                }
	            });
	        }
		}
		
		function userIdKeyDown(event) {
		    if (event.keyCode == 13) {
		        document.getElementById("email").focus();
		    }
		}

		function emailKeyDown(event) {
		    if (event.keyCode == 13) {
		        check_user();
		    }
		}
	</script>
</head>
<body>
	<div class="findPW">
		<div class="main">
			<div class="input-id">
				<input type="text" name="user_id" id="user_id" placeholder="아이디를 입력해주세요." onkeydown="userIdKeyDown(event)"/>
			</div>
			<div class="input-email">
				<input type="text" name="email" id="email" placeholder="이메일을 입력해주세요." onkeydown="emailKeyDown(event)"/>
			</div>
			<div class="btn1 btn-confirm" onclick="doUpdate()">확인</div>
		</div>
	</div>
</body>
</html>