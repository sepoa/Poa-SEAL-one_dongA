package sepoa.svc.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

import filters.ThreadLocalFilter;
import sepoa.filemanager.utils.storage.PathManager;
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
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaFormater;
import sepoa.seal.SepoaReqSealManager;
import sepoa.seal.rest.SepoaSealRestUtil;

@Description("Rest Interface Service : Request Stamp")
public class IF_REQ_STAMP extends SepoaService {
	public IF_REQ_STAMP(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	} 

	@Description("Interface Service")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		try {
			//String contentType = ThreadLocalFilter.getHttpServletRequest().getContentType();

			Map<String, Object> ifData = new HashMap<String, Object>();
			ifData.putAll(allData);

			Map<String, String> header = (Map<String, String>) MapUtils.getMap(ifData, "HEADER", null);
			String senderId = MapUtils.getString(header, "SENDER_ID", CommonUtil.getConfig("sepoa.interface.rest.sender.id.default"));
			Map<String, String> sealInfo = (Map<String, String>) MapUtils.getMap(ifData, "SEAL_INFO", null);
			Map<String, String> commonInfo = new HashMap<String, String>();
			List<Map<String, String>> docList = null;
			List<Map<String, String>> appLine = null;
			
			if(sealInfo == null || sealInfo.size() <= 0) {
				throw new Exception("Not found SEAL_INFO in data. Please check the parameter format.");
			}
			String companyCode = MapUtils.getString(header, "COMPANY_CODE");
			commonInfo.put("COMPANY_CODE", companyCode);
			
			SepoaInfo sealSepoaInfo = SepoaSealRestUtil.setSession(ThreadLocalFilter.getHttpServletRequest(), info, senderId);
			if(sealSepoaInfo.getSession("ID") == null || sealSepoaInfo.getSession("ID").isEmpty() || !sealSepoaInfo.getSession("ID").equals(senderId)) {
				throw new Exception("Creating session failed. Check the parameters.");
			}
			
			String aprvNo = MapUtils.getString(sealInfo, "APRV_NUMBER");
			commonInfo.put("COMPANY_CODE", companyCode);
			commonInfo.put("APRV_NUMBER", aprvNo);
			sxp = new SepoaXmlParser(this, "getAprvSeq");
			ssm = new SepoaSQLManager(sealSepoaInfo.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(commonInfo));
			String apprSeqMax = sf.getValue("APRV_SEQ_MAX", 0);
			String aprvSeq = Integer.toString(Integer.parseInt(apprSeqMax) + 1);
			commonInfo.put("APRV_SEQ", aprvSeq);

			if (ifData.containsKey("DOC_LIST")) {
				docList = (List<Map<String, String>>) MapUtils.getObject(ifData, "DOC_LIST", new ArrayList<>());
				Config config = new Configuration();
				PathManager pathManager = new PathManager(sealSepoaInfo, config);
				pathManager.setLayer("IF_TEMP");
				String tmpFolderPath = pathManager.getUniquePath();
				
				SepoaReqSealManager sm = new SepoaReqSealManager(sealSepoaInfo, config, getConnectionContext());
				
				
				for (int i = 0; i < docList.size(); i++) {
					Map<String, String> doc = docList.get(i);
					String file64 = MapUtils.getString(doc, "FILE64", "");
					if (!"".equals(file64)) {
						String docNo = DocumentUtil.getDocNumberRemote(sealSepoaInfo, "SMD").result[0];
						String orgFileName = MapUtils.getString(doc, "FILE_NAME", "");
						if ("".equals(orgFileName)) throw new Exception("'FILE_NAME' is necessary in DOC_LIST");
						doc.put("DOC_NO", docNo);
						doc.put("SMR_NUMBER", aprvNo);
						doc.put("SMD_NUMBER", docNo);
						sm.setDocBaiscInfo(doc);
						String filePath = tmpFolderPath+"\\"+docNo;
						File tempFile = new File(filePath);
						byte[] newFileBa = DatatypeConverter.parseBase64Binary(file64);
						FileUtils.writeByteArrayToFile(tempFile, newFileBa);
						
						sm.setTempDirectory(tmpFolderPath);
						Map<String, String> storedFileData = null;
						Map<String, String> file = new HashMap<String, String>();
						file.put("PATH", filePath);
						file.put("FILE_NAME_CLIENT", orgFileName);
						storedFileData = sm.doUploadDocumentFile("REQ_SEAL", file, doc);
						
						
						if(storedFileData == null) {
							int errCode = sm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
								throw new Exception("[ERROR "+errCode+"] File Setting Error");
							}
						} else {
							//String path = MapUtils.getString(storedFileData, "PATH", "");
							
						}
					}
					
				}
			}
			
			if (ifData.containsKey("APP_LINE")) {
				appLine = (List<Map<String, String>>) MapUtils.getObject(ifData, "APP_LINE", new ArrayList<>());
				
				String appLineJson = new Gson().toJson(appLine);
				sealInfo.put("APP_LINE_JSON", appLineJson);
			}
			
			sxp = new SepoaXmlParser(this, "doInsertSealInfo");
			ssm = new SepoaSQLManager(sealSepoaInfo.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(sealInfo, commonInfo);
			
			if (docList != null && docList.size() > 0) {
				sxp = new SepoaXmlParser(this, "doInsertDocList");
				ssm = new SepoaSQLManager(sealSepoaInfo.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(docList, commonInfo);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}

}
