package sepoa.contract.util;

import java.awt.Rectangle;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.util.Matrix;

import filters.ThreadLocalFilter;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.log.Logger;
import sepoa.fw.module.SepoaModuleManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.renderer.SepoaPDFRenderer;
import sepoa.fw.util.renderer.image.SepoaImageCapture;
import sepoa.fw.util.renderer.image.SepoaImageCapture.Data;
import sepoa.fw.util.renderer.image.SepoaImageCapture.Engine;

public class SepoaImageProcessor extends SepoaModuleManager {
	public enum PageSize {
		WIDTH, HEIGHT
	}
	
	private ServletContext sc;
	private SepoaPDFRenderer renderer;
	private SepoaImageCapture capture;
	private File pdfFile;
	private String fixedMsg;
	private Map<Integer, Map<PageSize, String>> pageSizeMap = new HashMap<>();

	public SepoaImageProcessor(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		init();
	}
	
	public void setPageSize(Integer pageNo, Map<PageSize, String> sizeMap) {
		this.pageSizeMap.put(pageNo, sizeMap);
	}
	
	public void setPDF(Map<String, String> fileInfo, boolean isEncrypted) {
		File origin = new PathManager(info, config).getFile(fileInfo);
		InputStream is = null;
		try {
			this.pdfFile = new File(this.tempStorage + "/" + System.nanoTime() + "_" + System.currentTimeMillis() + ".pdf");
			if(isEncrypted) {
				is = CipherManager.getCipher().getInputStream(origin, false);
				FileUtils.copyInputStreamToFile(is, this.pdfFile);
			} else {
				FileUtils.copyFile(origin, this.pdfFile);
			}
			
			this.renderer = new SepoaPDFRenderer(this.pdfFile);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {}
			}
		}
	}
	
	public PDDocument getPDFDoc() {
		return this.renderer.getPDFDoc();
	}
	
	public void setCustomCss(String cssStyle) {
		this.capture.setCssStyle(cssStyle);
	}
	
	public void setFixedMsg(String fixedMsg) {
		this.fixedMsg = fixedMsg;
	}
	
	public File process(Map<Integer, String> fixHtmlMap) throws Exception {
		if(this.renderer == null) {
			throw new Exception("PDF File is not set.");
		}
		
		PDDocument doc = this.renderer.getPDFDoc();
		
		if(this.fixedMsg != null && !"".equals(this.fixedMsg)) {
			for(int i = 0; i < doc.getNumberOfPages(); i++) {
				String value = MapUtils.getString(fixHtmlMap, i, "") + System.lineSeparator() + this.fixedMsg;
				fixHtmlMap.put(i, value);
			}
		}
		
		Map<Integer, Map<Data, String>> data = new HashMap<>();
		for(Entry<Integer, String> html : fixHtmlMap.entrySet()) {
			Map<Data, String> fixhtml = new HashMap<>();
			fixhtml.put(SepoaImageCapture.Data.HTML, html.getValue());
			
			PDPage page = doc.getPage(html.getKey());
			PDRectangle cropBox = page.getCropBox();
			if(this.pageSizeMap.containsKey(html.getKey())) {
				String width = MapUtils.getString(this.pageSizeMap.get(html.getKey()), PageSize.WIDTH, "");
				String height = MapUtils.getString(this.pageSizeMap.get(html.getKey()), PageSize.HEIGHT, "");
				
				double pageWidth = cropBox.getWidth();
				double pageHeight = cropBox.getHeight();
				if(page.getRotation() > 0) {
					Matrix matrix = Matrix.getRotateInstance(Math.toRadians(page.getRotation()), 0, 0);
					Rectangle rectangle = cropBox.transform(matrix).getBounds();
					
					pageWidth = rectangle.getWidth();
					pageHeight = rectangle.getHeight();
				}
				
				if(!"".equals(width)) {
					fixhtml.put(SepoaImageCapture.Data.WIDTH, width);
					
					if(!"".equals(height)) {
						fixhtml.put(SepoaImageCapture.Data.HEIGHT, height);
					} else {
						double heightRatio = pageHeight / pageWidth;
						fixhtml.put(SepoaImageCapture.Data.HEIGHT, Double.toString(Integer.parseInt(width) * heightRatio));
					}
				}
			}
			
			if(fixhtml.containsKey(SepoaImageCapture.Data.WIDTH) == false) {
				fixhtml.put(SepoaImageCapture.Data.WIDTH, String.valueOf(cropBox.getWidth()));
			}
			
			if(fixhtml.containsKey(SepoaImageCapture.Data.HEIGHT) == false) {
				fixhtml.put(SepoaImageCapture.Data.HEIGHT, String.valueOf(cropBox.getHeight()));
			}
			
			data.put(html.getKey(), fixhtml);
		}
		
		this.capture.setTempDirectory(this.tempStorage);
		this.capture.setEngine(Engine.CHROME);
		this.capture.setData(data);
		
		return renderer.render(this.capture.capture()).getAbsoluteFile();
	}
	
	private void init(){
		try {
			String docBase = "";
			try {
				this.sc = ThreadLocalFilter.getHttpServletRequest().getServletContext();
				if(this.sc == null) {
					throw new Exception("ThreadLocalFilter HttpServlertRequest ServletContext is Null. Make Custom ServletContext.");
				}
			} catch (Exception e) {
				Logger.err.println(e);
				this.sc = new SepoaCustomServletContext();
			}
			
			docBase = this.sc.getRealPath("/").replaceAll("\\\\", "/");
			List<String> resources = Arrays.asList(CommonUtil.getConfig("sepoa.renderer.resource.css").split(";"));
			for(String resource : resources) {
				resources.set(resources.indexOf(resource), Paths.get(docBase, resource).toString());
			}
			
			this.capture = new SepoaImageCapture();
			this.capture.setCssFile(resources);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
	}
	
}
