package sepoa.svc.approval;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;

@Description("통합결재 연계 서비스")
public class SepoaGWApprovalService extends SepoaService {

	final static String FORM_ID_SEAL = "SEAL_SE_01"; // 날인신청
	final static String FORM_ID_EXTERNAL = "SEAL_EX_01"; // 반출신청
	final static String FORM_ID_ISSUE = "SEAL_IS_01"; // 증명서발급
	final static String FORM_ID_CONTRACT = "SEAL_CT_01"; // 전자계약

	//final static String USE = "USE";			// 사용인감 날인신청
	//final static String CORP = "CORP";			// 법인인감 날인신청
	//final static String EXTERNAL = "EXTERNAL";	// 반출신청
	//final static String CERT = "CERT";			// 증명서 발급
	
	public SepoaGWApprovalService(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("통합결재 연계 서비스")
	public SepoaOut sendApproval(Map<String, Object> allData) throws Exception {

		String userId = null;
		String formId = null;
		String orgApprDocNo = null;

		setFlag(true);
		setStatus(1);

		List<File> fileList = new ArrayList<File>();
		try {
			Config config = new Configuration();
			String url = config.getString("approval.url");

			userId = info.getSession("ID").toLowerCase();

			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Logger.dev.println(" ########### headerData ######### ");
			Logger.dev.println(" == > " + headerData);

			String applyType = headerData.get("APPLY_TYPE");
			orgApprDocNo = headerData.get("SMR_NUMBER");
			if ("USE".equals(applyType) || "CORP".equals(applyType)) {
				// 날인 신청
				formId = FORM_ID_SEAL;

			} else if ("EXTERNAL".equals(applyType)) {
				formId = FORM_ID_EXTERNAL;

			} else if ("CERT".equals(applyType)) {
				formId = FORM_ID_ISSUE;

			}

			setValue(url);
			Map<String, String> resultMap = new HashMap<String, String>();
			resultMap.put("userId", userId);
			resultMap.put("formId", formId);
			resultMap.put("orgApprDocNo", orgApprDocNo);

			String result = new ObjectMapper().writeValueAsString(resultMap);
			setValue(result);

		} catch (Throwable exception) {
			Rollback();
			// exception.printStackTrace();
			setFlag(false);
			setStatus(0);
			setMessage(exception.getMessage());
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(this.getClass().getSimpleName()).append(" ERROR : ")
					.append(exception.getMessage()).toString());
		} finally {
			for (File file : fileList) {
				FileUtils.forceDelete(file);
			}
		}

		return getSepoaOut();
	}

