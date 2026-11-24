package sepoa.svc.batch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.google.gson.Gson;

import sepoa.fw.util.CommonUtil;

public class DateTest {
	public static void main(String[] args) throws Exception{
		String eplogintime = "20230413092600";
		SimpleDateFormat Format = new SimpleDateFormat("yyyyMMddHHmmss");
        Format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date sDate = Format.parse(eplogintime); // sso 인증 시간
		
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")); 
        Date LoginDate =  cal.getTime(); 
        long diffTime =(LoginDate.getTime() - sDate.getTime()) / 1000;
        long stdTime = 10;
        
        System.out.println(LoginDate);
        System.out.println(sDate);
        System.out.println(diffTime);
        
        if(diffTime > stdTime) {
        	System.out.println(true);
        } else {
        	System.out.println(false);
        }
        
	}
	
	public static void main332(String[] args) throws Exception{
		String json = "{A:1.5, B:1}";
		Map<String, String> jsonMap = new Gson().fromJson(json, Map.class);
		
		System.out.println(jsonMap);
		
		Map<String, Object> tmpMap = new HashMap<>();
		tmpMap.putAll(jsonMap);
		Double b = (Double) tmpMap.get("B");
		
		System.out.println(b.intValue());
		
	}
	
	public static void main232(String[] args) throws Exception{
		List<Map<String, String>> dataList = new ArrayList<>();
		
		Map<String, String> data1 = new HashMap<String, String>();
		data1.put("name", "choi");
		data1.put("add_date", "20230401");
		data1.put("age", "41");
		dataList.add(data1);
		
		Map<String, String> data2 = new HashMap<String, String>();
		data2.put("name", "baek");
		data2.put("add_date", "20230403");
		data2.put("age", "31");
		dataList.add(data2);
		
		Map<String, String> data3 = new HashMap<String, String>();
		data3.put("name", "kim");
		data3.put("add_date", "20230404");
		data3.put("age", "32");
		dataList.add(data3);
		
		Map<String, String> data4 = new HashMap<String, String>();
		data4.put("name", "park");
		data4.put("add_date", "20230405");
		data4.put("age", "33");
		dataList.add(data4);
		
		Map<String, String> data5 = new HashMap<String, String>();
		data5.put("name", "um");
		data5.put("add_date", "20230405");
		data5.put("age", "37");
		dataList.add(data5);
		
		Map<String, String> data6 = new HashMap<String, String>();
		data6.put("name", "lee");
		data6.put("add_date", "20230406");
		data6.put("age", "32");
		dataList.add(data6);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date stdDate = sdf.parse("20230405");
		
		for(Map<String, String> data : dataList) {
			String addDate = data.get("add_date");
			Date addDateVar = sdf.parse(addDate);
			
			if(addDateVar.after(stdDate)) {
				System.out.println(data);
			} 
		}
		
	}

}
