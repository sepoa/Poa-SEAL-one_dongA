package sepoa.svc.board;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.itextpdf.text.Paragraph;

import oracle.net.aso.e;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sepoa.fw.approval.SepoaApprovalLineSetting;
import sepoa.fw.approval.SepoaApprovalProgress;
import sepoa.fw.cache.Cache;
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
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.seal.SepoaReqSealManager;
import sepoa.seal.SepoaSealManagementManager;
import sepoa.svl.util.SepoaMultipart;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;
import sun.text.normalizer.Trie.DataManipulate;

@SuppressWarnings({ "unchecked" })
@Description("Q&A 게시판")
public class SepoaQAService extends SepoaService {

    public SepoaQAService(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
    public String getConfig(String s) {
        return CommonUtil.getConfig(s);
    }
    
    private String cleanXSS(String value) {
       	value = value.toLowerCase();
    	
    		value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    		value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
    		value = value.replaceAll("'", "&#39;");
    		value = value.replaceAll("eval\\((.*)\\)", "");
    		value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
    		value = value.replaceAll("script", "");
    		value = value.replaceAll("alert", "");
    		value = value.replaceAll("onerror", "");
    		value = value.replaceAll("append", "");
    		value = value.replaceAll("applet", "");
    		value = value.replaceAll("base", "");
    		value = value.replaceAll("bgsound", "");
    		value = value.replaceAll("binding", "");
    		value = value.replaceAll("blink", "");
    		value = value.replaceAll("charset", "");
    		value = value.replaceAll("cookie", "");
    		value = value.replaceAll("create", "");
    		value = value.replaceAll("embed", "");
    		value = value.replaceAll("expression", "");
    		value = value.replaceAll("frame", "");
    		value = value.replaceAll("frameset", "");
    		value = value.replaceAll("href", "");
    		value = value.replaceAll("iframe", "");
    		value = value.replaceAll("innerHTML", "");
    		value = value.replaceAll("layer", "");
    		value = value.replaceAll("link", "");
    		value = value.replaceAll("meta", "");
    		value = value.replaceAll("msgbox", "");
    		value = value.replaceAll("object", "");
    		value = value.replaceAll("refresh", "");
    		value = value.replaceAll("vbscript", "");
    		value = value.replaceAll("script", "");
    		value = value.replaceAll("string", "");
    		value = value.replaceAll("style", "");
    		value = value.replaceAll("title", "");
    		value = value.replaceAll("void", "");
    		value = value.replaceAll("xml", "");
    		value = value.replaceAll("FSCommand", "");		
    		value = value.replaceAll("onafter", "");
    		value = value.replaceAll("onbefore", "");
    		value = value.replaceAll("onbegin", "");
    		value = value.replaceAll("onblur", "");		
    		value = value.replaceAll("onbounce", "");
    		value = value.replaceAll("onchange", "");
    		value = value.replaceAll("oncontext", "");		
    		value = value.replaceAll("onbounce", "");
    		value = value.replaceAll("oncell", "");
    		value = value.replaceAll("onchange", "");
    		value = value.replaceAll("onclick", "");		
    		value = value.replaceAll("oncopy", "");		
    		value = value.replaceAll("oncut", "");		
    		value = value.replaceAll("ondata", "");		
    		value = value.replaceAll("ondbClick", "").replaceAll("ondbclick", "");		
    		value = value.replaceAll("ondrag", "");		
    		value = value.replaceAll("ondrop", "");		
    		value = value.replaceAll("onfilter", "");		
    		value = value.replaceAll("onfinish", "");		
    		value = value.replaceAll("onfocus", "");		
    		value = value.replaceAll("onmouse", "");		
    		value = value.replaceAll("onkey", "");		
    		value = value.replaceAll("onlayout", "");		
    		value = value.replaceAll("onlose", "");		
    		value = value.replaceAll("onload", "");		
    		value = value.replaceAll("onmove", "");		
    		value = value.replaceAll("onout", "");		
    		value = value.replaceAll("onprogress", "");		
    		value = value.replaceAll("onresize", "");		
    		value = value.replaceAll("onrow", "");		
    		value = value.replaceAll("onscroll", "");		
    		value = value.replaceAll("onseek", "");		
    		value = value.replaceAll("onselect", "");		
    		value = value.replaceAll("onstart", "");		
    		value = value.replaceAll("onstop", "");		
    		value = value.replaceAll("onsync", "");		
    		value = value.replaceAll("onsubmit", "");		
    		value = value.replaceAll("ontime", "");		
    		value = value.replaceAll("ontrack", "");		
    		value = value.replaceAll("onunload", "");		
    		value = value.replaceAll("onurlflip", "");		
    		value = value.replaceAll("seeksegmenttime", "");
		return value;
	}
	
	/**
	 * HTML 태그는 유지하면서 XSS 공격만 방지하는 함수
	 * @param value 원본 HTML 문자열
	 * @return XSS 필터링된 HTML 문자열
	 */
	private String cleanXSSForHTML(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		
		// 1. 위험한 태그 제거 (script, iframe, object, embed 등)
		value = value.replaceAll("(?i)<script[^>]*>.*?</script>", "");
		value = value.replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "");
		value = value.replaceAll("(?i)<object[^>]*>.*?</object>", "");
		value = value.replaceAll("(?i)<embed[^>]*>.*?</embed>", "");
		value = value.replaceAll("(?i)<link[^>]*>", "");
		value = value.replaceAll("(?i)<meta[^>]*>", "");
		value = value.replaceAll("(?i)<style[^>]*>.*?</style>", "");
		
		// 2. 이벤트 핸들러 속성 완전 제거 (onclick, onerror, onload 등)
		value = value.replaceAll("(?i)\\s*on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
		value = value.replaceAll("(?i)\\s*on\\w+\\s*=\\s*[^\\s>]+", "");
		
		// 3. href, src 속성에서 위험한 프로토콜이 있는 경우 속성 전체 완전 제거
		value = value.replaceAll("(?i)\\s*href\\s*=\\s*[\"']\\s*javascript:[^\"']*[\"']", "");
		value = value.replaceAll("(?i)\\s*src\\s*=\\s*[\"']\\s*javascript:[^\"']*[\"']", "");
		value = value.replaceAll("(?i)\\s*href\\s*=\\s*[\"']\\s*vbscript:[^\"']*[\"']", "");
		value = value.replaceAll("(?i)\\s*src\\s*=\\s*[\"']\\s*vbscript:[^\"']*[\"']", "");
		
		// 4. data: 프로토콜 중 위험한 타입이 있는 경우 속성 전체 완전 제거
		value = value.replaceAll("(?i)\\s*href\\s*=\\s*[\"']\\s*data\\s*:\\s*text/html[^\"']*[\"']", "");
		value = value.replaceAll("(?i)\\s*src\\s*=\\s*[\"']\\s*data\\s*:\\s*text/html[^\"']*[\"']", "");
		value = value.replaceAll("(?i)\\s*href\\s*=\\s*[\"']\\s*data\\s*:\\s*application/[^\"']*[\"']", "");
		value = value.replaceAll("(?i)\\s*src\\s*=\\s*[\"']\\s*data\\s*:\\s*application/[^\"']*[\"']", "");
		
		// 5. CSS expression() 제거
		value = value.replaceAll("(?i)expression\\s*\\(", "");
		
		// 6. 남아있는 javascript:, vbscript: 프로토콜 제거 (혹시 모를 경우 대비)
		value = value.replaceAll("(?i)javascript\\s*:", "");
		value = value.replaceAll("(?i)vbscript\\s*:", "");
		
		return value;
	}
	
	/**
	 * HTML 테이블을 엑셀 표 형식으로 변환
	 * - table, tr, td 태그는 표 형식으로 변환
	 * - 나머지 HTML 태그는 제거하고 텍스트만 추출
	 * @param html 원본 HTML 문자열
	 * @return 변환된 텍스트 (테이블은 탭과 줄바꿈으로 구분)
	 */
	private String convertHtmlToExcelFormat(String html) {
		if (html == null || html.isEmpty()) {
			return html;
		}
		
		try {
			Document doc = Jsoup.parse(html);
			StringBuilder result = new StringBuilder();
			
			// 테이블 태그 찾기
			Elements tables = doc.select("table");
			
			if (tables.isEmpty()) {
				// 테이블이 없으면 일반 HTML 태그만 제거
				return stripHtmlTags(html);
			}
			
			// 테이블이 있는 경우
			int lastIndex = 0;
			String htmlText = html;
			
			for (Element table : tables) {
				// 테이블 이전의 텍스트 처리
				String beforeTable = html.substring(lastIndex, html.indexOf(table.outerHtml(), lastIndex));
				if (!beforeTable.trim().isEmpty()) {
					result.append(stripHtmlTags(beforeTable));
				}
				
				// 테이블을 표 형식으로 변환
				Elements rows = table.select("tr");
				for (Element row : rows) {
					Elements cells = row.select("td, th");
					if (cells.isEmpty()) {
						continue;
					}
					
					boolean firstCell = true;
					for (Element cell : cells) {
						if (!firstCell) {
							result.append(" | "); // 셀 사이는 파이프(|)로 구분 (공백 포함)
						}
						// 셀 내용에서 HTML 태그 제거하고 텍스트만 추출
						// 공백 관련 태그(줄바꿈, 띄어쓰기, 탭)는 보존
						String cellText = stripHtmlTags(cell.html());
						// 캐리지 리턴만 제거 (줄바꿈과 탭은 보존)
						cellText = cellText.replace("\r", "");
						result.append(cellText);
						firstCell = false;
					}
					result.append("\n"); // 각 행은 줄바꿈으로 구분
				}
				
				lastIndex = html.indexOf(table.outerHtml(), lastIndex) + table.outerHtml().length();
			}
			
			// 테이블 이후의 텍스트 처리
			if (lastIndex < html.length()) {
				String afterTable = html.substring(lastIndex);
				if (!afterTable.trim().isEmpty()) {
					result.append(stripHtmlTags(afterTable));
				}
			}
			
			return result.toString().trim();
			
		} catch (Exception e) {
			// 파싱 실패 시 일반 HTML 태그만 제거
			Logger.err.println(info.getSession("ID"), this, "엑셀데이터 HTML 파싱 실패: " + e.getMessage());
			return stripHtmlTags(html);
		}
	}
	
	/**
	 * HTML 태그를 제거하고 순수 텍스트만 추출
	 * 공백 관련 태그와 엔티티는 보존 (줄바꿈, 띄어쓰기, 탭 유지)
	 * @param html 원본 HTML 문자열
	 * @return 태그가 제거된 텍스트
	 */
	private String stripHtmlTags(String html) {
		if (html == null || html.isEmpty()) {
			return html;
		}
		
		// 공백 관련 태그를 먼저 변환 (줄바꿈 보존)
		String text = html;
		
		// <br>, <br/> 태그를 줄바꿈으로 변환
		text = text.replaceAll("(?i)<br\\s*/?>", "\n");
		
		// <p>, </p> 태그를 줄바꿈으로 변환
		text = text.replaceAll("(?i)</p>", "\n");
		text = text.replaceAll("(?i)<p[^>]*>", "");
		
		// <div>, </div> 태그를 줄바꿈으로 변환 (블록 요소)
		text = text.replaceAll("(?i)</div>", "\n");
		text = text.replaceAll("(?i)<div[^>]*>", "");
		
		// <pre> 태그 내부는 공백과 줄바꿈을 그대로 유지하기 위해 별도 처리
		// <pre> 태그만 제거하고 내용은 보존
		text = text.replaceAll("(?i)<pre[^>]*>", "");
		text = text.replaceAll("(?i)</pre>", "");
		
		// 나머지 HTML 태그 제거
		text = text.replaceAll("<[^>]+>", "");
		
		// HTML 엔티티 디코딩 (공백 관련 엔티티 포함)
		text = text.replace("&nbsp;", " ")      // 공백
		           .replace("&ensp;", " ")      // en space (½ em)
		           .replace("&emsp;", "  ")    // em space (1 em) - 2칸 공백
		           .replace("&thinsp;", " ")   // thin space
		           .replace("&lt;", "<")
		           .replace("&gt;", ">")
		           .replace("&amp;", "&")
		           .replace("&quot;", "\"")
		           .replace("&#39;", "'")
		           .replace("&apos;", "'");
		
		// 연속된 공백 정리 제거 (줄바꿈과 탭은 보존)
		// 줄바꿈과 탭을 임시로 보호한 후 공백만 정리
		text = text.replace("\n", "\u0001")  // 줄바꿈 임시 보호
		           .replace("\r", "")        // 캐리지 리턴 제거
		           .replace("\t", "\u0002")  // 탭 임시 보호
		           .replaceAll(" +", " ")    // 연속된 공백을 하나로 (줄바꿈/탭 제외)
		           .replace("\u0001", "\n")  // 줄바꿈 복원
		           .replace("\u0002", "\t"); // 탭 복원
		
		return text;
	}

    /**
     * Q&A 목록 조회 
     * @param allData
     * @return
     */
    @Description("Q&A 체크여부 업데이트 (ST: QnA_MASTER만, L/A → CC, 완료 메일)")
    public SepoaOut doCheckFlagUpdate(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            String ctrlCode = info.getSession("CTRL_CODE");
            String sessionId = info.getSession("ID");
            if (ctrlCode == null || !ctrlCode.contains("QnA_MASTER")) {
            	setStatus(0);
            	setFlag(false);
            	setMessage("법무팀장(QnA_MASTER)만 완료여부를 지정할 수 있습니다.");
            	return getSepoaOut();
            }
            
            String checkFlag = headerData.get("CHECK_FLAG");
            if (!"Y".equals(checkFlag)) {
            	setStatus(0);
            	setFlag(false);
            	setMessage("완료여부는 Y로만 지정할 수 있으며, 지정 후 변경할 수 없습니다.");
            	return getSepoaOut();
            }
            
            sxp = new SepoaXmlParser(this, "getQnaDetail");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            if (sf.getRowCount() <= 0) {
            	setStatus(0);
            	setFlag(false);
            	setMessage("법무검토 문서를 찾을 수 없습니다.");
            	return getSepoaOut();
            }
            Map<String, String> qnaDetail = sf.getListMap().get(0);
            String qnaStatus = MapUtils.getString(qnaDetail, "STATUS", "");
            String currentCheckFlag = MapUtils.getString(qnaDetail, "CHECK_FLAG", "N");
            String qnaNumber = MapUtils.getString(headerData, "QNA_NUMBER",
            		MapUtils.getString(qnaDetail, "QNA_NUMBER", ""));
            if ("Y".equals(currentCheckFlag)) {
            	setStatus(0);
            	setFlag(false);
            	setMessage("완료여부(Y)는 변경할 수 없습니다.");
            	return getSepoaOut();
            }
            if (!"L".equals(qnaStatus) && !"A".equals(qnaStatus)) {
            	setStatus(0);
            	setFlag(false);
            	setMessage("이견없음 또는 검토요청 건에 대해서만<br/>완료여부를 지정할 수 있습니다.");
            	return getSepoaOut();
            }
            
            // ST: CHECK_FLAG=Y 확정 시 상태를 검토완료(CC)로 변경 (되돌리기 불가)
            // A=검토요청(담당자), CC=검토완료(팀장 확정)
            headerData.put("STATUS", "CC");
            sxp = new SepoaXmlParser(this, "doCheckFlagUpdate");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            int updated = ssm.doUpdate(headerData);
            if (updated <= 0) {
            	setStatus(0);
            	setFlag(false);
            	setMessage("완료여부 변경에 실패하였습니다.");
            	Rollback();
            	return getSepoaOut();
            }
            setValue(updated);

            // 기안자/지정담당자 완료 메일 — 실패해도 완료 확정은 유지
            try {
            	Map<String, Object> mailAllData = new HashMap<String, Object>();
            	Map<String, String> mailHeader = new HashMap<String, String>();
            	mailHeader.put("QNA_NUMBER", qnaNumber);
            	mailHeader.put("STATUS", "CC");
            	mailAllData.put(SepoaDataMapper.KEY_RAW_HEADER_DATA, mailHeader);
            	mailAllData.put(SepoaDataMapper.KEY_HEADER_DATA, mailHeader);
            	SepoaOut mailOut = ServiceConnector.doService(info,
            			"sepoa.svc.cbo.qna.postprocess.SepoaQnaPostProcessor", ctx, "doCompleteProcess",
            			new Object[] { mailAllData });
            	if (mailOut == null || mailOut.status != 1 || !mailOut.flag) {
            		Logger.err.println(sessionId, this, "QnA complete mail failed. QNA_NUMBER=" + qnaNumber
            				+ (mailOut != null ? (", msg=" + mailOut.message) : ""));
            	}
            } catch (Exception mailEx) {
            	Logger.err.println(sessionId, this, "QnA complete mail exception. QNA_NUMBER=" + qnaNumber);
            	Logger.err.println(sessionId, this, mailEx);
            }
            
            Commit();
        } catch (Exception e) {
        	Rollback();
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    /**
     * Q&A 목록 조회 
     * @param allData
     * @return
     */
    @Description("Q&A 조회")
    public SepoaOut doQuery(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
        	
            if (info.getSession("CTRL_CODE").contains("MANAGE_CONFIRM") && !"S".equals(info.getSession("USER_TYPE"))) {
            	headerData.put("COMPANY_CODE_CONDITION", "N");
            } else {
            	headerData.put("COMPANY_CODE_CONDITION", "Y");
            }
            
            String systemProfile = getConfig("sepoa.system.manager.profile.code"); // 시스템 매니저
            
            boolean isManager = false;
            if(info.getSession("CTRL_CODE").contains("QnA_MANAGER") || info.getSession("CTRL_CODE").contains("QnA_MANAGER_D") || systemProfile.equals(info.getSession("MENU_PROFILE_CODE"))){
            	isManager = true;
            	headerData.put("QNA_FLAG", "Y");
            } 

            // TODO : 내가 부서 장인지 검사 해야함.
            //final String[] Codes = {"O855638653", "O582630042", "O855184264", "O160165875", "O461008120", "O782448314", "O567480772", "O580033058"};
            final String responsibilityCode = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+'#'+info.getSession("LANGUAGE")+"#UX050");
            final List<Map<String,String>> responsibilityDefine = new Gson().fromJson(responsibilityCode, List.class);
            
            sxp = new SepoaXmlParser(this, "doQueryResponsibilityCode");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            
            boolean hasResponsibility = false;
            if(sf != null && sf.getListMap().size() > 0) {
            	hasResponsibility = sf.getListMap().stream()
                        .anyMatch(sfMap -> responsibilityDefine.stream()
                            .anyMatch(profile -> profile.get("value").equals(sfMap.get("RESPONSIBILITY"))));            	
            }
            
            // 부서장일경우 true
            if(!isManager && hasResponsibility) {
            	List<String> deptList = new ArrayList<String>();
                deptList.add(info.getSession("DEPARTMENT"));
                
                // 전체 부서 가져온다.
                sxp = new SepoaXmlParser(this, "doSelectSDEPT");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                sf = new SepoaFormater(ssm.doSelect(headerData));
                
                if (sf.getRowCount() > 0) {            	
                	List<Map<String, String>> deptListMap = sf.getListMap();
                	
                	boolean hasMoreParents = true;
                	while (hasMoreParents) {
                		hasMoreParents = false;
                		for (Map<String, String> map : deptListMap) {
                			String parentDept = map.get("PARENT_DEPT");
                			if (deptList.contains(parentDept)) {
                				
                				String dept = map.get("DEPT");
                				if (!deptList.contains(dept)) {
                					deptList.add(dept); 
                					hasMoreParents = true;
                				}
                			}
                		}
                	}
                }
                
                sxp = new SepoaXmlParser(this, "doSelectSQNAQ");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                sf = new SepoaFormater(ssm.doSelect(headerData));

                List<Map<String, String>> filteredQnaListMap = new ArrayList<Map<String, String>>();
                
                if (sf.getRowCount() > 0) {            	
                	List<Map<String, String>> qnaListMap = sf.getListMap();
                	
                	for (Map<String, String> qnaMap : qnaListMap) {
                		// 본인 부서 및 하위부서의 건이거나 본인이 작성한 건인 경우 조회
                		if (deptList.contains(qnaMap.get("QNA_USER_DEPT_CODE")) 
                				|| info.getSession("ID").equals(qnaMap.get("ADD_USER_ID"))) {
                			filteredQnaListMap.add(qnaMap);
                		}
                	}
                }
                setValue(new SepoaFormater(filteredQnaListMap).getString());
            }else {
            	sxp = new SepoaXmlParser();
            	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            	setValue(ssm.doSelect(headerData));
            	
            }
        
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    /**
     * Q&A 엑셀 다운로드
     * CONTENT 컬럼의 HTML을 엑셀 표 형식으로 변환
     * 선택한 행이 있으면 해당 행만, 없으면 필터 조건에 맞는 전체 데이터 다운로드
     * @param allData
     * @return
     */
    @Description("Q&A 엑셀 다운로드")
    public SepoaOut doExcelDownload(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
		ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            
            // 선택한 QNA_NUMBER 목록이 있는지 확인
            String selectedQnaNumbers = MapUtils.getString(headerData, "SELECTED_QNA_NUMBERS", "");
            
            if (selectedQnaNumbers != null && !selectedQnaNumbers.isEmpty()) {
				Map<String,String> tmpMap = new HashMap<>();
				tmpMap.put("SELECTED_QNA_NUMBERS", selectedQnaNumbers);

	            List<Map<String, String>> excelHeaderDataList = new ArrayList<>();
	            List<Map<String, String>> excelReplyDataList = new ArrayList<>();
	
	            // 게시글 데이터 조회
	            sxp = new SepoaXmlParser(this, "doQueryExcelHeaderData");
	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            sf = new SepoaFormater(ssm.doSelect(tmpMap));
	            excelHeaderDataList = sf.getListMap();
	
	            // 댓글 데이터 조회
	            sxp = new SepoaXmlParser(this, "doQueryExcelReplyData");
	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            sf = new SepoaFormater(ssm.doSelect(tmpMap));
	            excelReplyDataList = sf.getListMap();
	
	            // 엑셀 데이터 세팅
	            List<Map<String, String>> excelResultDataList = new ArrayList<>();
	            Map<String,String> initMap = new HashMap<>();
	            initMap.put("ROW_NUM","");
	            initMap.put("QNA_NUMBER","");
	            initMap.put("QNA_SEQ","");
	            initMap.put("REPLY_CNT","");
	            initMap.put("SUBJECT","");
	            initMap.put("CONTENT","");
	            initMap.put("KEYWORDS","");
	            initMap.put("QNA_USER_DEPT_NAME","");
	            initMap.put("ADD_USER_NAME_LOC","");
	            initMap.put("ADD_DATE","");
				initMap.put("ADD_TIME","");
				initMap.put("GW_CONFIRM_DATE","");
	            initMap.put("STATUS","");
	            initMap.put("MANAGER","");
	            excelResultDataList.add(initMap);
	
	            if(excelHeaderDataList != null && !excelHeaderDataList.isEmpty()) {
	                for(Map<String, String> excelHeaderData : excelHeaderDataList) {
	                    String headerContent = excelHeaderData.get("CONTENT");
	                    if (headerContent != null && !headerContent.isEmpty()) {
	                        excelHeaderData.put("CONTENT", convertHtmlToExcelFormat(headerContent));
	                    }
	                    excelResultDataList.add(excelHeaderData);
	                    
	                    // 해당 게시글의 댓글 추가
	                    if(excelReplyDataList != null && !excelReplyDataList.isEmpty()) {
	                        for(Map<String, String> excelReplyData : excelReplyDataList) {
	                            if(excelHeaderData.get("QNA_NUMBER").equals(excelReplyData.get("QNA_NUMBER"))) {
	                                String replyContent = excelReplyData.get("CONTENT");
	                                if (replyContent != null && !replyContent.isEmpty()) {
	                                    excelReplyData.put("CONTENT", convertHtmlToExcelFormat(replyContent));
	                                }
	                                excelResultDataList.add(excelReplyData);
	                            }
	                        }
	                    }
	                }
	            }
	
	            if (excelResultDataList.isEmpty()) {
	                setStatus(0);
	                setFlag(false);
	                setMessage("다운로드할 데이터가 없습니다.");
	                setValue("");
	            } else {
	                setValue(new SepoaFormater(excelResultDataList));
	            }
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        
        return getSepoaOut();
    }
    
    /**
     * Q&A 상세 조회 
     * @param allData
     * @return
     */
    @Description("Q&A 상세 조회")
    public SepoaOut getDetail(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            
            /*
             * 게시판에 맞는 QNA_TYPE 세팅 및 접근가능한 모든 QNA LIST 조회
             * 조회된(접근가능한) QNA_NUMBER만 Arr 추가
             * 요청받은 headerData의 값을 비교하여 접근거부 처리 (프록시 위변조 시도시)
             */

            String systemProfile = getConfig("sepoa.system.manager.profile.code");
            if (info.getSession("CTRL_CODE").contains("MANAGE_CONFIRM") && !"S".equals(info.getSession("USER_TYPE"))) {
            	headerData.put("COMPANY_CODE_CONDITION", "N");
            } else {
            	headerData.put("COMPANY_CODE_CONDITION", "Y");
            }
            
			boolean isManager = false;
            if(info.getSession("CTRL_CODE").contains("QnA_MANAGER") || info.getSession("CTRL_CODE").contains("QnA_MANAGER_D") || systemProfile.equals(info.getSession("MENU_PROFILE_CODE"))){
            	isManager = true;
				headerData.put("QNA_FLAG", "Y");
            }
            
            if(headerData.get("QNA_NUMBER").startsWith("C")) {
            	headerData.put("CNTR_TYPE", "C");
            }else {
            	headerData.put("CNTR_TYPE", "L");
            }

			// 부서장일 경우 상세내용 조회도 가능해야한다.
            //final String[] Codes = {"O855638653", "O582630042", "O855184264", "O160165875", "O461008120", "O782448314", "O567480772", "O580033058"};
            final String responsibilityCode = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+'#'+info.getSession("LANGUAGE")+"#UX050");
            final List<Map<String,String>> responsibilityDefine = new Gson().fromJson(responsibilityCode, List.class);
            
            sxp = new SepoaXmlParser(this, "doQueryResponsibilityCode");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            
            boolean hasResponsibility = false;
            if(sf != null && sf.getListMap().size() > 0) {
            	hasResponsibility = sf.getListMap().stream()
                        .anyMatch(sfMap -> responsibilityDefine.stream()
                            .anyMatch(profile -> profile.get("value").equals(sfMap.get("RESPONSIBILITY"))));            	
			}

			Map<String, Object> bodyDeatilData = new HashMap<String, Object>();
			String qnaNumber = headerData.get("QNA_NUMBER");

			// 부서장일경우 true
            if(!isManager && hasResponsibility) {
            	List<String> deptList = new ArrayList<String>();
                deptList.add(info.getSession("DEPARTMENT"));
                
                // 전체 부서 가져온다.
                sxp = new SepoaXmlParser(this, "doSelectSDEPT");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                sf = new SepoaFormater(ssm.doSelect(headerData));
                
                if (sf.getRowCount() > 0) {            	
                	List<Map<String, String>> deptListMap = sf.getListMap();
                	
                	boolean hasMoreParents = true;
                	while (hasMoreParents) {
                		hasMoreParents = false;
                		for (Map<String, String> map : deptListMap) {
                			String parentDept = map.get("PARENT_DEPT");
                			if (deptList.contains(parentDept)) {
                				
                				String dept = map.get("DEPT");
                				if (!deptList.contains(dept)) {
                					deptList.add(dept); 
                					hasMoreParents = true;
                				}
                			}
                		}
                	}
                }
                
                sxp = new SepoaXmlParser(this, "doSelectSQNAQ");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                sf = new SepoaFormater(ssm.doSelect(headerData));

                List<Map<String, String>> filteredQnaListMap = new ArrayList<Map<String, String>>();
                
                if (sf.getRowCount() > 0) {            	
                	List<Map<String, String>> qnaListMap = sf.getListMap();
                	
                	for (Map<String, String> qnaMap : qnaListMap) {
                		// 본인 부서 및 하위부서의 건이거나 본인이 작성한 건인 경우 조회
                		if (deptList.contains(qnaMap.get("QNA_USER_DEPT_CODE")) 
                				|| info.getSession("ID").equals(qnaMap.get("ADD_USER_ID"))) {
                			filteredQnaListMap.add(qnaMap);
                		}
                	}
                }
				List<String> qnaList = filteredQnaListMap.stream()
						.map(e -> e.get("QNA_NUMBER"))
						.collect(Collectors.toList());

				// 요청받은 headerData의 값과 접근가능한 문서인지 확인 후 Exception 처리  
				if(!qnaList.contains(qnaNumber)) {
					Logger.info.println("Request QNA Type : " + headerData.get("CNTR_TYPE"));
					throw new Exception("Request information is not valid, please check again.");
				}else {					
					sxp = new SepoaXmlParser(this, "getQnaDetail");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(headerData));
					if(sf.getRowCount() > 0) {
						bodyDeatilData.put("QNA_DETAIL", sf.getListMap().get(0));
					}
					
					sxp = new SepoaXmlParser(this, "getQnaDetailRefLink");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(headerData));
					if(sf.getRowCount() > 0) {
						bodyDeatilData.put("QNA_LINK", sf.getListMap().get(0));
					}
				}
            }else {
				sxp = new SepoaXmlParser(this, "getQnaListNumber");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData));
				
