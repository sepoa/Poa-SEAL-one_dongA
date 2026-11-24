package sepoa.svc.cbo.seal.approval;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import filters.ThreadLocalFilter;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;
import sepoa.fw.util.ux.sUtils;
import sepoa.seal.SepoaExtSealManager;
import sepoa.seal.SepoaReqSealManager;
import sepoa.seal.SepoaSealManager;
import sepoa.svl.common.RestUtil;
import sepoa.svl.util.SepoaMultipart;

@Description("한국장학재단 결재 서비스")
public class SepoaSealApprovalService extends SepoaService{
	public SepoaSealApprovalService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("한국장학재단 특정 결재선 세팅 서비스")
	public SepoaOut doSelectSealInfo(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) data.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			List<Map<String, String>> resultList = new ArrayList<>();
				
			// 결재자 적재 리스트
			List<Map<String, String>> approverList = new ArrayList<>();
			
			// 결재자, 참조자 데이터 적재 맵
			Map<String, Object> approvalSummaryData = new LinkedHashMap<>();
					
			//신청자 부서장(팀장) 조회
			resultList = doSelectApproverOrReferer(headerData, "doSelectUserLeader", false);
			if(resultList != null && resultList.size() > 0) {
				// 상신자가 부서장(팀장) 본인이면 제외
				for(Map<String,String> manager : resultList) {
					if(headerData.get("session_user_id").equals(manager.get("USER_ID"))) {
						continue;
					} else {
						//결재자 : 신청자 부서장(팀장) 추가
						approverList.addAll(resultList);
					}
				}
			}
			
			//총무팀 팀장 직무담당자 조회
			resultList = doSelectApproverOrReferer(headerData, "doSelectFixedApprover", false);
			if(resultList != null && resultList.size() > 0) {
				for(Map<String,String> manager : resultList) {
					// 상신자가 총무팀 팀장 직무담당자 본인이면 제외
					if(headerData.get("session_user_id").equals(manager.get("USER_ID"))) {
						continue;
					} else {
						//결재자 : 총무팀 팀장 직무담당자 추가
						approverList.addAll(resultList);
					}
				}
			}
			
			if(approverList != null && approverList.size() > 0) {
				approvalSummaryData.put("APPROVAL_DATA", approverList);
			}
			
			setValue(new Gson().toJson(approvalSummaryData));
			
		} catch (Exception e) {
			setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
		
	}
	
	@Description("한국장학재단 결재선, 참조선 조회 메소드, notChange 처리")
	private List<Map<String,String>> doSelectApproverOrReferer(Map<String, String> headerData, String methodName, Boolean notChange){
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		List<Map<String, String>> resultList = new ArrayList<>();
		
		try {
			
			sxp = new SepoaXmlParser(this, methodName);
			ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0) {
				List<Map<String, String>> tempResultList = sf.getListMap();
				if(notChange) { // 변경불가 해야하는 경우
					for(Map<String, String> approver : tempResultList) {
						approver.put("IS_FIXED", "true");
					}
					resultList.addAll(tempResultList);
				} else {
					resultList.addAll(tempResultList);
				}
			}
			
			
		} catch (Exception e) {
			setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return resultList;
	}
	
}

