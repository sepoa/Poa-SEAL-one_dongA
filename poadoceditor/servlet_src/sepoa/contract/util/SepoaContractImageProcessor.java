package sepoa.contract.util;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;

import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import filters.ThreadLocalFilter;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.log.Logger;
import sepoa.fw.module.SepoaModuleManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.converter.SepoaConverter;
import sepoa.fw.util.converter.SepoaConverter.OPTION;
import sepoa.fw.util.ux.sUtils;

public class SepoaContractImageProcessor extends SepoaModuleManager {
	protected String BASE_TEMPLATE;
	private ServletContext sc;

	public SepoaContractImageProcessor(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		initTemplate();
	}

	public String mergeHtml(Map<String, String> fileInfo) {
		File origin = new PathManager(info, config).getFile(fileInfo);
		InputStream is = null;
		try {
			is = CipherManager.getCipher().getInputStream(origin, false);
			String base64Image = DatatypeConverter.printBase64Binary(IOUtils.toByteArray(is));
			String htmlString = BASE_TEMPLATE.replace("#IMAGE_PATH#", "data:image/png;base64," + base64Image);
			htmlString = htmlString.replace("#FIX_HTML#", Matcher.quoteReplacement(fileInfo.get("FIX_HTML")).toString());
			String imageOrientation = fileInfo.get("IMAGE_ORIENTATION");
			
			if(StringUtils.isBlank(imageOrientation)) {
				imageOrientation = "V";
			}
			
			SepoaConverter sc = new SepoaConverter();
			sc.setTempDirectory(tempStorage);
			if("H".equals(imageOrientation)) {
				htmlString = htmlString.replace("#PAGE_STYLE#", "@page { size: A4 landscape !important; margin:0; }");
			} else {
				htmlString = htmlString.replace("#PAGE_STYLE#", "@page { size: A4 !important; margin:0; }");
			}
			sc.setSource(htmlString);
			
			if(config.getBoolean("sepoa.contract.debug.flag", false)){
				PathManager pm = new PathManager(info, config);
				pm.setLayer("CONT_DEV_HTML");
				String layerPath = pm.getLayerPath();
				String detailPath = sUtils.string.dataReplacer("$SESSION{HOUSE_CODE}/$CURRENT{YEAR}/$CURRENT{MONTH}/$CURRENT{DAY}");
				File dir = new File(layerPath+"/"+detailPath);
				if(!dir.exists()) {
					dir.mkdirs();
				}
				String tempHtmlName = String.valueOf(System.nanoTime()) + "_" + fileInfo.get("FILE_ID") + "_" + fileInfo.get("PART_ID")+".html";
				FileUtils.writeStringToFile(new File(layerPath+"/"+detailPath+"/"+tempHtmlName), htmlString);
			}
			
			File[] file = sc.getImageFiles();
			return file[0].getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {

				}
			}
		}
		return null;
	}

	private void initTemplate(){
		try {
//		String docBase = ThreadLocalFilter.getHttpServletRequest().getServletContext().getRealPath("/").replaceAll("\\\\", "/");
		String docBase = "";
		try {
			sc = ThreadLocalFilter.getHttpServletRequest().getServletContext();
			if(sc == null) {
				throw new Exception("ThreadLocalFilter HttpServlertRequest ServletContext is Null. Make Custom ServletContext.");
			}
		} catch (Exception e) {
			Logger.err.println(e);
			sc = new SepoaCustomServletContext();
		}
		docBase = sc.getRealPath("/").replaceAll("\\\\", "/");
		File resource = new File(docBase + CommonUtil.getConfig("sepoa.contract.resource"));
		
		String css = "";
		String html = "";
		for (File r : resource.listFiles()) {
			String ext = FilenameUtils.getExtension(r.getName());
			if ("css".equals(ext.toLowerCase())) {
				css += new String(Files.readAllBytes(r.toPath()), "UTF-8");
			} else if ("html".equals(ext)) {
				html = new String(Files.readAllBytes(r.toPath()), "UTF-8");
			}
		}

		html = html.replace("#CSS_CONTENTS#", css);
		html = html.replace("#DOC_BASE#", docBase);
		BASE_TEMPLATE = html;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
	}
}
