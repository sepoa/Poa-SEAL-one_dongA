package sepoa.svc.contract;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;

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
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자계약 - 계약항목")
public class SepoaContractItem extends SepoaService {
	public SepoaContractItem(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	protected String getClassName() {
		return this.getClass().getName();
	}
	
	@Description("조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("저장")
	@SuppressWarnings("unchecked")
	public SepoaOut doSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		Config config = null;
		PathManager pathManager = null;
		try {
			config = new Configuration();
			
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_RAW_GRID_DATA);
			for(Map<String, String> gridMap : gridData) {
				String methodName = "";
				
				if("".equalsIgnoreCase(MapUtils.getString(gridMap, "ITEM_NO", ""))) {
					String ciNo = DocumentUtil.getDocNumberRemote(info, "CI").result[0];
					gridMap.put("ITEM_NO", ciNo);
					methodName = "doSave";
				} else {
					sxp = new SepoaXmlParser(this, "doCheckExists");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					SepoaFormater sf = new SepoaFormater(ssm.doSelect(gridMap));
					if(0 == sf.getRowCount()) {
						methodName = "doSave";
					}else {
						methodName = "doUpdate";
					}
				}
				
				if(!"".equals(gridMap.get("DES_FILE_PATH"))) {
					pathManager = new PathManager(info, config);
					pathManager.setLayer(gridMap.get("ATTACH_TYPE"));
					String desFilePath = pathManager.getLayerPath() + "/" + gridMap.get("DES_FILE_PATH");
					File desFile = new File(desFilePath);
					if(!desFile.exists()) {
						String tempFilePath = pathManager.getLayerPath() + "/TEMP" + gridMap.get("DES_FILE_PATH");
						File tempFile = new File(tempFilePath);
						if(tempFile.exists()) {
							FileUtils.copyFile(tempFile, desFile);
						}
					}
				}
				
				try {
					sxp = new SepoaXmlParser(this, methodName);
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(gridMap);
				} catch (Exception e) {
					setStatus(105007);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
			
			if(pathManager != null) {
				String tempDirPath = pathManager.getLayerPath() + "/TEMP";
				File tempDir = new File(tempDirPath);
				if(tempDir.exists()) {
					FileUtils.deleteDirectory(tempDir);
				}
			}
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("삭제")
	@SuppressWarnings("unchecked")
	public SepoaOut doDelete(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_RAW_GRID_DATA);
			
			for(Map<String, String> gridMap : gridData) {
				if("".equalsIgnoreCase(MapUtils.getString(gridMap, "ITEM_NO", "")))
					continue;
				
				sxp = new SepoaXmlParser(this, "doDelete");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(gridData);
			}
			
			Commit();
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("계약서종류(CONT_TYPE) 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doQueryContType(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
				sxp = new SepoaXmlParser(this, "doQueryContType");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doSelect(headerData);

		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("아이템 이미지 파일 업로드")
	@SuppressWarnings("unchecked")
	public SepoaOut doUploadImageFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		Config config = null;
		PathManager pathManager = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> fileData = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
			
			config = new Configuration();
			pathManager = new PathManager(info, config);
			
			String attachType = MapUtils.getString(headerData, "ATTACH_TYPE", "STD") + "/TEMP";
			pathManager.setLayer(attachType);
			
			String tempFilePath = fileData.get(0).get("PATH");
			File tempFile = new File(tempFilePath);
			
			if(tempFile.exists()) {
				String desFilePath = pathManager.getUniquePath();
				File desFile = new File(desFilePath + "/" + fileData.get(0).get("FILE_NAME_CLIENT"));
				FileUtils.copyFile(tempFile, desFile);
				
				Map<String, String> result = new HashMap<>();
				result.put("ITEM_NO", headerData.get("ITEM_NO"));
				result.put("ATTACH_TYPE", attachType);
				result.put("DES_FILE_PATH", pathManager.getPathWithoutLayer(desFile));
				setValue(result);
			}

		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("이미지 조회")
	public SepoaOut doImage(Map<String, String> headerData) {
		setStatus(1);
		setFlag(true);
		
		Config config = null;
		PathManager pathManager = null;
		
		try {
			config = new Configuration();
			pathManager = new PathManager(info, config);
			pathManager.setLayer(headerData.get("ATTACH_TYPE"));
			setValue(pathManager.getLayerPath() + headerData.get("DES_FILE_PATH"));
			setValue("CONT");
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
}