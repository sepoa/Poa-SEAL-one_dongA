<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib prefix="c" uri="/WEB-INF/classes/config/tlds/c.tld"%>
<!DOCTYPE html>
<html>
<OBJECT ID="EpAdmC" HEIGHT=0 WIDTH=0 CLASSID="CLSID:C63E3330-049F-4C31-B47E-425C84A5A725"></OBJECT>
<title><c:out value="${LANG['lable.common.system.title']}"/></title>    
<head>
<script language="javaScript">

	var Browser = new Object();
	var userAgent = navigator.userAgent.toLowerCase();
	var isIE = userAgent.indexOf('trident');
	
	function NoIElogin(){	
		
		var websocket = new WebSocket("wss://localhost:29283"); 
		
		websocket.onopen = function (event) {
			websocket.send('{"rqtype":"getknoxsso","token":"","data":"KCC60TRAY0002"}');			
		}
		
		//웹소켓 응답 메시지 처리
		websocket.onmessage = function (event) {
		
			var rpJSON = event.data;  
			
			console.log(rpJSON);
			
		    //정상 리턴 == {"data":"{\"result\":\"success\",\"userInfo\":\"~~~",\"key\":\"~~~~\"},"rpcode":"RETURN_SUCCESS"}
		    //에러 리턴 == {"data":"{\"result\":\"fail\",\"errorCode\":\"KT007\",\"errorMsg\":\"The system does not exist.\"}","rpcode":"RETURN_SUCCESS"}
		    
			var obj = JSON.parse(rpJSON);
			obj = JSON.parse(obj.data);
			
			if(obj.result == "fail"){
				alert("Code : " +obj.errorCode  + ":" + obj.errorMsg);
				return;
			}
			
			form.TOTALDATA.value = rpJSON;
			form.isIE.value = isIE;
		    form.submit();
		    
		    websocket.close();
		}
		
		//웹소켓 에러 시 이벤트 처리
		websocket.onerror = function (event) {
			var errmsg = event.data;
		}			
	}
	
	function IElogin() {
	
		if(EpAdmC.GetSecureBox() == false) {
	
			alert('mySingle에 로그인하시기 바랍니다..');
			top.location.href = "http://www.samsung.net";
			return;
		
		} else {
			var rrtn = EpAdmC.GetSecureBox();
			
			form.TOTALDATA.value = rrtn;
			form.isIE.value = isIE;
			form.submit();
		}
	}
 
	function init(){
		
		if(isIE > -1){			
			IElogin();
		}else{
			NoIElogin();
		}
	}
	
</script>
</head>
<body onLoad="javascript:init()">
        <form name="form" method="post" action="/cube_hxb/login.do">
           <input type="hidden" name="TOTALDATA" value="">
           <input type="hidden" name="isIE" value="">
        </form>
</body>
</html>

