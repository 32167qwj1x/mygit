/**
 * 
 */
package com.android.autotesing;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.android.http.AutoTestHttpIO;
import com.android.io.AutoTestIO;
import com.android.io.Report;
import com.android.xmlParse.CaseInfo;
import com.android.xmlParse.TaskInfo;
import com.android.xmlParse.WarningInfo;
import com.android.xmlParse.xmlParse;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.database.Cursor;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Parcelable.Creator;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/*********************************************************************************
 * 后台启用服务
 * @author
 * 
 ********************************************************************************/
public class TaskService extends Service {
	
	//服务程序与底层通信的主要消息广播
	private static final String USER_DFINED_TASK_ID = "com.android.autotesing.TASK_TO_EXEC";
	//底层执行程序发上来的主要消息广播
	private static final String USER_DFINED_EXEC_ID = "com.android.autotesing.EXEC_TO_TASK";
	//任务开始时设置的定时广播，广播接收到时，是整个任务的最初触发
	private static final String ALARM_BROADCAST_ID = "android.intent.action.TASK_ALARM";
	//此广播消息预留，原有设计是定时打包测试结果
	private static final String PACKAGE_UPLOAD_BROADCAST_ID = "android.intent.action.UPLOAD_ALARM";
	//启动自动更新服务
	private static final String DATAUPDATE_SERVICE = "com.android.autotesing.DATA_UPDATE_SERVICE_START_ACTION";
	//如在执行过程中发现有更新，结束后，立即发起更新
	private static final String UPDATE_TASK_ID = "android.intent.action.UPDATE_TASK_FILE";
	//预留消息
	private static final String REFRESH_RUNNING_TASK_LIST = "android.intent.action.REFRESH_RUNNING_TASKS";
	//连续执行失败时，会发送此广播给更新服务，启动告警
	private final static String TASK_FALIED_WARNING = "android.intent.action.TASK_EXE_FALED_WARNING";
	//测试类型，时延、成功率还是下载速率
	private static final String TESTVALUE_TIME = "Time";
	private static final String TESTVALUE_RATE = "Rate";
	private static final String TESTVALUE_SPEED = "Speed";

	//正在运行的任务列表、从Activity获得
	private ArrayList<TaskInfo> mRunningTaskList = new ArrayList<TaskInfo>();

	//设置任务时间类型
	private static final int TASK_START_TIME_TYPE = 0;
	private static final int TASK_END_TIME_TYPE = 1;
	private static final int TASK_END_TYPE = 3;

	private static final int EXECUTE_TYPE_TIME = 1; // 按时执行
	private static final int EXECUTE_TYPE_NUM = 2; // 按次执行
	private static final int ITERATION_TYPE_NONE = 0; // 不限
	private static final int ITERATION_TYPE_DAY = 1; // 天
	private static final int ITERATION_TYPE_WEEK = 2; // 周
	private static final int ITERATION_TYPE_MONTH = 3; // 月
	
	//任务执行状态
//	private final int TASK_READY = -1;
	private final int TASK_RUNNING = 0;
	private final int TASK_PENDING = 1;
//	private final int TASK_BGRUNNING = 2;
	private final int TASK_FINISHED = 3;
	private final int TASK_TIME_OUT = 4;
	
	//从任务队列中获取顶端队列执行的新线程
	private TaskHandle mReceiverHandler;
	//数据库
	private TaskDB mTaskDB;
	//获取手机号码等信息
	private TelephonyManager mTelem;
	//预留，无用
	private TestReport mTestReport;

	//设备状态定义
	private final String DEV_STATE_IDLE = "00";
	private final String DEV_STATE_BUSY = "01";
	private final String DEV_STATE_EXCEPTION = "02";
	private final String DEV_STATE_OFFLINE = "03";

	//设备信息数据库
	private DataBaseRestore DB = null;
	//用于执行过程中，屏蔽灭屏
	private PowerManager powerManager = null;
	private WakeLock wakeLock = null;
	
	//设置闹钟程序时，Requestcode做为唯一标识
	private int mReqCode = 0;
	private List<Integer> mReqCodelist = new ArrayList<Integer>();
	private boolean isLoading = true;
	private boolean flag = true;
	private Context context;
	
	//任务获取线程是否已被结束
	public boolean mIsCancel = false;

	//任务起始时间
	private Date mDevBusyStartTime = new Date();
	private Date mDevBusyEndTime = new Date();
	
	//开始列表中任务的单独线程
	private ThreadStart mTaskThread = null;
	
	//抓包线程
	private ThreadPacket mCasePacket = null;
	
	//服务被关闭时，需要取消所有闹钟程序，需要启动单独线程调用，否则会造成假死
	private CancelAlarmThread mTaskStopThread = null;
	
	private boolean mTaskThreadkilled = false;
	private int mTaskFailedNum = 0;
	
	//任务持续失败的最大允许次数
	private int mContinueFailMax = -1;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	/********************************************************************************
	 * 服务开始
	 ********************************************************************************/
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		context = this;
		// 获取SDCard目录
		if ((mTaskStopThread != null && mTaskStopThread.isAlive() == true)
				|| (mTaskThread != null && mTaskThread.isAlive() == true)) {
			Toast.makeText(context, "系统繁忙，无法开始测试任务！！！", Toast.LENGTH_LONG)
					.show();
			return;
		}
		File sdCardDir = Environment.getExternalStorageDirectory();
		String name = sdCardDir.getPath() + "/log.txt";
		File file = new File(name);
		if (file.exists()) {
			file.delete();
		}
		mReceiverHandler = new TaskHandle(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ALARM_BROADCAST_ID);
		filter.addAction(USER_DFINED_EXEC_ID);
		filter.addAction(REFRESH_RUNNING_TASK_LIST);
		this.registerReceiver(mReceiverHandler, filter);
		mTelem = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		// start task
		mRunningTaskList = intent.getParcelableArrayListExtra("RUNNINGLIST");
		if (mRunningTaskList != null && mRunningTaskList.size() > 0) {
			// init DB
			mTaskDB = new TaskDB(this);
			mTestReport = new TestReport(mTaskDB);
			mTaskDB.openDB();
			DeleteDBIfNotExist();

			if (mTaskThread == null) {
				mTaskThread = new ThreadStart();
			}
			if (mTaskThread.isAlive() == false) {
				mTaskThread.start();
			}

		}
		
		//初始化最大允许失败次数，从配置文件中获取
		InitilizeMaxFailNum();
		
		//服务启动，初始设备状态设置为“空闲”
		DB = DataBaseRestore.getInstance(this);
		DB.SetDevStateDBValue(this, DEV_STATE_IDLE);
		
		//屏蔽屏幕灭屏，不允许屏幕熄灭
		this.powerManager = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		this.wakeLock = this.powerManager.newWakeLock(
				PowerManager.FULL_WAKE_LOCK, "My Lock");
		this.wakeLock.acquire();

