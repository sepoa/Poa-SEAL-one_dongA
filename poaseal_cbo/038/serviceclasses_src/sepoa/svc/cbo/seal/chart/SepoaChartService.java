package sepoa.svc.cbo.seal.chart;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.svc.seal.SepoaSealService;

@Description("인장 날인용 차트 서비스(동아제약 전용)")
public class SepoaChartService extends SepoaSealService{

	public SepoaChartService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("월별 날인 현황 추이")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectSIADDATA(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Gson gson = new Gson();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			
			ObjectMapper objectMapper = new ObjectMapper();
			
			Map<String, Object> filterMap = objectMapper.readValue(headerData.get("filterData"), new TypeReference<Map<String, Object>>() {});
			
            for (Map.Entry<String, Object> entry : filterMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();

                headerData.put(key, value);
            }
			
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> list = sf.getListMap();
			
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
			
			Collections.sort(list, new Comparator<Map<String, String>>() {
				@Override
				public int compare(Map<String, String> map1, Map<String, String> map2) {
					try {
						// 날짜 타입으로 변환
						Date date1 = simpleDateFormat.parse(map1.get("CONFIRM_DATE"));
						Date date2 = simpleDateFormat.parse(map2.get("CONFIRM_DATE"));
						
						// 날짜 비교
						return date1.compareTo(date2);
					} catch (Exception e) {
						e.printStackTrace();
						return 0;
					}
				}
			});
			
			String json = gson.toJson(list);
			
			setValue(json);
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("인감별 날인현황")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectSTMHI(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Gson gson = new Gson();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			
			Map<String, String> filteredData = gson.fromJson(headerData.get("filterData"), Map.class);
			headerData.putAll(filteredData);
						
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> list = sf.getListMap();
			
			Set<String> allowSealSet = new HashSet<>(Arrays.asList(CommonUtil.getConfig("sepoa.chart.allow.seal", "").split(";")));
			List<Map<String, String>> filteredList = null;
			String json = null;
			
			if (allowSealSet.size() > 0) {
		        filteredList = list.stream()
		                .filter(data -> allowSealSet.contains(data.get("MANAGE_NUMBER")))
		                .map(data -> {
		                    Map<String, String> map = new HashMap<>();
		                    map.put("subject", data.get("SUBJECT"));
		                    map.put("manageNumber", data.get("MANAGE_NUMBER"));
		                    map.put("totalSmrCount", data.get("TOTAL_SMR_COUNT"));
		                    map.put("reFlag", data.get("RE_FLAG"));
		                    return map;
		                })
		                .collect(Collectors.toList());
		        
		        json = gson.toJson(filteredList);	
			}
			
					
			setValue(json);
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("용도별 날인현황")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectSUDGLData(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Gson gson = new Gson();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			
			Map<String, String> filteredData = gson.fromJson(headerData.get("filterData"), Map.class);
			headerData.putAll(filteredData);
			
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> list = sf.getListMap();
			
			Set<String> allowSealSet = new HashSet<>(Arrays.asList(CommonUtil.getConfig("sepoa.chart.allow.seal.purposes", "").split(";")));
			List<Map<String, String>> filteredList = null;
			String json = null;
			
			if(allowSealSet.size() > 0) {
				filteredList = list.stream()
						.filter(data -> allowSealSet.contains(data.get("USE_NUMBER")))
						.map(data -> {
							Map<String, String> map = new HashMap<>();
							map.put("count", data.get("COUNT"));
							map.put("useNumber", data.get("USE_NUMBER"));
							map.put("reFlag", data.get("RE_FLAG"));
							map.put("subject", data.get("SUBJECT"));
							return map;
						})
						.collect(Collectors.toList());
				
				json = gson.toJson(filteredList);
			}
			
			setValue(json);
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("날인 현황 랭킹")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectRankSSIAD(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		Gson gson = new Gson();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			List<Map<String, String>> resultData = new ArrayList<Map<String, String>>();
			
			ObjectMapper objectMapper = new ObjectMapper();
			
			Map<String, Object> filterMap = objectMapper.readValue(headerData.get("filterData"), new TypeReference<Map<String, Object>>() {});
			
            for (Map.Entry<String, Object> entry : filterMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();

                headerData.put(key, value);
            }
			
			sxp = new SepoaXmlParser(this, "doSelectRankEFlag");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf1 = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> eFlagList = sf1.getListMap();
			for(Map<String, String> map : eFlagList) {
				resultData.add(map);
			}
			
			sxp = new SepoaXmlParser(this, "doSelectRankRFlag");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf2 = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> rFlagList = sf2.getListMap();
			for(Map<String, String> map : rFlagList) {
				resultData.add(map);
			}
			
			sxp = new SepoaXmlParser(this, "doSelectRankSSETL");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf3 = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> SETLList = sf3.getListMap();
			for(Map<String, String> map : SETLList) {
				resultData.add(map);
			}
			
			String json = gson.toJson(resultData);
			
			setValue(json);
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
}
