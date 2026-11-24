package sepoa.svc.batch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.authority.Permission.PrincipalAgentType;
import sepoa.fw.util.authority.SepoaAuthorityManager;
import sepoa.fw.util.authority.SepoaAuthorityObject;
import sepoa.fw.util.authority.SepoaAuthorityObject.Authorized;

@Description("[남부발전]문서권한 배치 서비스")
public class SepoaAuthBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaAuthBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaAuthBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("문서권한 배치 서비스")
	public SepoaOut executeWeb(Map<String, Object> allData) throws SepoaServiceException {
		return execute();
	}

	// dbcp 초기화(멀티커넥트 지원 sepoa.properties에 기술되어 있어야함)
	public static void initialize() {
		DBCPSetup.initDBCP();
	}

	// 실행(타켓 취득 및 비즈니스로직 수행)
	public SepoaOut execute() throws SepoaServiceException {
		setFlag(true);
		setStatus(1);
		ConnectionContext ctx = null;
		try {
			ctx = getConnectionContext();
			
			List<Map<String, String>> dataList = getTargetRecord(ctx);

			if (dataList != null && dataList.size() != 0) {
				setTargetRecord(ctx, dataList);
			} else {
				throw new Exception("SepoaAuthBatch Empty!");
			}
			
			Commit();
		} catch (Exception e) {
			Rollback();
			Logger.err.println("SepoaAuthBatch", this, e);
			setFlag(false);
			setStatus(0);
			Logger.info.println(info.getSession("ID"), this, "Error is occured.",  e);
			System.out.println((new StringBuilder()).append(this.getClass().getSimpleName()).append(" ERROR : ").append( e.getMessage()).toString());
		} finally {
			try {
				ctx.getConnection().close();
			} catch (Exception e) {
				Logger.err.println(info.getSession("ID"), this, e);
			}
		}
		return getSepoaOut();
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (isRunning) {
			System.out.println("The authBatch process is already running.");
			Logger.info.println("The authBatch process is already running.");
			return; // 현재 처리중
		}
		isRunning = true;
		String house_code = "012";
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

		try {
			sepoainfo.setThreadLocal();

			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : SepoaAuthBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaAuthBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaAuthBatch";
			System.out.println(endLog);
			Logger.info.println(endLog);
		} catch (Exception e) {
			Logger.err.println(e);
		} finally {
			isRunning = false;
			sepoainfo.unsetThreadLocal();
		}
	}

	@Override
	public List<Map<String, String>> getTargetRecord(ConnectionContext var1) {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		ConnectionContext ctx = var1;
	
		List<Map<String, String>> authList = new ArrayList<Map<String, String>>();
	
		try {
			sxp = new SepoaXmlParser(this, "getTargetData");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<String, String>()));
	
			if (sf.getRowCount() > 0) {
				authList = sf.getListMap();
			}
		} catch (Exception e) {
			Logger.info.print("Get TargetRecord Fail!!");
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return authList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		//고정 권한 총 객체 적제
		List<Map<String, String>> fixAuthUser = new ArrayList<Map<String, String>>();
		
		//관리자 권한 사람 조회
		sxp = new SepoaXmlParser(this,"doQueryWorkTypeZ");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		ssm.setLogEnable(false);
		try {
			sf = new SepoaFormater(ssm.doSelect(new HashMap<String, String>()));
			if(sf.getRowCount() > 0) {
				fixAuthUser.addAll(sf.getListMap());
			}
		} catch (Exception e) {
			Logger.err.println("work type z user error");
		}
		
		//계약 관리자 권한 사람 조회
		/*Cache.clearAll();
		final String superAuthUserDefineStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+"#"+info.getSession("LANGUAGE")+"#CT_305");
		final List<Map<String, String>> superAuthUserDefine = new Gson().fromJson(superAuthUserDefineStr, List.class);
		for(Map<String, String> superAuthUser : superAuthUserDefine) {
			sxp = new SepoaXmlParser(this,"doCheckExistUser");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			try {
				sf = new SepoaFormater(ssm.doSelect(superAuthUser));
				if(sf.getRowCount() > 0) {
					fixAuthUser.add(sf.getListMap().get(0));
				}
			} catch (Exception e) {
				Logger.err.println("superAuthUserDefineStr user error");
			}			
		}*/
		//법무검토관리자 조회
		Cache.clearAll();
		sxp = new SepoaXmlParser(this,"doSelectSBALN");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		ssm.setLogEnable(false);
		try {
			sf = new SepoaFormater(ssm.doSelect(new HashMap<String, String>()));
			if(sf.getRowCount() > 0) {
				 fixAuthUser.addAll(sf.getListMap()); 
			}
		} catch (Exception e) {
			Logger.err.println("doSelectSBALN user error");
		}		
		
		//SDCAM 권한 레코드별 루프
		for(Map<String, String> authData : var2) {
			try {
				//권한객체 초기화
				SepoaAuthorityObject authObject = SepoaAuthorityManager.getManager(info, ctx).getObject(
						info.getSession("HOUSE_CODE"), 
						info.getSession("COMPANY_CODE"), 
						"CONT", 
						authData.get("CONT_NO"), 
						authData.get("CONT_GL_SEQ"));
				
				//변동 권한자 적재객체
				List<Map<String, String>> varAuthUser = new ArrayList<>();
				
				//작성자 권한 조회
				String creatorUserId = null;
				String creatorCompanyCode = null;
				sxp = new SepoaXmlParser(this,"doSelectContCreator");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				try {
					sf = new SepoaFormater(ssm.doSelect(authData));
					if(sf.getRowCount() > 0) {
						String contType = sf.getValue("CONT_TYPE", 0);
						if("E".equalsIgnoreCase(contType)) {
							continue;
						}
						
						Map<String, String> creatorInfo = sf.getListMap().get(0);
						creatorUserId = creatorInfo.get("USER_ID");
						creatorCompanyCode = creatorInfo.get("COMPANY_CODE");
						
						varAuthUser.addAll(sf.getListMap());
					}
				} catch (Exception e) {
					Logger.err.println("cont creator user error");
				}
				
				//작성자의 부서 기준으로 팀장 권한 부여
				if(creatorUserId != null && creatorCompanyCode != null) {
					Map<String, String> teamManagerParam = new HashMap<String, String>();
					teamManagerParam.put("CREATOR_USER_ID", creatorUserId);
					teamManagerParam.put("CREATOR_COMPANY_CODE", creatorCompanyCode);
					
					sxp = new SepoaXmlParser(this, "doSelectTeamManagersByCreatorDept");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					try {
						sf = new SepoaFormater(ssm.doSelect(teamManagerParam));
						if(sf.getRowCount() > 0) {
							varAuthUser.addAll(sf.getListMap());
						}
					} catch (Exception e) {
						Logger.err.println("team manager by creator dept error");
					}
				}
				
				//변동 권한자 객체에 고정권한자 추가
				varAuthUser.addAll(fixAuthUser);
				
				//서명 참여자 권한 조회
				sxp = new SepoaXmlParser(this,"doSelectContSigner");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				try {
					sf = new SepoaFormater(ssm.doSelect(authData));
					if(sf.getRowCount() > 0) {
						varAuthUser.addAll(sf.getListMap());
					}
				} catch (Exception e) {
					Logger.err.println("cont signer user error");
				}
				
				//검토 참여자 권한 조회
				sxp = new SepoaXmlParser(this,"doSelectContReviewer");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				try {
					sf = new SepoaFormater(ssm.doSelect(authData));
					if(sf.getRowCount() > 0) {
						varAuthUser.addAll(sf.getListMap());
					}
				} catch (Exception e) {
					Logger.err.println("cont reviewer user error");
				}
				
				//기존 참여자 권한 조회
				List<Map<String, String>> currentAuthUser = new ArrayList<>();
				sxp = new SepoaXmlParser(this,"doSelectCurrentAuthUser");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				try {
					sf = new SepoaFormater(ssm.doSelect(authData));
					if(sf.getRowCount() > 0) {
						currentAuthUser.addAll(sf.getListMap());
					}
				} catch (Exception e) {
					Logger.err.println("cont current auth user error");
				}

				Logger.info.println("#### 변동 권한자 목록 ####");
				Logger.info.println(varAuthUser);
				Logger.info.println("#### 기존 권한자 목록 ####");
				Logger.info.println(currentAuthUser);
				
				//변동 권한자랑 기존 권한자 비교
				if(currentAuthUser.size() > 0) {
					for(Map<String, String> varAuth : varAuthUser) {
						String varUserId = varAuth.get("USER_ID");
						String varComanyCode = varAuth.get("COMPANY_CODE");
						
						//변동 권한자 객체에 A가 있고 기존 권한자에 A가 있으면 break;
						boolean containsFlag = false;
						for(Map<String, String> currentAuth : currentAuthUser) {
							String fixUserId = currentAuth.get("USER_ID");
							if(varUserId.equalsIgnoreCase(fixUserId)) {
								containsFlag = true;
								break;
							}
						}
						
						//변동 권한자 객체에 A가 있고 기존 권한자에 A가 없으면 추가
						if(!containsFlag) {
							authObject.grant(PrincipalAgentType.PERSONAL, varComanyCode, varUserId, Authorized.READ, null);
						}
					}
					
					//기존권한자 A가 변동권한자에 존재 하지않는 경우 삭제
					for(Map<String, String> currentAuth : currentAuthUser) {
						String currentUserId = currentAuth.get("USER_ID");
						String currentCompanyCode = currentAuth.get("COMPANY_CODE");
						
						boolean containsFlag = false;
						for(Map<String, String> varAuth : varAuthUser) {
							String varUserId = varAuth.get("USER_ID");
							if(currentUserId.equalsIgnoreCase(varUserId)) {
								containsFlag = true;
								break;
							}
						}
						
						if(!containsFlag) {
							authObject.revoke(PrincipalAgentType.PERSONAL, currentCompanyCode, currentUserId);
						}
					}
					
					Commit();
				}
			} catch (Exception e) {
				Rollback();
				Logger.err.println("Auth Update Fail! : " + authData);
				continue;
			}
		}
	}
}
