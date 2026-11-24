package sepoa.svc.admin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.jtx.SepoaTransactionalResource;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@SuppressWarnings("unused")
@Description("조직관리")
@Permission(key="WORK_TYPE", value="Z")
public class SepoaOrgManagementService extends SepoaService {
	
	public SepoaOrgManagementService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("부서 그리드 행 저장")
	public SepoaOut doSaveDeptGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,
					SepoaDataMapper.KEY_GRID_DATA);

			for (Map<String, String> map : gridData) {
				
				sxp = new SepoaXmlParser(this, "doDuplicateDept");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(map));
				
				if(sf.getRowCount() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdateDept");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(map);
				} else {
					sxp = new SepoaXmlParser(this, "doSaveDept");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(map);
				}
			}
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

	@SuppressWarnings("unchecked")
	@Description("부서 그리드 행 삭제")
	public SepoaOut doDeleteDeptGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,
					SepoaDataMapper.KEY_GRID_DATA);

			for (Map<String, String> map : gridData) {
				Map<String, String> data = new HashMap<String, String>();

				sxp = new SepoaXmlParser(this, "doDeleteDept");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(map);
			}
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

	@SuppressWarnings("unchecked")
	@Description("회사그리드 표출")
	public SepoaOut doQueryCompanyGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			List<Map<String,String>> resultList = new ArrayList<Map<String,String>>();
			
			if(sf.getRowCount() > 0) {
				for(Map<String, String> map : sf.getListMap()) {
					if(map.get("DEL_FLAG") != null && map.get("DEL_FLAG").equals("Y")) {
						map.put("DEL_FLAG", "");
						resultList.add(map);
					} else {
						map.put("DEL_FLAG", "Y");
						resultList.add(map);
					}
				}
			}
			setValue(resultList);
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

	@SuppressWarnings("unchecked")
	@Description("부서 그리드")
	public SepoaOut doQueryDeptGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser(this, "doQueryDeptGrid");
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

	@SuppressWarnings("unchecked")
	@Description("회사상세조회")
	public SepoaOut doQueryCompany(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

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

	@SuppressWarnings("unchecked")
	@Description("부서상세조회")
	public SepoaOut doQueryDept(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

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
	
	@SuppressWarnings("unchecked")
	@Description("대표회사 존재여부 조회")
	public SepoaOut doSelectRepresentativeCompany(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf != null && sf.getRowCount()>0) {
				setStatus(2); // 기존에 대표회사 존재 시 status는 2
				setValue("");
				
				for(int i=0; i<sf.getListMap().size(); i++) {
					if(headerData.get("COMPANY_CODE").equals(sf.getListMap().get(i).get("COMPANY_CODE")) && 
							info.getSession("HOUSE_CODE").equals(sf.getListMap().get(i).get("HOUSE_CODE"))) {
						setStatus(1);
					}
				}
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

	@SuppressWarnings("unchecked")
	@Description("회사저장")
	public SepoaOut doSaveCompany(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			String RCFlag = headerData.get("REPRESENTATIVE_COMPANY").equals("1") ? "Y" : null;
			headerData.put("REPRESENTATIVE_COMPANY_FLAG", RCFlag);
			
			String delFlag = headerData.get("DEL_FLAG").equals("0") ? "Y" : "N";
			headerData.put("DEL_FLAG", delFlag);
			
			sxp = new SepoaXmlParser(this, "doDuplicateCompany");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0) {
				sxp = new SepoaXmlParser(this, "doUpdateCompany");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(headerData);
			} else {
				sxp = new SepoaXmlParser(this, "doSaveCompany");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(headerData);
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

	@SuppressWarnings("unchecked")
	@Description("부서저장")
	public SepoaOut doSaveDept(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser();
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

	@SuppressWarnings("unchecked")
	@Description("회사삭제")
	public SepoaOut doDeleteCompany(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser();
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

	@SuppressWarnings("unchecked")
	@Description("부서삭제")
	public SepoaOut doDeleteDept(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser();
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

	@SuppressWarnings("unchecked")
	@Description("회사 중복체크")
	public SepoaOut doDuplicateCompany(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));

			if (sf.getRowCount() != 0) {
				throw new Exception(Cache.text.get("MESSAGE.1049"));
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

	@SuppressWarnings("unchecked")
	@Description("부서 중복체크")	
	public SepoaOut doDuplicateDept(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));

			if (sf.getRowCount() > 0) {
				for( Map<String, String> tempMap : sf.getListMap()) {
					if(tempMap.get("DEL_FLAG").equals("Y")) {
						throw new Exception(Cache.text.get("MESSAGE.1049"));
					}
				}
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

	@SuppressWarnings("unchecked")
	@Description("부서조회")
	public SepoaOut doQueryDeptTree(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

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

	@SuppressWarnings("unchecked")
	@Description("회사조회")
	public SepoaOut doQueryCompanyList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

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
	
	@SuppressWarnings("unchecked")
	@Description("직무그리드조회")
	public SepoaOut doQueryCtrlGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser(this, "doQueryCtrlGrid");
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
	
	@SuppressWarnings("unchecked")
	@Description("직무그리드저장")
	public SepoaOut doSaveCtrlGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser ssxp = null;
		SepoaSQLManager sssm = null;
		SepoaXmlParser usxp = null;
		SepoaSQLManager ussm = null;
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,
					SepoaDataMapper.KEY_GRID_DATA);
			
			ssxp = new SepoaXmlParser(this, "doQueryCtrl");
			sssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, ssxp);
			
			for (Map<String, String> map : gridData) {
				SepoaFormater sf = new SepoaFormater(sssm.doSelect(map));
				if (sf.getRowCount() > 0) {
					usxp = new SepoaXmlParser(this, "doUpdateCtrl");
					ussm = new SepoaSQLManager(info.getSession("ID"), this, ctx, usxp);
					ussm.doUpdate(map);
				} else {
					usxp = new SepoaXmlParser(this, "doInsertCtrl");
					ussm = new SepoaSQLManager(info.getSession("ID"), this, ctx, usxp);
					ussm.doInsert(map);
				}
			}
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
	
	@SuppressWarnings("unchecked")
	@Description("직무그리드삭제")
	public SepoaOut doDeleteCtrlGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			
			for (Map<String, String> map : gridData) {
				sxp = new SepoaXmlParser(this, "doDeleteCtrl");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(map);
				
				sxp = new SepoaXmlParser(this, "doDeleteCtrlManagerRelated");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(map);
			}
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
	
	@SuppressWarnings("unchecked")
	@Description("직무 중복체크")	
	public SepoaOut doDuplicateCtrl(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));

			if (sf.getRowCount() > 0) {
				for( Map<String, String> tempMap : sf.getListMap()) {
					if(tempMap.get("DEL_FLAG").equals("Y")) {
						throw new Exception(Cache.text.get("MESSAGE.1049"));
					}
				}
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
	
	@SuppressWarnings("unchecked")
	@Description("직무담당자그리드조회")
	public SepoaOut doQueryCtrlManagerGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser(this, "doQueryCtrlManagerGrid");
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
	
	@SuppressWarnings("unchecked")
	@Description("직무담당자그리드저장")
	public SepoaOut doSaveCtrlManagerGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
			
			for (Map<String, String> map : gridData) {
				sxp = new SepoaXmlParser(this, "doQueryCtrlManager");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sf = new SepoaFormater(ssm.doSelect(map));
				
				if (sf.getRowCount() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdateCtrlManager");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(map);
				} else {
					sxp = new SepoaXmlParser(this, "doInsertCtrlManager");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(map);
				}
			}
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
	
	@SuppressWarnings("unchecked")
	@Description("직무담당자그리드삭제")
	public SepoaOut doDeleteCtrlManagerGrid(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			
			for (Map<String, String> map : gridData) {
				sxp = new SepoaXmlParser(this, "doDeleteCtrlManager");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(map);
			}
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
	
	@SuppressWarnings("unchecked")
	@Description("직무담당자 중복체크")
	public SepoaOut doDuplicateCtrlManager(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));

			if (sf.getRowCount() > 0) {
				for( Map<String, String> tempMap : sf.getListMap()) {
					if(tempMap.get("DEL_FLAG").equals("Y")) {
						throw new Exception(Cache.text.get("MESSAGE.1049"));
					}
				}
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

	@Description("리스트 목록")
	public SepoaOut doQueryList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		String[] descriptions = new String[] { "회사정보", "부서정보", "직급정보", "직무정보", "직무담당자정보" };
		try {
			Config conf = new Configuration();
			String theme = conf.getString("sepoa.sux.theme", "basic");

			List<Map<String, String>> list = new ArrayList<Map<String, String>>();
			for (int i = 0; i < descriptions.length; i++) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("DESCRIPTION", descriptions[i]);
				map.put("THEME", theme);
				list.add(map);
			}
			SepoaFormater sf = new SepoaFormater(list);
			setValue(sf.getString());
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@Description("사용자정보 체크")
	public SepoaOut doCheckUser(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

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
	
	@Description("부서정보 엑셀업로드")
	public SepoaOut doUploadDepartmentData(Map<String, Object> allData) {
    	ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData,SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> excelDataList = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
            
            if(excelDataList == null || excelDataList.size() == 0) {
            	setStatus(105002);
            	Logger.err.println("Not Found Department Interface Data!");
            	throw new Exception("Not Found Department Interface Data!");
            } 
            
            sxp = new SepoaXmlParser(this, "doSelectCompanyList");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			final List<Map<String, String>> companyListDefine = sf.getListMap();
			
			sxp = new SepoaXmlParser(this, "doSelectSDEPT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			final List<Map<String, String>> deptListDefine = sf.getListMap();
			
            List<Map<String,String>> convertExcelList = new ArrayList<>();
            for(Map<String,String> rowData : excelDataList) {
            	Map<String,String> convertRowData = new HashMap<String, String>();
            	
            	for(Entry<String, String> data : rowData.entrySet()){
            		String key = data.getKey();
            		
            		switch (key) {
					case "COMPANY_NAME_LOC":
						convertRowData.put(data.getKey(), data.getValue());
						convertRowData.put("COMPANY_CODE", companyListDefine.stream().filter(e -> data.getValue().equals(e.get("COMPANY_NAME_LOC"))).findFirst().get().get("COMPANY_CODE"));
						break;
					case "DEPT":
						convertRowData.put(data.getKey(), data.getValue());
						try {
							if(deptListDefine.stream().anyMatch(e -> data.getValue().equals(e.get("DEPT")))) {
								throw new Exception("A department code that has already been registered.");
							}
						} catch (Exception e2) {
							convertRowData.put("RESULT",Cache.text.get("POA_CONTRACT.STATUS_105012"));
							Logger.err.println("DEPT [" + rowData.get("DEPT") + "] Upload Fail!");
							Logger.err.println(e2.getMessage());
						}

						break;
					case "PARENT_DEPT_NAME":
						convertRowData.put(data.getKey(), data.getValue());
						try {
							if(!"".equals(rowData.get("PARENT_DEPT_NAME"))) {
								convertRowData.put("PARENT_DEPT", deptListDefine.stream().filter(e -> data.getValue().equals(e.get("DEPT_NAME_LOC"))).findFirst().get().get("DEPT"));
							}
						} catch (Exception e2) {
							convertRowData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105013"));
							Logger.err.println("PARENT_DEPT_NAME [" + rowData.get("PARENT_DEPT_NAME") + "] Upload Fail!");
							Logger.err.println("Unregistered parent department!");
						} 
						break;
					default:
						if(!"RESULT".equals(data.getKey())) {
							convertRowData.put(data.getKey(), data.getValue());
						}
						break;
					}
            	}
            	convertExcelList.add(convertRowData);
            }
            setValue(new Gson().toJson(convertExcelList));
            
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("부서정보 엑셀업로드 저장")
	public SepoaOut doSaveDepartmentData(Map<String, Object> allData) {
		ConnectionContext newCtx = null;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> excelDataList = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
			List<Map<String,String>> resultDataList = new ArrayList<Map<String,String>>();
			newCtx = new SepoaTransactionalResource().getConnectionContext();
			
				for(Map<String,String> rowData : excelDataList) {
					Map<String,String> tmpData = new HashMap<String,String>();
					
					try {
					tmpData.putAll(rowData);
					
					//엑셀데이터 업로드시 예외처리가 발생할경우 RESULT에 에러메세지 세팅됨
					if(!"".equals(tmpData.get("RESULT"))) {
						throw new Exception();
					}
					
					sxp = new SepoaXmlParser(this, "doInsertSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, newCtx, sxp);
					ssm.doUpdate(tmpData);
					((SepoaTransactionalResource) newCtx).getUserTransaction().commit();
					tmpData.put("RESULT", Cache.text.get("POA_CONTRACT.MESSAGE_0001"));
					
					} catch (Exception e) {
						Logger.err.println("DEPT [" +  tmpData.get("DEPT_NAME_LOC") + "] Save Fail!", e);
						tmpData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105009") + Cache.text.get("POA_CONTRACT.STATUS_105011"));
						((SepoaTransactionalResource) newCtx).getUserTransaction().rollback();
					} finally {
						resultDataList.add(tmpData);
					}
				}
				
			Map<String,String> returnMap = new HashMap<String,String>();
			returnMap.put("returnType", "param");
			returnMap.put("result", new Gson().toJson(resultDataList));
			setObject(returnMap);
			
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		} finally {
			if(newCtx != null) {
				((SepoaTransactionalResource) newCtx).release();
			}
		}
		
		return getSepoaOut();
	}
	
	@Description("직급정보 조회")
	public SepoaOut doQueryPosition(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser(this, "doSelectPosition");
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
	
	@Description("직급정보 저장")
	public SepoaOut doSavePosition(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
			for (Map<String, String> map : gridData) {
				sxp = new SepoaXmlParser(this, "doSelectPosition");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sf = new SepoaFormater(ssm.doSelect(map));
				
				if (sf.getRowCount() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdatePosition");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(map);
				} else {
					sxp = new SepoaXmlParser(this, "doInsertPosition");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(map);
				}
				Commit();
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
	
	@Description("직급 행삭제")
	public SepoaOut doDeletePosition(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			for (Map<String, String> map : gridData) {
				sxp = new SepoaXmlParser(this, "doDeletePosition");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(map);
			}
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
	
	@Description("직급 엑셀업로드")
	public SepoaOut doUploadPositionData (Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData,SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> excelDataList = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
            
            if(excelDataList == null || excelDataList.size() == 0) {
            	setStatus(105002);
            	Logger.err.println("Not Found Position Interface Data!");
            	throw new Exception("Not Found Position Interface Data!");
            }
            
            sxp = new SepoaXmlParser(this, "doSelectPosition");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			final List<Map<String, String>> userPositionDefine = sf.getListMap();
            List<Map<String,String>> convertExcelDataList = new ArrayList<>();
            
            for (Map<String,String> rowData : excelDataList) {
            	Map<String,String> convertData = new HashMap<>() ;
            	for(Entry<String,String> data : rowData.entrySet()){
            		
            		String key = data.getKey();
            		switch (key) {
					case "POSITION":
						try {
							convertData.put(data.getKey(), data.getValue());
							if(userPositionDefine.stream().anyMatch(e -> data.getValue().equals(e.get("POSITION")))){
								throw new Exception("This code has already been registered.");
							}
						} catch (Exception e) {
							Logger.err.println("POSITION_CODE [" + rowData.get("POSITION") + "] Upload Fail!", e.getMessage());
							convertData.put("RESULT",Cache.text.get("POA_CONTRACT.STATUS_105031"));
						}
						break;
					default:
						convertData.put(data.getKey(), data.getValue());
						break;
					}
            	}
            	convertExcelDataList.add(convertData);
            }
            setValue(new Gson().toJson(convertExcelDataList));
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
	
	@Description("직급 엑셀업로드 저장")
	public SepoaOut doSavePositionData(Map<String, Object> allData) {
		ConnectionContext newCtx = null;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> excelDataList = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
			List<Map<String,String>> resultDataList = new ArrayList<Map<String,String>>();
			newCtx = new SepoaTransactionalResource().getConnectionContext();
			
				for(Map<String,String> rowData : excelDataList) {
					Map<String,String> tmpData = new HashMap<String,String>();
					
					try {
					tmpData.putAll(rowData);
					
					//엑셀데이터 업로드시 예외처리가 발생할경우 RESULT에 에러메세지 세팅됨
					if(!"".equals(tmpData.get("RESULT"))) {
						throw new Exception();
					}
					
					sxp = new SepoaXmlParser(this, "doInsertPosition");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, newCtx, sxp);
					ssm.doUpdate(tmpData);
					((SepoaTransactionalResource) newCtx).getUserTransaction().commit();
					tmpData.put("RESULT", Cache.text.get("POA_CONTRACT.MESSAGE_0001"));
					
					} catch (Exception e) {
						Logger.err.println("POSITION_NAME [" + rowData.get("POSITION_NAME") + "] Save Fail!");
						tmpData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105009") + Cache.text.get("POA_CONTRACT.STATUS_105030"));
						((SepoaTransactionalResource) newCtx).getUserTransaction().rollback();
					} finally {
						resultDataList.add(tmpData);
					}
				}
				
			Map<String,String> returnMap = new HashMap<String,String>();
			returnMap.put("returnType", "param");
			returnMap.put("result", new Gson().toJson(resultDataList));
			setObject(returnMap);
			
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		} finally {
			if(newCtx != null) {
				((SepoaTransactionalResource) newCtx).release();
			}
		}
		
		return getSepoaOut();
	}
}
