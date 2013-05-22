package com.android.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.util.List;


import android.content.Context;
import android.util.Log;

import com.android.autotesing.LOG;
import com.android.des.DESUtil;
import com.android.ftp.AutoTestFtpIO;
import com.android.ftp.UnZipFile;
import com.android.http.AutoTestHttpIO;
import com.android.xmlParse.CaseInfo;
import com.android.xmlParse.TaskInfo;



public class Report {

	static final String TAG = "Report";
	public static final String reportFilePath = "/AutoTest/TestReport.txt";
	private static int AllcaseExeNum = 0;
	private static List<CaseExe> mCaseExe = new ArrayList<CaseExe>();

	/**
	 * 文件过滤器，过滤出文件夹
	 */
	private static final FileFilter FOLDER_FILTER = new FileFilter() {

		public boolean accept(File f) {
			return f.isDirectory();
		}
	};

	/**
	 * 文件过滤器，过滤出以 _result_pic.PNG 结尾的文件
	 */
	private static final FileFilter IMAGES_FILTER = new FileFilter() {

		public boolean accept(File f) {
			return f.getName().matches("^.*?\\_result_pic.PNG$");
		}
	};

	/**
	 * 文件过滤器，过滤出以 _log.txt 结尾的文件
	 */
	private static final FileFilter LOG_FILTER = new FileFilter() {

		public boolean accept(File f) {
			return f.getName().matches("^.*?\\_log.txt$");
		}
	};
	private static final String LOG_I = null;

	
	/**
	 * 往report文件中添加一条数据
	 * @param Cmt	            Context
	 * @param RunningTask       运行中的任务信息
	 * @param CurrentCase       运行中的脚本信息
	 * @param ExecResult        脚本执行结果
	 * @param TestValue         脚本执行的TestValue值
	 * @param ExecNum           脚本第几次执行
	 * @param mCaseCodeIndex    当前脚本在当前任务所有脚本中的序号
	 * @param start             脚本开始时间
	 * @param end               脚本结束时间
	 * @param netCap            抓包文件
	 * @return
	 */
	public static void addReport(Context Cmt,TaskInfo RunningTask , CaseInfo CurrentCase,int ExecResult ,
			String TestValue ,int ExecNum , int mCaseCodeIndex, Date start,Date end,String netCap){
		
		FileWriter fw = null;
		LOG.Log(LOG.LOG_I, "addReport 0000");
		TaskInfo.KeySchema key = RunningTask.getKey().get(mCaseCodeIndex);
		LOG.Log(LOG.LOG_I, "addReport 1111");
		String NetworkFlag = AutoTestIO.getNetworkFlag(Cmt);
		LOG.Log(LOG.LOG_I, "addReport 2222");
		try {
			fw = new FileWriter(AutoTestIO.getSDPath() + reportFilePath , true);
			LOG.Log(LOG.LOG_I, "addReport 3333");
			SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmmss");
			LOG.Log(LOG.LOG_I, "addReport 4444");
			String StartDate = f.format(start);
			LOG.Log(LOG.LOG_I, "addReport 5555");
			String EndDate = f.format(end);
//			double d = ExecTime / 1000;
//			String TimeStr = String .format("%.2f" , d);
			if(fw == null){
				LOG.Log(LOG.LOG_I, "fw == null######## +");
			}
			LOG.Log(LOG.LOG_I, "path######## +"+AutoTestIO.getSDPath() + reportFilePath);
			
			if(ExecResult == 0){
				String netStr = "";
				if(netCap != null){
					File fNet = new File(netCap);
					if(fNet.exists()){
						netStr = getAttachFileName(fNet);
					}
				}
				
				if("".equals(netStr)){
							fw.write(key.getServiceCode() + "\t"+key.getTestKeyCode() + "\t" + RunningTask.getTestTaskCode() + 
									"\tINPHONE\t" + StartDate + "\t" + EndDate + "\t" + ExecNum + "\t" +
									"00" + "\t" + TestValue + "\t" + NetworkFlag + "\t0\t\t" + "\r\n");
				}else{
					fw.write(key.getServiceCode() + "\t"+key.getTestKeyCode() + "\t" + RunningTask.getTestTaskCode() + 
							"\tINPHONE\t" + StartDate + "\t" + EndDate + "\t" + ExecNum + "\t" +
							"00" + "\t" + TestValue + "\t" + NetworkFlag + "\t1\t" + netStr + "\t" + "\r\n");
				}
			}
			else{
				String temstr = String.format("%02d", ExecResult);
				int failedvalue = CurrentCase.getTestFailedValue();
				String BitInt = String.valueOf(failedvalue);
				if(failedvalue != -1 && BitInt.length() < 3 ){
					temstr = String.format("%02d", failedvalue);
				}else{
					temstr = "04";
				}
				fw.write(key.getServiceCode() + "\t"+key.getTestKeyCode() + "\t" + RunningTask.getTestTaskCode() + 
						"\tINPHONE\t" + StartDate + "\t" + EndDate + "\t" + ExecNum + "\t" +
						temstr + "\t" + TestValue + "\t" + NetworkFlag + "\t1\t" + 
						getAttachFileName(RunningTask.getTestTaskName() , key.getScriptName() , netCap) + "\t" + "\r\n");
			}
			fw.flush();
			fw.close();
		} catch (IOException e) {
			LOG.Log(LOG.LOG_I, "addReport +"+e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	/**
	 * 遍历结果文件夹，找出凭证文件
	 * @param taskName	        当前任务名称
	 * @param scriptName        当前脚本名称
	 * @param netCap            抓包文件名称
	 * @return
	 */
	private static String getAttachFileName(String taskName , String scriptName , String netCap){
		String fileName = "";
		String logName = "";
		File picFile = null;
		File logFile = null;
		
		String sdcard = AutoTestIO.getSDPath();
		File filepath = new File(sdcard + "/AutoTest/FILE");
		if(!filepath.exists()){
			filepath.mkdir();
		}
		
		File f = new File(sdcard + "/AutoTest/Result/"+ taskName +"/" + scriptName);
		if(f.exists()&&f.isDirectory()){
			for (File file : f.listFiles(IMAGES_FILTER)) {
				if(fileName.equals("")){
					fileName = file.getName();
					picFile = file;
				}
				else{
					if(!fileAnewThanB(fileName , file.getName())){
						fileName = file.getName();
						picFile = file;
					}
				}
			}
			for (File file : f.listFiles(LOG_FILTER)) {
				if(logName.equals("")){
					logName = file.getName();
					logFile = file;
				}
				else{
					if(!fileAnewThanB(logName , file.getName())){
						logName = file.getName();
						logFile = file;
					}
				}
			}
		}
		
		Collection<File> Files = new ArrayList<File>();
		if(picFile != null){
			Files.add(picFile);
		}
		if(logFile != null){
			Files.add(logFile);
		}
		if(netCap != null){
			File fNet = new File(netCap);
			if(fNet.exists()){
				Files.add(fNet);
			}
		}
		
		if(Files.size() <= 0){
			return "";
		}

		Date d = new Date();
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
		String dateStr = sf.format(d);
		String zipName = "voucher_" + dateStr + ".zip";
		File zipFile = new File(AutoTestIO.getSDPath() + "/AutoTest/FILE/" + zipName);

		try {
			UnZipFile.zipFiles(Files, zipFile);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		return zipName;
	}

	/**
	 * 遍历结果文件夹，找出凭证文件
	 * @param fNet            抓包文件
	 * @return
	 */
	private static String getAttachFileName(File fNet){

		Date d = new Date();
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
		String dateStr = sf.format(d);
		String zipName = "voucher_" + dateStr + ".zip";
		
		String sdcard = AutoTestIO.getSDPath();
		File filepath = new File(sdcard + "/AutoTest/FILE");
		if(!filepath.exists()){
			filepath.mkdir();
		}
		
		File zipFile = new File(sdcard + "/AutoTest/FILE/" + zipName);

		Collection<File> Files = new ArrayList<File>();
		Files.add(fNet);
		
		try {
			UnZipFile.zipFiles(Files, zipFile);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		return zipName;
	}

	/**
	 * 通过解析文件名，判断A、B两个文件哪个更新
	 * @param nameA            文件A 名称
	 * @param nameB            文件B 名称
	 * @return
	 */
	private static boolean fileAnewThanB(String nameA, String nameB) {

		try {
			Date d1 = new Date();
			d1.setYear(Integer.parseInt(nameA.substring(0, 4)) - 1900);
			d1.setMonth(Integer.parseInt(nameA.substring(5, 7)) - 1);
			d1.setDate(Integer.parseInt(nameA.substring(8, 10)));
			d1.setHours(Integer.parseInt(nameA.substring(11, 13)));
			d1.setMinutes(Integer.parseInt(nameA.substring(14, 16)));
			d1.setSeconds(Integer.parseInt(nameA.substring(17, 19)));

			Date d2 = new Date();
			d2.setYear(Integer.parseInt(nameB.substring(0, 4)) - 1900);
			d2.setMonth(Integer.parseInt(nameB.substring(5, 7)) - 1);
			d2.setDate(Integer.parseInt(nameB.substring(8, 10)));
			d2.setHours(Integer.parseInt(nameB.substring(11, 13)));
			d2.setMinutes(Integer.parseInt(nameB.substring(14, 16)));
			d2.setSeconds(Integer.parseInt(nameB.substring(17, 19)));

			if (d1.after(d2)) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	
	/**
	 * 生成report头部
	 * @param taskInfo        任务列表信息
	 * @param mCxt            Context
	 * @return
	 */
	private static String getReportHead(List<TaskInfo> taskInfo,Context mCxt){
		String reportHead = "nbpt\t";
		String DevType = AutoTestIO.getDevType(mCxt);
		String DevCode = AutoTestIO.getIMEI(mCxt);
		reportHead += (DevType + "\t" + DevCode + "\t\t\t" + getReportTotalNum() + "\t");
		for(int i = 0 ; taskInfo!=null && i < taskInfo.size() ; ++i){
			TaskInfo task = taskInfo.get(i);
			for(int j = 0 ; j < task.getKey().size() ; ++ j){
				TaskInfo.KeySchema key = task.getKey().get(j);
				reportHead += (key.getServiceCode() + "\t" + key.getTestKeyCode() + "\t" + 
						getReportCaseNum(key.getServiceCode() , key.getTestKeyCode(), key.getScriptName()) + "\t");
			}
		}
		
		reportHead += "\r\n";
		return reportHead;
	}
	
	/**
	 * 设置report总条数
	 * @param num       report总条数
	 * @return
	 */
	public static void setReportTotalNum(int num){
		AllcaseExeNum = num;
	}
	
	/**
	 * 读取report总条数
	 * @return
	 */
	public static int getReportTotalNum(){
		return AllcaseExeNum;
	}
	
	/**
	 * 设置report 某个脚本的条数
	 * @param scriptName   脚本名
	 * @param Num          report总条数
	 * @return
	 */
	public static void addReportCaseNum(String scriptName,int Num){
		CaseExe tmp = new CaseExe();
		tmp.setExeNum(Num);
		tmp.setScriptName(scriptName);
		mCaseExe.add(tmp);
	}

	/**
	 * 读取report 某个脚本的条数
	 * @param ServiceCode   ServiceCode
	 * @param TestKeyCode   TestKeyCode
	 * @param scriptName    脚本名
	 * @return
	 */
	public static int getReportCaseNum(String ServiceCode , String TestKeyCode ,String ScriptName){
		for(int i = 0;i<mCaseExe.size();i++){
			CaseExe item = mCaseExe.get(i);
			if(item.getScriptName() == null){
				LOG.Log(LOG.LOG_E, "getReportCaseNum scriptname is null");
			}
			if(item.getScriptName() != null && item.getScriptName().equals(ScriptName)){
				return item.getExeNum();
			}
		}
		return 0;
	}

	/**
	 * 对report 加密
	 * @param recordHead   report 的头部
	 * @param fileName     report 文件名
	 * @return
	 */
	private static boolean encodeReport(String  recordHead , String fileName){
		boolean ret = false;
		byte[] iv = new byte[8];
		
		try {
			String DESKey = AutoTestHttpIO.getKey();
			byte[] key = DESKey.getBytes();

			File file = new File(fileName);
			long start = System.currentTimeMillis();
			BufferedInputStream bis1 = new BufferedInputStream(
					new FileInputStream(AutoTestIO.getSDPath() + "/AutoTest/TestReport.txt"));
			BufferedOutputStream bos1 = new BufferedOutputStream(
					new FileOutputStream(file , true));
			byte[] data = new byte[1];
			
			byte[] head = recordHead.getBytes();
			// 加密写入文件头
			for(int i = 0 ; i < head.length ; ++ i){
				data[0] = head[i];
				bos1.write(DESUtil.CBCEncrypt(data, key, iv));
			}
			
			// 加密写入文件内容
			while (bis1.read(data) != -1) {
				bos1.write(DESUtil.CBCEncrypt(data, key, iv));
			}
			bos1.flush();
			bos1.close();
			bis1.close();
			long tmp = System.currentTimeMillis();
			System.out.println(tmp - start);

			//此处用于解密验证			
//			BufferedInputStream bis2 = new BufferedInputStream(
//					new FileInputStream(fileName));
//			BufferedOutputStream bos2 = new BufferedOutputStream(
//					new FileOutputStream("/mnt/sdcard/AutoTest/TestReport1.txt"));
//			data = new byte[8];
//			while (bis2.read(data) != -1) {
//				bos2.write(DESUtil.CBCDecrypt(data, key, iv));
//			}
//			bos2.flush();
//			bos2.close();
//			bis2.close();

			ret = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return ret;
	}

	/**
	 * 把凭证文件打包
	 * @return
	 */
	public static void zipPicAndLog(){
		String sdcard = AutoTestIO.getSDPath();
		
		File filepath = new File(sdcard + "/AutoTest/FILE");
		if(!filepath.exists()){
			filepath.mkdir();
		}
		File result = new File(sdcard + "/AutoTest/Result");
		Collection<File> logFiles = new ArrayList<File>();
		Collection<File> picFiles = new ArrayList<File>();
		if(result.exists()&&result.isDirectory()){
			for (File fileTask : result.listFiles(FOLDER_FILTER)) {
				for (File fileScript : fileTask.listFiles(FOLDER_FILTER)) {
					for (File file : fileScript.listFiles(IMAGES_FILTER)) {
						picFiles.add(file);
					}
					for (File file : fileScript.listFiles(LOG_FILTER)) {
						logFiles.add(file);
					}
				}
			}
		}

		File zipPic = new File(sdcard + "/AutoTest/FILE/Pic.zip");
		File zipLog = new File(sdcard + "/AutoTest/FILE/Log.zip");
		if(zipPic.exists()){
			zipPic.delete();
		}
		if(zipLog.exists()){
			zipLog.delete();
		}
		try{
			if(logFiles.size()>0){
				UnZipFile.zipFiles(logFiles, zipLog);
			}
			if(picFiles.size()>0){
				UnZipFile.zipFiles(picFiles, zipPic);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 上传 report
	 * @param mCxt   Context
	 * @return
	 */
	//打包上传，task结束
	public static boolean UpLoadReport(Context mCxt){
		boolean ret = false;
		String sdcard = AutoTestIO.getSDPath();
		
		List<TaskInfo> taskInfo = AutoTestIO.getTask();
		Date d = new Date();
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmmss");
		String UpDate = f.format(d);
		String fileName = sdcard + "/AutoTest/" + AutoTestIO.getDevType(mCxt) + "_" + 
							AutoTestIO.getIMEI(mCxt) + "_INPHONE_" + 
							AutoTestIO.getProvCode(mCxt) + "_" + AutoTestIO.getCityCode(mCxt) +	
							"_" + UpDate + "_001.txt";
		
		encodeReport(getReportHead(taskInfo,mCxt) , fileName);
		
		String zipName = sdcard + "/AutoTest/RESULT_" + AutoTestIO.getDevType(mCxt) + "_" + 
						AutoTestIO.getIMEI(mCxt) + "_INPHONE_" + 
						AutoTestIO.getProvCode(mCxt) + "_" + AutoTestIO.getCityCode(mCxt) +	
						"_" + UpDate + "_001.zip";
		
		File zipFile = new File(zipName);
		
		//zipPicAndLog();
		
		File file = new File(fileName);
		Collection<File> Files = new ArrayList<File>();
		Files.add(file);
		File filef = new File(sdcard + "/AutoTest/FILE");
		if(filef.exists()){
			Files.add(filef);
		}
		try {
			UnZipFile.zipFiles(Files, zipFile);
		} catch (IOException e) {
			e.printStackTrace();
			return ret;
		}

		AutoTestFtpIO.uploadReport(zipName,mCxt);
		
		return ret;
	}

	public  static class CaseExe {
		private static String ScriptName = "";
		private static int ExeNum = 0;
		
		public void setScriptName(String Name){
			if(Name != null && Name.equals("") == false){
				ScriptName = Name;
			}
		}
		public String getScriptName(){
			return ScriptName;
		}
		
		public void setExeNum(int num){
			ExeNum = num;
		}
		public int getExeNum(){
			return ExeNum;
		}
		
	}
}
