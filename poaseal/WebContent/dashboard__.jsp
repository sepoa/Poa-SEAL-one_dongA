<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.google.gson.Gson" %>
<%@page import="java.util.*"%>
<%@page import="org.apache.commons.collections.MapUtils"%>
<%@page import="sepoa.fw.cache.Cache" %>
<%@page import="sepoa.fw.util.*" %>
<%@page import="sepoa.fw.srv.*" %>
<%@page import="sepoa.fw.ses.*" %>
<%@page import="sepoa.fw.version.SepoaVersion" %>
<%@page import="sepoa.fw.ux.design.DynamicCSS" %>
<%@page import="sepoa.fw.ux.*" %>
<%@page import="sepoa.fw.cfg.Configuration"%>
<%@page import="sepoa.fw.srv.module.SepoaModuleService"%>
<%
	SepoaUXDispatcher.logJspElapsedTime(request);

	response.setHeader("X-UA-Compatible", "IE=Edge");
	sepoa.fw.ses.SepoaInfo info = sepoa.fw.ses.SepoaSession.getAllValue(request);
	if(request.getHeader("User-Agent").indexOf("Trident") > -1) /* IE */ { out.println("<p style='font-size:2em;text-align:center;margin-top:2em;'>Internet Explorer는 지원하지 않습니다.<br>다른 브라우저를 사용해주세요.</p>");}
	
	Map<String, String> params = new HashMap<String, String>();
	Enumeration<String> paramKeys = request.getParameterNames();
	while(paramKeys.hasMoreElements()){
	    String key  = paramKeys.nextElement();
	    params.put(key, request.getParameter(key));
	}
	
	
	String requestString = new Gson().toJson(params);
    
    String CONTEXT_NAME = request.getContextPath();
    String sTokenString = request.getHeader("s-token");
    if(sTokenString == null){
    	if(params.containsKey("s-token")){
    		sTokenString = params.get("s-token");
    	}else{
    		sTokenString = "";
    	}
    } 
    
    String LANGUAGE = info.getSession("LANGUAGE");
    if(LANGUAGE == null || LANGUAGE.isEmpty()){
    	LANGUAGE = CommonUtil.getConfig("sepoa.default.language", "KO");
    }
    
    String FILTERBAR_VERSION = "old".equals(CommonUtil.getConfig("sepoa.filter.version", "new"))?"":"_v2";
    String POABASE_VERSION = SepoaVersion.getVersionWithTime("POABASE");
    String D_CSS_VERSION = DynamicCSS.getVersion(info.getSession("HOUSE_CODE"));

	Map<String, Object> allData = new HashMap<String, Object>();
	Map<String, String> headerData = new HashMap<String, String>();
	allData.put(SepoaDataMapper.KEY_HEADER_DATA, headerData);

	Configuration bsConfig = new Configuration();
	
	Object[] obj = {allData};

	int dashBoardListCount = Integer.parseInt(CommonUtil.getConfig("sepoa.frame.dashBoard.listCount", "3"));

	boolean useDashBoard = SepoaModuleService.checkBusiness(bsConfig, info, "DSB");

	String my_menu_profile_code = info.getSession("MENU_PROFILE_CODE");
	String admin_menu_profile_code = CommonUtil.getConfig("sepoa.system.manager.profile.code");
	String[] sm_menu_profile_code = CommonUtil.getConfig("sepoa.seal.manager.porfile.code").split(",");
	
	boolean isSystemAdmin = admin_menu_profile_code.equals(my_menu_profile_code);
	String ctrlCode = info.getSession("CTRL_CODE");
	List<String> ctrlList = Arrays.asList(ctrlCode.split(","));
	String mySeal = info.getSession("MY_SEAL");
	boolean isSealOwner = !"".equals(mySeal);
	
	
	boolean isMarkConfirmer = ctrlList.contains("SEAL_CONFIRM");	// 날인신청 승인자
	boolean isManageConfirmer = ctrlList.contains("MANAGE_CONFIRM");	// 인감관리 승인자
	boolean isExtConfirmer = ctrlList.contains("EXT_CONFIRM");	// 인감불출 승인자
	boolean isAttachConfirmer = ctrlList.contains("ATTACH_CONFIRM");	// 교부서류 처리대상 승인자
	
	boolean isSealManager = Arrays.asList(sm_menu_profile_code).contains(my_menu_profile_code);
	
	boolean useDashBoardReqSeal = false;	// 날인요청 대시보드 사용
	boolean useDashBoardSealMng = false;	// 인감관리 대시보드 사용
	boolean useDashBoardSealExt = false;	// 반출요청 대시보드 사용
	if (useDashBoard) {
		if (SepoaModuleService.checkBusiness(bsConfig, info, "USW")
			|| SepoaModuleService.checkBusiness(bsConfig, info, "CSW")
		) { useDashBoardReqSeal = true; }
		
		if (SepoaModuleService.checkBusiness(bsConfig, info, "US")
			|| SepoaModuleService.checkBusiness(bsConfig, info, "CS")
			|| SepoaModuleService.checkBusiness(bsConfig, info, "SM")
			|| SepoaModuleService.checkBusiness(bsConfig, info, "SR")
			|| SepoaModuleService.checkBusiness(bsConfig, info, "SL")
		) { useDashBoardSealMng = true; }
		
		if (SepoaModuleService.checkBusiness(bsConfig, info, "ET")) {
			useDashBoardSealExt = true; 
		}
	}
	
	boolean useAppManagement = SepoaModuleService.checkBusiness(bsConfig, info, "DSB1649060242862");	// 결재관리 사용
	boolean useBoard = SepoaModuleService.checkBusiness(bsConfig, info, "DSB1649060260590");	// 게시판 사용
	boolean useNotice = SepoaModuleService.checkBusiness(bsConfig, info, "DSB1649060343601");
	boolean useQna = SepoaModuleService.checkBusiness(bsConfig, info, "DSB1649060352966");
	
	
	
	
	// 날인요청 처리대상 보이기
	boolean showReqSealTodo = false;
	if (useDashBoardReqSeal) {
		if ((SepoaModuleService.checkBusiness(bsConfig, info, "CSW1635988016964")) 
				&& (isMarkConfirmer || isSystemAdmin)
		   ) { // 사용자가 날인신청 승인자거나 관리자고, 인감책임자 승인을 사용한다면
			showReqSealTodo = true;
		} else if (isSealManager && SepoaModuleService.checkBusiness(bsConfig, info, "USW1637309480717")) {
			// 사용자가 인감담당자고 인감담당자 승인을 사용한다면
			showReqSealTodo = true;
		}
			
	}
	
	
	// 인감관리 처리대상 보이기
	boolean showSealMngTodo = false;
	if (useDashBoardSealMng && (isManageConfirmer || isSystemAdmin)
		&& (SepoaModuleService.checkBusiness(bsConfig, info, "US1635932977187") // 사용인감 등록신청 - 인감책임자 승인
				|| SepoaModuleService.checkBusiness(bsConfig, info, "CS1635987524638") // 법인인감 등록신청 - 인감책임자 승인
				|| SepoaModuleService.checkBusiness(bsConfig, info, "SM1635987583152")	// 인감용도 변경신청 - 인감책임자 승인
				|| SepoaModuleService.checkBusiness(bsConfig, info, "SR1635987653583")	// 인감반납 신청 - 인감책임자 승인
				|| SepoaModuleService.checkBusiness(bsConfig, info, "SL1635987694835")	// 인감분실 신고 - 인감책임자 승인
		   )
		) {
			// 사용자가 인감관리승인자거나 관리자고, 인감책임자 승인을 사용한다면 
			showSealMngTodo = true;
	}
	
	// 반출요청 처리대상 보이기
	boolean showSealExtTodo = false;
	if (useDashBoardSealExt) {
		if ((SepoaModuleService.checkBusiness(bsConfig, info, "ET1644374538390") && (isSystemAdmin || isSealManager))	// 인감반출 신청 - 인감담당자 승인
			|| (SepoaModuleService.checkBusiness(bsConfig, info, "ET1644374538390") && (isSystemAdmin || isExtConfirmer))	// 인감반출 신청 - 인감책임자 승인 (이거 없어서 임시처리)
		   ) {
		    	showSealExtTodo = true;
		}
	}
	
