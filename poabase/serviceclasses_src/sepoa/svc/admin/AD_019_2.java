package sepoa.svc.admin;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.auth.encrypt.SepoaAuthEncryptManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.login.SepoaLoginManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@SuppressWarnings({ "unused", "unchecked" })
@Description("사용자")
public class AD_019_2 extends SepoaService {


    public AD_019_2(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    @SuppressWarnings("unchecked")
    @Description("상세조회")
    public SepoaOut doFormLoad(Map<String, Object> allData) {
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
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    @Description("저장")
    public SepoaOut doFormSave(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            HttpServletRequest request = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
            
            if(!"".equals(MapUtils.getString(headerData, "PASSWORD_1"))){
                headerData.put("PASSWORD_1", SepoaAuthEncryptManager.getEncrypted(headerData.get("PASSWORD_1")));
            }
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            sxp = new SepoaXmlParser(this, "doUpdateSADDR");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            sf = new SepoaFormater(doFormLoad(allData).result[0]);
            if(sf.getRowCount() > 0) {
                SepoaSession.putValue(request, "THEME", sf.getValue("THEME", 0));
            }
            Commit();
        } catch (Exception e) {
            Rollback();
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    @Description("삭제")
    public SepoaOut doDelete(Map<String, Object> allData){
    	setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        
    	try {
    		Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            HttpServletRequest request = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);
            
            Commit();
    	}catch (Exception e) {
    		Rollback();
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
		}
    	return getSepoaOut();
    }
    
    @Description("수정")
    public SepoaOut changeUserInfo(Map<String, Object> allData){
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            
            String newPassword = headerData.get("NEW_PASSWORD");
//20171208 USER_TYPE 조건 삭제            
//            if("P".equals(headerData.get("USER_TYPE"))){
//                password = getEncryptedPassword(headerData.get("PASSWORD"));
//            }else if("S".equals(headerData.get("USER_TYPE"))){
//                password = headerData.get("PASSWORD");
//                if(newPassword != null && !"".equals(newPassword)){
//                    newPassword = getEncryptedPassword(newPassword);
//                }
//            }else{
//                throw new Exception(Cache.text.get("AD_019_2.MESSAGE0001"));//유저정보가 맞지않습니다.
//            }
            
            String password = SepoaAuthEncryptManager.getEncrypted(headerData.get("PASSWORD"));
            if(newPassword != null && !"".equals(newPassword)) {
            	newPassword = SepoaAuthEncryptManager.getEncrypted(newPassword);
            }
            
            headerData.put("password"  , password);
            headerData.put("user_id"   , info.getSession("ID"));
            headerData.put("house_code", info.getSession("HOUSE_CODE"));
            headerData.put("PASSWORD_1", newPassword);
            
            SepoaLoginManager manager = new SepoaLoginManager(info, new Configuration(), getConnectionContext());
    		if(!manager.initUser(info.getSession("ID")) || !manager.checkPassword(password)) {
    			throw new Exception(Cache.text.get("AD_019_2.MESSAGE0001"));//비밀번호가 틀립니다.
    		}
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            sxp = new SepoaXmlParser(this, "doUpdateSADDR");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            Commit();
        } catch (Exception e) {
            Rollback();
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