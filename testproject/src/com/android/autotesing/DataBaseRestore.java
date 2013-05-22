package com.android.autotesing;

import java.util.ArrayList;

import com.android.http.AutoTestHttpIO;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.TelephonyManager;
import android.util.Log;

/********************************************************************************
 * 设备信息及用户输入数据库
 * 
 * @author
 * 
 ********************************************************************************/
public class DataBaseRestore {
	// 设备类型
	public String mDevType;
	// 设备名称
	public String mDevName;
	// 城市编码
	public String mCityCode;
	// 省份编码
	public String mProvCode;
	// 设备状态更新时间间隔
	public String mDevUptime;
	// 设备状态记录
	public String mDevState;
	// 本机号码信息
	public String mPhoneNumber;
	// 任务更新时间间隔
	public String mTaskUptime;
	// Ftp服务器地址
	public String mServerAdress;
	// 登录密码
	public String mServerPassWord;
	// 呼叫禁止号码
	public String mCallBlockNum;
	// 设备是否处于任务更新状态
	public boolean mIsTaskUpdating;
	// 更新记录
	public boolean mIsTaskhaveUpdate;
	// 短信过滤号码
	public String mSmsFilter;

	private static Context mContext = null;

	private Context syncCxt = null;

	// 单实例变量
	private static DataBaseRestore DB = null;

	// 设备信息关键字
	private String DEV_INFO = "DevInfo";
	private String sDev_type = "devtype";
	private String sDev_name = "devname";
	private String sCity_code = "citycode";
	private String sProv_code = "provcode";
	private String sDev_updatetime = "devupdatetime";
	private String sDev_state = "devstate";
	private String sTask_updatetime = "taskupdatetime";
	private String sServer_adress = "serveradress";
	private String sServer_password = "serverpassword";
	private String sPhone_number = "phonenumber";
	private String sCall_blockNum = "blockcallnum";
	private String sTask_Updating = "taskupdating";
	private String sTask_haveUpdate = "taskhaveupdate";
	private String sSms_filterNum = "smsfilternum";

	private ArrayList<String> strArray = new ArrayList<String>();

	/********************************************************************************
	 * 初始化数据库Context以及DB。
	 * @param Cxt
	 * @return
	 ********************************************************************************/
	public static synchronized DataBaseRestore getInstance(Context Cxt) {
		mContext = Cxt;
		if (DB == null) {
			if (mContext != null) {
				DB = new DataBaseRestore(mContext);
			} else {
				return null;
			}

		}
		return DB;
	}

	public DataBaseRestore(Context Cxt) {

		mContext = Cxt;

		if ("".equals(strArray) == true) {
			strArray.add(sDev_type);
			strArray.add(sDev_name);
			strArray.add(sCity_code);
			strArray.add(sProv_code);
			strArray.add(sDev_updatetime);
			strArray.add(sDev_state);
			strArray.add(sTask_updatetime);
			strArray.add(sServer_adress);
			strArray.add(sServer_password);
			strArray.add(sCall_blockNum);
			strArray.add(sPhone_number);
			strArray.add(sSms_filterNum);
		}
		if (mContext == null) {
			return;
		} else {
			SharedPreferences sp;
			sp = mContext.getSharedPreferences(DEV_INFO, Context.MODE_PRIVATE);
			mDevType = sp.getString(sDev_type, "");
			mDevName = sp.getString(sDev_name, "");
			mCityCode = sp.getString(sCity_code, "");
			mProvCode = sp.getString(sProv_code, "");
			mDevUptime = sp.getString(sDev_updatetime, "");
			mDevState = sp.getString(sDev_state, "");
			mTaskUptime = sp.getString(sTask_updatetime, "");
			mServerAdress = sp.getString(sServer_adress, "");
			mServerPassWord = sp.getString(sServer_password, "");
			mCallBlockNum = sp.getString(sCall_blockNum, "");
			mPhoneNumber = sp.getString(sPhone_number, "");
			mIsTaskUpdating = sp.getBoolean(sTask_Updating, false);
			mIsTaskhaveUpdate = sp.getBoolean(sTask_haveUpdate, false);
			mSmsFilter = sp.getString(sSms_filterNum, "");
		}

	}
	/********************************************************************************
	 * 获取对应字段的数据库的值
	 * @param db_str
	 * @param key
	 * @return
	 ********************************************************************************/
	private String GetDBStringValue(String db_str, String key) {
		String ret = "";
		SharedPreferences sp;

		if (mContext == null) {
			return ret;
		} else {
			sp = mContext.getSharedPreferences(db_str, Context.MODE_PRIVATE);
			ret = sp.getString(key, "");
		}
		return ret;
	}

