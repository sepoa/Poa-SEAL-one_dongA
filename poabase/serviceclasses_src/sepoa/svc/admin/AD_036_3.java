package sepoa.svc.admin;

import java.util.List;
import java.util.Map;

import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
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

@Description("서비스관리")
public class AD_036_3 extends SepoaService {
	public AD_036_3(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("상세조회")
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
	@Description("삭제")
	public SepoaOut doDelete(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true);

			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_RAW_GRID_DATA);
			sxp = new SepoaXmlParser(this, "doDelete");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(gridData);
			
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
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_RAW_GRID_DATA);
			for (Map<String, String> map : gridData) {
				sxp = new SepoaXmlParser(this, "doQueryExistRow");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(map));
				if (sf.getDouble("CNT", 0)>0) {
					sxp = new SepoaXmlParser(this, "doUpdateData");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(map);
				} else {
					sxp = new SepoaXmlParser(this, "doInsertData");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(map);
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
