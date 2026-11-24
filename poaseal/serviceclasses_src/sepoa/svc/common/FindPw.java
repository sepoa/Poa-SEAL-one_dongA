package sepoa.svc.common;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.auth.encrypt.SepoaAuthEncryptManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.mail.MailTemplate;
import sepoa.fw.mail.RecvMailInfoVo;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.AES256Cipher;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;

@SuppressWarnings({ "unchecked" })
@Description("비밀번호 찾기")
public class FindPw extends SepoaService {

    private Map<String, String> returnMap = new HashMap<String, String>();
    
    public FindPw(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }

    /**
     * 사용자 ID, EMAIL 조회
     * @param allData
     * @return
     */
    @Description("사용자 ID, EMAIL 조회")
    public SepoaOut doQuery(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
        	
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doSelect(headerData));
            
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    
    @Description("메일발송")
    public SepoaOut sendMail(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
        	Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
        	Configuration conf = new Configuration();
            //메일보내기
            //Object[] obj = { allData, "FIND_PASSWORD" , getConnectionContext()};
            headerData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
			Random rnd = new Random();
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < 10; i++) {
				if (rnd.nextBoolean()) {
					buf.append((char) ((int) (rnd.nextInt(26)) + 97));
				} else {
					buf.append((rnd.nextInt(10)));
				}
			}
			String password = buf.toString();
			headerData.put("PASSWORD", password);

			// 비밀번호 변경
			resetPassword(headerData);
			