				List<String> qnaList = sf.getListMap().stream()
						.map(e -> e.get("QNA_NUMBER"))
						.collect(Collectors.toList());
				
				// 요청받은 headerData의 값과 접근가능한 문서인지 확인 후 Exception 처리  
				if(!qnaList.contains(qnaNumber)) {
					Logger.info.println("Request QNA Type : " + headerData.get("CNTR_TYPE"));
					throw new Exception("Request information is not valid, please check again.");
				}else {					
					sxp = new SepoaXmlParser(this, "getQnaDetail");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(headerData));
					if(sf.getRowCount() > 0) {
						bodyDeatilData.put("QNA_DETAIL", sf.getListMap().get(0));
					}
					
					sxp = new SepoaXmlParser(this, "getQnaDetailRefLink");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(headerData));
					if(sf.getRowCount() > 0) {
						bodyDeatilData.put("QNA_LINK", sf.getListMap().get(0));
					}
				}
			}
				
			if(bodyDeatilData.size() > 0) {
				setValue(new Gson().toJson(bodyDeatilData));
			}
			
			//	Reply 존재 여부 확인
			sxp = new SepoaXmlParser(this, "getQnaReply");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			//	Reply 존재할 경우 Reply 목록을 담는다
			if(sf.getRowCount() > 0) {            	
				setValue(ssm.doSelect(headerData));
			}
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    /**
     * Q&A 등록 , 수정, 댓글 등록
     * @param allData
     * @return
     */
    @Description("Q&A 글 / 댓글 등록, 수정")
    public SepoaOut doSave(Map<String, Object> allData) {
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        boolean nextFlag = false;
        ConnectionContext ctx = getConnectionContext();
        
        try {
        	Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
        	
            Configuration conf = new Configuration();
            
            // CONTENT XSS 필터링 (HTML 태그는 유지하면서 XSS만 방지)
            if (headerData.containsKey("CONTENT") && headerData.get("CONTENT") != null) {
                String content = headerData.get("CONTENT");
                String cleanedContent = cleanXSSForHTML(content);
                headerData.put("CONTENT", cleanedContent);
            }
           
            String qnaNumber = MapUtils.getString(headerData, "QNA_NUMBER", "");
            String jsonString = MapUtils.getString(headerData, "JOIN_STRING", "");
            
            if("".equals(qnaNumber)){	// Q&A 등록
            	
            	String qnaDocType = "";
            	
            	if(headerData.get("CNTR_TYPE").equals("C")) {
            		qnaDocType = "CQNA%"; // 일반 법무검토 요청현황
            		qnaNumber = DocumentUtil.getDocNumberRemote(info, "CQNA").result[0]; // 계약용 채번
            	} else {
            		qnaDocType = "LQNA%"; // 계약서 법무검토 요청현황
            		qnaNumber = DocumentUtil.getDocNumberRemote(info, "QNA").result[0]; // 계약외 채번
            	}
            	
                headerData.put("QNA_NUMBER", qnaNumber);
                headerData.put("CONTENT", headerData.get("CONTENT"));
                headerData.put("LINK_QNA_NUMBER", "LINK_" + qnaNumber);
                headerData.put("LINK_CONTENT", jsonString);
                headerData.put("STATUS", "T");
                headerData.put("QNA_DOC_TYPE", qnaDocType);
                
                sxp = new SepoaXmlParser(this, "insertQna");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doInsert(headerData);
                
                if(headerData.containsKey("JOIN_STRING")) {
                	sxp = new SepoaXmlParser(this, "duplicateInsertQna");
                	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                	ssm.doInsert(headerData);
                }
                
                // ST: 법무팀 메일은 GW 결재승인(검토대기) 시점에 발송
                setValue(qnaNumber);
            }else{
            	String mode = MapUtils.getString(headerData, "mode", "");
            	if("EDIT".equals(mode)) {	// Q&A 수정
    	            sxp = new SepoaXmlParser(this, "checkReply");
    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
    	            sf = new SepoaFormater(ssm.doSelect(headerData));
    	            if (sf.getRowCount() > 0) {
        				setStatus(2);
        	            setFlag(false);
        	            setMessage(Cache.text.get("QA_002.MESSAGE_CANNOT_UPDATE")); 
        	            nextFlag = true;
    	            } else {
    	                headerData.put("QNA_NUMBER", qnaNumber);
    	                headerData.put("CONTENT", headerData.get("CONTENT"));
    	                headerData.put("LINK_QNA_NUMBER", "LINK_" + qnaNumber);
    	                headerData.put("LINK_CONTENT", jsonString);
    	                headerData.put("STATUS", "T");
    	            	
	            		sxp = new SepoaXmlParser(this, "updateQna");
	                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                    ssm.doUpdate(headerData);
	                    
	                    if(headerData.containsKey("JOIN_STRING")) {
	                    	sxp = new SepoaXmlParser(this, "updateDuplicateQna");
	                    	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                    	ssm.doUpdate(headerData);
	                    }
    	            }
            	}else if("REPLY".equals(mode)){	// Q&A 답글 등록
            		sxp = new SepoaXmlParser(this, "insertReply");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doInsert(headerData);
                    
                    //QNA 댓글 작성 후처리 모듈
                    if(SepoaModuleService.checkBusiness(new Configuration(), info, "DSB1684744519396")) {
                    	SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.qna.postprocess.SepoaQnaPostProcessor", ctx, "doReplyProcess", new Object[] {allData});
    					if(out == null) {
    						setStatus(70009);
    						Logger.err.println("QnA Reply PostProcess Result Null!");
    					} else if(out.status != 1 || !out.flag) {
    						setStatus(out.status);
    						Logger.err.println("QnA Reply PostProcess Fail!");
    					}
                    }
            	}
            }
            
            if (nextFlag == false) {
	            Commit();
	            setStatus(1);
            }
        } catch (Exception e) {
            Rollback();
            if(getStatus() == 1) setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    /**
     * Q&A 삭제 , 댓글 삭제
     * @param allData
     * @return
     */
    @Description("Q&A 글 / 댓글 삭제")
    public SepoaOut doDelete(Map<String, Object> allData) {
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        boolean chk_exception = false;        
        ConnectionContext ctx = getConnectionContext();
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);

            if (info.getSession("CTRL_CODE").contains("MANAGE_CONFIRM")) {
            	headerData.put("ADD_USER_ID_CONDITION", "N");
            } else {
            	headerData.put("ADD_USER_ID_CONDITION", "Y");
            }
			
        	String mode = MapUtils.getString(headerData, "mode", "");
        	
        	if("".equals(mode)) {
	            sxp = new SepoaXmlParser(this, "checkReply");
	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            sf = new SepoaFormater(ssm.doSelect(headerData));
	            
	            if (sf.getRowCount() > 0) {
    				setStatus(2);
    	            setFlag(false);
    	            setMessage(Cache.text.get("QA_002.MESSAGE_CANNOT_UPDATE")); 
    	            chk_exception = true;
	            } else {
	            	
	            	/*
	            	 * 작성자외 글의 삭제를 막이위해 삭제요청들어온 글작성자 정보와 세션정보가 일치하는지 확인 후 처리 
	            	 */
	            	sxp = new SepoaXmlParser(this, "doCheckWriteInfo");
	                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                sf = new SepoaFormater(ssm.doSelect(headerData));
	                
	                if(sf != null && sf.getRowCount() > 0) {
	                	String addUserId = sf.getListMap().get(0).get("ADD_USER_ID");
	                	if(!addUserId.equals((info.getSession("ID")))){
	                		throw new Exception("Request information is not valid, please check again.");
	                	} else {
	                		sxp = new SepoaXmlParser(this, "deleteQna");
		                	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		                	ssm.doUpdate(headerData);
		                	
	                		sxp = new SepoaXmlParser(this, "deleteLinkQna");
		                	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		                	ssm.doDelete(headerData);
	                	}
	                }
	            }
        	}else if("REPLY".equals(mode)){
        		sxp = new SepoaXmlParser(this, "deleteReply");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doInsert(headerData);
        	}
        	
        	if (chk_exception == false) {
	        	Commit();
	            setStatus(1);
        	}
        } catch (Exception e) {
        	Rollback();
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    /**
     * 법무검토 결재 상신
     * @param allData
     * @return
     */
    @Description("결재 상신 서비스")
    public SepoaOut doRequestApproval(Map<String, Object> allData) {
        ConnectionContext ctx = getConnectionContext();
        setStatus(1);
		setFlag(true);
		
        try {
        	Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
        	        	
        	headerData.put("SUX_WKEY1", headerData.get("QNA_NUMBER"));
        	headerData.put("APP_TYPE", "Q01");
        	
        	SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
 			sepoaApprovalLineSetting.doApprovalProcess(allData);
        	
 			SepoaApprovalProgress sepoaApprovalProgress = new SepoaApprovalProgress(ctx, info);
            sepoaApprovalProgress.doApprovalProcess(allData);
 			
        	Commit();
        } catch (Exception e) {
        	Rollback();
        	e.printStackTrace();
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        
        return getSepoaOut();
    }
    
    @Description("법무검토 결재 진행")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalProgress(ConnectionContext ctx, Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        
		setStatus(1);
		setFlag(true);
		try {
			String qnaStatus = "I";
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			headerData.put("STATUS", qnaStatus);
			
			sxp = new SepoaXmlParser(this, "doUpdateDocStatus");
        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
        	ssm.doUpdate(headerData);
			
        	Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
    
    @Description("법무검토 결재 진행")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalReject(ConnectionContext ctx, Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        
		setStatus(1);
		setFlag(true);
		try {
			String qnaStatus = "R";
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			headerData.put("APPROVAL_STATUS", qnaStatus);
			headerData.put("QNA_NUMBER", headerData.get("SUX_WKEY1"));
			
			sxp = new SepoaXmlParser(this, "doUpdateDocStatus");
        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
        	ssm.doUpdate(headerData);
			
        	Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
    
    @Description("법무검토 결재 승인")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalConfirm(ConnectionContext ctx, Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        
		setStatus(1);
		setFlag(true);
		try {
			String qnaStatus = "F";
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			headerData.put("STATUS", qnaStatus);
			headerData.put("QNA_NUMBER", headerData.get("SUX_WKEY1"));
			
			sxp = new SepoaXmlParser(this, "doUpdateDocStatus");
        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
        	ssm.doUpdate(headerData);
			
        	Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
    
    @Description("검토대기 전환 시 법무팀 메일 발송")
	@SuppressWarnings("unchecked")
	public SepoaOut doSendReviewWaitMail(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			if (headerData == null) {
				headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			}
			if (headerData == null) {
				headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			}
			
			String qnaNumber = MapUtils.getString(headerData, "QNA_NUMBER", "");
			if (qnaNumber == null || "".equals(qnaNumber.trim())) {
				setStatus(0);
				setFlag(false);
				setMessage("QNA_NUMBER is empty.");
				return getSepoaOut();
			}
			
			// 첨부키 보강 (메일 첨부용) — INTERFACE 세션은 LANGUAGE 없어 getQnaDetail 사용 불가
			try {
				sxp = new SepoaXmlParser(this, "getQnaAttachNo");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData));
				if (sf.getRowCount() > 0) {
					Map<String, String> qnaData = sf.getListMap().get(0);
					if (qnaData.get("ATTACH_NO") != null && !"".equals(qnaData.get("ATTACH_NO"))) {
						headerData.put("ATTACH_NO", qnaData.get("ATTACH_NO"));
					}
				}
			} catch (Exception attachEx) {
				// 첨부 보강 실패해도 본문 메일은 발송
				Logger.err.println(info.getSession("ID"), this, "QnA review-wait attach enrich skip. QNA_NUMBER=" + qnaNumber);
				Logger.err.println(info.getSession("ID"), this, attachEx);
			}
			
			Map<String, Object> mailAllData = new HashMap<String, Object>();
			mailAllData.put(SepoaDataMapper.KEY_RAW_HEADER_DATA, headerData);
			mailAllData.put(SepoaDataMapper.KEY_HEADER_DATA, headerData);
			
			if (SepoaModuleService.checkBusiness(new Configuration(), info, "DSB1684477026257")) {
				SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.qna.postprocess.SepoaQnaPostProcessor", ctx, "doWriteProcess", new Object[] { mailAllData });
				if (out == null) {
					setStatus(70009);
					setFlag(false);
					Logger.err.println("QnA ReviewWait Mail PostProcess Result Null!");
				} else if (out.status != 1 || !out.flag) {
					setStatus(out.status);
					setFlag(false);
					setMessage(out.message);
					Logger.err.println("QnA ReviewWait Mail PostProcess Fail!");
				}
			} else {
				Logger.err.println(info.getSession("ID"), this,
						"QnA review-wait mail skipped: business DSB1684477026257 inactive. QNA_NUMBER=" + qnaNumber);
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
    
    @Description("법무검토 담당자 지정")
	@SuppressWarnings("unchecked")
	public SepoaOut doAssginManager(Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        ConnectionContext ctx = getConnectionContext();
        
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			//담당자 지정
			sxp = new SepoaXmlParser(this, "doUpdateQnaManager");
        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
        	ssm.doUpdate(headerData);
        	
			//상태 변경
			String qnaStatus = "G";
			headerData.put("STATUS", qnaStatus);
			sxp = new SepoaXmlParser(this, "doUpdateDocStatus");
        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
        	ssm.doUpdate(headerData);
			
        	Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
    
    @Description("법무검토 본인 담당 검토진행")
	@SuppressWarnings("unchecked")
	public SepoaOut doStartReview(Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        ConnectionContext ctx = getConnectionContext();
        
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			if (headerData == null) {
				headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			}
			
			String selectedQnaNumbers = MapUtils.getString(headerData, "SELECTED_QNA_NUMBERS", "");
			String cntrType = MapUtils.getString(headerData, "CNTR_TYPE", "");
			String ctrlCode = info.getSession("CTRL_CODE");
			
			boolean isAllowed;
			String qnaPrefix;
			// 일반(C): QnA_MANAGER_D / 계약서: QnA_MANAGER
			if ("C".equals(cntrType)) {
				isAllowed = ctrlCode != null && ctrlCode.contains("QnA_MANAGER_D");
				qnaPrefix = "CQNA";
			} else {
				isAllowed = ctrlCode != null && ctrlCode.contains("QnA_MANAGER");
				qnaPrefix = "LQNA";
			}
			
			if (!isAllowed) {
				setStatus(0);
				setFlag(false);
				setMessage("법무검토 담당자만 검토진행할 수 있습니다.");
				return getSepoaOut();
			}
			
			if (selectedQnaNumbers == null || selectedQnaNumbers.trim().isEmpty()) {
				setStatus(0);
				setFlag(false);
				setMessage("검토진행할 항목을 선택해주세요.");
				return getSepoaOut();
			}
			
			String[] qnaNumberArr = selectedQnaNumbers.split(",");
			String sessionId = info.getSession("ID");
			
			for (String rawQnaNumber : qnaNumberArr) {
				String qnaNumber = rawQnaNumber == null ? "" : rawQnaNumber.trim();
				if ("".equals(qnaNumber)) {
					continue;
				}
				
				if (!qnaNumber.startsWith(qnaPrefix)) {
					setStatus(0);
					setFlag(false);
					setMessage("현재 메뉴에서 처리할 수 없는 문서입니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
				}
				
				Map<String, String> param = new HashMap<String, String>();
				param.put("QNA_NUMBER", qnaNumber);
				
				sxp = new SepoaXmlParser(this, "doSelectQnaForStartReview");
	        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	        	sf = new SepoaFormater(ssm.doSelect(param));
	        	
	        	if (sf.getRowCount() <= 0) {
	        		setStatus(0);
					setFlag(false);
					setMessage("법무검토 문서를 찾을 수 없습니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
	        	
	        	Map<String, String> qnaData = sf.getListMap().get(0);
	        	String qnaStatus = MapUtils.getString(qnaData, "STATUS", "");
	        	String managerId = MapUtils.getString(qnaData, "MANAGER", "");
	        	
	        	if ("G".equals(qnaStatus) && sessionId.equals(managerId)) {
	        		setStatus(0);
					setFlag(false);
					setMessage("검토진행중인 건으로 선택할 수 없습니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
	        	
	        	if ("G".equals(qnaStatus)) {
	        		setStatus(0);
					setFlag(false);
					setMessage("이미 검토진행중인 건입니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
	        	
	        	if (!"F".equals(qnaStatus)) {
	        		setStatus(0);
					setFlag(false);
					setMessage("검토대기 건에 대해서만 검토진행할 수 있습니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
	        	
	        	sxp = new SepoaXmlParser(this, "doStartReviewUpdate");
	        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	        	ssm.doUpdate(param);
	        	
	        	sxp = new SepoaXmlParser(this, "doSelectQnaForStartReview");
	        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	        	sf = new SepoaFormater(ssm.doSelect(param));
	        	
	        	if (sf.getRowCount() <= 0) {
	        		setStatus(0);
					setFlag(false);
					setMessage("법무검토 문서를 찾을 수 없습니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
	        	
	        	Map<String, String> updatedData = sf.getListMap().get(0);
	        	String updatedStatus = MapUtils.getString(updatedData, "STATUS", "");
	        	String updatedManager = MapUtils.getString(updatedData, "MANAGER", "");
	        	
	        	if (!"G".equals(updatedStatus) || !sessionId.equals(updatedManager)) {
	        		setStatus(0);
					setFlag(false);
					setMessage("이미 처리되었거나 검토대기 상태가 아닙니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
			}
			
        	Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
    
    @Description("ST: 담당자 이견없음(L)/검토완료(A) 회수 → 검토진행중(G) (팀장 CHECK_FLAG=Y 전)")
	@SuppressWarnings("unchecked")
	public SepoaOut doRecallReview(Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        ConnectionContext ctx = getConnectionContext();
        
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			if (headerData == null) {
				headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			}
			
			String selectedQnaNumbers = MapUtils.getString(headerData, "SELECTED_QNA_NUMBERS", "");
			String cntrType = MapUtils.getString(headerData, "CNTR_TYPE", "");
			String ctrlCode = info.getSession("CTRL_CODE");
			
			boolean isAllowed;
			String qnaPrefix;
			if ("C".equals(cntrType)) {
				isAllowed = ctrlCode != null && ctrlCode.contains("QnA_MANAGER_D");
				qnaPrefix = "CQNA";
			} else {
				isAllowed = ctrlCode != null && ctrlCode.contains("QnA_MANAGER");
				qnaPrefix = "LQNA";
			}
			
			if (!isAllowed) {
				setStatus(0);
				setFlag(false);
				setMessage("법무검토 담당자만 회수할 수 있습니다.");
				return getSepoaOut();
			}
			
			if (selectedQnaNumbers == null || selectedQnaNumbers.trim().isEmpty()) {
				setStatus(0);
				setFlag(false);
				setMessage("회수할 항목을 선택해주세요.");
				return getSepoaOut();
			}
			
			String[] qnaNumberArr = selectedQnaNumbers.split(",");
			String sessionId = info.getSession("ID");
			
			for (String rawQnaNumber : qnaNumberArr) {
				String qnaNumber = rawQnaNumber == null ? "" : rawQnaNumber.trim();
				if ("".equals(qnaNumber)) {
					continue;
				}
				
				if (!qnaNumber.startsWith(qnaPrefix)) {
					setStatus(0);
					setFlag(false);
					setMessage("현재 메뉴에서 처리할 수 없는 문서입니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
				}
				
				Map<String, String> param = new HashMap<String, String>();
				param.put("QNA_NUMBER", qnaNumber);
				
				sxp = new SepoaXmlParser(this, "doSelectQnaForStartReview");
	        	ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
	        	sf = new SepoaFormater(ssm.doSelect(param));
	        	
	        	if (sf.getRowCount() <= 0) {
	        		setStatus(0);
					setFlag(false);
					setMessage("법무검토 문서를 찾을 수 없습니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
	        	
	        	Map<String, String> qnaData = sf.getListMap().get(0);
	        	String qnaStatus = MapUtils.getString(qnaData, "STATUS", "");
	        	String managerId = MapUtils.getString(qnaData, "MANAGER", "");
	        	String checkFlag = MapUtils.getString(qnaData, "CHECK_FLAG", "N");
	        	
	        	if (!"L".equals(qnaStatus) && !"A".equals(qnaStatus)) {
	        		setStatus(0);
					setFlag(false);
					setMessage("이견없음 또는 검토요청 건만 회수할 수 있습니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
	        	if ("Y".equals(checkFlag)) {
	        		setStatus(0);
					setFlag(false);
					setMessage("팀장이 완료여부(Y)를 지정한 건은 회수할 수 없습니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
	        	if (managerId == null || !sessionId.equals(managerId)) {
	        		setStatus(0);
					setFlag(false);
					setMessage("본인이 담당한 건만 회수할 수 있습니다.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
	        	
	        	sxp = new SepoaXmlParser(this, "doRecallReviewUpdate");
	        	ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
	        	int updated = ssm.doUpdate(param);
	        	if (updated <= 0) {
	        		setStatus(0);
					setFlag(false);
					setMessage("회수 처리에 실패하였습니다. 이미 처리되었는지 확인해주세요.<br/>[" + qnaNumber + "]");
					Rollback();
					return getSepoaOut();
	        	}
			}
			
        	Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
    
    @Description("ST: 담당자 이견없음(L)/검토완료(A) 확정 및 법무팀장 메일")
	@SuppressWarnings("unchecked")
	public SepoaOut doFinalizeReview(Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        ConnectionContext ctx = getConnectionContext();
        
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			if (headerData == null) {
				headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			}
			
			String qnaNumber = MapUtils.getString(headerData, "QNA_NUMBER", "");
			String finalizeStatus = MapUtils.getString(headerData, "STATUS", "");
			String sessionId = info.getSession("ID");
			
			if (qnaNumber == null || "".equals(qnaNumber.trim())) {
				setStatus(0);
				setFlag(false);
				setMessage("문서번호가 없습니다.");
				return getSepoaOut();
			}
			if (!"L".equals(finalizeStatus) && !"A".equals(finalizeStatus)) {
				setStatus(0);
				setFlag(false);
				setMessage("이견없음 또는 검토요청만 선택할 수 있습니다.");
				return getSepoaOut();
			}
			
			sxp = new SepoaXmlParser(this, "doSelectQnaForStartReview");
        	ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
        	sf = new SepoaFormater(ssm.doSelect(headerData));
        	if (sf.getRowCount() <= 0) {
        		setStatus(0);
				setFlag(false);
				setMessage("법무검토 문서를 찾을 수 없습니다.");
				return getSepoaOut();
        	}
        	
        	Map<String, String> qnaData = sf.getListMap().get(0);
        	String qnaStatus = MapUtils.getString(qnaData, "STATUS", "");
        	String managerId = MapUtils.getString(qnaData, "MANAGER", "");
        	
        	if (!"G".equals(qnaStatus)) {
        		setStatus(0);
				setFlag(false);
				setMessage("검토진행중 건에 대해서만 확정할 수 있습니다.");
				return getSepoaOut();
        	}
        	if (managerId == null || !sessionId.equals(managerId)) {
        		setStatus(0);
				setFlag(false);
				setMessage("담당자만 이견없음/검토요청을 선택할 수 있습니다.");
				return getSepoaOut();
        	}
        	
        	headerData.put("STATUS", finalizeStatus);
        	sxp = new SepoaXmlParser(this, "doFinalizeReviewUpdate");
        	ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
        	int updated = ssm.doUpdate(headerData);
        	if (updated <= 0) {
        		setStatus(0);
				setFlag(false);
				setMessage("상태 확정에 실패하였습니다. 이미 처리되었는지 확인해주세요.");
				Rollback();
				return getSepoaOut();
        	}
        	
        	// 법무팀장(QnA_MASTER) 메일 — 실패해도 확정은 유지
        	try {
        		Map<String, Object> mailAllData = new HashMap<String, Object>();
        		Map<String, String> mailHeader = new HashMap<String, String>();
        		mailHeader.put("QNA_NUMBER", qnaNumber);
        		mailHeader.put("STATUS", finalizeStatus);
        		mailAllData.put(SepoaDataMapper.KEY_RAW_HEADER_DATA, mailHeader);
        		mailAllData.put(SepoaDataMapper.KEY_HEADER_DATA, mailHeader);
        		SepoaOut mailOut = ServiceConnector.doService(info, "sepoa.svc.cbo.qna.postprocess.SepoaQnaPostProcessor", ctx, "doFinalizeMasterProcess", new Object[] { mailAllData });
        		if (mailOut == null || mailOut.status != 1 || !mailOut.flag) {
        			Logger.err.println(sessionId, this, "QnA finalize master mail failed. QNA_NUMBER=" + qnaNumber
        					+ (mailOut != null ? (", msg=" + mailOut.message) : ""));
        		}
        	} catch (Exception mailEx) {
        		Logger.err.println(sessionId, this, "QnA finalize master mail exception. QNA_NUMBER=" + qnaNumber);
        		Logger.err.println(sessionId, this, mailEx);
        	}
        	
        	Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
    
    @Description("ST: 계약서 법무검토 → 날인신청 연동(임시저장)")
	@SuppressWarnings("unchecked")
	public SepoaOut doSealLink(Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        ConnectionContext ctx = getConnectionContext();
        
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			if (headerData == null) {
				headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			}
			Map<String, String> rawHeaderData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
			
			String qnaNumber = MapUtils.getString(headerData, "QNA_NUMBER", "");
			String subject = MapUtils.getString(headerData, "SUBJECT", "");
			String explanation = MapUtils.getString(headerData, "EXPLANATION", "");
			String submissionPlace = MapUtils.getString(headerData, "SUBMISSION_PLACE", "");
			String selectSealJson = MapUtils.getString(headerData, "SELECT_SEAL_JSON", "");
			String manageNumbers = MapUtils.getString(headerData, "MANAGE_NUMBERS", "");
			String reFlag = MapUtils.getString(headerData, "RE_FLAG", "");
			String reDetail = MapUtils.getString(headerData, "RE_DETAIL", "");
			if (StringUtils.isBlank(selectSealJson) && rawHeaderData != null) {
				selectSealJson = MapUtils.getString(rawHeaderData, "SELECT_SEAL_JSON",
						MapUtils.getString(rawHeaderData, "unescaped_SELECT_SEAL_JSON", ""));
			}
			if (StringUtils.isBlank(manageNumbers) && rawHeaderData != null) {
				manageNumbers = MapUtils.getString(rawHeaderData, "MANAGE_NUMBERS",
						MapUtils.getString(rawHeaderData, "unescaped_MANAGE_NUMBERS", ""));
			}
			if (StringUtils.isBlank(reFlag) && rawHeaderData != null) {
				reFlag = MapUtils.getString(rawHeaderData, "RE_FLAG", "");
			}
			if (StringUtils.isBlank(reDetail) && rawHeaderData != null) {
				reDetail = MapUtils.getString(rawHeaderData, "RE_DETAIL", "");
			}
			// 전자(E) / 실물(R) — 미입력·잘못된 값이면 전자 기본
			if (!"E".equalsIgnoreCase(reFlag) && !"R".equalsIgnoreCase(reFlag)) {
				reFlag = "E";
			} else {
				reFlag = reFlag.toUpperCase();
			}
			if (StringUtils.isBlank(reDetail)) {
				reDetail = reFlag;
			} else {
				reDetail = reDetail.toUpperCase();
			}
			String sessionId = info.getSession("ID");
			
			if (StringUtils.isBlank(qnaNumber)) {
				setStatus(0); setFlag(false); setMessage("문서번호가 없습니다.");
				return getSepoaOut();
			}
			if (StringUtils.isBlank(subject) || StringUtils.isBlank(explanation) || StringUtils.isBlank(submissionPlace)) {
				setStatus(0); setFlag(false); setMessage("신청명, 설명, 제출처를 모두 입력해주세요.");
				return getSepoaOut();
			}
			
			List<Map<String, String>> selectSealList = new ArrayList<Map<String, String>>();
			// 1순위: MANAGE_NUMBERS (콤마구분)
			if (StringUtils.isNotBlank(manageNumbers)) {
				String[] manageNumberArr = manageNumbers.split(",");
				for (String manageNumber : manageNumberArr) {
					if (StringUtils.isBlank(manageNumber)) {
						continue;
					}
					Map<String, String> sealRow = new HashMap<String, String>();
					sealRow.put("MANAGE_NUMBER", manageNumber.trim());
					selectSealList.add(sealRow);
				}
			}
			// 2순위: SELECT_SEAL_JSON
			if (selectSealList.isEmpty() && StringUtils.isNotBlank(selectSealJson)) {
				try {
					Object parsed = new Gson().fromJson(selectSealJson, Object.class);
					if (parsed instanceof List) {
						List<?> rawSealList = (List<?>) parsed;
						for (Object rawItem : rawSealList) {
							if (!(rawItem instanceof Map)) {
								continue;
							}
							Map<?, ?> rawMap = (Map<?, ?>) rawItem;
							Map<String, String> sealRow = new HashMap<String, String>();
							for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
								if (entry.getKey() == null) {
									continue;
								}
								Object value = entry.getValue();
								sealRow.put(String.valueOf(entry.getKey()), value == null ? "" : String.valueOf(value));
							}
							if (StringUtils.isNotBlank(MapUtils.getString(sealRow, "MANAGE_NUMBER", ""))) {
								selectSealList.add(sealRow);
							}
						}
					} else if (parsed instanceof Map) {
						Map<?, ?> rawMap = (Map<?, ?>) parsed;
						for (Object rawItem : rawMap.values()) {
							if (!(rawItem instanceof Map)) {
								continue;
							}
							Map<?, ?> sealMap = (Map<?, ?>) rawItem;
							Map<String, String> sealRow = new HashMap<String, String>();
							for (Map.Entry<?, ?> entry : sealMap.entrySet()) {
								if (entry.getKey() == null) {
									continue;
								}
								Object value = entry.getValue();
								sealRow.put(String.valueOf(entry.getKey()), value == null ? "" : String.valueOf(value));
							}
							if (StringUtils.isNotBlank(MapUtils.getString(sealRow, "MANAGE_NUMBER", ""))) {
								selectSealList.add(sealRow);
							}
						}
					}
				} catch (Exception parseEx) {
					Logger.err.println(sessionId, this, "doSealLink SELECT_SEAL_JSON parse fail: " + parseEx.getMessage());
					selectSealList = new ArrayList<Map<String, String>>();
				}
			}
			if (selectSealList == null || selectSealList.isEmpty()) {
				setStatus(0); setFlag(false); setMessage("인장을 선택해주세요.");
				Logger.err.println(sessionId, this, "doSealLink seal empty. MANAGE_NUMBERS=" + manageNumbers
						+ ", SELECT_SEAL_JSON_LEN=" + (selectSealJson == null ? -1 : selectSealJson.length()));
				return getSepoaOut();
			}
			// 계약서 법무검토만 (LQNA)
			if (!qnaNumber.startsWith("LQNA")) {
				setStatus(0); setFlag(false); setMessage("계약서 법무검토 건만 날인연동할 수 있습니다.");
				return getSepoaOut();
			}
			
			sxp = new SepoaXmlParser(this, "doSelectQnaForSealLink");
        	ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
        	sf = new SepoaFormater(ssm.doSelect(headerData));
        	if (sf.getRowCount() <= 0) {
        		setStatus(0); setFlag(false); setMessage("법무검토 문서를 찾을 수 없습니다.");
				return getSepoaOut();
        	}
        	
        	Map<String, String> qnaData = sf.getListMap().get(0);
        	String qnaStatus = MapUtils.getString(qnaData, "STATUS", "");
        	String checkFlag = MapUtils.getString(qnaData, "CHECK_FLAG", "N");
        	String writerId = MapUtils.getString(qnaData, "ADD_USER_ID", "");
        	
        	if (!sessionId.equals(writerId)) {
        		setStatus(0); setFlag(false); setMessage("기안자만 날인연동할 수 있습니다.");
				return getSepoaOut();
        	}
        	// 팀장 완료확인 후 검토완료(CC)만 날인연동
        	if (!"CC".equals(qnaStatus)) {
        		setStatus(0); setFlag(false); setMessage("검토완료(팀장 확정) 건만 날인연동할 수 있습니다.");
				return getSepoaOut();
        	}
        	if (!"Y".equals(checkFlag)) {
        		setStatus(0); setFlag(false); setMessage("완료여부가 Y인 건만 날인연동할 수 있습니다.");
				return getSepoaOut();
        	}
        	
        	// 중복 연동 체크
        	Map<String, String> sourceParam = new HashMap<String, String>();
        	sourceParam.put("SOURCE_NO", qnaNumber);
        	sxp = new SepoaXmlParser(this, "doSelectSealBySourceNo");
        	ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
        	sf = new SepoaFormater(ssm.doSelect(sourceParam));
        	if (sf.getRowCount() > 0) {
        		String existSmr = MapUtils.getString(sf.getListMap().get(0), "SMR_NUMBER", "");
        		setStatus(0); setFlag(false);
        		setMessage("이미 날인연동된 문서입니다.<br/>신청번호: " + existSmr);
				return getSepoaOut();
        	}
        	
        	// 첨부 키: 댓글 첨부 우선(검토요청 경로), 없으면 원본(이견없음 경로)
        	String attachNo = null;
        	boolean usedReplyAttach = false;
        	sxp = new SepoaXmlParser(this, "doSelectLastReplyAttach");
        	ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
        	sf = new SepoaFormater(ssm.doSelect(headerData));
        	if (sf.getRowCount() > 0) {
        		attachNo = MapUtils.getString(sf.getListMap().get(0), "ATTACH_NO", "");
        		if (StringUtils.isNotBlank(attachNo) && !"0".equals(attachNo)) {
        			usedReplyAttach = true;
        		} else {
        			attachNo = null;
        		}
        	}
        	if (StringUtils.isBlank(attachNo)) {
        		attachNo = MapUtils.getString(qnaData, "ATTACH_NO", "");
        		if (StringUtils.isBlank(attachNo) || "0".equals(attachNo)) {
        			setStatus(0); setFlag(false); setMessage("연동할 첨부파일이 없습니다.");
    				return getSepoaOut();
        		}
        	}
        	
        	Map<String, String> fileParam = new HashMap<String, String>();
        	fileParam.put("ATTACH_NO", attachNo);
        	sxp = new SepoaXmlParser(this, "doSelectAttachFiles");
        	ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
        	sf = new SepoaFormater(ssm.doSelect(fileParam));
        	if (sf.getRowCount() <= 0) {
        		if (usedReplyAttach) {
        			// 댓글 첨부번호는 있으나 파일 없음 → 원본 재시도
        			attachNo = MapUtils.getString(qnaData, "ATTACH_NO", "");
        			if (StringUtils.isBlank(attachNo) || "0".equals(attachNo)) {
        				setStatus(0); setFlag(false); setMessage("연동할 첨부파일이 없습니다.");
        				return getSepoaOut();
        			}
        			fileParam.put("ATTACH_NO", attachNo);
        			sxp = new SepoaXmlParser(this, "doSelectAttachFiles");
        			ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
        			sf = new SepoaFormater(ssm.doSelect(fileParam));
        			if (sf.getRowCount() <= 0) {
        				setStatus(0); setFlag(false); setMessage("연동할 첨부파일이 없습니다.");
        				return getSepoaOut();
        			}
        		} else {
        			setStatus(0); setFlag(false); setMessage("연동할 첨부파일이 없습니다.");
    				return getSepoaOut();
        		}
        	}
        	List<Map<String, String>> sourceFiles = sf.getListMap();
        	
        	Configuration conf = new Configuration();
        	String smrNo = DocumentUtil.getDocNumber(info, "SMR").result[0];
        	String smdNo = DocumentUtil.getDocNumber(info, "SMD").result[0];
        	
        	SepoaReqSealManager srm = new SepoaReqSealManager(info, conf, ctx);
        	SepoaSealManagementManager smm = new SepoaSealManagementManager(info, conf, ctx);
        	
        	// 선택 인장 중 법인(C)이 하나라도 있으면 법인인감신청(C), 아니면 사용인감신청(U)
        	String applyType = "U";
        	for (Map<String, String> peekRow : selectSealList) {
        		if (peekRow == null) {
        			continue;
        		}
        		String peekManageNumber = MapUtils.getString(peekRow, "MANAGE_NUMBER", "");
        		if (StringUtils.isBlank(peekManageNumber)) {
        			continue;
        		}
        		Map<String, String> peekSeal = smm.doSelectSealData(peekManageNumber);
        		if (peekSeal != null && "C".equals(MapUtils.getString(peekSeal, "SEAL_TYPE", ""))) {
        			applyType = "C";
        			break;
        		}
        	}
        	
        	Map<String, String> reqSealInfo = new HashMap<String, String>();
        	reqSealInfo.put("SMR_NUMBER", smrNo);
        	reqSealInfo.put("SMR_STATUS", "T");
        	reqSealInfo.put("APPLY_TYPE", applyType);
        	reqSealInfo.put("SUBJECT", subject);
        	reqSealInfo.put("EXPLANATION", explanation);
        	reqSealInfo.put("SUBMISSION_PLACE", submissionPlace);
        	reqSealInfo.put("REQ_SEAL_USE_TYPE", "101");
        	reqSealInfo.put("SOURCE_NO", qnaNumber);
        	reqSealInfo.put("RE_FLAG", reFlag);
        	reqSealInfo.put("RE_DETAIL", reDetail);
        	
        	srm.setDocBaiscInfo(reqSealInfo);
        	if (srm.doCreateReqSeal() == null) {
        		setStatus(0); setFlag(false);
        		setMessage("날인신청 생성에 실패하였습니다." + (srm.getLastErrCode() != 0 ? (" (" + srm.getLastErrCode() + ")") : ""));
        		Rollback();
				return getSepoaOut();
        	}
        	
        	// SSIAD 구분 보정 (전자 E / 실물 R)
        	Map<String, String> reFlagFix = new HashMap<String, String>();
        	reFlagFix.put("SMR_NUMBER", smrNo);
        	reFlagFix.put("RE_FLAG", reFlag);
        	reFlagFix.put("RE_DETAIL", reDetail);
        	if (!srm.doUpdateReqSealDoc(reFlagFix)) {
        		String reFlagLabel = "R".equals(reFlag) ? "실물" : "전자";
        		setStatus(0); setFlag(false);
        		setMessage("날인신청 구분(" + reFlagLabel + ") 저장에 실패하였습니다." + (srm.getLastErrCode() != 0 ? (" (" + srm.getLastErrCode() + ")") : ""));
        		Rollback();
				return getSepoaOut();
        	}
        	
        	// 선택 인장 마스터 보강 (SEAL_MG_DEPT / SEAL_MG_USER_ID)
        	List<Map<String, String>> enrichedSealList = new ArrayList<Map<String, String>>();
        	String sealMgDeptForDoc = "";
        	Set<String> sealTypeSet = new LinkedHashSet<String>();
        	for (Map<String, String> selectedSeal : selectSealList) {
        		if (selectedSeal == null) {
        			continue;
        		}
        		String manageNumber = MapUtils.getString(selectedSeal, "MANAGE_NUMBER", "");
        		if (StringUtils.isBlank(manageNumber)) {
        			setStatus(0); setFlag(false); setMessage("선택한 인장 정보가 올바르지 않습니다.");
        			Rollback();
    				return getSepoaOut();
        		}
        		
        		Map<String, String> sealDocSeal = new HashMap<String, String>();
        		sealDocSeal.putAll(selectedSeal);
        		
        		Map<String, String> sealDetail = smm.doSelectSealData(manageNumber);
        		if (sealDetail == null || sealDetail.isEmpty()) {
        			setStatus(0); setFlag(false);
        			setMessage("선택한 인장 마스터 정보를 찾을 수 없습니다.<br/>" + manageNumber);
        			Rollback();
    				return getSepoaOut();
        		}
        		sealDocSeal.putAll(sealDetail);
        		
        		String sealMgUserId = MapUtils.getString(sealDocSeal, "SEAL_MG_USER_ID",
        				MapUtils.getString(sealDocSeal, "MANAGER_ID", ""));
        		String sealMgDept = MapUtils.getString(sealDocSeal, "SEAL_MG_DEPT",
        				MapUtils.getString(sealDocSeal, "MANAGE_DEPT", ""));
        		if (StringUtils.isBlank(sealMgDeptForDoc) && StringUtils.isNotBlank(sealMgDept)) {
        			sealMgDeptForDoc = sealMgDept;
        		}
        		String sealTypeFromMaster = MapUtils.getString(sealDetail, "SEAL_TYPE", "");
        		if (StringUtils.isNotBlank(sealTypeFromMaster)) {
        			sealTypeSet.add(sealTypeFromMaster);
        		}
        		
        		sealDocSeal.put("SMR_NUMBER", smrNo);
        		sealDocSeal.put("SMD_NUMBER", smdNo);
        		sealDocSeal.put("MANAGE_NUMBER", manageNumber);
        		sealDocSeal.put("SEAL_MG_USER_ID", sealMgUserId);
        		sealDocSeal.put("SEAL_MG_DEPT", sealMgDept);
        		sealDocSeal.put("MANAGER_ID", sealMgUserId);
        		sealDocSeal.put("MANAGE_DEPT", sealMgDept);
        		sealDocSeal.put("ACCEPT_FLAG", MapUtils.getString(selectedSeal, "ACCEPT_FLAG", "N"));
        		sealDocSeal.put("DEC_FLAG", MapUtils.getString(selectedSeal, "DEC_FLAG", "N"));
        		enrichedSealList.add(sealDocSeal);
        	}
        	if (enrichedSealList.isEmpty()) {
        		setStatus(0); setFlag(false); setMessage("선택한 인장 정보가 올바르지 않습니다.");
        		Rollback();
				return getSepoaOut();
        	}
        	
        	String docSubject = MapUtils.getString(sourceFiles.get(0), "SRC_FILE_NAME", subject);
        	String sealTypeValue;
        	if (sealTypeSet.isEmpty()) {
        		sealTypeValue = "C".equals(applyType) ? "C" : "U,E";
        	} else {
        		StringBuilder sealTypeSb = new StringBuilder();
        		for (String t : sealTypeSet) {
        			if (sealTypeSb.length() > 0) {
        				sealTypeSb.append(",");
        			}
        			sealTypeSb.append(t);
        		}
        		sealTypeValue = sealTypeSb.toString();
        	}
        	
        	Map<String, String> sealDoc = new HashMap<String, String>();
        	sealDoc.put("SMR_NUMBER", smrNo);
        	sealDoc.put("SMD_NUMBER", smdNo);
        	sealDoc.put("SUBJECT", docSubject);
        	sealDoc.put("SEAL_TYPE", sealTypeValue);
        	sealDoc.put("SEAL_MG_DEPT", sealMgDeptForDoc);
        	sealDoc.put("RE_DETAIL", reDetail);
        	sealDoc.put("COUNT", "1");
        	
        	srm.setDocBaiscInfo(sealDoc);
        	for (Map<String, String> srcFile : sourceFiles) {
        		String type = MapUtils.getString(srcFile, "TYPE", "");
        		String desFileName = MapUtils.getString(srcFile, "DES_FILE_NAME", "");
        		String srcFileName = MapUtils.getString(srcFile, "SRC_FILE_NAME", "");
        		if (StringUtils.isBlank(desFileName)) {
        			continue;
        		}
        		
        		String typePath = SepoaMultipart.getTypePath(type, "upload");
        		File src = new File(typePath + "/" + desFileName);
        		if (!src.exists() || !src.isFile()) {
        			setStatus(0); setFlag(false);
        			setMessage("첨부파일 원본을 찾을 수 없습니다.<br/>" + srcFileName);
        			Rollback();
    				return getSepoaOut();
        		}
        		
        		String copyPath = src.getAbsolutePath() + "_" + System.nanoTime() + "_" + System.currentTimeMillis();
        		FileUtils.copyFile(src, new File(copyPath));
        		
        		Map<String, String> uploadFileData = new HashMap<String, String>();
        		uploadFileData.put("PATH", copyPath);
        		uploadFileData.put("FILE_NAME_CLIENT", srcFileName);
        		uploadFileData.put("FILE_NAME_SERVER", src.getName());
        		
        		if (srm.doUploadDocumentFile("REQ_SEAL", uploadFileData, sealDoc) == null) {
        			setStatus(0); setFlag(false);
        			setMessage("날인서류 파일 연동에 실패하였습니다." + (srm.getLastErrCode() != 0 ? (" (" + srm.getLastErrCode() + ")") : ""));
        			Rollback();
    				return getSepoaOut();
        		}
        	}
        	
        	if (srm.doSaveSealDoc(sealDoc) == null) {
        		setStatus(0); setFlag(false);
        		setMessage("날인서류 저장에 실패하였습니다." + (srm.getLastErrCode() != 0 ? (" (" + srm.getLastErrCode() + ")") : ""));
        		Rollback();
				return getSepoaOut();
        	}
        	
        	for (Map<String, String> sealDocSeal : enrichedSealList) {
        		if (srm.doSaveSealDocSeal(sealDocSeal) == null) {
        			setStatus(0); setFlag(false);
        			setMessage("인장 정보 저장에 실패하였습니다." + (srm.getLastErrCode() != 0 ? (" (" + srm.getLastErrCode() + ")") : ""));
        			Rollback();
    				return getSepoaOut();
        		}
        	}
        	
        	Map<String, String> linkParams = new HashMap<String, String>();
        	linkParams.put("SMR_NUMBER", smrNo);
        	String accessLink = srm.doCreateReqSealAccessLink("USE.REQ.SEAL", smrNo, linkParams, null, "ALL");
        	if (accessLink == null || accessLink.isEmpty()) {
        		Logger.err.println(sessionId, this, "QnA seal link accessLink create failed. SMR_NUMBER=" + smrNo
        				+ ", err=" + srm.getLastErrCode());
        		// 링크 실패해도 임시저장 본문은 유지
        	}
        	
        	Commit();
        	setValue(smrNo);
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
    
    @Description("수정가능여부 검토")
	@SuppressWarnings("unchecked")
	public SepoaOut checkModify(Map<String, Object> allData) {
    	SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        ConnectionContext ctx = getConnectionContext();
        
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			//담당자 지정
			sxp = new SepoaXmlParser(this, "getQnaDetail");
        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
        	sf = new SepoaFormater(ssm.doSelect(headerData));
        	
			if(sf.getRowCount() > 0) {
				Map<String, String> qnaData = sf.getListMap().get(0);
				String qnaStatus = qnaData.get("STATUS");
				System.out.println(qnaStatus);
				if("T".equalsIgnoreCase(qnaStatus)) {
					setValue("1");
				} else {
					setValue("2");
				}
			} else {
				setValue("3");
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
    
    private List<String> getSplitString(String t1, int length) {
        List<String> rt = new LinkedList<String>();
        String ui = t1;

        int le = getLengthb(ui);
        
        while (le > 0 && t1 != null) {
            rt.add(getSubString(ui, 0, length));
            ui = getSubString(ui, length, le);
            le = getLengthb(ui);
        }

        return rt;
    }
    
    private int getLengthb(String str) {
        int rSize = 0;
        int len = 0;

        for (; rSize < str.length(); rSize++) {
            if (str.charAt(rSize) > 127) {
                len += 2;
            } else {
                len++;
            }
        }

        return len;
    }

    private String getSubString(String str, int start, int end) {
        int rSize = 0;
        int len = 0;

        StringBuffer ss = new StringBuffer();

        for (; rSize < str.length(); rSize++) {
            if (str.charAt(rSize) > 0x007F) {
                len += 2;
            } else {
                len++;
            }
            if ((len > start) && (len <= end)) {
                ss.append(str.charAt(rSize));
            }
        }

        return ss.toString();
    }
}