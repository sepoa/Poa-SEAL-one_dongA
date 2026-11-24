<%@ page language="java" contentType="text/html; charset=EUC-KR"
    pageEncoding="EUC-KR"%>
<%@ page import="java.io.IOException" %>
<%@ page import="java.security.*" %>

<%@ page import="java.security.*" %>

<%@ page import="sga.javax.crypto.*" %>
<%@ page import="sga.javax.crypto.spec.*" %>
<%@ page import="sga.jce.provider.SGAProvider" %>
<%@ page import="sga.jce.provider.util.Hex" %>
<%@ page import="sga.pki.api.PKIConf" %>
<%@ page import="sga.pki.api.exception.PKIException" %>
<%@ page import="sga.pki.api.util.Utils" %>

<%!
	private static Provider _prov = null;

	private static void TRACE(String msg) {
	;
		System.out.println(msg); //
	}

	public static void addProvider() throws PKIException {
		_prov = (Provider) new SGAProvider();
		
		try {
			Security.addProvider(_prov);
		}
		catch (NullPointerException e) {
			TRACE("[EncTest] Add SGAProvider Error : " + e.getMessage());//
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		}
		catch (SecurityException e) {
			TRACE("[EncTest] Add SGAProvider Error : " + e.getMessage());//
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		}

		try {
			PKIConf.init("/root/msp/tc_common/license/license.dat");
		} catch (PKIException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		}
	}
	
	public static String encrypt(byte[] key, byte[] iv, byte[] input) throws PKIException {
		byte[] enc = null;
		Cipher c = null;
	
		try {
			c = sga.javax.crypto.Cipher.getInstance("SEED/CBC/PKCS5", PKIConf.getProvider());
		} catch (NoSuchAlgorithmException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		} catch (NoSuchProviderException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		} catch (NoSuchPaddingException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		}
		
		try {
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "SEED"), new IvParameterSpec(iv));
		} catch (InvalidKeyException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		} catch (InvalidAlgorithmParameterException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		}
		
		try {
			enc = c.doFinal(input);
		} catch (IllegalBlockSizeException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		} catch (BadPaddingException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		}
		
		return Utils.base64Encode(enc);
	}
	
	public static byte[] decrypt(byte[] key, byte[] iv, String encrypt) throws PKIException {
		byte[] dec = null;
		Cipher c = null;
	
		try {
			c = sga.javax.crypto.Cipher.getInstance("SEED/CBC/PKCS5", PKIConf.getProvider());
		} catch (NoSuchAlgorithmException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		} catch (NoSuchProviderException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		} catch (NoSuchPaddingException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		}
		
		try {
			c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "SEED"), new IvParameterSpec(iv));
		} catch (InvalidKeyException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		} catch (InvalidAlgorithmParameterException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		}
		
		try {
			dec = c.doFinal(Utils.base64Decode(encrypt));
		} catch (IllegalBlockSizeException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		} catch (BadPaddingException e) {
			TRACE("[EncTest] Init Toolkit Error : " + e.getMessage());
			//e.printStackTrace();
			throw new PKIException(e.getMessage());
		}
		
		return dec;
	}

%>

<%
	String enc = null;
	byte[] dec = null;
	byte[] ret = null;
	String orgData = "thffntus3xla!";
	String str = null;
	
	try {
		addProvider();
		
		TRACE("[orgData] ORG: " + orgData);
		
		enc = encrypt("8888888888888888".getBytes(), "1111111111111111".getBytes(), orgData.getBytes());
		TRACE("[EncTest] ENC : " + enc);
		
		dec = decrypt("8888888888888888".getBytes(), "1111111111111111".getBytes(), enc);
		str = new String(dec);
		TRACE("[EncTest] DEC : " + str);
		TRACE("[EncTest] DEC(HEX) : " + Hex.encode(dec));
		
	} catch (PKIException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=EUC-KR">
<title>EncTest!</title>
</head>
<body>
[orgData] ORG : <%=orgData%><br>
[EncTest] ENC : <%=enc%><br>
[EncTest] DEC : <%=str%><br>
</body>
</html>
