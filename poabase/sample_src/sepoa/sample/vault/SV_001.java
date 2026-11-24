package sepoa.sample.vault;

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
public class SV_001 extends SepoaService{
	public SV_001(String opt,SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0"); 
	}
	
	@Description("정보조회")
	public SepoaOut TestDoQuery(Map<String, String> data) throws Exception{
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();
			
			SepoaXmlParser sxp = new SepoaXmlParser(this, "getSFileInfo");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			String doSelectSfileInfoData = ssm.doSelect(data);
			List<Map<String, String>> sfileMapList = new SepoaFormater(doSelectSfileInfoData).getListMap();
			
			for(Map<String, String> tmpSfileInfo : sfileMapList) {
				System.out.println(tmpSfileInfo.get("SRC_FILE_NAME") + "커스텀 클래스 조회 진행");
			}
			
			setValue(new SepoaFormater(sfileMapList).getString());
			
			sxp = new SepoaXmlParser(this, "getMaxSeq");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(data));
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@Description("업로드")
	public SepoaOut TestDoUpload(Map<String, String> data) throws Exception{
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser(this, "Insert");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			System.out.println(data.get("FILE_NAME") + "커스텀 클래스 파일 업로드 진행");
			ssm.doInsert(data);
			Commit();
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@Description("삭제")
	public SepoaOut TestDoDelete(Map<String, String> data) throws Exception{
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser(this, "Delete");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			System.out.println(data.get("SRC_FILE_NAME") + "커스텀 클래스 파일 삭제 진행");
			
			ssm.doInsert(data);
		
			Commit();
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
}