%>
<!DOCTYPE html>
<html>
<head>
<title>Summary</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<script type="text/javascript">
	window.request=<%= requestString %>
	window.CONTEXT_NAME = "<%=CONTEXT_NAME%>"
	window.sTokenString = "<%=sTokenString%>"
	window._CACHE_TIME_ = "<%=Cache.text.getCacheTime()%>"
	window._POABASE_VERSION_ = "<%=POABASE_VERSION%>" 
</script>

<!-- JQuery 1.12 -->
<script src="<%=CONTEXT_NAME%>/resource/plugin/jquery/jquery-1.12.4.min.js"></script>
<!-- JQuery UI -->
<script src="<%=CONTEXT_NAME%>/resource/plugin/jquery/jquery-ui-1.12.1/jquery-ui.js"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/jquery/jquery-ui-1.12.1/jquery.ui.position.js"></script>
<script>
	var Sepoa = window.parent[1].Sepoa;
</script>
<!-- JQuery extends -->
<script src="<%=CONTEXT_NAME%>/resource/plugin/jquery/jquery-form/jquery.form.min.js"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/jquery/jquery-contextmenu/jquery.contextMenu.js"></script>
<link href="<%=CONTEXT_NAME%>/resource/plugin/jquery/jquery-contextmenu/jquery.contextMenu.css" rel="stylesheet" type="text/css"/>

<!-- Sepoasoft -->
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.polyfill.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.utils.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.default.js?v=<%=POABASE_VERSION%>"></script>

<!-- dhtmlx -->
<script src="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/dhtmlx.js"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxVault_v25_pro/codebase/dhtmlxvault.js"></script>
<link href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/fonts/font_roboto/roboto.css" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/skins/web/dhtmlx.css" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/skins/material/dhtmlx.css" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxVault_v25_pro/codebase/dhtmlxvault.css" rel="stylesheet" type="text/css" />

<!-- dhtmlx sepoa custom -->
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/colorpicker.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/calendar.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/calendar_lang.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/calendarYM.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/window.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/carousel.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/tabbar.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/layout.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/toolbar.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/sidebar.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/tile.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/chart.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/dataview.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/list.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/ribbon.js?v=<%=POABASE_VERSION%>"></script>

<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/colorpicker.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/list.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/carousel.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/tile.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/toolbar.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/layout.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/tabbar.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/window.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/css/calendarYM.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />

<!-- Chart.js 6.2.3 -->
<script src="<%=CONTEXT_NAME%>/resource/plugin/chartjs/chart.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/chartjs/chart.js-plugin-datalabels.js?v=<%=POABASE_VERSION%>"></script>
<link href="<%=CONTEXT_NAME%>/resource/plugin/chartjs/chart.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css">

<!-- Sepoasoft -->
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/sepoa_scripts.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/button/button.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/bigCalendar/bigCalendar.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/clock/clock.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/contextmenu/contextMenu.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/filter<%=FILTERBAR_VERSION%>/filter.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/flowView/flowView.js?v=<%=POABASE_VERSION%>"></script>
<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/headerbar/sepoa.headerbar.js?v=<%=POABASE_VERSION%>"></script>

<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/button/button.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/bigCalendar/bigCalendar.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/filter<%=FILTERBAR_VERSION%>/filter.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/flowView/flowView.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/headerbar/sepoa.headerbar.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" />

<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/ux/sepoa.ux.css?v=<%=POABASE_VERSION%>" rel="stylesheet" type="text/css" >

<script src="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/dhtmlx_custom/common.js?v=<%=POABASE_VERSION%>"></script>

<!-- XEIcon -->
<link href="<%=CONTEXT_NAME%>/resource/plugin/XEIcon/xeicon.min.css" rel="stylesheet" type="text/css" >
<link href="<%=CONTEXT_NAME%>/resource/plugin/sepoasoft/XEIcon_custom/sepoa.xeicon.css" rel="stylesheet" type="text/css" >

<!-- Sepoasoft dynamic css -->
<link href="<%=CONTEXT_NAME%>/SepoaDynamicResource.sux?module=sepoa.fw.ux.design.DynamicCSS&mode=resource&v=<%=D_CSS_VERSION%>" rel="stylesheet" type="text/css" >

<!-- APPEND -->
<%
	for(String jsPath : SepoaCustomWebResource.getJavaScriptList(info.getSession("HOUSE_CODE"))){
%>		<script src="<%=CONTEXT_NAME + jsPath%>"></script>
<%	}
	for(String cssPath : SepoaCustomWebResource.getCSSList(info.getSession("HOUSE_CODE"))){
%>		<link rel="stylesheet" type="text/css" href="<%=CONTEXT_NAME + cssPath%>" />
<%	}	%>

