package sepoa.svc.summary;

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
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("서머리")
public class Summary extends SepoaService {
    public Summary(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
    @SuppressWarnings({ "unchecked" })
    @Description("조회")
    public SepoaOut getSummaryCount(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            String sId = headerData.get("SCREEN_ID");
            sxp = new SepoaXmlParser(this, sId);
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            setValue(sf.getValue(0, 0));
        } catch (Exception e) {
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    } 
    
    @SuppressWarnings({ "unchecked" })
    @Description("리스트조회")
    public SepoaOut getList(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            String sId = headerData.get("SCREEN_ID");
            sxp = new SepoaXmlParser(this, sId + "_LIST");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doSelect(headerData));
        } catch (Exception e) {
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    } 
}