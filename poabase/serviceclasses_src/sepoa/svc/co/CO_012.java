package sepoa.svc.co;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sepoa.fw.cache.Cache;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.ParamSql;
import sepoa.fw.db.SepoaConnectionInfo;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("공통코드")
public class CO_012 extends SepoaService {
	public CO_012(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@SuppressWarnings("unchecked")
	@Description("공통팝업 정보조회")
	public SepoaOut getCodeInfo(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, String> _tmpData = new HashMap<String, String>();
			_tmpData.put("LANGUAGE", "KO");
			_tmpData.put("HOUSE_CODE", CommonUtil.DEFAULT_HOUSE_CODE);
			_tmpData.put("DEFAULT_LANG", "KO");
			_tmpData.put("DEFAULT_HOUSE_CODE", CommonUtil.DEFAULT_HOUSE_CODE);
			
			String lang = info.getSession("LANGUAGE");
			if(lang != null && !lang.isEmpty()) {
				_tmpData.put("LANGUAGE", lang+","+_tmpData.get("LANGUAGE"));
				_tmpData.put("DEFAULT_LANG", lang);
			}
			
			String houseCode =info.getSession("HOUSE_CODE");
			if(houseCode != null && !houseCode.isEmpty()) {
				_tmpData.put("HOUSE_CODE", houseCode+","+_tmpData.get("HOUSE_CODE"));
				_tmpData.put("DEFAULT_HOUSE_CODE", houseCode);
			}
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData, _tmpData));
			setStatus(1);
			setMessage(Cache.text.get("MESSAGE.0001"));
		} catch (Exception e) {
			setStatus(0);
			setMessage(Cache.text.get("MESSAGE.1002"));
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
	

	@Description("데이터 조회")
	public SepoaOut getCodeSearch(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		PreparedStatement ps = null;
		
		try {
			@SuppressWarnings("unchecked")
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			Map<String, String> _tmpData = new HashMap<String, String>();
			_tmpData.put("LANGUAGE", "KO");
			_tmpData.put("HOUSE_CODE", CommonUtil.DEFAULT_HOUSE_CODE);
			_tmpData.put("DEFAULT_LANG", "KO");
			_tmpData.put("DEFAULT_HOUSE_CODE", CommonUtil.DEFAULT_HOUSE_CODE);
			
			String lang = info.getSession("LANGUAGE");
			if(lang != null && !lang.isEmpty()) {
				_tmpData.put("LANGUAGE", lang+","+_tmpData.get("LANGUAGE"));
				_tmpData.put("DEFAULT_LANG", lang);
			}
			
			String houseCode =info.getSession("HOUSE_CODE");
			if(houseCode != null && !houseCode.isEmpty()) {
				_tmpData.put("HOUSE_CODE", houseCode+","+_tmpData.get("HOUSE_CODE"));
				_tmpData.put("DEFAULT_HOUSE_CODE", houseCode);
			}
			
			sxp = new SepoaXmlParser(this, "getCodeQuery");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData, _tmpData));
			
			String query = sf.getValue("TEXT4", 0);

			int qCount = 0;
			for (int index = 0; index < query.length(); index++) {
				if (query.charAt(index) == '?') {
					qCount++;
				}
			}

			String[] values = headerData.get("param").split(",");
			
			
			Logger.dbwrap.println("----------------------------------------------------------------------------------------------------------------");
			Logger.dbwrap.println("Common Popup CODE : "+headerData.get("CODE"));
			Logger.dbwrap.println("----------------------------------------------------------------------------------------------------------------");
			
			for (int i = 0; i < values.length; i++) {
				Logger.dbwrap.println("recvData["+i+"]='"+values[i]+"'");
			}
			
			Connection conn = ctx.getConnection();
			ps = conn.prepareStatement(query);
			for (int i = 0; i < qCount; i++) {
				String p = "";
				if(values.length > i) {
					p = values[i];
				}
				ps.setString(i + 1, p);
				Logger.dbwrap.println("setString["+i+"]='"+p+"'");
			}
			
			ResultSet rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			
			List<Map<String, String>> result = new LinkedList<Map<String, String>>();
			while (rs.next()) {
				Map<String, String> row = new LinkedHashMap<String, String>();
				for (int i = 0; i < columnCount; i++) {
					row.put("TEXT"+String.valueOf(i), rs.getString(i + 1));
				}
				result.add(row);
			}
			
			// TODO : 추후 SepoaSQLManager 개선 후 쿼리 문자열 정렬 API 적용
			DatabaseMetaData dmd = conn.getMetaData();
			String dbMeta = "[" + dmd.getDatabaseProductName() + ":" + dmd.getURL() + "(" + dmd.getUserName() + ")]\n";
			
			StringBuilder sb = new StringBuilder(query);
			for(int i = 0; i < qCount; i++) {
				int targetIndex = sb.indexOf("?");
				String p = "''";
				if(values.length > i) {
					p = "'" + values[i] + "'";
				}
				sb.replace(targetIndex, targetIndex + 1, p);
			}
			
			Logger.dbwrap.println("Execute SQL =\n" + dbMeta + sb.toString() + "\n");

			setValue(result);
			setStatus(1);
			setMessage(Cache.text.get("MESSAGE.0001"));
		} catch (Exception e) {
			setStatus(0);
			setMessage(Cache.text.get("MESSAGE.1002"));
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		} finally {
			try {
				if(ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException e) {
				Logger.err.println(e);
			}
		}

		return getSepoaOut();
	}

	@SuppressWarnings("unchecked")
	@Description("자동완성 데이터 가져오기")
	public SepoaOut getAutoData(Map<String, Object> map) throws Exception {
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> connectionInfo = SepoaConnectionInfo.get(ctx);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) map.get(SepoaDataMapper.KEY_HEADER_DATA);
			headerData.put("HOUSE_CODE", info.getSession("HOUSE_CODE") + "," + CommonUtil.DEFAULT_HOUSE_CODE);
			headerData.put("LANGUAGE", info.getSession("LANGUAGE"));
			
			headerData.put("DEFAULT_LANG", "KO");
			headerData.put("DEFAULT_HOUSE_CODE", CommonUtil.DEFAULT_HOUSE_CODE);
			
			String mask = headerData.get("mask");
			String def  = headerData.get("default");
			
			sxp = new SepoaXmlParser(this, "getCodeQuery");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() <= 0) {
				throw new Exception("not found query data");
			}
			String queryString = sf.getValue("TEXT4", 0);
			if(queryString.endsWith("?")) {
				queryString += " ";
			}
			String[] queryArray = queryString.split("\\?");
			String[] values = headerData.get("param").split("#");
			
			ParamSql sm = new ParamSql(info.getSession("ID"), "MessageUtil", ctx);
			StringBuffer query = new StringBuffer();
			String vnd = connectionInfo.get(SepoaConnectionInfo.VENDOR);
			String topCount = CommonUtil.getConfig("sepoa.autocomplete.count", "10");
			if("MSSQL".equals(vnd.toUpperCase())) {
				query.append("SELECT TOP "+topCount+" *       \n");
			}else {
				query.append("SELECT * FROM (\n");
				query.append("SELECT A.*, ROW_NUMBER() OVER(ORDER BY A.VALUE) AS ROW_NUM__ \n");
			}
			query.append("  FROM (               \n");
			
			for(int i=0;i<queryArray.length;i++) {
				query.append(queryArray[i]);
				if(i != queryArray.length - 1) {
					query.append(sm.addFixString(" ? "));
					sm.addStringParameter((values.length > i)?values[i]:"");
				}
			}
			
			query.append(") A\n WHERE 1=1 \n");
			if(mask != null) {
				mask = mask.replaceAll("\\*", "%");
				mask = mask.replaceAll("\\?", "_");
				query.append(sm.addSelectString("AND A.TEXT like ? "));
				sm.addStringParameter("%" + mask+"%");
			}else {
				query.append(sm.addSelectString("AND A.VALUE = ? "));
				sm.addStringParameter(def);
			}
			
			query.append("\n");
			if(!"MSSQL".equals(vnd.toUpperCase())){
				query.append(") B \n");
				query.append(sm.addFixNumber("WHERE B.ROW_NUM__ <= ? "));
				sm.addNumberParameter(topCount);
				query.append("\n");
			}
			
			setValue(sm.doSelect(query.toString()));
			setStatus(1);
			setMessage(Cache.text.get("MESSAGE.0001"));
		} catch (Exception e) {
			setStatus(0);
			setMessage(Cache.text.get("MESSAGE.1002"));
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		}

		return getSepoaOut();
	}
}