<script type="text/javascript">
	var reqSealStatusList = [];
	var sealMngTypeList = [];
	var sealExtStatusList = [];
	
	function getComboText(comboList, code) {
		var comboText = null;
		comboList.some((data, idx) => { 
			if (data.value == code) {
				comboText = data.text; 
				return true;
			} 
		});
		return comboText;
	}
	
	function getCounterDiv(spanId, linkParamKey, linkParamVal, title, screenId, linkUrl, menuName) {
		if (!spanId) return "<div class='counterWrapper'></div>";
		
		var loadingImgTag = "<img src='<%=CONTEXT_NAME%>/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/skins/web/imgs/dhxwins_web/dhxwins_progress.gif' width=20/>";
		var loadingSpan = "<span id='"+spanId+"' data-param-key='"+linkParamVal+"'>"+loadingImgTag+"</span>";
		var counterDiv = "<div class='counterWrapper'>";
		var onclick = "onclick='parent.linkTo(\""+screenId+"\",\""+linkUrl+"\",{"+linkParamKey+":\""+linkParamVal+"\"}, \""+menuName+"\");'";
		counterDiv    += "	<div class='contentTitle' "+onclick+">"+title+"</div>";
		counterDiv    += "	<div class='contentValue' "+onclick+">"+loadingSpan+"</div>";
		counterDiv    += "</div>";
		
		return counterDiv;
	}
	
	// 카운터 세팅
	function setCounters() {
		// 공통 세팅
		var date = new Date();
		//date.setDate(date.getDate()- 28);
		document.getElementById('F_CALENDAR_FROM').value = ''+date.getFullYear()+lpad((date.getMonth()),2,'0')+lpad(date.getDate(),2,'0');
		document.getElementById('F_CALENDAR_TO').value = sUtils.date.getToday();
		// 날인요청
		<% if (useDashBoardReqSeal) {%>
			var reqSealScreenId = 'SLM_200';
			var reqSealScreenUrl = '/Seal/Document/Request/SealDocumentRequestLayout.sux';
			var reqSealMenuName =  Sepoa.cache.text.get(reqSealScreenId+'.LAYOUT_A_HEADER');
			
			reqSealStatusList.push('T');	// T 임시저장
			if (sUtils.workflow.check('USW1635987860041') || sUtils.workflow.check('CSW1635988016443')) reqSealStatusList.push('I','R'); // 결재모듈
			if (sUtils.workflow.check('CSW1635988016964')) reqSealStatusList.push('G','N'); // 법무팀 승인
			if (sUtils.workflow.check('USW1637309480717')) reqSealStatusList.push('W','F'); // 인감관리자 승인
			if (sUtils.workflow.check('USW1635987861018') || sUtils.workflow.check('CSW1635988019236')) reqSealStatusList.push('CR','C'); // 완료
			$('#REQ_SEAL_STATUS_LIST').val(reqSealStatusList.join(','));
			
			var reqSealComboList = sUtils.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#SM032');
			var reqSealContentArea = $('#dashBoxReqSeal .dash-content'); 
			var reqSealCounterDivs = '';
			var reqSealCounterPrefix = "countReqSeal_";
			
			for(var idx in reqSealStatusList) {
				var status = reqSealStatusList[idx];
				var elId = reqSealCounterPrefix+status;
				var statusText = getComboText(reqSealComboList, status);
				reqSealCounterDivs += getCounterDiv(elId, "status", status, statusText, reqSealScreenId, reqSealScreenUrl, reqSealMenuName);
			}
			
			<%if (showReqSealTodo) {%>	// 날인요청 처리대상
			var reqSealTodoScreenId = 'SLM_190';
			reqSealCounterDivs += getCounterDiv(reqSealCounterPrefix+'TODO', "status", 'TODO', Sepoa.cache.text.get(reqSealTodoScreenId+'.LAYOUT_A_HEADER'), reqSealTodoScreenId, '/Seal/Document/Todo/SealDocumentTodoLayout.sux', Sepoa.cache.text.get(reqSealTodoScreenId+'.LAYOUT_A_HEADER'));
			<%}%>
			
			reqSealContentArea.append(reqSealCounterDivs);
		<%}%>
		
		
		
		// 인감관리
		<% if (useDashBoardSealMng) {%>
			var sealMngScreenId = 'SLM_170';
			var sealMngScreenUrl = '/Seal/Manage/SealManageRequestCarousel.sux';
			var sealMngMenuName =  Sepoa.cache.text.get('SLM_110.LAYOUT_A_HEADER');
			var sealMngCounterPrefix = "countSealMng_";
			
			if (sUtils.workflow.check('US') || sUtils.workflow.check('CS')) sealMngTypeList.push('NE'); // 인감등록신청
			<% if (isSystemAdmin || isSealOwner || isSealManager) { %>
			if (sUtils.workflow.check('SM')) sealMngTypeList.push('UP'); // 인감변경신청
			if (sUtils.workflow.check('SR')) sealMngTypeList.push('RE'); // 인감반납신청
			if (sUtils.workflow.check('SL')) sealMngTypeList.push('LO'); // 인감분실신고
			<% } %>
			
			$('#SEAL_MNG_TYPE_LIST').val(sealMngTypeList.join(','));
			
			var sealMngComboList = sUtils.cache.combo.get('SL0002,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#SM031');
			var sealMngContentArea = $('#dashBoxSealMng .dash-content'); 
			var sealMngCounterDivs = '';
			
			for(var idx in sealMngTypeList) {
				var type = sealMngTypeList[idx];
				var elId = sealMngCounterPrefix+type;
				var typeText = getComboText(sealMngComboList, type);
				sealMngCounterDivs += getCounterDiv(elId, 'manageType', type, typeText, sealMngScreenId, sealMngScreenUrl, sealMngMenuName);
			}
			
			<%if (showSealMngTodo) {%>	// 인감신청 처리대상
			var sealMngTodoScreenId = 'SLM_160';
			sealMngCounterDivs += getCounterDiv(sealMngCounterPrefix+'TODO', 'type', 'TODO', Sepoa.cache.text.get(sealMngTodoScreenId+'.LAYOUT_A_HEADER'), sealMngTodoScreenId, '/Seal/Manage/SealManageToDoCarousel.sux', Sepoa.cache.text.get(sealMngTodoScreenId+'.LAYOUT_A_HEADER'));
			<%}%>
			
			sealMngContentArea.append(sealMngCounterDivs);
		<%}%>
		
		
		// 인감반출요청
		<% if (useDashBoardSealExt) {%>
			var sealExtScreenId = 'SLM_510';
			var sealExtScreenUrl = '/Seal/Document/External/Request/SealExternalRequestLayout.sux';
			var sealExtMenuName =  Sepoa.cache.text.get('SLM_511.LAYOUT_A_HEADER');
			
			sealExtStatusList.push('T');	// T 임시저장
			if (sUtils.workflow.check('ET1644374538390')) sealExtStatusList.push('W'); // 승인요청
			if (sUtils.workflow.check('ET1644372420935')) sealExtStatusList.push('I'); // 결재요청
			if (sUtils.workflow.check('ET1647222908416')) sealExtStatusList.push('RW','RR'); // 반출승인, 반납요청
			if (sUtils.workflow.check('ET1644375000197')) sealExtStatusList.push('C'); // 완료
			if (sUtils.workflow.check('ET1644374538390')) sealExtStatusList.push('F'); // 승인반려
			if (sUtils.workflow.check('ET1644372420935')) sealExtStatusList.push('R'); // 결재반려
			$('#SEAL_EXT_STATUS_LIST').val(sealExtStatusList.join(','));
			
			var sealExtComboList = sUtils.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#SM034');
			var sealExtContentArea = $('#dashBoxSealExt .dash-content'); 
			var sealExtCounterDivs = '';
			var sealExtCounterPrefix = "countSealExt_";
			
			for(var idx in sealExtStatusList) {
				var status = sealExtStatusList[idx];
				var elId = sealExtCounterPrefix+status;
				var statusText = getComboText(sealExtComboList, status);
				sealExtCounterDivs += getCounterDiv(elId, "status", status, statusText, sealExtScreenId, sealExtScreenUrl, sealExtMenuName);
			}
			
			<%if (showSealExtTodo) {%>	// 반출요청 처리대상
			sealExtCounterDivs += getCounterDiv(sealExtCounterPrefix+'TODO', "status", 'TODO', Sepoa.cache.text.get('SLM_521.LAYOUT_A_HEADER'), 'SLM_520', '/Seal/Document/External/Todo/SealDocumentExternalTodoLayout.sux', Sepoa.cache.text.get('SLM_521.LAYOUT_A_HEADER'));
			<%}%>
			
			sealExtContentArea.append(sealExtCounterDivs);
		<%}%>
	}
	
	// 활성 탭 변경
	function changeTabList(tabEl) {
		var className = Array.from(tabEl.parentElement.classList).filter(function(className){ return className == 'tabs' ? false : true; })[0];
		$("ul."+className+" li").removeClass("active");
		$(tabEl).addClass("active");
		$("."+className+" .tab_content").hide();
		var activeTab = tabEl.getAttribute('rel');
		$("#" + activeTab).fadeIn();
	}
	
	function init() {
		Sepoa.setScreenId('SUMMARY_DASH');
		setCarousel();
		setCounters();
		setTabList();
		
		// 리스트 Tab 부분
		$("ul.summary li div.content table th").parent("tr").next().find("td").css("padding-top", "5px");
		
		$(".tab_content").hide();

		setTimeout(function(){
			
			// 날인요청 카운트 가져오기
			<% if (useDashBoardReqSeal) { %>
			setTimeout(function(){
				doServiceAjax('sepoa.svc.seal.SepoaReqSealService','CONNECTION',"doCountRequestList", null, function( so ){
					if(so.status == '1'){
						var counterObj = new Object();
						var resultStr = so.result[0];
						resultStr.substring(1, resultStr.length-1).split(',').forEach(val => {
						   var kv = val.trim().split('=');
						    counterObj[kv[0]] = kv[1];
						});
						reqSealStatusList.forEach((status, idx) => {
							var count = counterObj['REQ_SEAL_COUNT_'+status];
							$("#countReqSeal_"+status).html(count);
						});
						<%if (showReqSealTodo) {%>
						$("#countReqSeal_TODO").html(counterObj['REQ_SEAL_COUNT_TODO']);
						<%}%>
					} else {
						Alert(so.message);
					}
				},true)
				}
			,100);
			<% } %>
			
			// 인감관리 카운트 가져오기
			<% if (useDashBoardSealMng) { %>
			setTimeout(function(){
				doServiceAjax('sepoa.svc.seal.SepoaSealManagementService','CONNECTION',"doCountRequestList", null, function( so ){
					if(so.status == '1'){
						var counterObj = new Object();
						var resultStr = so.result[0];
						resultStr.substring(1, resultStr.length-1).split(',').forEach(val => {
							var kv = val.trim().split('=');
							counterObj[kv[0]] = kv[1];
						});
						sealMngTypeList.forEach((type, idx) => {
							var count = counterObj['SEAL_MNG_COUNT_'+type];
							$("#countSealMng_"+type).html(count);
						});
						<%if (showSealMngTodo) {%>
						$("#countSealMng_TODO").html(counterObj['SEAL_MNG_COUNT_TODO']);
						<%}%>
					} else {
						Alert(so.message);
					}
				},true)
				}
			,100);	
			<% } %>
			
			// 반출요청 카운트 가져오기
			<% if (useDashBoardSealExt) { %>
			setTimeout(function(){
				doServiceAjax('sepoa.svc.seal.SepoaReqExternalService','CONNECTION',"doCountRequestList", null, function( so ){
					if(so.status == '1'){
						var counterObj = new Object();
						var resultStr = so.result[0];
						resultStr.substring(1, resultStr.length-1).split(',').forEach(val => {
						   var kv = val.trim().split('=');
						    counterObj[kv[0]] = kv[1];
						});
						sealExtStatusList.forEach((status, idx) => {
							var count = counterObj['SEAL_EXT_COUNT_'+status];
							$("#countSealExt_"+status).html(count);
						});
						<%if (showSealExtTodo) {%>
						$("#countSealExt_TODO").html(counterObj['SEAL_EXT_COUNT_TODO']);
						<%}%>
					} else {
						Alert(so.message);
					}
				},true)
				}
			,100);
			<% } %>
		},1000);
		
		$('.dhx_cell_cont_carousel').css('overflow','auto');
	}
	window.addEventListener('resize', function() {
		//sUtils.progress.off();
		//document.location.reload();
		const bodyWidth = document.getElementsByTagName('body')[0].clientWidth+'px';
		$('.dhx_cell_carousel').css('width', bodyWidth);
		$('.dhxcarousel_cont').css('width', bodyWidth);
		$('.dhx_cell_cont_carousel').css('width', bodyWidth);
	}, true);
	
	
	// 천단위 콤마 추가 : 1,000
	function addComma(n) {
		return (n+"").replace(/(\d)(?=(\d{3})+$)/g, "$1,");
	}

	var carouselObj;
	
	function setCarousel() {
		carouselObj = new SepoaDhtmlXCarouselObject({
			parent : 'carousel',
			effect : 'cards',
			item_width : 'auto',
			item_height : 'auto',
			offset_item : 0,
			offset_left : 0,
			offset_top : 0
		});
		carouselObj.addCell('dashBoard');
		carouselObj.cells('dashBoard').attachObject('SepoaDashBoard');
		
		carouselObj.cells('dashBoard').setActive();
		carouselObj.hideControls();
		SUX.attachEvent('onWiaddCommandowResize', function() {
			carouselObj.setSizes();
			$("#SepoaSummary").css("height", $(window).height() + "px");
		});
		carouselObj.setSizes();
		$(window).trigger('resize');
	}

	function changeToDashBoard() {
		//carouselObj.cells('dashBoard').setActive();
	}

	function changeToSummary() {
		//parent.SUX.root.hideMenu();
		parent.changeCarousel('summary');
	}

	// 리스트 세팅
	function setTabList() {
		// 공통
		var maxRowCount = <%=CommonUtil.getConfig("sepoa.dashboard.board.maxRowCount")%>;
		
		<% if (useAppManagement) { %>
		// 결재목록-미결재함
		var appAreaClass = 'appList';
		var appGridId = 'AP_102';
		var appServiceClass = 'sepoa.svc.approval.SepoaApprovalService';
		var appServiceMethod = 'doQuery';
		
		var appTurnListTabName = Sepoa.cache.text.get("AP_101.TITLE_TURN");
		var appTurnListColInfo = {
			'APP_NO' : {
				width : '120px'
			}, 
			'APP_TYPE' : {
				val2TextMethod : 'combo',
				val2TextParam : 'SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#AP001'
			}, 
			'APP_SUBJECT' : {
				width : '450px',
				style : 'text-align:left;'
			}, 
			'REQ_USER_NAME' : {}, 
			'REQ_USER_DEPT_NAME' : {}, 
			'REQ_DATE' : {
				val2TextMethod : 'date',
				val2TextParam : '/'
			}
		}
		
		var appTurnListLinkInfo = {
			linkScreenId : 'AP_100_TURN',
			linkUrl : '/Seal/Approval/SealApprovalCarousel.sux?MODE=TURN',
			linkParam : {},
			linkMenuName : appTurnListTabName
		}
		
		getListData(
			{
				serviceClass : appServiceClass,
				serviceMethod : appServiceMethod, 
				serviceParam : {MODE : 'TURN'}
			}, 
			maxRowCount, 
			appTurnListColInfo, 
			{
				areaClass : appAreaClass, 
				tabId : 'tab1',
				tabName : appTurnListTabName,
				active : true, 
				linkInfo : appTurnListLinkInfo, 
				gridId : appGridId
			}
		);
		
		// 결재목록-결재내역
		var appDoneListTabName = Sepoa.cache.text.get("AP_101.TITLE_DONE");
		var appDoneListColInfo = {
			'APP_NO' : {
				width : '120px'
			}, 
			'APP_TYPE' : {
				class : 'app_type',
				val2TextMethod : 'combo',
				val2TextParam : 'SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#AP001'
			},
			'APP_STATUS' : {
				class : 'app_status',
				val2TextMethod : 'combo',
				val2TextParam : 'SL0002,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#CT002'
			}, 
			'APP_SUBJECT' : {
				width : '450px',
				style : 'text-align:left;'
			}, 
			'REQ_USER_NAME' : {}, 
			'REQ_USER_DEPT_NAME' : {}, 
			'REQ_DATE' : {
				val2TextMethod : 'date',
				val2TextParam : '/'
			}
		}
		
		var appDoneListLinkInfo = {
			linkScreenId : 'AP_100_DONE',
			linkUrl : '/Seal/Approval/SealApprovalCarousel.sux?MODE=DONE',
			linkParam : {},
			linkMenuName : appDoneListTabName
		}
		
		getListData(
			{
				serviceClass : appServiceClass,
				serviceMethod : appServiceMethod, 
				serviceParam : {MODE : 'DONE'}
			}, 
			maxRowCount, 
			appDoneListColInfo, 
			{
				areaClass : appAreaClass, 
				tabId : 'tab2',
				tabName : appDoneListTabName,
				active : false, 
				linkInfo : appDoneListLinkInfo, 
				gridId : appGridId
			}
		);
		<% } %>
		
		<% if (useBoard) { %>
			// 게시판
			var boardAreaClass = 'boardList';
		
			<% if(useNotice) { %>
				// 게시판-공지사항
				var noticeServiceClass = 'sepoa.svc.admin.AD_202_3';
				var noticeServiceMethod = 'doQuery';
				var noticeTabName = Sepoa.cache.text.get("AD_202.LAYOUT_B_HEADER");
				var noticeListColInfo = {
					'NOTICE_NUMBER' : {
						colTitle : Sepoa.cache.text.get("SUMMARY.COL_NT_NOTICE_NUMBER"),
						width : '120px'
					}, 
					'SUBJECT' : {
						colTitle : Sepoa.cache.text.get("SUMMARY.COL_NT_SUBJECT"),
						style : 'text-align:left;padding-left: 40px;'
					}, 
					'ADD_USER_NAME' : {
						colTitle : Sepoa.cache.text.get("SUMMARY.COL_NT_ADD_USER_NAME"),
						width : '200px',
					}, 
					'ADD_DATE' : {
						colTitle : Sepoa.cache.text.get("SUMMARY.COL_NT_ADD_DATE"),
						width : '200px',
						val2TextMethod : 'date',
						val2TextParam : '/'
					}
				}
				
				var noticeListLinkInfo = {
					linkScreenId : 'AD_202',
					linkUrl : '/Admin/NoticeView/AdminNoticeViewLayout.sux',
					linkParam : {},
					linkMenuName : noticeTabName
				}
				
				getListData(
					{
						serviceClass : noticeServiceClass,
						serviceMethod : noticeServiceMethod, 
						serviceParam : {QUERY_MODE : 'doList'}
					}, 
					maxRowCount, 
					noticeListColInfo,
					{
						areaClass : boardAreaClass, 
						tabId : 'tab3',
						tabName : noticeTabName,
						active : true, 
						linkInfo : noticeListLinkInfo, 
						gridId : ''
					}
				);
			<% } %>
		
			<% if(useQna) { %>
				// 게시판-QnA
				var qnaServiceClass = 'sepoa.svc.board.SepoaQAService';
				var qnaServiceMethod = 'doQuery';
				var qnaTabName = Sepoa.cache.text.get("QA_002.LAYOUT_A_HEADER");
				var qnaListColInfo = {
					'QNA_NUMBER' : {
						width : '120px'
					},
	//	 			'QNA_TYPE' : {
	//	 				width : '150px',
	//	 				val2TextMethod : 'combo',
	//	 				val2TextParam : 'SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#UX920'
	//	 			},
					'SUBJECT' : {
						style : 'text-align:left;padding-left: 40px;'
					},
					'REPLY_CNT' : {
						width : '90px',
					},
					'ADD_USER_NAME' : {
						width : '200px',
					}, 
					'ADD_DATE' : {
						width : '200px',
						val2TextMethod : 'date',
						val2TextParam : '/'
					}
				}
				
				var qnaListLinkInfo = {
					linkScreenId : 'QA_002',
					linkUrl : '/Seal/Board/Qna/BoardQnaLayout.sux',
					linkParam : {},
					linkMenuName : qnaTabName
				}
				
				getListData(
					{
						serviceClass : qnaServiceClass,
						serviceMethod : qnaServiceMethod, 
						serviceParam : {}
					}, 
					maxRowCount, 
					qnaListColInfo,
					{
						areaClass : boardAreaClass, 
						tabId : 'tab4',
						tabName : qnaTabName,
						active : false, 
						linkInfo : qnaListLinkInfo, 
						gridId : 'QA_002_1'
					}
				);
			<% } %>
		<% } %>
	}

	function getListData(serviceObj, maxRowCount, colInfo, tabListParam) {
		
		// 탭 순서를 위해 먼저 세팅
		$('.tabs.'+tabListParam.areaClass).append('<li class="journal'+(tabListParam.active ? ' active':'')+'" rel="'+tabListParam.tabId+'" onclick="changeTabList(this);">'+tabListParam.tabName+'</li>');
		
		doServiceAjax(serviceObj.serviceClass, 'CONNECTION', serviceObj.serviceMethod, serviceObj.serviceParam, function( so ){
			if(so.status == '1'){
				var listContent = new Array(); 
				var arrMap = new SepoaFormater(so.result[0]).getArrayMap();
				var colArr = Object.keys(colInfo);
				for (var ri = 0; ri < Math.min(maxRowCount, arrMap.length); ri++) {
					var row = arrMap[ri];
					listContent[ri] = new Object();
					for (ci in colArr) {
						var colName = colArr[ci];
						listContent[ri][colName] = row[colName];
					}
				}
				
				setTabListData(tabListParam.areaClass, tabListParam.tabId, tabListParam.tabName, tabListParam.active, tabListParam.linkInfo, maxRowCount, tabListParam.gridId, colInfo, listContent);
			} else {
				Alert(so.message);
			}
		},true);
	}


	function setTabListData(areaClass, tabId, tabName, active, linkInfo, maxRowCount, gridId, listColInfo, listContent) {
		var listColArr = Object.keys(listColInfo);
		var colCount = listColArr.length;
		
		// 리스트 시작
		var listContentHtml = '';
		listContentHtml  = '<div id="'+tabId+'" class="tab_content">';
		listContentHtml += '    <div class="ra">';
		listContentHtml += '        <table class="ra" onclick="parent.linkTo(\''+linkInfo.linkScreenId+'\', \''+linkInfo.linkUrl+'\', '+linkInfo.linkParamObj+', \''+linkInfo.linkMenuName+'\');">';

		// 헤더
		listContentHtml += '            <tr>';
		for (var i = 0; i < colCount; i++) {
			var listColName = listColArr[i];
			var colInfo = listColInfo[listColName];
			var colTitle = colInfo['colTitle'] || Sepoa.cache.text.get(gridId+'.'+listColName); 
			listContentHtml += '            <th class="journal" '+(colInfo.width ? 'style="width:'+colInfo.width+';"' : '')+'>'+colTitle+'</th>';
		}
		listContentHtml += '            </tr>';
		
		// 내용
		if (listContent.length) {
			for (var ri = 0; ri < maxRowCount; ri++) {
				var rowObj = listContent[ri];
				listContentHtml += '    <tr>';
				if (rowObj) {
					for (var colName in listColInfo) {
						var contentVal = rowObj[colName] || '';
						var listColObj = listColInfo[colName];
						var contentText = val2Text(contentVal, listColObj['val2TextMethod'], listColObj['val2TextParam']);
						var eLclass = listColObj['class'] ? ' ' + listColObj['class'] :  ''
						var style = listColObj['style'] || '';
						style += (style && style.slice(-1) != ';' ? ';' : '');
						listContentHtml += '    <td class="listContentText'+ eLclass +'" style="'+ style +'">'+ contentText +'</td>';
					}
				} else {	// 내용이 없으면
					listContentHtml += '    <td colspan="'+colCount+'"></td>';
				}
				listContentHtml += '    </tr>';
			}
		}  else {	// 내용이 없을때
			var tdId = tabId+'_nodata';
			listContentHtml += '        <tr>';
			listContentHtml += '            <td colspan="'+colCount+'" class="journal" id="'+tdId+'">';
			listContentHtml += '                <div style="display:flex;justify-content:center;align-items:center;">'+Sepoa.cache.text.get("SUMMARY.NO_DATA")+'</div>';
			listContentHtml += '            </td>';
			listContentHtml += '        </tr>';
		}
		
		// 리스트 종료
		listContentHtml += '        </table>';
		listContentHtml += '    </div>';
		listContentHtml += '</div>';
		
		$('.tab_container.'+areaClass).append(listContentHtml);
		if (!listContent.length) {
			var $emptyTd = $('#'+tdId);
			$emptyTd.height($emptyTd.height() * maxRowCount);
		}
		
		if (active) $('#'+tabId).show();
	}

	function setDateDelim(yyyymmdd, delim) {
		return yyyymmdd.slice(0, 4) + delim + yyyymmdd.slice(4, 6) + delim + yyyymmdd.slice(6);
	}

	function val2Text(val, method, param) {
		if (!method) return val;
		if (method == 'combo') {
			var comboList = sUtils.cache.combo.get(param);
			return getComboText(comboList, val);
		} else if (method == 'date') {
			return setDateDelim(val, param);
		}
	}


