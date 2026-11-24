package sepoa.svc.cbo.seal.ledger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.seal.SepoaReqSealManager;
import sepoa.svc.seal.SepoaSealService;

@Description("제증명서 발급대장 서비스")
public class SepoaCertLedgerService extends SepoaSealService {

	public SepoaCertLedgerService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("제증명서 발급대장 조회 - APPLY_TYPE D/U/C 중 SSISD가 있는 건만 표출")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectCertLedgerList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			ConnectionContext ctx = getConnectionContext();
			SepoaReqSealManager srm = new SepoaReqSealManager(info, new Configuration(), ctx);

			headerData.put("APPLY_TYPE", "D,U,C");

			if (SepoaModuleService.checkBusiness(new Configuration(), info, "CSW1687399123985")
					|| SepoaModuleService.checkBusiness(new Configuration(), info, "USW1687397809419")) {
				headerData.put("SMR_STATUS", "C,DW,DC,DR");
			} else {
				headerData.put("SMR_STATUS", "C,DC");
			}

			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");

			if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code"))) {
				headerData.put("MANAGER_ID", "");
				headerData.put("CONFIRM_USER_ID", "");
			} else if (info.getSession("CTRL_CODE").toUpperCase().contains("SEAL_CONFIRM")) {
				headerData.put("CONFIRM_USER_ID", "");
				headerData.put("MANAGER_ID", "");
			} else if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.seal.manager.porfile.code"))
					|| myMenuProfile.equals(CommonUtil.getConfig("sepoa.legal.porfile.code"))) {
				headerData.put("CONFIRM_USER_ID", info.getSession("ID"));
				headerData.put("MANAGER_ID", info.getSession("ID"));
				headerData.put("MY_SEAL", "true");
				headerData.put("ACCEPT_FLAG", "N");
			} else {
				setStatus(200051);
				throw new Exception("권한이 없는 사용자입니다.");
			}

			if (SepoaModuleService.checkBusiness(new Configuration(), info, "USW1637309480717")) {
				headerData.put("SEAL_MANAGER_CONFIRM_USER_FLAG", "TRUE");
			}

			List<Map<String, String>> resultList = srm.doLedgerList(headerData);

			if (resultList != null && resultList.size() > 0) {
				resultList = new SepoaFormater(resultList).getListMap();

				List<String> smrWithSsisd = getSmrNumbersWithSsisd(ctx, headerData);

				Iterator<Map<String, String>> it = resultList.iterator();
				while (it.hasNext()) {
					Map<String, String> row = it.next();
					String applyType = row.get("APPLY_TYPE");
					if (!"D".equals(applyType)) {
						String smrNumber = row.get("SMR_NUMBER");
						if (!smrWithSsisd.contains(smrNumber)) {
							it.remove();
						}
					}
				}

				setValue(resultList);
			} else {
				setValue(new ArrayList<>());
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

	private List<String> getSmrNumbersWithSsisd(ConnectionContext ctx, Map<String, String> headerData) {
		List<String> smrNumbers = new ArrayList<>();
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this, "getSmrNumbersWithSsisd");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> list = sf.getListMap();
			if (list != null) {
				for (Map<String, String> row : list) {
					smrNumbers.add(row.get("SMR_NUMBER"));
				}
			}
		} catch (Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return smrNumbers;
	}
}
