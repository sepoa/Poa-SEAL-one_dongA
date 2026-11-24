<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="sepoa.fw.cache.Cache"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="sepoa.fw.ses.SepoaSession"%>
<%@ page import="sepoa.fw.ses.SepoaInfo"%>
<%@ page import="sepoa.fw.util.JSPUtil"%>
<%@ page import="sepoa.fw.log.Logger"%>
<%@ page import="sepoa.fw.cfg.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.util.Map.Entry"%>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="javax.servlet.RequestDispatcher"%>
<%
	SepoaInfo info = SepoaSession.getAllValue(request);

	String houseCode = request.getParameter("house_code");
	String sHouseCode = info.getSession("HOUSE_CODE");
	Config conf = new Configuration("000");
	String CONTEXT_NAME = request.getContextPath();
	
	if(houseCode == null && (sHouseCode == null || "000".equals(sHouseCode)) && conf.getBoolean("sepoa.server.development.flag", true)){
		Set<String> matchedKeys = conf.getMatchedKey(".*.house_code");
		
		final TreeMap<String, String> enabledCode = new TreeMap<String, String>();
		for(String propKey : matchedKeys){
			String v = conf.get(propKey);
			if(v.matches("[0-9]{3}")){
				enabledCode.put(v, propKey.replace(".house_code", ""));
			}
		}
		
		if(enabledCode.size() <= 0){
			out.println("<script>");
			out.println("location.href = '?house_code="+new Configuration("000").get(request.getServerName() + ".house_code", "000")+"'");
			out.println("</script>");
		}else if(enabledCode.size() == 1){
			out.println("<script>");
			out.println("location.href = '?house_code="+enabledCode.keySet().toArray(new String[]{})[0]+"'");
			out.println("</script>");
		}else{
%>
<!DOCTYPE html>
<html>
<head>
<script>
	function doMove(){
		var selector = document.getElementById("houseCodeSelect");
		movePage(selector.options[selector.selectedIndex].value);
	}
	
	function init(){
		if(document.getElementById("houseCodeSelect").options.length <= 1){
			movePage("000");
		}
	}
	
	function movePage(hc){
		location.href = "?house_code="+hc;
	}
</script>
<link href="<%=CONTEXT_NAME%>/resource/plugin/XEIcon/xeicon.min.css" rel="stylesheet" type="text/css" >
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/XEIcon_custom/sepoa.xeicon.css" rel="stylesheet" type="text/css" >
<style>
	html, body{
		height: 100%;
		overflow: hidden;
		font-family : Noto Sans KR;
	}
	
	.sepoa_index_body{
		margin: 0;
	}

	.sepoa_index_bg_area{
		background-image : url("./resource/design/imgs/main/bg_img.png");
		height: 100%;
		width: 100%;
		display: flex;
	    justify-content: center;
	}
	
	.sepoa_index_form_wrapper{
		height: 100%;
		width: 400px;
		display: flex;
  		flex-direction: column;
	    justify-content: center;
	    align-items: center;
	}
	
	.sepoa_client_select_text_area{
		width: 100%;
		margin-bottom: 10px;
	}
	
	.sepoa_client_select_text{
		font-size: 16pt;
	    font-family: 'Noto Sans KR';
	    font-weight: 800;
	}
	
	.sepoa_houseCode_select{
		width: 280px;
		height: 40px;
	    margin-right: 5px;
	    color: #8C8C8C;
	    font-size: 12pt;
	    appearance: none;
	    position: absolute;
	    padding-left: 10px;
	    background-color: transparent;
    	z-index: 99;
	}
	
	select::-ms-expand {
		display:none
	}
	
	.sepoa_houseCode_submit_btn{
		width: 110px;
		height: 40px;
		font-size: 13pt;
		font-weight: 1000;
		margin-left: 10px;
	}
	
	.sepoa_signup_form{
		height: 40px;
		width: 100%;
		display: flex;
		margin: 0;
	}
	
	.sepoa_index_footer_wrapper{
		position: fixed;
		width: 157px;
		height: 40px;
		bottom: 0;
		right: 0;
		margin-bottom: 10px;
		margin-right: 10px;
	}
	
	.sepoa_index_footer_logo{
		background-image : url("./resource/design/imgs/main/logo.png");
		background-repeat: no-repeat;
		background-size: 70% 70%;
		width: 100%;
		height: 100%;
	}
	
	.sepoa_index_discription_wrapper{
		position: fixed;
		width: 100%;
	    text-align: center;
	    height: auto;
	    bottom: 0;
	    margin-bottom: 250px;
	}
	
	.sepoa_index_discription{
		font-size: 14pt;
		font-weight: 500;
	}
	
	.sepoa_index_select_wrapper{
		display: table;
    	position: relative;
	}
	
	.sepoa_index_select_control{
		width: 280px;
	    z-index: 1;
	    display: inline-table;
	    float: left;
	}
	
	.sepoa_index_select_after{
		width: 270px;
	    position: absolute;
	    height: 100%;
	    left: 0px;
	    top: 0px;
	    font-size: 25px;
	    text-align: right;
	    padding-right: 5px;
	    padding-top: 8px;
	}
</style>
</head>
<body onload="init()" class="sepoa_index_body">
	<div class="sepoa_index_bg_area">
		<div class="sepoa_index_form_wrapper">
		<div class="sepoa_client_select_text_area">
			<span class="sepoa_client_select_text">고객사를 선택하세요.</span>
		</div>
			<form id="signup-form" method="post" action="#" class="sepoa_signup_form">
				<div class="sepoa_index_select_wrapper">
					<div class="sepoa_index_select_control">
						<select id="houseCodeSelect" class="sepoa_houseCode_select">
							<%
						        for(Entry<String, String> entry : enabledCode.entrySet()){
						    %>
							<option value="<%=entry.getKey()%>">[<%=entry.getKey()%>] <%=entry.getValue()%></option>
							<%
								}
							%>
						</select>
						<div class="sepoa_index_select_after xi-angle-down"></div>
					</div>
				</div>
				<input type="button" value="바로가기" onclick="doMove();" class="sepoa_houseCode_submit_btn">
			</form>
		</div>
		<div class="sepoa_index_discription_wrapper">
			<div class="sepoa_index_discription_area">
				<span class="sepoa_index_discription"><strong>전자인장관리</strong> 도입으로 비대면 업무환경을 위한 날인업무의 디지털화 반복날인,서류발급 및 보관의 불편함을 해소합니다. </span>
			</div>
		</div>
		<div class="sepoa_index_footer_wrapper">
			<div class="sepoa_index_footer_logo"></div>
		</div>
	</div>
</body>
</html>
<%
		}
	}else{
		if(houseCode == null){
			houseCode = new Configuration("000").get(request.getServerName() + ".house_code", "000");
		}
		
		//houseCode check
		if(houseCode == null){
			out.println("<script>");
			out.println("	document.body.innerHTML = 'Failed to set the house code automatically.'");
			out.println("</script>");	
			return;
		}
		
		//compare housecode
		if(!houseCode.equals(sHouseCode)){
			SepoaSession.invalidate(request);
			SepoaSession.putValue(request, "HOUSE_CODE", houseCode);
			info = SepoaSession.getAllValue(request);
			Logger.warn.println("[index.jsp] Session invalidate : [housecode mismatch] "+houseCode+" != "+sHouseCode);
		}
		
		conf = new Configuration(houseCode);
		
		String customIndexPage = Cache.client.get("DESIGN.PORTAL_PAGE_URL");
		if(org.apache.commons.lang.StringUtils.isBlank(customIndexPage)){
			String id = info.getSession("ID");
			if(id != null && !id.isEmpty() && !conf.getBoolean("sepoa.index.session.invalidate", true)){
				out.println("<script>");
				out.println("history.pushState({data:'dummy'},'index','"+request.getContextPath()+"/index.jsp')");
				out.println("document.location.href = '"+request.getContextPath()+"/Main.sux'");
				out.println("</script>");		
			}else{
				if (conf.getBoolean("sepoa.entraidssologin.flag", false)) {
					response.sendRedirect(CONTEXT_NAME + "/sso_entra/main.jsp?house_code=" + URLEncoder.encode(houseCode, "UTF-8"));
					return;
				}
				String url = "/UxDefault/Index/IndexLogin.sux?house_code=" + houseCode;
				
				RequestDispatcher dispatcher = request.getRequestDispatcher(url);
				dispatcher.forward(request, response);
			}
		}else{
			out.println("<script>");
			out.println("document.location.href = '"+customIndexPage+"'");
			out.println("</script>");
		}
	}
%>

