package sepoa.sealmk2;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.regex.Matcher;

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
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.CommonUtil;

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
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return false;
		}
		
		/* FIX_HTML 통합 데이터 FI의 META 데이터 저장  */
		String fileId = MapUtils.getString(editorHtmls.get(0), "FILE_ID");
		String layer = MapUtils.getString(editorHtmls.get(0), "LAYER");
		Map<String, String> mergeFixHtml = new HashMap<>();
		for(Map<String, String> page : editorHtmls) {
			mergeFixHtml.put(MapUtils.getString(page, "PART_ID"), MapUtils.getString(page, "FIX_HTML"));
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
	
	@Override
	public boolean doConvertReqSealDocument(String smrNo, String smdNo) {
		List<Map<String, String>> replaceDocList = new ArrayList<Map<String, String>>();
		if ("".equals(smrNo) || smrNo == null || "".equals(smdNo) || smdNo == null) {
			List<Map<String, String>> upDocList = this.doSelectSSIPD(this.docBasicInfo);
			if (upDocList == null) {
				//this.errCode = 103327;
				return true;
			}
			smrNo = this.docBasicInfo.get("SMR_NUMBER");
			if("R".equals(this.docBasicInfo.get("RE_FLAG"))) {	 // 실물이면 컨버팅하지 않음
				return true;
			}else {
				for (Map<String, String> upDoc : upDocList) {
					Map<String, String> doc = new HashMap<String, String>();
					doc.put("SMR_NUMBER", smrNo);
					String smdNumber = upDoc.get("SMD_NUMBER");
					if (smdNumber.startsWith("NO_DOC")) continue;	// 날인대상문서를 사용하지 않음
					doc.put("SMD_NUMBER", smdNumber);
	
					replaceDocList.add(upDoc);
				}
			}
		} else {
			Map<String, String> doc = new HashMap<String, String>();
			doc.put("SMR_NUMBER", smrNo);
			doc.put("SMD_NUMBER", smdNo);

			replaceDocList.add(doc);
		}

		if (replaceDocList.size() == 0) {
			return true;
			//this.errCode = 103327;
			//return false;
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
			//주의 :: 
			//발행번호는 footer로써 모든 페이지에 적용된다. 로고 랜더링시 모든 페이지를 불러오는 역활도 한다. 
			//페이징의 경우 맨 마지막에 진행해야 하기에 만약 발행번호를 주석처리 하게 된다면 페이징은 페이지 수 상관없이 1/1로 끝나므로 
			//아래 publishNoHtml 에서 display:none으로 하되 주석 처리는 하지 말아야 발행번호 미표시+ 페이지 표시가 가능하다.
			String publishNoHtml = "<div class=\"footer\" style=\"position:absolute;bottom:25px;width:100%;font-size:13px;z-index:100;display:none;justify-content:left;text-align:left;\"><div style=\"padding:10px;color:#999;\">"+publishText+"</div></div>";
			
			//footer 추가 부분 - fixHtml이 없어도 모든 장에 들어가야 하므로 맨 앞으로 뺌
			String legalWatermark = MapUtils.getString(this.docBasicInfo, "LEGAL_WATERMARK", "").toUpperCase();
			if(StringUtils.isNotBlank(legalWatermark)) {
				String legalfooterText = "";
				if("K".equals(legalWatermark)) {
					legalfooterText = Cache.text.get("FOOTER_TEXT.LEGAL_TEXT_KO");
				}else if("E".equals(legalWatermark)) {
					legalfooterText = Cache.text.get("FOOTER_TEXT.LEGAL_TEXT_EN");
				}
				
				String footerContents = "<div class=\"footer\" style=\"position:absolute;bottom:0%;width:100%;font-size:12px;z-index:100;display:flex;justify-content:#ALIGN#;text-align:#ALIGN#;\"><div style=\"padding:10px;\">#TEXT#</div></div>";
				footerContents = footerContents.replace("#TEXT#", legalfooterText);
				footerContents = footerContents.replace("#ALIGN#", CommonUtil.getConfig("sepoa.footer.legal.align", "left").toLowerCase());
				
				fixedMsg += footerContents;
			}
			// 발행번호
			fixedMsg += publishNoHtml;
			
			
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
				
				for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
					String fixHtml = entry.getValue();
					try {
						String repSealHtml = parseAttachSeal(fixHtml, replaceDoc);
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
				this.fileManager.doUploadFile(uploadFileData, null, false);
			}
		}
		
		return true;
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
		System.out.println("004");
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
	protected String createAuditTrailDocument(List<Map<String, String>> sipdList) throws Exception {
		String returnHtml = "";
		
		for(Map<String, String> docInfo : sipdList) {
			String nodeHtml = "<tr><td class=\"left-middle\">#DOCUMENT_NAME#</td><td class=\"left-middle\">#PAGE_COUNT#</td></tr>";
			returnHtml += nodeHtml.replace("#DOCUMENT_NAME#", MapUtils.getString(docInfo, "SUBJECT"))
					.replace("#PAGE_COUNT#", Integer.toString(this.docPageCount)+"p");
		}
		return returnHtml;
	}
}