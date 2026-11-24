package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.auth.encrypt.SepoaAuthEncryptManager;
import sepoa.fw.db.ConnectionContext;
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
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@SuppressWarnings("all")
@Description("UserManagement")
public class AD_025 extends SepoaService {
    public AD_025(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
    /**
     * 입.퇴사자 리스트 불러오는 메소드
     * @param allData
     * @return
     */
    @Description("doQueryList")
    public SepoaOut doQueryList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser(this, "doQueryList");
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
    
    /**
     * 기존 입사자 데이터 가져오는 메소드
     * @param allData
     * @return
     */
    @Description("doQueryForm")
    public SepoaOut doQueryForm(Map<String, Object> allData) {
    	setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
    	
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser(this, "doQueryForm");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

			String result = ssm.doSelect(headerData);
			
			Map<String, String> firstData = new SepoaFormater(result).getListMap(true).get(0);
			
			String convertResultData = new SepoaFormater(firstData).getString();
			
			setValue(convertResultData);
			
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
    
    /**
     * 신규 입사자 등록 시 기존 아이디 존재여부 체크 메소드
     * @param allData
     * @return
     */
    @Description("doUserCheck")
    public SepoaOut doUserCheck(Map<String, Object> allData) {
    	
    	setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, String> appendData = new HashMap<String, String>();
			String result = "";
			
			sxp = new SepoaXmlParser(this, "doUserCheck");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			result = ssm.doSelect(headerData);
			
			appendData.put("IS_EXIST", "false");
			
			List<Map<String, String>> firstData = new SepoaFormater(result).getListMap(true);
			
			if( firstData.size() > 0 ) {
				appendData.replace("IS_EXIST", "true");
			}
			
			SepoaFormater resultData = new SepoaFormater(appendData);
			
			setValue(resultData.getString());
			
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
    
    /**
     * 기존 입사자 데이터 저장 메소드
     * @param allData
     * @return
     */
    @Description("doUpdateForm")
    public SepoaOut doUpdateForm(Map<String, Object> allData) {
    	
    	setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		boolean isPasswordChange = false;
		String retirement_date = "";
    	
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			if( headerData.get("PASSWORD") == null || headerData.get("PASSWORD").equals("") ) {
				headerData.remove("PASSWORD");
			} else {
				isPasswordChange = true;
				headerData.replace("PASSWORD", SepoaAuthEncryptManager.getEncrypted(headerData.get("PASSWORD")));
			}			
						
			sxp = new SepoaXmlParser(this, "doUpdateUSMT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doUpdate(headerData);
			
			sxp = new SepoaXmlParser(this, "doUpdateADDR");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doUpdate(headerData);
			
			Commit();
			
			headerData.put("REQ_NAME", headerData.get("USER_NAME_LOC"));
			headerData.put("REQ_MAIL", headerData.get("EMAIL"));
			
			if( isPasswordChange ) {
				sendMail(headerData);
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
    
    /**
     * 신규 입사자 데이터 저장 메소드
     * @param allData
     * @return
     */
    @Description("doInsertForm")
    public SepoaOut doInsertForm(Map<String, Object> allData) {
    	
    	setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
    	
		String encPassword = "";
		String orgPassword = "";
		
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			orgPassword = "sepoa" + getRandomString(5);
			encPassword = SepoaAuthEncryptManager.getEncrypted(orgPassword);
			
			headerData.put("PASSWORD", encPassword);
			
			sxp = new SepoaXmlParser(this, "doInsertUSMT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doInsert(headerData);
			
			sxp = new SepoaXmlParser(this, "doInsertADDR");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doInsert(headerData);
			
			Commit();
			
			headerData.put("REQ_NAME", headerData.get("USER_NAME_LOC"));
			headerData.put("REQ_MAIL", headerData.get("EMAIL"));
			headerData.put("ORG_PASSWORD", orgPassword);
			headerData.put("IS_NEW", "true");
			
			sendMail(headerData);
			
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
     * 퇴사자 복구 처리 메소드
     * @param allData
     * @return
     */
    @Description("doRestore")
    public SepoaOut doRestore(Map<String, Object> allData) {
    	
    	setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
    	
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doRestore");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doUpdate(headerData);
			
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
     * 사용자 삭제 메소드
     * @param allData
     * @return
     */
    @Description("doDelete")
    public SepoaOut doDelete(Map<String, Object> allData) {
    	
    	setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
    	
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doDeleteUSMT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doUpdate(headerData);
			
			sxp = new SepoaXmlParser(this, "doDeleteADDR");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doUpdate(headerData);
			
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
     * 메일 보내는 메소드
     * @param mailData
     * @throws Exception
     */
    @Description("sendMail")
    private void sendMail(Map<String, String> mailData) throws Exception {
		List<RecvMailInfoVo> recvList = new ArrayList<RecvMailInfoVo>();
		List<RecvMailInfoVo> replyList = new ArrayList<RecvMailInfoVo>();

		RecvMailInfoVo rVo = new RecvMailInfoVo();			//고객 VO
		
		MailSendHistory msh = new MailSendHistory();
		MailSendVo vo = new MailSendVo();
		
		boolean is_new = false;
		
		if( Boolean.valueOf(MapUtils.getString(mailData, "IS_NEW", "false")) ) {
			is_new = true;
		}

		//메일 제목, 내용 세팅
		String subject = "";
		String mailContents = makeMailContents(mailData);
				
		//수신자 메타정보
		String receiverName = mailData.get("REQ_NAME");
		String receiverMail = mailData.get("REQ_MAIL");
		
		//운영팀 메타정보
		String helpTeamName = "세포아소프트 운영팀";
		
		// 이메일 null체크
		if (receiverMail == null || receiverMail.isEmpty()) {
			throw new Exception("Mail Address is Null");
		}
		
		if( is_new ) {
			subject = "[Sepoasoft KMS] 회원가입 완료 알림";
		} else {
			subject = "[Sepoasoft KMS] 비밀번호 변경 알림";
		}

		// 수신자 이름 null체크
		receiverName = (receiverName == null || receiverName.isEmpty()) ? receiverName : receiverName;

		//메일 관련자 VO 세팅
		rVo.setRecvMailAddress(receiverMail);			//고객 이메일
		rVo.setRecvMailPerson(receiverName);			//고객 이름
		
		//메일 수신자 List 적재
		recvList.add(rVo);			//고객
		
		//발신자 정보 설정 (메일설정은 안됨)
		vo.setSenderName(helpTeamName);

		vo.setMailSubject(subject);			//제목 설정
		vo.setMailContent(mailContents);	//메일 내용 설정
		vo.setRecvMailInfoList(recvList);	//메일 수신자
		vo.setReplyMailList(replyList);		//답변 받을사람
		msh.setMailSendHistory(vo, info);	//메일 발송
	}
    
    /**
     * 메일 내용 작성 메소드
     * @param mailData
     * @return
     * @throws Exception
     */
    @Description("makeMailContents")
    private String makeMailContents(Map<String, String> mailData) throws Exception {
		String reqName = mailData.get("REQ_NAME");
		boolean is_new = false;
		String contents = null;
		
		if( Boolean.valueOf(MapUtils.getString(mailData, "IS_NEW", "false")) ) {
			is_new = true;
		}
		
		contents = "안녕하세요 " + reqName + "님 <br>";
		contents += "세포아 소프트 운영팀 입니다. <br><br>";
		
		if( is_new ) {
			contents += "관리자에 의해 생성된 KMS 비밀번호를 전달드립니다.<br>";
			contents += "생성된 비밀번호는 " + mailData.get("ORG_PASSWORD") + " 이며, 로그인 후 비밀번호 변경 요청드립니다.<br><br>";
		} else {
			contents += "관리자에 의해 KMS 비밀번호 변경되었음을 알려드립니다.<br><br>";
		}
		contents += "감사합니다.<br>";
		contents += "세포아소프트 운영팀 드림<br>";

		return contents;
	}
    
    @Description("getRandomString")
    private String getRandomString(int length) {
		StringBuilder r = new StringBuilder();
		for (int i = 0; i < length; i++) {
			r.append(String.valueOf((int) (Math.random() * 10)));
		}
		return r.toString();
	}
}
