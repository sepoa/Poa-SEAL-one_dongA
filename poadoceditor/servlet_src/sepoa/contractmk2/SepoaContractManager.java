package sepoa.contractmk2;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.Gson;

import sepoa.contract.util.SepoaImageProcessor;
import sepoa.filemanager.FileManager;
import sepoa.filemanager.FileManager.LEVEL;
import sepoa.filemanager.utils.cipher.CipherException;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.log.Logger;
import sepoa.fw.module.pdf.SepoaPDFDocument;
import sepoa.fw.module.pdf.SepoaPDFModifier;
import sepoa.fw.module.pdf.content.SepoaPDFContent;
import sepoa.fw.module.pdf.content.SepoaPDFContentPositionSetter;
import sepoa.fw.module.pdf.content.SepoaPDFContentText;
import sepoa.fw.module.pdf.content.SepoaPDFContentText.FontType;
import sepoa.fw.module.pdf.pdfbox.SepoaPDFDocumentPDFBox;
import sepoa.fw.module.pdf.pdfbox.SepoaPDFModifierPDFBox;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaJSON;
import sepoa.fw.util.ux.sUtils;

public class SepoaContractManager extends sepoa.contract.SepoaContractManager {
	protected String[] progressImageLayer = new String[] {"DUMMY", "G", "C", "E", "B"};
	
	protected static Pattern signItemClassNamePattern = Pattern.compile("contract-item-box-sign-id-(\\w+)");
	
