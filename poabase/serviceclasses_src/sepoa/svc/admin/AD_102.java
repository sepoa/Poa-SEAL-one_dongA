package sepoa.svc.admin;

import java.util.HashMap;
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

@Description("조직관리")
public class AD_102 extends SepoaService {

    public AD_102(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    @SuppressWarnings("unchecked")
    @Description("회사상세조회")
    public SepoaOut doQueryCompany(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
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
    @Description("공장상세조회")
    public SepoaOut doQueryPlant(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
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
    @Description("부서상세조회")
    public SepoaOut doQueryDept(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
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
    @Description("창고상세조회")
    public SepoaOut doQueryStorage(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
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
    @Description("직무상세조회")
    public SepoaOut doQueryBa(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
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
    public SepoaOut doQueryWorkType(Map<String, Object> allData) {
    	setStatus(1);
    	setFlag(true);
    	ConnectionContext ctx = getConnectionContext();
    	SepoaXmlParser sxp = null;
    	SepoaSQLManager ssm = null;
    	try {
    		Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
    		
    		sxp = new SepoaXmlParser();
    		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
    		setValue(ssm.doSelect(headerData));
    		
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
    
    @SuppressWarnings("unchecked")
    @Description("회사저장")
    public SepoaOut doSaveCompany(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            Map<String, String> address = new HashMap<String, String>();
            address.put("CODE_NO", headerData.get("COMPANY_CODE"));
            address.put("CODE_TYPE", "COMPANY");
            
            sxp = new SepoaXmlParser(this, "doSaveAddress");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData, address);
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
    
    @SuppressWarnings("unchecked")
    @Description("공장저장")
    public SepoaOut doSavePlant(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            String COMPANY_CODE = headerData.get("COMPANY_CODE");
            String PLANT_CODE = headerData.get("PLANT_CODE");
            Map<String, String> address = new HashMap<String, String>();
            address.put("CODE_NO", COMPANY_CODE+"_"+PLANT_CODE);
            address.put("CODE_TYPE", "PLANT");
            
            sxp = new SepoaXmlParser(this, "doSaveAddress");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData, address);
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
    
    @SuppressWarnings("unchecked")
    @Description("창고저장")
    public SepoaOut doSaveStorage(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            String COMPANY_CODE = headerData.get("COMPANY_CODE");
            String PLANT_CODE = headerData.get("PLANT_CODE");
            String STORAGE_CODE = headerData.get("STORAGE_CODE");
            Map<String, String> address = new HashMap<String, String>();
            address.put("CODE_NO", COMPANY_CODE+"_"+PLANT_CODE+"_"+STORAGE_CODE);
            address.put("CODE_TYPE", "STORAGE");
            
            sxp = new SepoaXmlParser(this, "doSaveAddress");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData, address);
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
    
    @SuppressWarnings("unchecked")
    @Description("부서저장")
    public SepoaOut doSaveDept(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
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
    
    @SuppressWarnings("unchecked")
    @Description("")
    public SepoaOut doSaveCtrl(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
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
    
    @SuppressWarnings("unchecked")
    @Description("회사삭제")
    public SepoaOut doDeleteCompany(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);
            
            String COMPANY_CODE = headerData.get("COMPANY_CODE");
            String PLANT_CODE = headerData.get("PLANT_CODE");
            Map<String, String> address = new HashMap<String, String>();
            address.put("CODE_NO", COMPANY_CODE+"_"+PLANT_CODE);
            address.put("CODE_TYPE", "PLANT");
            
            sxp = new SepoaXmlParser(this,"doDeleteAddress");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData, address);
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
    
    @SuppressWarnings("unchecked")
    @Description("공장삭제")
    public SepoaOut doDeletePlant(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);
            
            String COMPANY_CODE = headerData.get("COMPANY_CODE");
            String PLANT_CODE = headerData.get("PLANT_CODE");
            Map<String, String> address = new HashMap<String, String>();
            address.put("CODE_NO", COMPANY_CODE+"_"+PLANT_CODE);
            address.put("CODE_TYPE", "PLANT");
            
            sxp = new SepoaXmlParser(this, "doDeleteAddress");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData, address);
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
    
    @SuppressWarnings("unchecked")
    @Description("창고삭제")
    public SepoaOut doDeleteStorage(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);
            
            String COMPANY_CODE = headerData.get("COMPANY_CODE");
            String PLANT_CODE = headerData.get("PLANT_CODE");
            String STORAGE_CODE = headerData.get("STORAGE_CODE");
            Map<String, String> address = new HashMap<String, String>();
            address.put("CODE_NO", COMPANY_CODE+"_"+PLANT_CODE+"_"+STORAGE_CODE);
            address.put("CODE_TYPE", "STORAGE");
            
            sxp = new SepoaXmlParser(this, "doDeleteAddress");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData, address);
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
    
    @SuppressWarnings("unchecked")
    @Description("부서삭제")
    public SepoaOut doDeleteDept(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);
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
    
    @SuppressWarnings("unchecked")
    @Description("")
    public SepoaOut doDeleteCtrl(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);
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