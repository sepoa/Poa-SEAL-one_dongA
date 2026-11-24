package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

@Description("모듈 선택")
public class AD_121 extends SepoaService {

	public AD_121(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("조회")
	public SepoaOut doTreeGridQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doQueryModuleMaster");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			List<Map<String, String>> masterList = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			
			String keyword = headerData.get("F_KEYWORD");
			if(keyword != null && !keyword.isEmpty()) {
				List<Map<String, String>> keywordList = new ArrayList<Map<String, String>>();
				for(Map<String, String> item : masterList) {
					if(item.get("DESCRIPTION").contains(keyword)) {
						keywordList.add(item);
					}
				}
				
				List<Map<String, String>> tempList = new ArrayList<Map<String, String>>();
				for(Map<String, String> item : keywordList) {
					findKeywordData(item.get("TREE_PARENT_ID"), masterList, tempList);
				}
				keywordList.addAll(tempList);
				Collections.reverse(keywordList);
				
				masterList.clear();
				masterList.addAll(keywordList);
			}
			
			setValue(new SepoaFormater(masterList));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("Filter Component 데이터 가공")
	public void findKeywordData(String tree_id, List<Map<String, String>> masterList, List<Map<String, String>> tempList) {
		for(Map<String, String> master : masterList) {
			if(tree_id.equals(master.get("TREE_ID"))) {
				for(Map<String, String> item : tempList) {
					if(item.get("TREE_ID").equals(master.get("TREE_ID"))) {
						return;
					}
				}
				tempList.add(master);
				tree_id = master.get("TREE_PARENT_ID");
				findKeywordData(tree_id, masterList, tempList);
				break;
			}
		}
	}
}