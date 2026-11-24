package sepoa.svc.approval;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cache.Cache;
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
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.ux.link.SepoaLinkManager;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("통합결재 연계 서비스")
public class SepoaGWApprovalService extends SepoaService {

	// 동아ST HTML 연동결재 (TOBE V.1.3) — formId
	final static String FORM_ID_SEAL				= "ST_SEAL_SE_01";		// 날인신청_사용인감(계약용)
	final static String FORM_ID_SEAL_CORP			= "ST_SEAL_SE_03";		// 날인신청_법인인감(계약용)
	
	final static String FORM_ID_EXTERNAL			= "ST_SEAL_EX_01";		// 반출신청
	final static String FORM_ID_ISSUE				= "ST_SEAL_IS_01";		// 증명서발급
	
	final static String FORM_ID_CONTRACT			= "ST_SEAL_CT_01";		// 전자계약 (추후 사용)
	final static String FORM_ID_CONTRACT_PRESET		= "ST_SEAL_CT_01";		// 전자계약 (추후 사용)
	
	final static String FORM_ID_QNA					= "ST_SEAL_QA_01";		// 법무검토
	
	final static String USE = "USE";			// 사용인감 날인신청
	final static String CORP = "CORP";			// 법인인감 날인신청
	final static String EXTERNAL = "EXTERNAL";	// 반출신청
	final static String CERT = "CERT";			// 증명서 발급
	final static String CONTRACT = "CONTRACT";	// 전자계약
	final static String QNA_D = "QNA_D";		// 법무검토 일반
	final static String QNA_C = "QNA_C";		// 법무검토 계약		
	
