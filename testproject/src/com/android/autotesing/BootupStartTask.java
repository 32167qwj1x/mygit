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

import com.android.io.AutoTestIO;
import com.android.xmlParse.TaskInfo;
import com.android.xmlParse.xmlParse;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/****************************************************************************
 * @author 
 * 
 ****************************************************************************/
public class BootupStartTask extends BroadcastReceiver {
	/* 开启启动广播 */
	private static final String ACTION = "android.intent.action.BOOT_COMPLETED";
	/* 任务执行服务进程启动命令 */
	private static final String SERVICE_ACTION = "com.android.autotesing.START_TASK_SERVICE";
	/* 任务初始化状态 */
	private final int TASK_READY = -1;
	/* 任务列表 */
	private ArrayList<TaskInfo> mTestTaskList = new ArrayList<TaskInfo>();
	/* 数据库 */
	private TaskRecord mTaskRecord;
	private int count = 0;
	private Context mCxt;
	/* 线程控制变量 */
	private ThreadStart handler = null;
	
	/****************************************************************************
	 * 检查服务是否已经开启
	 * @param context
	 * @param name
	 * @return
	 ***************************************************************************/
	private boolean checkServiceExist(Context context, String name) {
		ActivityManager activityManger = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> list = activityManger
				.getRunningServices(20);
		if (list == null)
			return false;
		for (int i = 0; i < list.size(); i++) {
			ActivityManager.RunningServiceInfo serviceinfo = list.get(i);
			if (serviceinfo.service.getClassName().equals(name)) {
				return true;
			}
			LOG.Log(LOG.LOG_I, "class name[" + i + "]= "
					+ serviceinfo.service.getClassName());
			LOG.Log(LOG.LOG_I, "process name[" + i + "]= "
					+ serviceinfo.process);
		}
		return false;
	}
	
	/****************************************************************************
	 * 
	 * @author 
	 *
	 ***************************************************************************/
	private class ThreadStart extends Thread {

		public void run() {
			try {
				while (count <= 20) {
					StartJarService(mCxt);
					//开启成功则不再开启
					if (AutoTestIO.startstate == false) {
						break;
					}
					count++;
					Thread.sleep(3000);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}
	
	/****************************************************************************
	 * 广播接收器
	 ***************************************************************************/
	@Override
	public void onReceive(Context context, Intent intent) {

		mTaskRecord = new TaskRecord(context);
		mTaskRecord.getSharedPreferences("TASK_INFO");
		mCxt = context;

		if (intent.getAction().equals(ACTION)) {
			if (handler == null) {
				handler = new ThreadStart();
				AutoTestIO.startstate = true;
				handler.start();
			}

		} else if (intent.getAction().equals(
				"com.android.autotesing.EXEC_TO_TASK")) {
			LOG.Log(LOG.LOG_I, "recevie exec running broadcast!");
			if (true) {
				AutoTestIO.startstate = false;
				if (handler != null && handler.isAlive() == true) {
					handler.stop();
				}
			}
			if (intent.getStringExtra("TASK_SEND_ACK").equals(
					"TASK_SEND_ACK_RSP")) {
				if (intent.getBooleanExtra("EXEC_RUNNING_DATA", false)) {
					if (checkServiceExist(context,
							"com.android.autotesing.TaskService")) {
						LOG.Log(LOG.LOG_I, "Task Service is running....");
						return;
					}
					String filename = mTaskRecord.getString("TASKNAME");
					LOG.Log(LOG.LOG_I, "task file :" + filename);
					if (!filename.equals("")) {
						if (parseXmlFile(filename)) {
							if (mTestTaskList.size() > 0) {
								LOG.Log(LOG.LOG_I,
										"bootup task service starting!");
								StartTaskService(context);
							}
						}
					}
				}
			}
		}
	}

	private void StopTaskService(Context context) {
		Intent it = new Intent(context, TaskService.class);// SERVICE_ACTION
		context.stopService(it);
	}
    /***************************************************************************
     * 开启后台测试服务
     * @param context
     ***************************************************************************/
	private void StartTaskService(Context context) {
		Intent intent = new Intent(SERVICE_ACTION);
		intent.putParcelableArrayListExtra("RUNNINGLIST", mTestTaskList);
		context.startService(intent);
	}

	
	/***************************************************************************
	 * 解析任务列表文件
	 * @param filename
	 * @return
	 ***************************************************************************/
	private boolean parseXmlFile(String filename) {
		File tskfile = new File(filename);
		xmlParse p = new xmlParse();
		InputStream in = null;
		if (!tskfile.exists()) {
			return false;
		}
		try {
			in = new BufferedInputStream(new FileInputStream(tskfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			// user_select == true
			String list[] = mTaskRecord.getString("TASKLIST").split("\\|");
			List<TaskInfo> tskall;
			in.close();
			tskall = AutoTestIO.getTask();

			Iterator<TaskInfo> it = tskall.iterator();
			while (it.hasNext()) {
				TaskInfo task = it.next();
				task.setTaskPath(tskfile.getParent());
				task.mTaskState = TASK_READY;
				for (int i = 0; i < list.length; i++) {
					if (task.getTestTaskCode().equals(list[i].split("\\#")[0])) {
						LOG.Log(LOG.LOG_I, "new task insert :"
								+ task.getTestTaskCode());
						mTestTaskList.add(task);
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (mTestTaskList.size() > 0) {
			return true;
		}

		return false;
	}

	/***************************************************************************
	 * 启动底层执行服务
	 * @param Cxt
	 * @return
	 ***************************************************************************/
	public boolean StartJarService(Context Cxt) {
		AutoTestIO.execCommandAsRoot("/system/bin/TestStartMain &");
		Intent it = new Intent("com.android.autotesing.TASK_TO_EXEC");
		it.putExtra("TASK_SEND_ACK", "TASK_SEND_ACK_STATE_REQ");
		Cxt.sendBroadcast(it);
		return true;
	}

}