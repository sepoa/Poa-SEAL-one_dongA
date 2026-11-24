package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cache.Cache;
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
import sepoa.fw.util.SepoaString;

@Description("공지사항")
@Permission(key="WORK_TYPE", value="Z")
public class AD_201 extends SepoaService {
    public AD_201(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
    @SuppressWarnings("unchecked")
    @Description("리스트조회")
    public SepoaOut doQueryList(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        ConnectionContext ctx = getConnectionContext();
        try{
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            String language = info.getSession("LANGUAGE");
            if(language == null || language.isEmpty()) {
            	language = "KO";
            }
            Map<String, String> languageMap = new SingletonMap("LANGUAGE", language);
            if(!"Y".equalsIgnoreCase(info.getSession("MASTER_COMPANY_FLAG"))) {
            	headerData.put("COMPANY_CODE",info.getSession("COMPANY_CODE"));
            }else {
            	headerData.remove("COMPANY_CODE");
            }
            
            sxp = new SepoaXmlParser(this, "selectNoticeList");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData, languageMap));
            setValue(ssm.doSelect(headerData, languageMap));
          
        }catch(Exception e){
        	setFlag(false);
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }

    @SuppressWarnings("unchecked")
    @Description("조회")
    public SepoaOut doQuery(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        ConnectionContext ctx = getConnectionContext();
        try{
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            String language = info.getSession("LANGUAGE");
            if(language == null || language.isEmpty()) {
            	language = "KO";
            }
            Map<String, String> languageMap = new SingletonMap("LANGUAGE", language);
            if(!"Y".equalsIgnoreCase(info.getSession("MASTER_COMPANY_FLAG"))) {
            	headerData.put("COMPANY_CODE",info.getSession("COMPANY_CODE"));
            }else {
            	headerData.remove("COMPANY_CODE");
            }
            
            sxp = new SepoaXmlParser(this, "selectNotice");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData, languageMap));
            List<Map<String, String>> noticeList = null;
            List<Map<String, String>> contentsList = null;
            List<Map<String, String>> companyNameList = null;
            
            if(sf.getRowCount() > 0) {
            	noticeList = sf.getListMap();
            	
            	if(!"doList".equals(headerData.get("QUERY_MODE"))){
            		Map<String, String> noticeHeader = noticeList.get(0);
            		
            		List<String> applyComapnyStrList = new ArrayList<String>();
            		for(Map<String, String> notice : noticeList) {
            			
                    		sxp = new SepoaXmlParser(this, "selectApplyComapny");
                        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                        	sf = new SepoaFormater(ssm.doSelect(notice, languageMap));
                        	
                        	
            		}
            		companyNameList = sf.getListMap();
            		for(Map<String,String> companyName :companyNameList ) {
            			applyComapnyStrList.add(companyName.get("COMPANY_NAME_LOC"));
            			
            		}
        			
            		noticeHeader.put("AFFILIATED_COMPANY", StringUtils.join(applyComapnyStrList, ", "));
            		setValue(new SepoaFormater(noticeHeader));
            		
                	sxp = new SepoaXmlParser(this, "selectNoticeContents");
                	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                	sf = new SepoaFormater(ssm.doSelect(headerData, languageMap));
                	
                	contentsList = sf.getListMap();
                } else {
                	
                	if(noticeList != null) setValue(new SepoaFormater(noticeList));
                }
            	if(contentsList != null) setValue(new SepoaFormater(contentsList));
            }
        }catch(Exception e){
        	setFlag(false);
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }

    @SuppressWarnings("unchecked")
    @Description("저장")
    public SepoaOut doSave(Map<String, Object> allData) {
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        ConnectionContext ctx = getConnectionContext();
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
            Map<String, String> filteredHeaderData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            Map<String, String> addMap = new HashMap<String, String>();
            addMap.put("SUBJECT", MapUtils.getString(filteredHeaderData, "SUBJECT"));
            
            headerData.put("NOTE_TYPE", "N");
            
            
            String codeVal = headerData.get("COMPANY_CODE");
            
            
            List<String> codeList = Arrays.asList(codeVal.split(","));
            
            
            //내용 분할
            List<String> contentList = SepoaString.getSplitString(headerData.get("CONTENT"), 1000);
            if (contentList.size() <= 0) {
                contentList.add("");
            }

            //리스트맵형태로 전환
            List<Map<String, String>> gridData = new ArrayList<Map<String, String>>(contentList.size());
            
            for (int i = 0; i < contentList.size(); i++) {
                String splitedContent = contentList.get(i);
                Map<String, String> row = new HashMap<String, String>();
                row.put("NOTICE_SEQ", String.valueOf(i + 1));
                row.put("SPLITED_CONTENT", splitedContent);
                gridData.add(row);
            }
            
            String NOTICE_NUMBER = headerData.get("NOTICE_NUMBER");
            if("".equals(NOTICE_NUMBER)){
            	if (codeList.size() <= 1) {
                headerData.put("NOTICE_NUMBER", DocumentUtil.getDocNumberRemote(info, "NOTICE").result[0]);
                headerData.put("COMPANY_CODE",codeVal);
                
                sxp = new SepoaXmlParser(this, "insertNotice");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doInsert(headerData, addMap);
            	}else {
            		
                    headerData.put("NOTICE_NUMBER", DocumentUtil.getDocNumberRemote(info, "NOTICE").result[0]);
                    for (String keyList: codeList) {
                    	headerData.put("COMPANY_CODE",keyList);
                    
                    	sxp = new SepoaXmlParser(this, "insertNotice");
                    	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    	ssm.doInsert(headerData, addMap);
                    }
            	}
            }else{
                sxp = new SepoaXmlParser(this, "updateNotice");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doInsert(headerData, addMap);
                
                sxp = new SepoaXmlParser(this, "deleteNoticeContents");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doInsert(headerData);
            }
            
            sxp = new SepoaXmlParser(this, "insertNoticeContents");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doInsert(gridData, headerData);

            Commit();
            setStatus(1);
            setMessage(headerData.get("NOTICE_NUMBER"));
        } catch (Exception e) {
            Rollback();
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    @SuppressWarnings("unchecked")
    @Description("삭제")
    public SepoaOut doDelete(Map<String, Object> allData){
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        ConnectionContext ctx = getConnectionContext();
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            Map<String, String> tableMap = new HashMap<String, String>();
            
            String[] TABLE_NAMES = {"SNOTE","SNOTD"};
            for(String TABLE_NAME : TABLE_NAMES){
                tableMap.put("TABLE_NAME", TABLE_NAME);
                sxp = new SepoaXmlParser(this, "updateNoticeDelFlag");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doUpdate(headerData, tableMap);
            }
            
            Commit();
            setStatus(1);
            setMessage(Cache.text.get("MESSAGE.0001"));
        }catch(Exception e){
            Rollback();
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    @SuppressWarnings("unchecked")
    @Description("카운트저장")
    public SepoaOut updateViewCount(Map<String, Object> allData) {
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        ConnectionContext ctx = getConnectionContext();
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser(this, "updateViewCount");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doInsert(headerData);

            Commit();
            setStatus(1);
            setMessage(Cache.text.get("MESSAGE.0001"));
        } catch (Exception e) {
            Rollback();
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    @SuppressWarnings("unchecked")
    @Description("공지사항 팝업대상 리스트 조회")
    public SepoaOut selectNoticePopup(Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        ConnectionContext ctx = getConnectionContext();
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doSelect(headerData));
            setStatus(1);
        } catch (Exception e) {
            Rollback();
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
}
