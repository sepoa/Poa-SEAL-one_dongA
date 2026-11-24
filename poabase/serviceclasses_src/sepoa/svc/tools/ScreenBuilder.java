package sepoa.svc.tools;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaXML;
import sepoa.fw.ux.builder.SepoaUXFileBuilder;
import sepoa.fw.ux.builder.SepoaUXFileReader;
import sepoa.fw.ux.builder.template.jaxb.ObjectFactory;
import sepoa.fw.ux.builder.template.jaxb.Root;

@Description("ScreenEditor")
@Permission(key="WORK_TYPE", value="Z")
public class ScreenBuilder extends SepoaService {
	private static final String TEMPLATE_PATH = "/sepoaux/template/";
	
	public static class ListMapComparator implements Comparator<Map<String, String>>{
		@Override
		public int compare(Map<String, String> o1, Map<String, String> o2) {
			return o1.get("NAME").compareTo(o2.get("NAME"));
		}
	}
	
	public ScreenBuilder(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
	
	@SuppressWarnings("unchecked")
	@Description("Save")
	public SepoaOut doSave(Map<String, Object> allData){
		setStatus(1);
		setFlag(true);
		try{
			Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			String data = headerData.get("unescaped_data");
			boolean forceFlag = MapUtils.getBoolean(headerData, "forceFlag", false);
			SepoaUXFileBuilder fb = new SepoaUXFileBuilder(info);
			SepoaOut so = fb.fromEditor(data, forceFlag);
			
			setStatus(so.status);
			setMessage(so.message);
			setFlag((so.status == 1)?true:false);
		}catch(Exception e) {
			setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            e.printStackTrace();
            Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("SaveTemplate")
	public SepoaOut doSaveTemplate(Map<String, Object> allData){
		setStatus(1);
		setFlag(true);
		try{
			Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			String data = DatatypeConverter.printBase64Binary(headerData.get("unescaped_DATA").getBytes());
			String fileName = headerData.get("NAME");
			String description = DatatypeConverter.printBase64Binary(headerData.get("DESCRIPTION").getBytes());
			String preview = headerData.get("PREVIEW_BASE64");
			String prehover = headerData.get("PREHOVER_BASE64");
			String project = headerData.get("project");
			boolean forceFlag = MapUtils.getBoolean(headerData, "forceFlag", false);
			
			String serviceRoot;
			if("FRWK".equals(project)) {
				serviceRoot = CommonUtil.getConfig("sepoa.service.root");
			}else {
				serviceRoot = CommonUtil.getConfig("sepoa.service.root."+project);
			}
			
			String templatePath = serviceRoot + TEMPLATE_PATH + fileName;
			if(!templatePath.endsWith(".xml")) {
				templatePath += ".xml";
			}
			
			File template = new File(templatePath);
			if(!forceFlag && template.exists()) {
				setStatus(2);
				setFlag(false);
				setMessage(fileName);
			}else {
				if(!template.getParentFile().exists()) {
					template.getParentFile().mkdirs();	
				}
				
				Root r = new ObjectFactory().createRoot();
				r.setPreview("\n    <![CDATA[\n        "+preview+"\n    ]]>\n    ");
				r.setData("\n    <![CDATA[\n        "+data+"\n    ]]>\n    ");
				r.setDescription("\n    <![CDATA[\n        "+description+"\n    ]]>\n    ");
				r.setPrehover("\n    <![CDATA[\n        "+prehover+"\n    ]]>\n    ");
				FileWriter fw = new FileWriter(template);
				fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				
				SepoaXML.marshall(r, fw, false);
				
				fw.close();
			}
		}catch(Exception e) {
			setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            e.printStackTrace();
            Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("Load")
	public SepoaOut doLoad(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try{
			Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			String target = headerData.get("target").trim();
			
			SepoaUXFileReader sr = new SepoaUXFileReader(target);
			setValue(sr.read());
		}catch(Exception e) {
			setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            e.printStackTrace();
            Logger.err.println(info.getSession("ID"), this,"ScreenEditor save fail", e);
		}
		return getSepoaOut();
	}
	
	@Description("do list template")
	public SepoaOut doListTemplate(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try{
			List<Map<String, String>> result = new LinkedList<Map<String, String>>();
			
			Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			String layoutPattern = headerData.get("LAYOUT_PATTERN");
			
			JAXBContext jaxbContext = JAXBContext.newInstance(Root.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			
			String[] priority = CommonUtil.getConfig("sepoa.module.priority", "FRWK").split(",");
			for(String project : priority) {
				String serviceRoot = null;
				if("FRWK".equals(project)) {
					serviceRoot = CommonUtil.getConfig("sepoa.service.root");
				}else {
					serviceRoot = CommonUtil.getConfig("sepoa.service.root."+project);
				}
				
				File templateDir = new File(serviceRoot + "/sepoaux/template/");
				if(templateDir.exists()) {
					for(File template : templateDir.listFiles()) {
						Root r = (Root) jaxbUnmarshaller.unmarshal(template);
						
						if(template.getName().indexOf(layoutPattern)<0) {continue;}
						
						Map<String, String> row = new HashMap<String, String>();
						row.put("NAME", template.getName().replace(".xml", ""));
				        row.put("DESCRIPTION", new String(DatatypeConverter.parseBase64Binary(r.getDescription()), StandardCharsets.UTF_8.name()).trim());
				        row.put("IMG", r.getPreview().trim());
				        row.put("PREHOVER", r.getPrehover().trim());
				        row.put("DATA", new String(DatatypeConverter.parseBase64Binary(r.getData()), StandardCharsets.UTF_8.name()).trim());
						
				        result.add(row);
					}
				}
			}
			
			result.sort(new ListMapComparator());
			
			Map<String, String> blank = new HashMap<String, String>();
			blank.put("NAME", "NEW");
			blank.put("DESCRIPTION" , "KO".equals(info.getSession("LANGUAGE"))?"새 프로젝트":"New Project");
			blank.put("IMG" , "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAPoAAADmCAYAAAD8+JjdAAAD20lEQVR4nO3dP2idVRyA4S9p2rRULDQ4mA9EWjIUHCrYDv4JOnRQsnZqobuju6ODW50cuxW6CjpIhxa6ZNCxxcnZzUGEpsm93SKZRLif95L3eeAOB8I5P7i85NzprA3f/TIfgFNtfdkDANMTOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQsLF2eHAwnw+zZQ8CTGNtbVgfxnHcXfYgwHTGcdx1dYcAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoR+2t3cuzd8+/TP48/OB58ueyT+fxvLHoCJnTl7bjj/xqXj9foZ33mQ/+gQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQ4NWOVXH91u3hwsVL//6H/9HV6x+fWF/76PPh8tvvLvyc2exo2P/hwcL3ZSGEvir2vvxmeOudncnP+ezOV5Pse3jwUuiry9UdAoQOAUKHAL/RV8VP33994nnjRbny/ifDjS/uHq+fPLw//PH7i4WfM58dLXxPFkboq+LXnx9Nsu98Pj8R+vNnPw6/7T+e5CxWlqs7BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BHip5bSbHb4aXv791z/rI08nFY3juLvsGYDpjOO46+oOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAjY2N7e/nBzc/PNZQ8CTGNra+u9105aQ3JcnDQjAAAAAElFTkSuQmCC");
			blank.put("PREHOVER" , "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAPoAAADmCAYAAAD8+JjdAAAD20lEQVR4nO3dP2idVRyA4S9p2rRULDQ4mA9EWjIUHCrYDv4JOnRQsnZqobuju6ODW50cuxW6CjpIhxa6ZNCxxcnZzUGEpsm93SKZRLif95L3eeAOB8I5P7i85NzprA3f/TIfgFNtfdkDANMTOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQsLF2eHAwnw+zZQ8CTGNtbVgfxnHcXfYgwHTGcdx1dYcAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoR+2t3cuzd8+/TP48/OB58ueyT+fxvLHoCJnTl7bjj/xqXj9foZ33mQ/+gQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQ4NWOVXH91u3hwsVL//6H/9HV6x+fWF/76PPh8tvvLvyc2exo2P/hwcL3ZSGEvir2vvxmeOudncnP+ezOV5Pse3jwUuiry9UdAoQOAUKHAL/RV8VP33994nnjRbny/ifDjS/uHq+fPLw//PH7i4WfM58dLXxPFkboq+LXnx9Nsu98Pj8R+vNnPw6/7T+e5CxWlqs7BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BHip5bSbHb4aXv791z/rI08nFY3juLvsGYDpjOO46+oOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAgQOgQIHQKEDgFChwChQ4DQIUDoECB0CBA6BAgdAoQOAUKHAKFDgNAhQOgQIHQIEDoECB0ChA4BQocAoUOA0CFA6BAgdAjY2N7e/nBzc/PNZQ8CTGNra+u9105aQ3JcnDQjAAAAAElFTkSuQmCC");
			blank.put("DATA" , "");
			result.add(0, blank);
//			result.add(blank);
//			Map<String, String> addTemplate = new HashMap<String, String>();
//			addTemplate.put("NAME", "ADD_TEMPLATE");
//			addTemplate.put("DESCRIPTION" , "KO".equals(info.getSession("LANGUAGE"))?"템플릿 만들기":"Create Template");
//			addTemplate.put("IMG" , "data:image/png;base64,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");
//			addTemplate.put("DATA" , "");
//			result.add(addTemplate);
//			
			String resultString = new SepoaFormater(result).getString(); 
			setValue(resultString);
		}catch(Exception e) {
			setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            e.printStackTrace();
            Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
}
