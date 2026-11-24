package sepoa.svc.contract;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.tika.Tika;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Base64;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("전자서명 관리")
public class SepoaContractDigitalSign extends SepoaService {

	public SepoaContractDigitalSign(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("전자계약 메인화면 서명 데이터 조회")
	public SepoaOut doQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			//Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("전자계약 메인화면 법인서명 데이터 조회")
	public SepoaOut doQueryCorpSign(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			headerData.put("SIGN_TYPE", "A");
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("전자서명 삭제")
	public SepoaOut doDelete(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(headerData);

			Commit();
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		
		return getSepoaOut();
	}
	
	@Description("전자서명 저장")
	public SepoaOut doSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;		
		BufferedImage image = null;
		Configuration conf = null;
		
		try{
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			image = setImageTranslucent((String)headerData.get("IMG_DATA"));
			String signSeq = headerData.get("SIGN_SEQ");
			String transMode = "INSERT";
			int result = 0;
			
			if("".equals(signSeq) || signSeq == null) {
				headerData.put("SIGN_SEQ", " ");
			}
			
			conf = new Configuration();			
			sxp = new SepoaXmlParser(this, "doQueryExistSign");			
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0) {
				transMode = "UPDATE";
			}
			
			try {
				if(transMode == "INSERT") {
					sxp = new SepoaXmlParser(this, "getMaxSeq");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(headerData));
					String maxSeq = ((int) sf.getDouble("MAX_SEQ", 0) + 1) + "";
					headerData.put("SIGN_SEQ", maxSeq);
				}

				String rootPathType = "SIGN";
				String rootPath = getTypePath(rootPathType, "upload");
				String filePath = info.getSession("HOUSE_CODE") + "/" + info.getSession("COMPANY_CODE") + "/" + info.getSession("ID");
				String fileName = info.getSession("ID") + "_" + headerData.get("SIGN_TYPE") + ".png";
				String destFilePath = rootPath + "/" + filePath + "/" + RandomStringUtils.randomAlphabetic(4) + String.valueOf(System.nanoTime());
				File targetFile = new File(destFilePath);

				headerData.put("DES_FILE_NAME", destFilePath.replace(rootPath, ""));
				headerData.put("SRC_FILE_NAME", fileName);
				headerData.put("ROOT_PATH_TYPE", rootPathType);
				
				if(transMode == "INSERT") {
					sxp = new SepoaXmlParser(this, "doSave");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					result = ssm.doInsert(headerData);
				} else {
					sxp = new SepoaXmlParser(this, "doUpdate");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					result = ssm.doUpdate(headerData);
				}

				if(result == 1) {
					if(!new File(targetFile.getParent()).exists()) {
						FileUtils.forceMkdir(new File(targetFile.getParent()));
					}
					
					ImageIO.write(image, "png", targetFile);
				} else if(result == -1) {
					setStatus(-1);
					setFlag(false);
					setValue("");
				} else {
					throw new Exception("Digital Sign Save Fail!");
				}

				Commit();
				
			} catch (Exception e) {
				Rollback();
				e.printStackTrace();
				setStatus(0);
				setValue("");
				setFlag(false);
				setMessage(e.toString());
				Logger.err.println(info.getSession("ID"), this, e);
			}

		} catch(Exception e) {			
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	private BufferedImage setImageTranslucent(String imgData) {
		String imgUrl = imgData.replace("&#43;", "+");
		BufferedImage image = null;
		
		try {
			byte[] bytes = decodeBase64ToBytes(imgUrl);
			image = ImageIO.read(new ByteArrayInputStream(bytes));
			
			for(int w = 0; w < image.getWidth(); w++) {
				for(int h = 0; h < image.getHeight(); h++) {
					if(image.getRGB(w, h) == -1) {
						image.setRGB(w, h, Color.TRANSLUCENT);
					} 
				}
			}			
		} catch(Exception e) {
			e.printStackTrace();
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return image;
	}
	
	private static byte[] decodeBase64ToBytes(String imageString) throws Exception {
		return Base64.base64Decode(imageString.split(",")[1].getBytes());
	}

	@Description("서명 첨부 저장")
	public SepoaOut doSaveAttachSign(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ArrayList<File> trashFileList = new ArrayList<File>();
		File orgFile = null;
		File desFile = null;

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			String fileName = headerData.get("DES_FILE_NAME").substring(headerData.get("DES_FILE_NAME").lastIndexOf("/"), headerData.get("DES_FILE_NAME").length());
			String rootPathType = headerData.get("ROOT_PATH_TYPE");
			String desFileName = headerData.get("DES_FILE_NAME");
			String newDesFilePath = "/" + info.getSession("HOUSE_CODE") + "/" + info.getSession("COMPANY_CODE") + "/" + info.getSession("ID") + "/" + fileName;
			
			orgFile = new File(getFilePath(rootPathType, desFileName));
			desFile = new File(getFilePath(rootPathType, newDesFilePath));
			
			headerData.put("DES_FILE_NAME", newDesFilePath);

			if("".equalsIgnoreCase(headerData.get("SIGN_SEQ")) || headerData.get("SIGN_SEQ") == null) {
				headerData.put("SIGN_SEQ", " ");
			}
			
			sxp = new SepoaXmlParser(this, "doQueryExistSign");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			if(sf.getRowCount() > 0) {
				sxp = new SepoaXmlParser(this, "doUpdate");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(headerData);
			} else {
				if("".equalsIgnoreCase(headerData.get("SIGN_TYPE"))) {
					headerData.put("SIGN_TYPE", "A");
				}
				sxp = new SepoaXmlParser(this, "getMaxSeq");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData));
				String maxSeq = ((int) sf.getDouble("MAX_SEQ", 0) + 1) + "";
				headerData.put("SIGN_SEQ", maxSeq);
				
				sxp = new SepoaXmlParser(this, "doSave");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(headerData);
			}

			Commit();

			if(orgFile != null && desFile != null) {
				FileUtils.copyFile(orgFile, desFile);

				if(!desFile.exists()) {
					throw new Exception("Digital Sign Image move failed");
				} else {
					trashFileList.add(orgFile);
				}
			}

		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		} finally {
			for(File f : trashFileList) {
				if(f.exists()) {
					try {
						FileUtils.forceDelete(f);
					} catch (IOException e) {
						e.printStackTrace();
						setStatus(0);
						setValue("");
						setFlag(false);
						setMessage(e.getMessage());
						Logger.err.println(info.getSession("ID"), this, e);
					}
				}
			}
		}

