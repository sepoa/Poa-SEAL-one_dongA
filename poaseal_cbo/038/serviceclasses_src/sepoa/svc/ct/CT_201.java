package sepoa.svc.ct;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.filemanager.FileManager;
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
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("계약대상")
public class CT_201 extends SepoaService {
	
	public CT_201(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@SuppressWarnings("unchecked")
	@Description("동아제약 계약 대상 유효성검사")
	public SepoaOut doSelectSignerInfo(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doSelectSignerInfo");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0) {
				Map<String, String> signerInfo = sf.getListMap().get(0);
				signerInfo.put("SEQ", "1");
				signerInfo.put("CATEGORY", "C");
				setValue(new Gson().toJson(signerInfo));
			} else {
				throw new Exception("Not Found First Signer Info!");
			}
		} catch(Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("동아제약 계약 대상 유효성검사")
	public SepoaOut doCheckContractCreateReady(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			final Map<String, String> defineTypeLayer = new LinkedHashMap<>();
			defineTypeLayer.put("SE", "CONVERT_SEAL_ORG");
			defineTypeLayer.put("SR", "REQ_SEAL_REAL_DOC");
			defineTypeLayer.put("ER", "EXT_SEAL_REAL_DOC");
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			String requestType = headerData.get("REQ_TYPE");
			String layerKey = "";
			String headerQueryName = "";
			String detailQueryName = "";
			
			if("S".equals(requestType)) {
				headerQueryName = "doSelectSSIAD";
				detailQueryName = "doSelectSSIPD";
			} else if ("E".equals(requestType)) {
				headerQueryName = "doSelectSSETL";
				detailQueryName = "doSelectSSEDC";
			}
			
			sxp = new SepoaXmlParser(this, headerQueryName);
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0) {
				Map<String, String> sourceData = sf.getListMap().get(0);
				List<Map<String, String>> targetFileList = null;
				
				if("S".equals(requestType)) {
					layerKey = "SR";
				} else if ("E".equals(requestType)) {
					layerKey = "ER";
				}
				
				sxp = new SepoaXmlParser(this, detailQueryName);
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(sourceData));
				
				if(sf.getRowCount() > 0) {
					targetFileList = sf.getListMap();
					List<Map<String, String>> contractTargetList = new ArrayList<>();
					FileManager fm = new FileManager(info, new Configuration(), ctx);
					
					for(Map<String, String> target : targetFileList) {
						String smrNumber = MapUtils.getString(target, "SMR_NUMBER");
						String smdNumber = MapUtils.getString(target, "SMD_NUMBER");
						String docNumber = smrNumber + "_" + smdNumber;
						
						if(fm.setDocNumber(docNumber) != null) {
							List<Map<String, String>> fileList = fm.doList();
							if(fileList != null && fileList.size() > 0) {
								for(Map<String, String> file : fileList) {
									if(MapUtils.getString(file, "LAYER", "").equals(defineTypeLayer.get(layerKey))) {
										contractTargetList.add(file);
										break;
									}
								}
							} else {
								setStatus(12000);
								throw new Exception("Not Found Document File of Request Data");
							}
						} else {
							setStatus(12001);
							throw new Exception("Search Fail Document File of Request Data");
						}
					}
					
					if(contractTargetList.size() == 0) {
						setStatus(12000);
						throw new Exception("Not Found Document File of Request Data");
					}
					
					
				} else {
					setStatus(12002);
					throw new Exception("Not Found Detail Data of Reuqest Data");
				}
			} else {
				setStatus(12003);
				throw new Exception("Not Found Header Data of Request Data");
			}
		} catch(Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("동아제약 계약 대상 테이블 조회")
	public SepoaOut doQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> result = new ArrayList<>();
			
			SimpleDateFormat input = new SimpleDateFormat("yyyyMMdd HHmmss");
			SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			List<String> filterStrList = null;
			if(headerData.containsKey("F_REQ_TYPE")) {
				filterStrList = Arrays.asList(headerData.get("F_REQ_TYPE").split(","));
			}
			
			if(!headerData.containsKey("F_KEYWORD")) {
				headerData.put("F_KEYWORD", "__EMPTY__KEYWORD__");
			}
			
			Config config = new Configuration();
			Map<String, String> commonCondition = new HashMap<>();
			String targetUseNumber = StringUtils.join(config.get("sepoa.req.seal.contract.use.number").split("[;,]"), ",");
			commonCondition.put("REQ_SEAL_USE_TYPE", targetUseNumber);
			commonCondition.put("SMR_STATUS", "C");		// 날인 완료 상태

			String systemAdminProfile = config.get("sepoa.system.manager.profile.code");
			boolean isSystemAdmin = systemAdminProfile.equals(info.getSession("MENU_PROFILE_CODE"));

			// 부서의 팀장인 지 확인 (시스템관리자는 전체 조회, 중첩 if 미평가 버그 방지를 위해 Java에서 분기)
			if (!isSystemAdmin) {
				sxp = new SepoaXmlParser(this, "doQueryIsTeamManager");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
				if (sf.getRowCount() > 0) {
					commonCondition.put("IS_TEAM_MANAGER", "Y");
				} else {
					commonCondition.put("RESTRICT_OWN_ONLY", "Y");
				}
			}
			
			if(filterStrList == null || filterStrList.contains("S")) {
				sxp = new SepoaXmlParser(this, "doQuery");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData, commonCondition));
				
				if(sf.getRowCount() > 0) {
					result.addAll(sf.getListMap());
				}
			}
			
			if(filterStrList == null || filterStrList.contains("E")) {
				sxp = new SepoaXmlParser(this, "doQuery_ExternalSeal");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData, commonCondition));
				
				if(sf.getRowCount() > 0) {
					result.addAll(sf.getListMap());
				}
			}
			
			for(Map<String, String> row : result) {
				String addDate = row.get("ADD_DATE");
				String addTime = row.get("ADD_TIME");
				row.put("ADD_DATE_TIME", output.format(input.parse(addDate + " " + addTime)));
			}
			
			setValue(result);
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
}
