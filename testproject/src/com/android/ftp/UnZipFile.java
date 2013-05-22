package com.android.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;

import android.util.Log;

public class UnZipFile {
	
	static final String TAG = "UnZipFile";
	private static final int BUFF_SIZE = 1024 * 1024;
	
	/**
     * 解压缩功能.
     * 将zipFile文件解压到folderPath目录下.
     * @throws Exception
 */

	public static int unZipFile(File zipFile, String folderPath)throws ZipException,IOException {
         ZipFile zfile=new ZipFile(zipFile);
         Enumeration<?> zList=zfile.entries();
         ZipEntry ze=null;
         byte[] buf=new byte[1024];
         while(zList.hasMoreElements()){
             ze=(ZipEntry)zList.nextElement();    
             if(ze.isDirectory()){
                 Log.d("TAG", "ze.getName() = "+ze.getName());
                 String dirstr = folderPath + ze.getName();
                 //dirstr.trim();
                 dirstr = new String(dirstr.getBytes("8859_1"), "GB2312");
                 Log.d("TAG", "str = "+dirstr);
                 File f=new File(dirstr);
                 f.mkdir();
                 continue;
             }
             Log.d("TAG", "ze.getName() = "+ze.getName());
             OutputStream os=new BufferedOutputStream(new FileOutputStream(getRealFileName(folderPath, ze.getName())));
             InputStream is=new BufferedInputStream(zfile.getInputStream(ze));
             int readLen=0;
             while ((readLen=is.read(buf, 0, 1024))!=-1) {
                 os.write(buf, 0, readLen);
             }
             is.close();
             os.close();    
         }
         zfile.close();
         Log.d("TAG", "finishssssssssssssssssssss");
         return 0;
     }
 
     /**
     * 给定根目录，返回一个相对路径所对应的实际文件名.
     * @param baseDir 指定根目录
     * @param absFileName 相对路径名，来自于ZipEntry中的name
     * @return java.io.File 实际的文件
 */
     public static File getRealFileName(String baseDir, String absFileName){
		String[] dirs = absFileName.split("/");
		File ret = new File(baseDir);
		String substr = null;
		if (dirs.length > 1) {
			for (int i = 0; i < dirs.length - 1; i++) {
				substr = dirs[i];
				try {
					// substr.trim();
					substr = new String(substr.getBytes("8859_1"), "GB2312");

				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ret = new File(ret, substr);
			}
			Log.d("TAG", "1ret = " + ret);
			if (!ret.exists())
				ret.mkdirs();
			substr = dirs[dirs.length - 1];
			try {
				// substr.trim();
				substr = new String(substr.getBytes("8859_1"), "GB2312");
				Log.d("TAG", "substr = " + substr);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			ret = new File(ret, substr);
			Log.d("TAG", "2ret = " + ret);
			return ret;
		}
		else{
			ret = new File(baseDir, absFileName);
		}
		return ret;
	}
     
	private static void zipFile(File resFile, ZipOutputStream zipout,
			String rootpath) throws FileNotFoundException, IOException {
		rootpath = rootpath
				+ (rootpath.trim().length() == 0 ? "" : File.separator)
				+ resFile.getName();
//		rootpath = new String(rootpath.getBytes("8859_1"), "GB2312");
		if (resFile.isDirectory()) {
			//modify Hz
			String rootString = rootpath;
			if(!rootString.endsWith("/"))
			{
				rootString += '/';
			}
			ZipEntry ze = new ZipEntry(rootString);
			ze.setTime(resFile.lastModified());
			zipout.putNextEntry(ze);
			zipout.closeEntry();
			
			File[] fileList = resFile.listFiles();
			for (File file : fileList) {
				zipFile(file, zipout, rootpath);
			}
		} else {
			byte buffer[] = new byte[BUFF_SIZE];
			BufferedInputStream in = new BufferedInputStream(
					new FileInputStream(resFile), BUFF_SIZE);
			zipout.putNextEntry(new ZipEntry(rootpath));
			int realLength;
			while ((realLength = in.read(buffer)) != -1) {
				zipout.write(buffer, 0, realLength);
			}
			in.close();
			zipout.flush();
			zipout.closeEntry();
		}
	}
	
	public static void zipFiles(Collection<File> resFileList, File zipFile)
			throws IOException {
		ZipOutputStream zipout = new ZipOutputStream(new BufferedOutputStream(
				new FileOutputStream(zipFile), BUFF_SIZE));
		for (File resFile : resFileList) {
			zipFile(resFile, zipout, "");
		}
		zipout.close();
	}

}