		return getSepoaOut();
	}
	
	@Description("이미지 조회")
	public SepoaOut doImage(Map<String, String> headerData) {
		setStatus(1);
		setFlag(true);
		
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		
		try {
			if(!"".equalsIgnoreCase(headerData.get("DES_FILE_NAME")) &&!"".equalsIgnoreCase(headerData.get("ROOT_PATH_TYPE"))) {
				List<Map<String, String>> result = new LinkedList<Map<String, String>>();
				String fileName = headerData.get("SRC_FILE_NAME");
				File f = new File(getFilePath(headerData.get("ROOT_PATH_TYPE"), headerData.get("DES_FILE_NAME")));
				String imageRootPathType = "CONVERT";
				if(!f.exists()) {
		        	throw new Exception("File not exist");
		        }
				
				String mimeType = new Tika().detect(f);
				if(mimeType.matches("image/.*")) {
					File tempFile = new File(getFilePath(imageRootPathType, headerData.get("DES_FILE_NAME")));

                	if(!tempFile.getParentFile().exists()) {
                		FileUtils.forceMkdir(tempFile.getParentFile());
                	}
            		
            		FileUtils.copyFile(f, tempFile);
            		
            		Map<String, String> dataMap = new HashMap<String, String>();
                    dataMap.put("IMG_PAGE_NUM", "1");
                    dataMap.put("IMG_DES_FILE_NAME", tempFile.getParent().replaceAll("\\\\", "/").replace(getTypePath(imageRootPathType, "upload").replaceAll("\\\\", "/"), "") + "/" + f.getName());
                    dataMap.put("IMG_SRC_FILE_NAME", fileName+" 1/1");
                    dataMap.put("IMG_FILE_SIZE", String.valueOf(f.length()));
                    dataMap.put("IMG_ROOT_PATH_TYPE", imageRootPathType);
                    result.add(dataMap); 
				}
				
				setValue(getFilePath(headerData.get("ROOT_PATH_TYPE"), headerData.get("DES_FILE_NAME")));
			}

			setValue("CONT");
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	public static String getTypePath(String type, String mode) {
		Config conf = null;
		type = type.toUpperCase();
		try {
			conf = new Configuration();
		} catch (ConfigurationException e) {
			return CommonUtil.getConfig("sepoa.attach." + mode) + type;
		}

		return (conf.get("sepoa.attach." + mode + "." + type,
				CommonUtil.getConfig("sepoa.attach." + mode) + File.separatorChar + type)).replaceAll(Matcher.quoteReplacement(File.separator) ,"/");
	}
	
	public static String getFilePath(String rootPath, String filePath) throws Exception {
		if("".equalsIgnoreCase(rootPath) || rootPath == null) {
			throw new Exception("ROOT_PATH_TYPE column is null");
		}
		
		if(!Pattern.matches("^[/\\\\].*", filePath)) {
			filePath = "/" + filePath;
		}

    	return (getTypePath(rootPath.toUpperCase(), "upload") + filePath).replaceAll("\\\\", "/");
    }
}
