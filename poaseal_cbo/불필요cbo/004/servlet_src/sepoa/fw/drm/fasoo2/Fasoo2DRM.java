package sepoa.fw.drm.fasoo2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.fasoo.adk.packager.WorkPackager;

import sepoa.fw.drm.SepoaDRMAbstract;
import sepoa.fw.drm.SepoaDRMException;
import sepoa.fw.log.Logger;

public class Fasoo2DRM extends SepoaDRMAbstract {
	private String secureLevel = null;
	private List<Map<String, String>> rightsList = null;

	@Override
	public boolean doProcess() throws SepoaDRMException {
		if (this.destFile == null) {
			throw new SepoaDRMException("not found setting : dest file");
		}

		String mode = this.getProp("mode");
		if ("extract".equals(mode)) {
			return doExtract();
		} else if ("package".equals(mode)) {
			return doPackaging();
		} else {
			Logger.debug.println("not used drm : TYPE : " + this.type + ", DETAIL : " + this.detailType);
			try {
				FileUtils.copyFile(this.orgFile, this.destFile);
				return true;
			} catch (IOException e) {
				Logger.err.println(e);
				return false;
			}
		}
	}

	public boolean doExtract() {
		String dsdcode = config.get("sepoa.drm.fasoo.fsn");
		boolean res = doExtract(dsdcode);
		if (!res) {
			dsdcode = config.get("sepoa.drm.fasoo.fsn2");
			res = doExtract(dsdcode);
		}
		return res;
	}

	protected boolean doExtract(String dsdcode) {
//		String dsdcode = config.get("sepoa.drm.fasoo.fsn");
		String fsdinit = config.get("sepoa.drm.fasoo.path");
		String filePath = this.orgFile.getAbsolutePath();
		String extractFilePath = this.destFile.getAbsolutePath();
		boolean bret = false;

		WorkPackager oWorkPackager = new WorkPackager();
		int fileType = oWorkPackager.GetFileType(filePath);

		Logger.debug.println("Start : file extract");
		Logger.debug.println("Check > " + FileTypeStr(fileType));
		if (fileType == 103 || fileType == 26) {
			Logger.debug.println("SRC  > " + filePath);
			Logger.debug.println("DEST > " + extractFilePath);

			bret = oWorkPackager.DoExtract(fsdinit, dsdcode, filePath, extractFilePath);
			if (bret) {
				String ext = FilenameUtils.getExtension(orgFile.getName());
					File file = new File(extractFilePath + "." + ext);
					try {
						if(StringUtils.isNotBlank(ext)) {
								FileUtils.moveFile(file, destFile);
						}
					} catch (Exception e) {
						Logger.err.println("END : File extract fail : ", e);
						return true;
				}

				Logger.debug.println("END : File extract Success");
				return true;
			} else {
				Logger.err.println("END : File extract fail ( CODE : " + oWorkPackager.getLastErrorNum() + ", MSG : "
						+ oWorkPackager.getLastErrorStr() + ")");
				return false;
			}
		} else {
			Logger.debug.println("END : not supported file");
			if (this.orgFile.compareTo(this.destFile) != 0) {
				try {
					FileUtils.copyFile(this.orgFile, this.destFile);
				} catch (Exception e) {
					Logger.err.println(e);
					return false;
				}
			}
			return true;
		}
	}

	public boolean doPackaging() {
		String dsdCode = config.get("sepoa.drm.fasoo.fsn");
		String fsdinit = config.get("sepoa.drm.fasoo.path");

		String filePath = this.orgFile.getAbsolutePath();
		WorkPackager oWorkPackager = new WorkPackager();
		oWorkPackager.setOverWriteFlag(false);

		int fileType = oWorkPackager.GetFileType(filePath);

		Logger.debug.println("Start : file packaging");
		Logger.debug.println("Check > " + FileTypeStr(fileType));
		if (fileType == 29) {
			String ext = FilenameUtils.getExtension(filePath);
			File tmpFile = this.getTempFile();
			tmpFile = new File(tmpFile.getAbsolutePath() + "." + ext);
			String packFilePath = tmpFile.getAbsolutePath();
			String packFileName = tmpFile.getName();

			Logger.debug.println("SRC  > " + filePath);
			Logger.debug.println("DEST > " + packFilePath);

			this.optionParser();
			if (this.secureLevel == null) {
				this.secureLevel = config.get("sepoa.drm.fasoo.level", null);
			}

			boolean bret = false;
			if (this.secureLevel == null || this.secureLevel.isEmpty()) {
				bret = oWorkPackager.DoPackaging(fsdinit, dsdCode, filePath, packFilePath);
			} else {
				String userID = info.getSession("ID");
				String userName = info.getSession("NAME_LOC");
				String deptId = info.getSession("DEPARTMENT");
				String deptName = info.getSession("DEPARTMENT_NAME_LOC");

				bret = oWorkPackager.DoPackagingFsn2(fsdinit, dsdCode, filePath, packFilePath, packFilePath, userID,
						userName, userID, userName, deptId, deptName, userID, userName, deptId, deptName,
						this.secureLevel);
			}

			if (bret) {
				if (this.rightsList != null) {
					for (Map<String, String> data : this.rightsList) {
						bret = oWorkPackager.AddRightsFsn(fsdinit, dsdCode, packFilePath, packFilePath,
								data.get("TYPE"), data.get("ID"), data.get("NAME"), data.get("RIGHT_CODE"));
						if (!bret) {
							Logger.err.println("END : File add rights fail ( CODE : " + oWorkPackager.getLastErrorNum()
									+ ", MSG : " + oWorkPackager.getLastErrorStr() + ")");
							return false;
						}
					}
				}
				try {
					FileUtils.moveFile(tmpFile, this.destFile);
					Logger.debug.println("END : File packaging Success");
					return true;
				} catch (Exception e) {
					Logger.err.println(e);
					return false;
				}
			} else {
				Logger.err.println("END : File packaging fail ( CODE : " + oWorkPackager.getLastErrorNum() + ", MSG : "
						+ oWorkPackager.getLastErrorStr() + ")");
				return false;
			}
		} else {
			Logger.debug.println("END : not supported file");
			if (this.orgFile.compareTo(this.destFile) != 0) {
				try {
					FileUtils.copyFile(this.orgFile, this.destFile);
				} catch (Exception e) {
					Logger.err.println(e);
					return false;
				}
			}
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	private void optionParser() {
		for (Entry<String, Object> entry : this.options) {
			switch (entry.getKey()) {
			case "LEVEL":
				this.secureLevel = (String) entry.getValue();
				break;
			case "RIGHT":
				if (this.rightsList == null) {
					this.rightsList = new ArrayList<Map<String, String>>();
				}
				this.rightsList.add((Map<String, String>) entry.getValue());
				break;
			}
		}
	}

	private String FileTypeStr(int i) {
		String ret = null;
		switch (i) {
		case 20:
			ret = "파일을 찾을 수 없습니다.";
			break;
		case 21:
			ret = "파일 사이즈가 0 입니다.";
			break;
		case 22:
			ret = "파일을 읽을 수 없습니다.";
			break;
		case 29:
			ret = "암호화 파일이 아닙니다.";
			break;
		case 26:
			ret = "FSD 파일입니다.";
			break;
		case 105:
			ret = "Wrapsody 파일입니다.";
			break;
		case 106:
			ret = "NX 파일입니다.";
			break;
		case 101:
			ret = "MarkAny 파일입니다.";
			break;
		case 104:
			ret = "INCAPS 파일입니다.";
			break;
		case 103:
			ret = "FSN 파일입니다.";
			break;
		}
		return ret;
	}
}
