<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="sepoa.fw.ses.SepoaSession" %>
<%@ page import="sepoa.fw.util.JSPUtil" %>
<%@ page import="sepoa.fw.cfg.*" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="sepoa.fw.cache.Cache" %>
<%@ page import="sepoa.fw.util.*" %>
<%@ page import="sepoa.fw.srv.*" %>
<%@ page import="com.google.gson.Gson" %>
<% response.setHeader("X-UA-Compatible", "IE=Edge"); %>
<!DOCTYPE HTML>
<%
	String AssetVersion = ""+System.currentTimeMillis();
	request.getSession().setAttribute("AssetVersion", AssetVersion);
	String CONTEXT_NAME = request.getContextPath();
	sepoa.fw.ses.SepoaInfo info = sepoa.fw.ses.SepoaSession.getAllValue(request);
	Map<String, String> params = new HashMap<String, String>();
	Enumeration<String> paramKeys = request.getParameterNames();
	while(paramKeys.hasMoreElements()){
	    String key  = paramKeys.nextElement();
	    params.put(key, request.getParameter(key));
	}

	HashMap<String, Object> allData = new HashMap<String, Object>();
	HashMap<String, String> headerData = new HashMap<String, String>();
	allData.put(SepoaDataMapper.KEY_HEADER_DATA, headerData);
	String houseCode = params.get ( "house_code" );
    String language = params.get ("LANGUAGE");
	headerData.put("HOUSE_CODE", houseCode);
	
	if(houseCode != null && !houseCode.isEmpty()){
        SepoaSession.putValue(request, "HOUSE_CODE", houseCode);
        info = SepoaSession.getAllValue(request);
    }
    
    //regist language
    if(language == null || language.isEmpty()){
        SepoaSession.putValue(request, "LANGUAGE", "KO");
        info = SepoaSession.getAllValue(request);
    }

    SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.admin.AD_360", "CONNECTION", "getSettings", new Object[]{});
    Map<String, String> pageData = (Map<String, String>)so.getData(0);
