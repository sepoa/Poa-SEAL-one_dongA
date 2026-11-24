<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="sepoa.fw.ses.SepoaSession" %>
<%@ page import="sepoa.fw.ses.SepoaInfo"%>
<%@ page import="sepoa.fw.util.JSPUtil" %>
<%@ page import="sepoa.fw.cache.Cache" %>
<%@ page import="sepoa.fw.cfg.*" %>
<%@ page import="sepoa.fw.util.*" %>
<%@ page import="sepoa.fw.srv.*" %>
<%@ page import="sepoa.fw.log.Logger"%>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="javax.servlet.RequestDispatcher"%>
<% response.setHeader("X-UA-Compatible", "IE=Edge"); %>
<!DOCTYPE html>
<%
	request.getSession().setAttribute("AssetVersion", System.currentTimeMillis());
	sepoa.fw.ses.SepoaInfo info = sepoa.fw.ses.SepoaSession.getAllValue(request);
	String CONTEXT_NAME = request.getContextPath();
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
	headerData.put("HOUSE_CODE", houseCode);
	String timemilli = String.valueOf(System.currentTimeMillis());
%>
<html>

<head>
	<title>EasySeal</title>
	<meta charset="UTF-8" />
	<script type="text/javascript" src="https://code.jquery.com/jquery-3.4.1.min.js"></script>
	<script type="text/javascript" src="../../Portal.js?v=<%=timemilli%>"></script>
	<script type="text/javascript" src="../../jquery.fullPage.js"></script>
    <link rel="stylesheet" type="text/css" href="css/Portal.css?v=<%=timemilli%>" />
	<link rel="stylesheet" type="text/css" href="css/jquery.fullPage.css" />
	
	<script src="../../../resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/codebase/dhtmlx.js"></script>
	<script src="../../../resource/plugin/sepoasoft/ux/sepoa.ux.utils.js"></script>
	<script src="../../../resource/plugin/sepoasoft/ux/sepoa.ux.default.js"></script>
	
	<script type="text/javascript" src="../../../resource/plugin/sepoasoft/sepoa_scripts.js"></script>
	
	<script src="../../../resource/plugin/sepoasoft/dhtmlx_custom/common.js"></script>
	<script src="../../../resource/plugin/sepoasoft/dhtmlx_custom/window.js"></script>
	<script src="../../../resource/plugin/sepoasoft/dhtmlx_custom/layout.js"></script>
	
	<link href="../../../resource/plugin/XEIcon/xeicon.min.css" rel="stylesheet" type="text/css" >
	<link href="../../../resource/plugin/sepoasoft/XEIcon_custom/sepoa.xeicon.css" rel="stylesheet" type="text/css" >
	
	<script>
		window.CONTEXT_NAME = "<%=CONTEXT_NAME%>"
	
		function questionEmailSend(){
			var coll = document.getElementById("bottom-err-null");
			var coll2 = document.getElementById("bottom-err-sendmail");
			if (coll2.style.display != "none") coll2.style.display = "none";
			
			var params = {};
			params.EMAIL = "sales@sepoasoft.co.kr";
			params.COMPANY_NAME_LOC = document.getElementsByTagName("input")[0].value;
			params.MANAGER = document.getElementsByTagName("input")[1].value;
			params.MANAGER_EMAIL = document.getElementsByTagName("input")[2].value;
			params.PHONE = document.getElementsByTagName("input")[3].value;
			params.WORKFORCE = document.getElementsByTagName("select")[0].value;
			params.INFLOW = document.getElementsByTagName("select")[1].value;
			if(params.INFLOW.includes("기타")) {
				params.INFLOW = document.getElementsByTagName("input")[4].value;
			}
			params.CONTENTS = document.getElementsByTagName("textarea")[0].value;
			params.AGREEMENT_FLAG = document.getElementsByTagName("input")[5].checked;
			params.PAGE = "PoaSeal";

			if (params.COMPANY_NAME_LOC == "" || params.MANAGER == "" || params.MANAGER_EMAIL == ""
				|| params.PHONE == "" || params.WORKFORCE == "" || params.CONTENTS == "" || !params.AGREEMENT_FLAG) {
				
				var resData = getText(params);
				var test = resData[0];
				document.getElementById("err-null-inner").innerHTML = test;
				coll.style.display = "flex";
				coll.style.flexDirection = "column";
				coll.style.alignItems = "flex-end";
				return;
			}
			
			if (coll.style.display != "none") coll.style.display = "none";
			
			doServiceAjax("sepoa.svc.sendInquiry.sendInquiry", "NONDBJOB", "sendMail", params, 
				function(SepoaOut){
			        if (SepoaOut.status == '1') {
			        	alert('성공적으로 전송하였습니다.',function(){
			        		location.href = "/";
			        	});
			        }else{
			        	alert((SepoaOut.message || '').replaceAll("<br>","\n"));
			        	document.getElementById("err-mail-inner").innerHTML = (SepoaOut.message || '').replaceAll("<br>","\n");
			        	coll2.style.display = "flex";
						coll2.style.flexDirection = "column";
						coll2.style.alignItems = "flex-end";
			        }
	        });
		}
	</script>
	<script>
		function deleteBadge(e) {
			e.querySelector('.badge').style.display = 'none';
		}
	</script>
	<% if("010".equals(houseCode)){ %>
	<!-- Global site tag (gtag.js) - Google Ads: 598830872 -->
	<script async src="https://www.googletagmanager.com/gtag/js?id=AW-598830872"></script>
	<script>
	    window.dataLayer = window.dataLayer || [];
	    function gtag(){dataLayer.push(arguments);}
	    gtag('js', new Date());
	
	    gtag('config', 'AW-598830872');
	</script>
	
	<!-- Event snippet for 전자인장_페이지 조회 conversion page -->
	<script>
	    gtag('event', 'conversion', {'send_to': 'AW-598830872/w5AOCMGp650DEJjexZ0C'});
	</script>
	<% } %>