	public SepoaContractManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.docBasicInfo = new HashMap<String, String>();
	}
	
	// 이미지 리스트조회시 파일리스트 출력하도록 수정
	@Override
	public List<Map<String, String>> doListImage(){
		String[] layers = null;
		if("A".equals(this.docBasicInfo.get("STATUS"))) {
			layers = this.completeFileLayer;
		}else {
			layers = this.progressImageLayer;
		}
		
		List<Map<String, String>> result = new LinkedList<Map<String, String>>();
		for(String layer : layers) {
			List<Map<String, String>> layerFile = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_"+layer), LEVEL.FILE);
			if(layerFile != null) {
				result.addAll(layerFile);
			}
		}
		if (result.size() == 0) {
			this.errCode = 100101;
		} 
		return result;
	}
	
	// 결과물이 이미지 or PDF 파일이 조회될 수 있음
	@Override
	public Map<String, String> doImage(String fileID, String partID, boolean thumb, boolean noImageFlag){
		if(fileID.isEmpty()) {
			this.errCode = 100102;
			return null;
		}else {
			List<String> fileList = new ArrayList<String>();
			fileList.add(fileID);
			
			Map<String, String> result = this.fileManager.doDownload(fileList, true);
			if(result == null) {
				Logger.err.println(info.getSession("ID"), this, "not found image : "+this.fileManager.getLastErrCode());
				this.errCode = 100103;
				return null;
			}
			return result;
		}
	}
	
	@Override
	public boolean doUpdateEditorHTML(List<Map<String, String>> editorHtmls) {
		Map<String, Map<String, String>> fileMetaDataMap = new HashMap<>();
		Map<String, Map<String, String>> fileFixHtmlMap = new HashMap<>();
		
		List<Map<String, String>> fileList = this.doListImage();
		for(Map<String, String> file : fileList) {
			String fileId = MapUtils.getString(file, "FILE_ID");
			if(!fileMetaDataMap.containsKey(fileId)) {
				fileMetaDataMap.put(fileId, new HashMap<>());
			}
			
			if(!fileFixHtmlMap.containsKey(fileId)) {
				fileFixHtmlMap.put(fileId, new HashMap<>());
			}
			
			Map<String, String> fileMetaData = fileMetaDataMap.get(fileId);
			if(!fileMetaData.containsKey("LAYER")) {
				fileMetaData.put("LAYER", MapUtils.getString(file, "LAYER"));
			}
		}
		
		for(Map<String, String> page : editorHtmls) {
			String fileId = MapUtils.getString(page, "FILE_ID");
			Map<String, String> fileMetaData = fileMetaDataMap.get(fileId);
			if(page.containsKey("FILE_TYPE")) {
				fileMetaData.put("FILE_TYPE", MapUtils.getString(page, "FILE_TYPE"));
			}
			
			Map<String, String> pageFixHtml = fileFixHtmlMap.get(fileId);
			pageFixHtml.put(MapUtils.getString(page, "PART_ID"), MapUtils.getString(page, "FIX_HTML", ""));
		}
		
		for(Entry<String, Map<String, String>> entry : fileFixHtmlMap.entrySet()) {
			String fileId = entry.getKey();
			String layer = fileMetaDataMap.get(fileId).get("LAYER");
			String fileType = MapUtils.getString(fileMetaDataMap.get(fileId), "FILE_TYPE", null);
			
			Map<String, String> fileFixHtml = entry.getValue();
			Map<String, String> meta = new HashMap<>();
			meta.put("FILE_ID", fileId);
			meta.put("LAYER", layer);
			meta.put("FIX_HTML", new Gson().toJson(fileFixHtml));
			if(fileType != null) {
				meta.put("FILE_TYPE", fileType);
			}
			
			this.fileManager.doModifyMeta(meta);
			int errCode = this.fileManager.getLastErrCode();
			if(errCode != 0) {
				this.errCode = 100204;
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean doMappingData(){
		Map<String, String> data = new HashMap<String, String>(this.docBasicInfo);
		List<String> signerNoList = new ArrayList<>();
		List<Map<String, String>> signerList = this.doSelectSigner();
		for(Map<String, String> signer : signerList) {
			String signerNo = signer.get("SIGNER_NO");
			if(!signerNoList.contains(signerNo)); {
				signerNoList.add(signerNo);
			}
			
			for(Entry<String, String> entry : signer.entrySet()) {
				data.put("SIGN_ITEM_"+entry.getKey()+"_"+signerNo, entry.getValue());
			}
		}
		
		List<Map<String, String>> cmList = this.doSelectSCTMD(this.docBasicInfo);
		for(Map<String, String> cmData : cmList) {
			data.put(cmData.get("ITEM_CODE"), cmData.get("ITEM_VALUE"));
		}
		
		List<Map<String, String>> fileList = this.doListImage();
		List<Map<String, String>> updateFileList = new ArrayList<Map<String, String>>();
		for(Map<String, String> file : fileList){
			String mergeFixHtml = MapUtils.getString(file, "FIX_HTML", "");
			if(!StringUtils.isBlank(mergeFixHtml)) {
				Map<String, String> fixHtmlMap = new Gson().fromJson(mergeFixHtml, Map.class);
				if(fixHtmlMap == null) {
					fixHtmlMap = new HashMap<>();
				}
				
				for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
					Document doc = Jsoup.parse(entry.getValue());
					List<String> templateSignerNoList = new ArrayList<>();
					Elements signItemList = doc.select("[class~=contract-item-box-sign-id-]");
					for(Element signItem : signItemList) {
						String classList = signItem.className();
						Matcher m = signItemClassNamePattern.matcher(classList);
						if(m.find()) {
							String signerNo = m.group(1);
							if(!templateSignerNoList.contains(signerNo)) {
								templateSignerNoList.add(signerNo);
							}
						}
					}
					
					templateSignerNoList.sort(Comparator.comparing(id -> id));
					
					Elements itemList = doc.select(".contract-item.contract-item-box");
					boolean mappingFlag = false;
					for(Element item : itemList) {
						String itemValue = null;
						
						String classList = item.className();
						Matcher signNoMatcher = signItemClassNamePattern.matcher(classList);
						if(signNoMatcher.find()) {
							String signerNo = signNoMatcher.group(1);
							int index = templateSignerNoList.indexOf(signerNo);
							if(index >= 0 && (index + 1) <= signerNoList.size()) {
								classList = classList.replaceAll(signerNo, signerNoList.get(index));
							}
						}
						
						Matcher m = itemBoxClassNamePattern.matcher(classList);
						if(m.find()) {
							String itemCode = m.group(1);
							itemValue = data.get(itemCode);
						}
						
						if(!StringUtils.isBlank(itemValue)) {
							Elements selectedInputElements = item.select("textarea.contract-item-input");
							Elements originInputElements = item.select(".contract-item-input-origin");
							if(selectedInputElements.size() > 0) {
								Element input = selectedInputElements.first();
								Element originInput = originInputElements.first();
								Map<String, String> itemProps = new HashMap<String, String>();
								for(String inputClassName : input.classNames()) {
									Matcher propMatcher = itemPropertiesClassNamePattern.matcher(inputClassName);
									if(propMatcher.find()) {
										itemProps.put(propMatcher.group(1), propMatcher.group(2));
									}
								}
								
								if(itemProps.containsKey("date_format")) {
									String format = itemProps.get("date_format");
									itemValue = sUtils.date.toString(sUtils.date.fromString(itemValue, "%Y%m%d"), format);
								}else if(itemProps.containsKey("number_format")) {
									String format = itemProps.get("number_format");
									itemValue = sUtils.number.format(itemValue, format);
								}
								
//								input.appendText(itemValue);
								input.text(itemValue);
								originInput.text(itemValue);
								
								item.addClass("contract-item-box-auto-mapping");
								mappingFlag = true;
							}
						}
					}
					if(mappingFlag) {
						fixHtmlMap.put(entry.getKey(), doc.body().html());
					}
				}
				
				Map<String, String> updateFileInfo = new HashMap<String, String>(file);
				updateFileInfo.put("FIX_HTML", new Gson().toJson(fixHtmlMap));
				updateFileList.add(updateFileInfo);
			}
		}
		
		if(updateFileList.size() > 0) {
			List<Map<String, String>> editorHtmlList = new ArrayList<>();
			for(Map<String, String> file : updateFileList) {
				if(file.containsKey("FIX_HTML")) {
					Map<String, String> fixHtml = new Gson().fromJson(file.get("FIX_HTML"), Map.class);
					for(Entry<String, String> entry : fixHtml.entrySet()) {
						Map<String, String> editorHtml = new HashMap<>();
						editorHtml.put("FILE_ID", file.get("FILE_ID"));
						editorHtml.put("LAYER", file.get("LAYER"));
						editorHtml.put("PART_ID", entry.getKey());
						editorHtml.put("FIX_HTML", entry.getValue());
						
						if(file.containsKey("FILE_TYPE")) {
							editorHtml.put("FILE_TYPE", file.get("FILE_TYPE"));
						}
						
						editorHtmlList.add(editorHtml);
					}
				}
			}
			
			this.doUpdateEditorHTML(editorHtmlList);
		}
		
		return true;
	}
	
	@Override
	public boolean doUpdateForm(String formNo) {
		String beforeFormNo = this.docBasicInfo.get("CONT_FORM_NO");
		if(formNo.equals(beforeFormNo)) {
			return true;
		}
		
		List<Map<String, String>> fileList = this.doListImage();
		if(fileList != null && fileList.size() > 0 && "".equals(beforeFormNo)) {
			String docNumber = formNo + "_1";
			FileManager fm = new FileManager(info, config, this.ctx);
			if (fm.setDocNumber(docNumber, true) == null) {
				return false;
			}
			Map<String, String> fixHtmlMap = new HashMap<>();
			List<Map<String, String>> templateFileList = fm.doList();
			if(templateFileList != null && templateFileList.size() > 0) {
				Collections.sort(templateFileList, Comparator.comparing(file -> file.get("DOC_SEQ")));
				List<File> tempFileList = new ArrayList<>();
				try {
					int prevPages = 0;
					PathManager pm = new PathManager(info, config);
					for(Map<String, String> file : templateFileList) {
						Map<String, String> fixHtml = new Gson().fromJson(file.get("FIX_HTML"), Map.class);
						for(Entry<String, String> entry : fixHtml.entrySet()) {
							String idx = StringUtils.leftPad(String.valueOf(prevPages + Integer.parseInt(entry.getKey())), 3, "0");
							fixHtmlMap.put(idx, entry.getValue());
						}
						
						if(file.get("LAYER").contains("DUMMY")) {
							prevPages += fixHtml.size();
						} else {
							File target = CipherManager.decrypt(pm.getFile(file), false);
							tempFileList.add(target);
							SepoaPDFDocumentPDFBox document = (SepoaPDFDocumentPDFBox) SepoaPDFDocument.load(target);
							prevPages += document.getPDDocument().getNumberOfPages();
							document.close();
						}
					}
					
					prevPages = 0;
					List<Map<String, String>> editorHtmls = new ArrayList<>();
					for(Map<String, String> file : fileList) {
						String fileId = file.get("FILE_ID");
						String layer = file.get("LAYER");
						File target = CipherManager.decrypt(pm.getFile(file), false);
						tempFileList.add(target);
						SepoaPDFDocumentPDFBox document = (SepoaPDFDocumentPDFBox) SepoaPDFDocument.load(target);
						int pageNumbers = document.getPDDocument().getNumberOfPages();
						document.close();
						
						for(int i = 1; i <= pageNumbers; i++) {
							String idx = StringUtils.leftPad(String.valueOf(prevPages + i), 3, "0");
							String fixHtml = MapUtils.getString(fixHtmlMap, idx, null);
							
							if(fixHtml != null) {
								Map<String, String> html = new HashMap<>();
								html.put("FIX_HTML", fixHtml);
								html.put("FILE_ID", fileId);
								html.put("PART_ID", StringUtils.leftPad(String.valueOf(i), 3, "0"));
								html.put("LAYER", layer);
								editorHtmls.add(html);
							}
						}
						
						prevPages += pageNumbers;
					}
					
					if(editorHtmls.size() > 0) {
						this.doUpdateEditorHTML(editorHtmls);
					}
					
				} catch(Exception e) {
					e.printStackTrace();
					Logger.err.println(e.getMessage());
				} finally {
					try {
						for(File f : tempFileList) {
							if(f.exists()) {
								FileUtils.moveFileToDirectory(f, new File(this.tempStorage), false);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
						Logger.err.println(e.getMessage());
					}
				}
			}
		} else {
			this.errCode = 0;
			
			this.doDeleteFile();
			this.fileManager.doCopy(formNo + "_1", "");
			if(this.fileManager.getLastErrCode() != 0) {
				this.errCode = 100202;
				return false;
			}
			
			fileList = this.fileManager.doList();
			for(Map<String, String> file : fileList) {
				String layer = file.get("LAYER");
				if(layer.contains("DUMMY")) {
					String docSeq = file.get("DOC_SEQ");
					String fileName = file.get("FILE_NAME_CLIENT");
					String fixHtml = file.get("FIX_HTML");
					
					Map<String, String> fixHtmlMap = new Gson().fromJson(fixHtml, Map.class);
					int maxPageNum = Integer.parseInt(fixHtmlMap.keySet().stream().max(Comparator.comparing(e -> Integer.parseInt(e))).get());
					
					String pdfPath = this.tempStorage+"/"+String.valueOf(System.nanoTime())+".pdf";
					File pdfFile = new File(pdfPath);
					try(PDDocument document = new PDDocument();) {
						for(int i = 0; i < maxPageNum; i++) {
							document.addPage(new PDPage(PDRectangle.A4));
						}
						document.save(pdfFile);
						document.close();
					} catch (IOException e) {
						Logger.err.println(e.getMessage());
					}
					
					this.fileManager.doDelete(file.get("FILE_ID"));
					
					Map<String, String> uploadFileData = new HashMap<String, String>();
					uploadFileData.put("PATH", pdfFile.getAbsolutePath());
					uploadFileData.put("FILE_NAME_CLIENT", fileName);
					uploadFileData.put("DOC_SEQ", docSeq);
					
					this.fileManager.setLayer("CONT_C");
					List<Map<String, String>> result = this.fileManager.doUploadFile(uploadFileData, null, false);
					if(fileManager.getLastErrCode() != 0) {
						this.errCode = 100401;
						return false;
					}
					
					Map<String, String> meta = new HashMap<>();
					meta.put("FILE_ID", result.get(0).get("FILE_ID"));
					meta.put("LAYER", "CONT_C");
					meta.put("FIX_HTML", fixHtml);
					
					this.fileManager.doModifyMeta(meta);
					int errCode = this.fileManager.getLastErrCode();
					if(errCode != 0) {
						this.errCode = 100204;
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	/*
	 * etc
	 */
	@Override
	public String getContractHash() {
		List<Map<String, String>> fileList = new LinkedList<Map<String, String>>();
		List<Map<String, String>> selectedList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_C"), LEVEL.FILE);
		if(selectedList != null) {
			fileList.addAll(selectedList);
		}
		selectedList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_E"), LEVEL.FILE);
		if(selectedList != null) {
			fileList.addAll(selectedList);
		}
		selectedList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_A"), LEVEL.FILE);
		if(selectedList != null) {
			fileList.addAll(selectedList);
		}
		selectedList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_B"), LEVEL.FILE);
		if(selectedList != null) {
			fileList.addAll(selectedList);
		}
	
		List<String> hashList = new ArrayList<String>(fileList.size());
		try {
			PathManager pm = new PathManager(info, config);
			for(Map<String, String> fileInfo : fileList) {
				File file = pm.getFile(fileInfo);
				if(file != null) {
					hashList.add(Files.hash(file, Hashing.sha512()).toString());
				}else {
					return null;
				}
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			return null;
		}
		return StringUtils.join(hashList, "_");
	}
	
	
	@Override
	protected boolean documentRendering() {
		List<Map<String, String>> fileList = this.doListImage();
		if(fileList == null) {
			this.errCode = 100704;
			return false;
		}
		
		PathManager pm = new PathManager(info, config);
		for(Map<String, String> file : fileList) {
			String mergeFixHtml = MapUtils.getString(file, "FIX_HTML", "");
			Map<String, String> fixHtmlMap = new Gson().fromJson(mergeFixHtml, Map.class);
			if(fixHtmlMap != null) {
				// SepoaImageProcessor = HTML 문자열을 이미지화 하여 PDF 파일에 삽입
				SepoaImageProcessor sip = new SepoaImageProcessor(info, config, ctx);
				sip.setTempDirectory(this.tempStorage);
				sip.setPDF(file, true);
				
				Map<Integer, String> parsedFixHtmlMap = new HashMap<Integer, String>();
				for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
					Integer pageNo = Integer.parseInt(entry.getKey()) - 1;
					String fixHtml = parseSepoaImage(entry.getValue());
					parsedFixHtmlMap.put(pageNo, fixHtml);
					
					// 현재 가로 너비 945px이 기준 (sepoa.contract.module.css의 .contract-editor-list-item-wrapper 참고)
					Map<SepoaImageProcessor.PageSize, String> sizeMap = new HashMap<>();
					sizeMap.put(SepoaImageProcessor.PageSize.WIDTH, "945");
					sip.setPageSize(pageNo, sizeMap);
				}
				
				try {
					File resultPDF = sip.process(parsedFixHtmlMap);
					if(resultPDF.exists()) {
						
						File oldFile = pm.getFile(file);
						this.fileManager.doDelete(file.get("FILE_ID"));
						FileUtils.moveFileToDirectory(oldFile, new File(this.tempStorage), false);
						
						Map<String, String> uploadFileData = new HashMap<String, String>();
						uploadFileData.put("PATH", resultPDF.getAbsolutePath());
						uploadFileData.put("FILE_NAME_CLIENT", file.get("FILE_NAME_CLIENT"));
						uploadFileData.put("DOC_SEQ", file.get("DOC_SEQ"));
						
						this.fileManager.setLayer(file.get("LAYER"));
						List<Map<String, String>> result = this.fileManager.doUploadFile(uploadFileData, null, false);
						if(fileManager.getLastErrCode() != 0) {
							this.errCode = 100401;
							return false;
						}
						
						Map<String, String> meta = new HashMap<>();
						meta.put("FILE_ID", result.get(0).get("FILE_ID"));
						meta.put("LAYER", file.get("LAYER"));
						meta.put("FIX_HTML", mergeFixHtml);
						
						this.fileManager.doModifyMeta(meta);
						int errCode = this.fileManager.getLastErrCode();
						if(errCode != 0) {
							this.errCode = 100204;
							return false;
						}
						
					} else {
						throw new Exception("SepoaImageProcessor process failed.");
					}
				} catch (Exception e) {
					e.printStackTrace();
					Logger.err.println(e.getMessage());
					this.errCode = 100705;
					return false;
				}
			}
		}
		
		return true;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected boolean createCompletePDF() {
		Map<String, Map<String, String>> fileSignHtmlMap = new HashMap<>();
		
		List<Map<String, String>> fileList = this.doListImage();
		if(fileList == null) {
			this.errCode = 100704;
			return false;
		}
		
		List<Map<String, String>> signerList = this.doSelectSigner();
		for(Map<String, String> signer : signerList) {
			String signFixHtml = signer.get("SIGN_FIX_HTML");
			if(!StringUtils.isBlank(signFixHtml)) {
				List<Map<String, String>> signerHtmlObject = (List<Map<String, String>>) SepoaJSON.toJSONObject(signFixHtml);
				for(Map<String, String> htmlPage : signerHtmlObject) {
					String fileId = htmlPage.get("FILE_ID");
					String pageNo = htmlPage.get("PART_ID");
					String fixHtml = htmlPage.get("FIX_HTML");
					
					if(!fileSignHtmlMap.containsKey(fileId)) {
						fileSignHtmlMap.put(fileId, new HashMap<>());
					}
					
					Map<String, String> signHtmlMap = fileSignHtmlMap.get(fileId);
					if(signHtmlMap.containsKey(pageNo)) {
						signHtmlMap.put(pageNo, signHtmlMap.get(pageNo) + fixHtml);
					} else {
						signHtmlMap.put(pageNo, fixHtml);
					}
				}
			}
		}
		
		List<File> pdfFileList = new ArrayList<>();
		PathManager pm = new PathManager(info, config);
		for(Map<String, String> file : fileList) {
			Map<String, String> fixHtmlMap = MapUtils.getMap(fileSignHtmlMap, file.get("FILE_ID"), null);
			if(fixHtmlMap != null) {
				SepoaImageProcessor sip = new SepoaImageProcessor(info, config, ctx);
				sip.setTempDirectory(this.tempStorage);
				sip.setPDF(file, true);
				
				Map<Integer, String> parsedFixHtmlMap = new HashMap<>();
				for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
					Integer pageNo = Integer.parseInt(entry.getKey()) - 1;
					String fixHtml = parseSepoaImage(entry.getValue());
					parsedFixHtmlMap.put(pageNo, fixHtml);
					
					Map<SepoaImageProcessor.PageSize, String> sizeMap = new HashMap<>();
					sizeMap.put(SepoaImageProcessor.PageSize.WIDTH, "945");
					sip.setPageSize(pageNo, sizeMap);
				}
				
				try {
					File resultPDF = sip.process(parsedFixHtmlMap);
					if(resultPDF.exists()) {
						pdfFileList.add(resultPDF);
					}
				} catch(Exception e) {
					e.printStackTrace();
					Logger.err.println(e.getMessage());
					this.errCode = 100705;
					return false;
				}
			} else {
				try {
					File target = CipherManager.decrypt(pm.getFile(file), false);
					pdfFileList.add(target);
				} catch (CipherException e) {
					Logger.err.println(e);
					return false;
				}
			}
		}
		
		SepoaPDFDocument document = null;
		SepoaPDFModifier modifier = null;
		try {
			if(pdfFileList.size() > 0) {
				document = SepoaPDFDocument.load(pdfFileList.get(0));
				modifier = SepoaPDFModifier.load(document, this.config);
				((SepoaPDFModifierPDFBox)modifier).optimize(true);
				
				for(File pdf : pdfFileList) {
					if(pdfFileList.indexOf(pdf) != 0) {
						modifier.setMergeDocument(pdf);
					}
				}
			} else {
				this.errCode = 100903;
				return false;
			}
			
			String completePdfFilePath = this.tempStorage+"/"+String.valueOf(System.nanoTime())+".pdf";
			
			//watermark
			String taskNumber = String.valueOf(System.nanoTime())+"_"+Thread.currentThread().getName();
			String customWatermarkService = config.get("sepoa.contract.watermark.service", null);
			if(StringUtils.isNotBlank(customWatermarkService)) {
				SepoaOut so = ServiceConnector.doService(info, customWatermarkService, ctx,"createPdfWatermark", new Object[] {taskNumber, this.getDocumentInfo()});
				if(so != null && so.flag) {
					if(so.getData(0) != null) {
						SepoaPDFContent content = (SepoaPDFContent) so.getData(0);
						modifier.addGlobalContent(content);
					}
				}else{
					this.errCode = 100904;
					return false;
				}
			}else {
				String watermark = Cache.text.get("POA_CONTRACT.WATERMARK_A");
				File fontFile = new File(this.tempStorage+"/"+String.valueOf(System.nanoTime())+".ttf");
				FileUtils.copyInputStreamToFile(this.getClass().getResourceAsStream("/fonts/NanumGothic/NanumGothic.ttf"), fontFile);
				
				SepoaPDFContentText watermarkContent = new SepoaPDFContentText(watermark, fontFile, FontType.TTF, 100);
				watermarkContent.setColor(Color.gray);
				watermarkContent.setOpacity(0.1);
				watermarkContent.setDegree(45);
				
				SepoaPDFContentPositionSetter x = new SepoaPDFContentPositionSetter()
					.addPageWidth(0.5)
					.reduceContentWidth(0.5);
				SepoaPDFContentPositionSetter y = new SepoaPDFContentPositionSetter()
					.addPageHeight(0.5)
					.reduceContentHeight(0.5);
				watermarkContent.setPosition(x, y);
				
				modifier.addGlobalContent(watermarkContent);
			}
			
			File complatePdfFile = new File(completePdfFilePath);
			modifier.save(complatePdfFile);
			
			boolean result = complatePdfFile.exists(); 
			if(StringUtils.isNotBlank(customWatermarkService)) {
				ServiceConnector.doService(info, customWatermarkService, ctx,"createPdfAfterEvent", new Object[] {taskNumber, this.getDocumentInfo()});
			}
			
			if(!result) {
				this.errCode = 100904;
				return false;
			}
			
			Map<String, String> completeFileInfo = new HashMap<String, String>();
			completeFileInfo.put("FILE_TYPE"       , "CC");
			completeFileInfo.put("PATH"            , completePdfFilePath);
			completeFileInfo.put("FILE_NAME_CLIENT", this.docBasicInfo.get("CONT_SUBJECT")+".pdf"); //파일 명 [파일 다운로드시 사용되는 원본 파일명]
			
			if(this.doAddFile(completeFileInfo.get("FILE_TYPE"), completeFileInfo, true) == null) {
				this.errCode = 100905;
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.errCode = 100903;
			return false;
		} finally {
			try {
				if(document != null) {
					document.close();
				}
				
				if(pdfFileList != null && pdfFileList.size() > 0) {
					for(File pdfFile : pdfFileList) {
						if(pdfFile.exists()) {
							FileUtils.moveFileToDirectory(pdfFile, new File(this.tempStorage), false);
						}
					}
				}
			} catch (IOException e) {
				Logger.err.println(e.getMessage());
			}
		}
		
		//감사추적인증서 생성부
		return true;
	}
}