	/********************************************************************************
	 * 数据库删除操作
	 * @return
	 ********************************************************************************/
	public boolean DeleteDB() {
		SharedPreferences sp;

		if (mContext == null) {
			return false;
		} else {
			sp = mContext.getSharedPreferences(DEV_INFO, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sp.edit();
			editor.clear();
			editor.commit();
			DB = null;
		}
		return true;
	}

	/********************************************************************************
	 * 设置响应字段的数据字符串
	 * @param db_str
	 * @param key
	 * @param value
	 * @return
	 ********************************************************************************/
	private boolean SetDBStringValue(String db_str, String key, String value) {
		boolean ret = false;
		SharedPreferences sp;

		if (mContext == null) {
			return ret;
		} else {
			sp = mContext.getSharedPreferences(db_str, Context.MODE_PRIVATE);
			Editor editor = sp.edit();
			if ("".equals(value)) {
				editor.remove(key);
			} else {
				editor.putString(key, value);
			}
			editor.commit();
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置响应字段的数据布尔值
	 * @param db_str
	 * @param key
	 * @param value
	 * @return
	 ********************************************************************************/
	private boolean SetDBBooleanValue(String db_str, String key, boolean value) {
		boolean ret = false;
		SharedPreferences sp;

		if (mContext == null) {
			return ret;
		} else {
			sp = mContext.getSharedPreferences(db_str, Context.MODE_PRIVATE);
			Editor editor = sp.edit();
			if ("".equals(value)) {
				editor.remove(key);
			} else {
				editor.putBoolean(key, value);
			}
			editor.commit();
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置设备类型
	 * @param name
	 * @return
	 ********************************************************************************/
	public boolean SetDevTypeDBValue(String name) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sDev_type, name);
			mDevType = name;
			ret = true;
		}
		return ret;
	}
	/********************************************************************************
	 * 设置设备名称
	 * @param name
	 * @return
	 ********************************************************************************/
	public boolean SetDevNameDBValue(String name) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sDev_name, name);
			mDevName = name;
			ret = true;
		}
		return ret;
	}

	
	/********************************************************************************
	 * 设置城市编码
	 * @param code
	 * @return
	 ********************************************************************************/
	public boolean SetCityCodeDBValue(String code) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sCity_code, code);
			mCityCode = code;
			ret = true;
		}
		return ret;
	}

	
	/********************************************************************************
	 * 设置省份编码
	 * @param code
	 * @return
	 ********************************************************************************/
	public boolean SetProvCodeDBValue(String code) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sProv_code, code);
			mProvCode = code;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置设备更新时间间隔
	 * @param code
	 * @return
	 ********************************************************************************/
	public boolean SetStUpdateDBValue(String code) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sDev_updatetime, code);
			mDevUptime = code;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置设备状态，同时异步更新
	 * @param Cxt
	 * @param st
	 * @return
	 ********************************************************************************/
	public boolean SetDevStateDBValue(Context Cxt, String st) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sDev_state, st);
			syncCxt = Cxt;
			mDevState = st;
			new Thread() {
				public void run() {
					try {
						SyncStateWithServer(syncCxt, mDevState);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}.start();
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置任务更新时间间隔
	 * @param st
	 * @return
	 ********************************************************************************/
	public boolean SetTaskUpdateTimeValue(String st) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sTask_updatetime, st);
			mTaskUptime = st;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置FTP服务器地址
	 * @param st
	 * @return
	 ********************************************************************************/
	public boolean SetServerAdressValue(String st) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sServer_adress, st);
			mServerAdress = st;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置服务器登录密码
	 * @param st
	 * @return
	 ********************************************************************************/
	public boolean SetServerPasswordValue(String st) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sServer_password, st);
			mServerPassWord = st;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置禁止通话号码
	 * @param st
	 * @return
	 ********************************************************************************/
	public boolean SetCallblockNumValue(String st) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sCall_blockNum, st);
			mCallBlockNum = st;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置本机号码
	 * @param num
	 * @return
	 ********************************************************************************/
	public boolean SetPhoneNumValue(String num) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sPhone_number, num);
			mPhoneNumber = num;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置短信过滤号码
	 * @param num
	 * @return
	 ********************************************************************************/
	public boolean SetSmsFilterNumValue(String num) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBStringValue(DEV_INFO, sSms_filterNum, num);
			mSmsFilter = num;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置更新标记位
	 * @param state
	 * @return
	 ********************************************************************************/
	public boolean SetTaskUpdatingState(boolean state) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBBooleanValue(DEV_INFO, sTask_Updating, state);
			mIsTaskUpdating = state;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 设置是否有更新
	 * @param state
	 * @return
	 ********************************************************************************/
	public boolean SetTaskHaveUpdateState(boolean state) {
		boolean ret = false;
		if (mContext == null) {
			return ret;
		} else {
			SetDBBooleanValue(DEV_INFO, sTask_haveUpdate, state);
			mIsTaskhaveUpdate = state;
			ret = true;
		}
		return ret;
	}

	/********************************************************************************
	 * 查询是否有为空信息
	 * @return
	 ********************************************************************************/
	public boolean isDevInfoEmpty() {
		if ("".equals(mProvCode) || "".equals(mCityCode)
				|| "".equals(mServerAdress) || "".equals(mServerPassWord)
				|| "".equals(mDevType) || "".equals(mDevName)
				|| "".equals(mPhoneNumber)) {
			return true;
		}

		return false;
	}

	public String getDevTypeValue() {
		return mDevType;
	}

	public String getPovCodeValue() {
		return mProvCode;
	}

	public String getCityCodeValue() {
		return mCityCode;
	}

	public String getDevUpdateTimeValue() {
		return mDevUptime;
	}

	public String getDevStateValue() {
		return mDevState;
	}

	public String getTaskUpdateTimeValue() {
		return mTaskUptime;
	}

	public String getServerAdressValue() {
		return mServerAdress;
	}

	public String getServerPasswordValue() {
		return mServerPassWord;
	}

	public String getCallblockNumValue() {
		return mCallBlockNum;
	}

	public String getPhoneNumValue() {
		return mPhoneNumber;
	}

	public boolean getTaskUpdatingState() {
		return mIsTaskUpdating;
	}

	public boolean getTaskHaveUpdateState() {
		return mIsTaskhaveUpdate;
	}

	public String getSmsFilterNum() {
		return mSmsFilter;
	}

	// 与服务器同步设备状态
	private void SyncStateWithServer(Context Cxt, String value) {
		String DevID;
		String DevSt = DB.getDevStateValue();
		TelephonyManager tm = (TelephonyManager) Cxt.getSystemService("phone");
		DevID = tm.getDeviceId();
		if (DevID == null || "".equals(DevID)) {
			DevID = "11111111111119";
		}
		LOG.Log(LOG.LOG_I, "DevID :" + DevID);
		LOG.Log(LOG.LOG_I, "DevSt :" + DevSt);
		String serverret;
		serverret = AutoTestHttpIO.setDeviceStatus(DevID, DevSt);
		if ("0000".equals(serverret) == true) {
			LOG.Log(LOG.LOG_I, "Sync Deivce with Server Success");
		} else {
			LOG.Log(LOG.LOG_I, "Sync Deivce with Server Fail");
		}

	}
}
