package sepoa.svc.approval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;

import com.google.gson.Gson;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("결재선 세팅")
public class SepoaAppLineSettingService extends SepoaService {
	List<String> innerList = new ArrayList<>();
	
	public SepoaAppLineSettingService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	} 

	@Description("직원 조회")
	public SepoaOut doQueryUser(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			setValue(sf);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("부서 조회")
	public SepoaOut doQueryDept(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));

			// 쿼리 결과 정렬 코드
			List<Map<String, String>> result = new ArrayList<Map<String, String>>();
			if(sf.getRowCount() > 0) {
				for(Map<String, String> data : sf.getListMap()) {
					ArrayList<Integer> indexList = new ArrayList<Integer>();
					if(result.stream().filter((e) -> {
						if(e.get("PARENTID").equals(data.get("ID"))) {
							indexList.add(result.indexOf(e));
							return true;	
						}
						return false;
					}).count() > 0) {
						result.add(indexList.get(0), data);
					} else {
						result.add(data);
					}
				}				
			}
			setValue(new SepoaFormater(result));
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("내 결재선 저장")
	public SepoaOut doSaveMyApp(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, String> rawheaderData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			List<Map<String, String>> dataList = new Gson().fromJson(rawheaderData.get("unescaped_DATA"), List.class);

			if(headerData.get("APP_LINE_CODE") == null) {
				String myAppNo = DocumentUtil.getDocNumberRemote(info, "MyApproval").result[0];
				headerData.put("APP_LINE_CODE", myAppNo);
				sxp = new SepoaXmlParser(this, "doInsertSGWMG");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(headerData);

				for(Map<String, String> data : dataList) {
					sxp = new SepoaXmlParser(this, "doInsertSGWML");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					data.put("APP_TYPE", "C02");
					data.put("APP_LINE_CODE", myAppNo);
					ssm.doInsert(data);	
				}
			} else {
				sxp = new SepoaXmlParser(this, "doUpdateSGWMG");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(headerData);

				sxp = new SepoaXmlParser(this, "doDeleteSGWML");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(headerData);

				for(Map<String, String> data : dataList) {
					sxp = new SepoaXmlParser(this, "doInsertSGWML");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					data.put("APP_TYPE", "C02");
					data.put("APP_LINE_CODE", headerData.get("APP_LINE_CODE"));
					ssm.doInsert(data);					
				}
			}

			Commit();
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
		
	@Description("내 결재선 삭제")
	public SepoaOut doDeleteMyApp(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(headerData);
			
			Commit();
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}

	@Description("내 결재선 헤더 조회")
	public SepoaOut doQuerySGWMG(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			setValue(sf);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}

	@Description("내 결재선 상세 조회")
	public SepoaOut doQuerySGWML(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			setValue(sf);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
	
	@Description("키워드 검색")
	public SepoaOut doSearch(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<String> result = new ArrayList<>();

			sxp = new SepoaXmlParser(this, "doSearchSDEPT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater deptSF = new SepoaFormater(ssm.doSelect(headerData));
			if(deptSF.getRowCount() > 0) {
				List<Map<String, String>> deptDataList = deptSF.getListMap();
				result.addAll(deptDataList.stream().filter((map) -> {
					return !"".equals(MapUtils.getString(map, "DEPT", ""));
				}).map((map) ->  MapUtils.getString(map, "DEPT")).collect(Collectors.toList()));
			}

			sxp = new SepoaXmlParser(this, "doSearchSUSMT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater userSF = new SepoaFormater(ssm.doSelect(headerData));
			if(userSF.getRowCount() > 0) {
				List<Map<String, String>> userDataList = userSF.getListMap();
				result.addAll(userDataList.stream().filter((map) -> {
					return !"".equals(MapUtils.getString(map, "DEPT", ""));
				}).map((map) ->  MapUtils.getString(map, "DEPT")).collect(Collectors.toList()));
			}

			setValue(new Gson().toJson(result));
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("검색결과 트리데이터 조회 및 파싱")
	public SepoaOut doQuerySearchTreeData(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> result = new ArrayList<>();
			List<Map<String, String>> userSelectTargetDeptList = new ArrayList<>();
			List<String> relatedDept = Arrays.asList(MapUtils.getString(headerData, "TARGET").split(","));
			innerList = new ArrayList<>();

			sxp = new SepoaXmlParser(this, "doQueryDept");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater allDeptSF = new SepoaFormater(ssm.doSelect(headerData));
			if(allDeptSF.getRowCount() > 0) {
				List<Map<String, String>> allDeptList = allDeptSF.getListMap();
				this.innerList.addAll(relatedDept);

				do {
					List<Map<String, String>> tmp = new ArrayList<>();
					tmp = allDeptList.stream().filter((map) -> {
						return this.innerList.contains(MapUtils.getString(map, "ID"));
					}).collect(Collectors.toList());

					for(Map<String, String> deptData : tmp) {
						if(!userSelectTargetDeptList.contains(deptData)) {
							userSelectTargetDeptList.add(deptData);
						}

						if(!result.contains(deptData)) {
							result.add(deptData);
						}
					}
					
					this.innerList.clear();
//					for(Map<String, String> dept : tmp) {
//						String parentID = MapUtils.getString(dept, "PARENTID", "");
//						if(!"".equals(parentID)) {
//							this.innerList.add(parentID);
//						}
//					}
				} while(this.innerList.size() > 0);
			}

			sxp = new SepoaXmlParser(this, "doQueryUser");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater allUserSF = new SepoaFormater(ssm.doSelect(headerData));
			if(allUserSF.getRowCount() > 0) {
				List<Map<String, String>> allUserList = allUserSF.getListMap();
				result.addAll(allUserList.stream().filter((map) -> {
					return userSelectTargetDeptList.stream().map((map2) -> {return MapUtils.getString(map2, "ID");}).collect(Collectors.toList()).contains(MapUtils.getString(map, "DEPT"));
				}).collect(Collectors.toList()));
			}

			setValue(new Gson().toJson(result));
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("인감 담당자 자동 셋팅")
	public SepoaOut doGetManageData(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			setValue(sf);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
	
	@Description("직무 결재자 자동셋팅")
	public SepoaOut doGetBALN(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			setValue(sf);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
}
