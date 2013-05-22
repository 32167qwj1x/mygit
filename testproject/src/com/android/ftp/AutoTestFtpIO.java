package com.android.ftp;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.android.autotesing.DataBaseRestore;
import com.android.io.AutoTestIO;

import android.content.Context;
import android.util.Log;

public class AutoTestFtpIO {

	//FTP服务器地址
	public static String FTPURL = "218.206.179.30"; 
	//端口号
	static final int FTPPort = 3456;
	//用户名密码
	static final String FTPUser = "nbpt";
	public static String FTPPass = "a5q8t6p0c#";
	//本地下载文件夹地址
	static final String FTPLocalPath = "/AutoTest/";
	//Log Tag
	static final String TAG = "AutoTestFtpIO";
	
//	public static String FTPURL = "nbpt.cn"; 
//	static final int FTPPort = 7801;
//	static final String FTPUser = "nbpt";
//	public static String FTPPass = "g6s8m3t7s";
	
	
	//设置FTP服务器地址
	public static void setFTPUserAdress(String adress){
		if(adress != null && adress.equals("") == false){
			FTPURL = adress;
		}
	}
	//设置FTP服务器用户密码
	public static void setFTPUserPassword(String password){
		if(password != null && password.equals("") == false){
			FTPPass = password;
		}
	}
	
	/********************************************************************************
	 * 从服务器获取脚本和Case对应xml文件 ftpScript.xml
	 * @param DevType 设备类型
	 * @param Cxt
	 * @return
	 ********************************************************************************/
	public static boolean getScriptXML(String DevType,Context Cxt) {
		ContinueFTP myFtp = new ContinueFTP();
		Date xmlDate = new Date();
		xmlDate.setYear(xmlDate.getYear()- 15);
		String xmlFileName = "";
		DataBaseRestore DB = DataBaseRestore.getInstance(Cxt);
		
		String LocalPath = AutoTestIO.getSDPath() + FTPLocalPath;
		
		boolean ret = true;
		try {
			File f = new File(LocalPath);
			f.mkdir();
			
			setFTPUserAdress(DB.getServerAdressValue());
			setFTPUserPassword(DB.getServerPasswordValue());
			
			myFtp.connect(FTPURL, FTPPort, FTPUser, FTPPass);
			myFtp.ftpClient.changeWorkingDirectory("test_script/"+ DevType + "/INPHONE");
			myFtp.ftpClient.enterLocalPassiveMode();
			
			FTPFile[] ftpFilelist = myFtp.ftpClient.listFiles();

			//找到最新的xml文件
			for (int i = 0; i < ftpFilelist.length; ++i) {
				String tempName = ftpFilelist[i].getName();
				int len = tempName.length();
				if(tempName.length() > 5 && tempName.substring(len - 4, len).equals(".xml")){
					String datestr = tempName.substring(len - 22 , len - 8);
					Date d = new Date();
					try{
						d.setYear(Integer.parseInt(datestr.substring(0,4))-1900);
						d.setMonth(Integer.parseInt(datestr.substring(4,6))-1);
						d.setDate(Integer.parseInt(datestr.substring(6,8)));
						d.setHours(Integer.parseInt(datestr.substring(8,10)));
						d.setMinutes(Integer.parseInt(datestr.substring(10,12)));
						d.setSeconds(Integer.parseInt(datestr.substring(12,14)));
						if(d.after(xmlDate)){
							xmlDate = d;
							xmlFileName = tempName;
						}
					}catch(Exception e){
						
					}
				}
			}

			if(!xmlFileName.equals("")){
				File scriptXML = new File(LocalPath + "ftpScript.xml");
				if(scriptXML.exists()){
					scriptXML.delete();
				}
				System.out.println(myFtp.download(xmlFileName,LocalPath + "ftpScript.xml"));
			}
			else{
				ret = false;
			}

			myFtp.disconnect();
		} catch (IOException e) {
			System.out.println("连接FTP出错：" + e.getMessage());
			return false;
		}
		
		return ret;
	}
	
