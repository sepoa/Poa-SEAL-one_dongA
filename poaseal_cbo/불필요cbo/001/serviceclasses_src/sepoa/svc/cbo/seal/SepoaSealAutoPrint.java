package sepoa.svc.cbo.seal;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.collections4.map.SingletonMap;
import org.apache.tika.io.IOUtils;

import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.seal.SepoaSealManagementManager;

@Description("교보AXA 자동 인감 찍기")
public class SepoaSealAutoPrint extends SepoaService{
	public SepoaSealAutoPrint(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("인장 자동 찍기")
	public SepoaOut doAutoPrintSeal(List<Map<String, String>> selectedSealList) {
		setStatus(1);
		setFlag(true);
		try {
			if(selectedSealList == null) {
				setStatus(0);
				setFlag(false);
				setMessage("DOCUMENT INFO IS NULL");
				return getSepoaOut();
			}
			List<Map<String, String>> fixHtmlDataList = new ArrayList<>();
			
			// 인감 무조건 하나인데 첫번째 인장을 찍을려고 break도 추가
			for(Map<String, String> seal : selectedSealList) {
				String manageDocNo = seal.get("MANAGE_NUMBER");
				SepoaSealManagementManager sm = new SepoaSealManagementManager(info, new Configuration(), getConnectionContext());
				Map<String, String> sealData = sm.doSelectSealList(new SingletonMap<>("MANAGE_NUMBER", manageDocNo)).get(0);
				
				if(sealData == null) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				if(getStatus() == 1) {
					StringBuilder sb = new StringBuilder();
					sb.append("<div class=\"contract-item contract-item-box contract-item-box-no-" + sealData.get("ITEM_NO") + " contract-item-box-code-" + sealData.get("ITEM_CODE") + " contract-item-box-seal-data contract-item-box-seal-code-" + manageDocNo + " contract-item-box-itemkey-"+String.valueOf(System.nanoTime())+" contract-item-box-source-org contract-item-type-seal contract-item-unit-save-ready\" style=\"left: 60%; top: 92%; width: 9.52%; height: 6.74%; z-index: 1;\">");
					sb.append("	<textarea class=\"contract-item contract-item-input contract-item-prop-seal_template-STD contract-item-prop-repeat-OFF contract-item-prop-cut_position-LEFT contract-item-prop-cut_percent-0 contract-item-prop-symmetry-OFF contract-item-prop-symmetry_cut_position-LEFT contract-item-prop-symmetry_cut_percent-0 contract-item-prop-symmetry_page_copy-OFF contract-item-prop-seal_width-2 contract-item-prop-seal_height-2\" readonly=\"\" wrap=\"off\" placeholder=\""+ sealData.get("SUBJECT") +"\" style=\"-webkit-transform: translateX(0%);transform: translateX(0%);\"></textarea>");
					sb.append("	<div class=\"contract-item contract-item-input-origin\" style=\"display:none;\"></div>");
					sb.append("	<div class=\"contract-item contract-item-input-suffix\" style=\"display:none;\"></div>");
					sb.append("	<div class=\"contract-item contract-item-input-prefix\" style=\"display:none;\"></div>");
					sb.append("	<div class=\"contract-item contract-item-cover\" style=\"background-image: url(&quot;http://gw.kyoboaxa.com:17790/SepoaActionController.sux?module=seal.management&amp;action=doSelectSealPreviewImage&amp;id="+sealData.get("ITEM_NO")+"&quot;);\"></div>");
					sb.append("	<div class=\"contract-item-se-resize\"></div>");
					sb.append("	<div class=\"contract-item-delete\"></div>");
					sb.append("</div>");
					
					Map<String, String> fixHtmlData = new HashMap<String, String>();
					fixHtmlData.put("PAGE", "1");
					fixHtmlData.put("FIX_HTML", sb.toString());
					fixHtmlDataList.add(fixHtmlData);
				}
				break;
			}
			
			if(getStatus() == 1) {
				setValue(fixHtmlDataList);
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	private String imageToBase64(String filePath){
		String out = null;
		InputStream in = null;
		File file = new File(filePath);
		try {
			if(file.exists()) {
				in = CipherManager.getCipher().getInputStream(file, false);
				out = DatatypeConverter.printBase64Binary(IOUtils.toByteArray(in));
			}
		} catch (Exception e) {
			Logger.err.println(this.info.getSession("ID"), this, e);
			return null;
		}
	    return out;
	}
}
