package sepoa.svc.mg;

import java.util.List;
import java.util.Map;

import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.msg.Message;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;

public class MG_007 extends SepoaService {
    @SuppressWarnings("unused")
    private Message msg;

    public MG_007(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
        msg = new Message(info, "MG_007");
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public SepoaOut doDelete(Map<String, Object> allData) {
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            setStatus(1);
            setFlag(true);
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

            //결재선 전체 삭제 
            sxp = new SepoaXmlParser(this, "doDelete_SGWAH");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);
            
            //결재선 전체 삭제 
            sxp = new SepoaXmlParser(this, "doDelete_SGWAD");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);

            Commit();
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }

    @SuppressWarnings("unchecked")
    public SepoaOut doSave(Map<String, Object> allData) {
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            setStatus(1);
            setFlag(true);

            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);

            //결재선 전체 삭제 
            sxp = new SepoaXmlParser(this, "doDelete_SGWAH");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);
            
            //결재선 전체 삭제 
            sxp = new SepoaXmlParser(this, "doDelete_SGWAD");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);


            //저장
            sxp = new SepoaXmlParser(this, "doSave_SGWAH");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doInsert(headerData);
            
            //저장
            sxp = new SepoaXmlParser(this, "doSave_SGWAD");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doInsert(gridData);

            Commit();
        } catch (Exception e) {
            Rollback();
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
}
