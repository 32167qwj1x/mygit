/**
 * 
 */
package com.android.autotesing;

import android.content.Context;
import android.content.SharedPreferences;

/***************************************************************************
 * SharedPreferences 数据库
 * 
 * @author
 * 
 ***************************************************************************/
public class TaskRecord {

	private SharedPreferences mSharedPre;

	private Context mCxt;

	public TaskRecord(Context context) {
		mCxt = context;
	}

	public SharedPreferences getSharedPreferences(String taskCode) {
		mSharedPre = mCxt.getSharedPreferences(taskCode, Context.MODE_PRIVATE);
		return (mSharedPre != null) ? mSharedPre : null;
	}

	public void putBoolean(String key, boolean value) {
		if (mSharedPre != null) {
			mSharedPre.edit().putBoolean(key, value).commit();
		}
	}

	public boolean getBoolean(String key) {
		if (mSharedPre != null) {
			return mSharedPre.getBoolean(key, false);
		} else {
			return false;
		}
	}

	public void putInt(String key, int value) {
		if (mSharedPre != null) {
			mSharedPre.edit().putInt(key, value).commit();
		}
	}

	public int getInt(String key) {
		if (mSharedPre != null) {
			return mSharedPre.getInt(key, 0);
		} else {
			return -1000;
		}
	}

	public void putString(String key, String value) {
		if (mSharedPre != null) {
			mSharedPre.edit().putString(key, value).commit();
		}
	}

	public String getString(String key) {
		if (mSharedPre != null) {
			return mSharedPre.getString(key, "");
		} else {
			return null;
		}
	}

	/***************************************************************************
	 * 删除数据库时，同步删除运行任务列表
	 * @return
	 ***************************************************************************/
	public boolean DeleteDB() {

		if (mSharedPre == null) {
			return false;
		} else {
			mSharedPre.edit().putString("TASKLIST", "").commit();
		}
		return true;
	}

}
