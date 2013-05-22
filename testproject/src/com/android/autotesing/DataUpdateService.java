package com.android.autotesing;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.android.ftp.AutoTestFtpIO;
import com.android.http.AutoTestHttpIO;
import com.android.io.AutoTestIO;
import com.android.xmlParse.TaskInfo;
import com.android.xmlParse.WarningInfo;
import com.android.xmlParse.xmlParse;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager.MemoryInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.TrafficStats;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.StatFs;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/********************************************************************************
 * 
 * @author
 * 
 ********************************************************************************/
public class DataUpdateService extends Service {
	// 广播接收器
	private UpdateReceiver mReceiverHandler;
	// SD卡移除广播接收器
	private SDcardRemoveReceiver mSDcardHandler;
	private Context mCxt;
	// 数据库
	private DataBaseRestore DB;
	private TaskRecord mTaskRecord;
	private xmlParse mXmlParse = new xmlParse();
	private ArrayList<TaskInfo> mRunningList = new ArrayList<TaskInfo>();
	private String LOG_TAG = "autotesing";
	// 设备状态定义
	private final int TASK_READY = -1;
	private final String DEV_STATE_IDLE = "00";
	private final String DEV_STATE_BUSY = "01";
	private final String DEV_STATE_EXCEPTION = "02";
	private final String DEV_STATE_OFFLINE = "03";
	// 告警信息
	private WarningInfo mWarningInfo = null;
	// 器件监听管理
	private SensorManager mManager = null;
	// 告警线程
	private ThreadWarning mWarningThread = null;
	private boolean killWarnTread = false;
	// 流量记录
	private long mTrafficRxStart = 0;
	// 点亮记录
	private int mAlarmPowerlevel = -1;
	// 设备温度记录
	private float mAlarmTemperature = -1;
	// 设备信号记录
	private int mAlarmSigalValue = -1;

	// 告警编码定义
	private final String ALARM_MSG_BATTERY_HOT = "101";
	private final String ALARM_MSG_SCREEN_OFF = "104";
	private final String ALARM_MSG_SDCARD_REMOVE = "106";
	private final String ALARM_MSG_CPU_BUSY = "107";
	private final String ALARM_MSG_RAM_FULL = "108";
	private final String ALARM_MSG_ROM_FULL = "109";
	private final String ALARM_MSG_LOW_POWER = "110";
	private final String ALARM_MSG_SIGNAL_WEAK = "111";
	private final String ALARM_MSG_DEVICE_HOT = "112";
	private final String ALARM_MSG_NETWORK_COST = "114";
	private final String ALARM_MSG_TASK_FAILED = "119";

	// 设备状态定时更新闹钟广播
	private static final String UPDATE_BROADCAST_ID = "android.intent.action.UPDATE_ALARM";
	// 设备更新间隔设置广播
	private static final String UPDATE_DEV_ALARM = "android.intent.action.UPDATE_DEV_ALARM";

	// 任务更新广播
	private static final String UPDATE_TASK_ID = "android.intent.action.UPDATE_TASK_FILE";
	// 任务更新间隔设置广播
	private static final String UPDATE_TASK_ALARM = "android.intent.action.UPDATE_TASK_ALARM";

	// 更新任务列表Activity
	private static final String REFRESH_FROM_SERVICE = "REFRESH_ACTIVITY_FROM_DATA_SERVICE";

	// 更新正在运行任务广播
	private static final String REFRESH_RUNNING_TASK_LIST = "android.intent.action.REFRESH_RUNNING_TASKS";

	// 启动测试服务命令
	private static final String SERVICE_ACTION = "com.android.autotesing.START_TASK_SERVICE";

	// 短信监听广播
	private static final String SMS_UPDATE_ACTION = "android.provider.Telephony.SMS_RECEIVED";

	// 转发短信内容
	private static final String SMS_SEND_EXTRA_KEY = "SMS_SEND_DATA";

	// 与底层执行端握手消息
	private final static String TASK_SEND_ACK_KEY = "TASK_SEND_ACK";

	// 连续执行错误广播
	private final static String TASK_FALIED_WARNING = "android.intent.action.TASK_EXE_FALED_WARNING";

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		mCxt = this;
		killWarnTread = false;
		StartUpdateAlarm();
		Log.i("warning", "Dateupdate service start");
		InputStream warninginputstream = null;