	public SepoaGWApprovalService(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("통합결재 연계 서비스")
	public SepoaOut sendApproval(Map<String, Object> allData) throws Exception {

		String titlePrefix = "";
		String empNo = null;
		String formId = null;
		String orgApprDocNo = null;
		String apprTitle = null;
		String formEditorData = null;
		String docTitleCode = null;
		String reqSystemType = null;
		String fileTempDirectory = null;

		setFlag(true);
		setStatus(1);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		List<File> fileList = new ArrayList<File>();
		try {
			Config config = new Configuration();
			String url = config.getString("approval.url");
			String systemDomain = config.get("sepoa.system.domain.name") +"/link.sux?ln=";
			
			empNo = info.getSession("ID").toLowerCase();
			reqSystemType = config.getString("approval.reqSystemType");

			ConnectionContext ctx = getConnectionContext();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Logger.dev.println(" ########### headerData ######### ");
			Logger.dev.println(" == > " + headerData);

			String applyType = headerData.get("APPLY_TYPE");
			String smrNumber = headerData.get("SMR_NUMBER");

			SepoaFormater sfHeader = null;

			switch(applyType) {
				case USE : {
					titlePrefix = "[전자인장_사용인감]";
					formId = FORM_ID_SEAL;
				}
				case CORP : {
					// 날인 신청
					if(formId == null) {
						titlePrefix = "[전자인장_법인인감]";
						formId = FORM_ID_SEAL_CORP;
					}
					// 사용/법인 공통 문서제목 코드 (명세)
					docTitleCode = "TITLE_ST_SEAL_SE_01";
					String template = config.getString("approval.seal.template.file");

					sxp = new SepoaXmlParser(this, "doSelectSSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sfHeader = new SepoaFormater(ssm.doSelect(headerData));
					
					orgApprDocNo = sfHeader.getValue("SMR_NUMBER", 0);
					apprTitle = sfHeader.getValue("SUBJECT", 0);

					InputStream is = FileUtils.openInputStream(new File(template));
					formEditorData = IOUtils.toString(is);
					
					String explanation = sfHeader.getValue("EXPLANATION", 0);
					String reFlag = sfHeader.getValue("RE_FLAG", 0).substring(0, 2);
					String reqSealUseType = sfHeader.getValue("REQ_SEAL_USE_TYPE", 0);
					String subMissionPlace = sfHeader.getValue("SUBMISSION_PLACE", 0);
					String reMark = sfHeader.getValue("REMARK", 0);
					// ST: USE→ST_SEAL_SE_01, CORP→ST_SEAL_SE_03 (용도코드 분기 없음)
					
					IOUtils.closeQuietly(is);

					sxp = new SepoaXmlParser(this, "doSelectSSIPD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					SepoaFormater sfTarget = new SepoaFormater(ssm.doSelect(headerData));

					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < sfTarget.getRowCount(); i++) {
						sb.append("<tr class='sepoa-tr'>");
						sb.append("<td class='sepoa-td'>" + reFlag + "</td>");
						sb.append("<td class='sepoa-td'>" + reqSealUseType + "</td>");
						sb.append("<td class='sepoa-td'>" + explanation + "</td>");
						sb.append("<td class='sepoa-td'>" + subMissionPlace + "</td>");
						String link = "<td class='sepoa-td'><a href='"+ systemDomain + sfTarget.getValue("ACCESS_LINK", i) + "' target='_blank'>" + sfTarget.getValue("SUBJECT", i) + "</a></td>";
						sb.append(link);
						sb.append("<td class='sepoa-td'>" + reMark + "</td>");
						sb.append("</tr>");
					}
					formEditorData = formEditorData.replace("#SEAL_DOCUMENTS#", sb.toString());

					sxp = new SepoaXmlParser(this, "doSelectSSRAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sfTarget = new SepoaFormater(ssm.doSelect(headerData));

					sb = new StringBuffer();
					for (int i = 0; i < sfTarget.getRowCount(); i++) {
						sb.append("<tr class='sepoa-tr'>");
						String link = "";
						if("Y".equalsIgnoreCase(sfTarget.getValue("REAL_DOC_CHK", i))){
							link = "&nbsp;&nbsp;<td class='sepoa-td'>" + (i+1) + ".&nbsp;" + sfTarget.getValue("SUBJECT", i) + "</td>";
						} else {
							link = "&nbsp;&nbsp;<td class='sepoa-td'>" + (i+1) + ".&nbsp;" + "<a href='" + systemDomain + sfTarget.getValue("ACCESS_LINK", i) + "' target='_blank'>" + sfTarget.getValue("SUBJECT", i) + "</a></td>";
						}
						sb.append(link);
						
						sb.append("</tr><br/>");
					}
					formEditorData = formEditorData.replace("#SEAL_CERT#", sb.toString());
					
					break;
				}
				case EXTERNAL : {
					// 반출 신청
					titlePrefix = "[ST_인장]";
					formId = FORM_ID_EXTERNAL;
					docTitleCode = "TITLE_ST_SEAL_EX_01";
					String template = config.getString("approval.external.template.file");

					sxp = new SepoaXmlParser(this, "doSelectSSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sfHeader = new SepoaFormater(ssm.doSelect(headerData));

					orgApprDocNo = smrNumber;
					apprTitle = sfHeader.getValue("SUBJECT", 0);

					InputStream is = FileUtils.openInputStream(new File(template));
					formEditorData = IOUtils.toString(is);
					formEditorData = formEditorData.replace("#REQ_SEAL_USE_TYPE#", sfHeader.getValue("REQ_SEAL_USE_TYPE", 0));
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
					SepoaFormater sfTarget = new SepoaFormater(ssm.doSelect(headerData));

					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < sfTarget.getRowCount(); i++) {
						sb.append(sfTarget.getValue("MANAGE_NUMBER", i));
						sb.append("<br/>");
					}
					formEditorData = formEditorData.replace("#MANAGE_NUMBER#", sb.toString());
					
					sb = new StringBuffer();
					for (int i = 0; i < sfTarget.getRowCount(); i++) {
						sb.append(sfTarget.getValue("SEAL_MG_SUBJECT", i));
						sb.append("<br/>");
					}
					formEditorData = formEditorData.replace("#SEAL_MG_SUBJECT#", sb.toString());
					
					sb = new StringBuffer();
					for (int i = 0; i < sfTarget.getRowCount(); i++) {
						String accessLink = sfTarget.getValue("ACCESS_LINK", i);
						String subject = sfTarget.getValue("SUBJECT", i);
						if (StringUtils.isBlank(accessLink)) {
							Logger.err.println(info.getSession("ID"), this,
									"EXTERNAL SEAL_DOCS ACCESS_LINK is blank. SMR_NUMBER="
											+ headerData.get("SMR_NUMBER") + ", SUBJECT=" + subject
											+ ", row=" + i + ", columns=" + Arrays.toString(sfTarget.getColumnNames()));
						}
						sb.append("<tr class='sepoa-tr'>");
						String link = "<td class='sepoa-td'><a href='" + systemDomain + StringUtils.defaultString(accessLink)
								+ "' target='_blank'>" + subject + "</a></td>";
						sb.append(link);
						sb.append("</tr><br/>");
					}
					formEditorData = formEditorData.replace("#SEAL_DOCS#", sb.toString());
					
					break;
				}
				case CERT : {
					// 증명서 발급
					titlePrefix = "[ST_인장]";
					formId = FORM_ID_ISSUE;
					docTitleCode = "TITLE_ST_SEAL_IS_01";
					String template = config.getString("approval.cert.template.file");
					
					sxp = new SepoaXmlParser(this, "doSelectSSIAD_CERT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sfHeader = new SepoaFormater(ssm.doSelect(headerData));

					orgApprDocNo = smrNumber;
					apprTitle = sfHeader.getValue("SUBJECT", 0);

					InputStream is = FileUtils.openInputStream(new File(template));
					formEditorData = IOUtils.toString(is);
					formEditorData = formEditorData.replace("#SUBJECT#", sfHeader.getValue("SUBJECT", 0));
					formEditorData = formEditorData.replace("#SUBMISSION_PLACE#", sfHeader.getValue("SUBMISSION_PLACE", 0));
					formEditorData = formEditorData.replace("#EXPLANATION#", sfHeader.getValue("EXPLANATION", 0));
					
					IOUtils.closeQuietly(is);

					sxp = new SepoaXmlParser(this, "doSelectSSRAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					SepoaFormater sfTarget = new SepoaFormater(ssm.doSelect(headerData));

					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < sfTarget.getRowCount(); i++) {
						sb.append("<tr class='sepoa-tr'>");
						String link = "";
						if("R".equalsIgnoreCase(sfTarget.getValue("REAL_DOC_CHK_LOC", i))){
							link = "<td class='sepoa-td'>" + sfTarget.getValue("SUBJECT", i) + "</td>";
						} else {
							link = "<td class='sepoa-td'><a href='" + systemDomain + sfTarget.getValue("ACCESS_LINK", i) + "' target='_blank'>" + sfTarget.getValue("SUBJECT", i) + "</a></td>";
						}
						sb.append("<td class='sepoa-td'>" + sfTarget.getValue("SUBJECT", i) + "</td>");
						sb.append("<td class='sepoa-td'>" + sfTarget.getValue("REAL_DOC_CHK", i) + "</td>");
						sb.append("<td class='sepoa-td'>" + extractCertSealUseType(sfTarget.getValue("TEMPLATE_MAPPING_DATA", i)) + "</td>");
						sb.append("<td class='sepoa-td'>" + sfTarget.getValue("CONTENTS", i) + "</td>");
						sb.append("<td class='sepoa-td'>" + sfHeader.getValue("SUBMISSION_PLACE", 0) + "</td>");
						sb.append(link);
						sb.append("</tr>");
					}
					formEditorData = formEditorData.replace("#SEAL_CERT#", sb.toString());

					break;
				}
				case CONTRACT : {
					// 전자계약
					titlePrefix = "[계약]";
					formId = FORM_ID_CONTRACT_PRESET;
					docTitleCode = "TITLE_ST_SEAL_CT_01";
					String template = config.getString("approval.contract.template.file");

					String method = "doDocumentUpdate";
					headerData.put("NEXT_STEP", "");
					
					Map<String, Object> paramData = new HashMap<>();
					paramData.putAll(allData);
					
					SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
					if(so.flag) {
						method = "doDocumentDetail";
						so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
						if(so.flag) {
							List<Object> result = so.getAllData();
							if(result.size() > 0) {
								Map<String, Object> detail = (Map<String, Object>) result.get(0);
								Map<String, String> docInfo = MapUtils.getMap(detail, "DOC_INFO_DATA");
								String contNo = docInfo.get("CONT_NO");
								String contGlSeq = docInfo.get("CONT_GL_SEQ");
								orgApprDocNo = contNo + "_" + contGlSeq;
								apprTitle = docInfo.get("CONT_SUBJECT");

								PathManager pm = new PathManager(info, config);
								pm.setLayer(PathManager.LAYER_TEMP);
								fileTempDirectory = pm.getUniquePath();
								paramData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);

								method = "doImageList";
								so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});

								InputStream is = FileUtils.openInputStream(new File(template));
								formEditorData = IOUtils.toString(is);
								IOUtils.closeQuietly(is);

								Map<String, String> linkParams = new HashMap<String, String>();
								linkParams.put("CONT_NO", contNo);
								linkParams.put("CONT_GL_SEQ", contGlSeq);

								SepoaLinkManager lm = new SepoaLinkManager(info, config, ctx);
								if(!lm.newLink("CONTRACT", contNo, contGlSeq)) {
									Logger.warn.println(info.getSession("ID"), this, "LinkManager Initialize failed > " + lm.getLastErrCode());
								}
								lm.setParams(linkParams);

								linkParams = new HashMap<String, String>();
								linkParams.put("CONT_MODULE", "VIEW");
								String privateLink = lm.addUser(
										docInfo.get("COMPANY_CODE"),
										docInfo.get("CREATOR"),
										linkParams);

								String link = "<a href='" + config.get("sepoa.system.domain.name") + "/link.sux?ln=" + privateLink + "' target='_blank'>" + apprTitle + "</a>";
								formEditorData = formEditorData.replace("#SUBJECT#", link);

								SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
								SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy/MM/dd");
								String contDate = "";
								String contFrom = "";
								String contTo = "";

								if(!StringUtils.isBlank(docInfo.get("CONT_DATE"))) {
									contDate = toDateFormat.format(fromDateFormat.parse(docInfo.get("CONT_DATE")));
								}

								if(!StringUtils.isBlank(docInfo.get("CONT_FROM"))) {
									contFrom = toDateFormat.format(fromDateFormat.parse(docInfo.get("CONT_FROM")));
								}

								if(!StringUtils.isBlank(docInfo.get("CONT_TO"))) {
									contTo = toDateFormat.format(fromDateFormat.parse(docInfo.get("CONT_TO")));
								}

								formEditorData = formEditorData.replace("#CONT_DATE#", contDate);
								formEditorData = formEditorData.replace("#CONT_FROM#", contFrom);
								formEditorData = formEditorData.replace("#CONT_TO#", contTo);

								String fileRow = "<tr class='sepoa-tr'><td class='sepoa-td'>#FILE_TYPE#</td><td class='sepoa-td'>#FILE_NAME_CLIENT#</td></tr>";
								List<Map<String, String>> fileTypeCode = new Gson().fromJson(Cache.combo.get("SL0001,000#KO#CT007"), List.class);
								List<Map<String, String>> documentList = (List<Map<String, String>>) so.getData(0);
								List<String> fileRowHtmlList = new ArrayList<>(documentList.size());
								for(Map<String, String> doc : documentList) {
									String fileType = "";
									Optional<Map<String, String>> codeValue = fileTypeCode.stream().filter(e -> e.get("value").equals(doc.get("LAYER").replace("CONT_", ""))).findFirst();
									if(codeValue.isPresent()) {
										fileType = codeValue.get().get("text");
									}

									fileRowHtmlList.add(fileRow
										.replace("#FILE_TYPE#", fileType)
										.replace("#FILE_NAME_CLIENT#", doc.get("FILE_NAME_CLIENT"))
									);
								}
								formEditorData = formEditorData.replace("#FILE_INFO#", StringUtils.join(fileRowHtmlList, System.lineSeparator()));

								String signerRow = "<tr class='sepoa-tr'><td class='sepoa-td'>#SIGNER_COMPANY_NAME#</td><td class='sepoa-td'>#SIGNER_NAME#</td><td class='sepoa-td'>#SIGNER_EMAIL#</td><td class='sepoa-td'>#SIGNER_PHONE#</td></tr>";
								List<Map<String, String>> signerList = (List<Map<String, String>>) MapUtils.getObject(detail, "SIGNER", null);
								List<String> signerRowHtmlList = new ArrayList<>(signerList.size());
								for(Map<String, String> signer : signerList) {
									signerRowHtmlList.add(signerRow
										.replace("#SIGNER_COMPANY_NAME#", signer.get("SIGNER_COMPANY_NAME"))
										.replace("#SIGNER_NAME#", signer.get("SIGNER_NAME"))
										.replace("#SIGNER_EMAIL#", signer.get("SIGNER_EMAIL"))
										.replace("#SIGNER_PHONE#", signer.get("SIGNER_PHONE"))
									);
								}
								formEditorData = formEditorData.replace("#SIGNER_INFO#", StringUtils.join(signerRowHtmlList, System.lineSeparator()));
							}
							
						}
						
						Commit();
					} else {
						throw new Exception("Contract Document Update Failed.");
					}

					break;
				}
				case QNA_D :
				case QNA_C :
					titlePrefix = ((applyType.equals(QNA_C)) ? "[ST_법무검토_일반]" : "[ST_법무검토_계약]");
					String category = ((applyType.equals(QNA_C)) ? "일반" : "계약");
					formId = FORM_ID_QNA;
					docTitleCode = "TITLE_ST_SEAL_QA_01";
					
					//QNA 템플릿 파일 적재
					String templateFilePath = config.getString("approval.qna.template.file", "");
					File templateFile = new File(templateFilePath);
					if(!templateFile.exists()) {
						throw new Exception("QNA Template File dose not Exist!");
					} 
					formEditorData = FileUtils.readFileToString(templateFile, "UTF-8");
					
					//QNA 헤더 조회
					sxp = new SepoaXmlParser(this, "doSelectSQNAQ");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sfHeader = new SepoaFormater(ssm.doSelect(headerData));
					
					orgApprDocNo = sfHeader.getListMap().get(0).get("QNA_NUMBER");
					apprTitle  = sfHeader.getListMap().get(0).get("SUBJECT");
					
					if(sfHeader.getRowCount() == 0) {
						throw new Exception("QNA Data is Null!");
					}
					
					Map<String, String> qnaHeader = new HashMap<String, String>();
					qnaHeader.putAll(sfHeader.getListMap().get(0));
					
					String content = qnaHeader.get("CONTENT");
					String attachList = "";
					String docValue = MapUtils.getString(qnaHeader, "ATTACH_NO", "");
					//첨부파일 조회 구간
					
					
					if(!"".equals(docValue)) {
						headerData.put("DOC_VALUE", docValue);
						SepoaFormater sf = null;
						sxp = new SepoaXmlParser(this, "doSearchFile");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						sf = new SepoaFormater(ssm.doSelect(headerData));
						if(sf.getRowCount() > 0) {
							List<String> tmpHtmlStrList = new ArrayList<>();
							for(Map<String, String> attachFile : sf.getListMap()) {
								String docNo = attachFile.get("DOC_NO");
								String docSeq = attachFile.get("DOC_SEQ");
								String tmpStr = "<a href='" + config.get("sepoa.system.domain.name") + "/CustomDownload.sux?attach_key=" + docNo + "&attach_seq=" + docSeq + "' target='_blank'> - "+attachFile.get("SRC_FILE_NAME")+"</a>";
								tmpHtmlStrList.add(tmpStr);
							}
							attachList = String.join("<br/>", tmpHtmlStrList);
						} else {
							attachList = " - 첨부된 파일이 없습니다.";
						}
					} else {
						attachList = " - 첨부된 파일이 없습니다.";
					}
					
					formEditorData = formEditorData.replace("#CATEGORY#", category);
					formEditorData = formEditorData.replace("#SUBJECT#", qnaHeader.get("SUBJECT"));
					formEditorData = formEditorData.replace("#KEYWORD#", qnaHeader.get("KEYWORDS"));
					formEditorData = formEditorData.replace("#CONTENT#", content);
					formEditorData = formEditorData.replace("#ATTACH_LIST#", attachList);
					
					break;
				default :
					break;
			}
			
			setValue(url);
			Map<String, String> resultMap = new HashMap<String, String>();
			resultMap.put("empNo", empNo);
			resultMap.put("formId", formId);
			resultMap.put("orgApprDocNo", orgApprDocNo);
			resultMap.put("apprTitle", titlePrefix + " " + apprTitle);
			resultMap.put("formEditorData", StringEscapeUtils.escapeHtml(formEditorData));
			resultMap.put("docTitleCode", docTitleCode);
			resultMap.put("reqSystemType", reqSystemType);
			
			Logger.info.println("## Output formId ## ==>");
			Logger.info.println("FORM_ID =" + formId);
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
			if(fileTempDirectory != null) {
				FileUtils.forceDelete(new File(fileTempDirectory));
			}
			
			if(fileList != null && fileList.size() > 0) {
				for (File file : fileList) {
					FileUtils.forceDelete(file);
				}
			}
		}

		return getSepoaOut();
	}

	@Description("결재콜백")
	public SepoaOut doApproval(Map<String, String> dataMap) {

		final String RESULT_STATUS_SUBMITTED = "A"; // 상신
		final String RESULT_STATUS_PROCEED = ""; // 진행
		final String RESULT_STATUS_APPROVED = "C"; // 결재완료
		final String RESULT_STATUS_CANCEL = "AC"; // 상신취소
		final String RESULT_STATUS_REJECTED = "R"; // 반려

		final String SSID_SMR_STATUS_PROCEED = "I";
		final String SSID_SMR_STATUS_TEMPORARY = "T";
		final String SSID_SMR_STATUS_REJECTED = "R";
		final String SSID_SMR_STATUS_COMPLETED = "CR";
		final String SSID_SMR_STATUS_EXT_COMPLETE = "RW"; // 반입대기

		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Logger.info.println("DataMap recieved from GW : " + dataMap);
			
			String resultStatus = dataMap.get("status");
			String formId = dataMap.get("formId");
			String orgApprDocNo = dataMap.get("orgApprDocNo");
			dataMap.put("SMR_NUMBER", orgApprDocNo);

			ConnectionContext ctx = getConnectionContext();

			if (FORM_ID_EXTERNAL.equals(formId)) {

			} else if (FORM_ID_SEAL.equals(formId) || FORM_ID_SEAL_CORP.equals(formId)) {
				sxp = new SepoaXmlParser(this, "doCountSSIAD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfCount = new SepoaFormater(ssm.doSelect(dataMap));
				if ("0".equals(sfCount.getValue("CNT", 0))) {
					throw new Exception("Data does not exist.");
				}
			}

			if(Arrays.asList(new String[]{FORM_ID_CONTRACT, FORM_ID_CONTRACT_PRESET}).contains(formId)) {
				Map<String, Object> paramData = new HashMap<>();
				Map<String, String> header = new HashMap<>();
				paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, header);
				
				String[] docNumber = orgApprDocNo.split("_");
				String contNo = docNumber[0];
				String contGlSeq = docNumber[1];
				header.put("CONT_NO", contNo);
				header.put("CONT_GL_SEQ", contGlSeq);
				
				SepoaOut so = null;
				switch(resultStatus) {
					case RESULT_STATUS_SUBMITTED : {	// 상신
						String method = "doApprovalProgress";
						so = ServiceConnector.doService(info, "sepoa.svc.ct.CT_205", ctx, method, new Object[] {ctx, paramData});
						break;
					}
					case RESULT_STATUS_PROCEED : {		// 진행
						break;
					}
					case RESULT_STATUS_APPROVED : {		// 결재완료
						String method = "doApprovalConfirm";
						so = ServiceConnector.doService(info, "sepoa.svc.ct.CT_205", ctx, method, new Object[] {ctx, paramData});
						break;
					}
					case RESULT_STATUS_CANCEL : {		// 상신취소(회수)
						String method = "doApprovalWithdraw";
						so = ServiceConnector.doService(info, "sepoa.svc.ct.CT_205", ctx, method, new Object[] {ctx, paramData});
						break;
					}
					case RESULT_STATUS_REJECTED : {		// 반려
						String method = "doApprovalReject";
						so = ServiceConnector.doService(info, "sepoa.svc.ct.CT_205", ctx, method, new Object[] {ctx, paramData});
						break;
					}
					default :
						break;
				}
				
				if(so.flag == false) {
					setFlag(so.flag);
					Logger.err.println("Approval Callback Failed. : " + dataMap);
					Logger.err.println(so.message);
				}
			} else {
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
					} else if (FORM_ID_SEAL.equals(formId) || FORM_ID_SEAL_CORP.equals(formId) || FORM_ID_ISSUE.equals(formId)) {

						sxp = new SepoaXmlParser(this, "updateSSIAD");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(dataMap);
					} else if(FORM_ID_QNA.equals(formId)) {
						dataMap.put("STATUS", "I");
						dataMap.put("QNA_NUMBER", orgApprDocNo);
						sxp = new SepoaXmlParser(this, "doUpdateSQNAQ");
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
					} else if (FORM_ID_SEAL.equals(formId) || FORM_ID_SEAL_CORP.equals(formId) || FORM_ID_ISSUE.equals(formId)) {
						dataMap.put("SMR_STATUS", SSID_SMR_STATUS_REJECTED);

						sxp = new SepoaXmlParser(this, "updateSSIAD");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(dataMap);
					} else if(FORM_ID_QNA.equals(formId)) {
						dataMap.put("STATUS", "R");
						dataMap.put("QNA_NUMBER", orgApprDocNo);
						sxp = new SepoaXmlParser(this, "doUpdateSQNAQ");
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

					} else if (FORM_ID_SEAL.equals(formId) || FORM_ID_SEAL_CORP.equals(formId) || FORM_ID_ISSUE.equals(formId)) {
						dataMap.put("SMR_STATUS", SSID_SMR_STATUS_COMPLETED);
						sxp = new SepoaXmlParser(this, "updateSSIAD");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(dataMap);

						sxp = new SepoaXmlParser(this, "doQuerySSIAD");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						sf = new SepoaFormater(ssm.doSelect(dataMap));
						dataMap.putAll(sf.getListMap().get(0));
						
						//SPMGT 테이블 INSERT
						String action = "";
						if(FORM_ID_SEAL.equals(formId) || FORM_ID_SEAL_CORP.equals(formId)) {
							action = "SEAL_COMPLETE";
						} else if(FORM_ID_ISSUE.equals(formId)) {
							action = "CERT_COMPLETE";
						}
                        Map<String, String> pmgtMap = new HashMap<String, String>();
                        pmgtMap.put("ACTION", action);
                        pmgtMap.put("IP", CommonUtil.getConfig("sepoa.dapharm.host.ip", "172.16.4.126"));
                        
                        sxp = new SepoaXmlParser(this, "doInsertSPMGT");
                        ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                        ssm.setSepoaInfo(info);
                        ssm.doUpdate(dataMap, pmgtMap);
                        
                        sxp = new SepoaXmlParser(this, "doAddSAPPP");
                        ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                        ssm.doInsert(dataMap);
					} else if(FORM_ID_QNA.equals(formId)) {
						dataMap.put("STATUS", "F");
						dataMap.put("QNA_NUMBER", orgApprDocNo);
						sxp = new SepoaXmlParser(this, "doUpdateSQNAQ");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(dataMap);
						
						// ST: 결재승인 → 검토대기(F) 전환 시 법무팀 메일 (실패해도 결재상태는 유지)
						try {
							Map<String, Object> mailParam = new HashMap<String, Object>();
							Map<String, String> mailHeader = new HashMap<String, String>();
							mailHeader.put("QNA_NUMBER", orgApprDocNo);
							mailParam.put(SepoaDataMapper.KEY_HEADER_DATA, mailHeader);
							mailParam.put(SepoaDataMapper.KEY_RAW_HEADER_DATA, mailHeader);
							SepoaOut mailOut = ServiceConnector.doService(info, "sepoa.svc.board.SepoaQAService", ctx, "doSendReviewWaitMail", new Object[] { mailParam });
							if (mailOut == null || mailOut.status != 1 || !mailOut.flag) {
								Logger.err.println(info.getSession("ID"), this, "QnA review-wait mail failed. QNA_NUMBER=" + orgApprDocNo
										+ (mailOut != null ? (", msg=" + mailOut.message) : ""));
							}
						} catch (Exception mailEx) {
							Logger.err.println(info.getSession("ID"), this, "QnA review-wait mail exception. QNA_NUMBER=" + orgApprDocNo);
							Logger.err.println(info.getSession("ID"), this, mailEx);
						}
					}

				} else if (RESULT_STATUS_CANCEL.equals(resultStatus)) {
					// 결재 취소
					dataMap.put("ACCEPT_DATE", SepoaDate.getShortDateString());
					dataMap.put("ACCEPT_TIME", SepoaDate.getShortTimeString());

					if (FORM_ID_EXTERNAL.equals(formId)) {
						dataMap.put("SMR_STATUS", SSID_SMR_STATUS_TEMPORARY);
						sxp = new SepoaXmlParser(this, "updateSSETL");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(dataMap);

					} else if (FORM_ID_SEAL.equals(formId) || FORM_ID_SEAL_CORP.equals(formId) || FORM_ID_ISSUE.equals(formId)) {
						dataMap.put("SMR_STATUS", SSID_SMR_STATUS_TEMPORARY);
						sxp = new SepoaXmlParser(this, "updateSSIAD");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(dataMap);

					} else if(FORM_ID_QNA.equals(formId)) {
						dataMap.put("STATUS", "T");
						dataMap.put("QNA_NUMBER", orgApprDocNo);
						sxp = new SepoaXmlParser(this, "doUpdateSQNAQ");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(dataMap);
					}
				}
			}

			Commit();

		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	/**
	 * 제증명서 인감계(TEMPLATE_MAPPING_DATA)에서 선택한 날인용도명(REQ_USE)을 추출한다.
	 */
	@SuppressWarnings("unchecked")
	private String extractCertSealUseType(String templateMappingData) {
		if (StringUtils.isBlank(templateMappingData)) {
			return "";
		}
		try {
			String json = StringEscapeUtils.unescapeHtml(templateMappingData);
			Map<String, Object> mappingData = new Gson().fromJson(json, Map.class);
			if (mappingData == null) {
				return "";
			}
			Object reqUse = mappingData.get("REQ_USE");
			return reqUse == null ? "" : StringUtils.defaultString(String.valueOf(reqUse));
		} catch (Exception e) {
			Logger.err.println(info.getSession("ID"), this,
					"Failed to parse TEMPLATE_MAPPING_DATA for seal use type: " + e.getMessage());
			return "";
		}
	}

}
