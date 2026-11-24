package sepoa.contract;

import java.util.HashMap;
import java.util.Map;

import sepoa.filemanager.FileManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.DocumentUtil;

public class SepoaContractTemplateManager extends SepoaContractManager{
	public SepoaContractTemplateManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.checkPermission = false;
		this.historyFlag = false;
		this.linkFlag = false;
	}
	
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
}
