package com.android.xmlParse;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class xmlParse {
	
	public List<TaskInfo> taskParse(InputStream in) throws Exception {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		TaskXmlParse taskXmlParse = new TaskXmlParse();
		parser.parse(in, taskXmlParse);
		in.close();
		return taskXmlParse.getData();
	}	

	public List<CaseInfo> caseParse(InputStream in) throws Exception {
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		CaseXmlParse caseXmlParse = new CaseXmlParse();
		parser.parse(in, caseXmlParse);
		in.close();
		return caseXmlParse.getData();
	}

	public String caseResultValue(InputStream in) throws Exception{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		CaseXmlParse caseXmlParse = new CaseXmlParse();
		parser.parse(in, caseXmlParse);
		in.close();
		return caseXmlParse.getResultvalue();
	}
	
	public ScriptInfo scriptParse(InputStream in) throws Exception {
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		ScriptXmlParse scriptXmlParse = new ScriptXmlParse();
		parser.parse(in, scriptXmlParse);
		in.close();
		return scriptXmlParse.getData();
	}

	/********************************************************************************
	 * 解析告警阀值设置文件
	 * 
	 * @param in
	 * @return
	 * @throws Exception
	 ********************************************************************************/
	public WarningInfo WarningParse(InputStream in) throws Exception {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		WarningXmlParse warningXmlParse = new WarningXmlParse();
		parser.parse(in, warningXmlParse);
		in.close();
		return warningXmlParse.getData();
	}
	
	public class TaskXmlParse extends DefaultHandler {	
		List<TaskInfo> all = null;
		TaskInfo task = null;
		String flag = null;
		TaskInfo.KeySchema Key = null;
		
		public List<TaskInfo> getData() {
			return all;
		}
	
		public void startDocument() throws SAXException {
	
			all = new ArrayList<TaskInfo>();  
		}
	
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if ("TaskInfo".equals(localName)) { 
				int priority = 0;
				task = new TaskInfo(attributes.getValue("TestTaskCode"));
				task.setTestTaskName(attributes.getValue("TestTaskName"));
				task.setTestTaskFlag(attributes.getValue("TestTaskFlag"));
				task.setTestTaskDesc(attributes.getValue("TestTaskDesc"));
				task.setLogLevel(attributes.getValue("LogLevel"));
				if(attributes.getValue("Priority").equals("L")){
					priority = 0;
				}else if(attributes.getValue("Priority").equals("M")){
					priority = 1;
				}else if(attributes.getValue("Priority").equals("H")){
					priority = 2;
				}
				task.setPriority(priority);
				task.setStartDate(attributes.getValue("StartDate"));
				task.setEndDate(attributes.getValue("EndDate"));
				task.setNetPacketCap(attributes.getValue("NetCapture"));
			}
			else if("ExecuteSchema".equals(localName) && task != null) {  
				task.setExeType(Integer.parseInt(attributes.getValue("ExeType")));
				task.setExeBeginTime(attributes.getValue("ExeBeginTime"));
				task.setExeEndTime(attributes.getValue("ExeEndTime"));
				task.setIterationType(Integer.parseInt(attributes.getValue("IterationType")));
				task.setIterationNum(Integer.parseInt(attributes.getValue("IterationNum")));
				task.setInterval(Integer.parseInt(attributes.getValue("Interval")));
			}
			else if("Key".equals(localName) && task != null){
				Key = task.newKey();
				Key.setServiceCode(attributes.getValue("ServiceCode"));
				Key.setTestKeyCode(attributes.getValue("TestKeyCode"));
				Key.setDevId(attributes.getValue("DevId"));
				task.addKey(Key);
			}
			flag = localName;  
		}
	
		public void characters(char[] ch, int start, int length)
				throws SAXException {
	
		}
	
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
	
			if ("TaskInfo".equals(localName)) {
				all.add(task);
			}
			flag = null;
		}
	}
	
	public class CaseXmlParse extends DefaultHandler {	
		List<CaseInfo> all = null;
		CaseInfo caseinfo = null;
		String flag = null;
		String resultvalue = null;
		CaseInfo.KeyInfo keyInfo = null;
		CaseInfo.TouchInfo touchInfo = null;
		CaseInfo.ResultPic picInfo = null;
		CaseInfo.ResultLog logInfo = null;
	
		public List<CaseInfo> getData() {
			return all;
		}
	
		public String getResultvalue(){
			return resultvalue;
		}
		
		public void startDocument() throws SAXException {
	
			all = new ArrayList<CaseInfo>();  
		}
	
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if("Case".equals(localName)){
				resultvalue = attributes.getValue("CaseResultValue");
			}else if ("Test".equals(localName)) {  
				caseinfo = new CaseInfo();
				caseinfo.setTestEventType(attributes.getValue("TestEventType"));
				caseinfo.setTestResultType(attributes.getValue("TestResultType"));
				caseinfo.setTestID(attributes.getValue("TestID"));
				caseinfo.setTestFailedGotoID(attributes.getValue("TestFailedGotoID"));
				caseinfo.setExecuteTime(attributes.getValue("ExecuteTime"));
				caseinfo.setTestSuccessGotoID(attributes.getValue("TestSuccessGotoID"));
				caseinfo.setTestValueType(attributes.getValue("TestValueType"));
				caseinfo.setTestLooper(attributes.getValue("TestLooper"));
				String temp = attributes.getValue("TestFailedValue");
				if(temp!= null && temp.equals("")== false){
					caseinfo.setTestFailedValue(Integer.parseInt(temp));
				}else{
					caseinfo.setTestFailedValue(-1);
				}
				caseinfo.setTestNeedPacket(attributes.getValue("TestNeedPacket"));
			}
			else if("Key".equals(localName) && caseinfo != null) {
				keyInfo = caseinfo.newKey();
				keyInfo.setKeyCode(attributes.getValue("KeyCode"));
				keyInfo.setKeyType(attributes.getValue("KeyType"));
				keyInfo.setLog(attributes.getValue("Log"));
				try{
					keyInfo.setHoldTime(Integer.parseInt(attributes.getValue("HoldTime")));
				}catch(Exception e){
					keyInfo.setHoldTime(0);
				}
				try{
					keyInfo.setKeyValue(Integer.parseInt(attributes.getValue("KeyValue")));
				}catch(Exception e){
					keyInfo.setKeyValue(0);
				}
				caseinfo.addKey(keyInfo);
			}
			else if("Msg".equals(localName) && caseinfo != null){
				caseinfo.addMsg(attributes.getValue("Id"));
			}
			else if("Touch".equals(localName) && caseinfo != null) {
				touchInfo = caseinfo.newTouch();
				touchInfo.setTouchX(Integer.parseInt(attributes.getValue("TouchX")));
				touchInfo.setTouchY(Integer.parseInt(attributes.getValue("TouchY")));
				touchInfo.setTouchType(attributes.getValue("TouchType"));
				touchInfo.setLog(attributes.getValue("Log"));
				try{
					touchInfo.setHoldTime(Integer.parseInt(attributes.getValue("HoldTime")));
				}catch(Exception e){
					touchInfo.setHoldTime(0);
				}
				caseinfo.addTouch(touchInfo);
			}
			else if("Pic".equals(localName) && caseinfo != null) {
				picInfo = caseinfo.newPic();
				picInfo.setPath(attributes.getValue("Path"));
				picInfo.setPicX(Integer.parseInt(attributes.getValue("PicX")));
				picInfo.setPicY(Integer.parseInt(attributes.getValue("PicY")));
				picInfo.setPicW(Integer.parseInt(attributes.getValue("PicW")));
				picInfo.setPicH(Integer.parseInt(attributes.getValue("PicH")));
				caseinfo.addPic(picInfo);
			}
			else if("Log".equals(localName) && caseinfo != null) {
				logInfo = caseinfo.newLog();
				logInfo.setLogInfo(attributes.getValue("LogInfo"));
				logInfo.setFilter(attributes.getValue("Filter"));
				caseinfo.addLog(logInfo);
			}
			flag = localName;  
		}
	
		public void characters(char[] ch, int start, int length)
				throws SAXException {
		}
	
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
	
			if ("Test".equals(localName)) {
				all.add(caseinfo);
			}
			flag = null;
		}
	}
	
	
	public class ScriptXmlParse extends DefaultHandler {	
		ScriptInfo all = null;
		ScriptInfo.ScriptDetail scriptDetail = null;
		String flag = null;
		CaseInfo.KeyInfo keyInfo = null;
		CaseInfo.TouchInfo touchInfo = null;
		CaseInfo.ResultPic picInfo = null;
	
		public ScriptInfo getData() {
			return all;
		}
	
		public void startDocument() throws SAXException {
	
			all = new ScriptInfo();  
		}
	
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if ("ScriptDetail".equals(localName)) {  
				scriptDetail = all.newScript();
				scriptDetail.setServiceCode(attributes.getValue("ServiceCode"));
				scriptDetail.setTestKeyCode(attributes.getValue("TestKeyCode"));
				scriptDetail.setScriptName(attributes.getValue("ScriptName"));
				scriptDetail.setScriptVersion(attributes.getValue("ScriptVersion"));
				scriptDetail.setTargetId(attributes.getValue("TargetId"));
				scriptDetail.setUploaddate(attributes.getValue("Uploaddate"));
				scriptDetail.setScriptDesc(attributes.getValue("ScriptDesc"));
				all.addScript(scriptDetail);
			}
			else if("ScriptInfo".equals(localName)) {
				all.setVenderCode(attributes.getValue("VenderCode"));
				all.setDevType(attributes.getValue("DevType"));
				all.setTestMode(attributes.getValue("TestMode"));
			}
			flag = localName;  
		}
	
		public void characters(char[] ch, int start, int length)
				throws SAXException {
		}
	
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
	
			flag = null;
		}
	}	
	/********************************************************************************
	 * 解析告警阀值配置表
	 * @author 
	 *
	 ********************************************************************************/
	public class WarningXmlParse extends DefaultHandler {	
		WarningInfo info = null;
		String flag = null;
	
		public WarningInfo getData() {
			return info;
		}
	
		public void startDocument() throws SAXException {
			info = new WarningInfo();
		}
	
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if ("Control".equals(localName)) {  
				
				//battery temp
				String temp = attributes.getValue("BatteryTemp");
				if(temp!= null && temp.equals("")== false){
					info.SetBatteryTemp(Integer.parseInt(temp));
				}else{
					info.SetBatteryTemp(-1);
				}
				
				//cpu usage
				temp = null;
				temp = attributes.getValue("CPUUsage");
				if(temp!= null && temp.equals("")== false){
					info.SetCpuUsage(Float.parseFloat(temp));
				}else{
					info.SetCpuUsage(0);
				}
				
				//RAMMemLeft
				temp = null;
				temp = attributes.getValue("RAMMemLeft");
				if(temp!= null && temp.equals("")== false){
					info.SetRamMemoryLeft(Integer.parseInt(temp));
				}else{
					info.SetRamMemoryLeft(-1);
				}
				
				//ROMMemLeft
				temp = null;
				temp = attributes.getValue("ROMMemLeft");
				if(temp!= null && temp.equals("")== false){
					info.SetRomMemoryLeft(Integer.parseInt(temp));
				}else{
					info.SetRomMemoryLeft(-1);
				}
				
				//BatteryLevel
				temp = null;
				temp = attributes.getValue("BatteryLevel");
				if(temp!= null && temp.equals("")== false){
					info.SetBatteryLevelLeft(Integer.parseInt(temp));
				}else{
					info.SetBatteryLevelLeft(-1);
				}
				
				//Signal
				temp = null;
				temp = attributes.getValue("Signal");
				if(temp!= null && temp.equals("")== false){
					info.SetSignalKey(Integer.parseInt(temp));
				}else{
					info.SetSignalKey(-1);
				}
				
				//DeviceTemp
				temp = null;
				temp = attributes.getValue("DeviceTemp");
				if(temp!= null && temp.equals("")== false){
					info.SetDeviceTemp(Integer.parseInt(temp));
				}else{
					info.SetDeviceTemp(-1);
				}
				
				//NetWorkCost
				temp = null;
				temp = attributes.getValue("NetWorkCost");
				if(temp!= null && temp.equals("")== false){
					info.SetNetworkCost(Integer.parseInt(temp));
				}else{
					info.SetNetworkCost(-1);
				}
				
				//ContinueFailNum
				temp = null;
				temp = attributes.getValue("ContinueFailNum");
				if(temp!= null && temp.equals("")== false){
					info.SetContinuFailNum(Integer.parseInt(temp));
				}else{
					info.SetContinuFailNum(-1);
				}
				
			}else if("Interval".equals(localName)){
				String temp = attributes.getValue("AlarmInterval");
				if(temp!= null && temp.equals("")== false){
					info.SetAlarmInterval(Integer.parseInt(temp));
				}else{
					info.SetAlarmInterval(-1);
				}
			}
			flag = localName;  
		}
	
		public void characters(char[] ch, int start, int length)
				throws SAXException {
		}
	
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
	
			flag = null;
		}
	}
	
	
}
