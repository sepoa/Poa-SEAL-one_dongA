package sepoa.seal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;
import org.apache.tika.io.IOUtils;

import filters.ThreadLocalFilter;
import sepoa.filemanager.FileManager;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.module.SepoaModuleManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.authority.Permission.PrincipalAgentType;
import sepoa.fw.util.authority.SepoaAuthorityManager;
import sepoa.fw.util.authority.SepoaAuthorityObject;
import sepoa.fw.util.authority.SepoaAuthorityObject.Authorized;
import sepoa.fw.ux.link.SepoaLinkManager;

public class SepoaSealManager extends SepoaModuleManager{
	/*
	 * Fields
	 */
	protected FileManager fileManager;
	protected SepoaAuthorityObject  authObject;
	protected SepoaAuthorityManager authManager;
	protected SepoaLinkManager linkManager;
	protected Map<String, String> docBasicInfo;
	protected boolean checkPermission = true;
	protected boolean linkFlag = true;
	private enum ACTION_TYPE{
		SEAL_REQUEST, LEGAL_APP, LEGAL_REJECT, DOC_ATTACH, SEAL_COMPLETE, DOC_DOWNLOAD, CERT_REQUEST, CERT_COMPLETE, DEC_COMPLETE
	}
	
	/*
	 * Constructors 
	 */
	public SepoaSealManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.docBasicInfo = new HashMap<String, String>();
	}
	
	/*
	 * Init
	 */
	protected boolean initFileManager(String docNumber) {
		if(this.fileManager == null) {
			FileManager fm = new FileManager(info, config, this.ctx);
			if (fm.setDocNumber(docNumber, true) == null) {
				return false;
			}else {
				fm.setTempDirectory(this.tempStorage);
				this.fileManager = fm;
				return true;
			}
		} else {
			return true;
		}
	}
	
	/**
	 *  Destory File Manager
	 */
	protected void destoryFileManager() {
		this.fileManager = null;
	}
	
	/**
	 * @param linkType 		Find Page in Properties
	 * @param docNo			Document No
	 * @param seq			Document Seq
	 * @param linkParams	{docNoKeyName : docNoValue , seqKeyName : seqValue}
	 * @return
	 */
	protected boolean initLinkManager(String linkType, String docNo, String seq, Map<String, String> linkParams) {
		if(linkFlag && this.linkManager == null) {
			this.linkManager = new SepoaLinkManager(info, config, ctx);
			if(!this.linkManager.newLink(linkType, docNo, seq) || !this.linkManager.setParams(linkParams)) {
				this.errCode = this.linkManager.getLastErrCode();
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Common 
	 */
	public boolean initAuthManager(String houseCode , String companyCode, String type, String code, String seq) {
		if(checkPermission) {
			if(this.authManager == null) {
				this.authManager = SepoaAuthorityManager.getManager(info, ctx);
			}
			SepoaAuthorityObject authObject = this.authManager.getObject(houseCode, companyCode, type, code, seq);
			if(authObject == null) {
				return false;
			}else {
				this.authObject = authObject; 
			}
		}
		return true;
	}
	
	//사용자 존재여부 검사
	public boolean doCheckExistUser(String houseCode, String companyCode, String userId) {
		Map<String, String> userData = new HashMap<>();
		userData.put("HOUSE_CODE", houseCode);
		userData.put("COMPANY_CODE", companyCode);
		userData.put("USER_ID", userId);
		
		List<Map<String, String>> checkResult = this.doSelectSUSMT(userData);
		if(checkResult == null || checkResult.size() == 0) {
			this.errCode = 102413;
			return false;
		}
		
		return true;
	}
	
	public void setDocBaiscInfo(Map<String, String> docBasicInfo) {
		this.docBasicInfo = docBasicInfo;
	}
	
	public Map<String, String> getDocBaiscInfo() {
		return this.docBasicInfo;
	}

	//권한매니저 권한 존재여부 확인
	public boolean checkAuthManager() {
		if(this.authManager == null) {
			return false;
		} else {
			return true;
		}
	}
	
	//권한 부여
	public boolean grantTo(PrincipalAgentType pa, String companyCode, String userId, Authorized auth) {
		if(this.checkPermission) {
			try {
				authObject.grant(pa, companyCode, userId, auth, null);
			} catch (Exception e) {
				this.errCode = 100003;
				Logger.err.println(this.info.getSession("ID"), this, e);
				return false;
			}
		}
		return true;
	}
	
	//권한 회수
	public boolean revokeFrom(PrincipalAgentType pa, String companyCode, String userId) {
		if(this.checkPermission) {
			try {
				authObject.revoke(pa, companyCode, userId);
			} catch (Exception e) {
				this.errCode = 100005;
				Logger.err.println(this.info.getSession("ID"), this, e);
				return false;
			}
		}
		return true;
	}
	
	//전체 사용자 권한 회수
	public boolean revokeAll(boolean ownerReovkeFlag) {
		if(this.checkPermission) {
			try {
				authObject.revokeAll(ownerReovkeFlag);
			} catch (Exception e) {
				this.errCode = 100005;
				Logger.err.println(this.info.getSession("ID"), this, e);
				return false;
			}
		}
		return true;
	}
	
	//반출된 인감인지 조회
		public String doSearchExtUse(Map<String, String> data){
			String resultData = this.doSearchExtUseList(data);
			
			if(resultData == null) {
				return null;
			}
			
			return resultData;
		}
	
	//반출된 인감인지 조회
	private String doSearchExtUseList(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			return ssm.doSelect(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
		
	//File to Binary
	protected String fileToBinaryString(String filePath) throws Exception {
		String out = new String();
		FileInputStream fis = new FileInputStream(new File(filePath));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		int len = 0;
		byte[] buf = new byte[1024];
		while ((len = fis.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
 
        byte[] fileArray = baos.toByteArray();
        out = new String(Base64.encodeBase64(fileArray));
 
        fis.close();
        baos.close();
		return out;
	}
	
	public List<Map<String, String>> doQueryProcessHistory(Map<String, String> extraData) {
		
		extraData.putAll(docBasicInfo);
		
		List<Map<String, String>> list = doSelectSPMGT(extraData);
		
		if (list == null || list.size() < 1) {
			this.errCode = 103332;
			return null;
		}else {
			return list;
		}
		
	}	
	
	
	public boolean doAddProcessHistory(String action) {
		Map<String, String> paramsMap = new HashMap<String, String>();
		boolean resultFlag = false;
		
		switch(action.toUpperCase()) {
			case "CERT_REQUEST" :
				paramsMap.put("ACTION", ACTION_TYPE.CERT_REQUEST.name());
				break;
			case "SEAL_REQUEST" :
				paramsMap.put("ACTION", ACTION_TYPE.SEAL_REQUEST.name());
				break;
			case "LEGAL_APP" :
				paramsMap.put("ACTION", ACTION_TYPE.LEGAL_APP.name());
				break;
			case "LEGAL_REJECT" :
				paramsMap.put("ACTION", ACTION_TYPE.LEGAL_REJECT.name());
				break;
			case "DOC_ATTACH" :
				paramsMap.put("ACTION", ACTION_TYPE.DOC_ATTACH.name());
				break;
			case "SEAL_COMPLETE" :
				paramsMap.put("ACTION", ACTION_TYPE.SEAL_COMPLETE.name());
				break;
			case "CERT_COMPLETE" : 
				paramsMap.put("ACTION", ACTION_TYPE.CERT_COMPLETE.name());
				break;
			case "DOC_DOWNLOAD" :
				paramsMap.put("ACTION", ACTION_TYPE.DOC_DOWNLOAD.name());
				break;
			case "DECIDE_COMPLETE" :
				paramsMap.put("ACTION", ACTION_TYPE.DEC_COMPLETE.name());	
				break;
		}
		paramsMap.put("IP", getClientIP());
		String confirmUserId = MapUtils.getString(docBasicInfo, "CONFIRM_USER_ID", null);
		if (confirmUserId == null || "".equals(confirmUserId)) paramsMap.put("CONFIRM_USER_ID", info.getSession("ID"));
		resultFlag = doInsertSPMGT(docBasicInfo, paramsMap);
		
		if(!resultFlag) {
			this.errCode = 103330;
			return false;
		}else {
			return true;		
		}
		
	}
	
	public boolean doDeleteProcessHistory() {	
		Boolean resultFlag = false;
		resultFlag = doDeleteSPMGT(docBasicInfo);
		
		if(!resultFlag) {
			this.errCode = 103331;
			return false;
		}else {
			return true;	
		}
	}
	//Image to base64
	protected String imageToBase64(String filePath){
		String out = null;
		InputStream in = null;
		File file = new File(filePath);
		try {
			if(file.exists()) {
				in = CipherManager.getCipher().getInputStream(file, false);
				out = DatatypeConverter.printBase64Binary(IOUtils.toByteArray(in));
			}
		} catch (Exception e) {
			this.errCode = 102411;
			Logger.err.println(this.info.getSession("ID"), this, e);
			return null;
		}
	    return out;
	}
	
	//사용자 정보 조회 
	private List<Map<String, String>> doSelectSUSMT(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//PMGT 데이터 조회
	private List<Map<String, String>> doSelectSPMGT(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//PMGT 데이터 적재
	private boolean doInsertSPMGT(Map<String, String> data1, Map<String, String> data2) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data1, data2);
			
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//PMGT 데이터 삭제
	private boolean doDeleteSPMGT(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(data);
			
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}	
	
	
	private String getClientIP() {
		String ip = null;
		HttpServletRequest req = ThreadLocalFilter.getHttpServletRequest();

		if (ip == null || "".equals(ip) || ip.length() == 0) {
			ip = req.getHeader("X-Forwarded-For");
		}
		if (ip == null || "".equals(ip) || ip.length() == 0) {
			ip = req.getHeader("Proxy-Client-ip");
		}
		if (ip == null || "".equals(ip) || ip.length() == 0) {
			ip = req.getHeader("WL-Proxy-Client-ip");
		}
		if (ip == null || "".equals(ip) || ip.length() == 0) {
			ip = req.getHeader("HTTP_CLIENT_ip");
		}
		if (ip == null || "".equals(ip) || ip.length() == 0) {
			ip = req.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || "".equals(ip) || ip.length() == 0) {
			ip = req.getHeader("X-Real-ip");
		}
		if (ip == null || "".equals(ip) || ip.length() == 0) {
			ip = req.getHeader("X-Realip");
		}
		if (ip == null || "".equals(ip) || ip.length() == 0) {
			ip = req.getRemoteAddr();
		}
		if (ip == null) {
			ip = "unknown";
		}
		return ip;
	}
}
