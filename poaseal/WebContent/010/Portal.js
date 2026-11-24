// page7 선택
function selectTab(o) {
	var tabBtnPrefix = 'sepoa-tab-btn';				// prefix 값은 상단버튼의 공통내용
	var tabContentsPrefix = 'sepoa-tab-contents'; // 실제 이미지들이 날여되는곳 (3개)
	var btnId = o.id;								// sepoa-tab-btn-economy, process, tech 
	var tabId = btnId.replace(tabBtnPrefix+'-', ''); // 'sepoa-tab-btn-  -> '' 공백으로 변경해서 economy, prcess, tech만 저장
	$('.'+tabBtnPrefix).removeClass('active');		// 불들어온거 active꺼주고
	$('.'+tabContentsPrefix).hide();				
	$('#'+btnId).addClass('active');
	var $selectedTabContent = $('#'+tabContentsPrefix+'-'+tabId); 
	$selectedTabContent.show();
	var $selectedTabContentImages = $selectedTabContent.find('div.sepoa-reason');
	for (var i = 0; i < $selectedTabContentImages.length; i++) {
		(function(x){
			setTimeout(function(){
				var $contentImage = $($selectedTabContentImages[x]); 
				$contentImage.addClass('showFromBottom');
				$contentImage.removeClass('opacity-0');					
			}, 500 * (x+1));
		})(i)
	}
}
// page7 버튼이동
function moveTab(direction) {
	var $tabBtnContainer = $('div.sepoa-tab-btn-container');
	var $tabBtns = $tabBtnContainer.find('button');
	var btnIdx = $tabBtnContainer.find('button.active').index();
	if (direction == 'prev') {
		var targetBtnIdx = btnIdx - 1;
	} else {
		var targetBtnIdx = btnIdx + 1;
	}
	var btnLen = $tabBtns.length; // 3이여야 정상작동
	if (targetBtnIdx < 0) {
		targetBtnIdx = btnLen - 1;
	} else if (targetBtnIdx > btnLen-1) {
		targetBtnIdx = 0;
	}
	$($tabBtns[targetBtnIdx]).click();
}

//

//page2 select*히든상태임
function imgTab(o) {
	var tabBtnPrefix = 'sepoa-tab-btn';
	var tabContentsPrefix = 'sepoa-tab-contents'; 
	var btnId = o.id;
	var tabId = btnId.replace(tabBtnPrefix+'-', '');
	$('.'+tabBtnPrefix).removeClass('active');
	$('.'+tabContentsPrefix).hide();				
	$('#'+btnId).addClass('active');
	var $selectedTabContent = $('#'+tabContentsPrefix+'-'+tabId); 
	$selectedTabContent.show();
	var $selectedTabContentImages = $selectedTabContent.find('div.sepoa-reason');
	for (var i = 0; i < $selectedTabContentImages.length; i++) {
		(function(x){
			setTimeout(function(){
				var $contentImage = $($selectedTabContentImages[x]); 
				$contentImage.addClass('showFromBottom');
				$contentImage.removeClass('opacity-0');					
			}, 500 * (x+1));
		})(i)
	}
}

// page2 < > 버튼이동
function ImgMoveTab(direction) {
	var $tabBtnContainer = $('div.sepoa-tab-btn-container-1');
	var $tabBtns = $tabBtnContainer.find('button');
	var btnIdx = $tabBtnContainer.find('button.active').index();
	if (direction == 'prev') {
		var targetBtnIdx = btnIdx - 1;
	} else {
		var targetBtnIdx = btnIdx + 1;
	}
	var btnLen = $tabBtns.length; //  2 여야 정상ㅈㄱ동
	if (targetBtnIdx < 0) {
		targetBtnIdx = btnLen - 1;
	} else if (targetBtnIdx > btnLen-1) {
		targetBtnIdx = 0;
	}
	$($tabBtns[targetBtnIdx]).click();
}

function getText(params) {
	var text = "";
	var text2 = "";
	var resFlag = true;
	var flag = true;
	var cnt = 0;
	var res = ["", true, true, true, true, true, true, true, true];
	if (params.COMPANY_NAME_LOC == "") {
		text += "회사명";
		cnt++; 
		resFlag = false; 
		res[1] = false;
	} if (params.MANAGER == "") {
		if (cnt > 0) text += ", 이름";
		else text += "이름";
		cnt++; 
		resFlag = false;
		res[2] = false;
	} if (params.MANAGER_EMAIL == "") {
		if (cnt > 0) text += ", 이메일";
		else text += "이메일";
		cnt++; 
		resFlag = false;
		res[3] = false;
	} if (params.PHONE == "") {
		if (cnt > 0) text += ", 전화번호";
		else text += "전화번호";
		cnt++; 
		flag = false; resFlag = false;
		res[4] = false;
	} if (params.WORKFORCE == "") {
		if (cnt > 0) text += ", 직원 수";
		else text += "직원 수";
		cnt++; 
		flag = false; resFlag = false;
		res[5] = false;
	} if (params.INFLOW == "") {
		if (cnt > 0) text += ", 유입경로";
		else text += "유입경로";
		cnt++; 
		flag = false; resFlag = false;
		res[6] = false;
	} if (params.CONTENTS == "") {
		if (cnt > 0) text += ", 문의내용";
		else text += "문의내용";
		cnt++; 
		flag = true; resFlag = false;
		res[7] = false;
	}
	if (flag) text += "이 필요합니다.";
	else text += "가 필요합니다.";

	if (!params.AGREEMENT_FLAG) {
		text2 += "개인정보 이용 동의가 필요합니다.";
		res[8] = false;
	}

	if (resFlag) res[0] = text2;
	else res[0] = text;

	return res;
}

