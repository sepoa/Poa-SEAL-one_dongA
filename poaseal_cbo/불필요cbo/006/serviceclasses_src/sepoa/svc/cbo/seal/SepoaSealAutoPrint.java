package sepoa.svc.cbo.seal;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.io.IOUtils;

import com.google.gson.Gson;

import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.seal.SepoaSealManagementManager;

@Description("창업진흥원 자동 인감 찍기")
public class SepoaSealAutoPrint extends SepoaService {
	final static String TEMPLATE = "<div class=\"contract-item contract-item-box contract-item-box-no-CI21110400002 contract-item-box-code-C contract-item-box-seal-data contract-item-box-seal-code-#MANAGE_NUMBER# contract-item-box-itemkey-1655706028708 contract-item-box-source-org contract-item-type-seal contract-item-unit-save-ready\" style=\"left: #LEFT#; top: #TOP#; width: #WIDTH#; height: #HEIGHT#; z-index: 1;\">"
			+ "<textarea class=\"contract-item contract-item-input contract-item-prop-seal_template-STD contract-item-prop-repeat-OFF contract-item-prop-cut_position-LEFT contract-item-prop-cut_percent-0 contract-item-prop-symmetry-OFF contract-item-prop-symmetry_cut_position-LEFT contract-item-prop-symmetry_cut_percent-0 contract-item-prop-symmetry_page_copy-OFF contract-item-prop-seal_width-2 contract-item-prop-seal_height-2\" readonly=\"\" wrap=\"off\" style=\"-webkit-transform: translateX(0%);transform: translateX(0%);\"></textarea>"
			+ "<div class=\"contract-item-delete\"></div></div>";

	public SepoaSealAutoPrint(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("인장 자동 찍기")
	public SepoaOut doAutoPrintSeal(List<Map<String, String>> selectedSealList) {
		setStatus(1);
		setFlag(true);
		try {
			if (selectedSealList == null) {
				setStatus(0);
				setFlag(false);
				setMessage("DOCUMENT INFO IS NULL");
				return getSepoaOut();
			}
			List<Map<String, String>> fixHtmlDataList = new ArrayList<>();

			// 인감 무조건 하나인데 첫번째 인장을 찍을려고 break도 추가
			for (Map<String, String> seal : selectedSealList) {
				String manageDocNo = seal.get("MANAGE_NUMBER");
				SepoaSealManagementManager sm = new SepoaSealManagementManager(info, new Configuration(),
						getConnectionContext());
				Map<String, String> sealData = sm.doSelectSealList(new SingletonMap<>("MANAGE_NUMBER", manageDocNo))
						.get(0);

				if (sealData == null) {
					int errCode = sm.getLastErrCode();
					if (errCode != 0) {
						setStatus(errCode);
					}
				}

				if (getStatus() == 1) {

//					sepoa.seal.auto.mark.01.page.001.img
					String objtCode = seal.get("OBJT_CODE");

					// 주의 : 페이지당 하나의 인장만 찍을 수 있음.
					// 주의 : 100페이지 이상 적용 불가.
					Config conf = new Configuration();
					Set<String> keys = conf.getMatchedKey("sepoa\\.seal\\.auto\\.mark\\." + objtCode + "\\.page\\..*\\.img");

					Map<String, String> map = new HashMap<String, String>();
					for (int i = 1; i < 100; i++) {
						if (keys.size() == 0) {
							break;
						}
						String page = StringUtils.leftPad(String.valueOf(i), 3, "0");
						String key = "sepoa.seal.auto.mark." + objtCode + ".page." + page + ".img";
						String manageNumber = conf.get(key, "");
						if (StringUtils.isBlank(manageNumber)) {
							continue;
						}

						String left = conf.get("sepoa.seal.auto.mark." + objtCode + ".page." + page + ".position.x");
						String top = conf.get("sepoa.seal.auto.mark." + objtCode + ".page." + page + ".position.y");
						String width = conf.get("sepoa.seal.auto.mark." + objtCode + ".page." + page + ".width");
						String height = conf.get("sepoa.seal.auto.mark." + objtCode + ".page." + page + ".height");
						
						keys.remove(key);

						String str = TEMPLATE;
						str = StringUtils.replace(str, "#MANAGE_NUMBER#", manageNumber);
						str = StringUtils.replace(str, "#LEFT#", left);
						str = StringUtils.replace(str, "#TOP#", top);
						str = StringUtils.replace(str, "#WIDTH#", width);
						str = StringUtils.replace(str, "#HEIGHT#", height);

						map.put(page, str);
					}

					String json = new Gson().toJson(map);

					Map<String, String> fixHtmlData = new HashMap<String, String>();
					fixHtmlData.put("PAGE", "1");
					fixHtmlData.put("FIX_HTML", json);
					fixHtmlDataList.add(fixHtmlData);
				}
				break;
			}

			if (getStatus() == 1) {
				setValue(fixHtmlDataList);
			}
		} catch (Exception e) {
			// e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	private String imageToBase64(String filePath) {
		String out = null;
		InputStream in = null;
		File file = new File(filePath);
		try {
			if (file.exists()) {
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