		// Start Update Data Service
		DataUpdateServiceStart();
	}
	/********************************************************************************
	 * 初始化最小告警失败次数，当任务执行连续失败至此次数时，发送告警信息
	 ********************************************************************************/
	private void InitilizeMaxFailNum() {
		InputStream warninginputstream = null;
		
		//获取配置文件路径
		File warningFile = new File(AutoTestIO.getSDPath() + "/AutoTest"
				+ "WarningSetting.xml");
		if (warningFile.exists()) {
			try {
				//解析配置文件
				WarningInfo WarningInfo = null;
				xmlParse Parse = new xmlParse();
				warninginputstream = new BufferedInputStream(
						new FileInputStream(warningFile));
				WarningInfo = Parse.WarningParse(warninginputstream);
				mContinueFailMax = WarningInfo.GetContinuFailNum();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				LOG.Log(LOG.LOG_E, "parseWarningInfo Exception");
				LOG.Log(LOG.LOG_E, e.getMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/********************************************************************************
	 * 开启全部任务线程，由于任务可能时间跨度很大，所以需要单独线程设置闹钟，避免卡死的情况
	 * @author 
	 *
	 ********************************************************************************/
	private class ThreadStart extends Thread {
		public void run() {
			try {
				while (isLoading) {
					mTaskThreadkilled = false;
					StartAllTask(mRunningTaskList);
					Thread.sleep(20);
				}
				mTaskThreadkilled = true;
				Message message = new Message();
				message.what = 1;
				handler.sendMessage(message);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case 1: {
				Toast.makeText(context, "测试任务加载完毕！！！", Toast.LENGTH_LONG)
						.show();
				LOG.Log(LOG.LOG_I, "测试任务加载完毕！！！");
			}
				break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		LOG.Log(LOG.LOG_I, "service stop.....");
		if (mReceiverHandler != null) {
			LOG.Log(LOG.LOG_I, "mReceiverHandler is not null");
			this.unregisterReceiver(mReceiverHandler);
		}
		FileService.fileClose();
		if (mRunningTaskList != null) {
			mRunningTaskList.clear();
		}

		//关闭数据库
		if (mTaskDB != null)
			mTaskDB.closeDB();

		//释放屏幕黑屏屏蔽
		if (wakeLock != null) {
			wakeLock.release();
		}

		// Stop Data update service
		DataUpdateServiceStop();
		
		//关闭线程
		if (mTaskThread.isAlive() == true) {
			mTaskThread.stop();
			mTaskThreadkilled = true;
		}

		if (mTaskStopThread == null) {
			mTaskStopThread = new CancelAlarmThread();
		}
		if (mTaskStopThread.isAlive() == false) {
			mTaskStopThread.start();
		}
		mReqCode = 0;
		mReqCodelist.clear();
		mIsCancel = true;
	}

	/********************************************************************************
	 * 停止更新服务
	 ********************************************************************************/
	private void DataUpdateServiceStop() {
		LOG.Log(LOG.LOG_I, "DATA UPDATE service stop.....");
		Intent intent;
		intent = new Intent(DATAUPDATE_SERVICE);
		this.stopService(intent);
	}

	/********************************************************************************
	 * 启动更新服务，如未开始测试任务，定时更新不起作用
	 ********************************************************************************/
	private void DataUpdateServiceStart() {
		LOG.Log(LOG.LOG_I, "DATA UPDATE service start.....");
		Intent upintent;
		upintent = new Intent(DATAUPDATE_SERVICE);
		this.startService(upintent);
	}

	/********************************************************************************
	 * 设置任务开始的闹钟时间，闹钟信息为任务开启的首要触发点
	 * @param date
	 * @param datetype 0表示任务开始，1表示任务结束
	 * @param task 被闹钟程序带入到任务执行
	 * @param index  任务列表中的任务顺序
	 ********************************************************************************/
	private void startAlarm(Date date, int datetype, TaskInfo task, int index) {
		if (date.after(task.getEndDate())) {
			LOG.Log(LOG.LOG_I, task.getTestTaskCode()
					+ ": validDate after task end time!!!");
			//任务已过期
			task.mTaskState = TASK_TIME_OUT;
			return;
		}
		try {
			
			//取消闹钟时，requestcode做为闹钟序列的唯一标识
			mReqCode++;
			task.mRequestCode = mReqCode;
			if (datetype == 0) {
				task.mTaskState = TASK_PENDING;
				task.mExecStartDate = date.getTime();
				LOG.Log(LOG.LOG_I, "--------------------------------------");
				LOG.Log(LOG.LOG_I, task.getTestTaskCode() + " START TIMER...");
				LOG.Log(LOG.LOG_I, "vialdDate start:" + date.toLocaleString());
			} else {
				task.mTaskState = TASK_PENDING;
				task.mExecEndDate = date.getTime();
				LOG.Log(LOG.LOG_I, "--------------------------------------");
				LOG.Log(LOG.LOG_I, task.getTestTaskCode() + " END TIMER...");
				LOG.Log(LOG.LOG_I, "vialdDate end:" + date.toLocaleString());
			}
			Calendar calendar = Calendar.getInstance();

			calendar.set(Calendar.YEAR, date.getYear() + 1900);
			calendar.set(Calendar.MONTH, date.getMonth());
			calendar.set(Calendar.DAY_OF_MONTH, date.getDate());
			calendar.set(Calendar.HOUR_OF_DAY, date.getHours());
			calendar.set(Calendar.MINUTE, date.getMinutes());
			calendar.set(Calendar.SECOND, date.getSeconds());
			calendar.set(Calendar.MILLISECOND, 0);
			/* 指定闹钟设置时间到时要运行CallAlarm.class */
			Intent intent = new Intent(ALARM_BROADCAST_ID);
			intent.putExtra("DATE_TYPE", datetype);
			intent.putExtra("RUNNING_TASK", task);
			intent.putExtra("RUNNING_TASK_INDEX", index);
			/* 创建PendingIntent */
			PendingIntent sender = PendingIntent.getBroadcast(this, mReqCode,
					intent, PendingIntent.FLAG_UPDATE_CURRENT);
			if (task.getExeType() == EXECUTE_TYPE_NUM
					&& datetype != TASK_END_TYPE) {
				Cursor cursor = mTaskDB.getRequestCodeFromRequestCode(task
						.getTestTaskCode(), mReqCode);
				if (cursor == null || cursor.getCount() == 0) {
					mTaskDB.insertRequestCodeTable(task.getTestTaskCode(),
							mReqCode, 0, datetype);
				}
				cursor.close();
			}
			AlarmManager am;
			am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			am.cancel(sender);
			am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
			LOG.Log(LOG.LOG_I, "current request code:" + mReqCode);
			mReqCodelist.add(mReqCode);
		} catch (Exception e) {
			LOG.Log(LOG.LOG_E, "startAlarm exception");
			LOG.Log(LOG.LOG_E, e.getMessage());
		}
	}

	/********************************************************************************
	 * 安序列，取消所有闹钟程序
	 ********************************************************************************/
	private void cancelAlarm() {
		if (mReqCodelist.size() > 0) {
			for (int i = 0; i < mReqCodelist.size(); i++) {
				StopAlarm(mReqCodelist.get(i));
			}
		}
	}

	public void StopAlarm(int code) {
		/* 指定闹钟设置时间到时要运行CallAlarm.class */
		Intent intent = new Intent(ALARM_BROADCAST_ID);
		/* 创建PendingIntent */
		PendingIntent sender = PendingIntent.getBroadcast(this, code, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am;
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
	}

	/********************************************************************************
	 * 开启所有任务，并判断任务起始时间是否已经过时。
	 * @param runningTaskList 需要执行任务列表
	 ********************************************************************************/
	public void StartAllTask(ArrayList<TaskInfo> runningTaskList) {
		// sort
		Collections.sort(runningTaskList, new PendingQComparePriority());
		try {
			int index = 0;
			Iterator<TaskInfo> it = runningTaskList.iterator();
			while (it.hasNext()) {
				TaskInfo item = it.next();
				if (mTaskThreadkilled == true) {
					break;
				}
				if (item.mTaskState != TASK_PENDING) {
					int DAYS = getIntervalDays(item.getStartDate(), item
							.getEndDate());
					// ExeType 1 按时执行 2 按次执行
					if (item.getExeType() == EXECUTE_TYPE_TIME) {
						LOG.Log(LOG.LOG_I, item.getTestTaskCode()
								+ " Exec Type EXECUTE_TYPE_TIME");
						if (item.getIterationType() == ITERATION_TYPE_DAY
								|| item.getIterationType() == ITERATION_TYPE_NONE) {
							LOG.Log(LOG.LOG_I, item.getTestTaskCode()
									+ " Exec Type ITERATION_TYPE_DAY");
							int day = item.getStartDate().getDate();
							int month = item.getStartDate().getMonth() + 1;
							int year = item.getStartDate().getYear() + 1900;
							int add = 0, pos = 0;
							for (int i = 0; i <= DAYS; i++) {
								Date tmpd = new Date();
								if (mTaskThreadkilled == true) {
									break;
								}
								if (day + i > getMonthTotalDays(year, month)) {
									i = 0;
									day = 1;
									tmpd.setDate(day);
									month++;
									if (month > 12) {
										i = 0;
										month = 1;
										year++;
									}
									tmpd.setMonth(month - 1);
									tmpd.setYear(year - 1900);
									DAYS -= add;
									add = 0;
								} else {
									tmpd.setDate(day + i);
									tmpd.setMonth(month - 1);
									tmpd.setYear(year - 1900);
								}
								add++;
								pos++;
								tmpd.setHours(item.getExeEndTime().getHours());
								tmpd.setMinutes(item.getExeEndTime()
										.getMinutes());
								tmpd.setSeconds(item.getExeEndTime()
										.getSeconds());
								Date curD = new Date();
								Date endD = tmpd;
								if (endD.after(curD)) {
									// task execute alarm
									tmpd.setHours(item.getExeBeginTime()
											.getHours());
									tmpd.setMinutes(item.getExeBeginTime()
											.getMinutes());
									tmpd.setSeconds(item.getExeBeginTime()
											.getSeconds());

									if (!tmpd.before(item.getStartDate())) {
										Date end = new Date();
										end.setYear(tmpd.getYear());
										end.setMonth(tmpd.getMonth());
										end.setDate(tmpd.getDate());
										end.setHours(item.getExeEndTime()
												.getHours());
										end.setMinutes(item.getExeEndTime()
												.getMinutes());
										end.setSeconds(item.getExeEndTime()
												.getSeconds());
										item.mExecEndDate = end.getTime();
										startAlarm(tmpd, TASK_START_TIME_TYPE,
												item, index);
										// task execute end alarm
										startAlarm(end, TASK_END_TIME_TYPE,
												item, index);
									} else {
										LOG
												.Log(
														LOG.LOG_I,
														item.getTestTaskCode()
																+ " ExecStartDate befor item start Date!!!");
									}
								} else {
									LOG.Log(LOG.LOG_I, item.getTestTaskCode()
											+ " ExecEndDate befor curDate!!!");
								}
							}
						} else if (item.getIterationType() == ITERATION_TYPE_WEEK) {
							LOG.Log(LOG.LOG_I, item.getTestTaskCode()
									+ " Exec Type ITERATION_TYPE_WEEK");
							// int DAYS = getIntervalDays(item.getStartDate(),
							// item.getEndDate());
							int day = item.getStartDate().getDate();
							int month = item.getStartDate().getMonth() + 1;
							int year = item.getStartDate().getYear() + 1900;
							int add = 0, pos = 0;
							int period = 7;
							for (int i = 0; i < DAYS; i++) {
								if (mTaskThreadkilled == true) {
									break;
								}
								Date tmpd = new Date(0);
								if (period > DAYS)
									return;
								if (day + i * period > getMonthTotalDays(year,
										month)) {
									i = 0;
									month++;
									if (month > 12) {
										i = 0;
										month = 1;
										year++;
									}
									tmpd.setDate(day);
									tmpd.setMonth(month - 1);
									tmpd.setYear(year - 1900);
								} else {
									tmpd.setDate(day + i * period);
									tmpd.setMonth(month - 1);
									tmpd.setYear(year - 1900);
								}
								if (add != 0)
									DAYS -= period;
								add++;
								tmpd.setHours(item.getExeEndTime().getHours());
								tmpd.setMinutes(item.getExeEndTime()
										.getMinutes());
								tmpd.setSeconds(item.getExeEndTime()
										.getSeconds());
								Date curD = new Date();
								Date endD = tmpd;
								if (endD.after(curD)) {
									// task execute alarm
									tmpd.setHours(item.getExeBeginTime()
											.getHours());
									tmpd.setMinutes(item.getExeBeginTime()
											.getMinutes());
									tmpd.setSeconds(item.getExeBeginTime()
											.getSeconds());

									if (!tmpd.before(item.getStartDate())) {
										Date end = new Date();
										end.setYear(tmpd.getYear());
										end.setMonth(tmpd.getMonth());
										end.setDate(tmpd.getDate());
										end.setHours(item.getExeEndTime()
												.getHours());
										end.setMinutes(item.getExeEndTime()
												.getMinutes());
										end.setSeconds(item.getExeEndTime()
												.getSeconds());
										startAlarm(tmpd, TASK_START_TIME_TYPE,
												item, index);
										// task execute end alarm

										startAlarm(end, TASK_END_TIME_TYPE,
												item, index);
									} else {
										LOG
												.Log(
														LOG.LOG_I,
														item.getTestTaskCode()
																+ " ExecStartDate befor item start Date!!!");
									}
								} else {
									LOG.Log(LOG.LOG_I, item.getTestTaskCode()
											+ " ExecEndDate befor curDate!!!");
								}
							}
						} else if (item.getIterationType() == ITERATION_TYPE_MONTH) {
							LOG.Log(LOG.LOG_I, item.getTestTaskCode()
									+ " Exec Type ITERATION_TYPE_MONTH");
							int day = item.getStartDate().getDate();
							int month = item.getStartDate().getMonth() + 1;
							int year = item.getStartDate().getYear() + 1900;
							int add = 0;
							for (int i = 0; i < DAYS; i++) {
								if (mTaskThreadkilled == true) {
									break;
								}
								Date tmpd = new Date(0);
								int period = getMonthTotalDays(year, month);
								if (period > DAYS)
									return;
								if (day + i * period > period) {
									i = 0;
									month++;
									if (month > 12) {
										i = 0;
										month = 1;
										year++;
									}
									tmpd.setDate(day);
									tmpd.setMonth(month - 1);
									tmpd.setYear(year - 1900);
								} else {
									tmpd.setDate(day + i * period);
									tmpd.setMonth(month - 1);
									tmpd.setYear(year - 1900);
								}
								DAYS -= period;
								add++;
								tmpd.setHours(item.getExeEndTime().getHours());
								tmpd.setMinutes(item.getExeEndTime()
										.getMinutes());
								tmpd.setSeconds(item.getExeEndTime()
										.getSeconds());
								Date curD = new Date();
								Date endD = tmpd;
								if (endD.after(curD)) {
									// task execute alarm
									tmpd.setHours(item.getExeBeginTime()
											.getHours());
									tmpd.setMinutes(item.getExeBeginTime()
											.getMinutes());
									tmpd.setSeconds(item.getExeBeginTime()
											.getSeconds());

									if (!tmpd.before(item.getStartDate())) {
										Date end = new Date();
										end.setYear(tmpd.getYear());
										end.setMonth(tmpd.getMonth());
										end.setDate(tmpd.getDate());
										end.setHours(item.getExeEndTime()
												.getHours());
										end.setMinutes(item.getExeEndTime()
												.getMinutes());
										end.setSeconds(item.getExeEndTime()
												.getSeconds());
										item.mExecEndDate = end.getTime();
										startAlarm(tmpd, TASK_START_TIME_TYPE,
												item, index);
										// task execute end alarm

										startAlarm(end, TASK_END_TIME_TYPE,
												item, index);
									} else {
										LOG
												.Log(
														LOG.LOG_I,
														item.getTestTaskCode()
																+ " ExecStartDate befor item start Date!!!");
									}
								} else {
									LOG.Log(LOG.LOG_I, item.getTestTaskCode()
											+ " ExecEndDate befor curDate!!!");
								}
							}
						}
					} else if (item.getExeType() == EXECUTE_TYPE_NUM) {
						LOG.Log(LOG.LOG_I, item.getTestTaskCode()
								+ " Exec Type EXECUTE_TYPE_NUM");
						int day = item.getStartDate().getDate();
						int month = item.getStartDate().getMonth() + 1;
						int year = item.getStartDate().getYear() + 1900;
						int add = 0, pos = 0;
						for (int i = 0; i <= DAYS; i++) {
							if (mTaskThreadkilled == true) {
								break;
							}
							Date tmpd = new Date();
							if (day + i > getMonthTotalDays(year, month)) {
								i = 0;
								day = 1;
								tmpd.setDate(day);
								month++;
								if (month > 12) {
									i = 0;
									month = 1;
									year++;
								}
								tmpd.setMonth(month - 1);
								tmpd.setYear(year - 1900);
								DAYS -= add;
								add = 0;
							} else {
								tmpd.setDate(day + i);
								tmpd.setMonth(month - 1);
								tmpd.setYear(year - 1900);
							}
							add++;
							pos++;
							tmpd.setHours(item.getExeEndTime().getHours());
							tmpd.setMinutes(item.getExeEndTime().getMinutes());
							tmpd.setSeconds(item.getExeEndTime().getSeconds());
							Date curD = new Date();
							Date endD = tmpd;
							if (endD.after(curD)) {
								// task execute alarm
								tmpd
										.setHours(item.getExeBeginTime()
												.getHours());
								tmpd.setMinutes(item.getExeBeginTime()
										.getMinutes());
								tmpd.setSeconds(item.getExeBeginTime()
										.getSeconds());

								if (!tmpd.before(item.getStartDate())) {
									startAlarm(tmpd, TASK_START_TIME_TYPE,
											item, index);
									// task execute end alarm
									tmpd.setHours(item.getExeEndTime()
											.getHours());
									tmpd.setMinutes(item.getExeEndTime()
											.getMinutes());
									tmpd.setSeconds(item.getExeEndTime()
											.getSeconds());

									startAlarm(tmpd, TASK_END_TIME_TYPE, item,
											index);
								} else {
									LOG
											.Log(
													LOG.LOG_I,
													item.getTestTaskCode()
															+ " ExecStartDate befor item start Date!!!");
								}
							} else {
								LOG.Log(LOG.LOG_I, item.getTestTaskCode()
										+ " ExecEndDate befor curDate!!!");
							}
						}
					}
					// task end alarm
					if (item.getEndDate().after(new Date())) {
						startAlarm(item.getEndDate(), TASK_END_TYPE, item,
								index);
					} else {
						LOG.Log(LOG.LOG_I, item.getTestTaskCode()
								+ " task endDate befor curDate!!!");
					}
				}
				index++;
			}
			isLoading = false;
		} catch (Exception e) {
			e.printStackTrace();
			LOG.Log(LOG.LOG_E, e.getMessage());
		}
	}

	/********************************************************************************
	 * 返回xx年xx月有多少天
	 * @param year
	 * @param month
	 * @return
	 ********************************************************************************/
	private int getMonthTotalDays(int year, int month) {
		int days = 0;
		boolean reY = false;
		int[] months = new int[] { 0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31,
				30, 31 };
		if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
			reY = true;
		} else {
			reY = false;
		}
		if (month > 0) {
			if (month == 2 && reY) {
				days = 29;
			} else {
				days = months[month];
			}
		}
		LOG.Log(LOG.LOG_I, year + "年 " + month + "月" + "总共有" + days + "天！");
		return days;
	}

	/********************************************************************************
	 * 返回起始时间内，共有多少天
	 * @param startday
	 * @param endday
	 * @return
	 ********************************************************************************/
	private int getIntervalDays(Date startday, Date endday) {
		if (startday.after(endday)) {
			return 0;
		}
		long sl = startday.getTime();
		long el = endday.getTime();
		long ei = el - sl;
		return (int) (ei / (1000 * 60 * 60 * 24));
	}

	private TaskInfo getTaskFromTaskCode(String code) {
		Iterator<TaskInfo> it = mRunningTaskList.iterator();
		while (it.hasNext()) {
			TaskInfo item = it.next();
			if (item.getTestTaskCode().equals(code)) {
				return item;
			}
		}
		return null;
	}

	/********************************************************************************
	 * 获取手机信息
	 * @return
	 ********************************************************************************/
	private String getPhoneInfo() {
		if (mTelem != null) {
			return mTelem.getSimSerialNumber() + "/" + mTelem.getDeviceId();
		} else {
			return null;
		}
	}

	/********************************************************************************
	 * 被插入到“task_table”中的任务会被记录为已开始执行
	 * @param task
	 ********************************************************************************/
	private void insertTasktoDB(TaskInfo task) {
		boolean flaginsert = true;
		Cursor cursor = mTaskDB.getTask();
		if (cursor.getCount() > 0) {
			while (!cursor.isAfterLast()) {
				String taskCode = cursor.getString(0);
				if (taskCode.equals(task.getTestTaskCode())) {
					flaginsert = false;
					break;
				}
				cursor.moveToNext();
			}
		}
		cursor.close();
		
		//如果当前数据库中存在此任务信息，则不再插入
		if (flaginsert) {
			mTaskDB.insertTaskTable(task.getTestTaskCode(), task
					.getTestTaskName(), task.getTestTaskDesc(), new Date()
					.getTime(), task.mExecEndDate, getPhoneInfo(), task
					.getTestTaskVersion(), 0, task.mTaskState);
		}
	}

	
	public void checkRequestCodeIsExist() {
		int index = 0;
		String list[] = mReceiverHandler.mTaskRecord.getString("TASKLIST")
				.split("\\|");
		Cursor cursor = mTaskDB.getRequestCode();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			boolean flagdelete = true;
			String taskCode = cursor.getString(0);
			for (index = 0; index < list.length; index++) {
				if (list[index].split("\\#")[0].equals(taskCode)) {
					flagdelete = false;
					break;
				}
			}
			if (flagdelete) {
				mTaskDB.deleteRequestCodeFromTaskCode(taskCode);
			}
			cursor.moveToNext();
		}
		cursor.close();
	}

	public void checkTaskIsExist() {
		int index = 0;
		String list[] = mReceiverHandler.mTaskRecord.getString("TASKLIST")
				.split("\\|");
		Cursor cursor = mTaskDB.getTask();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			boolean flagdelete = true;
			String taskCode = cursor.getString(0);
			String taskVer = cursor.getString(6);
			for (index = 0; index < list.length; index++) {
				if (list[index].split("\\#")[0].equals(taskCode)
						&& list[index].split("\\#")[1].equals(taskVer)) {
					flagdelete = false;
					break;
				}
			}
			if (flagdelete) {
				mTaskDB.deleteTaskRecords(taskCode);
			}
			cursor.moveToNext();
		}
		cursor.close();
	}

	/********************************************************************************
	 * 发送广播消息，广播消息是APK和底层jar通信的唯一通道
	 * USER_DFINED_TASK_ID是唯一消息体
	 * @author
	 *
	 ********************************************************************************/
	public class SendBroadCast {
		private Intent mIntent;

		public SendBroadCast() {
			mIntent = new Intent(USER_DFINED_TASK_ID);
		}

		public void putString(String key, String value) {
			mIntent.putExtra(key, value);
		}

		public void putBoolean(String key, boolean value) {
			mIntent.putExtra(key, value);
		}

		public void putInt(String key, int value) {
			mIntent.putExtra(key, value);
		}

		public void sendBroadcast(Context context) {
			context.sendBroadcast(mIntent);
		}
	}

	/********************************************************************************
	 * 广播接收器，是上层执行服务和底层执行程序的唯一通信，整个测试任务的定时执行，关闭，数据交互等，
	 * 均通过此接收器实现。是整个测试程序的核心。
	 * @author 
	 *
	 *******************************************************************************/
	public class TaskHandle extends BroadcastReceiver {
		//当前运行的任务
		private TaskInfo mRunningTask;
		//Case列表
		private List<CaseInfo> mCaeInfoList = new ArrayList<CaseInfo>();
		//数据库
		public TaskRecord mTaskRecord;
		//当前任务执行
		private int mExecResult = 0;
		//当前TestID的执行时延时间
		private long mExecTime = 0;
		//当前Case的执行类型，即需要写入报告的测试值（包括时延、速率、成功率）
		private String mCaseResultType = null;
		//底层上报的测试值
		private int mExecExtraData = -1;
		//底层上报的测试结果，成功 or 失败
		private int mExecResultValue = -1;
		//任务开始的时间，用来计算下载速率
		private long mStartItemMillis = 0;
		//任务开始时手机中记录的流量值，用于和结束时的流量值做差，计算下载速率
		private long mStartItemTotalRxBytes = 0;
		//下载速率
		private double mExecResultSpeed = 0;
		//脚本是否需要抓包标记
		private boolean mNetPacketCap = false;
		//循环执行列表
		private ArrayList<CaseItemLoopRecorder> mExeCaseItemLoop = new ArrayList<CaseItemLoopRecorder>();
		//case执行时，内部Test ID的index指向
		private int mCaseItemIndex = 0;
		//case在任务脚本中的index
		private int mCaseNameIndex = 0;
		
		private CaseInfo mPreCaseInfo = null;
		private CaseInfo mCurCaseInfo = null;
		private Context mCxt;
		private SendBroadCast mSendBroadcast;
		private int mTmpIndex = 1;
		
		//任务执行次数
		private int mExecNum = 0;
		//脚本及任务解析器
		private xmlParse mParse = new xmlParse();
		private List<CaseNum> mCaseNumList = new ArrayList<CaseNum>();
		private boolean flag = true;
		private String mPreKey;
		private List<Integer> mIndexList = new ArrayList<Integer>();
		private int mExecType = 0;
		private TaskInfo mPreTaskInfo = null;
		
		//任务队列，在执行时，依据队列顺序执行，当有高低优先级插入时，保证不会丢弃低优先级任务
		private List<TaskInfo> mPendingTaskQueue = new ArrayList<TaskInfo>();
		private boolean mJumpQ = false;
		private int mThreadExecState = THREAD_NONE;
		private boolean mchangedQ = false;
		/* msg key value */
		// ack msg
		//在case执行时，携带通信信息，包括任务握手，开始，结束，测试结果等
		private final static String TASK_SEND_ACK_KEY = "TASK_SEND_ACK";
		//在执行过程中，负责发送Case名称给底层执行程序
		private final static String TASK_SEND_CASE_FILE_NAME = "TASK_SEND_CASE_FILE_NAME";
		//握手信息，查询底层jar是否工作正常
		private final static String TASK_SEND_ACK_REQ_VALUE = "TASK_SEND_ACK_REQ";
		//握手确认信息
		private final static String TASK_SEND_ACK_RSP_VALUE = "TASK_SEND_ACK_RSP";
		//结束信息，如结束时间到，或case执行完毕，通知执行端
		private final static String TASK_SEND_END_REQ_VALUE = "TASK_SEND_END_REQ";
		// transmission
		//Case执行Item信息传送
		private final static String TASK_SEND_CASE_ITEM_VALUE = "TASK_SEND_CASE_ITEM_REQ";
		//发送Item的index给底层执行程序
		private final static String TEST_CASE_ITEM_DATA = "TEST_CASE_ITEM";
		//底层反馈Case的执行结果，成功失败和测试值
		private final static String TASK_SEND_CASE_ITEM_RSP_VALUE = "TASK_SEND_CASE_ITEM_RSP";
		//测试结果关键字
		private final static String TEST_CASE_ITEM_RET_VALUE = "TEST_CASE_ITEM_RET";
		//通知底层执行结果的条件，成功或失败
		private final static String TASK_CASE_RESULT_VALUE = "TASK_CASE_RESULT_REQ";
		// log处理
		private final static String TASK_SEND_LOG_VALUE = "TASK_SEND_LOG_REQ";
		private final static String TEST_TASK_NAME_DATA = "TEST_TASK_NAME";
		private final static String TEST_LOG_DATE_DATA = "TEST_LOG_DATE";
		private final static String TEST_CASE_ID_DATA = "TEST_CASE_ID";
		private final static String TSCREEN_WIDTH_DATA = "TSCREEN_WIDTH_DATA";
		private final static String TSCREEN_HEIGHT_DATA = "TSCREEN_HEIGHT_DATA";
		private final static String TSCREEN_COLOR_DATA = "TSCREEN_COLOR_DATA";
		private final static String CASE_EXEC_RESULT_DATA = "CASE_EXEC_RESULT_DATA";
		private final static String TEST_CASE_ITEM_TIME_DATA = "TEST_CASE_ITEM_TIME";
		private final static String TEST_TASK_PATH_DATA = "TEST_TASK_PATH_DATA";
		//底层返回成功率
		private final static String TEST_CASE_ITEM_EXTRA_DATA = "TEST_CASE_EXTRA_DATA";
		//返回Test ID的测试值
		private final static String TEST_CASE_ITEM_RESULT_VALUE = "TEST_CASE_RESULT_VALUE";
		//发送结束消息，底层执行程序需立即结束执行
		private static final int MSG_SEND_END = 0;
		//执行队列
		private static final int MSG_EXEC_Q_TASK = 1;
		
		private static final int THREAD_END_TASK = 0;
		private static final int THREAD_EXEC_TASK = 1;
		private static final int THREAD_NONE = 2;
		
		Handler mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_SEND_END:
					sendEnd();
					break;
				case MSG_EXEC_Q_TASK:
					try {
						startQueueTask();
						if (mPendingTaskQueue == null
								|| mPendingTaskQueue.size() <= 0) {
							return;
						}
						
						//如果在更新中停止执行
						boolean haveUpdate = DB.getTaskHaveUpdateState();
						boolean isUpdating = DB.getTaskUpdatingState();
						if (haveUpdate == true || isUpdating == true) {
							return;
						}

						mRunningTask = mPendingTaskQueue.get(0);
						if (mRunningTask != null) {
							mRunningTask.mTaskState = EXECUTE_TYPE_TIME;
							// remove task from Q
							mPendingTaskQueue.remove(0);
							LOG.Log(LOG.LOG_I,"**************QmRunningTask****************");
							execPrepare();
						}
					} catch (IndexOutOfBoundsException e) {
						LOG.Log(LOG.LOG_E, "startQueueTask exception");
						LOG.Log(LOG.LOG_E, e.getMessage());
					}
					break;
				default:
					break;
				}
				mHandler.removeMessages(msg.what);
				mThreadExecState = THREAD_NONE;
			}
		};

		class QThread extends Thread {
			@Override
			public void run() {
				while (!mIsCancel) {
					try {
						Message msg = new Message();
						switch (mThreadExecState) {
						case THREAD_END_TASK:
							msg.what = MSG_SEND_END;
							mHandler.sendMessage(msg);
							break;
						case THREAD_EXEC_TASK:
							msg.what = MSG_EXEC_Q_TASK;
							mHandler.sendMessage(msg);
							break;
						default:
							break;
						}
						Thread.sleep(80);
					} catch (Exception e) {
						LOG.Log(LOG.LOG_E, "QThread exception!");
						LOG.Log(LOG.LOG_E, e.getMessage());
					}
				}
			}
		}

		public TaskHandle(Context context) {
			mCxt = context;
			mSendBroadcast = new SendBroadCast();
			mTaskRecord = new TaskRecord(context);
			mTaskRecord.getSharedPreferences("TASK_INFO");
			new QThread().start();
		}

		private void sendAck() {
			setPreSendKey(TASK_SEND_ACK_REQ_VALUE);
			mSendBroadcast
					.putString(TASK_SEND_ACK_KEY, TASK_SEND_ACK_REQ_VALUE);
			mSendBroadcast.putBoolean(TSCREEN_COLOR_DATA, mTaskRecord
					.getBoolean("ISRGB"));
			int with = mTaskRecord.getInt("SCREEN_WIDTH");
			int height = mTaskRecord.getInt("SCREEN_HEIGHT");
			if (with == 0) {
				with = 320;
			}
			if (height == 0) {
				height = 480;
			}
			mSendBroadcast.putInt(TSCREEN_WIDTH_DATA, with);
			mSendBroadcast.putInt(TSCREEN_HEIGHT_DATA, height);
			mSendBroadcast.putString(TEST_TASK_PATH_DATA, mRunningTask
					.getTaskPath());
			LOG.Log(LOG.LOG_I, "send ack");
			LOG.Log(LOG.LOG_I, "send ack value :" + TASK_SEND_ACK_REQ_VALUE);
			mSendBroadcast.sendBroadcast(mCxt);

			mDevBusyStartTime = new Date();
			DB.SetDevStateDBValue(mCxt, DEV_STATE_BUSY);
		}

		private void sendEnd() {
			LOG.Log(LOG.LOG_I, "send end");
			LOG.Log(LOG.LOG_I, "send end value :" + TASK_SEND_END_REQ_VALUE);
			setPreSendKey(TASK_SEND_END_REQ_VALUE);
			Intent it = new Intent(USER_DFINED_TASK_ID);
			it.putExtra(TASK_SEND_ACK_KEY, TASK_SEND_END_REQ_VALUE);
			mCxt.sendBroadcast(it);

			//同步设备状态
			DB.SetDevStateDBValue(mCxt, DEV_STATE_IDLE);
			mDevBusyEndTime = new Date();
			// output task report
			// writeTaskReport(mRunningTask.getTestTaskCode());
			startUploadRport(mRunningTaskList);
			if (DB.getTaskHaveUpdateState() == true) {
				if (IsServiceRunning("DataUpdateService")) {
					Intent upintent = new Intent(UPDATE_TASK_ID);
					mCxt.sendBroadcast(upintent);
					LOG.Log(LOG.LOG_I, "TaskService broadcast update");
				} else {
					LOG.Log(LOG.LOG_I, "DataUpdateService is not running");
				}
			}

		}

		//重置执行变量
		private void resetVar() {
			mTmpIndex = 1;
			mExecNum = 0;
			mCaseItemIndex = 0;
			mCaseNameIndex = 0;
		}

		private void setPreSendKey(String key) {
			mPreKey = key;
		}

		private String getPreSendKey() {
			return mPreKey;
		}

		private String getRspKeyFromSendKey(String key) {
			if (key.equals(TASK_SEND_ACK_REQ_VALUE)) {
				return "TASK_SEND_ACK_RSP";
			} else if (key.equals(TASK_SEND_END_REQ_VALUE)) {
				return "TASK_SEND_END_RSP";
			} else if (key.equals(TASK_CASE_RESULT_VALUE)) {
				return "TASK_CASE_RESULT_RSP";
			} else if (key.equals(TASK_SEND_LOG_VALUE)) {
				return "TASK_SEND_LOG_RSP";
			} else if (key.equals(TASK_SEND_CASE_ITEM_VALUE)) {
				return "TASK_SEND_CASE_ITEM_RSP";
			}
			return null;
		}

		/********************************************************************************
		 * 从队列顶端获取任务，并开始执行
		 ********************************************************************************/
		private void startQueueTask() {
			int qindex = 0;
			int startpos = 0;
			int endpos = 0;
			boolean flag = true;
			if (!mchangedQ || mPendingTaskQueue.size() == 0) {
				return;
			}
			List<Integer> posL = new ArrayList<Integer>();
			Collections.sort(mPendingTaskQueue, new PendingQComparePriority());

			if (mPendingTaskQueue.size() > 2) {
				while (qindex < mPendingTaskQueue.size()) {
					TaskInfo cur = mPendingTaskQueue.get(qindex);
					TaskInfo next = mPendingTaskQueue.get(qindex + 1);
					if (cur != null && next != null
							&& cur.getPriority() != next.getPriority()) {
						endpos = qindex;
						posL.add(endpos);
						flag = false;
					}
					LOG.Log(LOG.LOG_I, "mPendingTaskQueue[ " + qindex + " ] = "
							+ cur.getTestTaskCode());
					qindex++;
				}
				// 存在相同优先级的task
				if (!flag) {
					int index = 0;
					while (index < mPendingTaskQueue.size()) {
						endpos = posL.get(index);
						List<TaskInfo> tmpL = mPendingTaskQueue.subList(
								startpos, endpos);
						Collections.sort(tmpL, new PendingQCompareDate());
						int subindex = startpos;
						while (subindex < endpos) {
							mPendingTaskQueue.set(subindex, tmpL.get(subindex));
							subindex++;
						}
						startpos = endpos;
						index++;
					}
				}
			}
			// print mPendingTaskQueue elements
			qindex = 0;
			while (qindex < mPendingTaskQueue.size()) {
				TaskInfo cur = mPendingTaskQueue.get(qindex);
				LOG.Log(LOG.LOG_I, "sort mPendingTaskQueue[ " + qindex
						+ " ] = " + cur.getTestTaskCode());
				qindex++;
			}
		}

		/********************************************************************************
		 * 优先级高的任务打断时，会将本次执行的任务加入队列。
		 * @param task 被压入栈的任务
		 ********************************************************************************/
		private void pushIntoQ(TaskInfo task) {
			if (task != null) {
				LOG.Log(LOG.LOG_I, "Task " + task.getTestTaskCode()
						+ " push into a queue !");
				mPendingTaskQueue.add(task);
				mchangedQ = true;
			}
		}

		/********************************************************************************
		 * 执行任务
		 ********************************************************************************/
		private void execPrepare() {
			boolean taskExist = false;
			// 初始化变量
			resetVar();
			mPreTaskInfo = mRunningTask;
			int num = 0;
			// 查询数据库，获取task的结束时间和上次测试的次数
			Cursor cursor = mTaskDB.getTaskFromTaskCode(mPreTaskInfo
					.getTestTaskCode());
			if (cursor.getCount() > 0) {
				num = cursor.getInt(1);
				LOG.Log(LOG.LOG_I, "To be continued num: " + num);
				if (mPreTaskInfo.getExeType() == EXECUTE_TYPE_TIME) {
					Date d = new Date(cursor.getLong(0));
					Date curD = new Date();
					LOG.Log(LOG.LOG_I, "task table end Date: "
							+ d.toLocaleString());
					LOG
							.Log(LOG.LOG_I, "current Date: "
									+ curD.toLocaleString());
					// 如果任务的结束时间在当前时间之后，续测或者从0开始
					if (d.after(curD)) {
						mExecNum = num;
						// 如果数据表中的num和task的num相等，取消任务或者续测
						if (mPreTaskInfo.getIterationNum() <= num) {
							LOG.Log(LOG.LOG_I, "Task "
									+ mPreTaskInfo.getTestTaskCode()
									+ " had been finished!!!");
							return;
						}
					} else {
						mExecNum = 0;
						// update start time
						mTaskDB.updateTaskStartDateTable(mPreTaskInfo
										.getTestTaskCode(),
										mPreTaskInfo.mExecStartDate);
						// update end time
						mTaskDB.updateTaskEndDateTable(mPreTaskInfo
								.getTestTaskCode(), mPreTaskInfo.mExecEndDate);
					}
				} else {
					// 如果数据表中的num和task的num相等，取消任务反之续测
					if (mPreTaskInfo.getIterationNum() <= num) {
						LOG.Log(LOG.LOG_I, "Task "
								+ mPreTaskInfo.getTestTaskCode()
								+ " had been finished!!!");
						return;
					} else {
						mExecNum = num;
					}
				}
				taskExist = true;
			} else {
				// 插入新数据
				insertTasktoDB(mPreTaskInfo);
			}
			cursor.close();
			LOG.Log(LOG.LOG_I, "Task " + mPreTaskInfo.getTestTaskCode()
					+ "is starting form " + mExecNum);
			mCaseNumList.clear();
			List<String> casefilelist = new ArrayList<String>();
			for (int ii = 0; ii < mPreTaskInfo.getKey().size(); ++ii) {
				casefilelist.add(mPreTaskInfo.getKey().get(ii).getScriptName());
			}
			Iterator<String> it = casefilelist.iterator();

			while (it.hasNext()) {
				String casename = it.next();
				if (casename != null) {
					CaseNum casenum = new CaseNum();
					// 如果task存在，更新case的续测信息
					if (taskExist) {
						Cursor caseCursor = mTaskDB.getCaseFromTaskCode(
								mPreTaskInfo.getTestTaskCode(), casename);
						if (caseCursor != null && caseCursor.getCount() > 0) {
							casenum.mTotalNum = caseCursor.getInt(0);
							casenum.mErrorNum = caseCursor.getInt(1);
							casenum.mSuessceNum = caseCursor.getInt(2);
							casenum.mCaseName = casename;
						}
						caseCursor.close();
					} else {
						casenum.mCaseName = casename;
						casenum.mErrorNum = 0;
						casenum.mSuessceNum = 0;
						casenum.mTotalNum = 0;
						// 不存在插入数据表
						mTaskDB.insertCaseTable(mPreTaskInfo.getTestTaskCode(),
								casenum.mCaseName, casenum.mTotalNum,
								casenum.mSuessceNum, casenum.mErrorNum);
					}
					if (casenum != null) {
						mCaseNumList.add(casenum);
					}
				}
			}
			String Needpack = mPreTaskInfo.getNetPacketCap();
			if (Needpack != null && Needpack.equals("1")) {
				mNetPacketCap = true;
			} else {
				mNetPacketCap = false;
			}

			mIndexList.clear();
			mExeCaseItemLoop.clear();
			mPreTaskInfo.mTaskState = TASK_RUNNING;
			sendAck();
		}

		/********************************************************************************
		 * 接收并分发通信消息
		 ********************************************************************************/
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				Bundle bundle = intent.getExtras();
				LOG
						.Log(LOG.LOG_I, "onReceive broadcast :"
								+ intent.getAction());
				
				//闹钟消息，整个任务启动的原始触发点
				if (intent.getAction().equals(ALARM_BROADCAST_ID)) {
					// TODO alarm broadcast
					if (bundle != null) {
						mExecType = bundle.getInt("DATE_TYPE");
						LOG.Log(LOG.LOG_I, "current mExecType : " + mExecType);

						mRunningTask = bundle.getParcelable("RUNNING_TASK");

						if (mRunningTask == null) {
							LOG.Log(LOG.LOG_I, "mRunningTask == null");
							return;
						}

						//如在脚本中存在应用程序校验关键字，校验失败则停止执行并发送告警
						if (CheckIfAppExistInSystem(mRunningTask
								.getTestTaskName()) == false) {
							LOG.Log(LOG.LOG_I, "Application not present");
							Toast.makeText(context, "测试应用程序不存在",
									Toast.LENGTH_LONG).show();
							SendWanningMsgToserver();
							return;
						}

						//如在脚本中存在应用程序自动启动关键字，则先启动程序
//						LaunchAppAccordName(mRunningTask.getTestTaskName());

						LOG.Log(LOG.LOG_I, "mRunningTask pass");
						if (mRunningTask == null) {
							LOG.Log(LOG.LOG_I, "mRunningTask == null 1111");
							return;
						}
						// end alarm
						if (mExecType == TASK_END_TIME_TYPE
								|| mExecType == TASK_END_TYPE) {
							LOG.Log(LOG.LOG_I, "receive "
									+ mRunningTask.getTestTaskCode()
									+ " end alarm " + "Code: "
									+ mRunningTask.mRequestCode);
							mRunningTask.mTaskState = TASK_FINISHED;
							// 更新任务结束的小时间
							if (mExecType == TASK_END_TIME_TYPE) {
								mTaskDB.updateTaskEndDateTable(mRunningTask
										.getTestTaskCode(), new Date()
										.getTime());
							}
							sendEnd();
							// TODO close activity application
							return;
						}
						if (mPreTaskInfo != null
								&& mPreTaskInfo.mTaskState == TASK_RUNNING) {
							// 检查新来task的优先级,不大于当前进Q
							if (mRunningTask.getPriority() <= mPreTaskInfo
									.getPriority()) {
								LOG
										.Log(LOG.LOG_I,
												"###################mRunningTask######################");
								pushIntoQ(mRunningTask);
								return;
							} else {
								// 当前task 进Q
								LOG
										.Log(LOG.LOG_I,
												"======================mPreTaskInfo======================");
								mJumpQ = true;
								pushIntoQ(mPreTaskInfo);
								// 高于当前处理，停止当前任务，开始新来的任务
								mThreadExecState = THREAD_END_TASK;
								return;
							}
						}
						execPrepare();
					}
				} else if (intent.getAction().equals(USER_DFINED_EXEC_ID)) {
					// TODO user-defined broadcast
					String value = bundle.getString(TASK_SEND_ACK_KEY);
					LOG.Log(LOG.LOG_I, "get rsp vaule: " + value);
					if (!getRspKeyFromSendKey(getPreSendKey()).equals(value)) {
						LOG.Log(LOG.LOG_I,
										"ERROREROREROREROREROREROREROREROREROREROREROREROR");
						return;
					}
					/* ack rsp */

					if (value.equals(TASK_SEND_ACK_RSP_VALUE)) {
						if (!intent.getBooleanExtra("EXEC_RUNNING_DATA", false)) {
							parseCaseInfo();
						}
						return;
					}
					/* exec rsp */
					if (value.equals(TASK_SEND_CASE_ITEM_RSP_VALUE)) {
						// result
						mExecResult = bundle.getInt(TEST_CASE_ITEM_RET_VALUE);
						// time
						mExecTime = bundle.getLong(TEST_CASE_ITEM_TIME_DATA);

						LOG.Log(LOG.LOG_I, "000 mExecTime = " + mExecTime);
						mExecExtraData = -1;

						mExecExtraData = bundle
								.getInt(TEST_CASE_ITEM_EXTRA_DATA);

						LOG.Log(LOG.LOG_D, "get case item res extra :"
								+ mExecExtraData);

						mExecResultValue = -1;

						mExecResultValue = bundle
								.getInt(TEST_CASE_ITEM_RESULT_VALUE);

						LOG.Log(LOG.LOG_D, "get case item res value :"
								+ mExecResultValue);

						if (mExecResult == 0) {
							String valuetype = mCurCaseInfo.getTestValueType();
							
							//校验返回值类型，时延、成功率或者下载速率
							if (mCurCaseInfo.getExecuteTime()
									&& valuetype != null
									&& valuetype.equals(TESTVALUE_TIME)) {
								mTaskDB.insertTimeTable(mPreTaskInfo
										.getTestTaskCode(), mCaseNumList
										.get(mCaseNameIndex).mCaseName,
										mCaseItemIndex, mExecTime);
								LOG.Log(LOG.LOG_I, "mExecTime = " + mExecTime);
							} else if (valuetype != null
									&& valuetype.equals(TESTVALUE_SPEED)) {
								double currentMillis = (double) (SystemClock
										.uptimeMillis() - mStartItemMillis) / 1000;
								long currentRxBytes = TrafficStats
										.getTotalRxBytes()
										- mStartItemTotalRxBytes;
								if (currentMillis <= 0 || currentRxBytes <= 0) {
									mExecResultSpeed = 0;
								} else {
									mExecResultSpeed = (double) (currentRxBytes / currentMillis);
								}
								mTaskDB.insertSpeedTable(mPreTaskInfo
										.getTestTaskCode(), mCaseNumList
										.get(mCaseNameIndex).mCaseName,
										mCaseItemIndex, mExecResultSpeed);
							} else if (valuetype != null
									&& valuetype.equals(TESTVALUE_RATE)) {
								if (mExecResultValue == 0 || mExecResultValue == 1) {
									LOG.Log(LOG.LOG_I, "mExecResultValue = "
											+ mExecResultValue);
									LOG.Log(LOG.LOG_I,
											"mPreTaskInfo.getTestTaskCode() = "
													+ mPreTaskInfo
															.getTestTaskCode());
									LOG
											.Log(
													LOG.LOG_I,
													"mCaseNumList.get(mCaseNameIndex).mCaseName = "
															+ mCaseNumList
																	.get(mCaseNameIndex).mCaseName);
									LOG.Log(LOG.LOG_I,
											"mCaseNumList.get(mExecResultValue = "
													+ mExecResultValue);
									mTaskDB.insertRateTable(mPreTaskInfo
											.getTestTaskCode(), mCaseNumList
											.get(mCaseNameIndex).mCaseName,
											mCaseItemIndex, mExecResultValue);
								}
							}
							boolean executenext = true;
							if (mPreCaseInfo != null && mExecExtraData != -1) {
								String successgoto = mPreCaseInfo
										.getTestSuccessGotoID();

								if (successgoto != null
										&& successgoto.length() > 0) {
									String successitem = GetIndexStringFromeGoto(
											mExecExtraData, successgoto);
									if (successitem != null
											&& successitem.length() > 0) {
										int getindex = getGotocaseitemId(successitem);
										if (getindex != -1) {
											mCaseItemIndex = getindex;
											mPreCaseInfo = mCaeInfoList
													.get(mCaseItemIndex);
											if (mPreCaseInfo != null) {
												executenext = false;
											}
										}

									}
								}

							}
							if (executenext == true) {
								nextCaseItem();
							}

						} else {
							String valuetype = mCurCaseInfo.getTestValueType();
							if (mExecResult != 0 && mPreCaseInfo != null) {
								if (mPreCaseInfo.getTestFailedGotoID() != null
										&& mPreCaseInfo.getTestFailedGotoID()
												.length() > 0) {
									if (mExecExtraData != -1) {
										String faliedGotoID = mPreCaseInfo
												.getTestFailedGotoID();
										String faileditem = GetIndexStringFromeGoto(
												mExecExtraData, faliedGotoID);
										if (faileditem != null
												&& faileditem.length() > 0) {
											int getindex = getGotocaseitemId(faileditem);
											if (getindex != -1) {
												mCaseItemIndex = getindex;
												mPreCaseInfo = mCaeInfoList
														.get(mCaseItemIndex);
											}
										}
									} else {
										mCaseItemIndex = getGotocaseitemId(mPreCaseInfo
												.getTestFailedGotoID());
										mPreCaseInfo = mCaeInfoList
												.get(mCaseItemIndex);
									}

								} else if (ExecLoopItem() == false) {
									if (valuetype != null
											&& valuetype
													.equals(TESTVALUE_SPEED)) {
										double currentMillis = (double) (SystemClock
												.uptimeMillis() - mStartItemMillis) / 1000;
										long currentRxBytes = TrafficStats
												.getTotalRxBytes()
												- mStartItemTotalRxBytes;
										if (currentMillis <= 0
												|| currentRxBytes <= 0) {
											mExecResultSpeed = 0;
										} else {
											mExecResultSpeed = (double) (currentRxBytes / currentMillis);
										}
										mTaskDB
												.insertSpeedTable(
														mPreTaskInfo
																.getTestTaskCode(),
														mCaseNumList
																.get(mCaseNameIndex).mCaseName,
														mCaseItemIndex,
														mExecResultSpeed);
									}
									// error num
									mCaseNumList.get(mCaseNameIndex).mErrorNum++;
									// 更新case table mErrorNum数据
									mTaskDB
											.updateCaseTableErrorNumber(
													mPreTaskInfo
															.getTestTaskCode(),
													mCaseNumList
															.get(mCaseNameIndex).mCaseName,
													mCaseNumList
															.get(mCaseNameIndex).mErrorNum);
									// TODO save item的errorCode和item的index
									mTaskDB
											.insertItemTable(
													mPreTaskInfo
															.getTestTaskCode(),
													mCaseNumList
															.get(mCaseNameIndex).mCaseName,
													mCaseItemIndex,
													mCaseNumList
															.get(mCaseNameIndex).mErrorNum,
													mExecResult);
									// TODO current case exec failed
									// send result to exec
									LOG.Log(LOG.LOG_I, "send case exec result");
									LOG.Log(LOG.LOG_I,
											"send case exec result F :"
													+ TASK_CASE_RESULT_VALUE);
									setPreSendKey(TASK_CASE_RESULT_VALUE);
									mSendBroadcast.putString(TASK_SEND_ACK_KEY,
											TASK_CASE_RESULT_VALUE);
									mSendBroadcast.putBoolean(
											CASE_EXEC_RESULT_DATA, false);
									mSendBroadcast.sendBroadcast(mCxt);
									mCaseItemIndex = 0;
									// nextCase();
									return;
								}
							}
							//即使执行成功，依然记录下载速率
							if (valuetype != null
									&& valuetype.equals(TESTVALUE_SPEED)) {
								double currentMillis = (double) (SystemClock
										.uptimeMillis() - mStartItemMillis) / 1000;
								long currentRxBytes = TrafficStats
										.getTotalRxBytes()
										- mStartItemTotalRxBytes;
								if (currentMillis <= 0 || currentRxBytes <= 0) {
									mExecResultSpeed = 0;
								} else {
									mExecResultSpeed = (double) (currentRxBytes / currentMillis);
								}
								mTaskDB.insertSpeedTable(mPreTaskInfo
										.getTestTaskCode(), mCaseNumList
										.get(mCaseNameIndex).mCaseName,
										mCaseItemIndex, mExecResultSpeed);
							}
						}

						parseCaseInfo();
						return;
					}
					if (value.equals("TASK_SEND_END_RSP")) {
						mPreTaskInfo.mTaskState = TASK_FINISHED;
						setPreSendKey("END");
						if (mExecType == TASK_START_TIME_TYPE) {
							if (mPreTaskInfo.getExeType() == EXECUTE_TYPE_NUM) {
								if (mPreTaskInfo.getIterationNum() == mExecNum) {
									LOG.Log(LOG.LOG_I, "Task "
											+ mPreTaskInfo.getTestTaskCode()
											+ " finished and cancel alarm!");
									Cursor cursor = mTaskDB
											.getRequestCodeFromTaskCode(mPreTaskInfo
													.getTestTaskCode());
									if (cursor.getCount() > 0) {
										while (!cursor.isAfterLast()) {
											int code = cursor.getInt(1);
											StopAlarm(code);
											LOG.Log(LOG.LOG_I, "Cancel alarm :"
													+ code);
											mReqCodelist.remove((Integer) code);
											cursor.moveToNext();
										}
									}
									cursor.close();
								} else {
									LOG.Log(LOG.LOG_I, "Task "
											+ mPreTaskInfo.getTestTaskCode()
											+ " end,cancel alarm " + "Code: "
											+ (mPreTaskInfo.mRequestCode + 1));
									LOG.Log(LOG.LOG_I, "Task "
											+ mPreTaskInfo.getTestTaskCode()
											+ " To be continued...");
									mReqCodelist
											.remove((Integer) mPreTaskInfo.mRequestCode);
									mReqCodelist
											.remove((Integer) (mPreTaskInfo.mRequestCode + 1));
									StopAlarm(mPreTaskInfo.mRequestCode + 1);
								}
							} else {
								if (mExecType == TASK_START_TIME_TYPE) {
									LOG.Log(LOG.LOG_I, "Task "
											+ mPreTaskInfo.getTestTaskCode()
											+ " end,cancel alarm " + "Code: "
											+ (mPreTaskInfo.mRequestCode + 1));
									StopAlarm(mPreTaskInfo.mRequestCode + 1);
									mReqCodelist
											.remove((Integer) mPreTaskInfo.mRequestCode);
									mReqCodelist
											.remove((Integer) (mPreTaskInfo.mRequestCode + 1));
								}
							}
						} else {
							LOG.Log(LOG.LOG_I, "Task "
									+ mPreTaskInfo.getTestTaskCode()
									+ "end alarm coming...");
						}
						mIndexList.clear();
						mExeCaseItemLoop.clear();
						LOG.Log(LOG.LOG_I, "Task report create completed...");
						resetVar();
						closeApp();
						if (mJumpQ) {
							// 插队执行
							LOG.Log(LOG.LOG_I, "Task "
									+ mPreTaskInfo.getTestTaskCode()
									+ " jump a queue !");
							mJumpQ = false;
							execPrepare();
						} else {
							if (mPendingTaskQueue.size() > 0) {
								mThreadExecState = THREAD_EXEC_TASK;
							}
						}
						return;
					}
					if (value.equals("TASK_SEND_LOG_RSP")) {
						parseCaseInfo();
						return;
					}
					if (value.equals("TASK_CASE_RESULT_RSP")) {
						if (mExecNum >= mPreTaskInfo.getIterationNum()) {
							LOG.Log(LOG.LOG_I,
									"task times finsihed...............");
							sendEnd();
							return;
						}
						if (mCasePacket != null
								&& mCasePacket.isInterrupted() == false) {
							mCasePacket.interrupt();
						}
						LOG.Log(LOG.LOG_I, "writeTaskReport");
						
						//写日志
						writeTaskReport(mRunningTask.getTestTaskCode());
						mCaseItemIndex = 0;
						nextCase();
						parseCaseInfo();
						return;
					}
				} else if (intent.getAction().equals(
						PACKAGE_UPLOAD_BROADCAST_ID)) {
					LOG.Log(LOG.LOG_I, "Pakcage upload time arriving ");
					startUploadRport(mRunningTaskList);
				} else if (intent.getAction().equals(REFRESH_RUNNING_TASK_LIST)) {
					LOG.Log(LOG.LOG_I, "refresh running task");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private int getGotocaseitemId(String gotoId) {
			if (gotoId.length() > 0) {
				int index = 0;
				Iterator<CaseInfo> it = mCaeInfoList.iterator();
				while (it.hasNext()) {
					CaseInfo info = it.next();
					if (gotoId.equals(info.getTestID())) {
						return index;
					}
					index++;
				}
			}
			return -1;
		}

		/********************************************************************************
		 * 下一个case开始之前，初始化所有变量
		 ********************************************************************************/
		private void nextCase() {
			mExecResult = 0;
			mIndexList.clear();
			mExeCaseItemLoop.clear();
			mCaseNameIndex++;
		}

		private void nextCaseItem() {
			if (ExecLoopItem() == true) {
				return;
			}
			mCaseItemIndex++;
		}

		private boolean ExecLoopItem() {
			String looper = mPreCaseInfo.getTestLooper();
			if (looper != null && looper.length() > 0) {
				CaseItemLoopRecorder looprecord = new CaseItemLoopRecorder();
				looprecord.mItemIndex = mCaseItemIndex;
				int loopindex = FinditemInLoopRecord(looprecord);
				if (loopindex == -1) {
					String loopto = GetIndexStringFromeLoop(0, looper);
					String loopNum = GetIndexStringFromeLoop(1, looper);
					if (loopto != null && loopNum != null
							&& loopto.length() > 0 && loopNum.length() > 0) {
						looprecord.mLoopto = loopto;
						looprecord.mLoopTotalNum = Integer.parseInt(loopNum);
						looprecord.mLoopTimes = 0;
						AddLoopItemToRecord(looprecord);
						String looptoitem = FindLooptoFromRecord(0);
						if (looptoitem != null && looptoitem.length() > 0) {
							int getindex = getGotocaseitemId(looptoitem);
							mCaseItemIndex = getindex;
							mPreCaseInfo = mCaeInfoList.get(mCaseItemIndex);
							return true;
						}
					}
				} else {
					String looptoitem = FindLooptoFromRecord(loopindex);
					if (looptoitem != null && looptoitem.length() > 0) {
						int getindex = getGotocaseitemId(looptoitem);
						mCaseItemIndex = getindex;
						mPreCaseInfo = mCaeInfoList.get(mCaseItemIndex);
						return true;
					}
				}
			}
			return false;
		}

		private boolean checkCaseIndexExist(int index) {
			Iterator<Integer> it = mIndexList.iterator();
			while (it.hasNext()) {
				int code_index = it.next();
				if (code_index == index) {
					return true;
				}
			}
			return false;
		}

		private boolean execCase() {

			LOG.Log(LOG.LOG_I, "execCase...");
			for (int itm = mCaseItemIndex; itm < mCaeInfoList.size(); itm++) {
				mCurCaseInfo = mCaeInfoList.get(mCaseItemIndex);
				if (mCurCaseInfo.getExecuteTime()
						&& !checkCaseIndexExist(mCaseItemIndex)) {
					mTaskDB.insertTimeIndexTable(
							mPreTaskInfo.getTestTaskCode(), mCaseNumList
									.get(mCaseNameIndex).mCaseName,
							mCaseItemIndex);
					mIndexList.add(mCaseItemIndex);
				}
				if (mCurCaseInfo != null) {
					mPreCaseInfo = mCurCaseInfo;
					LOG.Log(LOG.LOG_I, "send item exec result");
					LOG.Log(LOG.LOG_I, "send item exec result :"
							+ TASK_SEND_CASE_ITEM_VALUE);
					setPreSendKey(TASK_SEND_CASE_ITEM_VALUE);
					mSendBroadcast.putString(TASK_SEND_ACK_KEY,
							TASK_SEND_CASE_ITEM_VALUE);
					mSendBroadcast.putInt(TEST_CASE_ITEM_DATA, mCaseItemIndex);
					mSendBroadcast.sendBroadcast(mCxt);
					String valuetype = mCurCaseInfo.getTestValueType();
					if (valuetype != null && valuetype.equals(TESTVALUE_SPEED)) {
						mStartItemMillis = SystemClock.uptimeMillis();
						mStartItemTotalRxBytes = TrafficStats.getTotalRxBytes();
					} else {
						mStartItemMillis = 0;
						mStartItemTotalRxBytes = 0;
					}
					return true;
				}
			}
			mCaseNumList.get(mCaseNameIndex).mSuessceNum++;
			// 更新case table mSuessceNum数据
			mTaskDB.updateCaseTableSuessceNumber(
					mPreTaskInfo.getTestTaskCode(), mCaseNumList
							.get(mCaseNameIndex).mCaseName, mCaseNumList
							.get(mCaseNameIndex).mSuessceNum);
			// send result to exec
			LOG.Log(LOG.LOG_I, "send case exec result");
			LOG.Log(LOG.LOG_I, "send case exec result T :"
					+ TASK_CASE_RESULT_VALUE);
			setPreSendKey(TASK_CASE_RESULT_VALUE);
			mSendBroadcast.putString(TASK_SEND_ACK_KEY, TASK_CASE_RESULT_VALUE);
			mSendBroadcast.putBoolean(CASE_EXEC_RESULT_DATA, true);
			mSendBroadcast.sendBroadcast(mCxt);
			return true;
		}

		/********************************************************************************
		 * 解析case
		 ********************************************************************************/
		private void parseCaseInfo() {
			if (mPreTaskInfo.mTaskState != TASK_RUNNING) {
				return;
			}
			// task loop times
			for (int i = mExecNum; i < mPreTaskInfo.getIterationNum(); i++) {
				List<String> casefilelist = new ArrayList<String>();

				for (int ii = 0; ii < mPreTaskInfo.getKey().size(); ++ii) {
					casefilelist.add(mPreTaskInfo.getKey().get(ii)
							.getScriptName());
				}
				// List<String> casefilelist =
				// mRunningTask.getKey().get(mCaseNameIndex).getScriptName();
				// case file loop
				for (int f = mCaseNameIndex; f < casefilelist.size(); f++) {
					// case file name
					String casename = mPreTaskInfo.getTaskPath() + "/"
							+ casefilelist.get(f) + "/" + casefilelist.get(f)
							+ ".xml";
					LOG.Log(LOG.LOG_I, "case name = " + casename);
					InputStream caseinputstream = null;
					// parse case
					File casefile = new File(casename);
					try {
						caseinputstream = new BufferedInputStream(
								new FileInputStream(casefile));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						LOG.Log(LOG.LOG_E, "parseCaseInfo Exception");
						LOG.Log(LOG.LOG_E, e.getMessage());
					}
					try {
						// send log header msg
						if (mCaseNameIndex != mTmpIndex) {
							LOG.Log(LOG.LOG_I, "send log");
							LOG.Log(LOG.LOG_I, "send log key :"+ TASK_SEND_LOG_VALUE);
							//抓包
							if (mNetPacketCap == true) {
								if (mCasePacket == null) {
									mCasePacket = new ThreadPacket();
								} else if (mCasePacket.isInterrupted() == false) {
									mCasePacket.interrupt();
								}
								mCasePacket.taskName = mPreTaskInfo.getTestTaskName();
								mCasePacket.caseId = mCaseNumList.get(mCaseNameIndex).mCaseName;
								mCasePacket.start();
							} else {
								if (mCasePacket != null	&& mCasePacket.isInterrupted() == false) {
									mCasePacket.interrupt();
								}
							}
							setPreSendKey(TASK_SEND_LOG_VALUE);
							mSendBroadcast.putString(TASK_SEND_ACK_KEY,
									TASK_SEND_LOG_VALUE);
							mSendBroadcast.putString(TEST_TASK_NAME_DATA,
									mRunningTask.getTestTaskName());
							mSendBroadcast.putString(TEST_CASE_ID_DATA,
									casefilelist.get(f));
							mSendBroadcast.putString(TEST_LOG_DATE_DATA,
									new Date().toLocaleString());
							mSendBroadcast.putString(TASK_SEND_CASE_FILE_NAME,casename);
							mSendBroadcast.sendBroadcast(mCxt);
							mCaseNumList.get(mCaseNameIndex).mTotalNum++;
							// 更新case table mTotalNum数据
							mTaskDB.updateCaseTableTotalNumber(mPreTaskInfo
									.getTestTaskCode(), mCaseNumList
									.get(mCaseNameIndex).mCaseName,
									mCaseNumList.get(mCaseNameIndex).mTotalNum);
							mTmpIndex = mCaseNameIndex;
							return;
						}
						mCaeInfoList.clear();
						mCaeInfoList = mParse.caseParse(caseinputstream);
						try {
							caseinputstream = new BufferedInputStream(
									new FileInputStream(casefile));
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							LOG.Log(LOG.LOG_E, "parseCaseInfo Exception");
							LOG.Log(LOG.LOG_E, e.getMessage());
						}
						mCaseResultType = mParse
								.caseResultValue(caseinputstream);

						if (mCaeInfoList.size() > 0) {
							// execute case
							if (execCase())
								return;
						}
					} catch (Exception e) {
						LOG.Log(LOG.LOG_E, "caseParse Exception");
						LOG.Log(LOG.LOG_E, e.getMessage());
					}
				}
				mExecNum++;
				// 更新task数据
				mTaskDB.updateTaskNubmerTable(mPreTaskInfo.getTestTaskCode(),
						mExecNum);
				LOG.Log(LOG.LOG_I, "[EXEC NUM] current num :" + mExecNum);
				mCaseNameIndex = 0;
				mTmpIndex = 1;
				// interval time
				if (mPreTaskInfo.getInterval() > 0) {
					try {
						Thread.sleep(mPreTaskInfo.getInterval());
					} catch (InterruptedException ie) {
						LOG.Log(LOG.LOG_E, ie.getMessage());
					}
				}
			}
			// send end
			sendEnd();
		}

		/********************************************************************************
		 * 写日志
		 * @param TestCode
		 ********************************************************************************/
		private void writeTaskReport(String TestCode) {
			String valuetype = mCaseResultType;
			String value = "00";
			mDevBusyEndTime = new Date();
			if (mContinueFailMax != -1) {
				if (mExecResult == 0) {
					mTaskFailedNum = 0;
				} else {
					mTaskFailedNum++;
					if (mTaskFailedNum >= mContinueFailMax) {
						Intent intent = new Intent(TASK_FALIED_WARNING);
						mCxt.sendBroadcast(intent);
					}
				}
			}

			//转换变量类型
			if (valuetype != null && valuetype.length() > 0) {
				if (valuetype.equals(TESTVALUE_TIME)) {
					long time = GetTotalExeTime(TestCode);
					double d = time / 1000;
					value = String.format("%.2f", d);
				} else if (valuetype.equals(TESTVALUE_RATE)) {
//					double rate = GetAvragExeSuccess(TestCode);
//					value = String.format("%.2f", rate);
					double rate;
					if(mExecResult == 0){
						rate = 1;
					}else{
						rate = 0;
					}
					value = String.format("%.2f", rate);
				} else if (valuetype.equals(TESTVALUE_SPEED)) {
					double speed = GetAvragDownSpeed(TestCode);
					value = String.format("%.2f", (speed / 1000));
				} else {
					long time = GetTotalExeTime(TestCode);
					double d = time / 1000;
					value = String.format("%.2f", d);
				}

				Report.addReport(mCxt, mRunningTask, mCurCaseInfo, mExecResult,
						value, mCaseNumList.get(mCaseNameIndex).mTotalNum,
						mCaseNameIndex, mDevBusyStartTime, mDevBusyEndTime,
						null);
			} else {
				Report.addReport(mCxt, mRunningTask, mCurCaseInfo, mExecResult,
						"00", mCaseNumList.get(mCaseNameIndex).mTotalNum,
						mCaseNameIndex, mDevBusyStartTime, mDevBusyEndTime,
						null);
			}

		}

		private void closeApp() {

		}

		public class CaseNum {
			public String mCaseName;
			public int mErrorNum;
			public int mSuessceNum;
			public int mTotalNum;
		}


		public class CaseItemLoopRecorder {
			public int mItemIndex;
			public String mLoopto;
			public int mLoopTotalNum;
			public int mLoopTimes;
		}

		//上传日志的线程
		private class ReportUploadHandle extends Thread {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				LOG.Log(LOG.LOG_E, "Uploading start");
				Report.UpLoadReport(mCxt);
				LOG.Log(LOG.LOG_E, "Uploading Done");
			}
		}

		/********************************************************************************
		 * 开始上传日志
		 * @param tasklist
		 ********************************************************************************/
		private void startUploadRport(ArrayList<TaskInfo> tasklist) {
			int totalcase = 0;
			Iterator<TaskInfo> task = tasklist.iterator();
			while (task.hasNext()) {
				TaskInfo item = task.next();
				Cursor caseCursor = mTaskDB.getCaseFromTaskCode(item
						.getTestTaskCode());
				int temp = caseCursor.getCount();
				if (caseCursor.getCount() > 0) {
					caseCursor.moveToFirst();
					while (!caseCursor.isAfterLast()) {
						String caseName = caseCursor.getString(1);
						int Num = caseCursor.getInt(2);
						Report.addReportCaseNum(caseName, Num);
						totalcase += Num;
						caseCursor.moveToNext();
					}

				}
				caseCursor.close();
			}
			Report.setReportTotalNum(totalcase);
			new ReportUploadHandle().start();
			LOG.Log(LOG.LOG_I, "Upload report DONE");
		}

		public class AppInfo {
			public String appName = "";
			public String packageName = "";
			public String versionName = "";
			public int versionCode = 0;
			public Drawable appIcon = null;
		}

		/********************************************************************************
		 * 检查应用是否存在
		 * @param name
		 * @return
		 ********************************************************************************/
		private boolean CheckIfAppExistInSystem(String name) {
			boolean ret = false;
			String checkName = null;

			if (name == null || "".equals(name)) {
				return true;
			}
			int index = name.indexOf("##应用名称##");
			if (index >= 0) {

				checkName = name.substring(index + "##应用名称##".length(), name
						.length());
				List<PackageInfo> packages = getPackageManager()
						.getInstalledPackages(0);
				LOG.Log(LOG.LOG_I, "Checked Application Name :" + name);
				LOG.Log(LOG.LOG_I, "Checked checkName :" + checkName);

				if (checkName == null || "".equals(checkName)) {
					return false;
				}
				for (int i = 0; i < packages.size(); i++) {
					PackageInfo packageInfo = packages.get(i);
					AppInfo tmpInfo = new AppInfo();
					tmpInfo.appName = packageInfo.applicationInfo.loadLabel(
							getPackageManager()).toString();
					tmpInfo.packageName = packageInfo.packageName;
					tmpInfo.versionName = packageInfo.versionName;
					tmpInfo.versionCode = packageInfo.versionCode;
					tmpInfo.appIcon = packageInfo.applicationInfo
							.loadIcon(getPackageManager());
					if (tmpInfo.appName.contains(checkName) == true) {
						ret = true;
						break;
					}
				}
				return ret;
			} else {
				return true;
			}

		}

		/********************************************************************************
		 * 根据脚本内编写应用名称，自动启动应用
		 * @param name 脚本名称
		 ********************************************************************************/
		private void LaunchAppAccordName(String name) {
			String appName = null;
			if (name == null || "".equals(name)) {
				return;
			}
			int index = name.indexOf("##启动应用##");
			if (index >= 0) {
				appName = name.substring(index + "##启动应用##".length(), name
						.length());
				List<PackageInfo> packages = getPackageManager()
						.getInstalledPackages(0);
				LOG.Log(LOG.LOG_I, "Checked Task Name :" + name);
				LOG.Log(LOG.LOG_I, "Checked appName :" + appName);
				for (int i = 0; i < packages.size(); i++) {
					PackageInfo packageInfo = packages.get(i);
					AppInfo tmpInfo = new AppInfo();
					tmpInfo.appName = packageInfo.applicationInfo.loadLabel(
							getPackageManager()).toString();
					tmpInfo.packageName = packageInfo.packageName;
					tmpInfo.versionName = packageInfo.versionName;
					tmpInfo.versionCode = packageInfo.versionCode;
					tmpInfo.appIcon = packageInfo.applicationInfo
							.loadIcon(getPackageManager());
					if (tmpInfo.appName.contains(appName) == true) {
						PackageInfo pi;
						try {
							pi = getPackageManager().getPackageInfo(
									tmpInfo.packageName, 0);
							Intent resolveIntent = new Intent(
									Intent.ACTION_MAIN, null);
							resolveIntent.setPackage(pi.packageName);
							PackageManager pManager = getPackageManager();
							List<ResolveInfo> apps = pManager
									.queryIntentActivities(resolveIntent, 0);
							ResolveInfo ri = apps.iterator().next();
							if (ri != null) {
								String packageName;
								packageName = ri.activityInfo.packageName;
								String className = ri.activityInfo.name;
								Intent intent = new Intent(Intent.ACTION_MAIN);
								ComponentName cn = new ComponentName(
										packageName, className);
								intent.setComponent(cn);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
								LOG.Log(LOG.LOG_I, "Launch Acitvity of :"
										+ packageName + "." + className);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						break;
					}
				}
			}
		}

		/********************************************************************************
		 * 同步告警信息
		 ********************************************************************************/
		private void SendWanningMsgToserver() {
			String DevID;
			String AlarmString;
			TelephonyManager tm = (TelephonyManager) mCxt
					.getSystemService(TELEPHONY_SERVICE);
			DevID = tm.getDeviceId();
			if (DevID == null || "".equals(DevID)) {
				DevID = "11111111111119";
			}

			AlarmString = AutoTestHttpIO.DeviceAlarm(DevID, "113",
					"APP not exist");
			if ("0000".equals(AlarmString) == true) {
				LOG.Log(LOG.LOG_I, "Alarm with server success");
			} else {
				LOG.Log(LOG.LOG_I, "Alarm with server fail");
			}
		}

		// private CaseItemLoopRecorder GetLoopInfofromCaseItem(){
		// CaseItemLoopRecorder loop = null;
		// return loop;
		//		   
		// }
		private int FinditemInLoopRecord(CaseItemLoopRecorder loop) {
			if (loop != null) {
				int index = 0;
				Iterator<CaseItemLoopRecorder> it = mExeCaseItemLoop.iterator();
				while (it.hasNext()) {
					CaseItemLoopRecorder info = it.next();
					if (info.mItemIndex == loop.mItemIndex) {
						return index;
					}
					index++;
				}
			}
			return -1;
		}

		/********************************************************************************
		 * 生成内循环信息栈
		 * @param loop
		 ********************************************************************************/
		private void AddLoopItemToRecord(CaseItemLoopRecorder loop) {
			if (loop != null && loop.mLoopto != null
					&& loop.mLoopto.length() > 0) {
				mExeCaseItemLoop.add(loop);
			}
		}

		/********************************************************************************
		 * 在内循环栈里查询需要跳转ID
		 * @param index
		 * @return
		 ********************************************************************************/
		private String FindLooptoFromRecord(int index) {
			if (index < mExeCaseItemLoop.size()) {
				CaseItemLoopRecorder loopitem = mExeCaseItemLoop.get(index);
				if (loopitem != null) {
					if (loopitem.mLoopTimes < loopitem.mLoopTotalNum) {
						mExeCaseItemLoop.get(index).mLoopTimes++;
						if (loopitem.mLoopto != null) {
							return loopitem.mLoopto;
						}
					}
				}

			}
			return null;
		}

	}

	// sort
	// 优先级排序
	class PendingQComparePriority implements Comparator<TaskInfo> {

		public int compare(TaskInfo task1, TaskInfo task2) {
			int result = 0;
			if (task1 != null && task2 != null) {
				boolean p = (task1.getPriority() - task2.getPriority()) <= 0;
				if (p) {
					result = 1;
				} else {
					result = -1;
				}
			}
			return result;
		}

		public boolean equals(TaskInfo obj) {
			return true;
		}
	}

	// 时间排序
	class PendingQCompareDate implements Comparator<TaskInfo> {

		public int compare(TaskInfo task1, TaskInfo task2) {
			int result = 0;
			if (task1 != null && task2 != null) {
				boolean d = !(task1.getStartDate().after(task2.getStartDate()));
				if (d) {
					result = 1;
				} else {
					result = -1;
				}
			}
			return result;
		}

		public boolean equals(TaskInfo obj) {
			return true;
		}
	}

	/********************************************************************************
	 * 预留，原始设计是在所有任务的最后执行时间结束后上传，目前是执行完，立即上传。
	 * @param tasklist
	 ********************************************************************************/
	private void SetReportUpdateAlarm(ArrayList<TaskInfo> tasklist) {
		Iterator<TaskInfo> info = tasklist.iterator();

		// 初始化last date
		Iterator<TaskInfo> last_info = tasklist.iterator();
		TaskInfo last_task = last_info.next();
		Date last = last_task.getEndDate();
		// 初始化last date完成


		while (info.hasNext()) {
			TaskInfo item = info.next();
			Date enddate = item.getEndDate();
			if (enddate.after(last)) {
				last = enddate;
				last_task = item;
			}
		}


	}

	//预留
	private void startPackageUploadalarm(Date last, TaskInfo last_task) {
		if (last == null || last_task == null) {
			return;
		}
		Calendar packCal = Calendar.getInstance();
		packCal.set(Calendar.YEAR, last.getYear() + 1900);
		packCal.set(Calendar.MONTH, last.getMonth());
		packCal.set(Calendar.DAY_OF_MONTH, last.getDate());
		packCal.set(Calendar.HOUR_OF_DAY, last.getHours());
		packCal.set(Calendar.MINUTE, last.getMinutes());
		packCal.set(Calendar.SECOND, last.getSeconds());
		packCal.set(Calendar.MILLISECOND, 0);
		Intent intent = new Intent(PACKAGE_UPLOAD_BROADCAST_ID);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		AlarmManager am;
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, packCal.getTimeInMillis(), sender);
		Date d = new Date();
		d.setTime(packCal.getTimeInMillis());
		LOG.Log(LOG.LOG_I, "upload 有效时间" + d.toLocaleString());
	}

	/********************************************************************************
	 * 从数据库中删除不存在的任务记录
	 * @param ExistCodes 任务列表中的TestCodes
	 * @param taskCode	 数据库中的taskCode
	 ********************************************************************************/
	public void RemoveNotExistTaskCodeFromDB(ArrayList<String> ExistCodes,
			String taskCode) {
		boolean deleteflag = false;
		for (int index = 0; index < ExistCodes.size(); index++) {
			String Code = ExistCodes.get(index);
			if (Code != null && Code.equals(taskCode)) {
				deleteflag = true;
				break;
			}
		}
		if (deleteflag == false) {
			mTaskDB.deleteTaskRecords(taskCode);
		}
	}

	/********************************************************************************
	 * 如果数据库中的任务信息在当前任务列表中找不到，则删除该信息。
	 ********************************************************************************/
	public void DeleteDBIfNotExist() {
		String taskpath = null;
		taskpath = AutoTestIO.getSDPath() + AutoTestIO.TaskFilePath;
		ArrayList<String> taskCodelist = new ArrayList<String>();
		if (taskpath.length() > 0) {
			File tskfile = new File(taskpath);
			InputStream in = null;
			if (!tskfile.exists()) {
				return;
			}
			try {
				in = new BufferedInputStream(new FileInputStream(tskfile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			List<TaskInfo> tskall;
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tskall = AutoTestIO.getTask();
			Iterator<TaskInfo> it = tskall.iterator();
			while (it.hasNext()) {
				TaskInfo task = it.next();
				taskCodelist.add(task.getTestTaskCode());
			}

			Cursor cursor = mTaskDB.getTask();
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				while (!cursor.isAfterLast()) {
					String taskCode = cursor.getString(0);
					//从数据库中删除
					RemoveNotExistTaskCodeFromDB(taskCodelist, taskCode);
					cursor.moveToNext();
				}
			}
			cursor.close();
		}
	}

	private boolean IsServiceRunning(String ServiceName) {
		ActivityManager mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = mActivityManager
				.getRunningServices(30);
		if ((serviceList.size() > 0)) {
			for (int i = 0; i < serviceList.size(); i++) {
				if (serviceList.get(i).service.getClassName().contains(
						ServiceName) == true) {
					return true;
				}
			}
		}
		return false;
	}

	private class CancelAlarmThread extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			cancelAlarm();
			if (DB != null) {
				DB.SetDevStateDBValue(context, DEV_STATE_OFFLINE);
			}
		}
	}

	/********************************************************************************
	 * 根据当前任务的TaskCode，在数据库中查找对应的时延记录的总和，这个总和包括各个Test ID的测试结果，
	 * 不包括Task重复测试的次数之间的和。
	 * @param Code  TaskCode
	 * @return
	 ********************************************************************************/
	private long GetTotalExeTime(String Code) {
		long time = 0;
		if (mTaskDB != null && Code != null) {
			Cursor taskCursor = mTaskDB.getTask();
			if (taskCursor.getCount() > 0) {
				taskCursor.moveToFirst();
				while (!taskCursor.isAfterLast()) {
					String taskCode = taskCursor.getString(0);
					if (Code.equals(taskCode)) {
						if (taskCursor.getInt(8) == TASK_TIME_OUT) {
							taskCursor.moveToNext();
							continue;
						}
						// case code cursor
						Cursor caseCursor = mTaskDB
								.getCaseFromTaskCode(taskCode);
						if (caseCursor.getCount() > 0) {
							caseCursor.moveToFirst();
							while (!caseCursor.isAfterLast()) {
								String caseName = caseCursor.getString(1);
								// time index cursor
								Cursor indexCursor = mTaskDB
										.getItemIndexFromTaskCode(taskCode,
												caseName);
								int count = indexCursor.getCount();
								LOG.Log(LOG.LOG_I, "indexCursor.getCount() :"
										+ count);
								if (count > 0) {
									indexCursor.moveToFirst();
									while (!indexCursor.isAfterLast()) {
										int index = indexCursor.getInt(2);
										// time cursor
										LOG.Log(LOG.LOG_I, "index :" + index);
										Cursor timeCursor = mTaskDB
												.getExecSumTimeFromTaskCode(
														taskCode, caseName,
														index);
										LOG.Log(LOG.LOG_I, "AVG cursor count :"
												+ timeCursor.getCount());
										if (timeCursor.getCount() > 0) {
											timeCursor.moveToFirst();
											while (!timeCursor.isAfterLast()) {
												LOG
														.Log(
																LOG.LOG_I,
																"timeCursor.getLong(0) :"
																		+ timeCursor
																				.getLong(0));
												time += timeCursor.getLong(0);
												timeCursor.moveToNext();
											}
										}
										timeCursor.close();
										indexCursor.moveToNext();
									}
									// 修改多次执行时间累加问题
									if (time > 0) {
										LOG.Log(LOG.LOG_I, "delete taskCode :"
												+ taskCode);
										mTaskDB.deleteTimeTable(taskCode);
									}
								}
								indexCursor.close();
								caseCursor.moveToNext();
							}
						}
						caseCursor.close();
						taskCursor.moveToNext();
					}
				}
			}
			taskCursor.close();
		}
		LOG.Log(LOG.LOG_I, "time :" + time);
		return time;
	}

	/********************************************************************************
	 * 获取下载速录的平均值，这个平均值是各个Test ID的值的平均。
	 * @param Code
	 * @return
	 ********************************************************************************/
	private double GetAvragDownSpeed(String Code) {
		double speed = 0;
		if (mTaskDB != null && Code != null) {
			Cursor taskCursor = mTaskDB.getTask();
			if (taskCursor.getCount() > 0) {
				taskCursor.moveToFirst();
				while (!taskCursor.isAfterLast()) {
					String taskCode = taskCursor.getString(0);
					if (Code.equals(taskCode)) {
						if (taskCursor.getInt(8) == TASK_TIME_OUT) {
							taskCursor.moveToNext();
							continue;
						}
						// case code cursor
						Cursor caseCursor = mTaskDB
								.getCaseFromTaskCode(taskCode);
						if (caseCursor.getCount() > 0) {
							caseCursor.moveToFirst();
							while (!caseCursor.isAfterLast()) {
								String caseName = caseCursor.getString(1);
								// time index cursor
								Cursor index1Cursor = mTaskDB
										.getItemIndexFromTaskCode(taskCode,
												caseName);
								// time cursor
								Cursor speedCursor = mTaskDB
										.getExecAvgSpeedFromTaskCode(taskCode,
												caseName, 0);
								LOG.Log(LOG.LOG_I, "AVG cursor count :"
										+ speedCursor.getCount());
								if (speedCursor.getCount() > 0) {
									speedCursor.moveToFirst();
									double speedSum = 0;
									while (!speedCursor.isAfterLast()) {
										speedSum += speedCursor.getDouble(0);
										speedCursor.moveToNext();
									}
									if (speedSum != 0) {
										speed = (double) (speedSum / speedCursor
												.getCount());
									}

								}
								speedCursor.close();
								caseCursor.moveToNext();
							}
						}
						caseCursor.close();
						taskCursor.moveToNext();
					}
				}
			}
			taskCursor.close();
		}
		LOG.Log(LOG.LOG_I, "rate = " + speed);
		return speed;
	}

	/********************************************************************************
	 * 获取测试平均成功率
	 * @param Code
	 * @return
	 ********************************************************************************/
	private double GetAvragExeSuccess(String Code) {
		double rate = 0;
		if (mTaskDB != null && Code != null) {
			Cursor taskCursor = mTaskDB.getTask();
			if (taskCursor.getCount() > 0) {
				taskCursor.moveToFirst();
				while (!taskCursor.isAfterLast()) {
					String taskCode = taskCursor.getString(0);
					if (Code.equals(taskCode)) {
						if (taskCursor.getInt(8) == TASK_TIME_OUT) {
							taskCursor.moveToNext();
							continue;
						}
						// case code cursor
						Cursor caseCursor = mTaskDB
								.getCaseFromTaskCode(taskCode);
						if (caseCursor.getCount() > 0) {
							caseCursor.moveToFirst();
							while (!caseCursor.isAfterLast()) {
								String caseName = caseCursor.getString(1);
								// time cursor
								Cursor rateCursor = mTaskDB
										.getExecAvgRateFromTaskCode(taskCode,
												caseName, 0);
								LOG.Log(LOG.LOG_I, "AVG cursor count :"
										+ rateCursor.getCount());
								if (rateCursor.getCount() > 0) {
									rateCursor.moveToFirst();
									int rateSum = 0;
									LOG.Log(LOG.LOG_I,
											"rateCursor.getCount() > 0");
									while (!rateCursor.isAfterLast()) {
										LOG.Log(LOG.LOG_I,
												"!rateCursor.isAfterLast()");
										LOG.Log(LOG.LOG_I,
												"rateCursor.getInt(0) = "
														+ rateCursor.getInt(0));
										rateSum += rateCursor.getInt(0);
										LOG.Log(LOG.LOG_I, "rateSum = "
												+ rateSum);
										rateCursor.moveToNext();
									}
									if (rateSum != 0) {
										LOG.Log(LOG.LOG_I, "rateSum 00 = "
												+ rateSum);
										LOG
												.Log(
														LOG.LOG_I,
														"rateCursor.getCount() = "
																+ rateCursor
																		.getCount());
										rate = (double) (rateSum / rateCursor
												.getCount());
									}

								}
								rateCursor.close();
								caseCursor.moveToNext();
							}
						}
						caseCursor.close();
						taskCursor.moveToNext();
					}
				}
			}
			taskCursor.close();
		}
		LOG.Log(LOG.LOG_I, "rate = " + rate);
		return rate;
	}
	/********************************************************************************
	 * 依据脚本，从success字串中获取index对应的Test ID
	 * @param index
	 * @param success  脚本中success字串用","分隔
	 * @return
	 ********************************************************************************/
	private String GetIndexStringFromeGoto(int index, String success) {
		ArrayList<String> successArray = new ArrayList<String>();
		StringTokenizer token = new StringTokenizer(success, ",");
		if (token.countTokens() <= 0) {
			return success;
		}
		while (token.hasMoreTokens()) {
			successArray.add(token.nextToken());
		}

		if (successArray.size() > 0) {
			if (index >= successArray.size()) {
				return successArray.get(0);
			}
			return successArray.get(index);
		} else {
			return success;
		}
	}

	/********************************************************************************
	 * 依据脚本，从loop字段中获取所需循环的index
	 * @param index
	 * @param loop
	 * @return
	 ********************************************************************************/
	private String GetIndexStringFromeLoop(int index, String loop) {
		ArrayList<String> loopArray = new ArrayList<String>();
		StringTokenizer token = new StringTokenizer(loop, ":");
		if (token.countTokens() <= 0) {
			return null;
		}
		while (token.hasMoreTokens()) {
			loopArray.add(token.nextToken());
		}

		if (loopArray.size() == 2) {
			if (index >= 0 && index <= 1) {
				return loopArray.get(index);
			}
			return null;
		} else {
			return null;
		}
	}

	/********************************************************************************
	 * 抓包线程
	 * @author 
	 *
	 ********************************************************************************/
	private class ThreadPacket extends Thread {
		String taskName;
		String caseId;

		public void run() {
			try {
				Date time = new Date();
				SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmmss");
				String StartDate = f.format(time);
				if (taskName == null || caseId == null) {
					return;
				}
				File targetDir = new File(AutoTestIO.getSDPath() + "/AutoTest"
						+ "/Result" + "/" + taskName + "/" + caseId);
				if (targetDir.exists() == false) {
					targetDir.mkdirs();
				}
				//执行抓包命令
				execCommandAsRoot("chmod 777 /data/local/tcpdump");
				execCommandAsRoot("/data/local/tcpdump -p -vv -s 0 -w "
						+ targetDir + "/" + StartDate + ".pcap");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		@Override
		public void interrupt() {
			// TODO Auto-generated method stub
			super.interrupt();
			taskName = null;
			caseId = null;
		}

	}

	/********************************************************************************
	 * 执行shell命令，必须在有su且安装了busybox的机器上才可以运行成功
	 * @param command
	 * @return
	 ********************************************************************************/
	private boolean execCommandAsRoot(String command) {
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

}