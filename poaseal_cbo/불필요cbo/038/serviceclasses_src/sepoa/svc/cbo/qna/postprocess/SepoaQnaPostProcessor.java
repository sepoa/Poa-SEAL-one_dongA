package sepoa.svc.cbo.qna.postprocess;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.util.SepoaMultipart;

@Description("QnA 등록 후처리 프로세서")
public class SepoaQnaPostProcessor extends SepoaService {
	public SepoaQnaPostProcessor(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("ST(038) QnA 댓글 등록 후처리 — 담당자(MANAGER) ↔ 기안자 상호 알림")
	public SepoaOut doReplyProcess(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Configuration conf = new Configuration();
			String sessionId = info.getSession("ID");

			sxp = new SepoaXmlParser(this, "doSelectReplyNotifyInfo");
			ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() <= 0) {
				Logger.err.println(sessionId, this, "QnA reply mail skip: document not found. QNA_NUMBER="
						+ (headerData != null ? headerData.get("QNA_NUMBER") : ""));
				return getSepoaOut();
			}

			Map<String, String> qnaInfo = sf.getListMap().get(0);
			String writerId = qnaInfo.get("WRITER_ID");
			String writerName = qnaInfo.get("WRITER_NAME");
			String writerEmail = qnaInfo.get("WRITER_EMAIL");
			String managerId = qnaInfo.get("MANAGER_ID");
			String managerName = qnaInfo.get("MANAGER_NAME");
			String managerEmail = qnaInfo.get("MANAGER_EMAIL");

			String receiverId = null;
			String receiverName = null;
			String receiverEmail = null;

			// 담당자 댓글 → 기안자 / 기안자 댓글 → 담당자 (SQNAQ.MANAGER 기준)
			if (managerId != null && !"".equals(managerId.trim()) && sessionId.equals(managerId)) {
				receiverId = writerId;
				receiverName = writerName;
				receiverEmail = writerEmail;
			} else if (writerId != null && sessionId.equals(writerId)) {
				if (managerId == null || "".equals(managerId.trim())) {
					Logger.debug.println(sessionId, this,
							"QnA reply mail skip: manager not assigned yet. QNA_NUMBER=" + qnaInfo.get("QNA_NUMBER"));
					return getSepoaOut();
				}
				receiverId = managerId;
				receiverName = managerName;
				receiverEmail = managerEmail;
			} else {
				Logger.debug.println(sessionId, this,
						"QnA reply mail skip: commenter is neither writer nor manager. QNA_NUMBER="
								+ qnaInfo.get("QNA_NUMBER"));
				return getSepoaOut();
			}

			if (receiverId == null || "".equals(receiverId.trim())
					|| receiverEmail == null || "".equals(receiverEmail.trim())) {
				Logger.err.println(sessionId, this,
						"QnA reply mail skip: receiver email missing. receiverId=" + receiverId
								+ ", QNA_NUMBER=" + qnaInfo.get("QNA_NUMBER"));
				return getSepoaOut();
			}

			SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat fromTimeFormat = new SimpleDateFormat("HHmmss");
			SimpleDateFormat toTimeFormat = new SimpleDateFormat("HH:mm");

			Map<String, String> mailData = new HashMap<String, String>();
			String subjectTitle = headerData.get("SUBJECT");
			if (subjectTitle == null || "".equals(subjectTitle.trim())) {
				subjectTitle = qnaInfo.get("SUBJECT");
			}
			mailData.put("SUBJECT", Cache.text.get("MAIL.QNA_REPLY_MAIL_TILE"));
			mailData.put("MAIL_SUBJECT", Cache.text.get("MAIL.QNA_REPLY_MAIL_HEADER"));
			mailData.put("MAIL_TITLE", Cache.text.get("MAIL.QNA_REPLY_TITLE"));
			mailData.put("CONTENTS", Cache.text.get("MAIL.QNA_REPLY_CONTENTS"));
			mailData.put("CONTENTS_TITLE", subjectTitle);
			mailData.put("QNA_NUMBER", qnaInfo.get("QNA_NUMBER"));
			mailData.put("ADD_DATE", toDateFormat.format(fromDateFormat.parse(qnaInfo.get("ADD_DATE"))));
			mailData.put("ADD_TIME", toTimeFormat.format(fromTimeFormat.parse(qnaInfo.get("ADD_TIME"))));
			mailData.put("URL", CommonUtil.getConfig("sepoa.system.domain.name") + "/sso/business.jsp");

			SepoaPushManager mailpush = new SepoaPushManager(info, conf);
			SepoaMessage message = new SepoaMessage();
			message.addParams(mailData);
			message.setMessageType("QNA_REPLAY_ALARM");
			message.addReceiver(receiverId, receiverName, receiverEmail, null);

			if (headerData.containsKey("ATTACH_NO")) {
				sxp = new SepoaXmlParser(this, "doSelectAttachFile");
				ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData));
				if (sf.getRowCount() > 0) {
					for (Map<String, String> fileData : sf.getListMap()) {
						String tmpType = fileData.get("TYPE");
						String tmpFileRootPath = SepoaMultipart.getTypePath(tmpType, "upload");
						String tmpDesFilePath = fileData.get("DES_FILE_NAME");
						String tmpSrcFileName = fileData.get("SRC_FILE_NAME");
						File tmpFile = new File(tmpFileRootPath + "/" + tmpDesFilePath);
						if (tmpFile.exists()) {
							message.addAttach(new FileInputStream(tmpFile), tmpSrcFileName);
						}
					}
				}
			}

			mailpush.addMessage(message);
			mailpush.accept();
		} catch (Exception e) {
			if (getStatus() == 1)
				setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}

	@Description("ST: 이견없음/검토요청 확정 시 법무팀장(QnA_MASTER) 메일")
	public SepoaOut doFinalizeMasterProcess(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Configuration conf = new Configuration();

			sxp = new SepoaXmlParser(this, "doSelectQnaMaster");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() <= 0) {
				Logger.err.println(info.getSession("ID"), this,
						"QnA finalize mail skip: QnA_MASTER not found. QNA_NUMBER=" + headerData.get("QNA_NUMBER"));
				return getSepoaOut();
			}
			List<Map<String, String>> masterList = sf.getListMap();

			sxp = new SepoaXmlParser(this, "doSelectQnaData");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() <= 0) {
				Logger.err.println(info.getSession("ID"), this,
						"QnA finalize mail skip: document not found. QNA_NUMBER=" + headerData.get("QNA_NUMBER"));
				return getSepoaOut();
			}
			Map<String, String> legalData = sf.getListMap().get(0);

			SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat fromTimeFormat = new SimpleDateFormat("HHmmss");
			SimpleDateFormat toTimeFormat = new SimpleDateFormat("HH:mm");

			String finalizeStatus = headerData.get("STATUS");
			String statusLabel = "A".equals(finalizeStatus) ? "검토요청" : "이견없음";

			for (Map<String, String> master : masterList) {
				if (master.get("EMAIL") == null || "".equals(master.get("EMAIL").trim())) {
					continue;
				}
				SepoaMessage message = new SepoaMessage();
				SepoaPushManager mailpush = new SepoaPushManager(info, conf);
				Map<String, String> mailData = new HashMap<String, String>();

				mailData.put("SUBJECT", "[법무검토] " + statusLabel + " 안내");
				mailData.put("MAIL_TITLE", Cache.text.get("MAIL.QNA_WRITE_TITLE"));
				mailData.put("CONTENTS", "담당자가 '" + statusLabel + "'을(를) 선택하였습니다. 완료여부를 확인해 주세요.");
				mailData.put("QNA_NUMBER", legalData.get("QNA_NUMBER"));
				mailData.put("CONTENTS_TITLE", legalData.get("SUBJECT"));
				mailData.put("ADD_DATE", toDateFormat.format(fromDateFormat.parse(legalData.get("ADD_DATE"))));
				mailData.put("ADD_TIME", toTimeFormat.format(fromTimeFormat.parse(legalData.get("ADD_TIME"))));
				mailData.put("URL", CommonUtil.getConfig("sepoa.system.domain.name") + "/sso/business.jsp");

				message.addParams(mailData);
				message.setMessageType("QNA_WRITE_ALARM");
				message.addReceiver(master.get("USER_ID"), master.get("USER_NAME_LOC"), master.get("EMAIL"), null);
				mailpush.addMessage(message);
				mailpush.accept();
			}
		} catch (Exception e) {
			if (getStatus() == 1)
				setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}

	@Description("ST: 팀장 최종완료(CC) 시 기안자/지정담당자 완료 메일")
	public SepoaOut doCompleteProcess(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Configuration conf = new Configuration();
			String sessionId = info.getSession("ID");

			sxp = new SepoaXmlParser(this, "doSelectReplyNotifyInfo");
			ssm = new SepoaSQLManager(sessionId, this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() <= 0) {
				Logger.err.println(sessionId, this,
						"QnA complete mail skip: document not found. QNA_NUMBER="
								+ (headerData != null ? headerData.get("QNA_NUMBER") : ""));
				return getSepoaOut();
			}

			Map<String, String> qnaInfo = sf.getListMap().get(0);
			String qnaNumber = qnaInfo.get("QNA_NUMBER");

			SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat fromTimeFormat = new SimpleDateFormat("HHmmss");
			SimpleDateFormat toTimeFormat = new SimpleDateFormat("HH:mm");

			Map<String, String> mailData = new HashMap<String, String>();
			mailData.put("SUBJECT", "[법무검토] 검토완료 안내");
			mailData.put("MAIL_TITLE", Cache.text.get("MAIL.QNA_WRITE_TITLE"));
			mailData.put("CONTENTS", "법무검토가 완료되었습니다.");
			mailData.put("QNA_NUMBER", qnaNumber);
			mailData.put("CONTENTS_TITLE", qnaInfo.get("SUBJECT"));
			mailData.put("ADD_DATE", toDateFormat.format(fromDateFormat.parse(qnaInfo.get("ADD_DATE"))));
			mailData.put("ADD_TIME", toTimeFormat.format(fromTimeFormat.parse(qnaInfo.get("ADD_TIME"))));
			mailData.put("URL", CommonUtil.getConfig("sepoa.system.domain.name") + "/sso/business.jsp");

			Map<String, Map<String, String>> receivers = new HashMap<String, Map<String, String>>();
			addCompleteReceiver(receivers, qnaInfo.get("WRITER_ID"), qnaInfo.get("WRITER_NAME"),
					qnaInfo.get("WRITER_EMAIL"), sessionId);
			addCompleteReceiver(receivers, qnaInfo.get("MANAGER_ID"), qnaInfo.get("MANAGER_NAME"),
					qnaInfo.get("MANAGER_EMAIL"), sessionId);

			if (receivers.isEmpty()) {
				Logger.err.println(sessionId, this,
						"QnA complete mail skip: no valid receiver. QNA_NUMBER=" + qnaNumber);
				return getSepoaOut();
			}

			for (Map<String, String> receiver : receivers.values()) {
				SepoaMessage message = new SepoaMessage();
				SepoaPushManager mailpush = new SepoaPushManager(info, conf);
				message.addParams(mailData);
				message.setMessageType("QNA_WRITE_ALARM");
				message.addReceiver(receiver.get("USER_ID"), receiver.get("USER_NAME"), receiver.get("EMAIL"), null);
				mailpush.addMessage(message);
				mailpush.accept();
			}
		} catch (Exception e) {
			if (getStatus() == 1)
				setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}

	private void addCompleteReceiver(Map<String, Map<String, String>> receivers, String userId, String userName,
			String email, String excludeUserId) {
		if (userId == null || "".equals(userId.trim())) {
			return;
		}
		if (excludeUserId != null && excludeUserId.equals(userId)) {
			return;
		}
		if (email == null || "".equals(email.trim())) {
			Logger.err.println(info.getSession("ID"), this,
					"QnA complete mail skip receiver email missing. userId=" + userId);
			return;
		}
		if (receivers.containsKey(userId)) {
			return;
		}
		Map<String, String> receiver = new HashMap<String, String>();
		receiver.put("USER_ID", userId);
		receiver.put("USER_NAME", userName);
		receiver.put("EMAIL", email);
		receivers.put(userId, receiver);
	}

	@Description("동아제약 QnA 등록 후처리 프로세서")
	public SepoaOut doWriteProcess(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Configuration conf = new Configuration();

			sxp = new SepoaXmlParser(this, "doSelectQnaManager");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));

			if (sf.getRowCount() > 0) {
				List<Map<String, String>> qnaManagerList = sf.getListMap();
				// QnA_MANAGER + QnA_MANAGER_D 동시 보유 시 중복 발송 방지
				Set<String> sentUserIds = new HashSet<String>();

				SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
				SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");

				SimpleDateFormat fromTimeFormat = new SimpleDateFormat("HHmmss");
				SimpleDateFormat toTimeFormat = new SimpleDateFormat("HH:mm");

				for (Map<String, String> qnaManager : qnaManagerList) {
					String receiverId = qnaManager.get("USER_ID");
					if (receiverId == null || "".equals(receiverId.trim()) || sentUserIds.contains(receiverId)) {
						continue;
					}
					if (qnaManager.get("EMAIL") == null || "".equals(qnaManager.get("EMAIL").trim())) {
						continue;
					}
					sentUserIds.add(receiverId);

					SepoaMessage message = new SepoaMessage();

					sxp = new SepoaXmlParser(this, "doSelectQnaData");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(headerData));

					if (sf.getRowCount() > 0) {
						Map<String, String> legalData = sf.getListMap().get(0);

						// 메일 발송 진행
						SepoaPushManager mailpush = new SepoaPushManager(info, conf);
						Map<String, String> mailData = new HashMap<String, String>();

						mailData.put("SUBJECT", Cache.text.get("MAIL.QNA_WRITE_MAIL_TITLE"));
						mailData.put("MAIL_TITLE", Cache.text.get("MAIL.QNA_WRITE_TITLE"));
						mailData.put("CONTENTS", Cache.text.get("MAIL.QNA_WRITE_CONTENTS"));
						mailData.put("QNA_NUMBER", legalData.get("QNA_NUMBER"));
						mailData.put("CONTENTS_TITLE", legalData.get("SUBJECT"));
						mailData.put("ADD_DATE", toDateFormat.format(fromDateFormat.parse(legalData.get("ADD_DATE"))));
						mailData.put("ADD_TIME", toTimeFormat.format(fromTimeFormat.parse(legalData.get("ADD_TIME"))));
						mailData.put("URL", CommonUtil.getConfig("sepoa.system.domain.name") + "/sso/business.jsp");

						message.addParams(mailData);
						message.setMessageType("QNA_WRITE_ALARM");
						message.addReceiver(qnaManager.get("USER_ID"), qnaManager.get("USER_NAME_LOC"),
								qnaManager.get("EMAIL"), null);

						if (headerData.containsKey("ATTACH_NO")) {
							sxp = new SepoaXmlParser(this, "doSelectAttachFile");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							sf = new SepoaFormater(ssm.doSelect(headerData));

							if (sf.getRowCount() > 0) {
								for (Map<String, String> fileData : sf.getListMap()) {
									String tmpType = fileData.get("TYPE");
									String tmpFileRootPath = SepoaMultipart.getTypePath(tmpType, "upload");
									String tmpDesFilePath = fileData.get("DES_FILE_NAME");
									String tmpSrcFileName = fileData.get("SRC_FILE_NAME");
									File tmpFile = new File(tmpFileRootPath + "/" + tmpDesFilePath);
									if (tmpFile.exists()) {
										message.addAttach(new FileInputStream(tmpFile), tmpSrcFileName);
									}
								}
							}
						}

						mailpush.addMessage(message);
						mailpush.accept();
					} else {
						setStatus(120001);
						throw new Exception("Not Found Legal Check Data!");
					}
				}
			} else {
				setStatus(120002);
				throw new Exception("Not Found Legal Check Manager!");
			}
		} catch (Exception e) {
			if (getStatus() == 1)
				setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
}
