package sepoa.svc.iams;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.bxi.iams.client.approval.ApprovalRequest;
import com.bxi.iams.client.config.IamsConfig;
import com.bxi.iams.enums.Action;
import com.bxi.iams.enums.ApprMethod;
import com.bxi.iams.enums.ApprType;
import com.bxi.iams.enums.DistributeType;
import com.bxi.iams.xml.build.ApprovalBuilder;
import com.bxi.iams.xml.element.Approval;

import filters.ThreadLocalFilter;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.drm.fasoo2.Fasoo2DRM;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("통합결재 연계 서비스")
public class SepoaIamsService extends SepoaService {

	public SepoaIamsService(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	private void initialize() {
		synchronized (IamsConfig.class) {
			if (IamsConfig.getLegacyId() == null) {
				try {
					Config config = new Configuration();
					String iamsProperties = config.get("iams.properties.file");
					InputStream is = FileUtils.openInputStream(new File(iamsProperties));
					IamsConfig.load(is);
					IOUtils.closeQuietly(is);
				} catch (Exception e) {
					throw new RuntimeException("Iams Properties loding failed.", e);
				}
			}
		}
	}

	public String setRequestURL(Approval resAppr, String userId) throws UnsupportedEncodingException {
		HttpServletRequest request = ThreadLocalFilter.tl.get().request;

		String ssoToken = (String) request.getSession().getAttribute("ssoToken");
		String domainName = (String) request.getSession().getAttribute("domainName");
//		String ssoToken = info.getSession("ssoToken");
//		String domainName = info.getSession("domainName");

		String url = ApprovalRequest.getIamsServiceUrl(null);
		StringBuilder reqUrl = new StringBuilder();

		reqUrl.append(url);
		reqUrl.append("/requestApproval.ex?apprDocId=");
		reqUrl.append(resAppr.getHeader().getApprovalId());
		reqUrl.append("&avn=");
		reqUrl.append(resAppr.getHeader().getAvn());
		reqUrl.append("&ovn=");
		reqUrl.append(resAppr.getHeader().getOvn());
		reqUrl.append("&apvn=");
		reqUrl.append(resAppr.getHeader().getApvn());
		reqUrl.append("&ssoToken=");
		reqUrl.append(URLEncoder.encode(ssoToken, "UTF-8"));
		reqUrl.append("&domainName=");
		reqUrl.append(URLEncoder.encode(domainName, "UTF-8"));
		reqUrl.append("&_SSO_USER_ID_=");
		reqUrl.append(userId);
		return reqUrl.toString();

	}

	@SuppressWarnings("unchecked")
	@Description("통합결재 연계 서비스")
	public SepoaOut sendApproval(Map<String, Object> allData) throws Exception {

		initialize();

		String LEGACY_JOB_ID ;
		String FORM_ID ;
		final String apprIp = InetAddress.getLocalHost().getHostAddress();

		setFlag(true);
		setStatus(1);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		List<File> fileList = new ArrayList<File>();
		try {

			HttpServletRequest request = ThreadLocalFilter.tl.get().request;

			String ssoToken = (String) request.getSession().getAttribute("ssoToken");
			String domainName = (String) request.getSession().getAttribute("domainName");

			if (StringUtils.isBlank(ssoToken) || StringUtils.isBlank(domainName)) {
				throw new Exception("EP를 통해 로그인해 주시기 바랍니다.");
			}

			ConnectionContext ctx = getConnectionContext();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Logger.debug.println(" ########### headerData ######### ");
			Logger.debug.println(" == > " + headerData);
			
			SepoaFormater sfHeader = null;
			if("D".equals(headerData.get("APPLY_TYPE"))) {
				//증명서 발급신청 
				LEGACY_JOB_ID = "DO";
				FORM_ID = "DO";
				sxp = new SepoaXmlParser(this, "doSelectSSIAD_CERT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfHeader = new SepoaFormater(ssm.doSelect(headerData));
			}else if("EXT".equals(headerData.get("APPLY_TYPE"))) {
				//반출 신청 
				LEGACY_JOB_ID = "EX";
				FORM_ID = "EX";
				sxp = new SepoaXmlParser(this, "doSelectSSETL");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfHeader = new SepoaFormater(ssm.doSelect(headerData));
			}else {
				//날인 신청
				LEGACY_JOB_ID = "SE01";
				FORM_ID = "SE01";
				sxp = new SepoaXmlParser(this, "doSelectSSIAD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfHeader = new SepoaFormater(ssm.doSelect(headerData));
			}
			Config conf = new Configuration();
			ctx = getConnectionContext();

			String title = sfHeader.getValue("SUBJECT", 0);
			String empCd = info.getSession("ID").toLowerCase();

			ApprovalBuilder ab = new ApprovalBuilder(Action.TRANSMIT, title, LEGACY_JOB_ID, FORM_ID,
					ApprMethod.INTERFACE, empCd, apprIp);

			// 결재선은 통합결재에서 결정
			SepoaFormater sfAppLine = null;
			// 결재선
			System.out.println("====AppLine ");
			Logger.debug.println("APPLY_TYPE : "+headerData.get("APPLY_TYPE"));
			if ("D".equals(headerData.get("APPLY_TYPE"))) {
				//협조자 : 김지현 고정, 결재자 : 소속부서장
				sxp = new SepoaXmlParser(this, "getAppLine_D");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfAppLine = new SepoaFormater(ssm.doSelect(headerData));				
			}else {
				//협조자 : 인감 담당자, 결재자 : 소속부서장				
				sxp = new SepoaXmlParser(this, "getAppLine");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfAppLine = new SepoaFormater(ssm.doSelect(headerData));
			}

			Set<String> approvalSet = new HashSet<String>();
			Set<String> cooperationSet = new HashSet<String>();
			Set<String> distributeSet = new HashSet<String>();

			for (int i = 0; i < sfAppLine.getRowCount(); i++) {
				String appMethod = sfAppLine.getValue("APPTYPE", i);
				String appUserId = sfAppLine.getValue("EMPCD", i);
				Logger.debug.println(appMethod + " : " + appUserId);
				if ("APPROVAL".equals(appMethod) && !"".equals(appUserId)) {
					// 결재
					approvalSet.add(appUserId);
				} else if ("COOPERATION".equals(appMethod) && !"".equals(appUserId)) {
					// 협조
					cooperationSet.add(appUserId);
				} else if ("DISTRIBUTE".equals(appMethod) && !"".equals(appUserId)) {
					// 배포
					distributeSet.add(appUserId);
				}
			}

			int cnt = 1;
			for (String appUserId : cooperationSet) {
				ab.addApprovalLine(ab.createApprovalLine(cnt++, 1, ApprType.COOPERATION, appUserId));
			}
			for (String appUserId : approvalSet) {
				ab.addApprovalLine(ab.createApprovalLine(cnt++, 1, ApprType.APPROVAL, appUserId));
			}
			for (String appUserId : distributeSet) {
				ab.addDistribute(ab.createDistribute(DistributeType.INDIVIDUAL, appUserId));
			}
			
			SepoaFormater sfTarget = null;
			List<Map<String, String>> targets = null ;
			// 헤더내용
			List<Map<String, String>> headers = sfHeader.getListMap();
			if("D".equals(headerData.get("APPLY_TYPE"))) {
				//증명서 발급신청 	
				ab.addDataset("DO_HEADER", headers);
				Logger.debug.println("DO_HEADER : " + headers);
			}else if("EXT".equals(headerData.get("APPLY_TYPE"))) {
				//반출 신청
				String EXT_OUT_DATE = headers.get(0).get("EXT_OUT_EXPECT_DATE");
				String EXT_OUT_TIME = headers.get(0).get("EXT_OUT_EXPECT_TIME");
				String EXT_IN_DATE = headers.get(0).get("EXT_IN_EXPECT_DATE");
				String EXT_IN_TIME = headers.get(0).get("EXT_IN_EXPECT_TIME");
				
				String EXT_OUT = EXT_OUT_DATE.substring(0,4)+"/"+EXT_OUT_DATE.substring(4,6)+"/"+EXT_OUT_DATE.substring(6,8)+"  "+EXT_OUT_TIME.substring(0,2)+"시";
				String EXT_IN = EXT_IN_DATE.substring(0,4)+"/"+EXT_IN_DATE.substring(4,6)+"/"+EXT_IN_DATE.substring(6,8)+"  "+EXT_IN_TIME.substring(0,2)+"시";
				Logger.debug.println("EXT_OUT ==> "+EXT_OUT);
				Logger.debug.println("EXT_IN ==> "+EXT_IN);
				headers.get(0).put("EXT_OUT_EXPECT_DATE", EXT_OUT);
				headers.get(0).put("EXT_IN_EXPECT_DATE", EXT_IN);
				
				ab.addDataset("EX_HEADER", headers);
				sxp = new SepoaXmlParser(this, "doSelectSSEDC");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfTarget = new SepoaFormater(ssm.doSelect(headerData));
				targets = sfTarget.getListMap();
				ab.addDataset("EX_ADDITIONAL_DOC_DETAIL", targets);
			}else {
				//날인 및 교부서류 신청
				ab.addDataset("SM_HEADER", headers);
				sxp = new SepoaXmlParser(this, "doSelectSSIPD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sfTarget = new SepoaFormater(ssm.doSelect(headerData));
				targets = sfTarget.getListMap();
				ab.addDataset("SM_DOC_DETAIL", targets);
			}

			sxp = new SepoaXmlParser(this, "doSelectSSRAD");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sfAdditional = new SepoaFormater(ssm.doSelect(headerData));

			List<Map<String, String>> additionals = sfAdditional.getListMap();
			if("D".equals(headerData.get("APPLY_TYPE"))) {
				//증명서 발급신청
				ab.addDataset("DO_ADDITIONAL_DOC_DETAIL", additionals);
			}else if(!"EXT".equals(headerData.get("APPLY_TYPE"))){
				//날인 및 교부서류 신청
				ab.addDataset("SM_ADDITIONAL_DOC_DETAIL", additionals);				
			}
			
			if("EXT".equals(headerData.get("APPLY_TYPE"))) {
				
				sxp = new SepoaXmlParser(this, "doSearchAttchNo");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfext = new SepoaFormater(ssm.doSelect(headerData));
				ArrayList<String> AnoAll = new ArrayList<String>();
				String Val = null;
				if(sfext.getRowCount() > 1) {
					for(int i = 0; i < sfext.getRowCount(); i++) {
						AnoAll.add(sfext.getValue("ATTACH_NO",i));
					}
					Val= StringUtils.join(AnoAll, ',');
				}else {
					Val= sfext.getValue("ATTACH_NO",0);
				}
				headerData.put("DOC_VALUE", Val);
				
				sxp = new SepoaXmlParser(this, "doSearchFile");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfext_val = new SepoaFormater(ssm.doSelect(headerData));
				
				for (int i = 0; i < sfext_val.getRowCount(); i++) {
					String fileNameClient = sfext_val.getValue("SRC_FILE_NAME", i);
					String fileNameServer = "/"+sfext_val.getValue("DES_FILE_NAME", i);
					
					File file = null;
					
					String ext = FilenameUtils.getExtension(fileNameClient);
					
/*					if ("pdf".equalsIgnoreCase(ext)) {
						File orgFile = new File(conf.get("sepoa.attach.download"),
								sfext_val.getValue("TYPE", i) + fileNameServer);
						file = new File(conf.get("sepoa.attach.download"),
								sfext_val.getValue("TYPE", i) + fileNameServer + ".pdf");
						FileUtils.copyFile(orgFile, file);
						CipherManager.decrypt(file, true);
						fileList.add(file);
						
						Fasoo2DRM drm = new Fasoo2DRM();
						drm.setSession(info);
						drm.setConfig(conf);
		
						File destFile = new File(FileUtils.getTempDirectory() + "/package_" + file.getName());
		
						Logger.info.println("Fasoo2 > Set Original File : " + file);
						drm.setOrgFile(file);
						Logger.info.println("DRMPlugin > Set Dest File : " + destFile);
						drm.setDestFile(destFile);
		
						drm.doPackaging();
						fileList.add(destFile);
						
						ab.addAttachFile(file, fileNameClient);
					} else {*/
						File orgFile = new File(conf.get("sepoa.attach.download"),
								sfext_val.getValue("TYPE", i) + fileNameServer);
						ab.addAttachFile(orgFile, fileNameClient);
					//}
	
					/*Fasoo2DRM drm = new Fasoo2DRM();
					drm.setSession(info);
					drm.setConfig(conf);
	
					File destFile = new File(FileUtils.getTempDirectory() + "/package_" + file.getName());
	
					Logger.info.println("Fasoo2 > Set Original File : " + file);
					drm.setOrgFile(file);
					Logger.info.println("DRMPlugin > Set Dest File : " + destFile);
					drm.setDestFile(destFile);
	
					drm.doPackaging();*/
				}
			}else {
				// Attachements
				sxp = new SepoaXmlParser(this, "doSelectSFMFI");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfAttach = new SepoaFormater(ssm.doSelect(headerData));
	
				for (int i = 0; i < sfAttach.getRowCount(); i++) {
					String fileNameClient = sfAttach.getValue("FILE_NAME_CLIENT", i);
					String fileNameServer = sfAttach.getValue("FILE_NAME_SERVER", i);
	
					File file = null;
	
					String ext = FilenameUtils.getExtension(fileNameClient);
					if ("pdf".equalsIgnoreCase(ext)) {
						File orgFile = new File(conf.get("sepoa.attach.download"),
								sfAttach.getValue("LAYER", i) + fileNameServer);
						file = new File(conf.get("sepoa.attach.download"),
								sfAttach.getValue("LAYER", i) + fileNameServer + ".pdf");
						FileUtils.copyFile(orgFile, file);
						CipherManager.decrypt(file, true);
						fileList.add(file);
					} else {
						file = new File(conf.get("sepoa.attach.download"),
								sfAttach.getValue("LAYER", i) + fileNameServer + "." + ext);
					}
	
					Fasoo2DRM drm = new Fasoo2DRM();
					drm.setSession(info);
					drm.setConfig(conf);
	
					File destFile = new File(FileUtils.getTempDirectory() + "/package_" + file.getName());
	
					Logger.info.println("Fasoo2 > Set Original File : " + file);
					drm.setOrgFile(file);
					Logger.info.println("DRMPlugin > Set Dest File : " + destFile);
					drm.setDestFile(destFile);
	
					drm.doPackaging();
					fileList.add(destFile);
	
					ab.addAttachFile(destFile, fileNameClient);
				}
			}
			// 6. 결재 데이터 결재서버로 전송
			Approval r = ab.send(ab.getApproval());
			if ("SUCCESS".equals(r.getHeader().getActionResult())) {
				String iamsDocNo = r.getHeader().getDocumentNo();

				if (StringUtils.isNotEmpty(iamsDocNo) && "EXT".equals(headerData.get("APPLY_TYPE"))) {
					headerData.put("APVID", iamsDocNo);
					sxp = new SepoaXmlParser(this, "updateSSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(headerData);
					Commit();
				}else {
					headerData.put("APPR_DOC_NO", iamsDocNo);
					sxp = new SepoaXmlParser(this, "updateSSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(headerData);
					Commit();
				}
			}

			String url = setRequestURL(r, empCd);
			Logger.debug.println("URL -> " + url);
			setValue(url);

		} catch (Throwable exception) {
			Rollback();
			// exception.printStackTrace();
			setFlag(false);
			setStatus(0);
			setMessage(exception.getMessage());
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(this.getClass().getSimpleName()).append(" ERROR : ")
					.append(exception.getMessage()).toString());
		} finally {
			for (File file : fileList) {
				FileUtils.forceDelete(file);
			}
		}

		return getSepoaOut();
	}
}