%>
<html>
<head>
	<title>EasySeal</title>
	<meta charset="utf-8" />
	<script src="<%=CONTEXT_NAME%>/resource/plugin/jquery/jquery-1.12.4.min.js?<%=AssetVersion%>"></script>
	<meta name="viewport" content="width=1024, maximum-scale=3.0" />

	<link rel="stylesheet" href="<%=CONTEXT_NAME%>/resource/fonts/NotoSansKR/NotoSansKR.css?<%=AssetVersion%>">
	<link rel="stylesheet" href="css/login.css?<%=AssetVersion%>">
	
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.polyfill.js?v=<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.utils.js?v=<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.default.js?v=<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/dhtmlx.js?<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxVault_v25_pro/codebase/dhtmlxvault.js?<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/window.js?v=<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/010/form/default/js/login.js?<%=AssetVersion%>"></script>
	<%-- <script type="text/javascript">Sepoa.cache.text.load([{url:CONTEXT_NAME+'/resource/ux/admin/langs/'+(Sepoa.request.LANGAUGE || 'KO')+'/POA_BASE.js'}]);</script> --%>
	
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/fonts/font_roboto/roboto.css?<%=AssetVersion%>" />
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/skins/web/dhtmlx.css?<%=AssetVersion%>" />
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/skins/material/dhtmlx.css?<%=AssetVersion%>" />
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/dhtmlx.css?<%=AssetVersion%>" />
	<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/window.css?v=<%=AssetVersion%>"/>
	
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/sepoa_scripts.js?<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/common.js?<%=AssetVersion%>"></script>
	
	<%--<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/window.js?<%=AssetVersion%>"></script>
	<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/layout.js?<%=AssetVersion%>"></script> --%>
	
	<script type="text/javascript">
		var $var = {};
	
		function userIdKeyDown(event) {
		    if (event.keyCode == 13) {
		        document.getElementById("password").focus();
		    }
		}

		function passwdKeyDown(event) {
		    if (event.keyCode == 13) {
		        check_user();
		    }
		}
		
		var $message;
		function check_user() {
			if (document.getElementById("user_id").value == "") {
	        	Alert('아이디를 입력해주세요.');
	            $message._show('failure', 'ID를 입력해주세요.');
	            return;
	        }

	        if (document.getElementById("password").value == "") {
	        	Alert('비밀번호를 입력해주세요.');
	            $message._show('failure', 'PassWord를 입력해주세요.');
	            return;
	        }
	        
	        $var.module.config.sendParams = {};
	        $var.module.config.sendParams['ID'] = document.getElementById("user_id").value;
	        $var.module.config.sendParams['PASSWORD'] = document.getElementById("password").value;
			$var.module.login.send();
		}
		
		function getLanguage() {
			/*
		    var language = navigator.systemLanguage;
		    // NON IE
		    if (!language) {
		        language = navigator.language;
		    }
		    language = language.toUpperCase().substring(0, 2);

		    return language;
		    */
		    return 'KO';
		}
		
		
		function inquiryClearAll(){
			$("#email").val("");
			$("#phone").val("");
			$("#accounting").val("");
			$("#account").val("");
			$("#company_name").val("");
			$("#inquiry_content").val("");
		}
		
		function validEmail(){
			 var emailVal = $("#email").val();

			  var regExp = /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$/i;
			  // 검증에 사용할 정규식 변수 regExp에 저장

			  if (emailVal.match(regExp) != null) {
			    return true;
			  }
			  else {
			    
			    return false;
			  }
		}
		
		function find() {
			$var.module.find();
		}
		
		function find_user(type){
			
			var name = document.getElementById("find_user_name").value;
			var email = document.getElementById("find_user_email").value;

			if(name == ""){
				Alert("이름을 입력해 주세요.");
				return;
			}else if(email == ""){
				Alert("이메일을 입력해 주세요.");
				return;
			}
			
			doServiceAjax("CO_004", "CONNECTION", "findUserInfo", {user_name:name, user_email:email, type:type},function(SepoaOut){
		        if (SepoaOut.status == '1') {
		        	if(type == "ID"){
		        		Alert('아이디가 입력하신 메일로 전송되었습니다.');	
		        	}else if(type == "PWD"){
		        		Alert('임시비밀번호가 입력하신 메일로 전송되었습니다.');
		        	}
		        }else{
		        	Alert(SepoaOut.message);
		        }
	        }, false);
		}
		
		function saveCheck(flag){
			window.localStorage.autoLogin = flag;
			if(eval(window.localStorage.autoLogin)){
				window.localStorage.loginId = document.getElementById("user_id").value;
			}else{
				window.localStorage.loginId = "";
			}
		} 
		
		function init(){
			if(eval(window.localStorage.autoLogin) && document.getElementById("user_id")){
				document.getElementById("user_id").value = window.localStorage.loginId;
				document.getElementById("autoLogin").checked = window.localStorage.autoLogin;
			}
			
			if(document.getElementById("user_id") && document.getElementById("user_id").value != "" && document.getElementById("user_id").value != null){
				document.getElementById("password").focus();
			}
			
			var clientDesignObj = <%=new Gson().toJson(pageData)%>;
            $var.module = new SepoaIndex({
                mode:Sepoa.request.login || "login",
                form:clientDesignObj.LOGIN_DESIGN_TYPE || "default",
                style:clientDesignObj,
                service:Sepoa.request.service || "login",
                isPopup:(Sepoa.request.isPopup == 'true'),
                sendParams:(Sepoa.params)?Sepoa.params.params:null,
            });
            
            $var.module.attachEvent("onLoginAfter", onLoginAfter);
            if(Sepoa.params && Sepoa.params.callback){
                indexObj.attachEvent("onLoginAfter", Sepoa.params.callback);
            }
            
            $var.module.attachEvent("onLoginEnd", function(flag){
                if(flag){
                    if(Sepoa.request.isPopup != 'true'){
                        location.href = window.CONTEXT_NAME + "/Main.sux";
                    }else{
                        self.close();
                    }
                } else {
                	
                }
            });
		
			$(window).scroll(function(event){
				if($(this).scrollTop() > 30){
					$(".header").css("background-color","#fff");
					$(".header").css("opacity","0.8");	
					$(".otherService").css("opacity","0.8");	
				} else {
					$(".header").css("background-color","");
					$(".header").css("opacity","");		
					$(".otherService").css("opacity","1");				
				}
			});
		}
		
		function goURL(type){
			var action = "";
			var form = document.createElement("form");
			form.setAttribute("method","post");
			form.setAttribute("target","_self");
			if(type=="index"){
				action = "/";
			}else{
				action = type + ".jsp";
				$('.otherService').css('display','none');
				form.setAttribute("target","_self");
			}
			
			form.setAttribute("action",action);
			document.body.appendChild(form);
			form.submit();
			document.body.removeChild(form);
		}
		
		function viewOtherSerivce(){
			$('.otherService').css('display','');
		}
		
		function onLoginAfter(so, share) {
			switch(so.status){
            case "1":
                share.message = sUtils.cache.text.get("LOGIN.STATUS_"+so.status);
                return true;
            case "100016":
                if(so.param.TYPE == 'POPUP'){
                    sUtils.popup.modal({
                        url:window.CONTEXT_NAME+so.param.URL,
                        params:so.param,
                        title:so.param.TITLE
                    });
                }else{
                    share.message = sUtils.cache.text.get("LOGIN.STATUS_"+so.status);
                }
                break;
            case "100011":
            case "100012":
            case "100013":
            case "100014":
                sUtils.popup.modal({
                    url:window.CONTEXT_NAME+"/Admin/UpdateUserInfo/AdminUpdateUserInfoPassWord.sux",
                    params:{
                        ID:SepoaOut.param.ID, 
                        mode:"PWROT"
                    },
                    width:400,
                    height:450,
                    title:sUtils.cache.text.get("LOGIN.STATUS_"+so.status)
                })
                break;
            default:
                share.message = sUtils.cache.text.get("LOGIN.STATUS_"+so.status);
            }
            return false;
		}
	</script>
