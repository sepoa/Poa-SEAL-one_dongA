package sepoa.svc.tools;

import java.io.File;
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
import sepoa.fw.ux.outputs.OutputsWriterFactory;
import sepoa.fw.ux.outputs.OutputsWriterFactory.OUTPUT_TYPE;

@Description("Save")
public class Outputs extends SepoaService {
	public Outputs(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("Save")
	public SepoaOut doSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		File d = new File("D:/test.xls");
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			String data = headerData.get("unescaped_data");

			OutputsWriterFactory.getWriter(OUTPUT_TYPE.specification)
					.setDestination(d)
					.setJSONString(data)
					.write();

		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.println(info.getSession("ID"), this, e);
		}
		setValue("xxx.xls");
		setObject(d);
		return getSepoaOut();
	}
}
