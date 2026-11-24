package sepoa.fw.approval;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.google.gson.Gson;

import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.approval.exception.SepoaApprovalLineSettingException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

public class SepoaApprovalLineSetting extends SepoaApproval {

    /**
     * 현재 마지막 결재 Stage
     */
    private int maxAppStage = -1;
    
    /**
     * 현재 마지막 결재 Seq
     */
    private int maxAppLineSeq = -1;
    
    public SepoaApprovalLineSetting(SepoaInfo info) throws SepoaApprovalException {
        super(info);
    }

    public SepoaApprovalLineSetting(ConnectionContext ctx, SepoaInfo info) throws SepoaApprovalException {
        super(ctx, info);
    }

    /**
     * 
     * 세포아 솔루션 내부결재 시스템일 경우
     * 
     * 결재라인 지정시 무조건 임시저장 상태로 저장.
     * 
     * 결재진행중 상태로 변경 및 결재완료상태로 변경시에는 별도의 프로세스를 타도록 한다.
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalException 
     */
    @SuppressWarnings("unchecked")
    @Override
    public void doSolution(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalLineSettingException {
        String appStage = "0";
        String deleteAppLine = "0";
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"");
			Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);
            List<Map<String, String>> appLine = (List<Map<String, String>>) parseMap.get("APP_LINE");
            Map<String, String> appInfo = (Map<String, String>) parseMap.get("APP_INFO");
            if(appLine != null){
                getApprovalNumber(info, ctx, allData);
            	//평화산업 인한 체크로직 추가 검인라인만 결재라인 변경하는 경우
            	// 검인결재라인 Stage 시작 구하기
            	int managementStartStageAndSeq = 1;

                String firstLineAppType = "";
            	if(appLine.size() > 1) {
                    Map<String, String> appLineMap = appLine.get(1);
                    firstLineAppType = MapUtils.getString(appLineMap, "APP_TYPE", SepoaApproval.APP_TYPE_OPERATION);
            	}
                
                
                //결재라인이 검인라인이 존재할 경우.
                if(SepoaApproval.APP_TYPE_MANAGEMENT.equals(firstLineAppType)) {
                    int tempStage = 0;
                    int maxStage = 0;
                	// 현재 검인라인 데이타 가져오기
                    for (int i = 1; i < appLine.size(); i++) {
						Map<String, String> appLineMap = appLine.get(i);
						tempStage = managementStartStageAndSeq + Integer.parseInt(MapUtils.getString(appLineMap, "APP_STAGE", "0"));
						if(maxStage < tempStage) {
							maxStage = tempStage;
						}
                        appLineMap.put("APP_NO", MapUtils.getString(appInfo, "APP_NO", ""));
                        appLineMap.put("APP_NO_SEQ", MapUtils.getString(appInfo, "APP_NO_SEQ", ""));
                        appLineMap.put("APP_TYPE", SepoaApproval.APP_TYPE_MANAGEMENT);
                        appLineMap.put("APP_METHOD", SepoaApproval.APP_METHOD_APPROVAL);
						
                        // 결재가 승인 또는 완료일 경우 기존결재ID와 비교하여 틀리다면, 변경불가하도록 처리
						String appLineStatus = "";
						String appUserId = "";
						if (i < sf.getRowCount()) {
							appLineStatus = sf.getValue("APP_LINE_STATUS", i);
							appUserId = sf.getValue("APP_USER_ID", i);
						}else {
							appLineStatus = "";
							appUserId = "";
						}
						
						if ((SepoaApproval.APP_LINE_STATUS_CONFIRM.equals(appLineStatus)
								|| SepoaApproval.APP_LINE_STATUS_COMPLETED.equals(appLineStatus))
								&& !MapUtils.getString(appLineMap, "APP_USER_ID", "")
										.equals(appUserId)) {
                            throw new SepoaApprovalLineSettingException("LINE_MSG_01");
						}else if ((SepoaApproval.APP_LINE_STATUS_CONFIRM.equals(sf.getValue(appLineStatus))
								|| SepoaApproval.APP_LINE_STATUS_COMPLETED.equals(sf.getValue(appLineStatus)))
								&& MapUtils.getString(appLineMap, "APP_USER_ID", "")
										.equals(appUserId)) {
							//Pass
						} else {
							appLineMap.put("APP_LINE_SEQ", String.valueOf(tempStage));
							appLineMap.put("APP_STAGE", String.valueOf(tempStage));
							
							sxp = new SepoaXmlParser(this.getClass(), "doDetailDuplicateCheck");
	                        ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                        sf = new SepoaFormater(ssm.doSelect(appLineMap));
	                        if (sf != null && sf.getRowCount() > 0 && !"0".equals(sf.getValue(0, 0))) {
	                            //라인이 존재한다면 UPDATE
	                            sxp = new SepoaXmlParser(this.getClass(), "doUpdateDetail");
	                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                            ssm.doUpdate(appLineMap);

	                        } else {
	                            //라인이 존재하지 않으면 INSERT
	                            sxp = new SepoaXmlParser(this.getClass(), "doInsertDetail");
	                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                            ssm.doInsert(appLineMap);
	                        }
						}

                        // 마지막까지 Insert한 후 마지막에 저장된 Stage보다 큰라인은 삭제처리한다.
                        if (appLine.size() - 1 == i) {
                            appLineMap.put("DELETE_APP_LINE_SEQ", String.valueOf(maxStage));
                            appLineMap.put("APP_TYPE", SepoaApproval.APP_TYPE_MANAGEMENT);
                            sxp = new SepoaXmlParser(this.getClass(), "doManagementDeleteDetail");
                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                            ssm.doDelete(appLineMap);
                        }
                    }
                }else {

                    for (int i = 0; i < appLine.size(); i++) {
                        Map<String, String> appLineMap = appLine.get(i);
                        
                        // 결재라인의 마지막 Stage를 구한다.
                        int tempAppStage = Integer.parseInt(MapUtils.getString(appLineMap, "APP_STAGE","0")); 
                        if(maxAppStage < tempAppStage){
                            maxAppStage = tempAppStage;
                        }
                        
                        // 결재라인의 마지막 Line Seq를 구한다.
                        int tempAppLineSeq = Integer.parseInt(MapUtils.getString(appLineMap, "APP_LINE_SEQ","0")); 
                        if(maxAppLineSeq < tempAppLineSeq){
                            maxAppLineSeq = tempAppLineSeq;
                        }
                        
                        if("0".equals(MapUtils.getString(appLineMap, "APP_LINE_SEQ",""))){
                            appLineMap.put("ACCEPT_REASON", MapUtils.getString(appInfo, "APP_COMMENT", ""));
                        }

                        appLineMap.put("APP_NO", MapUtils.getString(headerData, "APP_NO", ""));
                        appLineMap.put("APP_NO_SEQ", MapUtils.getString(headerData, "APP_NO_SEQ", ""));

                        // 현재 결재라인 Stage를 가져와서 어디까지 결재가 이루어졌는지 확인.
                        if ("0".equals(appStage)) {
                            sxp = new SepoaXmlParser(this.getClass(), "doNowStage");
                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                            sf = new SepoaFormater(ssm.doSelect(headerData));
                            if (sf != null && sf.getRowCount() > 0) {
                                appStage = sf.getValue(0, 0);
                            }
                        }

                        if (Integer.parseInt(MapUtils.getString(appLineMap, "APP_STAGE", "0")) < Integer.parseInt(appStage)) {
                            // 입력하려는 결재라인 Stage가 현재까지 결재된 Stage보다 적을 경우, 해당 결재라인의 아이디와 비교하여,
                            // 틀릴경우 이미 결재된 사람은 변경이 불가능하도록 한다. ( 결재중에도 결재라인이 변경가능해야 하므로 들어가는 로직 )    
                            sxp = new SepoaXmlParser(this.getClass(), "doStageSignUserId");
                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                            sf = new SepoaFormater(ssm.doSelect(headerData, appLineMap));
                            if (sf != null && sf.getRowCount() > 0) {
                                if (!sf.getValue(0, 0).equals(MapUtils.getString(appLineMap, "APP_USER_ID", ""))) {
                                    throw new SepoaApprovalLineSettingException("LINE_MSG_01");
                                }
                            }
                        } 
                        // 결재타입이 비어있다면 현업결재로 고정.
                        if ("".equals(MapUtils.getString(appLineMap, "APP_TYPE", ""))) {
                            appLineMap.put("APP_TYPE", SepoaApproval.APP_TYPE_OPERATION);
                        }

                        sxp = new SepoaXmlParser(this.getClass(), "doDetailDuplicateCheck");
                        ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                        sf = new SepoaFormater(ssm.doSelect(headerData, appLineMap));
                        if (sf != null && sf.getRowCount() > 0 && !"0".equals(sf.getValue(0, 0))) {
                            //라인이 존재한다면 UPDATE
                            sxp = new SepoaXmlParser(this.getClass(), "doUpdateDetail");
                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                            ssm.doUpdate(headerData, appLineMap);

                        } else {
                            //라인이 존재하지 않으면 INSERT
                            sxp = new SepoaXmlParser(this.getClass(), "doInsertDetail");
                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                            ssm.doInsert(headerData, appLineMap);
                        }
                        if(Integer.parseInt(deleteAppLine) < Integer.parseInt(MapUtils.getString(appLineMap, "APP_LINE_SEQ"))){
                            deleteAppLine = MapUtils.getString(appLineMap, "APP_LINE_SEQ");
                        }
                        
                        // 마지막까지 Insert한 후 마지막에 저장된 Stage보다 큰라인은 삭제처리한다.
                        if (appLine.size() - 1 == i) {
                            appLineMap.put("DELETE_APP_LINE_SEQ", deleteAppLine);
                            appLineMap.put("APP_TYPE", SepoaApproval.APP_TYPE_MANAGEMENT);
                            sxp = new SepoaXmlParser(this.getClass(), "doDeleteDetail");
                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                            ssm.doDelete(headerData, appLineMap);
                            //검인결재라인 추가
                            setApprovalManagementLine(info, ctx, allData);
                        }
                    }

                    // 결재Header 데이타를 Insert 하자.
                    sxp = new SepoaXmlParser(this.getClass(), "doHeaderDuplicateCheck");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    sf = new SepoaFormater(ssm.doSelect(headerData, appInfo));
                    
                    Map<String, Object> reqSealInfo = (Map<String, Object>) parseMap.get("REQ_SEAL_INFO");
                    appInfo.put("APP_SUBJECT", (String)reqSealInfo.get("SUBJECT"));
                    if (sf != null && sf.getRowCount() > 0 && !"0".equals(sf.getValue(0, 0))) {
                        //라인이 존재한다면 UPDATE
                        sxp = new SepoaXmlParser(this.getClass(), "doUpdateHeader");
                        ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                        ssm.doUpdate(headerData, appInfo);

                    } else {
                        // 결재상태는 INSERT할 때는 무조건 임시저장상태이다.
                        appInfo.put("APP_STATUS", SepoaApproval.APP_STATUS_TEMP);
                        
                        //라인이 존재하지 않으면 INSERT
                        sxp = new SepoaXmlParser(this.getClass(), "doInsertHeader");
                        ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                        ssm.doInsert(headerData, appInfo);
                    }
                }
                
            }
        } catch (Exception e) {
            throw new SepoaApprovalLineSettingException(e);
        }
    }

    /**
     * 
     * 그룹웨어 결재라인
     * 
     * 결재라인 상태값을 모두 그룹웨어에서 받아와야한다.
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalLineSettingException 
     */
    @Override
    public void doGroupware(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalLineSettingException {
        // 그룹웨어는.......
    }

    /**
     * 
     * 결재를 진행하지 않는 건에 대해서는 임의 1인 결재완료 상태로 만든다.
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalLineSettingException 
     */
    @Override
    public void doNotApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalLineSettingException {
        setApprovalGarbageData(info, ctx, allData);
    }

    /**
     * 
     * 결재요청상태로 만들고 업무단에서 승인을 받는 경우.
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalLineSettingException 
     */
    @Override
    public void doDirectApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalLineSettingException {
        setApprovalGarbageData(info, ctx, allData);
    }
    
    /**
     * Approval 번호 채번
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalLineSettingException 
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getApprovalNumber(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalLineSettingException {
        String appNo = "";
        String appNoSeq = "";
        Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
        Map<String, Object> parseMap = null;
        Map<String, String> appInfo = null;
        Map<String, String> useMap = null; 
        
        if (headerData.get("PARAMETER") != null) {
        	parseMap = new Gson().fromJson(headerData.get("PARAMETER").replaceAll("&#34;", "\""), Map.class);
        	appInfo = (Map<String, String>) parseMap.get("APP_INFO");
        	if (appInfo != null) {
        		useMap = appInfo;
        	} else {
        		useMap = headerData;
        	}
        } else {
        	useMap = headerData;
        }
        
        try {
            // 첫번째 Loop에서만 확인해야 할 사항들 체크.
            // 결재 채번 번호가 있는지 판단.
            if ("".equals(MapUtils.getString(useMap, "APP_NO", ""))) {
                // 결재번호가 없을 경우 채번룰에 의해 채번을 한다.
                appNo = DocumentUtil.getDocNumberRemote(info, "APPROVAL").result[0];
                // 결재번호가 없을 경우 당연히 APP_NO_SEQ는 1이다.
                appNoSeq = "1";
            } else {
                // 결재번호가 존재한다면 해당 번호를 가져온다.
                appNo = MapUtils.getString(useMap, "APP_NO", null);

                // appSeq를 가져오기전에 반려된 건인지 확인
                sxp = new SepoaXmlParser(this.getClass(), "doApprovalStatusCheck");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                sf = new SepoaFormater(ssm.doSelect(useMap));

                // 반려된건이라면 Count가 0보다 크다.
                if (sf != null && sf.getRowCount() > 0 && Integer.parseInt(sf.getValue(0, 0)) > 0) {
                    // 반려된 건은  APP_SEQ의 MAX값을 가져와서 1을 증가시켜주자.
                    sxp = new SepoaXmlParser(this.getClass(), "doApprovalMaxSeq");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    sf = new SepoaFormater(ssm.doSelect(useMap));
                    if (sf != null && sf.getRowCount() > 0) {
                        appNoSeq = String.valueOf(Integer.parseInt(sf.getValue(0, 0)) + 1);
                    }
                } else {
                    // 그냥 수정된 건이라면 현재 APP_SEQ를 그대로 사용한다.
                    appNoSeq = MapUtils.getString(useMap, "APP_NO_SEQ", "");
                }
            }
            headerData.put("APP_NO", appNo);
            headerData.put("APP_NO_SEQ", appNoSeq);

        } catch (Exception e) {
            throw new SepoaApprovalLineSettingException(e);
        }

        return headerData;
    }

    /**
     * Approval 가비지 데이타 생성.
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalLineSettingException 
     */
    @SuppressWarnings("unchecked")
    private void setApprovalGarbageData(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalLineSettingException {
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            

            // 현재 업무 번호로 가비지 데이타가 존재하는지 확인한다.
            if(!"".equals(MapUtils.getString(headerData, "SUX_WKEY1"))){
                sxp = new SepoaXmlParser(this.getClass(), "doGarbageDataCheck");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                sf = new SepoaFormater(ssm.doSelect(headerData));

                if (sf != null && sf.getRowCount() > 0) {
                    // 현재 업무번호로 Approval 가비지 데이타가 존재할 경우 삭제처리 한다.
                    headerData.put("APP_NO", sf.getValue("APP_NO",0));
                    headerData.put("APP_NO_SEQ", sf.getValue("APP_NO_SEQ",0));
                    
                    sxp = new SepoaXmlParser(this.getClass(), "doGarbageDataDeleteHeader");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doDelete(headerData);

                    sxp = new SepoaXmlParser(this.getClass(), "doGarbageDataDeleteDetail");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doDelete(headerData);
                }
            }
            if ("".equals(MapUtils.getString(headerData, "APP_NO", ""))) {
                // Approval번호 생성 후 headerData에 담는다.
                getApprovalNumber(info, ctx, allData);
            }

            // 결재상태는 INSERT할 때는 무조건 임시저장상태이다.
            Map<String, String> tempMap = new HashMap<String, String>();
            tempMap.put("APP_STATUS", SepoaApproval.APP_STATUS_TEMP);
            sxp = new SepoaXmlParser(this.getClass(), "doInsertHeader");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doInsert(headerData,tempMap);

            // 가비지 데이타에는 APP_USER_ID를 넣지 않는다. 나중에 조회 방지용.
            headerData.put("APP_LINE_SEQ", "1");
            headerData.put("APP_STAGE", "1");
            headerData.put("APP_METHOD", SepoaApproval.APP_METHOD_REPORT);
            sxp = new SepoaXmlParser(this.getClass(), "doInsertDetail");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doInsert(headerData);

            headerData.put("APP_LINE_SEQ", "2");
            headerData.put("APP_STAGE", "2");
            headerData.put("APP_METHOD", SepoaApproval.APP_METHOD_APPROVAL);
            sxp = new SepoaXmlParser(this.getClass(), "doInsertDetail");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doInsert(headerData);

        } catch (Exception e) {
            throw new SepoaApprovalLineSettingException(e);
        }

    }
    

    /**
     * Approval 관리결재라인 셋팅.
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalLineSettingException 
     */
    @SuppressWarnings("unchecked")
    private void setApprovalManagementLine(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalLineSettingException {
        SepoaFormater defaultSf = null;
        SepoaFormater moduleDepartmentSf = null;
        SepoaFormater accountCodeSf = null;
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            if("".equals(MapUtils.getString(headerData, "APP_USER_AMT", ""))){
               headerData.put("APP_USER_AMT", "0"); 
            }
            BigDecimal appUserAmt = new BigDecimal(MapUtils.getString(headerData, "APP_USER_AMT", ""));
            /**
             * 관리 결재선 LINE_TYPE 기본값, 부서별, 계정별, MODULE
             */
            Map<String, String> commonMap = new HashMap<String, String>();
            commonMap.put("APP_NO", MapUtils.getString(headerData, "APP_NO", ""));
            commonMap.put("APP_NO_SEQ", MapUtils.getString(headerData, "APP_NO_SEQ", ""));
            commonMap.put("APP_TYPE", SepoaApproval.APP_TYPE_MANAGEMENT);
            commonMap.put("APP_METHOD", SepoaApproval.APP_METHOD_APPROVAL);
            commonMap.put("SEPOA_APPROVAL_S_COMPANY_CODE", MapUtils.getString(headerData, "SEPOA_APPROVAL_S_COMPANY_CODE", ""));

            try {
                sxp = new SepoaXmlParser(this.getClass(), "setApprovalManagementLineUseFlag");
                headerData.put("MANAGEMENT_LINE_TYPE", "TEST");
                headerData.put("MANAGEMENT_APP_TYPE", "TEST");
                headerData.put("MANAGEMENT_CODE", "TEST");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            	ssm.doSelect(headerData);
            	Logger.debug.println(info.getSession("ID"), "컬럼확인을 위한 쿼리 에러나도 되는 쿼리임.");
            }catch(Exception e) {
                sxp = new SepoaXmlParser(this.getClass(), "setApprovalManagementLine");
            }
            
            List<Map<String, String>> managementLineArray = new ArrayList<Map<String,String>>();
            while(true){
                headerData.put("MANAGEMENT_LINE_TYPE", SepoaApproval.LINE_TYPE_ACCOUNT);
                headerData.put("MANAGEMENT_APP_TYPE", MapUtils.getString(headerData, "APP_TYPE"));
                headerData.put("MANAGEMENT_CODE", MapUtils.getString(headerData, SepoaApproval.LINE_TYPE_ACCOUNT_CODE, "NO_DATA"));
                if("".equals(MapUtils.getString(headerData, "MANAGEMENT_CODE",""))){
                    headerData.put("MANAGEMENT_CODE", "NO_DATA");
                }
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                accountCodeSf = new SepoaFormater(ssm.doSelect(headerData));
                int accountSeq = 0;
                if(accountCodeSf != null && accountCodeSf.getRowCount() > 0){
                    for (int i = 0; i < accountCodeSf.getRowCount(); i++) {

                        Logger.debug.println(info.getSession("ID"), "USE_FLAG : " + accountCodeSf.getValue("USE_FLAG",i));
                    	if("Y".equals(accountCodeSf.getValue("USE_FLAG",i))) {
                            BigDecimal managementAppUserAmt = new BigDecimal(accountCodeSf.getValue("APP_USER_AMT", i));
                            Logger.debug.println(info.getSession("ID"), "appUserAmt.compareTo(managementAppUserAmt) : " + appUserAmt.compareTo(managementAppUserAmt) + "/" + appUserAmt + "/" + managementAppUserAmt);
                            if(appUserAmt.compareTo(managementAppUserAmt) != -1 && !accountCodeSf.getValue("APP_USER_ID", i).equals(info.getSession("ID"))){
                            	Map<String, String> managementLineMap = new HashMap<String, String>();
                                managementLineMap.put("APP_LINE_SEQ", String.valueOf(maxAppLineSeq + accountSeq + 1));
                                managementLineMap.put("APP_STAGE", String.valueOf(maxAppStage + accountSeq + 1));
                                managementLineMap.put("APP_USER_ID", accountCodeSf.getValue("APP_USER_ID", i));
                                managementLineArray.add(managementLineMap);
                                accountSeq++;
                            }
                    	}
                    }   
                    break;
                }

                headerData.put("MANAGEMENT_LINE_TYPE", SepoaApproval.LINE_TYPE_MODULE_DEPT);
                headerData.put("MANAGEMENT_APP_TYPE", MapUtils.getString(headerData, "APP_TYPE"));
                headerData.put("MANAGEMENT_CODE", MapUtils.getString(headerData, "APP_DEPT_CODE", info.getSession("DEPARTMENT")));
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                moduleDepartmentSf = new SepoaFormater(ssm.doSelect(headerData));
                int moduleDeptSeq = 0;
                if(moduleDepartmentSf != null && moduleDepartmentSf.getRowCount() > 0){
                    for (int i = 0; i < moduleDepartmentSf.getRowCount(); i++) {
                        Logger.debug.println(info.getSession("ID"), "USE_FLAG : " + moduleDepartmentSf.getValue("USE_FLAG",i));
                    	if("Y".equals(moduleDepartmentSf.getValue("USE_FLAG",i))) {
                            BigDecimal managementAppUserAmt = new BigDecimal(moduleDepartmentSf.getValue("APP_USER_AMT", i));
                            Logger.debug.println(info.getSession("ID"), "appUserAmt.compareTo(managementAppUserAmt) : " + appUserAmt.compareTo(managementAppUserAmt) + "/" + appUserAmt + "/" + managementAppUserAmt);
                            if(appUserAmt.compareTo(managementAppUserAmt) != -1 && !moduleDepartmentSf.getValue("APP_USER_ID", i).equals(info.getSession("ID"))){
                            	Map<String, String> managementLineMap = new HashMap<String, String>();
                                managementLineMap.put("APP_LINE_SEQ", String.valueOf(maxAppLineSeq + moduleDeptSeq + 1));
                                managementLineMap.put("APP_STAGE", String.valueOf(maxAppStage + moduleDeptSeq + 1));
                                managementLineMap.put("APP_USER_ID", moduleDepartmentSf.getValue("APP_USER_ID", i));
                                managementLineArray.add(managementLineMap);
                                moduleDeptSeq++;
                            }
                    	}
                    }   
                    break;
                }
                
                headerData.put("MANAGEMENT_LINE_TYPE", SepoaApproval.LINE_TYPE_DEFAULT);
                headerData.put("MANAGEMENT_APP_TYPE", MapUtils.getString(headerData, "APP_TYPE"));
                headerData.put("MANAGEMENT_CODE", "ALL");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                defaultSf = new SepoaFormater(ssm.doSelect(headerData));
                int defaultDeptSeq = 0;
                if(defaultSf != null && defaultSf.getRowCount() > 0){
                    for (int i = 0; i < defaultSf.getRowCount(); i++) {
                        Logger.debug.println(info.getSession("ID"), "USE_FLAG : " + defaultSf.getValue("USE_FLAG",i));
                    	if("Y".equals(defaultSf.getValue("USE_FLAG",i))) {
                            BigDecimal managementAppUserAmt = new BigDecimal(defaultSf.getValue("APP_USER_AMT", i));
                            Logger.debug.println(info.getSession("ID"), "appUserAmt.compareTo(managementAppUserAmt) : " + appUserAmt.compareTo(managementAppUserAmt) + "/" + appUserAmt + "/" + managementAppUserAmt);
                            if(appUserAmt.compareTo(managementAppUserAmt) != -1 && !defaultSf.getValue("APP_USER_ID", i).equals(info.getSession("ID"))){
                            	Map<String, String> managementLineMap = new HashMap<String, String>();
                                managementLineMap.put("APP_LINE_SEQ", String.valueOf(maxAppLineSeq + defaultDeptSeq + 1));
                                managementLineMap.put("APP_STAGE", String.valueOf(maxAppStage + defaultDeptSeq + 1));
                                managementLineMap.put("APP_USER_ID", defaultSf.getValue("APP_USER_ID", i));
                                managementLineArray.add(managementLineMap);
                                defaultDeptSeq++;
                            }
                    	}
                    }   
                    break;
                }
                break;
            }
            if(managementLineArray.size() > 0){
                sxp = new SepoaXmlParser(this.getClass(), "doInsertDetail");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doInsert(managementLineArray, commonMap);   
            }
        } catch (Exception e) {
            throw new SepoaApprovalLineSettingException(e);
        }

    }
}
