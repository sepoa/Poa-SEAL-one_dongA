package sepoa.svc.sap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;

import sepoa.fw.log.Logger;
import sepoa.fw.sap.srv.SepoaRFCService;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;

@Description("SAP JCo Proxy Logger")
public class SepoaJCoProxyLogger extends SepoaRFCService {

	public SepoaJCoProxyLogger(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Override
	@Description("called from sepoa service")
	public SepoaOut callRFCFunction(Map<String, Object> allData) {
		return getSepoaOut();
	}

	@SuppressWarnings("unchecked")
	@Override
	@Description("called from rfc proxy server")
	public SepoaOut fromRFCProxy(Map<String, Object> allData) {
		// get receive data
		Map<String, String> info = (Map<String, String>) allData.get(FUNCTION_INFO);
		Map<String, Object> params = (Map<String, Object>) allData.get(FUNCTION_PARAMS);
		
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			// logging function info
			List<String> logString = new LinkedList<String>();
			logString.add("--------------------------------------------------------------------");
			logString.add("FUNCTION INFO");
			for(Entry<String, String> entry : info.entrySet()) {
				logString.add("\t"+entry.getKey()+"="+entry.getValue()+"");
			}
			logString.add("--------------------------------------------------------------------");
			Logger.info.println("SYSTEM", this, logString.toArray(new String[] {}));
			
			// call dataProcessor
			dataProcessing(params);
		}catch(Exception e) {
			e.printStackTrace();
			Logger.err.println("SYSTEM", this, e);
		}
		
		setValue(result);
		return getSepoaOut(); 
	}

	@SuppressWarnings("unchecked")
	@Override
	public void dataProcessing(Map<String, Object> data) throws Exception{
		
		List<String> logString = new LinkedList<String>();
		
		for(Entry<String, Object> entry : data.entrySet()) {
			if(entry.getValue() instanceof String) {
				logString.add("TYPE PARAM : "+entry.getKey()+"="+entry.getValue()+"");
				logString.add("--------------------------------------------------------------------");
			}else if(entry.getValue() instanceof Map) {
				logString.add("TYPE STRUCTURE : ["+entry.getKey()+"]");
				Map<String, String> structure = (Map<String, String>) entry.getValue();
				for(Entry<String, String> inEntry : structure.entrySet()) {
					logString.add("\t"+inEntry.getKey()+"="+inEntry.getValue()+"");
				}
				logString.add("--------------------------------------------------------------------");
			}else if(entry.getValue() instanceof List) {
				logString.add("TYPE TABLE : ["+entry.getKey()+"]");
				List<Map<String, String>> table = (List<Map<String, String>>) entry.getValue();
				for(int i=0;i<table.size();i++) {
					Map<String, String> row = table.get(i);
					logString.add("\t---------");
					logString.add("\tROW["+i+"]");
					for(Entry<String, String> inEntry : row.entrySet()) {
						logString.add("\t\t"+inEntry.getKey()+"="+inEntry.getValue()+"");
					}
					logString.add("\t---------");
				}
				logString.add("--------------------------------------------------------------------");
			}
		}
		
		Logger.info.println("SYSTEM", this, logString.toArray(new String[] {}));
	}
}