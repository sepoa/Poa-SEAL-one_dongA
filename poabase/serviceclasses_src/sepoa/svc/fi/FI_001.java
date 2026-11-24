package sepoa.svc.fi;

import java.util.List;
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
import sepoa.fw.util.SepoaFormater;

@Description("파일첨부")
public class FI_001 extends SepoaService{
	public FI_001(String opt,SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0"); 
	}
	
	@Description("업로드")
	public SepoaOut Insert(Map<String, String> data) throws Exception{
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(data);
		
			Commit();
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("업로드-멀티")
	public SepoaOut InsertMulti(List<Map<String, String>> data) throws Exception{
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(data);
		
			Commit();
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("삭제")
	public SepoaOut Delete(Map<String, String> data) throws Exception{
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(data);
		
			Commit();
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("정보조회")
	public SepoaOut getSFileInfo(Map<String, String> data) throws Exception{
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(data));
			
			sxp = new SepoaXmlParser(this, "getMaxSeq");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(data));
			
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("파일경로조회")
	 public SepoaOut getImagePath(Map<String, String> data) throws Exception{
	        setStatus(1);
	        setFlag(true);
	        try {
	            ConnectionContext ctx = getConnectionContext();
	            SepoaXmlParser sxp = new SepoaXmlParser(this, "getImagePath");
	            SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            SepoaFormater sf = new SepoaFormater(ssm.doSelect(data));
	            String filePath = null;
	            String fileType = null;
	            if(sf.getRowCount() > 0){
	                filePath = sf.getValue("DES_FILE_NAME", 0);
	                fileType = sf.getValue("TYPE", 0);
	            }
	            setValue(filePath);
	            setValue(fileType);
	        } catch (Exception e) {
	            Rollback();
	            setStatus(0);
	            setFlag(false);
	            setMessage(e.getMessage());
	            Logger.err.println(info.getSession("ID"), this, e);
	        }
	        return getSepoaOut();
	    }
}