</script>

<style type="text/css">
body {
	background-color: #ffffff;
}
.dash-content {
	display: flex;
   	flex-wrap: wrap;
   	align-content: flex-start;
   	justify-content: flex-start;
}
.counterWrapper {
	width: 180px;
	height:30px;
	display:inline-flex;
	padding-left: 20px;
}
.counterWrapper div {
	display: inline-flex;
   	align-items: center;
   	cursor:pointer;
}
.counterWrapper div.contentTitle {
	width:80%;
}
.counterWrapper div.contentValue {
	width:20%;text-align: left;
}

a:hover {
	text-decoration: none;
}

.dashbox h4 img {
	position: relative;
	top: 20px;
}

.dashbox h4 span {
	padding-left: 15px;
}

.dash-row {
	width: 100%;
	max-width: 1200px;
}

.dash-box {
	position: relative;
	mid-height: 230px;
	border: none;
	margin: 0px;
	background-position: 0 0;
	background-repeat: no-repeat;
	background-color: #FFF;
	display: inline-block;
	float: none;
	width:170px;
	margin-right:15px;
}

.dash-title {
	font-size: 17px;
	font-weight: bold;
	position: relative;
	font-family: 'Roboto', 'Noto Sans KR';
	color:<%=CommonUtil.getConfig("css.dashboard.dashBox.header.text.color")%>;
	line-height:3;
	border:1px solid <%=CommonUtil.getConfig("css.dashboard.dashBox.header.border.color")%>;
    border-radius: 5px 5px 0 0;
    background-color:<%=CommonUtil.getConfig("css.dashboard.dashBox.header.background.color")%>;
}