		// 读取告警配置文件
		File warningFile = new File(AutoTestIO.getSDPath() + "/AutoTest"
				+ "/WarningSetting.xml");
		if (warningFile.exists()) {
			Log.i("warning", "warningFile.exists()");
			try {
				warninginputstream = new BufferedInputStream(
						new FileInputStream(warningFile));
				mWarningInfo = mXmlParse.WarningParse(warninginputstream);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				LOG.Log(LOG.LOG_E, "parseWarningInfo Exception");
				LOG.Log(LOG.LOG_E, e.getMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// 注册告警温度告警监视器
			RegisterTempSensorWarning();
			// 注册SD卡状态监视器
			RegisterSdcardRemoveWarning();
			// 注册信号状态监视器
			RegisterSignalWarning();
			// 开启告警线程
			StartWarningcheckThread();
			// 设置起始流量
			SetStartTrafficRxRecord();

		} else {
			LOG.Log(LOG.LOG_E, "Warning file not exist");
		}

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mReceiverHandler != null) {
			this.unregisterReceiver(mReceiverHandler);
		}

		if (mSDcardHandler != null) {
			this.unregisterReceiver(mSDcardHandler);
		}

		if (mWarningThread != null) {
			if (mWarningThread.isAlive() == true) {
				mWarningThread.interrupt();
				mWarningThread.stop();
				killWarnTread = true;
			}
		}

		if (DB != null) {
			DB.SetDevStateDBValue(mCxt, DEV_STATE_OFFLINE);
			// SyncStateWithServer(DEV_STATE_OFFLINE);
		}

	}

	private void RegisterSdcardRemoveWarning() {
		mSDcardHandler = new SDcardRemoveReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		intentFilter.addDataScheme("file");
		registerReceiver(mSDcardHandler, intentFilter);
	}

