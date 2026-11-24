package sepoa.sealmk2;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.google.gson.Gson;

import sepoa.contract.util.SepoaImageProcessor;
import sepoa.filemanager.FileManager.LEVEL;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.ux.sUtils;

public class SepoaReqSealManager extends sepoa.seal.SepoaReqSealManager {
	private int docPageCount;
	
	public SepoaReqSealManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
	}
	
	// 이미지 리스트조회시 파일리스트 출력하도록 수정
	@Override
	public List<Map<String, String>> doListImage(String layer){
		if("".equals(layer) || layer == null) {
			layer = this.docBasicInfo.get("LAYER");
		}
		List<Map<String, String>> result = new LinkedList<Map<String, String>>();
		this.initFileManager(this.docBasicInfo.get("KEY"));
		
		List<Map<String, String>> layerFile = this.fileManager.doList(new SingletonMap<String, String>("LAYER", layer), LEVEL.FILE);
		if(layerFile != null) {
			result.addAll(layerFile);
		}
		if (result.size() == 0) {
			this.errCode = 102321;
			return null;
		} 
		return result;
	}
	
	// 이미지 조회대신 PDF 파일 조회하도록 수정
	@Override
	public Map<String, String> doImage(String fileID, String partID, boolean thumb, boolean noImageFlag){
		if(!this.initFileManager(this.docBasicInfo.get("KEY"))) {
			this.errCode = 100001;
			return null;
		}
		
		if(fileID.isEmpty()) {
			this.errCode = 100004;
			return null;
		}else {
			List<String> condition = new ArrayList<String>();
			condition.add(fileID);
			
			Map<String, String> result = this.fileManager.doDownload(condition,true);
			if(result == null) {
				Logger.err.println(info.getSession("ID"), this, "not found image : "+this.fileManager.getLastErrCode());
				this.errCode = 102321;
				return null;
			}
			return result;
		}
	}
	
	@Override
	public boolean doUpdateEditorHTML(List<Map<String, String>> editorHtmls) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("KEY"))) {
			this.errCode = 100001;
			return false;
		}
		
		/* FIX_HTML 통합 데이터 FI의 META 데이터 저장  */
		String fileId = MapUtils.getString(editorHtmls.get(0), "FILE_ID");
		String layer = MapUtils.getString(editorHtmls.get(0), "LAYER");
		Map<String, String> mergeFixHtml = new HashMap<>();
		for(Map<String, String> page : editorHtmls) {
			if(!"".equalsIgnoreCase(page.get("PART_ID")) && !"".equalsIgnoreCase(page.get("FIX_HTML"))) {
				mergeFixHtml.put(MapUtils.getString(page, "PART_ID"), MapUtils.getString(page, "FIX_HTML"));
			}
		}
		
		Map<String, String> meta = new HashMap<>();
		meta.put("FILE_ID", fileId);
		meta.put("LAYER", layer);
		meta.put("FIX_HTML", new Gson().toJson(mergeFixHtml));
		
		this.fileManager.doModifyMeta(meta);
		int errCode = this.fileManager.getLastErrCode();
		if(errCode != 0) {
			this.errCode = 103325;
			return false;
		}
		
		return true;
	}
	
	//날인서류 파일 조회
	public Map<String, String> doSelectDocumentFile() {
		if (this.fileManager != null) {
			this.destoryFileManager();
		}
		
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		//날인서류 업로드 파일리스트 조회
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL"), LEVEL.FILE);
		if(documentList.size() < 1) {
			this.errCode = 103326;
			return null;
		}
		
		//패스 매니저 생성
		PathManager pm = new PathManager(info, config);
		pm.setLayer("REQ_SEAL");
		
		Map<String, String> sealData = new HashMap<>();
		sealData.putAll(documentList.get(0));
		
		//ActionControllor에서 파일을 내보낼 때 사용할려고 설정함.
		sealData.put("FILE_PATH", pm.getLayerPath() + documentList.get(0).get("FILE_NAME_SERVER"));
		
		return sealData;
	}
	
	public boolean doConvertCertDocument() {
		//SSISD 조회해서 교부서류 리스트 조회
		List<Map<String, String>> upDocList = this.doSelectSSISD(this.docBasicInfo);
		if(upDocList == null || upDocList.size() == 0) {
			this.errCode = 102008;
			return false;
		}
		
		for(Map<String, String> elecDoc : upDocList) {
			//실물교부서류 스킵
			if("R".equalsIgnoreCase(elecDoc.get("REAL_DOC_CHK"))) continue;
			elecDoc.put("APPLY_TYPE", "D");
			
			//파일 매니저 초기화
			if(this.fileManager != null) this.destoryFileManager();
			if(!this.initFileManager(elecDoc.get("SMR_NUMBER") + "_" + elecDoc.get("SD_NUMBER"))) {
				this.errCode = 100001;
				return false;
			}
			List<Map<String, String>> layerFile = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "ATTACH_DOC_TEMPLATE"), LEVEL.FILE);
			if(layerFile != null) {
				this.fileManager.doDelete();
			}
			
			//교부서류 원본에서 복제
			this.fileManager.doCopy(elecDoc.get("SMI_NUMBER"), "");
			layerFile = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "ATTACH_DOC_TEMPLATE"), LEVEL.FILE);
			if (layerFile == null) {
				this.errCode = 103333;
				return false;
			}
			
			String fixedMsg = "";
			
			// 발행번호 저장
			String publishNo = UUID.randomUUID().toString()+System.nanoTime();
			elecDoc.put("PUBLISH_NO", publishNo);
			if (doUpdateCertDocPublishNo(elecDoc) == 0) {
				this.errCode = 100000;
				return false;
			}
			
			// 발행번호
			if (SepoaModuleService.checkBusiness(config, info, "SID1671440510341")){
				String publishText = Cache.text.get("FOOTER_TEXT.PUB_NO") + " : " + publishNo;
				String bottomPx = CommonUtil.getConfig("seal.document.publish.bottom.px", "0");
				String publishNoHtml = "<div class=\"footer\" style=\"position:absolute;bottom:"+bottomPx+"px;width:100%;font-size:13px;z-index:100;display:flex;justify-content:left;text-align:left;\"><div style=\"padding:10px;color:#999;\">"+publishText+"</div></div>";
				fixedMsg += publishNoHtml;
			}
			
			//FIX_HTML파싱
			Map<String, String> fixHtmlMap = new Gson().fromJson(MapUtils.getString(layerFile.get(0), "FIX_HTML", ""), Map.class);
			if(fixHtmlMap == null) {
				fixHtmlMap = new HashMap<>();
			}
			
			//간인 등의 옵션 전용 css 초기화
			Map<String, String> optionMap = parseOptionReqSeal(layerFile);
			Map<String, String> customCssMap = new HashMap<String, String>();
			customCssMap.put("CONVERT_CERT_ORG", "<style>.contract-item-box-source-symmetry .contract-item-prop-symmetry_page_copy-ON{display: none;}.contract-item-box-source-copy .contract-item-prop-symmetry_page_copy-ON{display:block;}.contract-item-box-source-org .contract-item-prop-symmetry_page_copy-ON{display:block;}</style>");
			customCssMap.put("CONVERT_CERT_SYM", "<style>.contract-item-box-source-symmetry .contract-item-prop-symmetry_page_copy-ON{display: block;}.contract-item-box-source-copy .contract-item-prop-symmetry_page_copy-ON{display:none;}.contract-item-box-source-org .contract-item-prop-symmetry_page_copy-ON{display:none;}</style>");
			customCssMap.put("CONVERT_CERT_MERGE", "");
			
			//간인 여부 확인
			String[] prcLayers = this.convertCertLayers;
			if (!"ON".equalsIgnoreCase(optionMap.get("PAGE_COPY"))) {
				String[] notSymLayer = { "CONVERT_CERT_ORG" };
				prcLayers = notSymLayer;
			}
			
			//레이어 단위로 루프 시작
			for (String layer : prcLayers) {
				SepoaImageProcessor sip = new SepoaImageProcessor(info, config, ctx);
				sip.setTempDirectory(this.tempStorage);
				sip.setPDF(layerFile.get(0), true);
				sip.setCustomCss(customCssMap.get(layer));
				sip.setFixedMsg(fixedMsg);
				
				int pdfTotalPage = sip.getPDFDoc().getNumberOfPages();
				for(int i = 1; i <= pdfTotalPage; i++) {
					String pagTotalNo = i + "/" + pdfTotalPage;
					String bottomPx = CommonUtil.getConfig("seal.document.publish.bottom.px", "0");
					String publishTotalNoHtml = "<div class=\"footer\" style=\"position:absolute;bottom:"+bottomPx+"px;width:100%;font-size:13px;z-index:100;display:flex;justify-content:right;text-align:center;\"><div style=\"padding:10px;color:#999;\">"+pagTotalNo+"</div></div>";
					String key = sUtils.string.lpad(String.valueOf(i), 3, "00");
					
					if (SepoaModuleService.checkBusiness(config, info, "SID1671440510341")){
						if(fixHtmlMap.containsKey(key)) {
							String legacyValue = fixHtmlMap.get(key);
							String newValue = legacyValue + System.lineSeparator() + publishTotalNoHtml;
							fixHtmlMap.put(key, newValue);
						} else {
							fixHtmlMap.put(key, publishTotalNoHtml);
						}
					} 
				}
				
				for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
					String fixHtml = entry.getValue();
					try {
						String repSealHtml = parseAttachSeal(fixHtml, elecDoc, optionMap);
						if(repSealHtml == null) {
							this.errCode = this.getLastErrCode();
							return false;
						}
						
						fixHtmlMap.put(entry.getKey(), repSealHtml);
					} catch (Exception e) {
						this.errCode = this.getLastErrCode();
						Logger.err.println(this.info.getSession("ID"), this, e);
						
						return false;
					}
				}
				
				File resultPDF = new File(this.tempStorage + "/" + System.nanoTime() + "_" + System.currentTimeMillis() + ".pdf"); 
				try {
					Map<Integer, String> castingFixHtmlMap = new HashMap<>();
					for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
						castingFixHtmlMap.put(Integer.parseInt(entry.getKey()) - 1, entry.getValue());							
					}
					
					File tmpPDF = sip.process(castingFixHtmlMap);
					FileUtils.moveFile(tmpPDF, resultPDF);
				} catch (Exception e) {
					this.errCode = 103345;
					Logger.err.println(this.info.getSession("ID"), this, e);
					
					return false;
				}
				
				//sfmfi에 업로드 진행
				Map<String, String> uploadFileData = new HashMap<String, String>();
				uploadFileData.put("PATH", resultPDF.getAbsolutePath());
				uploadFileData.put("FILE_NAME_CLIENT", layerFile.get(0).get("FILE_NAME_CLIENT"));
				uploadFileData.put("FILE_NAME_SERVER", resultPDF.getName());
				
				this.fileManager.setLayer(layer);
				try {
					if(this.fileManager.doUploadFile(uploadFileData, null, false) == null) {
						Logger.err.println("Document convert and upload fail : " + elecDoc.get("SMR_NUMBER"));
						this.errCode = this.fileManager.getLastErrCode();
						return false;
					}
				} catch(Exception e) {
					Logger.err.println("Document convert and upload fail : " + elecDoc.get("SMR_NUMBER"));
					this.errCode = this.fileManager.getLastErrCode();
					return false;
				}
			}
		}
		
		return true;
	}
	
	@Override
	public boolean doConvertReqSealDocument(String smrNo, String smdNo) {
		List<Map<String, String>> replaceDocList = new ArrayList<Map<String, String>>();
		
		//날인 서류 치환
		if(!"R".equals(this.docBasicInfo.get("RE_FLAG"))) {
			if ("".equals(smrNo) || smrNo == null || "".equals(smdNo) || smdNo == null) {
				List<Map<String, String>> upDocList = this.doSelectSSIPD(this.docBasicInfo);
				if (upDocList == null) {
					this.errCode = 103327;
					return false;
				}
				smrNo = this.docBasicInfo.get("SMR_NUMBER");
				
				for (Map<String, String> upDoc : upDocList) {
					Map<String, String> doc = new HashMap<String, String>();
					doc.put("SMR_NUMBER", smrNo);
					String smdNumber = upDoc.get("SMD_NUMBER");
					if (smdNumber.startsWith("NO_DOC")) continue;	// 날인대상문서를 사용하지 않음
					doc.put("SMD_NUMBER", smdNumber);

					replaceDocList.add(upDoc);
				}
			} else {
				Map<String, String> doc = new HashMap<String, String>();
				doc.put("SMR_NUMBER", smrNo);
				doc.put("SMD_NUMBER", smdNo);

				replaceDocList.add(doc);
			}

			for (Map<String, String> replaceDoc : replaceDocList) {
				if (this.fileManager != null) {
					this.destoryFileManager();
				}

				if (!this.initFileManager(replaceDoc.get("SMR_NUMBER") + "_" + replaceDoc.get("SMD_NUMBER"))) {
					this.errCode = 100001;
					return false;
				}
				
				String fixedMsg = "";
				// 발행번호 저장
				String publishNo = UUID.randomUUID().toString()+System.nanoTime();
				replaceDoc.put("PUBLISH_NO", publishNo);
				if (doUpdatePublishNo(replaceDoc) == 0) {
					this.errCode = 100000;
					return false;
				}
				String publishText = Cache.text.get("FOOTER_TEXT.PUB_NO") + " : " + publishNo;
				String bottomPx = CommonUtil.getConfig("seal.document.publish.bottom.px", "0");
				String publishNoHtml = "<div class=\"footer\" style=\"position:absolute;bottom:"+bottomPx+"px;width:100%;font-size:10px;z-index:100;display:flex;justify-content:left;text-align:left;\"><div style=\"padding:10px;color:#999;\">"+publishText+"</div></div>";
				
				//footer 추가 부분 - fixHtml이 없어도 모든 장에 들어가야 하므로 맨 앞으로 뺌
				String legalWatermark = MapUtils.getString(this.docBasicInfo, "LEGAL_WATERMARK", "").toUpperCase();
				if(StringUtils.isNotBlank(legalWatermark)) {
					String legalfooterText = "";
					if("K".equals(legalWatermark)) {
						legalfooterText = Cache.text.get("FOOTER_TEXT.LEGAL_TEXT_KO");
					}else if("E".equals(legalWatermark)) {
						legalfooterText = Cache.text.get("FOOTER_TEXT.LEGAL_TEXT_EN");
					}
					
					String footerContents = "<div class=\"footer\" style=\"position:absolute;bottom:0%;width:100%;font-size:10px;z-index:100;display:flex;justify-content:#ALIGN#;text-align:#ALIGN#;\"><div style=\"padding:10px;\">#TEXT#</div></div>";
					footerContents = footerContents.replace("#TEXT#", legalfooterText);
					footerContents = footerContents.replace("#ALIGN#", CommonUtil.getConfig("sepoa.footer.legal.align", "left").toLowerCase());
					
					fixedMsg += footerContents;
				}
				// 발행번호
				if (SepoaModuleService.checkBusiness(config, info, "SID1671440510341")){
					fixedMsg += publishNoHtml;
				}
				
				List<Map<String, String>> layerFile = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL"), LEVEL.FILE);
				if (layerFile == null) {
					this.errCode = 103333;
					return false;
				}
				
				Map<String, String> fixHtmlMap = new Gson().fromJson(MapUtils.getString(layerFile.get(0), "FIX_HTML", ""), Map.class);
				if(fixHtmlMap == null) {
					fixHtmlMap = new HashMap<>();
				}
				
				Map<String, String> optionMap = parseOptionReqSeal(layerFile);
				Map<String, String> customCssMap = new HashMap<String, String>();
				customCssMap.put("CONVERT_SEAL_ORG", "<style>.contract-item-box-source-symmetry .contract-item-prop-symmetry_page_copy-ON{display: none;}.contract-item-box-source-copy .contract-item-prop-symmetry_page_copy-ON{display:block;}.contract-item-box-source-org .contract-item-prop-symmetry_page_copy-ON{display:block;}</style>");
				customCssMap.put("CONVERT_SEAL_SYM", "<style>.contract-item-box-source-symmetry .contract-item-prop-symmetry_page_copy-ON{display: block;}.contract-item-box-source-copy .contract-item-prop-symmetry_page_copy-ON{display:none;}.contract-item-box-source-org .contract-item-prop-symmetry_page_copy-ON{display:none;}</style>");
				customCssMap.put("CONVERT_SEAL_MERGE", "");
				
				//간인 여부 확인
				String[] prcLayers = this.convertLayers;
				if (!"ON".equalsIgnoreCase(optionMap.get("PAGE_COPY"))) {
					String[] notSymLayer = { "CONVERT_SEAL_ORG" };
					prcLayers = notSymLayer;
				}

				//레이어 단위로 루프 시작
				for (String layer : prcLayers) {
					SepoaImageProcessor sip = new SepoaImageProcessor(info, config, ctx);
					sip.setTempDirectory(this.tempStorage);
					sip.setPDF(layerFile.get(0), true);
					sip.setCustomCss(customCssMap.get(layer));
					sip.setFixedMsg(fixedMsg);
					
					int pdfTotalPage = sip.getPDFDoc().getNumberOfPages();
					replaceDoc.put("pdfTotalPage", Integer.toString(pdfTotalPage));
					
					for(int i = 1; i <= pdfTotalPage; i++) {
						String pagTotalNo = i + "/" + pdfTotalPage;
						String publishTotalNoHtml = "<div class=\"footer\" style=\"position:absolute;bottom:"+bottomPx+"px;width:100%;font-size:10px;z-index:100;display:flex;justify-content:right;text-align:center;\"><div style=\"padding:10px;color:#999;\">"+pagTotalNo+"</div></div>";
						String key = sUtils.string.lpad(String.valueOf(i), 3, "00");
						
						if (SepoaModuleService.checkBusiness(config, info, "SID1671440510341")){
							if(fixHtmlMap.containsKey(key)) {
								String legacyValue = fixHtmlMap.get(key);
								String newValue = legacyValue + System.lineSeparator() + publishTotalNoHtml;
								fixHtmlMap.put(key, newValue);
							} else {
								fixHtmlMap.put(key, publishTotalNoHtml);
							}
						} 
					}
					int cnt = 0;
					for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
						cnt++;
						replaceDoc.put("pageCnt", Integer.toString(cnt));
						String fixHtml = entry.getValue();
						try {
							String repSealHtml = parseAttachSeal(fixHtml, replaceDoc,optionMap);
							if(repSealHtml == null) {
								this.errCode = this.getLastErrCode();
								return false;
							}
							
							fixHtmlMap.put(entry.getKey(), repSealHtml);
						} catch (Exception e) {
							this.errCode = this.getLastErrCode();
							Logger.err.println(this.info.getSession("ID"), this, e);
							
							return false;
						}
					}
					
					File resultPDF = new File(this.tempStorage + "/" + System.nanoTime() + "_" + System.currentTimeMillis() + ".pdf"); 
					try {
						Map<Integer, String> castingFixHtmlMap = new HashMap<>();
						for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
							castingFixHtmlMap.put(Integer.parseInt(entry.getKey()) - 1, entry.getValue());							
						}
						
						File tmpPDF = sip.process(castingFixHtmlMap);
						FileUtils.moveFile(tmpPDF, resultPDF);
					} catch (Exception e) {
						this.errCode = 103345;
						Logger.err.println(this.info.getSession("ID"), this, e);
						
						return false;
					}
					
					//sfmfi에 업로드 진행
					Map<String, String> uploadFileData = new HashMap<String, String>();
					uploadFileData.put("PATH", resultPDF.getAbsolutePath());
					uploadFileData.put("FILE_NAME_CLIENT", layerFile.get(0).get("FILE_NAME_CLIENT"));
					uploadFileData.put("FILE_NAME_SERVER", resultPDF.getName());
					
					if(SepoaModuleService.checkBusiness(config, info, "CSW1678268402593") 
						|| SepoaModuleService.checkBusiness(config, info, "USW1678268370920")) {
						uploadFileData.put("PUBLISH_NO", publishNo);
						uploadFileData.put("SMR_NUMBER", replaceDoc.get("SMR_NUMBER"));
						uploadFileData.put("SMD_NUMBER", replaceDoc.get("SMD_NUMBER"));
						
						SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.postprocess.SepoaSealPostProcessor", ctx, "doProcess", new Object[] {uploadFileData});
						if(out == null || out.status != 1) {
							this.errCode = out.status;
							return false;
						}
						
						uploadFileData.remove("SMR_NUMBER");
						uploadFileData.remove("SMD_NUMBER");
					}
					
					this.fileManager.setLayer(layer);
					try {
						if(this.fileManager.doUploadFile(uploadFileData, null, false) == null) {
							Logger.err.println("Document conver and upload fail : " + replaceDoc.get("SMR_NUMBER"));
							this.errCode = this.fileManager.getLastErrCode();
							return false;
						}
					} catch(Exception e) {
						Logger.err.println("Document conver and upload fail : " + replaceDoc.get("SMR_NUMBER"));
						this.errCode = this.fileManager.getLastErrCode();
						return false;
					}
				}
			}
		}
		
		if(SepoaModuleService.checkBusiness(this.config, info, "STIF1686537827571")) {
			List<Map<String, String>> attachDocList = this.doSelectSSISD(docBasicInfo);
			if(attachDocList != null && attachDocList.size() > 0) {
				this.doConvertCertDocument();
			}
		}
		
		return true;
	}
	
	@Override
	protected String createCertAuditTrailHeader(String templateHtml, Map<String, String> data, String sealedDate) throws Exception {
		SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		
		String subject = MapUtils.getString(data, "SUBJECT");
		String docId = MapUtils.getString(data, "SMR_NUMBER");
		String reqDate = toDateFormat.format(fromDateFormat.parse(MapUtils.getString(data, "ADD_DATE")));
		String deptName = MapUtils.getString(data, "DEPT_NAME");
		String reqName = MapUtils.getString(data, "ADD_USER_NAME");
		String sealDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new SimpleDateFormat("yyyyMMddHHmmss").parse(sealedDate));	
		
		templateHtml = templateHtml.replace("#SUBJECT#", Matcher.quoteReplacement(subject));
		templateHtml = templateHtml.replace("#DOC_ID#", docId);
		templateHtml = templateHtml.replace("#REQ_DATE#", reqDate);
		templateHtml = templateHtml.replace("#DEPT_NAME#", deptName);
		templateHtml = templateHtml.replace("#REQ_NAME#", reqName);
		templateHtml = templateHtml.replace("#SEAL_DATE#", sealDate);
				
		return templateHtml;
	}
	
	@Override
	protected String createAuditTrailHeader(String templateHtml, Map<String, String> data, String firstDocNumber, String sealedDate) throws Exception {
		//첫 번째 이미지 base64로 가져오기 위함
		String layer = "REQ_SEAL";
		Map<String, String> appendData = new HashMap<>();
		appendData.put("LAYER", layer);
		
		//파일매니저 초기화
		if(this.fileManager != null) {
			this.fileManager = null;
		}
		//파일 매니저 셋팅
		if(!this.initFileManager(firstDocNumber)) {
			this.errCode = 100001;
			return null;
		}
		
		List<Map<String, String>> docList = this.fileManager.doList(appendData, LEVEL.FILE);
		Map<String, String> docFileInfo = null;
		if(docList.size() > 0) {
			docFileInfo = docList.get(0);
		}
		
		//패스 매니저 생성
		PathManager pm = new PathManager(info, config);
		pm.setLayer(layer);
		
		String docFilePath = pm.getLayerPath() + docFileInfo.get("FILE_NAME_SERVER");
		InputStream in = null;
		File file = new File(docFilePath);
		try {
			if(file.exists()) {
				in = CipherManager.getCipher().getInputStream(file, false);
			}
		} catch (Exception e) {
			this.errCode = 102411;
			Logger.err.println(this.info.getSession("ID"), this, e);
			return null;
		}
		
		//PDF 문서에서 첫번째 페이지를 이미지로 렌더링
		PDDocument pdfDoc = PDDocument.load(in);
		
		this.docPageCount = pdfDoc.getNumberOfPages();
		
		PDFRenderer pdfImageRenderer = new PDFRenderer(pdfDoc);
		BufferedImage firstImage = pdfImageRenderer.renderImage(0, 0.2f);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(firstImage, "PNG", baos);
		
		String base64 = DatatypeConverter.printBase64Binary(baos.toByteArray());
		pdfDoc.close();
		baos.close();
		
		SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		
		String subject = MapUtils.getString(data, "SUBJECT");
		String docId = MapUtils.getString(data, "SMR_NUMBER");
		String reqDate = toDateFormat.format(fromDateFormat.parse(MapUtils.getString(data, "ADD_DATE")));
		String deptName = MapUtils.getString(data, "DEPT_NAME");
		String reqName = MapUtils.getString(data, "ADD_USER_NAME");
		String sealDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new SimpleDateFormat("yyyyMMddHHmmss").parse(sealedDate));	
		
		templateHtml = templateHtml.replace("#SUBJECT#", Matcher.quoteReplacement(subject));
		templateHtml = templateHtml.replace("#FIRST_PAGE_IMAGE_BASE64#", base64);
		templateHtml = templateHtml.replace("#DOC_ID#", docId);
		templateHtml = templateHtml.replace("#REQ_DATE#", reqDate);
		templateHtml = templateHtml.replace("#DEPT_NAME#", deptName);
		templateHtml = templateHtml.replace("#REQ_NAME#", reqName);
		templateHtml = templateHtml.replace("#SEAL_DATE#", sealDate);
		
		return templateHtml;
	}
	
	@Override
	protected String createCertAuditTrailDocument(List<Map<String, String>> sisdList) throws Exception{
		String returnHtml = "";
		
		for(Map<String, String> docInfo : sisdList) {
			String customColumnSetStr = CommonUtil.getConfig("sepoa.seal.certaudittrail.customDocColumn", "");
			String nodeHtml = "";
			if(StringUtils.isNotBlank(customColumnSetStr)) {
				Set<String> customColumnSet = new LinkedHashSet<>(Arrays.asList(customColumnSetStr.split(";")));
				nodeHtml = "<tr>";
				for(String column : customColumnSet) {
					nodeHtml += "<td class=\"center-middle\">#"+column+"#</td>";
				}
				nodeHtml += "</tr>";
			} else {
				nodeHtml = "<tr><td class=\"center-middle\">#DOC_TYPE_LOC#</td><td class=\"center-middle\">#DOCUMENT_NAME#</td><td class=\"center-middle\">#PUBLISH_DATE#</td><td class=\"center-middle\">#PUBLISH_NO#</td></tr>";
			}
			
			List<Map<String, String>> attachDoc = this.doSelectCorpAttachDocHeader(docInfo);
			if(attachDoc == null || attachDoc.size() == 0) {
				return null;
			}
			String docType = MapUtils.getString(attachDoc.get(0), "DOC_TYPE_LOC", "");
			String subject = MapUtils.getString(attachDoc.get(0), "SUBJECT", "");
			String publishDate = MapUtils.getString(docInfo, "PUBLISH_DATE", "");
			String publishNo = MapUtils.getString(docInfo, "PUBLISH_NO", "");
			
			SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초");
			
			
			nodeHtml = nodeHtml.replace("#DOC_TYPE_LOC#", docType)
						.replace("#DOCUMENT_NAME#", subject)
						.replace("#PUBLISH_DATE#", toDateFormat.format(fromDateFormat.parse(publishDate)))
						.replace("#PUBLISH_NO#", publishNo);
			
			
			returnHtml += nodeHtml;
		}
		
		return returnHtml;
	}
	
	@Override
	protected String createAuditTrailDocument(List<Map<String, String>> sipdList) throws Exception {
		String returnHtml = "";
		
		for(Map<String, String> docInfo : sipdList) {
			
			//파일매니저 초기화
			if(this.fileManager != null) {
				this.fileManager = null;
			}
			String doc_No = docInfo.get("SMR_NUMBER") +"_"+ docInfo.get("SMD_NUMBER"); 
			//파일 매니저 셋팅
			if(!this.initFileManager(doc_No)) {
				this.errCode = 100001;
				return null;
			}
			
			String layer = "REQ_SEAL";
			Map<String, String> appendData = new HashMap<>();
			//패스 매니저 생성
			List<Map<String, String>> docList = this.fileManager.doList(appendData, LEVEL.FILE);
			Map<String, String> docFileInfo = null;
			if(docList.size() > 0) {
				docFileInfo = docList.get(0);
			}
			PathManager pm = new PathManager(info, config);
			pm.setLayer(layer);
			
			String docFilePath = pm.getLayerPath() + docFileInfo.get("FILE_NAME_SERVER");
			InputStream in = null;
			File file = new File(docFilePath);
			try {
				if(file.exists()) {
					in = CipherManager.getCipher().getInputStream(file, false);
				}
			} catch (Exception e) {
				this.errCode = 102411;
				Logger.err.println(this.info.getSession("ID"), this, e);
				return null;
			}
			
			//PDF 문서에서 첫번째 페이지를 이미지로 렌더링
			if(in != null) {
				PDDocument pdfDoc = PDDocument.load(in);
				
				int pageCount = pdfDoc.getNumberOfPages(); 
				
				String nodeHtml = "<tr><td class=\"center-middle\">#DOCUMENT_NAME#</td><td class=\"center-middle\">#PUBLISH_NO#</td><td class=\"center-middle\">#PAGE_COUNT#</td></tr>";
				returnHtml += nodeHtml.replace("#DOCUMENT_NAME#", MapUtils.getString(docInfo, "SUBJECT"))
						.replace("#PUBLISH_NO#", MapUtils.getString(docInfo, "PUBLISH_NO"))
						.replace("#PAGE_COUNT#", Integer.toString(pageCount)+"p");
			}else {//간인의 경우 
				String nodeHtml = "<tr><td class=\"center-middle\">#DOCUMENT_NAME#</td><td class=\"center-middle\">#PUBLISH_NO#</td><td class=\"center-middle\">#PAGE_COUNT#</td></tr>";
				returnHtml += nodeHtml.replace("#DOCUMENT_NAME#", MapUtils.getString(docInfo, "SUBJECT"))
						.replace("#PUBLISH_NO#", MapUtils.getString(docInfo, "PUBLISH_NO"))
						.replace("#PAGE_COUNT#", "");
			}
		}
		return returnHtml;
	}

	public List<Map<String, String>> doSelectSFMFI(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	public List<Map<String, String>> doSelectSFMMT(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			
			return list;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
}