.dash-content {
	color: #ff8000;
	font-size: 1.1em;
	font-weight: 400;
	background: none;
	border:1px solid #eeeeee;
	border-radius: 0 0 5px 5px;
	display: flex;
	min-height: 180px;
}

.dash-content table{
	width:100%;
}
.contentTitle {
	width:65%;
	font-size: 13px !important;
	color:#666666 !important;
	font-weight:700;
}

.contentValue {
	width:35%;
	font-size: 13px !important;
	color: <%=CommonUtil.getConfig("css.dashboard.dashBox.count.color")%> !important;
	font-weight:700;
}

div.dashbox {
	margin: 0;
	padding: 0 20px;
}

div.wrap {
	position: absolute;
	left: 0;
	right: 230px;
	top: 0;
	bottom: 0;
}

div.wrap>table {
	border-collapse: collapse;
	height: 100%;
}

div.wrap>table td {
	text-align: center;
	vertical-align: middle;
}

/* Summary Widgets */
ul.summary {
	margin: 5px 0px 0px 40px;
	padding: 0;
	display: table;
    width: calc(100% - 40px);
}

ul.summary li {
	list-style-type: none;
}

ul.summary li div.title {
	height: 30px;
	font-weight: bold;
	font-size: 17px;
	margin-bottom: 10px;
}

