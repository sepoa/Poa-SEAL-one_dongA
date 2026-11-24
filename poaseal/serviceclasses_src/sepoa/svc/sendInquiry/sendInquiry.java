package sepoa.svc.sendInquiry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.mail.RecvMailInfoVo;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;

@SuppressWarnings({ "unchecked" })
@Description("문의발송")
public class sendInquiry extends SepoaService {

    private Map<String, String> returnMap = new HashMap<String, String>();
    
    public sendInquiry(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }

    @Description("메일발송")
    public SepoaOut sendMail(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        SepoaOut out = null;
        try {
            //메일보내기
        	Configuration conf = new Configuration();
        	SepoaPushManager spm = new SepoaPushManager(info, conf);
        	Map<String, String> mailData = new HashMap<String, String>();
        	Map<String, String> map = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
        	Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일");
			String format = sdf.format(cal.getTime());
        	mailData.put("SUBJECT", "홈페이지에서 "+map.get("PAGE")+" 제품에 대한 문의가 접수되었습니다.");
			mailData.put("MAIL_TITLE", "홈페이지에서 문의가 접수되었습니다.");
			
			mailData.put("TITLE1","문의 페이지");
			mailData.put("CONTENT1", map.get("PAGE"));
			
			mailData.put("TITLE2","이름");
			mailData.put("CONTENT2", map.get("MANAGER"));
			
			mailData.put("TITLE3","전화번호");
			mailData.put("CONTENT3", map.get("PHONE"));
			
			mailData.put("TITLE4","이메일");
			mailData.put("CONTENT4",  map.get("MANAGER_EMAIL"));

			mailData.put("TITLE5","회사명");
			mailData.put("CONTENT5", map.get("COMPANY_NAME_LOC"));

			mailData.put("TITLE6","유입경로");
			mailData.put("CONTENT6", map.get("INFLOW"));

			mailData.put("TITLE7","문의내용");
			mailData.put("CONTENT7", map.get("CONTENTS").toString().replaceAll("&#10;", "<br>"));
			
			mailData.put("TIME", format);
									
			SepoaMessage message = new SepoaMessage();
			// 받는 사람 정보 셋팅
			message.addReceiver("SepoaSales", "SepoaSales","sales@sepoasoft.co.kr","");
//			message.setSender(map.get("COMPANY_NAME_LOC"), map.get("COMPANY_NAME_LOC"), map.get("MANAGER_EMAIL"));
			// 메일 보내기
			message.addParams(mailData);
			message.setMessageType("sendInquiry");
			// 메일 보내기
			spm.addMessage(message);
			spm.accept();
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
}