			sxp = new SepoaXmlParser(this, "setMailRequestor");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			Map<String, String> mailData = new HashMap<String, String>();
			SepoaPushManager spm = new  SepoaPushManager(info, new Configuration());
			if (sf.getRowCount() > 0) {

				
				mailData.put("SUBJECT", sf.getValue("USER_NAME", 0) + " 님의 임시 비밀번호가 발급되었습니다.");
    			
				mailData.put("TITLE1", "아이디");
				mailData.put("CONTENT1", sf.getValue("USER_ID", 0));
    			
    			String now_date = SepoaDate.getShortDateString();
    			String now_time = SepoaDate.getShortTimeString();
    			mailData.put("TITLE2", "이름");
    			mailData.put("CONTENT2", sf.getValue("USER_NAME", 0));

    			mailData.put("TITLE3", "발급일시");
    			mailData.put("CONTENT3", 
    					now_date.substring(0, 4) + "년 " + now_date.substring(4, 6) + "월 " + now_date.substring(6, 8) + "일" + 
    					now_time.substring(0, 2) + "시 " + now_time.substring(2, 4) + "분");
    			
    			mailData.put("CONTENT4", "발급된 임시 비밀번호는 " + "<strong>" + password + "</strong>" + "입니다.");
    			Calendar cal = Calendar.getInstance();
    			cal.setTime(new Date());
    			mailData.put("TIME", String.valueOf(cal.getTimeInMillis()));
    			mailData.put("LINK", conf.get("sepoa.system.domain.name"));
    			
    			//메일상단 로고처리
    			String logo = "http://onesource.sepoa.co.kr/010/form/default/img/logo.png";
    			String logoAttachKey = "D"+info.getSession("HOUSE_CODE")+"LOGINLOGO";
    			headerData.put("LOGO_ATTACH_KEY", logoAttachKey);
    			sxp = new SepoaXmlParser(this, "getLogo");
    			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
    			SepoaFormater sf_logo = new SepoaFormater(ssm.doSelect(headerData));
    			if(!"0".equals(sf_logo.getValue("SEQ",0))) {
    				logo = conf.get("sepoa.system.domain.name")+"/SepoaImage.sux?attach_key="+logoAttachKey+"&attach_seq="+sf_logo.getValue("SEQ",0)+"&HOUSE_CODE="+info.getSession("HOUSE_CODE"); 
    			}
    			mailData.put("LOGO", logo);
    			Logger.debug.println("HEADER Data : "+headerData);
    			Logger.debug.println("LOGO Data : "+logo);
    			
    			SepoaMessage message = new SepoaMessage();
    			message.addParams(mailData);
    			message.setMessageType("VENDOR_SEND");
    			
    			mailData.put("SUBJECT", "임시 비밀번호가 발급되었습니다.");
    			message.addReceiver(sf.getValue("USER_ID", 0), sf.getValue("USER_NAME", 0),sf.getValue("EMAIL", 0), "");

    			// 메일 보내기
				spm.addMessage(message);
    			spm.accept();
            //ServiceConnector.doService(info, "SendMessage", "NONDBJOB", "sendMessage", obj);
			}
            Commit();
        } catch (Exception e) {
        	Rollback();
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    /**
     * 사용자 ID조회
     * @param allData
     * @return
     */
    @Description("사용자 ID조회")
    public SepoaOut doQueryID(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
        	
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            Configuration conf = new Configuration();
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            
            SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
            if(sf.getRowCount() > 0) {
				//메일보내기
            	Map<String, String> map = sf.getListMap().get(0);
            	SepoaPushManager spm = new  SepoaPushManager(info, new Configuration());
            	Set<String> keys = conf.getMatchedKey(".*house_code$");
            	for(String key : keys) {
            		if(map.get("HOUSE_CODE").equals(conf.getString(key))) {
            			String serverUrl = key.replace(".house_code", "");
            			map.put("SERVER_URL", serverUrl);
            		}
            	}
            	Map<String, String> mailData = new HashMap<String, String>();
            	allData.put(SepoaDataMapper.KEY_HEADER_DATA, map);
				//Object[] obj = { allData, "FIND_ID" , getConnectionContext()};
				mailData.put("MAIL_TITLE", headerData.get("USER_NAME") + " 회원님의 아이디 확인요청이 접수되었습니다.");		    		
		    	mailData.put("TITLE1", "이름");
		    	mailData.put("CONTENT1", headerData.get("USER_NAME"));
		    	mailData.put("TITLE2", "아이디");
		    	mailData.put("CONTENT2", map.get("USER_ID"));
		    	mailData.put("TITLE3", "");
		    	mailData.put("CONTENT3", "아이디 확인요청은 누구나 할 수 있습니다.<br> 아이디는 등록 메일주소와 일치하는 경우 해당 메일주소를 통하여 전달드립니다.");			
	    		Calendar cal = Calendar.getInstance();
	    		cal.setTime(new Date());
	    		mailData.put("TIME", String.valueOf(cal.getTimeInMillis()));
	    		mailData.put("SUBJECT","아이디 확인요청이 접수되었습니다.");
	    		
	    		//메일상단 로고처리
	    		String logo = "http://onesource.sepoa.co.kr/010/form/default/img/logo.png";
	    		String logoAttachKey = "D"+info.getSession("HOUSE_CODE")+"LOGINLOGO";
	    		headerData.put("LOGO_ATTACH_KEY", logoAttachKey);
	    		sxp = new SepoaXmlParser(this, "getLogo");
	    		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	    		SepoaFormater sf_logo = new SepoaFormater(ssm.doSelect(headerData));
	    		if(!"0".equals(sf_logo.getValue("SEQ",0))) {
	    			logo = conf.get("sepoa.system.domain.name")+"/SepoaImage.sux?attach_key="+logoAttachKey+"&attach_seq="+sf_logo.getValue("SEQ",0)+"&HOUSE_CODE="+info.getSession("HOUSE_CODE"); 
	    		}
	    		mailData.put("LOGO", logo);
	    		Logger.debug.println("HEADER Data : "+headerData);
	    		Logger.debug.println("LOGO Data : "+logo);
	    		
	    		SepoaMessage message = new SepoaMessage();
    			message.addParams(mailData);
    			message.setMessageType("VENDOR_SEND");
    			System.out.println(headerData.get("EMAIL"));
	    		message.addReceiver(headerData.get("USER_NAME"), headerData.get("USER_NAME"),headerData.get("EMAIL"), "");
				//ServiceConnector.doService(info, "SendMessage", "NONDBJOB", "sendMessage", obj);
				
	    		// 메일 보내기
				spm.addMessage(message);
    			spm.accept();
	    		
				setMessage(Cache.text.get("FINDPW.IDINFO_SENDMAIL"));
            } else {
            	setStatus(-1);
				setMessage(Cache.text.get("FINDPW.IDINFO_NOTFOUND"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    public SepoaOut resetPassword(Map<String, String> dataMap) {

		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		String password = "";
		try {

			password = SepoaAuthEncryptManager.getEncrypted(dataMap.get("PASSWORD"));
			// headerData.put("USER_ID", userId);
			dataMap.put("PASSWORD", password);

			sxp = new SepoaXmlParser(this, "resetPassword");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(dataMap);

			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
    
    static final private String ENC_KEY = "sepoasoft0127!@#";
    
    /**
     * 비밀번호 변경 링크 전송
     * @param allData
     * @return
     */
    @Description("비밀번호 변경 링크 전송")
    public SepoaOut doSendPWResetLink(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser(this, "doValidateResetPW");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            
            if(sf.getRowCount() > 0) {
            	try {
            		Logger.debug.println("##### 임시비밀번호 메일 발송 #####");
            		Object[] obj = { allData, "FIND_PASSWORD", ctx };
            		sendMail(allData);
            		//ServiceConnector.doService(info, "SendMessage", "NONDBJOB", "sendMessage", obj);
            	} catch (Exception e2) {
            		Logger.err.println("$$$$$ 임시비밀번호 메일 발송 실패 $$$$$");
            		throw new Exception("임시비밀번호 메일 발송 실패");
            	}
				Commit();
				setMessage(Cache.text.get("FINDPW.PW_RESET_SENDMAIL"));
            } else {
            	setStatus(-1);
				setMessage(Cache.text.get("FINDPW.IDINFO_NOTFOUND"));
            }
        } catch (Exception e) {
        	Rollback();
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
}