</head>

<body>
	<div id="header" class="sepoa-top-header">
		<img id="logoImg" onclick="clicked(this.id)" class="header-logo" src="./img/logo.png" alt="logo">
		<!-- <button id="service_page" onclick="clicked(this.id)"
			class="header-button header-button-service sepoa-text">서비스소개</button>
		<button id="faq_page" onclick="clicked(this.id)" class="header-button header-button-faq sepoa-text text-blur">FAQ</button>
		<button id="notice_page" onclick="clicked(this.id)"
			class="header-button header-button-notice sepoa-text text-blur">공지사항</button>
		<button id="free_page" onclick="clicked(this.id)"
			class="header-button header-button-freetrial sepoa-text text-blur">무료체험</button> -->
		<button id="login_page" onclick="clicked(this.id)" class="header-button header-button-login sepoa-text">
			<div class="login_icon"></div>
			<div id="login_text">로그인</div>
		</button>
	</div>
	<div id ="fullpage" class="temp">
	<section id="page1" class="section page1">
		<div class="sepoa-index-home-image-container">
			<img class="image-main" src="img/main_1.png" alt="Img1" />
			<div class="sepoa-index-home-image-text">
				<div class="sepoa-text sepoa-text-L-intro">
					인감사용부터 관리까지 인감의<br>모든 정보를 종합적으로 관리합니다.
				</div>
				<div class="sepoa-text sepoa-text-M-intro">
					전자인장관리솔루션 도입으로 비대면 업무환경을 위한 날인업무의<br>
					디지털화 반복날인, 서류발급 및 보관의 불편함 해소
				</div>
				<div class="sepoa-index-home-image-button-area">
					<div id="service_guide" onclick="clicked(this.id)" class="sepoa-index-home-image-button sepoa-text">
						<img class="" src="<%=CONTEXT_NAME%>/010/form/default/img/btn_service_guide.png">
						소개서<br>다운로드
					</div>
					<div id="ytb_page" onclick="clicked(this.id)" class="sepoa-index-home-image-button sepoa-text">
						<img class="" src="<%=CONTEXT_NAME%>/010/form/default/img/btn_video.png">
						유튜브<br>영상보기
					</div>
				</div>
			</div>
			<div class="sepoa-text sepoa-text-M sepoa-text-help">
				<p id="sepoa-text-help-title">서비스상담</p>
				<p id="sepoa-text-help-phone">T. 02.6242.3094</p>
				<p id="sepoa-text-help-email">sales@sepoasoft.co.kr</p>
			</div>
		</div>
	</section>
	<section id="page2" class="section page2">
		<div class="sepoa-text" style="margin-bottom: 25px;">
			<div class="sepoa-text-L opacity-0">
				전자인장관리 솔루션 기대효과
			</div>
		</div>
		<div class="sepoa-index-home-image-container">
			<div class="image-container-row">
				<div class="sepoa-expect sepoa-expect-image-1 opacity-0"></div>
				<div class="sepoa-expect sepoa-expect-image-2 opacity-0"></div>
			</div>
			<div class="image-container-row">
				<div class="sepoa-expect sepoa-expect-image-3 opacity-0"></div>
				<div class="sepoa-expect sepoa-expect-image-4 opacity-0"></div>
			</div>
		</div>
	</section>
	<section id="page3" class="section page3">
		<div id="page3_inner">
			<div id="page3_inner_col">
				<div class="sepoa-text">
					<div class="sepoa-text-L opacity-0" style="text-align:left;font-weight: 500;">
						<span style="font-size:1.1em;font-weight: 600;">EasySeal</span>은<br>업무 효율성을 높입니다.
					</div>
					<div class="sepoa-text-S sepoa-text-child opacity-0" style="text-align:left;">
						법인인감과 사용인감을 사용하고자 하는 경우 조직 내 많은<br>
						사용자들이 오프라인으로 업무를 진행하던 것을 온라인<br>
						시스템화하여 업무의 효율성을 증대시킵니다.
					</div>
				</div>
			</div>
			<div id="page3_inner_col">
				<div class="sepoa-index-home-image-container">
					<div class="image-container-row">
						<div class="sepoa-efficiency sepoa-efficiency-image-1 opacity-0">
							<img src="<%=CONTEXT_NAME%>/010/form/default/img/efficiency/efficiency01.png">
						</div>
						<div class="sepoa-efficiency sepoa-efficiency-image-2 opacity-0">
							<img src="<%=CONTEXT_NAME%>/010/form/default/img/efficiency/efficiency02.png">
						</div>
					</div>
				</div>
			</div>
		</div>
	</section>
	<section id="page4" class="section page4">
		<div class="sepoa-index-home-image-container">
			<img class="image-main" src="img/target/target_background.png" alt="seal on the smartphone" />
			<div class="sepoa-index-home-image-text">
				<div class="sepoa-text" style="margin-bottom: 50px;">
					<div class="sepoa-text-L opacity-0">
						전자인장관리 적용대상
					</div>
					<span class="sepoa-text-S sepoa-text-child opacity-0">
						기업에서 오프라인으로 관리하는 인감(법인인감, 사용인감) 사용신청을 전자인장을 통해 관리함으로써<br>
						인감관리 규정준수 및 업무 효율성을 극대화할 수 있습니다
					</span>
				</div>
			</div>
			<div class="sepoa-index-home-image-sub-container">
				<div class="image-container-row">
					<div class="sepoa-target sepoa-target-image-1 opacity-0"></div>
					<div class="sepoa-target sepoa-target-image-2 opacity-0"></div>
				</div>
				<div class="image-container-row">
					<div class="sepoa-target sepoa-target-image-3 opacity-0"></div>
					<div class="sepoa-target sepoa-target-image-4 opacity-0"></div>
				</div>
			</div>
		</div>
	</section>
	<section id="page5" class="section page5">
		<div class="sepoa-text"> 
			<span class="sepoa-text-L opacity-0">프로세스</span><br>
			<span class="sepoa-text-S sepoa-text-child opacity-0">
				전자인장관리솔루션 도입으로 날인업무(출력-날인-스캔)를 전자화함으로써 비용과 시간을 절약하여 업무의 효율성을 증대시킵니다.
			</span>
		</div>
		<div class="sepoa-index-home-image-container">
			<div class="sepoa-process sepoa-process-image-1 opacity-0"></div>
		</div>
	</section>
	<section id="page6" class="section page6">
		<div class="sepoa-text"> 
			<span class="sepoa-text-L opacity-0">타사와 차별화</span><br>
			<span class="sepoa-text-S sepoa-text-child opacity-0">
				문서에 인감 이미지만 삽입하는 단순한 기능 제공이 아니라 인장관리 전체 업무 프로세스를 지원합니다.<br>
				또한 구축형 솔루션 제공과 호스팅 형태의 서비스 모두 제공합니다.
			</span>
		</div>
		<div class="sepoa-index-home-image-container">
			<div class="sepoa-difference sepoa-difference-image-1"></div>
		</div>
	</section>
	
	<section id="page7" class="section page7">
		<div class="sepoa-text"> 
 			<!-- <span class="sepoa-text-S opacity-0">S 많은 기업에서 전자인장관리 솔루션을 이용하고 있습니다</span><br> -->
			<!-- <span class="sepoa-text-M opacity-0">M 많은 기업에서 전자인장관리 솔루션을 이용하고 있습니다</span><br> -->
			<!-- <span class="sepoa-text-L opacity-0">많은 기업에서 <b>전자인장관리 솔루션</b>을 이용하고 있습니다</span><br> -->
			<span class="sepoa-text-L-custom opacity-0">많은 기업에서 <b>전자인장관리 솔루션</b>을 이용하고 있습니다</span><br>
		</div>	
			<div class="sepoa-index-home-image-container">
				<div class="sepoa-customer sepoa-customer-image-1 opacity-0"></div>
			</div>
	</section>
	
	<section id="page8" class="section page8">
		<div class="sepoa-text"> 
			<span class="sepoa-text-L opacity-0">Why EasySeal?</span>
		</div>
		<div class="sepoa-tab-btn-container">
			<button id="sepoa-tab-btn-economy" class="sepoa-tab-btn opacity-0" onclick="selectTab(this);">경제적인 측면</button>
			<button id="sepoa-tab-btn-process" class="sepoa-tab-btn opacity-0" onclick="selectTab(this);">프로세스 측면</button>
			<button id="sepoa-tab-btn-tech" class="sepoa-tab-btn opacity-0" onclick="selectTab(this);">기술적인 측면</button>
		</div>
        <div class="sepoa-index-home-image-container-wrapper">
			<div class="button-wrapper left-area opacity-0">
	        	<div class="button button-left" onclick="moveTab('prev');"></div>
	        </div>
			<div class="sepoa-index-home-image-container">
				<div class="sepoa-tab-contents" id="sepoa-tab-contents-economy" style="display: flex; justify-content: space-between; width : 100%">
					<div class="sepoa-reason img-size-option sepoa-reason-image-economy-1 opacity-0">
						<img src="<%=CONTEXT_NAME%>/010/form/default/img/reason/reason_economy01.png" style ="border-radius: 10%;">
					</div>
					<div class="sepoa-reason img-size-option sepoa-reason-image-economy-2 opacity-0">
						<img src="<%=CONTEXT_NAME%>/010/form/default/img/reason/reason_economy02.png" style ="border-radius: 10%;">
					</div>
					<div class="sepoa-reason img-size-option sepoa-reason-image-economy-3 opacity-0">
						<img src="<%=CONTEXT_NAME%>/010/form/default/img/reason/reason_economy03.png" style ="border-radius: 10%;">
					</div>
				</div>
				<div class="sepoa-tab-contents" id="sepoa-tab-contents-process" style="display: flex; justify-content: space-between; width : 100%">
					<div class="sepoa-reason img-size-option sepoa-reason-image-process-1 opacity-0">
						<img src="<%=CONTEXT_NAME%>/010/form/default/img/reason/reason_process01.png" style ="border-radius: 10%;">
					</div>
					<div class="sepoa-reason img-size-option sepoa-reason-image-process-2 opacity-0">
						<img src="<%=CONTEXT_NAME%>/010/form/default/img/reason/reason_process02.png" style ="border-radius: 10%;">
					</div>
					<div class="sepoa-reason img-size-option sepoa-reason-image-process-3 opacity-0">
						<img src="<%=CONTEXT_NAME%>/010/form/default/img/reason/reason_process03.png" style ="border-radius: 10%;">
					</div>
				</div>
				<div class="sepoa-tab-contents" id="sepoa-tab-contents-tech" style="display: flex; justify-content: space-between; width : 100%">
					<div class="sepoa-reason img-size-option sepoa-reason-image-tech-1 opacity-0">
						<img src="<%=CONTEXT_NAME%>/010/form/default/img/reason/reason_tech01.png" style ="border-radius: 10%;">
					</div>
					<div class="sepoa-reason img-size-option sepoa-reason-image-tech-2 opacity-0">
						<img src="<%=CONTEXT_NAME%>/010/form/default/img/reason/reason_tech02.png" style ="border-radius: 10%;">
					</div>
					<div class="sepoa-reason img-size-option sepoa-reason-image-tech-3 opacity-0">
						<img src="<%=CONTEXT_NAME%>/010/form/default/img/reason/reason_tech03.png" style ="border-radius: 10%;">
					</div>
				</div>
			</div>
			<div class="button-wrapper right-area opacity-0">
	        	<div class="button button-right" onclick="moveTab('next');"></div>
	        </div>
        </div>
	</section>
	<section class="section sepoa-bottom">
		<div class="sepoa-bottom-widget">
			<div class="sepoa-bottom-widget-text-container">
				<div class="sepoa-text sepoa-text-L sepoa-bottom-widget-text-L opacity-0">
					전문가가 빠른 상담을 <br> 도와드립니다.
				</div>
				<div class="sepoa-text sepoa-text-S sepoa-bottom-widget-text-S opacity-0">
					입력하신 정보를 토대로 담당자가 1:1 맞춤 상담을<br>도와드립니다.
				</div>
			</div>
			<div class="sepoa-bottom-widget-ask opacity-0">
				<input id="comp_name" type=text Placeholder="회사명"
					class="sepoa-bottom-widget-ask-input ask-input-company-name" required>
				<div>
					<input id="manager" type=text Placeholder="담당자" class="sepoa-bottom-widget-ask-input" required>
					<select name="member_cnt" id="member_cnt" class="sepoa-text sepoa-bottom-widget-ask-select">
						<option value="" disabled="" selected="">직원수</option>
						<option value="3_5">3 ~ 5명</option>
						<option value="6_10">6 ~ 10명</option>
						<option value="11_15">11 ~ 15명</option>
						<option value="16_20">16 ~ 20명</option>
						<option value="21_30">21 ~ 30명</option>
						<option value="31_50">31 ~ 50명</option>
						<option value="51_100">51 ~ 100명</option>
						<option value="100_up">100명 이상</option>
					</select>
				</div>
				<div>
					<input id="email" type=text Placeholder="이메일" class="sepoa-bottom-widget-ask-input" required>
					<input id="phone_no" type=text Placeholder="전화번호" class="sepoa-bottom-widget-ask-input" required>
				</div>
				<div>
					<select name="inflow" id="inflow" class="sepoa-text sepoa-bottom-widget-ask-select sepoa-bottom-widget-ask-inflow">
						<option value="" disabled="" selected="">유입경로</option>
						<option value="구글 검색광고">구글 검색광고</option>
						<option value="구글 블로그(ex.웹사이트 글,tistory)">구글 블로그(ex.웹사이트 글,tistory)</option>
						<option value="네이버 검색광고(ex.파워링크)">네이버 검색광고(ex.파워링크)</option>
						<option value="네이버 블로그">네이버 블로그</option>
						<option value="네이버 카페">네이버 카페</option>
						<option value="SNS(ex.페이스북,인스타그램,LinkedIn,유튜브)">SNS(ex.페이스북,인스타그램,LinkedIn,유튜브)</option>
						<option value="이메일광고">이메일광고</option>
						<option value="자동차광고">자동차광고</option>
						<option value="신문광고">신문광고</option>
						<option value="기타(직접입력)">기타(직접입력)</option>
					</select>
				</div>
				<div>
					<input id="inflow2" type=text class="sepoa-bottom-widget-ask-input ask-input-inflow">
				</div>
				<div>
					<textarea id="content" Placeholder="문의내용" class="sepoa-bottom-widget-ask-input ask-input-remark" required></textarea>
				</div>
				<div class="sepoa-bottom-widget-ask-check">
					<input id="check" type="checkbox" required> <label for="check"
						class="sepoa-text sepoa-text-S sepoa-bottom-widget-text-S">(필수) 개인정보 수집 및 이용에 동의합니다.</label>					
				</div>
				<button id="ask" onclick="questionEmailSend()" class="sepoa-bottom-widget-ask-button sepoa-text">문의하기</button>		
				<div id="bottom-err-null" class="bottom-err-null collapsible">
					<div id="close_err1" class="close-text">X</div>
					<div id="err-null-inner" class="sepoa-text sepoa-text-S err-text"></div>
				</div>
				<div id="bottom-err-sendmail" class="bottom-err-sendmail collapsible">
					<div id="close_err2" class="close-text">X</div>
					<div id="err-mail-inner" class="sepoa-text sepoa-text-S err-text"></div>
				</div>
			</div>
		</div>
		<div class="sepoa-bottom-copyright">
			<div class="sepoa-bottom-copyright-text">COPYRIGHT 2022 Sepoasoft. All Rights Reserved.
				Powered by Sepoasoft 서울시 서초구 서초대로 122 효찬빌딩 5층 | Tel 02-6242-3094</div>
		</div>
	</section>
	</div>

	<!-- 유튜브 영상보기 Modal -->
	<div id="videoPopup" class="popup_youtube" onclick="closeYoutube(this);">
		<div class="popup-content_youtube">
			<iframe style="width: 100%;height: 100%;" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe>
		</div>
	</div>
	
	<!-- 네이버 애널리틱스  -->
	<script type="text/javascript" src="//wcs.naver.net/wcslog.js"></script>
	<script type="text/javascript">
		if(!wcs_add) var wcs_add = {};
		wcs_add["wa"] = "105eead086b4110";
		if(window.wcs) {
		  wcs_do();
		}
	</script>
</body>

</html>
