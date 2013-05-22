/**
 * 
 */
package com.android.autotesing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.android.xmlParse.TaskInfo;
import com.android.xmlParse.xmlParse;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;


/********************************************************************************
 * 
 * @author 
 *
 */
public class TimerTaskHandle {
	
	private static final String SERVICE_ACTION = "com.android.autotesing.START_TASK_SERVICE";
	private final int TASK_READY = -1;
	private final int TASK_RUNNING = 0;
	private final int TASK_PENDING = 1;
	private final int TASK_FINISHED = 2;
	private ArrayList<TaskInfo> mTestTaskList = new ArrayList<TaskInfo>();
	private Context mCxt;
	private final Timer timer;
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
		// TODO Auto-generated method stub
			if(!checkServiceExist()){
				TaskRecord taskRecord = new TaskRecord(mCxt);
				taskRecord.getSharedPreferences("TASK_INFO");
	    		String filename = taskRecord.getString("TASKNAME");
	    		LOG.Log(LOG.LOG_I,"task file :"+filename);
	    		if(!filename.equals("string")){
		    		if(parseXmlFile(filename)){
		    			if(mTestTaskList.size() > 0){
		    				LOG.Log(LOG.LOG_I,"auto start service...");
		    				StartTaskService(mCxt);
		    			}
		    		}
	    		}
			}else {
//				Toast.makeText(mCxt,
//						"Service is running", 
//						Toast.LENGTH_LONG).show();
				LOG.Log(LOG.LOG_I,"Service is running...");
			}
			super.handleMessage(msg);
		}
	};
	
	//初始化计时器任务。
	TimerTask task = new TimerTask() {
		@Override
		public void run() {
		// TODO Auto-generated method stub
			Message message = new Message();
			message.what = 1;
			handler.sendMessage(message);
		}
	};
	
	public TimerTaskHandle(Context cxt) {
		mCxt = cxt;
		timer = new Timer();
	}

	public void StartTimer(long period) {
		try{
		timer.schedule(task,period * 1000,period * 1000);
		}catch(IllegalArgumentException e){
			e.printStackTrace();
		}
	}
	public void CancleTimer() {
		timer.cancel();
	}	
	
	private boolean checkServiceExist(){
		ActivityManager am = (ActivityManager) mCxt.getSystemService(Context.ACTIVITY_SERVICE);
		try{
			List<ActivityManager.RunningServiceInfo> taskList = am.getRunningServices(20);
			for(int index = 0; index < taskList.size(); index++){
				RunningServiceInfo info = taskList.get(index);
				LOG.Log(LOG.LOG_I,"TimerTaskHandle packageName:" + info.service.getPackageName());
				LOG.Log(LOG.LOG_I,"TimerTaskHandle ClassName:" + info.service.getClassName());
				if(info.service.getClassName().equals("com.broadcast.ao.AutoService")){
					return true;
				}
			}
		}catch(SecurityException e){
			LOG.Log(LOG.LOG_E,e.getMessage());
		}
		return false;
	}
	
    private void StartTaskService(Context context){
		Intent intent = new Intent(SERVICE_ACTION);
		intent.putParcelableArrayListExtra("RUNNINGLIST",mTestTaskList);
		context.startService(intent);
    }

    private boolean parseXmlFile(String filename){
		File tskfile = new File(filename);
		xmlParse p = new xmlParse();
		InputStream in = null;
		if(!tskfile.exists()){
			return false;
		}
		try{
			in = new BufferedInputStream(new FileInputStream(tskfile));
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
		try{
			List<TaskInfo> tskall = p.taskParse(in);
    		Iterator<TaskInfo> it = tskall.iterator();
    		while (it.hasNext()) {
    			TaskInfo task = it.next();
    			task.setTaskPath(tskfile.getParent());
    			task.mTaskState = TASK_READY;
    			mTestTaskList.add(task);
    		}
		}catch(Exception e){
			LOG.Log(LOG.LOG_E, "Exception");
			LOG.Log(LOG.LOG_E, e.getMessage());
		}
		if(mTestTaskList.size() > 0){
			return true;
		}
		
		return false;
    }
}