package com.android.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;

import android.util.Log;
import android.util.Xml;

public class AutoTestHttpIO {
	
	static final String getKeyURL = "http://218.206.179.30:8080/generateKey/nbpt/g1s2m3t4s/"; 
	static final String TaskQueryURL = "http://218.206.179.30:8080/TaskQuery.action";
	static final String devRegURL = "http://218.206.179.30:8080/DeviceRegister.action";
	static final String devStatusURL = "http://218.206.179.30:8080/DeviceStatus.action";
	static final String devAlarm = "http://218.206.179.30:8080/DeviceAlarm.action";
	static final String scriptQueryURL = "http://218.206.179.30:8080/ScriptQuery.action";

//	static final String getKeyURL = "http://nbpt.cn:7805/TSQuestNew/generateKey/nbpt/g1s2m3t4s/"; 
//	static final String TaskQueryURL = "http://nbpt.cn:7805/TSQuestNew/TaskQuery.action";
//	static final String devRegURL = "http://nbpt.cn:7805/TSQuestNew/DeviceRegister.action";
//	static final String devStatusURL = "http://nbpt.cn:7805/TSQuestNew/DeviceStatus.action";
//	static final String devAlarm = "http://nbpt.cn:7805/TSQuestNew/DeviceAlarm.action";
//	static final String scriptQueryURL = "http://nbpt.cn:7805/TSQuestNew/ScriptQuery.action";

	static final String TAG = "AutoTestHttpIO";
	
	public static String getKey() {
		String key = "";
		
		SimpleDateFormat dateformatyyyyMMdd = new SimpleDateFormat("yyyyMMdd");
		String date = dateformatyyyyMMdd.format(new Date());
		
		String path = getKeyURL + date;
		
		URL url = null;
		
		try {
			url = new URL(path);
			//url = new URL("http://218.206.179.30:8080/TSQuestNew/generateKey/nbpt/g1s2m3t4s/20120201");
		} catch (MalformedURLException e) {

			e.printStackTrace();
			return null;
		}
		
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setReadTimeout(5 * 1000);
			conn.connect();
			
			if (conn.getResponseCode() == 200) {
				InputStream responseStream = conn.getInputStream();
				key = ResponseParseXML(responseStream);
			}
		} catch (IOException e) {

			e.printStackTrace();
			return null;
		} catch (Exception e) {

			e.printStackTrace();
			return null;
		}
		
