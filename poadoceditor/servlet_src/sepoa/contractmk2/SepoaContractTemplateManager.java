package sepoa.contractmk2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import sepoa.filemanager.FileManager;
import sepoa.filemanager.FileManager.LEVEL;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.DocumentUtil;

public class SepoaContractTemplateManager extends sepoa.contractmk2.SepoaContractManager {
	
	public SepoaContractTemplateManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.checkPermission = false;
		this.historyFlag = false;
		this.linkFlag = false;
	}
	@Override
	public List<Map<String, String>> doListImage(){
		String[] layers = null;
		if("A".equals(this.docBasicInfo.get("STATUS"))) {
			layers = this.completeFileLayer;
		}else {
			layers = this.progressImageLayer;
		}
		
		List<Map<String, String>> result = new LinkedList<Map<String, String>>();
		for(String layer : layers) {
			List<Map<String, String>> layerFile = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_" + layer), LEVEL.FILE);
			if(layerFile != null) {
				result.addAll(layerFile);
			}
		}
		if (result.size() == 0) {
			this.errCode = 100101;
		} 
		return result;
	}
	
	@Override
	public boolean doUpdateEditorHTML(List<Map<String, String>> editorHtmls) {
		List<Map<String, String>> fileList = this.doListImage();
		if(fileList != null) {
			String layer = "CONT_DUMMY";
			List<Map<String, String>> dummyLayerFixHtml = editorHtmls.stream().filter((page) -> layer.equalsIgnoreCase(page.get("LAYER"))).collect(Collectors.toList());
			List<Map<String, String>> dummyFile = fileList.stream().filter((file) -> layer.equalsIgnoreCase(file.get("LAYER"))).collect(Collectors.toList());
			if(dummyLayerFixHtml != null && dummyLayerFixHtml.size() > 0) {
				String fileId = null;
				if(dummyFile != null && dummyFile.size() > 0) {
					Map<String, String> fileInfo = dummyFile.get(0);
					fileId = fileInfo.get("FILE_ID");
				} else {
					File dummyPDFFile = createDummyPDF();
					
					Map<String, String> uploadFileData = new HashMap<String, String>();
					uploadFileData.put("PATH", dummyPDFFile.getAbsolutePath());
					uploadFileData.put("FILE_NAME_CLIENT", layer + ".pdf");
					
					this.fileManager.setLayer(layer);
					List<Map<String, String>> result = this.fileManager.doUploadFile(uploadFileData, null, false);
					if(fileManager.getLastErrCode() != 0) {
						this.errCode = 100401;
						return false;
					}
					
					fileId = result.get(0).get("FILE_ID");
				}
				
				if(fileId != null) {
					for(Map<String, String> page : editorHtmls) {
						if(layer.equalsIgnoreCase(MapUtils.getString(page, "LAYER"))) {
							page.put("FILE_ID", fileId);
							page.put("FILE_TYPE", "C");
						}
					}
				}
			}
		}
		
		return super.doUpdateEditorHTML(editorHtmls);
	}
	
	@Override
	public boolean deleteDocument(boolean delFlag) {
		if(!"T".equals(this.docBasicInfo.get("STATUS"))) {
			//STATUS에 대한 처리가 없고, 기간이 오래된 템플릿을 삭제할 수 있어야해서 주석처리.
			//this.errCode = 100011;
			//return false;
		}
		this.fileManager.doDelete();
		int errCode = this.fileManager.getLastErrCode();
		if(errCode != 0) {
			System.out.println(errCode);
			this.errCode = 100010;
			return false;
		}
		
		Map<String, String> data = new HashMap<String, String>(this.docBasicInfo);
		if(delFlag) {
			data.put("DEL_FLAG"    , "Y");
		}
		if(!this.doUpdateDocument(data)) {
			this.errCode = 100013;
			return false;
		}
		this.docBasicInfo = data;
		if(!this.revokeAll()) {
			return false;
		}
		return true;
	}
	
	@Override
	public Map<String, String> doCopyDocument() {
		String newContNo = DocumentUtil.getDocNumber(info, "CT").result[0];
		
		Map<String, String> newInfo = new HashMap<String, String>(this.docBasicInfo);
		newInfo.put("CONT_NO", newContNo);
		newInfo.put("CONT_FORM_NAME", this.docBasicInfo.get("CONT_FORM_NAME")+"_사본");
		
		newInfo = this.doInsertDocument(newInfo);
		if(newInfo == null) {
			this.errCode = 101001;
			return null;
		}
		
		FileManager fm = new FileManager(info, config, ctx);
		if(fm.setDocNumber(newContNo+"_1", true) == null) {
			this.errCode = 101002;
			return null;
		}
		
		fm.doCopy(this.docBasicInfo.get("CONT_FORM_NO") + "_1", "");
		int err = fm.getLastErrCode();
		if(err != 0) {
			this.errCode = err;
			return null;
		}
		
		return newInfo;
	}
	
	private File createDummyPDF() {
		String dummyPDFPath = this.tempStorage+"/"+String.valueOf(System.nanoTime())+".pdf";
		File dummyPDFFile = new File(dummyPDFPath);
		try(PDDocument document = new PDDocument();) {
			document.addPage(new PDPage(PDRectangle.A4));
			document.save(dummyPDFFile);
			document.close();
		} catch (IOException e) {
			Logger.err.println(e.getMessage());
		}
		
		return dummyPDFFile;
	}
}
