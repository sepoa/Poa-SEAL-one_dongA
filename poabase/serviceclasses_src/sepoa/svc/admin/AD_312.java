package sepoa.svc.admin;

import java.util.Map;

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
import sepoa.fw.util.SepoaDataMapper;

@Description("사용자로그")
@Permission(key="WORK_TYPE", value="Z")
public class AD_312 extends SepoaService {

    public AD_312(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
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
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
}
