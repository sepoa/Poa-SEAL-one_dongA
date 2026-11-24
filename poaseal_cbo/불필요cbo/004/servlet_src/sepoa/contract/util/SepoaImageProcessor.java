package sepoa.contract.util;

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
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import filters.ThreadLocalFilter;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cache.Cache;
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
	private ServletContext sc;
	private SepoaPDFRenderer renderer;
	private SepoaImageCapture capture;
	private File pdfFile;
	private String fixedMsg;

	public SepoaImageProcessor(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		init();
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
	
	public void setCustomCss(String cssStyle) {
		this.capture.setCssStyle(cssStyle);
	}
	
	public void setFixedMsg(String fixedMsg) {
		this.fixedMsg = fixedMsg;
	}
	
	public File process(Map<Integer, String> fixHtmlMap) throws Exception {
		if(this.pdfFile == null) {
			throw new Exception("PDF File is not set.");
		}
		
		this.renderer = new SepoaPDFRenderer(this.pdfFile);
		PDDocument doc = this.renderer.getPDFDoc();
		
		if(this.fixedMsg != null && !"".equals(this.fixedMsg)) {
			for(int i = 0; i < doc.getNumberOfPages(); i++) {
				String value = MapUtils.getString(fixHtmlMap, i, "") + System.lineSeparator() + this.fixedMsg;
				fixHtmlMap.put(i, value);
			}
		}
		
		int pagNo = 0;
		Map<Integer, Map<Data, String>> data = new HashMap<>();
		for(Entry<Integer, String> html : fixHtmlMap.entrySet()) {
			pagNo ++;
			String pagTotalNo = pagNo+"/"+fixHtmlMap.size();  
			String publishNoHtml = "<div class=\"footer\" style=\"position:absolute;bottom:25px;width:100%;font-size:13px;z-index:100;display:flex;justify-content:center;text-align:center;\"><div style=\"padding:10px;color:#999;\">"+pagTotalNo+"</div></div>";
			
			Map<Data, String> fixhtml = new HashMap<>(); 
			 
			fixhtml.put(SepoaImageCapture.Data.HTML, html.getValue()+publishNoHtml);
			PDRectangle cropBox = doc.getPage(html.getKey()).getCropBox();
			//fixhtml.put(SepoaImageCapture.Data.HTML, publishNoHtml); 
			fixhtml.put(SepoaImageCapture.Data.WIDTH, String.valueOf(cropBox.getWidth()));
			fixhtml.put(SepoaImageCapture.Data.HEIGHT, String.valueOf(cropBox.getHeight())); 
			
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
