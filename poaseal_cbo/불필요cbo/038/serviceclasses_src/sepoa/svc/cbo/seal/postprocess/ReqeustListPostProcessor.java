package sepoa.svc.cbo.seal.postprocess;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;

@Description("신청현황 데이터 조회결과 후처리 프로세서")
public class ReqeustListPostProcessor extends SepoaService{
	public ReqeustListPostProcessor(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("동아제약 날인신청현황 데이터 조회결과 후처리 프로세서")
	public SepoaOut reqSealDoProcess(List<Map<String, String>> data) {
		setStatus(1);
		setFlag(true);
        
		try {
			for(Map<String, String> element : data) {
				LocalDate date = LocalDate.parse(element.get("ADD_DATE"), DateTimeFormatter.ofPattern("yyyyMMdd"));
				LocalTime time = LocalTime.parse(element.get("ADD_TIME"), DateTimeFormatter.ofPattern("HHmmss"));
				String dateString = date.toString() + " " +time.toString();
				element.put("ADD_DATE", dateString);
			}
			
			setValue(data);
		} catch (Exception e) {
			if(getStatus() == 1) setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("동아제약 인감반출신청현황 데이터 조회결과 후처리 프로세서")
	public SepoaOut reqExtDoProcess(List<Map<String, String>> data) {
		setStatus(1);
		setFlag(true);
        
		try {
			for(Map<String, String> element : data) {
				LocalDate date = LocalDate.parse(element.get("ADD_DATE"), DateTimeFormatter.ofPattern("yyyyMMdd"));
				LocalTime time = LocalTime.parse(element.get("ADD_TIME"), DateTimeFormatter.ofPattern("HHmmss"));
				String dateString = date.toString() + " " +time.toString();
				element.put("ADD_DATE", dateString);
				
				try {
					LocalDate confirmDate = LocalDate.parse(element.get("CONFIRM_DATE"), DateTimeFormatter.ofPattern("yyyyMMdd"));
					LocalTime confirmTime = LocalTime.parse(element.get("CONFIRM_TIME"), DateTimeFormatter.ofPattern("HHmmss"));
					String confirmDateString = confirmDate.toString() + " " +confirmTime.toString();
					element.put("CONFIRM_DATE", confirmDateString);
				} catch (Exception e) {
					element.put("CONFIRM_DATE", element.get("CONFIRM_DATE"));
				}
			}
			
			setValue(data);
		} catch (Exception e) {
			if(getStatus() == 1) setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
}
