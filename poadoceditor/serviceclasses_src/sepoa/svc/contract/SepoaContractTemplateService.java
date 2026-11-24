package sepoa.svc.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.contract.SepoaContractManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자계약서식")
public class SepoaContractTemplateService extends SepoaContractService {
	public SepoaContractTemplateService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	protected String getClassName() {
		return this.getClass().getName();
	}
	
	@Description("문서 리스트 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this.getClassName(), "doDocumentList");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			List<Map<String ,String>> result = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			
			setValue(result);
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("문서 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentDetail(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				Map<String, Object> result = new HashMap<String, Object>();
				Map<String, String> docInfoData = manager.getDocumentInfo();
				result.put("DOC_INFO_DATA", docInfoData);
				if(docInfoData.containsKey("SIGNER_CODE")) {
					String[] signerCodes = docInfoData.get("SIGNER_CODE").split(",");
					
					List<Map<String, String>> signerList = new ArrayList<Map<String, String>>(signerCodes.length);
					
					for(String signerCode : signerCodes) {
						Map<String, String> signer = new HashMap<String, String>();
						signerList.add(signer);
						signer.put("SIGNER_NO", signerCode);
					}
					
				}
				setValue(result);
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	public boolean onDocumentUpdateAfter(Map<String, Object> allData, SepoaContractManager manager) {
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);

		String nextStep = headerData.get("NEXT_STEP");
		switch(nextStep) {
		case "END":
			if(!manager.doUpdateStatus("S")) {
				setStatus(manager.getLastErrCode());
				return false;
			}
			Cache.combo.removeAll();
			break;
		case "MODULE":
		case "UPLOAD":
			if(!manager.doUpdateStatus("T")) {
				setStatus(manager.getLastErrCode());
				return false;
			}
			break;
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Description("삭제")
	public SepoaOut doDocumentDelete(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				if(!manager.deleteDocument(true)) {
					setStatus(manager.getLastErrCode());
				}
			}
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@Description("문서 업데이트")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentUpdate(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory(MapUtils.getString(allData,SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
				if(getStatus() == 1 && headerData.containsKey("DOC_INFO_DATA")) {
					
					Map<String, String> docInfoData = (Map<String, String>) jsonData.get("DOC_INFO_DATA");
					if(docInfoData.containsKey("CONT_FORM_NO")) {
						if(!manager.doUpdateForm(docInfoData.get("CONT_FORM_NO"))) {
							setStatus(manager.getLastErrCode());
						}
					}
					
					if(getStatus() == 1 && !manager.doUpdate(docInfoData)) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("EDITOR_HTMLS")) {
					if(!manager.doUpdateEditorHTML((List<Map<String, String>>) jsonData.get("EDITOR_HTMLS"))) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("SIGNER")) {
					List<String> validated = manager.doInsertSigner((List<Map<String, String>>) jsonData.get("SIGNER"));
					if(validated != null) {
						int errCode = manager.getLastErrCode();
						setStatus(errCode);
						setMessage(StringUtils.join(validated, "\n"));
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("EXTRA_FILES")) {
					List<Map<String, String>> fileList = (List<Map<String, String>>) jsonData.get("EXTRA_FILES");
					for(Map<String, String> fileInfo : fileList) {
						manager.doAddFile(fileInfo.get("FILE_TYPE"), fileInfo, true);
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
							break;
						}
					}
				}
			}
			this.onDocumentUpdateAfter(allData, manager);
			
			if(getStatus() != 1) {
				setFlag(false);
				if(StringUtils.isBlank(getMessage())){
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				}
				Rollback();
			}else if(!MapUtils.getBoolean(allData, SepoaModuleDataMapper.WITHOUT_COMMIT, false)){
				Commit();
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("복제")
	public SepoaOut doDocumentCopy(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				Map<String, String> newInfo = manager.doCopyDocument();
				if(newInfo == null) {
					setStatus(manager.getLastErrCode());
				}else {
					setValue(newInfo);
				}
			}
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
}