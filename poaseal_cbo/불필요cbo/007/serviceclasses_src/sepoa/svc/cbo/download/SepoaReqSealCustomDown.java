package sepoa.svc.cbo.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("SECL 날인요청 다운로드 커스텀 서비스")
public class SepoaReqSealCustomDown extends SepoaService{
	public SepoaReqSealCustomDown(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("SECL 날인요청 다운로드 건 보안정책 적용 메소드")
	public SepoaOut doProcess(Map<String, String> data) {
		setStatus(1);
		setFlag(true);
		
		InputStream in = null;
		
		try {
			String targetFilePath = data.get("FILE_PATH");
			File targetFile = new File(targetFilePath);
			in = CipherManager.getCipher().getInputStream(targetFile, false);
			
			String desFilePath = data.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY);
			String desFileName = info.getSession("HOUSE_CODE") + "_" + System.nanoTime() + "_" + System.currentTimeMillis() + ".pdf";
			File desFile = new File(desFilePath + "/" + desFileName);
			
			String policyFilePath = data.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY) + "/" + info.getSession("HOUSE_CODE") + "_" + System.nanoTime() + "_" + System.currentTimeMillis() + "_policy.pdf";
			File policyFile = new File(policyFilePath);
			
			FileUtils.copyInputStreamToFile(in, desFile);
			
			PDDocument doc = PDDocument.load(desFile);
			
			AccessPermission ap = new AccessPermission();
			ap.setCanModify(false);
			ap.setCanExtractContent(false);
			ap.setCanPrint(true);
			ap.setCanPrintDegraded(false);
			ap.setCanAssembleDocument(false);
			ap.setCanModifyAnnotations(false);
			ap.setCanFillInForm(false);
			ap.setCanExtractForAccessibility(false);
			ap.setReadOnly();
			
			StandardProtectionPolicy spp = new StandardProtectionPolicy(UUID.randomUUID().toString(), "", ap);
			doc.protect(spp);
			doc.save(policyFile);
			doc.close();
			
			Map<String, Object> policyFileData = new HashMap<String, Object>();
			policyFileData.put("FILE_NAME_CLIENT", data.get("FILE_NAME_CLIENT"));
			policyFileData.put("FILE_PATH", policyFile.getAbsolutePath());
			policyFileData.put("FILE_INPUT_STREAM", new FileInputStream(policyFile));
			
			setValue(policyFileData);
		} catch (Exception e) {
			setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
		}
		
		return getSepoaOut();
	}
}
