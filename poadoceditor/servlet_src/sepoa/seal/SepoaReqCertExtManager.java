package sepoa.seal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;

public class SepoaReqCertExtManager extends SepoaSealManager {

	public SepoaReqCertExtManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.docBasicInfo = new HashMap<String, String>();
	}

	/**
	 * 공동인증서 반출 요청 생성 (SSETL INSERT)
	 */
	public Map<String, String> doCreateReqCertExt() {
		if(this.checkPermission) {
			if(!this.initAuthManager(
					info.getSession("HOUSE_CODE"),
					info.getSession("COMPANY_CODE"),
					"REQ_CERTEXT",
					this.docBasicInfo.get("SMR_NUMBER"),
					null)) {
				this.errCode = 100002;
				return null;
			} else {
				try {
					authObject.setOwner(
							info.getSession("COMPANY_CODE"),
							info.getSession("ID"),
							false);
					initAuthManager(
							info.getSession("HOUSE_CODE"),
							info.getSession("COMPANY_CODE"),
							"REQ_CERTEXT",
							this.docBasicInfo.get("SMR_NUMBER"),
							null);
				} catch (Exception e) {
					this.errCode = 100003;
					Logger.err.println(this.info.getSession("ID"), this, e);
					return null;
				}
			}
		}

		String applyType = MapUtils.getString(this.docBasicInfo, "APPLY_TYPE", null);
		if("".equals(applyType) || applyType == null) {
			this.errCode = 104006;
			return null;
		}

		if(this.doInsertSSETL(docBasicInfo) == null) {
			this.errCode = 104007;
			return null;
		}

		return new HashMap<>(docBasicInfo);
	}

	/**
	 * 공동인증서 반출 요청 수정 (SSETL UPDATE)
	 */
	public boolean doUpdateReqCertExt(Map<String, String> data) {
		if(!this.doUpdateSSETL(data)) {
			this.errCode = 104010;
			return false;
		}
		return true;
	}

	/**
	 * 공동인증서 반출 요청 삭제 (논리삭제)
	 */
	public boolean doDeleteReqCertExt(Map<String, String> data) {
		if(!this.doDeleteSSETL(data)) {
			this.errCode = 104008;
			return false;
		}
		return true;
	}

	/**
	 * 상태 업데이트
	 */
	public boolean doUpdateReqCertExtStatus(String status) {
		if(status == null || status.isEmpty()) {
			this.errCode = 104011;
			return false;
		}

		Map<String, String> data = new HashMap<String, String>();
		data.putAll(this.docBasicInfo);
		data.put("SMR_STATUS", status);

		if(!this.doUpdateReqCertExt(data)) {
			this.errCode = 104012;
			return false;
		}

		if("C".equals(status)) {
			this.doConfirmSSETL(data);
		} else if("RR".equals(status)) {
			this.doConfirmReqSSETL(data);
		}

		return true;
	}

	/**
	 * 결재 승인/반려에 의한 상태 업데이트
	 */
	public boolean doUpdateReqCertExtSignStatus(String status) {
		if(status == null || status.isEmpty()) {
			this.errCode = 104011;
			return false;
		}

		Map<String, String> data = new HashMap<String, String>();
		data.putAll(this.docBasicInfo);
		data.put("SMR_STATUS", status);

		this.doConfirmSignSSETL(data);
		return true;
	}

	/**
	 * 목록 조회 (CERTEXT 전용)
	 */
	public List<Map<String, String>> doSelectRequestList(Map<String, String> map) {
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		if(map == null) {
			map = this.docBasicInfo;
		}

		resultList = this.doSelectSSETL(map);
		if(resultList == null) {
			this.errCode = 104013;
			return null;
		}

		return resultList;
	}

	public List<Map<String, String>> doSelectRequestList() {
		return this.doSelectRequestList(null);
	}

	/**
	 * 공동인증서 반출/반입 대장 조회
	 */
	public List<Map<String, String>> doLedgerList(Map<String, String> map) {
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		if (map == null) {
			map = this.docBasicInfo;
		}

		resultList = this.doSelectLedgerList(map);
		if (resultList == null) {
			this.errCode = 104014;
			return null;
		}

		return resultList;
	}

	/**
	 * 회수가능여부 확인
	 */
	public boolean doCheckWithdrawable(String smrNumber) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doCheckCertExtWithdrawable");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(new org.apache.commons.collections4.map.SingletonMap("SMR_NUMBER", smrNumber))).getListMap();
			if (list.size() == 1) {
				String st = list.get(0).get("SMR_STATUS");
				String appStage = list.get(0).get("APP_STAGE");
				if (!("I".equals(st) && "1".equals(appStage))) {
					this.errCode = 102510;
					return false;
				}
			} else {
				this.errCode = 102416;
			}
		} catch(Exception e) {
			this.errCode = 100004;
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return true;
	}

	/**
	 * 결재선 삭제
	 */
	public boolean doDeleteAppInfo(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(data);
			return true;
		} catch(Exception e) {
			this.errCode = 102501;
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}

	// ========== Private SQL Methods ==========

	private Map<String, String> doInsertSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
			return data;
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	private boolean doUpdateSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
			return true;
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}

	private boolean doDeleteSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
			return true;
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}

	private List<Map<String, String>> doSelectSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list.size() > 0) {
				return list;
			}
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	private List<Map<String, String>> doSelectLedgerList(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectLedgerList");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			return new SepoaFormater(ssm.doSelect(data)).getListMap();
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	private void doConfirmSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doConfirmSSETL");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
	}

	private void doConfirmReqSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doConfirmReqSSETL");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
	}

	private void doConfirmSignSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doConfirmSignSSETL");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
	}

	/**
	 * 공동인증서 대장(서류) 조회
	 */
	public List<Map<String, String>> doSelectDocumentList(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectSSEDC");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			return new SepoaFormater(ssm.doSelect(data)).getListMap();
		} catch(Exception e) {
			this.errCode = 104013;
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	/**
	 * 공동인증서 대장 전체 저장 (기존 삭제 후 재등록)
	 */
	public boolean doSaveDocumentList(String smrNumber, List<Map<String, String>> documentList) {
		try {
			Map<String, String> key = new HashMap<String, String>();
			key.put("SMR_NUMBER", smrNumber);

			SepoaXmlParser delXp = new SepoaXmlParser(this.getClass(), "doDeleteAllSSEDC");
			SepoaSQLManager delSm = new SepoaSQLManager(info.getSession("ID"), this, ctx, delXp);
			delSm.setSepoaInfo(info);
			delSm.doDelete(key);

			if (documentList == null || documentList.isEmpty()) {
				return true;
			}

			int seq = 1;
			for (Map<String, String> doc : documentList) {
				Map<String, String> row = new HashMap<String, String>();
				row.putAll(doc);
				row.put("SMR_NUMBER", smrNumber);
				if (StringUtils.isBlank(row.get("SMR_SEQ"))) {
					row.put("SMR_SEQ", String.valueOf(seq));
				}
				if (StringUtils.isBlank(row.get("ADD_USER_ID"))) {
					row.put("ADD_USER_ID", StringUtils.defaultIfBlank(row.get("SEAL_MG_USER_ID"), info.getSession("ID")));
				}
				if (StringUtils.isBlank(row.get("SEAL_MG_USER_ID"))) {
					row.put("SEAL_MG_USER_ID", row.get("ADD_USER_ID"));
				}
				if (StringUtils.isBlank(row.get("ADD_DATE"))) {
					row.put("ADD_DATE", SepoaDate.getShortDateString());
				} else {
					row.put("ADD_DATE", row.get("ADD_DATE").replaceAll("[^0-9]", ""));
					if (row.get("ADD_DATE").length() > 8) {
						row.put("ADD_DATE", row.get("ADD_DATE").substring(0, 8));
					}
				}
				if (StringUtils.isBlank(row.get("SEAL_MG_SUBJECT"))) {
					row.put("SEAL_MG_SUBJECT", "법인 공동인증서");
				}
				if (StringUtils.isBlank(row.get("COUNT"))) {
					row.put("COUNT", "1");
				}
				if (row.get("SEAL_TYPE") == null) {
					row.put("SEAL_TYPE", "");
				}
				if (row.get("MANAGE_NUMBER") == null) {
					row.put("MANAGE_NUMBER", "");
				}
				if (row.get("SEAL_MG_DEPT") == null) {
					row.put("SEAL_MG_DEPT", "");
				}
				if (row.get("SUBJECT") == null) {
					row.put("SUBJECT", "");
				}
				if (row.get("ATTACH_NO") == null) {
					row.put("ATTACH_NO", "");
				}
				if (row.get("SEAL_MG_SUBJECT") == null) {
					row.put("SEAL_MG_SUBJECT", "");
				}

				SepoaXmlParser insXp = new SepoaXmlParser(this.getClass(), "doInsertSSEDC");
				SepoaSQLManager insSm = new SepoaSQLManager(info.getSession("ID"), this, ctx, insXp);
				insSm.setSepoaInfo(info);
				insSm.doInsert(row);
				seq++;
			}
			return true;
		} catch(Exception e) {
			this.errCode = 104002;
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
}
