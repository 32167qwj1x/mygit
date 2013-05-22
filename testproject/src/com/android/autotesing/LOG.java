/**
 * 
 */
package com.android.autotesing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

/**
 * Log 写入系统，如不需要在T卡上写log，可以将DEBUG = false。
 * @author 
 *
 */
public class LOG {
	
	public static final int LOG_I = 0;
	public static final int LOG_W = 1;
	public static final int LOG_E = 2;
	public static final int LOG_D = 3;
	public static final int LOG_V = 4;
	public static final String TAG = "autotesing";
	
	
	public static void Log(int type,String msg){
		switch(type){
			case LOG_I:
				Logs.RecordLog("[I/" + TAG + "(" + new Date().getTime() + ")" +  ":]" + msg + "\n");
				Log.i(TAG, msg);
				break;
			case LOG_W:
				Logs.RecordLog("[W/" + TAG + "(" + new Date().getTime() + ")" +  ":]" + msg + "\n");
				Log.w(TAG, msg);
				break;
			case LOG_E:
				Logs.RecordLog("[E/" + TAG + "(" + new Date().getTime() + ")" +  ":]" + msg + "\n");
				Log.e(TAG, msg);
				break;
			case LOG_D:
				Logs.RecordLog("[D/" + TAG + "(" + new Date().getTime() + ")" +  ":]" + msg + "\n");
				Log.d(TAG, msg);
				break;
			case LOG_V:
				Logs.RecordLog("[V/" + TAG + "(" + new Date().getTime() + ")" +  ":]" + msg + "\n");
				Log.v(TAG, msg);
				break;	
		}
	}
}

class Logs
{
  private static final boolean DEBUG = true;
  public static void RecordLog(String value)
  {    
	if (!DEBUG) 
		return;
	else{
	    if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){   
	      //获取SDCard目录
	      File sdCardDir = Environment.getExternalStorageDirectory();
	      String name = sdCardDir.getPath() + "/log.txt";
	      try
	      {
	        FileOutputStream fos = new FileOutputStream(name, true);
	        fos.write(value.getBytes());
	        fos.flush();
	        fos.close();
	      } catch(IOException e){}
	    }
	  }
  }
  
  public static void RecordLog(byte[] value)
  {    
    if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){   
    //获取SDCard目录
      File sdCardDir = Environment.getExternalStorageDirectory();    
      String TxtName = sdCardDir.getPath() + "/log.txt";
      try
      {
        FileOutputStream fos = new FileOutputStream(TxtName, true);
        fos.write(value);
        fos.flush();
        fos.close();
      } catch(IOException e){}
    }
  }
}