ul.summary li div.title img {
	margin-right: 10px;
}

ul.summary li div.title span.desc {
	float: right;
	font-size: 0.6em;
	color: #666;
}

ul.summary li div.content {
	width: calc(100% - 60px);
	height: 180px;
	padding: 10px 20px;
	border-radius: 6px;
	border: none;
	box-shadow: 3px 1px 5px 1px #d4d4d4;
	background-color: #FFF;
}

ul.summary li div.content>table, ul.summary li div.content>div>table {
	width: 100%;
}

ul.summary li div.content>table th, ul.summary li div.content>div>table th
	{
	padding: 0 5px;
	height: 28px;
	font-size: 1.2em;
	border-bottom: solid 1px #eee;
	text-align: left;
	color: <%=CommonUtil.getConfig("css.dashboard.summary.title.text.color")%>;
	font-weight: 700;
}

.titleTh {
	border-bottom:none !important;
}

ul.summary li div.content>table th a, ul.summary li div.content>div>table th a
	{
	display: inline-block;
	padding-right: 15px;
	height: 28px;
	line-height: 28px;
	color: #666;
}

ul.summary li div.content>table td, ul.summary li div.content>div>table td {
	height: 25px;
	font-size: 1.1em;
	color: #333;
}

ul.tabs {
	margin: 0;
	padding: 0;
	float: left;
	list-style: none;
	height: 32px;
	border-bottom: 1px solid #eee;
	width: 100%;
	font-size: 17px;
	position: relative;
}

