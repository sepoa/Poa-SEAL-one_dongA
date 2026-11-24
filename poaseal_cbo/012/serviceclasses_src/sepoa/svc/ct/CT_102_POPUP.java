package sepoa.svc.ct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.ux.sUtils;
import sepoa.fw.ux.link.SepoaLinkManager;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@SuppressWarnings({ "unchecked" })
@Description("계약문서 재전송 CBO")
public class CT_102_POPUP extends SepoaService {

	public CT_102_POPUP(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("재전송")
	public SepoaOut doPushResend(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			Map<String, Object> objData = (Map<String, Object>) allData.get(SepoaModuleDataMapper.KEY_OBJECT_DATA);
			
			SepoaPushManager pushManager = new SepoaPushManager(info, config);
			List<Map<String, String>> listData = (List<Map<String, String>>) objData.get("list");
			List<String> messageSignerContents = new ArrayList<String>(listData.size());
			List<Map<String, String>> messageReceiverList = new ArrayList<>(listData.size());
			String addUserName = "";
			if(listData != null) {
				for(int i = 0; i < listData.size(); i++) {
					Map<String, String> signer = listData.get(i);
					String flag = signer.get("SIGN_FLAG");
					messageReceiverList.add(signer);
					messageSignerContents.add(Cache.text.get("POA_CONTRACT.SIGNER_STATUS_"+flag) + sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), signer));
					
					// 요청자 정보를 가져올 구간이 안보여서 SEQ의 서명자 이름을 가져옴. (계약생성자 이름) 
					if("1".equalsIgnoreCase(signer.get("SEQ"))){
						addUserName = signer.get("SIGNER_NAME");
					}
				}
			}
			
			for(Map<String, String> receiver : messageReceiverList) {
				if("".equals(receiver.get("LINK"))) {
					String contNo = receiver.get("CONT_NO");
					String contGlSeq = receiver.get("CONT_GL_SEQ");
					
					Map<String, String> linkParams = new HashMap<String, String>();
					linkParams.put("CONT_NO", contNo);
					linkParams.put("CONT_GL_SEQ", contGlSeq);
					
					SepoaLinkManager lm = new SepoaLinkManager(info, config, ctx);
					if(!lm.newLink("CONTRACT", contNo, contGlSeq)) {
						Logger.warn.println(info.getSession("ID"), this, "LinkManager Initialize failed > " + lm.getLastErrCode());
						continue;
					}
					
					linkParams = new HashMap<String, String>();
					linkParams.put("CONT_MODULE", "SIGN");
					String privateLink = lm.addUser(
							receiver.get("SIGNER_COMPANY_CODE"),
							receiver.get("SIGNER_ID"),
							linkParams);
					receiver.put("LINK", privateLink);
					
					if(StringUtils.isBlank(privateLink)) {
						Logger.warn.println(info.getSession("ID"), this, "Generate new link failed > " + lm.getLastErrCode());
						continue;
					}
					
					sxp = new SepoaXmlParser(this, "doUpdateSSIGN_LINK");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(receiver);
				}
				
				
				/*
				 * SIGN_FLAG
				 * 		W : SIGN_START
				 * 		M : SIGN_NEXT
				 * 		C : SIGN_REJECT
				 * 		S : SIGN_COMPLETE
				 * */
				String meesageType = "";
				switch(receiver.get("SIGN_FLAG")) {
					case "W" :
						meesageType = "SIGN_START";
						break;
					case "M" :
						meesageType = "SIGN_NEXT";
						break;
					case "C" :
						meesageType = "SIGN_REJECT";
						break;
					case "S" :
						meesageType = "SIGN_COMPLETE";
						break;
					default :
						break;
				}
				
				//create message params
				Map<String, String> messageParams = this.getMessageParams(meesageType, receiver); 
				messageParams.put("TITLE_COMMENT", "[" +receiver.get("SIGNER_NAME")+"] "+Cache.text.get("POA_CONTRACT." + meesageType + "_TITLE_COMMENT"));
				messageParams.put("CONTENTS_1", StringUtils.join(messageSignerContents, "<br/>"));
				messageParams.put("CREATOR_NAME", addUserName);
				
				Map<String, String> tmp = new HashMap<>();
				tmp.putAll(receiver);
				for(Entry<String, String> entry : messageParams.entrySet()) {
					messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
				}
				
				//create message
				SepoaMessage message = new SepoaMessage();
				message.setMessageType("POA_CONTRACT_WITH_BUTTON");
				message.setSepoaInfo(info);
				message.addParams(messageParams);
				message.addReceiver(receiver.get("SIGNER_ID"), receiver.get("SIGNER_NAME"), receiver.get("SIGNER_EMAIL"), null);
				
				//add message
				if(!pushManager.addMessage(message)) {
					Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
				}
				
			}
			
			pushManager.accept();
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
	
	protected Map<String, String> getMessageParams(String type, Map<String, String> defaultParams){
		Map<String, String> messageParams = new HashMap<String, String>();
		if(defaultParams != null){
			messageParams.putAll(defaultParams);
		}
		
		messageParams.put("SUBJECT"       , "[" + "동아제약" + "] " + defaultParams.get("CONT_SUBJECT") + " 계약완료 안내");
		messageParams.put("TITLE"         , Cache.text.get("POA_CONTRACT."+type));
		messageParams.put("TITLE_COMMENT" , Cache.text.get("POA_CONTRACT."+type+"_TITLE_COMMENT"));
		messageParams.put("NOTICE_COMMENT", Cache.text.get("POA_CONTRACT."+type+"_NOTICE_COMMENT"));
		messageParams.put("LABEL_1"       , Cache.text.get("POA_CONTRACT."+type+"_LABEL_1"));
		return messageParams;
	}
	
}