package sepoa.seal.rest;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBOpenException;
import sepoa.fw.jtx.CommitException;
import sepoa.fw.jtx.RollbackException;
import sepoa.fw.jtx.SepoaTransactionalResource;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;

public class SepoaSealRestManager {
	private String serviceName;
	private Object parameter;
	private ConnectionContext ctx;
	private SepoaOut so;
	private SepoaInfo info;
	
	public SepoaSealRestManager(String houseCode, String serviceName, Object parameter) throws Exception {
		this.serviceName = serviceName;
		this.parameter = parameter;
		this.ctx = new SepoaTransactionalResource().getConnectionContext();
		Map<String, String> header = MapUtils.getMap((Map<String, Object>)parameter, "HEADER", new HashMap<String, String>());
		String companyCode = MapUtils.getString(header, "COMPANY_CODE");
		String senderID = MapUtils.getString(header, "SENDER_ID", CommonUtil.getConfig("sepoa.interface.rest.sender.id.default"));
		
		this.info = new SepoaInfo(houseCode, "COMPANY_CODE="+companyCode+"^@^ID="+senderID+"^@^LANGUAGE=KO^@^");
	}

	public String getServiceName() {
		return serviceName;
	}

	public Object getParameter() {
		return parameter;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setParameter(Object parameter) {
		this.parameter = parameter;
	}

	public SepoaOut runService() throws Exception {
		try{
			this.so = ServiceConnector.doService(this.info, "sepoa.svc.rest.IF_"+serviceName, this.ctx, "doService", new Object[] {parameter});
			if(!this.so.flag) {
				throw new Exception(this.so.message);
			}

			((SepoaTransactionalResource) this.ctx).getUserTransaction().commit();
		} catch (Exception e) {
			try {
				if(this.ctx != null) {
					((SepoaTransactionalResource) this.ctx).getUserTransaction().rollback();	
				}
			} catch (RollbackException e1) {
				e1.printStackTrace();
			}
		} finally {
			if(this.ctx != null) {
				((SepoaTransactionalResource) this.ctx).release();	
			}
		}

		return this.so;
	}
}