ul.tabs li {
	float: left;
	text-align: center;
	cursor: pointer;
	width: 120px;
	height: 32.4px;
	line-height: 31px;
	/* border: 1px solid #eee; */
	border-left: none;
	font-weight: 700;
	overflow: hidden;
	position: relative;
	color: #333;
}

ul.tabs li.active {
	background: #FFFFFF;
	color: <%=CommonUtil.getConfig("css.dashboard.tab.active.title.text.color")%>;
}

ul.tabs li.active:before {
	content: "";
	position: absolute;
	top: 29.4px;
	left: 33%;
	bottom: 0;
	height: 1px;
	width: 45%;
	border-bottom: 2px solid <%=CommonUtil.getConfig("css.dashboard.tab.active.underline.color")%>;
	/* margin: 20px auto; */
}

ul.tabs li {
	list-style-type: none;
	float: left;
	color:<%=CommonUtil.getConfig("css.dashboard.tab.title.text.color")%>;
	/* margin:0 0 40px 0; */
}

.tab_container {
	/* border: 1px solid #eee; */
	border-top: none;
	clear: both;
	/* float: left; */
	width: 100%;
	background: #FFFFFF;
	margin: 0 auto;
}

.tab_content {
	width: 100%;
	padding: 0;
	font-size: 12px;
	display: none;
}