	/********************************************************************************
	 * 从服务器下载所有脚本文件
	 * @param DevType
	 * @param TargetId
	 * @param Cxt
	 * @return
	 ********************************************************************************/
	public static boolean getAllScriptFile(String DevType,String TargetId,Context Cxt) {
		boolean ret = true;
		ContinueFTP myFtp = new ContinueFTP();
		DataBaseRestore DB = DataBaseRestore.getInstance(Cxt);
		String LocalPath = AutoTestIO.getSDPath() + FTPLocalPath;
		
		try {
			
			setFTPUserAdress(DB.getServerAdressValue());
			setFTPUserPassword(DB.getServerPasswordValue());
			
			myFtp.connect(FTPURL, FTPPort, FTPUser, FTPPass);
			myFtp.ftpClient.changeWorkingDirectory("test_script/"+ DevType + "/INPHONE");
			myFtp.ftpClient.enterLocalPassiveMode();
			myFtp.ftpClient.pasv();
			
			FTPFile[] ftpFilelist = myFtp.ftpClient.listFiles();

			for (int i = 0; i < ftpFilelist.length; ++i) {
				boolean destFolder = false;
				if(ftpFilelist[i].getName().equals("common") && ftpFilelist[i].isDirectory()){
					myFtp.ftpClient.changeWorkingDirectory("common");
					destFolder = true;
				}
				else if(ftpFilelist[i].getName().equals(TargetId) && ftpFilelist[i].isDirectory()){
					myFtp.ftpClient.changeWorkingDirectory(TargetId);
					destFolder = true;
				}
				
				if(destFolder){
					FTPFile[] ftpScriptlist = myFtp.ftpClient.listFiles();
					for(int t = 0; t < ftpScriptlist.length ; ++t){
						String tempName = ftpScriptlist[t].getName();
						int len = tempName.length();
						if(len < 4){
							continue;
						}
						if(tempName.substring(len - 4, len).equals(".zip")){
							File scriptFile = new File(LocalPath + tempName);
							if(scriptFile.exists()){
								if(true/*ftpScriptlist[t].getSize() != scriptFile.length()*/){
									scriptFile.delete();
								}
							}
							System.out.println(myFtp.download(tempName,LocalPath + tempName));
							
							Log.i(TAG, "## 解压下载好的zip文件 ##");
							
							scriptFile = new File(LocalPath + tempName);
							String zipFolder = tempName.substring(0, len - 4);
							File f = new File(LocalPath + zipFolder);
							f.mkdir();
							try {
								UnZipFile.unZipFile(scriptFile, "sdcard/AutoTest/" + zipFolder);
							} catch (Exception e) {
								e.printStackTrace();
								return false;
							}
							
						}
					}
					myFtp.ftpClient.changeToParentDirectory();
				}
			}


			myFtp.disconnect();
		} catch (IOException e) {
			System.out.println("连接FTP出错：" + e.getMessage());
			return false;
		}		
		
		return ret;
	}

	public static boolean downloadScriptFile( String target , String name , Context Cxt) {
		
		String DevType = AutoTestIO.getDevType(Cxt);
		boolean ret = true;
		ContinueFTP myFtp = new ContinueFTP();
		String LocalPath = AutoTestIO.getSDPath() + FTPLocalPath;
		
		int len = name.length();
		if(len > 4 && name.substring(len - 4, len).equals(".zip")){
		
			try {
				myFtp.connect(FTPURL, FTPPort, FTPUser, FTPPass);
				myFtp.ftpClient.changeWorkingDirectory("test_script/"+ DevType + "/INPHONE");
				myFtp.ftpClient.enterLocalPassiveMode();
				myFtp.ftpClient.pasv();
				
				myFtp.ftpClient.changeWorkingDirectory(target);
				File f = new File(LocalPath + name);
				if(f.exists()){
					f.delete();
				}
				System.out.println(myFtp.download(name,LocalPath + name));
	
				File scriptFile = new File(LocalPath + name);
				if(scriptFile.exists()){
					Log.i(TAG, "## 解压下载好的zip文件 ##");
					String zipFolder = name.substring(0, len - 4);
					File fz = new File(LocalPath + zipFolder);
					fz.mkdir();
					try {
						UnZipFile.unZipFile(scriptFile, "sdcard/AutoTest/" + zipFolder);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				myFtp.disconnect();
			} catch (IOException e) {
				System.out.println("连接FTP出错：" + e.getMessage());
				return false;
			}		
		}
		return ret;
	}
	
	/********************************************************************************
	 * 上传日志文件
	 * @param fileName
	 * @param Cxt
	 * @return
	 ********************************************************************************/
	public static boolean uploadReport(String fileName,Context Cxt) {
		boolean ret = false;
		ContinueFTP myFtp = new ContinueFTP();
		DataBaseRestore DB = DataBaseRestore.getInstance(Cxt);
		
		Date d = new Date();
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
		String Date = f.format(d);
		
		try {
			setFTPUserAdress(DB.getServerAdressValue());
			setFTPUserPassword(DB.getServerPasswordValue());
			myFtp.connect(FTPURL, FTPPort, FTPUser, FTPPass);
			myFtp.ftpClient.changeWorkingDirectory("test_result/upload");
			myFtp.ftpClient.makeDirectory(Date);
			myFtp.ftpClient.changeWorkingDirectory(Date);
			myFtp.ftpClient.enterLocalPassiveMode();
			myFtp.ftpClient.pasv();
			String[] str = fileName.split("/");
			if (str.length >= 1){
				String fileNameServer = str[str.length - 1];
				System.out.println(myFtp.upload(fileName, fileNameServer + ".tmp"));
				System.out.println("文件：" + fileNameServer + ".tmp 上传完成！");
				myFtp.renameFile(fileNameServer + ".tmp", fileNameServer);
				System.out.println("文件重命名为：" + fileNameServer + " ！");
				ret = true;
			}
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("文件上传失败！");
			return false;
		}
		
		return ret;
	}
}
