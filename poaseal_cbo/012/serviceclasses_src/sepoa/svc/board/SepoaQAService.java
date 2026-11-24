package sepoa.svc.board;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;

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
    @Description("Q&A 체크여부 업데이트")
    public SepoaOut doCheckFlagUpdate(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            String checkFalg = headerData.get("CHECK_FLAG");
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doUpdate(headerData));
            
            String qnaStatus = "";
            if("Y".equals(checkFalg)) {
            	qnaStatus = "A";
            } else {
            	qnaStatus = "G";
            }
            
            //상태 변경
			headerData.put("STATUS", qnaStatus);
			sxp = new SepoaXmlParser(this, "doUpdateDocStatus");
        	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
        	ssm.doUpdate(headerData);
            
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
                
                //QNA 작성 후처리 모듈
                if(SepoaModuleService.checkBusiness(new Configuration(), info, "DSB1684477026257")) {
                	SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.qna.postprocess.SepoaQnaPostProcessor", ctx, "doWriteProcess", new Object[] {allData});
					if(out == null) {
						setStatus(70009);
						Logger.err.println("QnA PostProcess Result Null!");
					} else if(out.status != 1 || !out.flag) {
						setStatus(out.status);
						Logger.err.println("QnA PostProcess Fail!");
					}
                }
                
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