package sepoa.svc.admin;

import java.util.Map;

import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.lang.StringUtils;

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

@Description("공지사항")
public class AD_202_3 extends SepoaService {
    public AD_202_3(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }

    @SuppressWarnings("unchecked")
    @Description("조회")
    public SepoaOut doQuery(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        ConnectionContext ctx = getConnectionContext();
        try{
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            String language = info.getSession("LANGUAGE");
            if(language == null || language.isEmpty()) {
            	language = "KO";
            }
            Map<String, String> languageMap = new SingletonMap("LANGUAGE", language);
            
            String userType = info.getSession("USER_TYPE");
            
            if(StringUtils.isNotBlank(userType)) {
            	headerData.put("USER_TYPE", userType);
            } else {
            	headerData.put("USER_TYPE", "A");
            }
            
            sxp = new SepoaXmlParser(this, "selectNotice");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doSelect(headerData, languageMap));
            
            if(!"doList".equals(headerData.get("QUERY_MODE"))){
                sxp = new SepoaXmlParser(this, "selectNoticeContents");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                setValue(ssm.doSelect(headerData));
            }
            
        }catch(Exception e){
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
