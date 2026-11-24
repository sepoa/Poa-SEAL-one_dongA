package sepoa.svc.common;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.ParamSql;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.mail.MailSendHistory;
import sepoa.fw.mail.MailSendVo;
import sepoa.fw.mail.RecvMailInfoVo;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaJSON;

@SuppressWarnings("unchecked")
@Description("공통")
public class COMMON extends SepoaService {
	public COMMON(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("쿼리조회")
	public SepoaOut callQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			String query = headerData.get("callMethodName");
			sxp = new SepoaXmlParser(this, query);
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("활성언어조회")
	public SepoaOut getActiveLanguage(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		StringBuffer Attach_Sql;

		try {
			setStatus(1);
			setFlag(true);

			String res = "";
			
			Attach_Sql = new StringBuffer();
			ParamSql sm = new ParamSql(info.getSession("ID"), this, ctx);
			sm.removeAllValue();
			Attach_Sql.setLength(0);

			Attach_Sql.append("SELECT CODE     AS  LANGUAGE             \n");
			Attach_Sql.append("      ,TEXT1                             \n");
			Attach_Sql.append("      ,SORT_SEQ                          \n");
			Attach_Sql.append("  FROM SCODE                             \n");
			Attach_Sql.append(sm.addFixString(" WHERE HOUSE_CODE = ?    \n"));sm.addStringParameter(info.getSession("HOUSE_CODE"));
			Attach_Sql.append(sm.addFixString("   AND LANGUAGE   = ?    \n"));sm.addStringParameter(info.getSession("LANGUAGE"));
			Attach_Sql.append("   AND USE_FLAG   = 'Y'                  \n");
			Attach_Sql.append("   AND TYPE       = 'AD001'              \n");
			Attach_Sql.append(" ORDER BY SORT_SEQ                       \n");
			
			res = sm.doSelect(Attach_Sql.toString());
			
			SepoaFormater sf = new SepoaFormater(res);
			if(sf.getRowCount() == 0) {
				sm.removeAllValue();
				Attach_Sql.setLength(0);
				
				Attach_Sql.append("SELECT CODE     AS  LANGUAGE             \n");
				Attach_Sql.append("      ,TEXT1                             \n");
				Attach_Sql.append("      ,SORT_SEQ                          \n");
				Attach_Sql.append("  FROM SCODE                             \n");
				Attach_Sql.append(" WHERE HOUSE_CODE = '000'    			\n");
				Attach_Sql.append(sm.addFixString("   AND LANGUAGE   = ?    \n"));sm.addStringParameter(info.getSession("LANGUAGE"));
				Attach_Sql.append("   AND USE_FLAG   = 'Y'                  \n");
				Attach_Sql.append("   AND TYPE       = 'AD001'              \n");
				Attach_Sql.append(" ORDER BY SORT_SEQ                       \n");
				
				res = sm.doSelect(Attach_Sql.toString());
			}
			
			setValue(res);
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("활성언어조회")
	public static List<Map<String, String>> getSystemActiveLanguage(SepoaInfo info, ConnectionContext ctx) {
		try {
			Object[] obj = {new HashMap<String, Object>()};
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.common.COMMON", "CONNECTION", "getActiveLanguage", obj);
			return new SepoaFormater(so.result[0]).getListMap(true);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.println(e);
		}
		return null;
	}
	
	@Description("메일전송")
	public SepoaOut sendMail(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			if (!"true".equals(CommonUtil.getConfig("sepoa.smtp.send.flag", "false"))) {
				throw new Exception("Server setting : Mail rejected.");
			}
			
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			String subject  = headerData.get("SUBJECT");
			String senderName = headerData.get("SENDER_NAME");
			String toData = (headerData.containsKey("unescaped_TO"))?headerData.get("unescaped_TO"):headerData.get("TO");
			String contents = (headerData.containsKey("unescaped_CONTENTS"))?headerData.get("unescaped_CONTENTS"):headerData.get("CONTENTS");
			
			
			List<Map<String, String>> toList = (List<Map<String, String>>) SepoaJSON.toJSONObject(toData);
			List<RecvMailInfoVo> recvList = new ArrayList<RecvMailInfoVo>(toList.size());
			
			for(Map<String, String> to : toList) {
				RecvMailInfoVo rVo = new RecvMailInfoVo();
				String mail = to.get("MAIL");
				String name = to.get("NAME");
				if(mail == null || mail.isEmpty()) {
					throw new Exception("Mail Address is Null");
				}
				name = (name == null || name.isEmpty())?mail:name;
				rVo.setRecvMailPerson(name);
				rVo.setRecvMailAddress(mail);
				recvList.add(rVo);
			}
			
			MailSendHistory msh = new MailSendHistory();
			MailSendVo vo = new MailSendVo();
			if(senderName != null && !senderName.isEmpty()) {
				vo.setSenderName(senderName);
			}
			vo.setMailContent(contents);
			vo.setMailSubject(subject);
			vo.setRecvMailInfoList(recvList);
			msh.setMailSendHistory(vo, info);
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
}
