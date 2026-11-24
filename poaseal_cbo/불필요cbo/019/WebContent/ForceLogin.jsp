<%@ page import="java.util.*" %>
<%@ page import="sepoa.fw.util.*" %>
<%@ page import="sepoa.fw.srv.*" %>
<%@ page import="sepoa.fw.ses.*" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String logonId = "";         // 로그온 된 사용자 아이디
    String scriptMsg = "";       // 에러 메시지
    
    try {
    	logonId = request.getParameter("LOGIN_ID");
        
        if(!logonId.equals("") && logonId != null){
    		Map<String, String> headerData = new HashMap<String, String>();
    		headerData.put("ID", logonId);

    		Object[] o = new Object[]{headerData};
    		sepoa.fw.ses.SepoaInfo info = sepoa.fw.ses.SepoaSession.getAllValue(request); 
    		SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION", "forceLogin", o);
    		String flag = String.valueOf(so.flag);
    		
    		if(flag.equals("true")){
    			scriptMsg += "goLogonPage();";
    		} else{
    			scriptMsg += "OnLogoff();";
    		}
    	}
%>
<!DOCTYPE html>
<head>
    <title>NETS*SSO</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <script language="javascript" type="text/javascript">
        //에러 메시지
        function alertError(msg, url) {
            if (msg != "")
                alert(msg);
            if (url != "")
                document.location.href = url;
        }
        //로그오프
        function OnLogoff() {
            document.location.href = "/";
        }
        //로그인 페이지 이동
        function goLogonPage() {
            window.location.href = "/Main.sux";
        }
    </script>
</head>
<body>
<script language="javascript"><%=scriptMsg%>
</script>


</body>
</html>
<%
} catch (Exception e) {
    e.printStackTrace();
%><%=e.toString()%><%
}
%>



