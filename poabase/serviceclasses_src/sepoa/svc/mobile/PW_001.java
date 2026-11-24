package sepoa.svc.mobile;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.mail.MailSendHistory;
import sepoa.fw.mail.MailSendVo;
import sepoa.fw.mail.RecvMailInfoVo;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.AES256Cipher;
import sepoa.fw.util.SepoaFormater;

public class PW_001 extends SepoaService {

	public PW_001(String opt, SepoaInfo info) throws SepoaServiceException
	{
		super(opt, info);
		setVersion("1.0.0");
	}

	public SepoaOut doQuery(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		//SepoaFormater sf = null;
		
		try {
			Map<String, String> header = MapUtils.getMap(data, "headerData");
			Map<String, String> customHeader = new HashMap<String, String>();
			sxp = new SepoaXmlParser(this, "doQuery");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(header, customHeader));
			if(sf.getRowCount() == 0) {
				// Not exists.
				setStatus(9);
			} else {
				setValue(sf.getValue("USER_ID", 0));
				String email = sf.getValue("EMAIL", 0);
				String[] sp = email.split("@");
				int lastIdx = sp[1].lastIndexOf('.');
				setValue(sp[0].substring(0, 2) + "****@" + sp[1].substring(0, 2) + "***" + sp[1].substring(lastIdx)); 
			}
			
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	

	public SepoaOut send(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> header = MapUtils.getMap(data, "headerData");
			String email = header.get("email");
			
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("houseCode", header.get("houseCode"));
			map.put("companyCode", header.get("companyCode"));
			map.put("userId", header.get("userId"));
			map.put("time", new Date().getTime());
			
			String jsonStr = new ObjectMapper().writeValueAsString(map);

//			Config config = new Configuration();
//			String key = config.get("sepoa.sso.key");
//
//			jsonStr = AES256Cipher.AES_Encode(jsonStr, key);		

			StringBuffer sb =  new StringBuffer("http://localhost:9090/mobile/account/changePassword.jsp?p=");
			sb.append(URLEncoder.encode(jsonStr, "UTF-8"));
			
			MailSendVo vo = new MailSendVo();
			vo.setMailSubject("[Expense Management System] 비밀번호 변경 절차");
			vo.setMailContent(sb.toString());
			
			RecvMailInfoVo miVo = new RecvMailInfoVo();
			miVo.setRecvMailPerson(email);
			miVo.setRecvMailAddress(email);
			List<RecvMailInfoVo> recvMailInfoList = new ArrayList<RecvMailInfoVo>(1);
			recvMailInfoList.add(miVo);
			vo.setRecvMailInfoList(recvMailInfoList);
			
			MailSendHistory msh = new MailSendHistory();
			msh.setMailSendHistory(vo, info);
			
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
}
