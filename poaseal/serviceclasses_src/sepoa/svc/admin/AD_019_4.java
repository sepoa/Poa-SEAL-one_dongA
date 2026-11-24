package sepoa.svc.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.msg.Message;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("사용자직무")
@Permission(key="WORK_TYPE", value="Z")
public class AD_019_4 extends SepoaService {
    private Message msg;

    public AD_019_4(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
        msg = new Message(info, "AD_019_4");
    }

    @SuppressWarnings("unchecked")
    @Description("조회")
    public SepoaOut doQuery(Map<String, Object> allData) {
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            setStatus(1);
            setFlag(true);

            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doSelect(headerData));
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    @Description("삭제")
    @SuppressWarnings("unchecked")
    public SepoaOut doDelete(Map<String, Object> allData) {
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            setStatus(1);
            setFlag(true);
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            Map<String, String> userData = new HashMap<String, String>();
            userData.put("USER_ID", headerData.get("USER_ID"));
            List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,
                    SepoaDataMapper.KEY_GRID_DATA);
            sxp = new SepoaXmlParser(this, "doDelete");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

            int result  = ssm.doDelete(gridData,userData);
            for(Map<String, String> map : gridData){
                if("1".equals(map.get("REPRESENT_CTRL"))){
                    map.put("CTRL_CODE", "");
                    sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doUpdate(map,userData);
                    break;
                }
            }
            
            Commit();
            Logger.info.println(info.getSession("ID"), this, "서비스 삭제 row갯수 :"+result);
        } catch (Exception e) {
            Rollback();
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    
    @SuppressWarnings("unchecked")
    @Description("저장")
    public SepoaOut doSave(Map<String, Object> allData) {

        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            Map<String, String> userData = new HashMap<String, String>();
            userData.put("USER_ID", headerData.get("USER_ID"));
            List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,
                    SepoaDataMapper.KEY_GRID_DATA);
            sxp = new SepoaXmlParser(this, "doUpdateSBALN");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(gridData,userData);
            for(Map<String, String> map : gridData){
                if("1".equals(map.get("REPRESENT_CTRL"))){
                    sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doUpdate(map,userData);
                    break;
                }
            }//for    

            Commit();
        } catch (Exception e) {
            Rollback();
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
}
