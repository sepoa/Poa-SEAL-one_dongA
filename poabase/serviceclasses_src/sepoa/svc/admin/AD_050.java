package sepoa.svc.admin;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import platform.fw.account.AccountManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.login.SepoaLoginManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@SuppressWarnings({ "unused", "unchecked" })
@Description("사용자정보변경")
public class AD_050 extends SepoaService {


    public AD_050(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
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
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    @Description("비밀번호 정책 조회")
    public SepoaOut getPasswordPolicy(Map<String, Object> data) {
    	setStatus(1);
    	setFlag(true);
    	setValue(AccountManager.getPasswordPolicy());
    	return getSepoaOut();
    }
    
    @Description("저장")
    public SepoaOut doSave(Map<String, Object> allData){
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            Map<String, String> rawHeaderData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
            String password = rawHeaderData.get("PASS_WORD");
            if(StringUtils.isBlank(password)) {
            	setStatus(4);
            }else {
            	Config config = new Configuration();
            	AccountManager manager = new AccountManager(info, config, ctx);
            	if(!manager.initUser(info.getSession("ID")) || !manager.checkPassword(password)) {
            		setStatus(4);
            	}else if(!manager.updatePassword(rawHeaderData.get("NEW_PASSWORD_1"), rawHeaderData.get("NEW_PASSWORD_2"))) {
        			setStatus(manager.getLastErrCode());
            	}
            }
            if(getStatus() == 1) {
            	//NEW_PASSWORD 를 처리하지않으므로 아래 쿼리에서 password 가 저장되지 않음
            	//update SUSMT
                sxp = new SepoaXmlParser(this, "doUpdate_SUSMT");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doUpdate(headerData);
                
                //update SADDR
                sxp = new SepoaXmlParser(this, "doQuery_SADDR");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                sf = new SepoaFormater(ssm.doSelect(headerData));
                if(sf.getRowCount() > 0) {
                	sxp = new SepoaXmlParser(this, "doUpdate_SADDR");
                	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                	ssm.doUpdate(headerData);
                }else {
                	sxp = new SepoaXmlParser(this, "doInsert_SADDR");
                	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                	ssm.doInsert(headerData);
                }
                Commit();
                
                headerData.put("ID", info.getSession("ID"));
                setStatus(ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION", "forceLogin", new Object[] {headerData}).status);
            }else {
            	Rollback();
            }
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
    
    @Description("초기 비밀번호 변경")
    public SepoaOut doChangePassword(Map<String, Object> allData) {
    	this.setStatus(1);
    	this.setFlag(true);
    	
    	try {
    		Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
    		Map<String, String> rawHeaderData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
    		Map<String, String> sInfoMap = new HashMap<String, String>();
    		
    		String password = rawHeaderData.get("PASS_WORD");
    		String token = "";
    		
    		sInfoMap.put("HOUSE_CODE", MapUtils.getString(rawHeaderData, "HOUSE_CODE", "000"));
    		sInfoMap.put("ID", rawHeaderData.get("USER_ID"));
    		
    		SepoaInfo sInfo = new SepoaInfo(sInfoMap);
    		
    		if(StringUtils.isBlank(sInfoMap.get("ID"))) {
    			throw new Exception("User id mssing");
    		}
    		
    		if(StringUtils.isBlank(password)) {
    			setStatus(4);
    		} else {
    			Config config = new Configuration(sInfoMap.get("HOUSE_CODE"));
    			AccountManager manager = new AccountManager(sInfo, config, this.getConnectionContext());
    			if(!manager.initUser(sInfo.getSession("ID")) || !manager.checkPassword(password)) {
    				setStatus(4);
    			}else if(!manager.updatePassword(rawHeaderData.get("NEW_PASSWORD_1"), rawHeaderData.get("NEW_PASSWORD_2"))) {
    				setStatus(manager.getLastErrCode());
    			}
    		}
    		
    		this.Commit();
		} catch (Exception e) {
			this.Rollback();
			e.printStackTrace();
			this.setStatus(0);
			this.setValue("");
            this.setFlag(false);
            this.setMessage(e.getMessage());
            Logger.err.println("SYSTEM", this, e);
		}
    	
    	return this.getSepoaOut();
    }
}