	private void RegisterTempSensorWarning() {
		mManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);
		mManager.registerListener(TempListener, mManager
				.getDefaultSensor(Sensor.TYPE_TEMPERATURE),
				SensorManager.SENSOR_DELAY_FASTEST);

	}

	private void StartWarningcheckThread() {
		Log.i("warning", "StartWarningcheckThread");
		if (mWarningThread == null) {
			Log.i("warning", "mWarningThread == null");
			mWarningThread = new ThreadWarning();
		}
		Log.i("warning", "mWarningThread.isAlive() == "
				+ mWarningThread.isAlive());
		if (mWarningThread.isAlive() == false) {
			Log.i("warning", "mWarningThread.start()");
			mWarningThread.start();
		}
	}

	private void SetStartTrafficRxRecord() {
		mTrafficRxStart = TrafficStats.getTotalRxBytes();
	}

	private void RegisterSignalWarning() {
		TelephonyManager Tel;
		MyPhoneStateListener MyListener;

		MyListener = new MyPhoneStateListener();
		Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}
	/********************************************************************************
	 * 开启更新服务定时程序
	 /********************************************************************************/
	private void StartUpdateAlarm() {
		mReceiverHandler = new UpdateReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_BROADCAST_ID);
		filter.addAction(TASK_FALIED_WARNING);
		filter.addAction(UPDATE_TASK_ID);
		filter.addAction(UPDATE_DEV_ALARM);
		filter.addAction(UPDATE_TASK_ALARM);
		filter.addAction(REFRESH_RUNNING_TASK_LIST);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		filter.addAction(SMS_UPDATE_ACTION);
		filter.setPriority(2147483647);
		this.registerReceiver(mReceiverHandler, filter);
		DB = DataBaseRestore.getInstance(mCxt);
		// 初始化设备状态
		InitialDeviceState();
		// 设备状态更新
		StartDeviceUpdateAlarm();
		// 任务更新
		StartTaskUpdateAlarm();

	}

	/********************************************************************************
	 * 初始化设备状态，检查后台测试服务是否开启，未开启则为空闲状态
	 /********************************************************************************/
	private void InitialDeviceState() {
		boolean isTaskServRunning = false;

		ActivityManager mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = mActivityManager
				.getRunningServices(30);

		if (DB == null) {
			return;
		}
		if ((serviceList.size() > 0)) {
			for (int i = 0; i < serviceList.size(); i++) {
				if (serviceList.get(i).service.getClassName().contains(
						"TaskService") == true) {
					isTaskServRunning = true;
					break;
				}
			}
			if (isTaskServRunning) {
				DB.SetDevStateDBValue(mCxt, DEV_STATE_IDLE);
				return;
			}
		}
		DB.SetDevStateDBValue(mCxt, DEV_STATE_OFFLINE);

	}

	/********************************************************************************
	 * 启动自动更新任务定时器，默认时间为30分钟一次
	 /********************************************************************************/
	private void StartDeviceUpdateAlarm() {

		String StDB = DB.getDevUpdateTimeValue();
		int duration = 30;
		if (StDB != null && "".equals(StDB) == false) {
			duration = Integer.parseInt(StDB);
		}
		duration = duration * 60 * 1000;

		Intent intent = new Intent(UPDATE_BROADCAST_ID);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		AlarmManager am;
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		// am.set(AlarmManager.RTC_WAKEUP,
		// System.currentTimeMillis()+duration,sender);
		if (duration > 0) {
			am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
					+ duration, duration, sender);
		} else {
			am.cancel(sender);
		}
	}

	/********************************************************************************
	 *设置设备状态自动更新定时器，默认为30分钟更新一次
	 ********************************************************************************/
	private void UpdateDeviceUpdateAlarm() {

		String StDB = DB.getDevUpdateTimeValue();
		int duration = 30;
		if (StDB != null && "".equals(StDB) == false) {
			duration = Integer.parseInt(StDB);
		}
		duration = duration * 60 * 1000;

		Intent intent = new Intent(UPDATE_BROADCAST_ID);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		AlarmManager am;
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		// am.set(AlarmManager.RTC_WAKEUP,
		// System.currentTimeMillis()+duration,sender);
		if (duration > 0) {
			am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
					+ duration, duration, sender);
		}
	}

	/********************************************************************************
	 * 开启更新任务定时程序
	 ********************************************************************************/
	private void StartTaskUpdateAlarm() {

		String TaskUpdatetime = DB.getTaskUpdateTimeValue();
		int duration = 0;
		if (TaskUpdatetime != null && "".equals(TaskUpdatetime) == false) {
			duration = Integer.parseInt(TaskUpdatetime);
		}
		duration = duration * 60 * 1000;
		Intent intent = new Intent(UPDATE_TASK_ID);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		AlarmManager am;
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		// am.set(AlarmManager.RTC_WAKEUP,
		// System.currentTimeMillis()+duration,sender);
		if (duration > 0) {
			am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
					+ duration, duration, sender);
		} else {
			am.cancel(sender);
		}

	}

	private void UpdateTaskUpdateAlarm() {

		String TaskUpdatetime = DB.getTaskUpdateTimeValue();
		int duration = 0;
		if (TaskUpdatetime != null && "".equals(TaskUpdatetime) == false) {
			duration = Integer.parseInt(TaskUpdatetime);
		}
		duration = duration * 60 * 1000;
		Intent intent = new Intent(UPDATE_TASK_ID);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		AlarmManager am;
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		// am.set(AlarmManager.RTC_WAKEUP,
		// System.currentTimeMillis()+duration,sender);
		if (duration > 0) {
			am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
					+ duration, duration, sender);
		}
	}

	// 与服务器同步设备状态
	private void SyncStateWithServer(String value) {
		String DevID;
		String DevSt = DB.getDevStateValue();
		TelephonyManager tm = (TelephonyManager) this
				.getSystemService(TELEPHONY_SERVICE);
		DevID = tm.getDeviceId();
		if (DevID == null || "".equals(DevID)) {
			DevID = "11111111111119";
		}
		Log.i(LOG_TAG, "DevID :" + DevID);
		Log.i(LOG_TAG, "DevSt :" + DevSt);
		String serverret;
		serverret = AutoTestHttpIO.setDeviceStatus(DevID, DevSt);
		if ("0000".equals(serverret) == true) {
			Log.i(LOG_TAG, "Sync Deivce with Server Success");
		} else {
			Log.i(LOG_TAG, "Sync Deivce with Server Fail");
		}

	}

	/********************************************************************************
	 * 与服务器同步设备状态
	 ********************************************************************************/
	private void SyncTaskWithServer() {
		Log.i(LOG_TAG, "Task updating");
		String DevID;
		TelephonyManager tm = (TelephonyManager) this
				.getSystemService(TELEPHONY_SERVICE);
		DevID = tm.getDeviceId();
		if (DevID == null || "".equals(DevID)) {
			DevID = "11111111111119";
		}
		String TestCodes = "";
		List<TaskInfo> tskall;
		tskall = AutoTestIO.getTask();
		if (tskall != null && tskall.size() > 0) {
			for (int i = 0; i < tskall.size(); i++) {
				if (TestCodes.equals("") == false) {
					TestCodes += ",";
				}
				TestCodes = TestCodes + tskall.get(i).getTestTaskCode();
			}
		}
		Log.i(LOG_TAG, "TestCodes = " + TestCodes);
		int provCode = Integer.parseInt(DB.getPovCodeValue());
		AutoTestIO.getNewTask(DevID, DB.getDevTypeValue(), provCode, TestCodes,
				mCxt);
		Log.i(LOG_TAG, "Task update DONE!");
	}

	private void SyncScriptWithServer() {
		Log.i(LOG_TAG, "Scripts updating");
		String DevType = DB.getDevTypeValue();
		AutoTestFtpIO.getAllScriptFile(DevType, DB.getPovCodeValue(), mCxt);

		Log.i(LOG_TAG, "Scripts DONE!");
	}

	/********************************************************************************
	 * 广播接收器
	 * @author 
	 *
	 */
	public class UpdateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(UPDATE_BROADCAST_ID)) {
				// Toast.makeText(mCxt,"Alarm Arriving",
				// Toast.LENGTH_SHORT).show();

				String DevState = DB.getDevStateValue();
				Log.i(LOG_TAG, "start SyncState with Server");
				SyncStateWithServer(DevState);
				Log.i(LOG_TAG, "SyncState with Server DevState :" + DevState);
			} else if (intent.getAction().equals(UPDATE_TASK_ID)) {
				// SyncTaskWithServer();
				// SyncScriptWithServer();
				ReceiveUpdateMsg();
			} else if (intent.getAction().equals(UPDATE_DEV_ALARM)) {
				Log.i(LOG_TAG, "Update Dev update alarm");
				UpdateDeviceUpdateAlarm();
			} else if (intent.getAction().equals(UPDATE_TASK_ALARM)) {
				Log.i(LOG_TAG, "Update task update alarm");
				UpdateTaskUpdateAlarm();
			} else if (intent.getAction().equals(REFRESH_RUNNING_TASK_LIST)) {
				Log.i(LOG_TAG, "任务更新启动");
				reFreshRunningTasksList();
			} else if (intent.getAction().equals(SMS_UPDATE_ACTION)) {
				Object[] pdus = (Object[]) intent.getExtras().get("pdus");

				for (Object pdu : pdus) {
					SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
					String address = sms.getOriginatingAddress();
					String body = sms.getMessageBody();
					LOG.Log(LOG.LOG_D, "SMS receive body :" + body);
					String num = DB.getSmsFilterNum();
					if (address.contains(num)) {
						// 转发SMS给执行端
						Intent it = new Intent(
								"com.android.autotesing.TASK_TO_EXEC");
						it.putExtra(TASK_SEND_ACK_KEY, SMS_SEND_EXTRA_KEY);
						it.putExtra("pdus", pdus);
						mCxt.sendBroadcast(it);
						//
					}
					if (RecieveMsgSMSCompare(address, body) == true) {
						ReceiveUpdateMsg();
						break;
					}
				}
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				SendWanningMsgToserver(ALARM_MSG_SCREEN_OFF, true);
			} else if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
				int level = intent.getIntExtra("level", 0);
				Log.i("warning", "battery changed arrive level :" + level);
				mAlarmPowerlevel = level;
			} else if (intent.getAction().equals(TASK_FALIED_WARNING)) {
				SendWanningMsgToserver(ALARM_MSG_TASK_FAILED, false);
			}
		}

	}

	public class SDcardRemoveReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			SendWanningMsgToserver(ALARM_MSG_SDCARD_REMOVE, true);
		}

	}

	private class UpdateHandle extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			DB.SetTaskUpdatingState(true);
			SyncTaskWithServer();
			SyncScriptWithServer();
			Intent it = new Intent(REFRESH_FROM_SERVICE);
			sendBroadcast(it);
			Intent itRefresh = new Intent(REFRESH_RUNNING_TASK_LIST);
			sendBroadcast(itRefresh);
			DB.SetTaskUpdatingState(false);

		}
	}

	/********************************************************************************
	 * 更新正在运行任务列表，按照规则将新任务添加到运行任务列表中
	 ********************************************************************************/
	private void reFreshRunningTasksList() {
		mTaskRecord = new TaskRecord(this);
		mTaskRecord.getSharedPreferences("TASK_INFO");
		if (GetRunningTasksList()) {
			if (IsServiceRunning("TaskService")) {
				StopTaskService();
			}
			StartTaskService();
		}

	}

	private boolean IsServiceRunning(String ServiceName) {
		ActivityManager mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = mActivityManager
				.getRunningServices(30);
		if ((serviceList.size() > 0)) {
			for (int i = 0; i < serviceList.size(); i++) {
				String tmp = serviceList.get(i).service.getClassName();
				if (serviceList.get(i).service.getClassName().contains(
						ServiceName) == true) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean parseXmlFile(String filename) {
		File tskfile = new File(filename);
		if (!tskfile.exists()) {
			return false;
		}
		try {
			// user_select == true
			String list[] = mTaskRecord.getString("TASKLIST").split("\\|");
			List<TaskInfo> tskall;
			tskall = AutoTestIO.getTask();
			if (tskall == null) {
				return false;
			}

			Iterator<TaskInfo> it = tskall.iterator();
			while (it.hasNext()) {
				TaskInfo task = it.next();
				task.setTaskPath(tskfile.getParent());
				task.mTaskState = TASK_READY;
				LOG.Log(LOG.LOG_I, "task.getTestTaskFlag() =  :"
						+ task.getTestTaskFlag());
				if ("1".equals(task.getTestTaskFlag())) {
					mRunningList.add(task);
					LOG.Log(LOG.LOG_I, "new task insert :"
							+ task.getTestTaskName());
					continue;
				} else {
					for (int i = 0; i < list.length; i++) {
						if (task.getTestTaskCode().equals(
								list[i].split("\\#")[0])) {
							LOG.Log(LOG.LOG_I, "old running task :"
									+ task.getTestTaskName());
							mRunningList.add(task);
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.Log(LOG.LOG_E, "Exception");
			LOG.Log(LOG.LOG_E, e.getMessage());
		}
		if (mRunningList.size() > 0) {
			return true;
		}

		return false;
	}

	private boolean GetRunningTasksList() {
		String filename = AutoTestIO.getSDPath() + AutoTestIO.TaskFilePath;
		LOG.Log(LOG.LOG_I, "GetRunningTasksList task file :" + filename);
		if (!filename.equals("")) {
			if (parseXmlFile(filename)) {
				if (mRunningList.size() > 0) {
					LOG.Log(LOG.LOG_I, "Things have been Done");
					return true;
				}
			}
		}
		return false;
	}

	private void StartTaskService() {
		String taskList = "";
		int index = 0;
		while (index < mRunningList.size()) {
			TaskInfo task = mRunningList.get(index);
			taskList += (task.getTestTaskCode() + "#"
					+ task.getTestTaskVersion() + "|");
			index++;
		}
		if (taskList.length() > 1) {
			mTaskRecord.putString("TASKLIST", taskList);
		}
		Intent it = new Intent(SERVICE_ACTION);
		it.putParcelableArrayListExtra("RUNNINGLIST", mRunningList);
		startService(it);
	}

	private void StopTaskService() {
		Intent it = new Intent(SERVICE_ACTION);// SERVICE_ACTION
		this.stopService(it);
	}

	private void ReceiveUpdateMsg() {
		boolean updating = DB.getTaskUpdatingState();
		if (updating) {
			Log.i(LOG_TAG, "任务更新正在进行，本次更新会被忽略");
			return;
		}
		String DevState = DB.getDevStateValue();
		if (DEV_STATE_BUSY.equals(DevState)) {
			Log.i(LOG_TAG, "device is busy");
			DB.SetTaskHaveUpdateState(true);
		} else {
			Log.i(LOG_TAG, "update task");
			DB.SetTaskHaveUpdateState(false);
			new UpdateHandle().start();
		}
	}

	/********************************************************************************
	 * 解析短信消息
	 * @param phoneNum
	 * @param body
	 * @return
	 ********************************************************************************/
	private boolean RecieveMsgSMSCompare(String phoneNum, String body) {
		String num = DB.getSmsFilterNum();
		if (num == null || "".equals(num)) {
			return false;
		}
		if (phoneNum.contains(num) && body.contains("02后端任务更新")) {
			return true;
		} else {
			return false;
		}

	}

	/********************************************************************************
	 * 发送告警信息到服务器
	 * @param WARNING_MSG
	 * @param stop 判定是否依据告警停止测试服务
	 ********************************************************************************/
	private void SendWanningMsgToserver(String WARNING_MSG, boolean stop) {
		String DevID;
		String AlarmString;
		TelephonyManager tm = (TelephonyManager) mCxt
				.getSystemService(TELEPHONY_SERVICE);
		DevID = tm.getDeviceId();
		if (DevID == null || "".equals(DevID)) {
			DevID = "11111111111119";
		}

		LOG.Log(LOG.LOG_I, "SendWanningMsgToserver WARNING_MSG:"
				+ WARNING_MSG);
		AlarmString = AutoTestHttpIO.DeviceAlarm(DevID, WARNING_MSG, "");
		if ("0000".equals(AlarmString) == true) {
			LOG.Log(LOG.LOG_I, "Alarm with server success");
		} else {
			LOG.Log(LOG.LOG_I, "Alarm with server fail");
		}

		if (stop) {
			if (IsServiceRunning("TaskService")) {
				StopTaskService();
			}
		}
	}

	private final SensorEventListener TempListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if (event.sensor.getType() == Sensor.TYPE_TEMPERATURE) {
				float temp = event.values[SensorManager.DATA_X];
				LOG.Log(LOG.LOG_I, "current temerature :" + temp);
				mAlarmTemperature = temp;
			}
		}

	};

	private class MyPhoneStateListener extends PhoneStateListener {

		/* 得到信号的强度由每个tiome供应商,有更新 */
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);
			mAlarmSigalValue = signalStrength.getGsmSignalStrength();
		}
	}

	/********************************************************************************
	 * 根据配置表，定时检查设备状态，如需告警，发送告警信息
	 * @author 
	 *
	 ********************************************************************************/
	private class ThreadWarning extends Thread {
		public void run() {
			try {
				while (killWarnTread == false) {
					Log.i("warning", "ThreadWarning running killWarnTread :"
							+ killWarnTread);
					long startTimeMillis = System.currentTimeMillis();
					if (mWarningInfo.GetCpuUsage() != -1
							&& GetCpuUsage() > mWarningInfo.GetCpuUsage()) {
						SendWanningMsgToserver(ALARM_MSG_CPU_BUSY, false);
					}

					if (mWarningInfo.GetRamMemoryLeft() != -1
							&& GetRamAvailSize() < mWarningInfo
									.GetRamMemoryLeft()) {
						SendWanningMsgToserver(ALARM_MSG_RAM_FULL, false);
					}

					if (mWarningInfo.GetRomMemoryLeft() != -1
							&& getSDcardAvailaleSize() < mWarningInfo
									.GetRomMemoryLeft()) {
						SendWanningMsgToserver(ALARM_MSG_ROM_FULL, true);
					}

					if (mWarningInfo.GetNetworkCost() != -1
							&& GetTrafficRxRecord() > mWarningInfo
									.GetNetworkCost()) {
						SendWanningMsgToserver(ALARM_MSG_NETWORK_COST, false);
					}
					Log.i("warning", "mWarningInfo.GetBatteryLevelLeft() ="
							+ mWarningInfo.GetBatteryLevelLeft());
					if (mAlarmPowerlevel != -1
							&& mAlarmPowerlevel < mWarningInfo
									.GetBatteryLevelLeft()) {
						SendWanningMsgToserver(ALARM_MSG_LOW_POWER, false);
					}

					if (mAlarmTemperature != -1) {
						if (mWarningInfo.GetBatteryTemp() != -1
								&& mAlarmTemperature > mWarningInfo
										.GetBatteryTemp()) {
							SendWanningMsgToserver(ALARM_MSG_BATTERY_HOT, false);
						}

						if (mWarningInfo.GetDeviceTemp() != -1
								&& mAlarmTemperature > mWarningInfo
										.GetDeviceTemp()) {
							SendWanningMsgToserver(ALARM_MSG_DEVICE_HOT, false);
						}
					}

					long endTimeMillis = System.currentTimeMillis();

					if (mWarningInfo.GetSignalKey() != -1
							&& mAlarmSigalValue != -1
							&& mAlarmSigalValue < mWarningInfo.GetSignalKey()) {
						SendWanningMsgToserver(ALARM_MSG_SIGNAL_WEAK, false);
					}
					if (mWarningInfo.GetAlarmInterval() != -1) {
						int interval = mWarningInfo.GetAlarmInterval();
						if (endTimeMillis - startTimeMillis < interval * 1000) {
							long sleepTimeMillis = interval * 1000
									- (endTimeMillis - startTimeMillis);
							try {
								Thread.sleep(sleepTimeMillis);

							} catch (Exception e) {
							}
						}
					} else {
						try {
							Thread.sleep(5 * 1000);

						} catch (Exception e) {
						}
					}

				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		@Override
		public void interrupt() {
			// TODO Auto-generated method stub
			super.interrupt();
			Log.i("warning", "ThreadWarning interrupted");
		}

	}

	/********************************************************************************
	 * 获取当前流量
	 * @return 单位KB
	 ********************************************************************************/
	public long GetTrafficRxRecord() {
		long traffic = TrafficStats.getTotalRxBytes();

		if (traffic > mTrafficRxStart) {
			return (traffic - mTrafficRxStart) / 1024;
		} else {
			return -1;
		}

	}

	/********************************************************************************
	 * 获取可用空间信息
	 * @return 单位MB
	 */
	public long GetRamAvailSize() {
		long ret = 0;
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		MemoryInfo memInfo = new MemoryInfo();
		am.getMemoryInfo(memInfo);
		if (memInfo != null) {
			ret = memInfo.availMem / (1024 * 1024);
		}
		return ret;
	}

	/********************************************************************************
	 * 获取可用SD卡空间
	 * @return 单位MB
	 ********************************************************************************/
	public long getSDcardAvailaleSize() {
		File path = Environment.getExternalStorageDirectory(); // 取得sdcard文件路径
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return (availableBlocks * blockSize) / 1024 / 1024;

	}

	/********************************************************************************
	 * 获取CPU使用情况
	 * @return
	 ********************************************************************************/
	public float GetCpuUsage() {
		float use_rate = 0;
		try {
			// 执行shell命令需要点时间，重开一个线程执行操作
			String[] cpu_info = runcmd(
					new String[] { "/system/bin/top", "-n", "1" }).split(" ");

			// User 109 + Nice 0 + Sys 40 + Idle 156 + IOW 0 + IRQ 0 + SIRQ
			// 1 = 306
			// 把数字挑出来
			int[] values = { Integer.parseInt(cpu_info[1]),
					Integer.parseInt(cpu_info[4]),
					Integer.parseInt(cpu_info[7]),
					Integer.parseInt(cpu_info[10]),
					Integer.parseInt(cpu_info[13]),
					Integer.parseInt(cpu_info[16]),
					Integer.parseInt(cpu_info[19]),
					Integer.parseInt(cpu_info[21]) };
			use_rate = 100f - 100f * values[3] / values[7];
			// 增加CPU使用率数据
			// 写入一条CPU信息记录

		} catch (Exception e) {
			// 任务数-1
		}
		return use_rate;
	}

	public static synchronized String runcmd(String[] cmd) {
		String line = "";
		InputStream is = null;
		try {
			Runtime runtime = Runtime.getRuntime();
			Process proc = runtime.exec(cmd);
			is = proc.getInputStream();
			// 换成BufferedReader
			BufferedReader buf = new BufferedReader(new InputStreamReader(is));
			do {
				line = buf.readLine();
				// 前面有几个空行
				if (line.startsWith("User") || null == line) {
					// 读到第一行时，我们再读取下一行
					line = buf.readLine();
					break;
				}
			} while (true);
			if (is != null) {
				buf.close();
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}

	//预留
	public static float readUsage() {

		try {
			RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");

			String load = reader.readLine();

			String[] toks = load.split(" ");

			long idle1 = Long.parseLong(toks[5]);

			long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			try {
				Thread.sleep(360);

			} catch (Exception e) {
			}

			reader.seek(0);

			load = reader.readLine();

			reader.close();

			toks = load.split(" ");

			long idle2 = Long.parseLong(toks[5]);

			long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			return (int) (100 * (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1)));

		} catch (IOException ex) {
			ex.printStackTrace();

		}
		return 0;
	}
}
