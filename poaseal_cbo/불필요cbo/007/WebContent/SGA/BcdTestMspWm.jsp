<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>

<%@ page import="com.sga.tc.gs30.ccd.TrustCertCodeVO"%>
<%@ page import="com.sga.tc.gs30.pdf.CertCodePdf"%>
<%@ page import="com.sga.tc.gs30.util.UsingProperties"%>
<%@ page import="com.voiceye.SmartCertCodeZeroJNI"%>
<%@ page import="com.sga.tc.gs30.pdf.watermark.SGAWaterMark"%>    
<%

	String strPath = "D:\\Dev\\03.WorkSpace\\Poa-SEAL-one\\poaseal_cbo\\007\\WebContent\\SGA\\TrustCertificate.xml";
	UsingProperties.getInstance(strPath);


	CertCodePdf ccp = new CertCodePdf();
	String pdfPath = null;
	String resultPdfPath = null;
	String DocNo = null;
	String Subject = null;
	// 1번째 인자(필수) : 소스파일 Path
	// 2번째 인자(필수) : 문서 번호 (중복되지 않는 값 권장. 요청번호 등으로 설정)
	// 3번째 인자(필수) : 문서명 (서식명)
	TrustCertCodeVO vo = new TrustCertCodeVO(pdfPath, DocNo, Subject);
	//////////////////////////////////////////////////////////////////////////////
	// 붙이기(watermark) 방식으로 2D바코드를 PDF에 붙이기
	//////////////////////////////////////////////////////////////////////////////
	// 붙이기(watermark)로 설정

	vo.setWaterMark(true);
	vo.setWmXaxis(30);
    vo.setWmYaxis(30);
	
	// 붙이기 대상 PDF 파일 Path 설정
	vo.setResultPdfPath(resultPdfPath);
	if(!ccp.applyMspCertCode(vo)) {
    	// 에러 처리
		System.out.println("FAIL!");
    	System.out.println(vo);  // <== 고객사 로거(logger)에 출력
	} else {
		System.out.println("SUCCESS!");
		}


	/*========================================
	최종 PDF를 화면에 출력하는 부분(확인용 샘플, 사용X)
	========================================*/
	out.clear();
	out=pageContext.pushBody();
	//생성된 pdf를 화면에
	File downloadFile = new File(resultPdfPath);
	FileInputStream inStream = new FileInputStream(downloadFile);
	
	// obtains response's output stream
	OutputStream outStream = response.getOutputStream();
	
	response.setContentType("application/pdf; charset=UTF-8");
	response.setHeader("Content-Disposition", "filename=" + resultPdfPath);
	response.setHeader("Content-Length", "" + downloadFile.length());
	
	byte[] buffer = new byte[4096];
	int bytesRead = -1;
	
	while ((bytesRead = inStream.read(buffer)) != -1) {
		outStream.write(buffer, 0, bytesRead);
	}

	outStream.close();
	inStream.close();
			

%>






























<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>바코드 삽입테스트</title>
</head>
<body>

</body>
</html>