package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaConnectionInfo;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;

@SuppressWarnings({ "unused", "unchecked" })
@Description("채번관리")
@Permission(key="WORK_TYPE", value="Z")
public class AD_330 extends SepoaService {

    public AD_330(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    	
    @Description("조회")
    public SepoaOut doList(Map<String, Object> allData) {
    	setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
        	Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
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

    @Description("헤더조회")
    public SepoaOut doQueryHeader(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
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

    @Description("조회")
    public SepoaOut doQuery(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doSelect(headerData));
            
            Map<String, String> returnMap = new HashMap<String, String>();
            returnMap.put("returnType", "param");
            sxp = new SepoaXmlParser(this, "doQueryHeader");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            returnMap.put("headerData", ssm.doSelect(headerData));
            setObject(returnMap);
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
    
    @Description("삭제")
    public SepoaOut doDelete(Map<String, Object> allData) {

        setStatus(1);
        setFlag(true);
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
            
            ArrayList<ConnectionContext> ctxList = new ArrayList<ConnectionContext>(5);
			ctxList.add(getConnectionContext());
			String jndiStr = CommonUtil.getConfig("sepoa.admin.sync.jndi");
			if (!StringUtils.isBlank(jndiStr)) {
				String defaultDatasource = CommonUtil.getConfig("sepoa.app."+CommonUtil.getConfig("sepoa.app.name").toLowerCase()+".datasource");
				String[] jndiStrArray = jndiStr.split(";");
				for (String jndi : jndiStrArray) {
					String datasource = CommonUtil.getConfig("sepoa.app."+jndi+".datasource");
					if (!datasource.equalsIgnoreCase(defaultDatasource)) {
						joinTransaction(jndi);
						ctxList.add(getConnectionContext(jndi));
					}
				}
			}
            
			for(ConnectionContext ctx : ctxList) {
				if(gridData == null) {
	            	sxp = new SepoaXmlParser(this, "doDelete_SDCGL");
	            	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            	ssm.doDelete(headerData);
	            	
	            	sxp = new SepoaXmlParser(this, "doDelete_SDCLN");
	            	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            	ssm.doDelete(headerData);
	            }else {
	            	sxp = new SepoaXmlParser(this, "doDelete_SDCLN");
	            	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            	ssm.doDelete(gridData);
	            }
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
    
    @Description("저장")
    public SepoaOut doSave(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        SepoaFormater sf1 = null;
        
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
            
            ArrayList<ConnectionContext> ctxList = new ArrayList<ConnectionContext>(5);
			ctxList.add(getConnectionContext());
			String jndiStr = CommonUtil.getConfig("sepoa.admin.sync.jndi");
			if (!StringUtils.isBlank(jndiStr)) {
				String defaultDatasource = CommonUtil.getConfig("sepoa.app."+CommonUtil.getConfig("sepoa.app.name").toLowerCase()+".datasource");
				String[] jndiStrArray = jndiStr.split(";");
				for (String jndi : jndiStrArray) {
					String datasource = CommonUtil.getConfig("sepoa.app."+jndi+".datasource");
					if (!datasource.equalsIgnoreCase(defaultDatasource)) {
						joinTransaction(jndi);
						ctxList.add(getConnectionContext(jndi));
					}
				}
			}
            
			for(ConnectionContext ctx : ctxList) {
				String mode = headerData.get("SAVE_MODE");
	            if("INSERT".equals(mode)) {
	            	sxp = new SepoaXmlParser(this, "doInsert_SDCGL");
	                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                ssm.doInsert(headerData);
	            }else if("UPDATE".equals(mode)){
	            	sxp = new SepoaXmlParser(this, "doUpdate_SDCGL");
	                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                ssm.doUpdate(headerData);
	            }
	            
	            int yearLength = ("FULL".equals(CommonUtil.getConfig("sepoa.document.generation.year.type")))?4:2;
	            
	            for(Map<String, String> rowData : gridData) {
	            	String seq = rowData.get("SEQ");
	            	if("".equals(seq)) {
	            		String prefix = rowData.get("PREFIX_FORMAT");
	                    String year   = rowData.get("YEAR_FLAG");
	                    String month  = rowData.get("MONTH_FLAG");
	                    String day    = rowData.get("DAY_FLAG");
	                    String end    = rowData.get("END_NO") ;
	                    
	                    int numberLength = 0;
	                    
	                    if("1".equals(year) ) numberLength += yearLength;
	                    if("1".equals(month)) numberLength += 2;
	                    if("1".equals(day))   numberLength += 2;
	                    
	                    numberLength += end.length();
	                    
	                    StringBuffer currentNum = new StringBuffer();
	                    currentNum.append(prefix);
	                    for(int i=0;i<numberLength;i++) {
	                    	currentNum.append("0");
	                    }
	                    
	                    rowData.put("CURRENT_NO", currentNum.toString());
	                    
	                    sxp = new SepoaXmlParser(this, "getMaxSeq");
	                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                    sf = new SepoaFormater(ssm.doSelect(headerData));
	                    
	                    rowData.put("SEQ" , (sf.getLong("MAXSEQ", 0)+1)+"");
	                    
	                    sxp = new SepoaXmlParser(this, "doInsert_SDCLN");
	                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                    ssm.doInsert(rowData, headerData);
	            	}else {
	            		sxp = new SepoaXmlParser(this, "doUpdate_SDCLN");
	                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                    ssm.doInsert(rowData, headerData);
	            	}
	            }
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
}