/**
 * 
 */
package com.android.autotesing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.os.Environment;

/**
 * @author 
 *
 */
public class FileService {

	private static FileOutputStream mOutStream = null;
	
	public static boolean fileOpen(String filename){
		boolean ret = false;
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			try{
				File file = new File(filename);
				if(file.exists()){
					file.delete();
				}
				if(mOutStream != null){
					try{
						mOutStream.close();
					}catch(IOException e){
						LOG.Log(LOG.LOG_E,"stream exist close throw exception");
						LOG.Log(LOG.LOG_E,e.getMessage());
					}
				}
				mOutStream = new FileOutputStream(filename,true);
				ret = true;
			}catch(FileNotFoundException e){
				LOG.Log(LOG.LOG_E,"open file throw exception");
				LOG.Log(LOG.LOG_E,e.getMessage());
				ret = false;
			}
		}else {
			//TODO pls insert sdcard
			ret = false;
		}
		return ret;
	}
	
	public static void fileClose(){
		try{
			if(mOutStream != null)
			{
				mOutStream.close();
			}
		}catch(IOException e){
			LOG.Log(LOG.LOG_E,"close file throw exception");
			LOG.Log(LOG.LOG_E,e.getMessage());
		}
	}
	
    public static void writeAppend(String content){
    	try{
    		content = content + "\n";
    		mOutStream.write(content.getBytes());
    	}catch(IOException e){
			LOG.Log(LOG.LOG_E,"write file throw exception");
			LOG.Log(LOG.LOG_E,e.getMessage());
    	}
    } 
        
    public void saveToSDCard(String filename, String content) throws Exception{
    	File file = new File(Environment.getExternalStorageDirectory(),filename);
    	FileOutputStream outStream = new FileOutputStream(file);
    	outStream.write(content.getBytes());
    	outStream.close();
    }
    
    public String readFile(String filename) throws Exception{
    	File file = new File(Environment.getExternalStorageDirectory(),filename);
    	FileInputStream inStream = new FileInputStream(file);
    	byte[] data = readData(inStream);
    	return new String(data);
    }
    
    private byte[] readData(FileInputStream inStream) throws Exception{
    	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    	byte[] buffer = new byte[1024];
    	int len = 0;
    	while( (len = inStream.read(buffer))!= -1){
    	outStream.write(buffer, 0, len);
    	}
    	outStream.close();
    	inStream.close();
    	return outStream.toByteArray();
    }
}
