package sepoa.svc.seal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.fw.approval.SepoaApproval;
import sepoa.fw.approval.SepoaApprovalLineSetting;
import sepoa.fw.approval.SepoaApprovalProgress;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.authority.Permission.PrincipalAgentType;
import sepoa.fw.util.authority.SepoaAuthorityObject.Authorized;
import sepoa.seal.SepoaReqCertExtManager;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자인장 - 공동인증서 반출 요청 모듈")
public class SepoaReqCertExtService extends SepoaSealService {

	public SepoaReqCertExtService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	protected String getClassName() {
		return this.getClass().getName();
	}

	private SepoaReqCertExtManager getCertExtManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		return new SepoaReqCertExtManager(info, config, ctx);
	}

	/** 목록 그리드용: 요청/반입 일자를 yyyy-MM-dd HH:mm:ss 로 표시 */
	private void formatListDateTimeColumns(List<Map<String, String>> resultList) {
		if (resultList == null || resultList.isEmpty()) {
			return;
		}
		for (Map<String, String> row : resultList) {
			// 화면 번호(ROWNUM)에 DB 채번(ROW_NUM) 유지
			String rowNum = row.get("ROW_NUM");
			if (StringUtils.isNotBlank(rowNum)) {
				row.put("ROWNUM", rowNum);
			}
			row.put("ADD_DATE", toDateTimeDisplay(row.get("ADD_DATE"), row.get("ADD_TIME")));
			row.put("CONFIRM_DATE", toDateTimeDisplay(row.get("CONFIRM_DATE"), row.get("CONFIRM_TIME")));
			row.put("EXT_IN_EXPECT_DATE", toDateTimeDisplay(row.get("EXT_IN_EXPECT_DATE"),
					toExpectTime(row.get("EXT_IN_EXPECT_HOUR"), row.get("EXT_IN_EXPECT_MINUTE"), row.get("EXT_IN_EXPECT_TIME"))));
			row.put("EXT_OUT_EXPECT_DATE", toDateTimeDisplay(row.get("EXT_OUT_EXPECT_DATE"),
					toExpectTime(row.get("EXT_OUT_EXPECT_HOUR"), row.get("EXT_OUT_EXPECT_MINUTE"), row.get("EXT_OUT_EXPECT_TIME"))));
		}
	}

	private String toExpectTime(String hour, String minute, String fullTime) {
		if (StringUtils.isNotBlank(fullTime)) {
			return fullTime;
		}
		String h = StringUtils.defaultString(hour).replaceAll("[^0-9]", "");
		String m = StringUtils.defaultString(minute).replaceAll("[^0-9]", "");
		if (h.isEmpty() && m.isEmpty()) {
			return "";
		}
		if (h.length() == 1) h = "0" + h;
		if (m.length() == 1) m = "0" + m;
		return (h + "00").substring(0, 2) + (m + "00").substring(0, 2) + "00";
	}

	private String toDateTimeDisplay(String date, String time) {
		if (StringUtils.isBlank(date)) {
			return "";
		}
		String d = date.replaceAll("[^0-9]", "");
		if (d.length() < 8) {
			return StringUtils.defaultString(date);
		}
		String t = StringUtils.defaultString(time).replaceAll("[^0-9]", "");
		if (t.length() < 6) {
			t = (t + "000000").substring(0, 6);
		}
		return d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6, 8)
				+ " " + t.substring(0, 2) + ":" + t.substring(2, 4) + ":" + t.substring(4, 6);
	}

	@Description("공동인증서 반출 요청등록 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doRequestService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = null;

		try {
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Configuration conf = new Configuration();
			String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"");
			Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);

			List<String> Id_data = new ArrayList<String>();
			Map<String, String> mailData = new HashMap<String, String>();
			SimpleDateFormat format = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분");

			String currentPrc = headerData.get("CURRENT_PRC").toString();
			String status = "";
			String smrNumber = "";
			boolean isGrant = false;

			SepoaReqCertExtManager srm = getCertExtManager(info, config, getConnectionContext());
			srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			Map<String, String> reqExtInfo = (Map<String, String>) parseMap.get("REQ_EXT_INFO");

			if (StringUtils.isNotBlank(currentPrc)) {
				srm.setDocBaiscInfo(reqExtInfo);

				switch (currentPrc) {
				case "CREATE":
					smrNumber = reqExtInfo.get("SMR_NUMBER");
					Map<String, String> paramMap = new HashMap<String, String>();
					paramMap.put("SMR_NUMBER", smrNumber);
					paramMap.put("APPLY_TYPE", "CEXT");

					if (srm.doSelectRequestList(paramMap) != null) {
						if (!srm.doUpdateReqCertExt(reqExtInfo)) {
							int errCode = srm.getLastErrCode();
							if (errCode != 0) setStatus(errCode);
						}
					} else {
						if (srm.doCreateReqCertExt() == null) {
							int errCode = srm.getLastErrCode();
							if (errCode != 0) setStatus(errCode);
						} else {
							if (!srm.doAddProcessHistory("CERTEXT_REQUEST")) {
								int errCode = srm.getLastErrCode();
								if (errCode != 0) setStatus(errCode);
							}
						}
					}
					status = "T";
					break;

				case "APPROVAL":
					ctx = getConnectionContext();
					smrNumber = reqExtInfo.get("SMR_NUMBER");
					headerData.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
					headerData.put("APP_TYPE", reqExtInfo.get("APP_TYPE"));
					headerData.put("SUX_WKEY1", smrNumber);
					headerData.put("SUX_WKEY2", currentPrc);

					SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
					sepoaApprovalLineSetting.doApprovalProcess(allData);

					if (!srm.checkAuthManager()) {
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_CERTEXT", smrNumber, null);
					}

					List<Map<String, String>> appLine = (List<Map<String, String>>) parseMap.get("APP_LINE");
					if (appLine != null) {
						for (int i = 0; i < appLine.size(); i++) {
							Map<String, String> appLineMap = appLine.get(i);
							if (appLineMap.get("APP_METHOD").equals("03")) {
								isGrant = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.READ);
							} else {
								isGrant = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.WRITE);
							}
							if (getStatus() == 1 && !isGrant) {
								int errCode = srm.getLastErrCode();
								if (errCode != 0) setStatus(errCode);
							}
						}
					}

					Map<String, String> map = new HashMap<>();
					map.put("returnType", "param");
					map.put("APP_NO", MapUtils.getString(headerData, "APP_NO", ""));
					map.put("APP_NO_SEQ", MapUtils.getString(headerData, "APP_NO_SEQ", ""));
					map.put("APP_STATUS", MapUtils.getString(headerData, "APP_STATUS", "I"));
					map.put("SAVE_MODE", MapUtils.getString(headerData, "SAVE_MODE", ""));
					setObject(map);

					SepoaApprovalProgress sepoaApprovalProgress = new SepoaApprovalProgress(ctx, info);
					sepoaApprovalProgress.doApprovalProcess(allData);

					status = "I";
					break;

				case "SIGN_APPROVE":
					smrNumber = reqExtInfo.get("SMR_NUMBER");
					if (!srm.checkAuthManager()) {
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_CERTEXT", smrNumber, null);
					}
					srm.doUpdateReqCertExtSignStatus("RW");
					status = "RW";
					break;

				case "SIGN_REJECT":
					smrNumber = reqExtInfo.get("SMR_NUMBER");
					if (!srm.checkAuthManager()) {
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_CERTEXT", smrNumber, null);
					}
					srm.doUpdateReqCertExtSignStatus("R");
					status = "R";
					break;

				case "RETURN":
					smrNumber = reqExtInfo.get("SMR_NUMBER");
					String confirmStatus = reqExtInfo.get("CONFIRM_STATUS");

					mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
					mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
					mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
					mailData.put("SMR_NUMBER", smrNumber);
					mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
					mailData.put("TIME", format.format(new Date()));
					mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
					mailData.put("CONTENTS", smrNumber);

					if ("RETURN_REQUEST".equals(confirmStatus)) {
						status = "RR";
					} else if ("RETURN_COMPLETE".equals(confirmStatus)) {
						status = "C";
						if (!srm.doAddProcessHistory("CERTEXT_COMPLETE")) {
							int errCode = srm.getLastErrCode();
							if (errCode != 0) setStatus(errCode);
						}
					}
					break;

				default:
					break;
				}
			}

			String curStatus = MapUtils.getString(reqExtInfo, "SMR_STATUS", null);
			if (curStatus == null || !"T".equals(curStatus) || !"T".equals(status)) {
				if (getStatus() == 1 && !srm.doUpdateReqCertExtStatus(status)) {
					int errCode = srm.getLastErrCode();
					if (errCode != 0) setStatus(errCode);
				}
			}

			if (getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}

	@Description("공동인증서 반출 처리대상 조회 서비스")
	@SuppressWarnings("unchecked")
	@Permission(key = "MENU_PROFILE_CODE", value = {"MUP2011020001", "MUP21111000001", "MUP21111900001"})
	public SepoaOut doSelectToDoList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqCertExtManager srm = getCertExtManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = null;
			String mode = MapUtils.getString(headerData, "MODE", null);

			// 처리대상: 반입대기(RR)만 조회
			headerData.put("SMR_STATUS", "RR");
			headerData.put("APPLY_TYPE", "CEXT");

			srm.setDocBaiscInfo(headerData);
			List<Map<String, String>> resultList = srm.doSelectRequestList();

			if (resultList == null) {
				int errCode = srm.getLastErrCode();
				if (("LIST_COUNT".equals(mode) || "TODO_COUNT".equals(mode)) && errCode == 104013) {
					setStatus(1);
					setValue(0);
					return getSepoaOut();
				} else if (errCode != 0) {
					setStatus(errCode);
				}
			}

			if (getStatus() == 1) {
				sf = new SepoaFormater(resultList);
				if (sf.getRowCount() > 0) {
					resultList = new SepoaFormater(resultList).getListMap();
				}
				if ("LIST_COUNT".equals(mode) || "TODO_COUNT".equals(mode)) {
					setValue(resultList.size());
				} else {
					formatListDateTimeColumns(resultList);
					setValue(resultList);
				}
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
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

	@Description("공동인증서 반출 신청현황 조회 서비스")
	@SuppressWarnings("unchecked")
	@Permission(key = "MENU_PROFILE_CODE", value = {"MUP2011020001", "MUP21111000001", "MUP21111900001", "MUP21111900000"})
	public SepoaOut doSelectRequestList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqCertExtManager srm = getCertExtManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = null;
			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
			Configuration config = new Configuration();
			String ctrlCode = info.getSession("CTRL_CODE").replaceAll("\\s", "");
			List<String> ctrlCodeList = Arrays.asList(ctrlCode.split(","));
			headerData.put("F_KEYWORD", MapUtils.getString(headerData, "F_KEYWORD", "").trim());
			String mode = MapUtils.getString(headerData, "MODE", null);

			boolean viewSameDept = SepoaModuleService.checkBusiness(config, info, "ETL1655792101584");
			if (!(myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code")) || ctrlCodeList.contains("EXT_CONFIRM"))) {
				if (viewSameDept) {
					headerData.put("REQ_USER_DEPT", info.getSession("DEPARTMENT"));
				} else {
					headerData.put("REQ_USER_ID", info.getSession("ID"));
				}
			}

			headerData.put("APPLY_TYPE", "CEXT");
			srm.setDocBaiscInfo(headerData);
			List<Map<String, String>> resultList = srm.doSelectRequestList();

			if (resultList == null) {
				int errCode = srm.getLastErrCode();
				if (("LIST_COUNT".equals(mode) || "TODO_COUNT".equals(mode)) && errCode == 104013) {
					setStatus(1);
					setValue(0);
					return getSepoaOut();
				} else if (errCode != 0) {
					setStatus(errCode);
				}
			}

			if (getStatus() == 1) {
				sf = new SepoaFormater(resultList);
				if (sf.getRowCount() > 0) {
					resultList = new SepoaFormater(resultList).getListMap();
				}
				if ("LIST_COUNT".equals(mode) || "TODO_COUNT".equals(mode)) {
					setValue(resultList.size());
				} else {
					formatListDateTimeColumns(resultList);
					setValue(resultList);
				}
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
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

	@Description("공동인증서 반출/반입 대장 조회 서비스")
	@SuppressWarnings("unchecked")
	@Permission(key = "MENU_PROFILE_CODE", value = {"MUP2011020001", "MUP21111000001", "MUP21111900001", "MUP21111900000"})
	public SepoaOut doSelectLedgerList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqCertExtManager srm = getCertExtManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = null;
			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
			Configuration config = new Configuration();
			String ctrlCode = info.getSession("CTRL_CODE").replaceAll("\\s", "");
			List<String> ctrlCodeList = Arrays.asList(ctrlCode.split(","));
			headerData.put("F_KEYWORD", MapUtils.getString(headerData, "F_KEYWORD", "").trim());

			boolean viewSameDept = SepoaModuleService.checkBusiness(config, info, "ETL1655792101584");
			if (!(myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code")) || ctrlCodeList.contains("EXT_CONFIRM"))) {
				if (viewSameDept) {
					headerData.put("REQ_USER_DEPT", info.getSession("DEPARTMENT"));
				} else {
					headerData.put("REQ_USER_ID", info.getSession("ID"));
				}
			}

			headerData.put("SMR_STATUS", "C");
			headerData.put("APPLY_TYPE", "CEXT");
			srm.setDocBaiscInfo(headerData);
			List<Map<String, String>> resultList = srm.doLedgerList(headerData);

			if (resultList == null) {
				int errCode = srm.getLastErrCode();
				if (errCode != 0) {
					setStatus(errCode);
				}
			}

			if (getStatus() == 1) {
				sf = new SepoaFormater(resultList);
				if (sf.getRowCount() > 0) {
					resultList = new SepoaFormater(resultList).getListMap();
				}
				formatListDateTimeColumns(resultList);
				// 대장: 실제 반출/반입일도 일시 표시
				if (resultList != null) {
					for (Map<String, String> row : resultList) {
						row.put("EXT_OUT_DATE", toDateTimeDisplay(row.get("EXT_OUT_DATE"), row.get("EXT_OUT_TIME")));
						row.put("EXT_IN_DATE", toDateTimeDisplay(row.get("EXT_IN_DATE"), row.get("EXT_IN_TIME")));
					}
				}
				setValue(resultList);
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
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

	@Description("공동인증서 반출 회수 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doWithdrawService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			String smrNumber = headerData.get("SMR_NUMBER");

			SepoaReqCertExtManager srm = getCertExtManager(info, config, getConnectionContext());

			if (!srm.doCheckWithdrawable(smrNumber)) {
				int errCode = srm.getLastErrCode();
				if (errCode != 0) setStatus(errCode);
			}

			if (getStatus() == 1) {
				Map<String, String> data = new HashMap<>();
				data.put("SMR_NUMBER", smrNumber);
				srm.setDocBaiscInfo(data);
				srm.doUpdateReqCertExtStatus("T");
				srm.doDeleteAppInfo(data);
			}

			if (getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}

	@Description("공동인증서 반출 요청정보 조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectRequestInfo(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, Object> resultMap = new HashMap<String, Object>();
			List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

			SepoaReqCertExtManager srm = getCertExtManager(info, new Configuration(), getConnectionContext());
			headerData.put("APPLY_TYPE", "CEXT");
			srm.setDocBaiscInfo(headerData);

			List<Map<String, String>> reqList = srm.doSelectRequestList();
			if (reqList == null) {
				int errCode = srm.getLastErrCode();
				if (errCode != 0) setStatus(errCode);
			} else {
				resultMap.put("REQ_EXT_INFO", reqList.get(0));
				List<Map<String, String>> docList = srm.doSelectDocumentList(headerData);
				if (docList != null) {
					resultMap.put("DOCUMENT_LIST", docList);
				} else {
					resultMap.put("DOCUMENT_LIST", new ArrayList<Map<String, String>>());
				}
			}

			if (getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
			} else {
				resultList.add(resultMap);
				setValue(new Gson().toJson(resultList));
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

	@Description("공동인증서 대장 저장")
	@SuppressWarnings("unchecked")
	public SepoaOut doSaveDocumentList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			String smrNumber = MapUtils.getString(headerData, "SMR_NUMBER", "");

			List<Map<String, String>> documentList = new ArrayList<Map<String, String>>();
			String parameter = MapUtils.getString(headerData, "PARAMETER", "");
			if (StringUtils.isNotBlank(parameter)) {
				String jsonStr = parameter.replaceAll("&#34;", "\"");
				Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);
				List<Map<String, Object>> parsed = (List<Map<String, Object>>) parseMap.get("DOCUMENT_LIST");
				if (parsed != null) {
					for (Map<String, Object> item : parsed) {
						Map<String, String> row = new HashMap<String, String>();
						for (Map.Entry<String, Object> e : item.entrySet()) {
							row.put(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
						}
						documentList.add(row);
					}
				}
				if (StringUtils.isBlank(smrNumber) && !documentList.isEmpty()) {
					smrNumber = documentList.get(0).get("SMR_NUMBER");
				}
			}

			if (StringUtils.isBlank(smrNumber)) {
				setStatus(0);
				setFlag(false);
				setMessage(Cache.text.get("MESSAGE.1002"));
				return getSepoaOut();
			}

			SepoaReqCertExtManager srm = getCertExtManager(info, new Configuration(), getConnectionContext());
			Map<String, String> key = new HashMap<String, String>();
			key.put("SMR_NUMBER", smrNumber);
			key.put("APPLY_TYPE", "CEXT");
			srm.setDocBaiscInfo(key);

			List<Map<String, String>> reqList = srm.doSelectRequestList(key);
			if (reqList == null || reqList.isEmpty()) {
				setStatus(0);
				setFlag(false);
				setMessage(Cache.text.get("MESSAGE.1002"));
				return getSepoaOut();
			}
			if (!"RW".equals(reqList.get(0).get("SMR_STATUS"))) {
				setStatus(0);
				setFlag(false);
				setMessage(Cache.text.get("MESSAGE.1002"));
				return getSepoaOut();
			}

			if (!srm.doSaveDocumentList(smrNumber, documentList)) {
				int errCode = srm.getLastErrCode();
				if (errCode != 0) setStatus(errCode);
			}

			if (getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
			} else {
				Commit();
				setMessage(Cache.text.get("MESSAGE.0001"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}

	@Description("공동인증서 반출 삭제 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doDeleteService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			SepoaReqCertExtManager srm = getCertExtManager(info, config, getConnectionContext());
			srm.setDocBaiscInfo(headerData);

			if (!srm.doDeleteReqCertExt(headerData)) {
				int errCode = srm.getLastErrCode();
				if (errCode != 0) setStatus(errCode);
			}

			if (getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}

	@Description("공동인증서 반출 결재진행")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalProgress(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();

			SepoaReqCertExtManager srm = getCertExtManager(info, config, ctx);
			Map<String, String> certInfo = new HashMap<String, String>();
			certInfo.put("SMR_NUMBER", headerData.get("SUX_WKEY1"));
			srm.setDocBaiscInfo(certInfo);

			if (getStatus() == 1 && !srm.doUpdateReqCertExtSignStatus("I")) {
				int errCode = srm.getLastErrCode();
				if (errCode != 0) setStatus(errCode);
			}

			if (getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}

		return getSepoaOut();
	}

	@Description("공동인증서 반출 결재완료")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalConfirm(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();

			if (SepoaApproval.APP_STATUS_FINAL.equals(MapUtils.getString(headerData, "APP_STATUS", ""))) {
				SepoaReqCertExtManager srm = getCertExtManager(info, config, ctx);

				Map<String, String> certInfo = new HashMap<String, String>();
				certInfo.put("SMR_NUMBER", headerData.get("SUX_WKEY1"));
				certInfo.put("CURRENT_PRC", headerData.get("SUX_WKEY2"));
				certInfo.put("REJECT_REMARK", headerData.get("ACCEPT_REASON"));
				certInfo.put("SIGN_DATE", SepoaDate.getShortDateString());
				certInfo.put("SIGN_TIME", SepoaDate.getShortTimeString());
				String signUserIdForced = MapUtils.getString(headerData, "SIGN_USER_ID_FORCED", null);
				certInfo.put("SIGN_USER_ID", signUserIdForced == null ? info.getSession("ID") : signUserIdForced);

				srm.setDocBaiscInfo(certInfo);

				// 결재승인 시 MANAGER 없이 바로 반출승인(RW)
				if (getStatus() == 1 && !srm.doUpdateReqCertExtSignStatus("RW")) {
					int errCode = srm.getLastErrCode();
					if (errCode != 0) setStatus(errCode);
				}

				if (getStatus() != 1) {
					Rollback();
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
				} else {
					Commit();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}

		return getSepoaOut();
	}

	@Description("공동인증서 반출 결재반려")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalReject(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();

			if (SepoaApproval.APP_STATUS_REJECT.equals(MapUtils.getString(headerData, "APP_STATUS", ""))) {
				SepoaReqCertExtManager srm = getCertExtManager(info, config, ctx);

				Map<String, String> certInfo = new HashMap<String, String>();
				certInfo.put("SMR_NUMBER", headerData.get("SUX_WKEY1"));
				certInfo.put("REJECT_REMARK", headerData.get("ACCEPT_REASON"));
				certInfo.put("SIGN_DATE", SepoaDate.getShortDateString());
				certInfo.put("SIGN_TIME", SepoaDate.getShortTimeString());
				String signUserIdForced = MapUtils.getString(headerData, "SIGN_USER_ID_FORCED", null);
				certInfo.put("SIGN_USER_ID", signUserIdForced == null ? info.getSession("ID") : signUserIdForced);

				srm.setDocBaiscInfo(certInfo);

				if (getStatus() == 1 && !srm.doUpdateReqCertExtSignStatus("R")) {
					int errCode = srm.getLastErrCode();
					if (errCode != 0) setStatus(errCode);
				}

				if (getStatus() != 1) {
					Rollback();
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_" + String.valueOf(this.getStatus())));
				} else {
					Commit();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}

		return getSepoaOut();
	}
}
