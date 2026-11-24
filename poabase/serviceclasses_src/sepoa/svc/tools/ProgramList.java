package sepoa.svc.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections.map.SingletonMap;

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
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.ux.analysis.Analyzer;

@Description("ProgramList")
public class ProgramList extends SepoaService {
	public ProgramList(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("Save")
	public SepoaOut doExcelDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			String[] pArray = headerData.get("PROFILE_CODE").split("#");

			Set<String> urlSet = new TreeSet<String>();

			for (String profileNo : pArray) {
				sxp = new SepoaXmlParser("sepoa.svc.co.CO_005", "getUserMenu");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				List<Map<String, String>> rs = new SepoaFormater(
						ssm.doSelect(new SingletonMap("profile_no", profileNo))).getListMap();
				if (rs != null && rs.size() > 0) {
					for (Map<String, String> row : rs) {
						String url = row.get("URL");
						if (url != null && !url.isEmpty()) {
							urlSet.add(url);
						}
					}
				}
			}

			setValue(CommonUtil.getConfig("sepoa.programlist.filename", "Program List.xls"));
			setObject(new Analyzer(urlSet).getExcel());
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			e.printStackTrace();
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@SuppressWarnings("unchecked")
	@Description("Query")
	public SepoaOut doQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, Map<String, String>> result = new TreeMap<String, Map<String, String>>();

			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> rs = sf.getListMap();

			if (rs != null && rs.size() > 0) {
				for (Map<String, String> row : rs) {
					String url = row.get("URL");
					if (url != null && !url.isEmpty()) {
						result.put(row.get("SCREEN_ID"), row);
					}
				}
				setValue(new SepoaFormater(new ArrayList<Map<String, String>>(result.values())).getString());
			} else {
				setValue(sf);
			}
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			e.printStackTrace();
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
}