</head>

<body class="index is-preload" onload="init();">

	<form id="ADDMEMBER" action="/theme/main/addMember.jsp" method="post" style="display: none;">
		<div style="width: 50px;height: 40px;display: none;"></div>
		<input type="hidden" name="house_code" value="<%=headerData.get("HOUSE_CODE")%>">
	</form>
	<input type="hidden" name="demo_flag" id="demo_flag" value=""/>
	<div class="centerDiv">

		<!-- 아이템 1번 시작 -->
		<div class="layout-item login01">
			<div class="main noselect">
				<div class="loginTxt1" onclick="goURL('index')">
					<span class="loginTxt-highlight loginTxt-highlight-blue">Easy</span><span class="loginTxt-highlight">Seal</span> <span>전자인장관리</span>
				</div>
				<div class="loginTxt2">인장관리 원스톱 솔루션</div>
				<div>
					<input type="text" name="user_id" id="user_id" placeholder="아이디" onkeydown="userIdKeyDown(event)"/>
				</div>
				<div>
					<input type="password" name="password" id="password" placeholder="비밀번호" onkeydown="passwdKeyDown(event)"/>
				</div>
				<span id="login_message"></span>
				<div>
					<div class="btn1" onclick="check_user()">로그인</div>
				</div>
				<div class="lineDiv"></div>
				
				<div class="login-help-area">
					<div class="button password-find" onclick="find()">비밀번호가 생각나지 않으세요? 비밀번호 찾기</div>
					<!-- <div class="button signup"  onclick="Alert('준비중 입니다.')">계정이 없으신가요? 회원가입</div> -->
				</div>
				
				<div class="lineDiv"></div>
				
				<div class="footerText">
					<div>시스템 도입 문의 : sales@sepoasoft.co.kr ❘  070-7878-9846</div>
					<div>시스템 지원 문의 : help@sepoasoft.co.kr ❘  02-577-9111 </div>
					<div class="footer-highlight">© 2022. Sepoasoft Co,. All rights reserved.</div>
					<!-- <div class="footer-button">
			    		<div class="button policy" onclick="Alert('준비중 입니다.')">이용약관</div>
						&nbsp&nbsp|&nbsp&nbsp
			    		<div class="button privacy" onclick="Alert('준비중 입니다.')">개인정보처리방침</div>
						&nbsp&nbsp|&nbsp&nbsp
			    		<div class="button support" onclick="Alert('준비중 입니다.')">고객센터</div>
		    		</div>-->
				</div>
			</div>
		</div>
	</div>
</body>
</html>