		return key;
	}

	public static String setDeviceStatus(String devicesId , String devicesStatus) {

		InputStream inStream = AutoTestHttpIO.class.getClassLoader()
				.getResourceAsStream("DeviceStatusRequest.xml");
		
		String xml = null;
		
		try{
			byte[] data = StreamTool.readInputStream(inStream);
			xml = new String(data);
			xml = xml.replaceAll("\\$devicesId", devicesId);
			xml = xml.replaceAll("\\$devicesStatus", devicesStatus);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		/**
		 * 正则表达式$为特殊正则中的特殊符号须转义，即\$mobile 而\为字符串中的特殊符号，所以用两个反斜杠，即"\\{1}quot;
		 */

		String path = devStatusURL;

		byte[] data = xml.getBytes();// 得到了xml的实体数据

		URL url;
		try {
			url = new URL(path);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setReadTimeout(5 * 1000);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			conn.setRequestProperty("Content-Length", String.valueOf(data.length));
			conn.connect();
			OutputStream outStream = conn.getOutputStream();
			outStream.write(data);
			outStream.flush();
			outStream.close();
			if (conn.getResponseCode() == 200) {
				InputStream responseStream = conn.getInputStream();
				return ResponseParseXML(responseStream);
			}
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (ProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}


	private static String ResponseParseXML(InputStream responseStream) throws Exception {
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(responseStream, "UTF-8");
		int event = parser.getEventType();
		String ret = null;
		while (event != XmlPullParser.END_DOCUMENT) {
			switch (event) {
				case XmlPullParser.START_TAG:
					if ("Message".equals(parser.getName())) {
						String error = parser.nextText();
						
						if(error != null && !error.equals("")){
							Log.e(TAG, error);
						}
					}
					else if ("DeviceStatusResponse".equals(parser.getName())) {
						String nameSpace = parser.getNamespace();
						ret = parser.getAttributeValue(nameSpace, "Status");
						if(ret.equals("0000")) {
							return ret;
						}
					}
					else if ("DeviceRegisterResponse".equals(parser.getName())) {
						String nameSpace = parser.getNamespace();
						ret = parser.getAttributeValue(nameSpace, "Status");
						if(ret.equals("0000")) {
							return ret;
						}
					}
					else if ("DeviceAlarmResponse".equals(parser.getName())) {
						String nameSpace = parser.getNamespace();
						ret = parser.getAttributeValue(nameSpace, "Status");
						if(ret.equals("0000")) {
							return ret;
						}
					}
					else if ("key".equals(parser.getName())) {
						return parser.nextText();
					}
					else if ("TaskQueryResponse".equals(parser.getName())) {
						String nameSpace = parser.getNamespace();
						ret = parser.getAttributeValue(nameSpace, "Status");
						return ret;
					}
					break;
			}
			event = parser.next();
		}
		return ret;
	}

	public static String DeviceRegister(String devicesId , String devicesType , String devicesName , String ProvCode ,
			String CityCode , String SWVersion , String VenderCode , String devicesStatus ,String devMSIN) {

		InputStream inStream = AutoTestHttpIO.class.getClassLoader()
				.getResourceAsStream("DeviceRegisterRequest.xml");
		
		String xml = null;
		
		try{
			byte[] data = StreamTool.readInputStream(inStream);
			xml = new String(data);
			xml = xml.replaceAll("\\$devicesId", devicesId);
			xml = xml.replaceAll("\\$devicesType", devicesType);
			xml = xml.replaceAll("\\$devicesName", devicesName);
			xml = xml.replaceAll("\\$devProvCode", ProvCode);
			xml = xml.replaceAll("\\$devCityCode", CityCode);
			xml = xml.replaceAll("\\$devSWVersion", SWVersion);
			xml = xml.replaceAll("\\$devVenderCode", VenderCode);
			xml = xml.replaceAll("\\$devicesStatus", devicesStatus);
			xml = xml.replaceAll("\\$devMSIN", devMSIN);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		/**
		 * 正则表达式$为特殊正则中的特殊符号须转义，即\$mobile 而\为字符串中的特殊符号，所以用两个反斜杠，即"\\{1}quot;
		 */

		String path = devRegURL;

		byte[] data = xml.getBytes();// 得到了xml的实体数据

		URL url;
		try {
			url = new URL(path);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setReadTimeout(5 * 1000);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			conn.setRequestProperty("Content-Length", String.valueOf(data.length));
			conn.connect();
			OutputStream outStream = conn.getOutputStream();
			outStream.write(data);
			outStream.flush();
			outStream.close();
			if (conn.getResponseCode() == 200) {
				InputStream responseStream = conn.getInputStream();
				return ResponseParseXML(responseStream);
			}
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (ProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}

	public static String DeviceAlarm(String devicesId , String EventCode , String Message) {

		InputStream inStream = AutoTestHttpIO.class.getClassLoader()
				.getResourceAsStream("DeviceAlarmRequest.xml");
		
		String xml = null;
		
		try{
			byte[] data = StreamTool.readInputStream(inStream);
			xml = new String(data);
			xml = xml.replaceAll("\\$devicesId", devicesId);
			xml = xml.replaceAll("\\$devEventCode", EventCode);
			xml = xml.replaceAll("\\$devMessage", Message);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		/**
		 * 正则表达式$为特殊正则中的特殊符号须转义，即\$mobile 而\为字符串中的特殊符号，所以用两个反斜杠，即"\\{1}quot;
		 */

		String path = devAlarm;

		byte[] data = xml.getBytes();// 得到了xml的实体数据

		URL url;
		try {
			url = new URL(path);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setReadTimeout(5 * 1000);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			conn.setRequestProperty("Content-Length", String.valueOf(data.length));
			conn.connect();
			OutputStream outStream = conn.getOutputStream();
			outStream.write(data);
			outStream.flush();
			outStream.close();
			if (conn.getResponseCode() == 200) {
				InputStream responseStream = conn.getInputStream();
				return ResponseParseXML(responseStream);
			}
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (ProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}

	public static QueryScriptResponse QueryScript(String IMEI , String DevType , String VenderCode ,
			String ProvCode , String LastDownloadTime) {

		InputStream inStream = AutoTestHttpIO.class.getClassLoader()
				.getResourceAsStream("QueryScriptRequest.xml");
		
		String xml = null;
		
		try{
			byte[] data = StreamTool.readInputStream(inStream);
			xml = new String(data);
			xml = xml.replaceAll("\\$devIMEI", IMEI);
			xml = xml.replaceAll("\\$devDevType", DevType);
			xml = xml.replaceAll("\\$devVenderCode", VenderCode);
			xml = xml.replaceAll("\\$devProvCode", ProvCode);
			xml = xml.replaceAll("\\$devLastDownloadTime", LastDownloadTime);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		/**
		 * 正则表达式$为特殊正则中的特殊符号须转义，即\$mobile 而\为字符串中的特殊符号，所以用两个反斜杠，即"\\{1}quot;
		 */

		String path = scriptQueryURL;

		byte[] data = xml.getBytes();// 得到了xml的实体数据

		URL url;
		try {
			url = new URL(path);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setReadTimeout(5 * 1000);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			conn.setRequestProperty("Content-Length", String.valueOf(data.length));
			conn.connect();
			OutputStream outStream = conn.getOutputStream();
			outStream.write(data);
			outStream.flush();
			outStream.close();
			if (conn.getResponseCode() == 200) {
				InputStream responseStream = conn.getInputStream();
				return QueryScriptResponseParseXML(responseStream);
			}
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (ProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}

	private static QueryScriptResponse QueryScriptResponseParseXML(InputStream responseStream) throws Exception {
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(responseStream, "UTF-8");
		int event = parser.getEventType();
		QueryScriptResponse ret = new QueryScriptResponse();
		while (event != XmlPullParser.END_DOCUMENT) {
			switch (event) {
				case XmlPullParser.START_TAG:
					if ("Message".equals(parser.getName())) {
						ret.Message = parser.nextText();
						
						if(ret.Message != null && !ret.Message.equals("")){
							Log.e(TAG, ret.Message);
						}
					}
					else if ("QueryScriptResponse".equals(parser.getName())) {
						String nameSpace = parser.getNamespace();
						ret.Status = parser.getAttributeValue(nameSpace, "Status");
					}
					else if ("UpdateFlag".equals(parser.getName())) {
						ret.UpdateFlag = parser.nextText();
					}
					else if ("LastUpdateTime".equals(parser.getName())) {
						ret.LastUpdateTime = parser.nextText();
					}

					break;
			}
			event = parser.next();
		}
		return ret;
	}

	public static String TaskQuery(String IMEI , String DevType , String VenderCode ,
			String ProvCode , String devTestTaskCode ,OutputStream xmlOutStream) {

		InputStream inStream = AutoTestHttpIO.class.getClassLoader()
				.getResourceAsStream("TaskQueryRequest.xml");
		
		String xml = null;
		
		try{
			byte[] data = StreamTool.readInputStream(inStream);
			xml = new String(data);
			xml = xml.replaceAll("\\$devIMEI", IMEI);
			xml = xml.replaceAll("\\$devDevType", DevType);
			xml = xml.replaceAll("\\$devVenderCode", VenderCode);
			xml = xml.replaceAll("\\$devProvCode", ProvCode);
			xml = xml.replaceAll("\\$devTestTaskCode", devTestTaskCode);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		/**
		 * 正则表达式$为特殊正则中的特殊符号须转义，即\$mobile 而\为字符串中的特殊符号，所以用两个反斜杠，即"\\{1}quot;
		 */

		String path = TaskQueryURL;

		byte[] data = xml.getBytes();// 得到了xml的实体数据
		
		URL url;
		try {
			url = new URL(path);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setReadTimeout(5 * 1000);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			conn.setRequestProperty("Content-Length", String.valueOf(data.length));
			conn.connect();
			OutputStream outStream = conn.getOutputStream();
			outStream.write(data);
			outStream.flush();
			outStream.close();
			if (conn.getResponseCode() == 200) {
				InputStream responseStream = conn.getInputStream();
				byte[] xmlData = StreamTool.readInputStream(responseStream);
				File tempTaskxml = new File("sdcard/tempTaskxml.xml");
				FileOutputStream xmlfos = new FileOutputStream(tempTaskxml);
				xmlfos.write(xmlData);
				xmlfos.flush();
				xmlfos.close();
				FileInputStream xmlfis = new FileInputStream(tempTaskxml);
				String ret = ResponseParseXML(xmlfis);
				tempTaskxml.delete();
				if(ret.equals("0000")){
					
					xmlOutStream.write(xmlData);
					xmlOutStream.flush();
					xmlOutStream.close();
				}
				return ret;
			}
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (ProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (Exception e) {

			e.printStackTrace();
		}

		try {
			xmlOutStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
