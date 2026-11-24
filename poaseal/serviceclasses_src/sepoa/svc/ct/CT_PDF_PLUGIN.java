package sepoa.svc.ct;

import java.io.File;
import java.util.Map;

import sepoa.fw.log.Logger;
import sepoa.fw.module.pdf.content.SepoaPDFContent;
import sepoa.fw.module.pdf.content.SepoaPDFContentImage;
import sepoa.fw.module.pdf.content.SepoaPDFContentPositionSetter;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;

@Description("계약모듈 워터마크 플러그인")
public class CT_PDF_PLUGIN extends SepoaService {
	public CT_PDF_PLUGIN(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("계약모듈 워터마크 플러그인 - mk2")
	public SepoaOut createPdfWatermark(String taskNumber, Map<String, String> contractInfo) throws Throwable {
		try {
			SepoaPDFContent result = null;
			File watermarkFile = new File(CommonUtil.getConfig("sepoa.contract.watermark"));
			if(watermarkFile.exists()) {
				result = new SepoaPDFContentImage(watermarkFile);
				
				// 투명도
				result.setOpacity(0.2);
			}
			
			setValue(result);
		} catch (Throwable e) {
			Logger.debug.println(e);
			throw new Exception(e);
		}
		return getSepoaOut();
	}
	
	@Description("계약모듈 워터마크 플러그인 - 후속처리구간")
	public SepoaOut createPdfAfterEvent(String taskNumber, Map<String, String> contractInfo) {
		setFlag(true);
		return getSepoaOut();
	}
}
