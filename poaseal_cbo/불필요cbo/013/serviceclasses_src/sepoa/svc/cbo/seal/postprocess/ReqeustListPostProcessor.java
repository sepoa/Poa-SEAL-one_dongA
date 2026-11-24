package sepoa.svc.cbo.seal.postprocess;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
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
	
	@Description("날인신청현황 데이터 조회결과 후처리 프로세서")
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
			
			//날짜순 정리
			Collections.sort(data, new Comparator<Map<String, String>>() {
				@Override
				public int compare(Map<String, String> m1, Map<String, String> m2) {
					String date1 = (String) m1.get("ADD_DATE");
					String date2 = (String) m2.get("ADD_DATE");
					return date2.compareTo(date1);
				}
			});
			
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