//버튼 클릭
function clicked(id) {
	if (id == 'logoImg') {
		location.href = "/010/form/default/Portal.jsp?house_code=010";
	} else if (id == 'login_page') {
		location.href = "/010/form/default/login.jsp?house_code=010";
	} else if (id == 'service_page') {
		Alert('준비중입니다.');
	} else if (id == 'faq_page') {
		Alert('준비중입니다.');
	} else if (id == 'notice_page') {
		Alert('준비중입니다.');
	} else if (id == 'free_page') {
		Alert('준비중입니다.');
	} else if (id == 'service_guide') {
		var option = "target = blank";
		window.open("/010/form/default/service_guide.pdf", '소개서', option);
	} else if (id == 'ytb_page') {
		var popup = document.getElementById('videoPopup');
		var youtube = popup.getElementsByTagName('iframe')[0];
		if (youtube) {
			youtube.src = 'https://www.youtube.com/embed/CoVFMEMxm6k';
		}
		popup.style.display = 'block';
	} else {
		alert("wrong id!");
	}
}



//fullpage.js 
$(function(){
	$('#fullpage').fullpage({
		autoScrolling: true,
		continuousHorizontal: false,
		continuousVertical: false,
		touchSensitivity: 1,
		navigation: true,
		scrollingSpeed: 700,
		bigSectionsDestination: top,
		paddingTop: '100px',
		onLeave: function(origin, destination, direction){
			console.log(destination)
			switch (destination+'') {
			case '1':
				break;
			case '2':
				setTimeout(function(){
					$('.page2 .sepoa-text-L').addClass('showText');
					$('.page2 .sepoa-text-L').removeClass('opacity-0');				
					$('.page2 .sepoa-text .sepoa-text-M').addClass('showText');
					$('.page2 .sepoa-text .sepoa-text-M').removeClass('opacity-0');
					
					$('.page2 .sepoa-tab-btn').addClass('showFromBottom');
					$('.page2 .sepoa-tab-btn').removeClass('opacity-0');
					$('.page2 .sepoa-tab-btn-container-1 .sepoa-tab-btn:first-child').click();
					$('.page2 .button-wrapper').addClass('showFromBottom');
					$('.page2 .button-wrapper').removeClass('opacity-0');
					
				}, 500);
			case '3':
				setTimeout(function(){
					$('.page3 .sepoa-text-L').addClass('showText');
					$('.page3 .sepoa-text-L').removeClass('opacity-0');						
				}, 500);
				
				setTimeout(function(){
					$('.page3 .sepoa-text-S').addClass('normalFadeInEffect');
					$('.page3 .sepoa-text-S').removeClass('opacity-0');						
				}, 1000);								
				break;
			case '4':
				setTimeout(function(){
					$('.page4 .sepoa-text-L').addClass('showText');
					$('.page4 .sepoa-text-L').removeClass('opacity-0');					
				}, 500);
				
				setTimeout(function(){
					$('.page4 .sepoa-text-S').addClass('normalFadeInEffect');
					$('.page4 .sepoa-text-S').removeClass('opacity-0');						
				}, 1000);	
			case '5':
				setTimeout(function(){
					$('.page5 .sepoa-text-L').addClass('showText');
					$('.page5 .sepoa-text-L').removeClass('opacity-0');					
				}, 500);
				
				setTimeout(function(){
					$('.page5 .sepoa-text-S').addClass('normalFadeInEffect');
					$('.page5 .sepoa-text-S').removeClass('opacity-0');						
				}, 1000);
				
				break;				
			case '6':
				setTimeout(function(){
					$('.page6 .sepoa-text-L').addClass('showText');
					$('.page6 .sepoa-text-L').removeClass('opacity-0');					
				}, 500);
				
				setTimeout(function(){
					$('.page6 .sepoa-text-S').addClass('normalFadeInEffect');
					$('.page6 .sepoa-text-S').removeClass('opacity-0');						
				}, 1000);
				
				break;
			case '7':
				setTimeout(function(){
					$('.page7 .sepoa-text-L-custom').addClass('showText');
					$('.page7 .sepoa-text-L-custom').removeClass('opacity-0');
					
					$('.page7 .sepoa-text-L').addClass('showText');
					$('.page7 .sepoa-text-L').removeClass('opacity-0');
					
					$('.page7 .sepoa-text .sepoa-text-M').addClass('showText');
					$('.page7 .sepoa-text .sepoa-text-M').removeClass('opacity-0');
					
					$('.page7 .sepoa-text-S').addClass('normalFadeInEffect');
					$('.page7 .sepoa-text-S').removeClass('opacity-0');	
					
				}, 500);
				break;
			case '8':
				setTimeout(function(){
					$('.page8 .sepoa-text-L').addClass('showText');
					$('.page8 .sepoa-text-L').removeClass('opacity-0');					
				}, 500);
				setTimeout(function(){
					$('.page8 .sepoa-tab-btn').addClass('showFromBottom');
					$('.page8 .sepoa-tab-btn').removeClass('opacity-0');
					$('.page8 .sepoa-tab-btn-container .sepoa-tab-btn:first-child').click();
					
					$('.page8 .button-wrapper').addClass('showFromBottom');
					$('.page8 .button-wrapper').removeClass('opacity-0');
				}, 1000);
				
				break;	
			case '9':
				setTimeout(function(){
					$('.sepoa-bottom .sepoa-bottom-widget-text-L').addClass('showText');
					$('.sepoa-bottom .sepoa-bottom-widget-text-L').removeClass('opacity-0');				
				}, 500);				

				setTimeout(function(){
					$('.sepoa-bottom .sepoa-bottom-widget-text-container .sepoa-bottom-widget-text-S').addClass('normalFadeInEffect');
					$('.sepoa-bottom .sepoa-bottom-widget-text-container .sepoa-bottom-widget-text-S').removeClass('opacity-0');						
				}, 1000);
				
				setTimeout(function(){
					$('.sepoa-bottom .sepoa-bottom-widget-ask').addClass('normalFadeInEffect');
					$('.sepoa-bottom .sepoa-bottom-widget-ask').removeClass('opacity-0');						
				}, 1500);
				break;					
			}
		},
		afterLoad: function(origin, destination, direction){
			switch (destination+'') {
			case '1':		
				break;
			case '2':
				var images = document.querySelectorAll('.page2 .sepoa-expect');			
				for(var i= 0; i<images.length;i++){
					setTimeout(function(div){
						div.classList.add('showFromBottom');
						div.classList.remove('opacity-0');						
					}, 500 * (i+1), images[i]);
				}
				break;
			case '3':
				var images = document.querySelectorAll('.page3 .sepoa-efficiency');
				for(var i= 0; i<images.length;i++){
					setTimeout(function(div){
						div.classList.add('showFromBottom');
						div.classList.remove('opacity-0');						
					}, 500 * (i+1), images[i]);
				}				
				break;
			case '4':
				var images = document.querySelectorAll('.page4 .sepoa-target');
				for(var i= 0; i<images.length;i++){
					setTimeout(function(div){
						div.classList.add('showFromBottom');
						div.classList.remove('opacity-0');						
					}, 500 * (i+1), images[i]);
				}				
				break;				
			case '5':
				var images = document.querySelectorAll('.page5 .sepoa-process');
				for(var i= 0; i<images.length;i++){
					setTimeout(function(div){
						div.classList.add('showFromBottom');
						div.classList.remove('opacity-0');						
					}, 500 * (i+1), images[i]);
				}				
				break;	
			case '6':
				var images = document.querySelectorAll('.page6 .sepoa-difference');
				for(var i= 0; i<images.length;i++){
					setTimeout(function(div){
						div.classList.add('showFromBottom');
						div.classList.remove('opacity-0');
					}, 500 * (i+1), images[i]);
				}				
				break;
			case '7':
				var images = document.querySelectorAll('.page7 .sepoa-customer');
				for(var i= 0; i<images.length;i++){
					setTimeout(function(div){
						div.classList.add('showFromBottom');
						div.classList.remove('opacity-0');
					}, 500 * (i+1), images[i]);
				}				
				break;
			case '9':
				$("#inflow").change(function(){
					if($(this).val().includes("기타")){
						$("#inflow2").css("display", "inline-block");
					} else { 
						$("#inflow2").css("display", "none");
					}
				});
				break;
			case '7':
				var images = document.querySelectorAll('.page6 .sepoa-reason');
				for(var i= 0; i<images.length;i++){
					setTimeout(function(div){
						div.classList.add('showFromBottom');
						div.classList.remove('opacity-0');
					}, 500 * (i+1), images[i]);
				}				
				break;
			}			
		}
	});
});

function closeYoutube(e) {
	$('.popup-content_youtube iframe').each(function(){
		  this.contentWindow.postMessage('{"event":"command","func":"stopVideo","args":""}', '*')
	});
	document.getElementById('videoPopup').style.display = 'none';
}
