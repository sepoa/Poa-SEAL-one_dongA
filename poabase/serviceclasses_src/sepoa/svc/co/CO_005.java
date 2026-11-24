package sepoa.svc.co;

import java.util.Map;

import org.apache.commons.collections.MapUtils;

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
import sepoa.fw.util.SepoaDataMapper;

@Description("메뉴")
public class CO_005 extends SepoaService {

    public CO_005(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
 
    @SuppressWarnings("unchecked")
    @Description("사용자메뉴조회")
    public SepoaOut getUserMenu(Map<String, Object> allData) {

        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            sxp = new SepoaXmlParser(this, "getUserMenu");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            
            String profile_no = MapUtils.getString(headerData, "profile_no", "");
            
            if("".equals(profile_no)){
                headerData.put("profile_no", info.getSession("MENU_PROFILE_CODE"));
            }
            
            headerData.put("HIDDEN_FLAG"  , "Y");
            setValue(ssm.doSelect(headerData));
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
    
    @SuppressWarnings("unchecked")
    @Description("관리자메뉴조회")
    public SepoaOut getUserMenu_mgt(Map<String, Object> allData) {

        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            sxp = new SepoaXmlParser(this, "getUserMenu");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            
            String profile_no = MapUtils.getString(headerData, "profile_no", "");
            
            if("".equals(profile_no)){
                headerData.put("profile_no", info.getSession("MENU_PROFILE_CODE"));
            }
            if(!"".equals(CommonUtil.getConfig("sepoa.sux.menu",""))){
            	headerData.put("ICON_TYPE_FLAG", "Y");
            }
            setValue(ssm.doSelect(headerData));
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
}
