package com.android.io;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.android.autotesing.DataBaseRestore;
import com.android.ftp.AutoTestFtpIO;
import com.android.http.AutoTestHttpIO;
import com.android.xmlParse.ScriptInfo;
import com.android.xmlParse.TaskInfo;
import com.android.xmlParse.xmlParse;

import android.content.Context;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AutoTestIO {

	static final String TAG = "AutoTestIO";
	public static final String TaskFilePath = "/AutoTest/TaskInfo.xml";
	public static final String ScriptXMLPath = "/AutoTest/ftpScript.xml";
	/*标记是否后台服务已经开启*/
	public static boolean startstate = false;
	
	
	/********************************************************************************
	 * 获取本机IMEI号码
	 * @param Cmt
	 * @return
	 ********************************************************************************/
	public static String getIMEI(Context Cmt){
		String DevID;
		TelephonyManager tm = (TelephonyManager) Cmt.getSystemService("phone"); 
		DevID = tm.getDeviceId();
		if(DevID == null || "".equals(DevID)){
			DevID = "11111111111119";
		}
		return DevID;
	}
	
	/********************************************************************************
	 * 获取本机设备类型
	 * @param Cmt
	 * @return
	 ********************************************************************************/
	public static String getDevType(Context Cmt){
		DataBaseRestore DB = DataBaseRestore.getInstance(Cmt);
		return DB.getDevTypeValue();
	}
	
	/********************************************************************************
	 * 获取省份编码
	 * @param Cmt
	 * @return
	 ********************************************************************************/
	public static String getProvCode(Context Cmt){
		DataBaseRestore DB = DataBaseRestore.getInstance(Cmt);
		return DB.getPovCodeValue();
	}
	
	/********************************************************************************
	 * 获取城市编码
	 * @param Cmt
	 * @return
	 ********************************************************************************/
	public static String getCityCode(Context Cmt){
		DataBaseRestore DB = DataBaseRestore.getInstance(Cmt);
		return DB.getCityCodeValue();
	}
	
	/********************************************************************************
	 * 获取网络类型，2G or 3G
	 * @param Cmt
	 * @return
	 ********************************************************************************/
	public static String getNetworkFlag(Context Cmt){
		int NetType;
		TelephonyManager tm = (TelephonyManager) Cmt.getSystemService("phone"); 
		NetType = tm.getNetworkType();
		if(NetType == tm.NETWORK_TYPE_GPRS ||
				NetType == tm.NETWORK_TYPE_EDGE){
			return "2G";
		}else{
			return "3G";
		}
	}
	
	/********************************************************************************
	 * 任务更新，获取服务器上的对应任务列表文件
	 * @param IMEI
	 * @param DevType
	 * @param ProvCode
	 * @param TestTaskCode
	 * @param Cxt
	 * @return
	 ********************************************************************************/
	public static List<TaskInfo> getNewTask(String IMEI, String DevType , int ProvCode , String TestTaskCode,Context Cxt) {
		boolean ret = true;
		xmlParse p = new xmlParse();
		
		File folder = new File(getSDPath() + "/AutoTest/");
		folder.mkdir();
		
		File taskFile = new File(AutoTestIO.getSDPath() + TaskFilePath);
		
		OutputStream ops;
		try {
			ops = new FileOutputStream(taskFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		String taskQueryRet = AutoTestHttpIO.TaskQuery(IMEI, DevType, "nbpt", String.valueOf(ProvCode) , TestTaskCode, ops);
		if(!"0000".equals(taskQueryRet)){
			Log.e(TAG, "获取任务失败");
			return null;
		}
		
		InputStream ips;
		try {
			ips = new FileInputStream(taskFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		List<TaskInfo> task = null;
		
		try {
			task = p.taskParse(ips);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Task解析失败");
			return null;
		}
		
		Log.i(TAG, "任务下载成功，开始连接FTP下载包含脚本名的XML文件");
		
		ret = AutoTestFtpIO.getScriptXML(DevType,Cxt);
		
		if(!ret){
			Log.e(TAG, "获取FTP脚本XML失败");
			return task;
		}
		
		Log.i(TAG, "任务下载成功，包含脚本名的XML文件下载成功");

		File f = new File(AutoTestIO.getSDPath() + ScriptXMLPath);
		InputStream in;
		try {
			in = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return task;
		}

		ScriptInfo script = null;
		try{
			script = p.scriptParse(in);
		}catch(Exception e){
			Log.e(TAG, "FTP脚本XML解析失败");
			return task;
		}
		
		int tastSize = task.size();
		
		for(int i = 0 ; i < tastSize ; ++i){
			TaskInfo task_i = task.get(i);
			List<TaskInfo.KeySchema> keySchema = task_i.getKey();
			int keySize = keySchema.size();
			for(int t = 0 ; t < keySize ; ++t){
				TaskInfo.KeySchema key = keySchema.get(t);
				ScriptNameVer name = getScriptNameFromCode(
						script , 
						key.getServiceCode() , 
						key.getTestKeyCode());
				if(name == null){
					continue;
				}
				key.setScriptName(name.ScriptName);
				key.setScriptVersion(name.ScriptVersion);
				keySchema.set(t, key);
			}
			task_i.setKey(keySchema);
			task.set(i, task_i);
		}
		
		Log.i(TAG, "任务下载成功，且成功解析脚本包名称。");
		return task;
	}
	
	private static ScriptNameVer getScriptNameFromCode(ScriptInfo script , String ServiceCode , String TestKeyCode){
		int size = script.getScript().size();
		float ver = -1;
		ScriptNameVer name = new ScriptNameVer();
		if(ServiceCode == null || TestKeyCode == null){
			return null;
		}
		
		for(int i=0 ; i<size ; ++i){
			ScriptInfo.ScriptDetail script_i = script.getScript().get(i);
			
			if(ServiceCode.equals(script_i.getServiceCode()) && TestKeyCode.equals(script_i.getTestKeyCode())){
				float scriptVer = -1;
				try{
					scriptVer = Float.parseFloat(script_i.getScriptVersion());
				}
				catch(Exception e){
					continue;
				}
				if(scriptVer > ver){
					String tempName = script_i.getScriptName();
					int len = tempName.length();
					if(len > 4){
						name.ScriptName = tempName.substring(0, len - 4);
						name.ScriptVersion = script_i.getScriptVersion();
						ver = scriptVer;
					}
				}
			}
		}
		
		if(name == null){
			Log.i(TAG, ("未找到ServiceCode：" + ServiceCode + "  TestKeyCode：" + TestKeyCode + " 的脚本。"));
		}
		
		return name;
	}

	/********************************************************************************
	 * 获取本地任务文件
	 * @return
	 ********************************************************************************/
	public static List<TaskInfo> getTask() {
		File fileTask = new File(AutoTestIO.getSDPath() + TaskFilePath);
		File fileScript = new File(AutoTestIO.getSDPath() + ScriptXMLPath);
		
		xmlParse p = new xmlParse();
		
		if(!fileTask.exists()){
			return null;
		}
		
		InputStream ips;
		try {
			ips = new FileInputStream(fileTask);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		List<TaskInfo> task = null;
		
		try {
			task = p.taskParse(ips);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Task解析失败");
			return null;
		}

		if(!fileScript.exists()){
			return task;
		}
		
		InputStream in;
		try {
			in = new FileInputStream(fileScript);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return task;
		}
		
		ScriptInfo script = null;
		try{
			script = p.scriptParse(in);
		}catch(Exception e){
			Log.e(TAG, "FTP脚本XML解析失败");
			return task;
		}
		
		int tastSize = task.size();
		
		for(int i = 0 ; i < tastSize ; ++i){
			TaskInfo task_i = task.get(i);
			List<TaskInfo.KeySchema> keySchema = task_i.getKey();
			int keySize = keySchema.size();
			for(int t = 0 ; t < keySize ; ++t){
				TaskInfo.KeySchema key = keySchema.get(t);
				ScriptNameVer name = getScriptNameFromCode(
						script , 
						key.getServiceCode() , 
						key.getTestKeyCode());
				if(name == null){
					continue;
				}
				key.setScriptName(name.ScriptName);
				key.setScriptVersion(name.ScriptVersion);
				keySchema.set(t, key);
			}
			task_i.setKey(keySchema);
			task.set(i, task_i);
		}
		
		return task;
	}
	
	private static class ScriptNameVer{
		public String ScriptName;
		public String ScriptVersion;
		
		public ScriptNameVer(){
			
		}
	}
	
	public static boolean updateScriptFile( Date date , Context Cxt) {
		boolean ret = true;
		String DevType = getDevType(Cxt);
		
		ret = AutoTestFtpIO.getScriptXML(DevType , Cxt);
		
		if(!ret){
			Log.e(TAG, "获取FTP脚本XML失败");
			return false;
		}
		
		xmlParse p = new xmlParse();
		File f = new File(AutoTestIO.getSDPath() + ScriptXMLPath);
		InputStream in;
		try {
			in = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		ScriptInfo script = null;
		try{
			script = p.scriptParse(in);
		}catch(Exception e){
			Log.e(TAG, "FTP脚本XML解析失败");
			return false;
		}
		
		int size = script.getScript().size();
		
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String d1 = format.format(date);
		Log.e(TAG, d1);
		for(int i=0 ; i<size ; ++i){
			ScriptInfo.ScriptDetail script_i = script.getScript().get(i);
			String d2 = format.format(script_i.getUploaddate());
			Log.e(TAG, d2);
			if(script_i.getUploaddate().after(date)){
				ret = AutoTestFtpIO.downloadScriptFile(script_i.getTargetId() , script_i.getScriptName() , Cxt);
			}
		}
		
		return true;
	}
	
	public static boolean execCommandAsRoot(String command) {
        DataOutputStream os = null;
        System.err.println("command: " + command);
        try {
            Process process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            System.err.println("exit value = " + process.exitValue());
        } catch (Exception e) {
            e.printStackTrace();
     return false;
        }
        return true;
    }
	
    public static String getSDPath(){ 
        String status = android.os.Environment.getExternalStorageState();
        if (status.equals(android.os.Environment.MEDIA_MOUNTED)) {
            File sdDir = null; 
            boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            if(sdCardExist)   
            {                               
                sdDir = Environment.getExternalStorageDirectory();
                return sdDir.toString(); 
            }   
        }
        return "/mnt/sdcard";
    }
}