	@Description("결재콜백")
	public SepoaOut doApproval(Map<String, Object> inDataMap) {
		// -----------------------------------------------------------
		// actionType : naver dooray / resultStatus
		// -----------------------------------------------------------
		// final String RESULT_STATUS_SUBMITTED = "A"; // 상신
		// final String RESULT_STATUS_PROCEED = ""; // 진행
		// final String RESULT_STATUS_APPROVED = "C"; // 결재완료
		// final String RESULT_STATUS_CANCEL = "AC"; // 상신취소
		// final String RESULT_STATUS_REJECTED = "R"; // 반려
		final String RESULT_STATUS_SUBMITTED = "DRAFT"; // 상신 / 두레이: 기안
		final String RESULT_STATUS_PROCEED = "PROGRESS"; // 진행 / 두레이: 승인(진행중)
		final String RESULT_STATUS_APPROVED = "COMPLETION"; // 결재완료 / 두레이: 승인(완료)
		final String RESULT_STATUS_CANCEL = "CANCEL"; // 상신취소
		final String RESULT_STATUS_REJECTED = "RETURN"; // 반려
		// DRAFT 기안
		// PROGRESS 승인(진행중)
		// COMPLETION 승인(완료)
		// CANCEL 상신취소
		// RETURN 반려
		// -------------------------------------------------------
		// TEMPORARY 임시저장 필요없음.
		// EDIT_CONTENTS 본문수정 필요없음.
		// -----------------------------------------------------------
		// approveDocumentStatus : naver dooray /
		// -----------------------------------------------------------
		final String SSID_SMR_STATUS_PROCEED = "I";
		final String SSID_SMR_STATUS_TEMPORARY = "T";
		final String SSID_SMR_STATUS_REJECTED = "R";
		final String SSID_SMR_STATUS_COMPLETED = "CR";
		final String SSID_SMR_STATUS_EXT_COMPLETE = "RW"; // 반입대기
		// PROGRESS 진행중
		// TEMPORARY 임시저장
		// RETURN 반려
		// COMPLETION 완료
		// DISPOSAL 폐기

		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Logger.debug.println("DataMap recieved from GW : " + inDataMap);
			// Map<String, Object> externalParamMap = (Map<String,
			// Object>)dataMap.get("externalParam");

			String resultStatus = inDataMap.get("actionType").toString(); // ex) DRAFT(기안)
			// String formId = inDataMap.get("formId").toString(); // ex) SEAL_SE_01
			// String orgApprDocNo = inDataMap.get("orgApprDocNo").toString();
			Map<String, String> externalParamMap = (Map<String, String>) inDataMap.get("externalParam");
			String formId = externalParamMap.get("formId");
			String orgApprDocNo = externalParamMap.get("orgApprDocNo");
			// String documentType = dataMap.get("documentType").toString();
			// Logger.debug.println("documentType : " + documentType);
			// String externalParam = dataMap.get("externalParam").toString();
			// Logger.debug.println("externalParam : " + externalParam);
			// Logger.debug.println("DataMap status : " + resultStatus);
			// Logger.debug.println("DataMap formId: " + formId);

			Map<String, String> dataMap = new HashMap<String, String>();

			dataMap.put("SMR_NUMBER", orgApprDocNo);

			ConnectionContext ctx = getConnectionContext();

			if (FORM_ID_EXTERNAL.equals(formId)) {

			} else if (FORM_ID_SEAL.equals(formId)) {
				sxp = new SepoaXmlParser(this, "doCountSSIAD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfCount = new SepoaFormater(ssm.doSelect(dataMap));
				if ("0".equals(sfCount.getValue("CNT", 0))) {
					throw new Exception("Data does not exist.");
				}
			}

			if (RESULT_STATUS_SUBMITTED.equals(resultStatus)) {
				dataMap.put("ACCEPT_DATE", SepoaDate.getShortDateString());
				dataMap.put("ACCEPT_TIME", SepoaDate.getShortTimeString());
				dataMap.put("SMR_STATUS", SSID_SMR_STATUS_PROCEED);
				// 결재 상신
				if (FORM_ID_EXTERNAL.equals(formId)) {

					sxp = new SepoaXmlParser(this, "updateSSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);

					dataMap.put("COMPLETE_FLAG", "N");
					sxp = new SepoaXmlParser(this, "doChangeFlag");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setSepoaInfo(info);
					ssm.doUpdate(dataMap);
				} else if (FORM_ID_SEAL.equals(formId) || FORM_ID_ISSUE.equals(formId)) {

					sxp = new SepoaXmlParser(this, "updateSSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);
				}
			} else if (RESULT_STATUS_PROCEED.equals(resultStatus)) {
				// 결재 진행중
			} else if (RESULT_STATUS_REJECTED.equals(resultStatus)) {
				// 결재 반려
				dataMap.put("ACCEPT_DATE", SepoaDate.getShortDateString());
				dataMap.put("ACCEPT_TIME", SepoaDate.getShortTimeString());

				if (FORM_ID_EXTERNAL.equals(formId)) {
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_REJECTED);

					sxp = new SepoaXmlParser(this, "updateSSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);
				} else if (FORM_ID_SEAL.equals(formId) || FORM_ID_ISSUE.equals(formId)) {
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_REJECTED);

					sxp = new SepoaXmlParser(this, "updateSSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);
				}
			} else if (RESULT_STATUS_APPROVED.equals(resultStatus)) {
				// 결재 완료
				dataMap.put("ACCEPT_DATE", SepoaDate.getShortDateString());
				dataMap.put("ACCEPT_TIME", SepoaDate.getShortTimeString());

				if (FORM_ID_EXTERNAL.equals(formId)) {
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_EXT_COMPLETE);
					sxp = new SepoaXmlParser(this, "updateSSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);

					sxp = new SepoaXmlParser(this, "doQuerySSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(dataMap));
					dataMap.putAll(sf.getListMap().get(0));

				} else if (FORM_ID_SEAL.equals(formId) || FORM_ID_ISSUE.equals(formId)) {
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_COMPLETED);
					sxp = new SepoaXmlParser(this, "updateSSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);

					sxp = new SepoaXmlParser(this, "doQuerySSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(dataMap));
					dataMap.putAll(sf.getListMap().get(0));

				}
				sxp = new SepoaXmlParser(this, "doAddSAPPP");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(dataMap);

			} else if (RESULT_STATUS_CANCEL.equals(resultStatus)) {
				// 결재 취소
				dataMap.put("ACCEPT_DATE", SepoaDate.getShortDateString());
				dataMap.put("ACCEPT_TIME", SepoaDate.getShortTimeString());

				if (FORM_ID_EXTERNAL.equals(formId)) {
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_TEMPORARY);
					sxp = new SepoaXmlParser(this, "updateSSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);

				} else if (FORM_ID_SEAL.equals(formId) || FORM_ID_ISSUE.equals(formId)) {
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_TEMPORARY);
					sxp = new SepoaXmlParser(this, "updateSSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);

				}
			}
			Commit();

		} catch (Exception e) {
			// e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("통합결재 연계 서비스 - HTML생성")
	public SepoaOut doGenerateHtml(Map<String, String> inData) throws Exception {

		setFlag(true);
		setStatus(0);
		setMessage("");
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		map.put("header", headerMap);
		Map<String, Map<String, String>> resultMap = new HashMap<String, Map<String, String>>();
		Map<String, String> contentMap = new HashMap<String, String>();
		resultMap.put("content", contentMap);
		map.put("result", resultMap);

		List<File> fileList = new ArrayList<File>();
		try {
			Config config = new Configuration();
			String systemDomain = config.get("sepoa.system.domain.name") +"/link.sux?ln=";

			ConnectionContext ctx = getConnectionContext();
			Logger.dev.println(" ########### inData ######### ");
			Logger.dev.println(" == > " + inData);

			String userId = inData.get("userId");
			String formId = inData.get("formId");
			String orgApprDocNo = inData.get("orgApprDocNo");

			inData.put("SMR_NUMBER", orgApprDocNo);

			if (StringUtils.isBlank(userId)) {
				throw new Exception("Parameter[userId] is empty.");
			}
			if (StringUtils.isBlank(formId)) {
				throw new Exception("Parameter[formId] is empty.");
			}
			if (StringUtils.isBlank(orgApprDocNo)) {
				throw new Exception("Parameter[orgApprDocNo] is empty.");
			}

			SepoaFormater sfHeader = null;
			String title = null;
			String formEditorData = null;
			if (FORM_ID_SEAL.equals(formId)) {
				// 날인 신청
				formId = FORM_ID_SEAL;
				String template = config.getString("approval.seal.template.file");

				sxp = new SepoaXmlParser(this, "doSelectSSIAD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfHeader = new SepoaFormater(ssm.doSelect(inData));
				
				String explanation = sfHeader.getValue("EXPLANATION", 0);
		        String reqSealUseType = sfHeader.getValue("REQ_SEAL_USE_TYPE", 0);
		        String subMissionPlace = sfHeader.getValue("SUBMISSION_PLACE", 0);
		        String reMark = sfHeader.getValue("REMARK", 0);
		        
				title = sfHeader.getValue("SUBJECT", 0);
				orgApprDocNo = sfHeader.getValue("SMR_NUMBER", 0);

				InputStream is = FileUtils.openInputStream(new File(template));
				formEditorData = IOUtils.toString(is);
				formEditorData = formEditorData.replace("#EXPLANATION#", sfHeader.getValue("EXPLANATION", 0));
				formEditorData = formEditorData.replace("#RE_FLAG#", sfHeader.getValue("RE_FLAG", 0));
				formEditorData = formEditorData.replace("#REQ_SEAL_USE_TYPE#",sfHeader.getValue("REQ_SEAL_USE_TYPE", 0));
				formEditorData = formEditorData.replace("#SUBMISSION_PLACE#", sfHeader.getValue("SUBMISSION_PLACE", 0));
				formEditorData = formEditorData.replace("#REMARK#", sfHeader.getValue("REMARK", 0));

				IOUtils.closeQuietly(is);

				sxp = new SepoaXmlParser(this, "doSelectSSIPD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfTarget = new SepoaFormater(ssm.doSelect(inData));

				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < sfTarget.getRowCount(); i++) {
					sb.append("<tr class='sepoa-tr'>");
			          sb.append("<td class='sepoa-td'>" + sfTarget.getValue("SEAL_TYPE", i) + "</td>");
			          sb.append("<td class='sepoa-td'>" + reqSealUseType + "</td>");
			          sb.append("<td class='sepoa-td'>" + explanation + "</td>");
			          sb.append("<td class='sepoa-td'>" + subMissionPlace + "</td>");
			          String link = "<td class='sepoa-td'><a href='" + systemDomain + sfTarget.getValue("ACCESS_LINK", i) + "' target='_blank'>" + sfTarget.getValue("SUBJECT", i) + "</a></td>";
			          sb.append(link);
			          sb.append("<td class='sepoa-td'>" + reMark + "</td>");
			          sb.append("</tr>");
				}
				formEditorData = formEditorData.replace("#SEAL_DOCUMENTS#", sb.toString());

				sxp = new SepoaXmlParser(this, "doSelectSSRAD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfTarget = new SepoaFormater(ssm.doSelect(inData));

				sb = new StringBuffer();
				for (int i = 0; i < sfTarget.getRowCount(); i++) {
					sb.append("<tr class='sepoa-tr'>");
			          String link = "";
			          if ("Y".equalsIgnoreCase(sfTarget.getValue("REAL_DOC_FLAG", i)))
			            link = "&nbsp;&nbsp;<td class='sepoa-td'>" + (i + 1) + ".&nbsp;" + sfTarget.getValue("SUBJECT", i) + "</td>";
			          else {
			            link = "&nbsp;&nbsp;<td class='sepoa-td'>" + (i + 1) + ".&nbsp;" + "<a href='" + systemDomain + sfTarget.getValue("ACCESS_LINK", i) + "' target='_blank'>" + sfTarget.getValue("SUBJECT", i) + "</a></td>";
			          }
			          sb.append(link);

			          sb.append("</tr><br/>");
				}
				formEditorData = formEditorData.replace("#SEAL_CERT#", sb.toString());
			} else if (FORM_ID_EXTERNAL.equals(formId)) {
				String template = config.getString("approval.external.template.file");

				sxp = new SepoaXmlParser(this, "doSelectSSETL");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfHeader = new SepoaFormater(ssm.doSelect(inData));

				title = sfHeader.getValue("SUBJECT", 0);
				InputStream is = FileUtils.openInputStream(new File(template));
				formEditorData = IOUtils.toString(is);
				formEditorData = formEditorData.replace("#SUBJECT#", sfHeader.getValue("SUBJECT", 0));
				formEditorData = formEditorData.replace("#SUBMISSION_PLACE#", sfHeader.getValue("SUBMISSION_PLACE", 0));
				formEditorData = formEditorData.replace("#REMARK#", sfHeader.getValue("REMARK", 0));
				formEditorData = formEditorData.replace("#EXT_OUT_EXPECT_DATE#", sfHeader.getValue("EXT_OUT_EXPECT_DATE", 0));
				formEditorData = formEditorData.replace("#EXT_OUT_EXPECT_TIME#", sfHeader.getValue("EXT_OUT_EXPECT_TIME", 0));
				formEditorData = formEditorData.replace("#EXT_IN_EXPECT_DATE#", sfHeader.getValue("EXT_IN_EXPECT_DATE", 0));
				formEditorData = formEditorData.replace("#EXT_IN_EXPECT_TIME#", sfHeader.getValue("EXT_IN_EXPECT_TIME", 0));
				formEditorData = formEditorData.replace("#REQ_SEAL_USE_TYPE#", sfHeader.getValue("REQ_SEAL_USE_TYPE", 0));

				IOUtils.closeQuietly(is);

				sxp = new SepoaXmlParser(this, "doSelectSSEDC");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfTarget = new SepoaFormater(ssm.doSelect(inData));

				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < sfTarget.getRowCount(); i++) {
					sb.append("<tr>");
					String link ="<td class='sepoa-td'>" + (i + 1) + ".&nbsp;" + "<a href='" + systemDomain + sfTarget.getValue("ACCESS_LINK", i) + "' target='_blank'>" + sfTarget.getValue("SUBJECT", i) + "</a></td>";
					sb.append(link);
					sb.append("</tr>");
				}
				
				formEditorData = formEditorData.replace("#MANAGE_NUMBER#", sfTarget.getValue("MANAGE_NUMBER", 0));
				formEditorData = formEditorData.replace("#SEAL_MG_SUBJECT#", sfTarget.getValue("SEAL_MG_SUBJECT", 0));
				formEditorData = formEditorData.replace("#SEAL_DOCS#", sb.toString());

			} else if (FORM_ID_ISSUE.equals(formId)) {
				String template = config.getString("approval.cert.template.file");

				sxp = new SepoaXmlParser(this, "doSelectSSIAD_CERT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfHeader = new SepoaFormater(ssm.doSelect(inData));

				title = sfHeader.getValue("SUBJECT", 0);
				InputStream is = FileUtils.openInputStream(new File(template));
				formEditorData = IOUtils.toString(is);
				formEditorData = formEditorData.replace("#SUBJECT#", sfHeader.getValue("SUBJECT", 0));
				formEditorData = formEditorData.replace("#SUBMISSION_PLACE#", sfHeader.getValue("SUBMISSION_PLACE", 0));
				formEditorData = formEditorData.replace("#EXPLANATION#", sfHeader.getValue("EXPLANATION", 0));

				IOUtils.closeQuietly(is);

				sxp = new SepoaXmlParser(this, "doSelectSSRAD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfTarget = new SepoaFormater(ssm.doSelect(inData));

				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < sfTarget.getRowCount(); i++) {
					sb.append("<tr class='sepoa-tr'>");
			          sb.append("<td class='sepoa-td'>" + sfTarget.getValue("SUBJECT", i) + "</td>");
			          sb.append("<td class='sepoa-td'>" + sfTarget.getValue("REAL_DOC_CHK", i) + "</td>");
			          sb.append("<td class='sepoa-td'>" + sfTarget.getValue("CONTENTS", i) + "</td>");
			          sb.append("<td class='sepoa-td'>" + sfHeader.getValue("SUBMISSION_PLACE", 0) + "</td>");
			          String link = "";
			          if ("R".equalsIgnoreCase(sfTarget.getValue("REAL_DOC_CHK_LOC", i)))
			            link = "<td class='sepoa-td'>" + sfTarget.getValue("SUBJECT", i) + "</td>";
			          else {
			            link = "<td class='sepoa-td'><a href='" + systemDomain + sfTarget.getValue("ACCESS_LINK", i) + "' target='_blank'>" + sfTarget.getValue("SUBJECT", i) + "</a></td>";
			          }
			          sb.append(link);
			          sb.append("</tr>");
				}
				formEditorData = formEditorData.replace("#SEAL_CERT#", sb.toString());
			}

			contentMap.put("viewHtml", formEditorData);
			contentMap.put("title", title);

		} catch (Throwable exception) {
			Rollback();
			// exception.printStackTrace();
			setFlag(false);
			setStatus(-1);
			setMessage(exception.getMessage());
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(this.getClass().getSimpleName()).append(" ERROR : ")
					.append(exception.getMessage()).toString());

		} finally {
			for (File file : fileList) {
				FileUtils.forceDelete(file);
			}

			headerMap.put("resultCode", getStatus());
			headerMap.put("resultMessage", getMessage());
			headerMap.put("isSuccessful", getFlag());

			ObjectMapper objectMapper = new ObjectMapper();
			String jsonString = objectMapper.writeValueAsString(map);
			setValue(jsonString);
		}

		return getSepoaOut();
	}

}
