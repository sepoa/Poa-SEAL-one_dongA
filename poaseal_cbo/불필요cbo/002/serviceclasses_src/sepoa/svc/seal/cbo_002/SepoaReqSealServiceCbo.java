package sepoa.svc.seal.cbo_002;

import java.sql.CallableStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.cfg.Configuration;
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
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.webservice.logger.WSLogger;
import sepoa.fw.webservice.logger.WSLoggerManager;
import sepoa.seal.SepoaReqSealManager;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자인장 - 날인요청 모듈 CBO")

public class SepoaReqSealServiceCbo extends SepoaService{ 
	
	public SepoaReqSealServiceCbo(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("그룹웨어 날인신청")
	@SuppressWarnings("unchecked")
	public SepoaOut sendAppData(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		WSLogger logger = null;
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		Map<String, String> ifMap = new HashMap<String, String>();
		
		try {
			String companyCode = info.getSession("COMPANY_CODE");
			String smrNumber = headerData.get("SMR_NUMBER");
			String dtApply = SepoaDate.getShortDateString();
			String applyFor = headerData.get("APPLY_FOR");	//US 사용인감 CS 법인인감 XT 인감불출
			
			String gwDb = CommonUtil.getConfig("dlcon.gw.database.name");
			String sql = "{CALL "+gwDb+".dbo.DASPR_DLSEAL_INSERT(?,?,?,?,?,?,?,?)}";
			CallableStatement cs = ctx.getConnection().prepareCall(sql);
			
			cs.setString(1, companyCode);	// 회사코드
			cs.setString(2, smrNumber); 	// 날인신청번호
			cs.setString(3, dtApply);		// 신청일자(PK)
			cs.setString(4, applyFor);  	// US 사용인감 CS 법인인감 XT 인감불출
			cs.registerOutParameter(5,java.sql.Types.INTEGER);	// 신청차수(PK)
			cs.registerOutParameter(6,java.sql.Types.VARCHAR);	// 신청자 부서
			cs.registerOutParameter(7,java.sql.Types.VARCHAR);	// 에러코드
			cs.registerOutParameter(8,java.sql.Types.VARCHAR);	// 에러메시지
			
			// 인터페이스 로그
			ifMap.put("IN_COMPANY_CD", companyCode);
			ifMap.put("IN_LMS_NO", smrNumber);
			ifMap.put("IN_USERID", dtApply);
			ifMap.put("IN_STATUS", applyFor);
			
			//ResultSet rs = null;
			int resultInt = -1;
			Boolean callErr = false;
			try {
				resultInt = cs.executeUpdate();  // 쿼리 실행
			} catch(Exception e1) {
				callErr = true;
				Logger.err.println(info.getSession("ID"), this, e1);
				e1.printStackTrace();
			}
			
			String noChaSu = cs.getString(5);
			String cdOrg2apply = cs.getString(6);
			String resultCode = cs.getString(7);
			String resultMsg = cs.getString(8);
			
			ifMap.put("OUT_NO_CHASU", noChaSu);
			ifMap.put("OUT_CD_ORG2APPLY", cdOrg2apply);
			ifMap.put("OUT_RESULT_CODE", resultCode);
			ifMap.put("OUT_RESULT_MSG", resultMsg);
			ifMap.put("SQL_ERROR", callErr+"");
			
			Logger.err.println(info.getSession("ID"), this, "callErr:"+callErr+" / resultInt:"+resultInt+"/noChaSu:"+noChaSu+" / cdOrg2apply:"+cdOrg2apply+" / resultCode:"+resultCode+" / resultMsg:"+resultMsg);
			if (!"OK".equals(resultCode) || callErr) throw new Exception(resultMsg);
			
			Map<String, String> returnMap = new HashMap<String, String>();
			returnMap.put("DT_APPLY", dtApply);
			returnMap.put("NO_CHASU", noChaSu);
			returnMap.put("CD_ORG2APPLY", cdOrg2apply);
			setValue(new SepoaFormater(returnMap));
			
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		} finally {
			if (logger != null) {
				try {
					String ifCode = DocumentUtil.getDocNumberRemote(info, "INTERFACE").result[0];
					logger = WSLoggerManager.getLogger(info);
					logger.setFuncionName("GW_APP");
					logger.setTransactionCode(ifCode);
					logger.setData("GW_APP", ifMap);
					logger.write();
				} catch (Exception e1) {
					Logger.err.println(info.getSession("ID"), this, e1);
				}
			}
		}
        
		return getSepoaOut();
	}
	
	@Description("그룹웨어 결재상태변경 배치")
	@SuppressWarnings("unchecked")
	public SepoaOut GroupwareApprovalEnd() {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		List<Map<String, String>> list = null;
		SepoaOut so = null;
		
		try {
			sxp = new SepoaXmlParser(this, "getGroupwareApprovalEnd");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
			if (sf.getRowCount() > 0) {
				list = sf.getListMap();
			
				for (Map<String, String> map : list) {
					String smrNumber = "";
					
					try {
						Map<String, Object> allMap = new HashMap<String, Object>();
						Map<String, String> headerMap = new HashMap<String, String>();
						allMap.put(SepoaModuleDataMapper.KEY_HEADER_DATA, headerMap);
						Object[] paramObj = { ctx, allMap };
						headerMap.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
						headerMap.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
						headerMap.put("ACCEPT_REASON", "");	// 결재사유
						smrNumber = MapUtils.getString(map, "SMR_NUMBER");
						headerMap.put("SUX_WKEY1", smrNumber);
						String confirmUserId = null;	// 승인자
						
						String applyFor = MapUtils.getString(map, "APPLY_FOR");
						String dlAppStatus = MapUtils.getString(map, "APP_STATUS");	// C 상신, A 결재완료, R 반려
						String method;
						if ("A".equals(dlAppStatus)) {
							method = "doApprovalConfirm";
							headerMap.put("APP_STATUS", "F");
							confirmUserId = MapUtils.getString(map, "CONFIRM_USER_ID", null);	// 승인자
						} else if ("R".equals(dlAppStatus)) {
							method = "doApprovalReject";
							headerMap.put("APP_STATUS", "R");
							confirmUserId = MapUtils.getString(map, "CONFIRM_USER_ID", null);	// 승인자
						} else if ("C".equals(dlAppStatus)) {
							method = "doApprovalProgress";
							headerMap.put("APP_STATUS", "I");
						} else {
							throw new Exception("잘못된 상태값입니다.");
						}
						
						
						if ("XT".equals(applyFor)) {	// 반출
							if (confirmUserId != null) headerMap.put("SIGN_USER_ID_FORCED", confirmUserId);
							if("mk2".equals(CommonUtil.getConfig("sepoa.seal.process"))){
								so = ServiceConnector.doService(info, "sepoa.svc.sealmk2.SepoaReqExternalService", "TRANSACTION", method, paramObj);
							}else {
								so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaReqExternalService", "TRANSACTION", method, paramObj);
							}
						} else {	// 날인신청
							if (confirmUserId != null) headerMap.put("CONFIRM_USER_ID_FORCED", confirmUserId);
							if("mk2".equals(CommonUtil.getConfig("sepoa.seal.process"))){
								so = ServiceConnector.doService(info, "sepoa.svc.sealmk2.SepoaReqSealService", "TRANSACTION", method, paramObj);
							}else {
								so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaReqSealService", "TRANSACTION", method, paramObj);
							}
						}
						String postprocessed = null;
						if (so.flag) {
							postprocessed = "Y";
			            } else {
			            	Rollback();
			            	
			            	SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddhhmmss");
			    			Date addDateTime = fmt.parse(map.get("ADD_DATE")+map.get("ADD_TIME"));
			    			Date now = fmt.parse(SepoaDate.getShortDateString()+SepoaDate.getShortTimeString());
			    			long diffMin = (now.getTime() - addDateTime.getTime()) / 60000;	// 분
			    			if (diffMin > 10) {	// 시간이 지났는데도 계속 처리가 안됐다면 오류처리
			    				postprocessed = "E";
			    			}
			            }
						
						if (postprocessed != null) {
							// DLCON_IF_APP 상태값 변경 (POSTPROCESSED = 'Y')
							headerMap.put("POSTPROCESSED", postprocessed);							
							headerMap.put("APP_SEQ", MapUtils.getString(map, "SEQ"));
							sxp = new SepoaXmlParser(this, "updateStatusDLCON_IF_APP");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.doUpdate(headerMap);
							
							Commit();
						}
						
						if (!so.flag) {
							throw new Exception(so.message);
						}
					} catch (Exception e) {
						String log = "["+smrNumber+"] [" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())+ "] ERROR-GroupwareApprovalEnd : " + e;
						System.out.println(log);
						Logger.err.println(log);
						Rollback();
						continue;
					}
				}
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
        
		return getSepoaOut();
	}
	
	@Description("결재문서 접속링크")
	@SuppressWarnings("unchecked")
	public SepoaOut getSealDocLinkCode(Map<String, Object> allData) throws Exception {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		try {
			String smrNo = headerData.get("SMR_NUMBER");
			String smdNo = headerData.get("SMD_NUMBER");
			SepoaReqSealManager srm = new SepoaReqSealManager(info, new Configuration(), ctx);
			
			Map<String, String> linkParams = new HashMap<String, String>();
			linkParams.put("SMR_NUMBER", smrNo);
			linkParams.put("TARGET", "REQ_SEAL");
			linkParams.put("SMR_STATUS", "I");
			linkParams.put("SMD_NUMBER", smdNo);
			
			String linkType = null;
			if("C".equals(headerData.get("APPLY_TYPE"))){
				linkType = "CORP.REQ.SEAL.DOC";
			} else {
				linkType = "USE.REQ.SEAL.DOC";
			}
			
			Map<String, String> checkMap = new HashMap<String, String>();
			checkMap.put("LINK_TYPE", linkType);
			checkMap.put("SMD_NUMBER", smdNo);
			SepoaXmlParser sxp = new SepoaXmlParser(this, "checkLinkExists");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(checkMap));
			
			String accessLinkCode = null;
			
			if (sf.getRowCount() == 0) {
				accessLinkCode = srm.doCreateReqSealDocAccessLink(linkType, smdNo, linkParams, null, "ALL");
				if(accessLinkCode == null || accessLinkCode.isEmpty()) {
					int errCode = srm.getLastErrCode();
					throw new Exception("[ERROR "+errCode+"]");
				}
			} else {
				accessLinkCode = sf.getValue("LINK_CODE", 0);
			}
			
			//System.out.println(accessLinkCode);
			//System.out.println((CommonUtil.getConfig("sepoa.system.domain.name")+"/link.sux?ln="+accessLinkCode).replaceAll("//", "/"));
			
			setValue(smdNo+":"+accessLinkCode);
			
			Commit();
		} catch(Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setValue("");
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("그룹웨어 결재여부 체크")
	@SuppressWarnings("unchecked")
	public SepoaOut checkGW_applied(Map<String, Object> allData) throws Exception {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		SepoaFormater sf = null;
		
		try {
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			int rc = sf.getRowCount();
			if (rc > 0) {
				setValue("Y");				
			} else {
				setValue("N");
			}
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setValue("");
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("그룹웨어 결재문서 키 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut getNoDoc2(Map<String, Object> allData) throws Exception {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		SepoaFormater sf = null;
		
		try {
			SepoaXmlParser sxp = new SepoaXmlParser();
			sxp.addVar("GW_DATABASE", CommonUtil.getConfig("dlcon.gw.database.name"));
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() == 0) {
				setValue("");
			} else {
				String noDoc2 = sf.getValue("NO_DOC2", 0);
				setValue(noDoc2);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setValue("");
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("어디서 작성했는지 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut checkOrigin(Map<String, Object> allData) throws Exception {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		SepoaFormater sf = null;
		
		try {
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() == 0) {
				setValue("");
			} else {
				String erpCode = sf.getValue("ERP_CODE", 0);
				setValue(erpCode);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setValue("");
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	
	@Description("임시저장 인터페이스로그 남기기")
	@SuppressWarnings("unchecked")
	public SepoaOut setTempSaveIfLog(Map<String, Object> allData) throws Exception {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this, "updateDLCON_IF_APP_old");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(headerData);
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(headerData);
			
			Commit();
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			Rollback();
			setFlag(false);
			setValue("");
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("기본 QR링크 세팅")
	@SuppressWarnings("unchecked")
	public SepoaOut setDefaultQR(Map<String, Object> allData) throws Exception {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		
		try {
			String itemKey = Long.toString(new Date().getTime());
			String leftPercent = CommonUtil.getConfig("sepoa.req.seal.qr.default.left");
			String topPercent = CommonUtil.getConfig("sepoa.req.seal.qr.default.top");
			String domain = CommonUtil.getConfig("sepoa.system.domain.name");
			String contextName = CommonUtil.getConfig("sepoa.system.context.name", "");
			if (!"".equals(contextName)) domain = domain + contextName;
			
			// fixHtml 의 클래스에 autosize를 추가하면 용지에 맞춰 자동으로 크기조정
			String fixHtml = "{\"001\":\"\\u003cdiv class\\u003d\\\"contract-item contract-item-box contract-item-box-no-CI22022200001 contract-item-box-code-QR_LINK contract-item-box-seal-data contract-item-box-seal-code-CI22022200001 contract-item-box-itemkey-#ITEM_KEY# contract-item-box-source-org contract-item-type-seal contract-item-unit-save-ready autosize\\\" style\\u003d\\\"left:#LEFT_PERCENT#%; top:#TOP_PERCENT#%; width: 9.47%; height: 6.7%; z-index: 1;\\\"\\u003e\\u003ctextarea class\\u003d\\\"contract-item contract-item-input contract-item-prop-seal_template-STD contract-item-prop-repeat-OFF contract-item-prop-cut_position-LEFT contract-item-prop-cut_percent-0 contract-item-prop-symmetry-OFF contract-item-prop-symmetry_cut_position-LEFT contract-item-prop-symmetry_cut_percent-0 contract-item-prop-symmetry_page_copy-OFF contract-item-prop-seal_width-2 contract-item-prop-seal_height-2\\\" readonly\\u003d\\\"\\\" wrap\\u003d\\\"off\\\" placeholder\\u003d\\\"QR링크\\\" style\\u003d\\\"-webkit-transform: translateX(0%);transform: translateX(0%);\\\"\\u003e\\u003c/textarea\\u003e\\u003cdiv class\\u003d\\\"contract-item contract-item-input-origin\\\" style\\u003d\\\"display:none;\\\"\\u003e\\u003c/div\\u003e\\u003cdiv class\\u003d\\\"contract-item contract-item-input-suffix\\\" style\\u003d\\\"display:none;\\\"\\u003e\\u003c/div\\u003e\\u003cdiv class\\u003d\\\"contract-item contract-item-input-prefix\\\" style\\u003d\\\"display:none;\\\"\\u003e\\u003c/div\\u003e\\u003cdiv class\\u003d\\\"contract-item contract-item-cover\\\" style\\u003d\\\"background-image: url(\\u0026quot;#DOMAIN#/SepoaActionController.sux?module\\u003dseal.management\\u0026amp;action\\u003ddoSelectSealPreviewImage\\u0026amp;id\\u003dCI22022200001\\u0026quot;);\\\"\\u003e\\u003c/div\\u003e\\u003cdiv class\\u003d\\\"contract-item-se-resize\\\" style\\u003d\\\"display: none;\\\"\\u003e\\u003c/div\\u003e\\u003cdiv class\\u003d\\\"contract-item-delete\\\"\\u003e\\u003c/div\\u003e\\u003c/div\\u003e\"}";
			fixHtml = fixHtml.replace("#ITEM_KEY#", itemKey).replace("#LEFT_PERCENT#", leftPercent).replace("#TOP_PERCENT#", topPercent).replace("#DOMAIN#", domain);
			headerData.put("FIX_HTML", fixHtml);
			headerData.put("PAGE", "1");
			
			SepoaReqSealManager srm = new SepoaReqSealManager(info, new Configuration(), ctx);
			srm.setDocBaiscInfo(headerData);
			srm.doAppendSealFixHtml(headerData);

			Commit();
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			Rollback();
			setFlag(false);
			setValue("");
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	
	@Description("인감담당자 사번 가져오기")
	@SuppressWarnings("unchecked")
	public SepoaOut getSealManagers(Map<String, Object> allData) throws Exception {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		SepoaFormater sf = null;
		
		try {
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			String sealMgUserIds = "";
			int rc = sf.getRowCount();
			for (int i = 0; i < rc; i++) {
				String sealMgUserId = sf.getValue("SEAL_MG_USER_ID", i);
				if (i != 0) sealMgUserIds = sealMgUserIds+",";
				sealMgUserIds = sealMgUserIds + sealMgUserId;
			}
			setValue(sealMgUserIds);				
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setValue("");
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("20220809 이후 QR링크코드 가져오기")
	@SuppressWarnings("unchecked")
	public SepoaOut getNewQRLink(Map<String, Object> allData) throws Exception {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		SepoaFormater sf = null;
		
		try {
			SepoaXmlParser sxp = new SepoaXmlParser();
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() == 0) throw new Exception("링크를 찾을 수 없습니다");
			String privateCode = sf.getValue("PRIVATE_CODE", 0);
			
			setValue(privateCode);				
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setValue("");
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
}