.tab_container .tab_content ul {
	width: 100%;
	margin: 0px;
	padding: 0px;
}

.tab_container .tab_content ul li {
	padding: 5px;
	list-style: none
}

#container {
	width: 100%;
	margin: 0 auto;
}

div.summaryLoading {
	display: none;
}

div.container .tabs .journal {
	padding-left: 15px;
}

/* div.container .tabs .journal:nth-child(2) {
	margin-left: -25px;
} */

/* Grid Header */
div.gridbox_material.gridbox .xhdr {
	border-bottom: solid 1px #BBB;
}

div.gridbox_material.gridbox table.hdr td {
	background: linear-gradient(#dce9f6, #edf4fb);
}

div.gridbox_material.gridbox table.hdr td div.hdrcell {
	font-size: 11px;
	font-weight: 500;
	color: #333;
}

/* Grid List */
div.gridbox_material.gridbox table.obj.row20px tr td {
	font-size: 11px;
	color: #666;
	border-bottom: solid 1px #DDD;
}

div.ra {
	padding: 0px 0px 40px 0px;
}

table.ra {
	width: 100%;
	border-collapse: collapse;
	border-spacing: 0;
}

ul.summary li div.content table.ra th, ul.summary li div.content table.ra td {
	font-size: 13px;
}

table.ra th {
	text-align: center !important;
}

td.dash-Contain {
	text-align: center;
}

td.listContentText {
	text-align:center; 
	white-space:nowrap; 
	overflow:hidden; 
	text-overflow:ellipsis;
}

#SepoaDashBoard *{
	font-family: 'Roboto', 'Noto Sans KR';
}

ul.summary li:last-child {
	margin-bottom:20px;
}

</style>

</head>

<body onload="init();">
	<div id="carousel" style="width: 100%; height: 100%"></div>
	<div id="SepoaDashBoard" class="SepoaSummary container lowerDiv" style="overflow: auto; background: #f4f4f6;">
		<div id="dash" style="width: 100%;">
			<ul class="summary">
				<!-- Go To Menu -->
				<li style="display: flex;margin-bottom: 7px;">
					<div style="width:100%; height:auto;padding-right: 20px;">
						<a href="javascript:changeToSummary();">
							<div id="goToMenuBtn" style="text-align: center; width: 181px; height: 40px; background-color:<%=CommonUtil.getConfig("css.dashboard.button.menu.background.color")%>; color:<%=CommonUtil.getConfig("css.dashboard.button.menu.text.color")%>;float: right;">
								<span style="color: white; font-weight: 500; font-size: 16px; position: relative; top: 8px;">Go To Menu</span>
							</div>
						</a>
					</div>
				</li>
				<!-- To Do List -->
				<li>
					<% if (useDashBoardReqSeal || useDashBoardSealMng || useDashBoardSealExt) {%>
					<div class="content" style="height: auto;flex: 1;">
						<table>
							<tr>
								<th class="titleTh">
									<div class="title"><%=Cache.text.get("SUMMARY.TITLE_TODO_LIST")%></div> <!-- To Do List -->
									
								</th>
							</tr>
							<tr>
								<td class="dash-Contain" style="text-align: center;">
									<div style="width:100%;margin-bottom:15px;display:flex;justify-content: space-around;">
									<% if (useDashBoardReqSeal) { %>
										<div class="dash-box" id="dashBoxReqSeal" style="flex: 1;">
											<div class="dash-title journal"><%=Cache.text.get("SUMMARY.TITLE_REQ_SEAL")%></div>	<!-- 날인요청 -->
											<div class="dash-content" id="dashContentReqSeal">
											</div>
										</div>
									<% } %>
									<% if (useDashBoardSealMng) { %>	
										<div class="dash-box" id="dashBoxSealMng" style="flex-basis:180px;">
											<div class="dash-title journal"><%=Cache.text.get("SUMMARY.TITLE_SEAL_MNG")%></div> <!-- 인감관리 -->
											<div class="dash-content">
											</div>
										</div>
									<% } %>
									<% if (useDashBoardSealExt) { %>
										<div class="dash-box"  id="dashBoxSealExt" style="flex: 1;">
											<div class="dash-title journal"><%=Cache.text.get("SUMMARY.TITLE_SEAL_EXT")%></div>	<!-- 인감반출요청 -->
											<div class="dash-content">
											</div>
										</div>
									<% } %>
									</div>
								</td>
							</tr>
						</table>
					</div>
					<br> 
					<% } %>
				</li>
				<% if (useAppManagement) {%>
				<!-- 결재관리 -->
				<li>
					<div class="content" style="height:auto;">
						<table>
							<tr>
								<th class="titleTh">
									<div class="title"><%=Cache.text.get("SUMMARY.TITLE_APP_MANAGE")%></div>	<!-- 결재관리 -->
								</th>
							</tr>
							<tr>
								<td>
									<div id="container">
									    <ul class="tabs appList">
									    </ul>
									    <div class="tab_container appList">
									    </div>
									</div>															
								</td>
							</tr>	
						</table>
					</div>
				</li>
				<br>
				<% } %>
				<% if (useBoard) {%>
				<!-- 게시판 -->
				<li>
					<div class="content" style=" height:auto;">
						<table>
							<tr>
								<th class="titleTh">
									<div class="title"><%=Cache.text.get("SUMMARY.TITLE_BOARD")%></div>	<!-- 게시판 -->
								</th>
							</tr>
							<tr>
								<td>
									<div id="container">
									    <ul class="tabs boardList">
									    </ul>
									    <div class="tab_container boardList">
									    </div>
									</div>															
								</td>
							</tr>	
						</table>
					</div>
				</li>
				<% } %>
			</ul>
		</div>
	</div>
	<input type="hidden" id="REQ_SEAL_STATUS_LIST" name="REQ_SEAL_STATUS_LIST" value="">
	<input type="hidden" id="SEAL_MNG_TYPE_LIST" name="SEAL_MNG_TYPE_LIST" value="">
	<input type="hidden" id="SEAL_EXT_STATUS_LIST" name="SEAL_EXT_STATUS_LIST" value="">
	<input type="hidden" id="F_CALENDAR_FROM" name="F_CALENDAR_FROM" value="">
	<input type="hidden" id="F_CALENDAR_TO" name="F_CALENDAR_TO" value="">
	<div id="footer">
		<div id="input_data"></div>
	</div>
</body>
</html>