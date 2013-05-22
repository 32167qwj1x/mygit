/**
 * 
 */
package com.android.autotesing;

import java.sql.Date;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/***************************************************************************
 * 任务执行数据库
 * 
 * @author
 * 
 ***************************************************************************/
public class TaskDB {
	// 数据库名称
	private static final String DATABASE_NAME = "TASK_DB.db";
	// 任务表
	private static final String DB_TASK_TABLE = "task_table";
	// Case执行次数表格
	private static final String DB_CASE_NUMS_TABLE = "case_nums_table";
	// Item执行记录表格
	private static final String DB_ITEM_TABLE = "item_table";
	// 时延记录表格
	private static final String DB_TIME_TABLE = "time_table";
	// 下载速率记录表格
	private static final String DB_SPEED_TABLE = "speed_table";
	// 成功率记录表格
	private static final String DB_RATE_TABLE = "rate_table";
	// 时延记录和Case内Index关系记录表格
	private static final String DB_TIME_INDEX_TABLE = "time_index_table";
	// 任务开始时request code记录表格
	private static final String DB_REQUEST_CODE_TABLE = "request_code_table";

	//数据库版本号
	private static final int DATABASE_VERSION = 3;
	// common keys
	private final static String TASK_CODE = "task_code";
	private final static String CASE_NAME = "case_name";
	// task table keys
	private final static String TASK_NAME = "task_name";
	private final static String TASK_DESC = "task_desc";
	private final static String TASK_START_DATE = "start_date";
	private final static String TASK_END_DATE = "end_date";
	private final static String TASK_PHONE_INFO = "phone_info";
	private final static String TASK_VER = "task_version";
	private final static String TASK_NUM = "task_num";
	private final static String TASK_STATE = "task_state";
	// case table keys
	private final static String TOTAL_NUM = "total_num";
	private final static String TOTAL_ERROR_NUM = "total_error_num";
	private final static String TOTAL_SUESSCE_NUM = "total_suessce_num";
	// item table keys
	private final static String ITEM_INDEX = "item_index";
	private final static String ERROR_NUM = "cur_error_num";
	private final static String ERROR_CODE = "error_code";
	// time table keys
	private final static String EXEC_TIME = "exec_time";
	private final static String DOWN_SPEED = "down_speed";
	private final static String EXEC_SUCCESS = "exec_success";
	// request code table keys
	private final static String REQUEST_CODE = "request_code";
	private final static String COMPLETED_NUM = "completed_num";
	private final static String TASK_EXEC_TYPE = "task_exec_type";

	private static final String DATABASE_CREATE_TASK = "create table task_table (task_code text not null,"
			+ "task_name text not null,"
			+ "task_desc text not null,"
			+ "start_date long,"
			+ "end_date long,"
			+ "phone_info text,"
			+ "task_version text,"
			+ "task_num integer,"
			+ "task_state integer);";

	private static final String DATABASE_CREATE_CASE_NUMS = "create table case_nums_table (task_code text not null,"
			+ "case_name text not null,"
			+ "total_num integer,"
			+ "total_suessce_num integer,"
			+ "total_error_num integer,"
			+ "datatime long);";

	private static final String DATABASE_CREATE_ITEM = "create table item_table (task_code text not null,"
			+ "case_name text not null,"
			+ "item_index integer,"
			+ "cur_error_num integer," + "error_code integer);";

	private static final String DATABASE_CREATE_TIME = "create table time_table (task_code text not null,"
			+ "case_name text not null,"
			+ "item_index integer,"
			+ "exec_time long);";

	private static final String DATABASE_CREATE_SPEED = "create table speed_table (task_code text not null,"
			+ "case_name text not null,"
			+ "item_index integer,"
			+ "down_speed double);";

	private static final String DATABASE_CREATE_RATE = "create table rate_table (task_code text not null,"
			+ "case_name text not null,"
			+ "item_index integer,"
			+ "exec_success int);";

	private static final String DATABASE_CREATE_TIME_INDEX_TABLE = "create table time_index_table (task_code text not null,"
			+ "case_name text not null," + "item_index integer);";

	private static final String DATABASE_CREATE_REQUEST_CODE_TABLE = "create table request_code_table (task_code text not null,"
			+ "request_code integer,"
			+ "completed_num integer,"
			+ "task_exec_type integer);";

	private Context mCtx;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	private static TaskDB mInstance = null;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(DATABASE_CREATE_TASK);
				db.execSQL(DATABASE_CREATE_CASE_NUMS);
				db.execSQL(DATABASE_CREATE_ITEM);
				db.execSQL(DATABASE_CREATE_TIME);
				db.execSQL(DATABASE_CREATE_SPEED);
				db.execSQL(DATABASE_CREATE_RATE);
				db.execSQL(DATABASE_CREATE_TIME_INDEX_TABLE);
				db.execSQL(DATABASE_CREATE_REQUEST_CODE_TABLE);
			} catch (SQLException e) {
				LOG.Log(LOG.LOG_E, "onCreate threw exception");
				LOG.Log(LOG.LOG_E, e.getMessage());
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			try {
				db.execSQL("DROP TABLE IF EXISTS " + DB_TASK_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + DB_CASE_NUMS_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + DB_ITEM_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + DB_TIME_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + DB_SPEED_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + DB_RATE_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + DB_TIME_INDEX_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + DB_REQUEST_CODE_TABLE);
				onCreate(db);
			} catch (SQLException e) {
				LOG.Log(LOG.LOG_E, "onUpgrade threw exception");
				LOG.Log(LOG.LOG_E, e.getStackTrace().toString());
			}
		}
	}


	public TaskDB(Context ctx) {
		this.mCtx = ctx;
	}

	public TaskDB openDB() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void closeDB() {
		mDbHelper.close();
	}

	public void dropAll() {
		try {
			mDb.execSQL("DROP TABLE IF EXISTS " + DB_TASK_TABLE);
			mDb.execSQL("DROP TABLE IF EXISTS " + DB_TIME_INDEX_TABLE);
			mDb.execSQL("DROP TABLE IF EXISTS " + DB_CASE_NUMS_TABLE);
			mDb.execSQL("DROP TABLE IF EXISTS " + DB_ITEM_TABLE);
			mDb.execSQL("DROP TABLE IF EXISTS " + DB_TIME_TABLE);
			mDb.execSQL("DROP TABLE IF EXISTS " + DB_SPEED_TABLE);
			mDb.execSQL("DROP TABLE IF EXISTS " + DB_RATE_TABLE);
			mDb.execSQL("DROP TABLE IF EXISTS " + DB_REQUEST_CODE_TABLE);
		} catch (SQLException e) {
			LOG.Log(LOG.LOG_E, "dropAll threw exception");
			LOG.Log(LOG.LOG_E, e.getStackTrace().toString());
		}
	}

	/***************************************************************************
	 * 删除数据库内容
	 ***************************************************************************/
	public void deleteAll() {
		try {
			mDb.delete(DB_TASK_TABLE, null, null);
			mDb.delete(DB_TIME_INDEX_TABLE, null, null);
			mDb.delete(DB_ITEM_TABLE, null, null);
			mDb.delete(DB_TIME_TABLE, null, null);
			mDb.delete(DB_SPEED_TABLE, null, null);
			mDb.delete(DB_RATE_TABLE, null, null);
			mDb.delete(DB_CASE_NUMS_TABLE, null, null);
			mDb.delete(DB_REQUEST_CODE_TABLE, null, null);
		} catch (SQLException e) {
			LOG.Log(LOG.LOG_E, "deleteAll threw exception");
			LOG.Log(LOG.LOG_E, e.getStackTrace().toString());
		}
	}

	public long insertTaskTable(String task_code, String task_name,
			String task_desc, long start_date, long end_date,
			String phone_info, String task_version, int num, int state) {
		ContentValues values = new ContentValues();
		values.put(TASK_CODE, task_code);
		values.put(TASK_NAME, task_name);
		values.put(TASK_DESC, task_desc);
		values.put(TASK_START_DATE, start_date);
		values.put(TASK_END_DATE, end_date);
		values.put(TASK_PHONE_INFO, phone_info);
		values.put(TASK_VER, task_version);
		values.put(TASK_NUM, num);
		values.put(TASK_STATE, state);
		return mDb.insert(DB_TASK_TABLE, null, values);
	}

	public int updateTaskNubmerTable(String task_code, int num) {
		int index = 0;
		ContentValues values = new ContentValues();
		values.put(TASK_NUM, num);
		index = mDb.update(DB_TASK_TABLE, values, "task_code=?",
				new String[] { task_code });
		LOG.Log(LOG.LOG_I, "[TaskDB] updateTaskNubmerTable num: " + index);
		return index;
	}

	public int updateTaskStartDateTable(String task_code, long date) {
		int index = 0;
		ContentValues values = new ContentValues();
		values.put(TASK_START_DATE, date);
		index = mDb.update(DB_TASK_TABLE, values, "task_code=?",
				new String[] { task_code });
		LOG.Log(LOG.LOG_I, "[TaskDB] updateTaskStartDateTable date: " + index);
		return index;
	}

	public int updateTaskEndDateTable(String task_code, long date) {
		int index = 0;
		ContentValues values = new ContentValues();
		values.put(TASK_END_DATE, date);
		index = mDb.update(DB_TASK_TABLE, values, "task_code=?",
				new String[] { task_code });
		LOG.Log(LOG.LOG_I, "[TaskDB] updateTaskEndDateTable date: " + index);
		return index;
	}

	public long insertCaseTable(String task_code, String case_name,
			int total_num, int total_suessce_num, int total_error_num) {
		ContentValues values = new ContentValues();
		values.put(TASK_CODE, task_code);
		values.put(CASE_NAME, case_name);
		values.put(TOTAL_NUM, total_num);
		values.put(TOTAL_SUESSCE_NUM, total_suessce_num);
		values.put(TOTAL_ERROR_NUM, total_error_num);
		return mDb.insert(DB_CASE_NUMS_TABLE, null, values);
	}

	public int updateCaseTableTotalNumber(String task_code, String case_name,
			int total) {
		int index = 0;
		ContentValues values = new ContentValues();
		values.put(TOTAL_NUM, total);
		index = mDb.update(DB_CASE_NUMS_TABLE, values,
				"task_code=? and case_name=?", new String[] { task_code,
						case_name });
		LOG.Log(LOG.LOG_I, "[TaskDB] updateCaseTableTotalNumber total: "
				+ index);
		return index;
	}

	public int updateCaseTableSuessceNumber(String task_code, String case_name,
			int suessce) {
		int index = 0;
		ContentValues values = new ContentValues();
		values.put(TOTAL_SUESSCE_NUM, suessce);
		index = mDb.update(DB_CASE_NUMS_TABLE, values,
				"task_code=? and case_name=?", new String[] { task_code,
						case_name });
		LOG.Log(LOG.LOG_I, "[TaskDB] updateCaseTableTotalNumber suessce: "
				+ index);
		return index;
	}

	public int updateCaseTableErrorNumber(String task_code, String case_name,
			int error) {
		int index = 0;
		ContentValues values = new ContentValues();
		values.put(TOTAL_ERROR_NUM, error);
		index = mDb.update(DB_CASE_NUMS_TABLE, values,
				"task_code=? and case_name=?", new String[] { task_code,
						case_name });
		LOG.Log(LOG.LOG_I, "[TaskDB] updateCaseTableTotalNumber error: "
				+ index);
		return index;
	}

	public long insertTimeIndexTable(String task_code, String case_name,
			int index) {
		ContentValues values = new ContentValues();
		values.put(TASK_CODE, task_code);
		values.put(CASE_NAME, case_name);
		values.put(ITEM_INDEX, index);
		return mDb.insert(DB_TIME_INDEX_TABLE, null, values);
	}

	public long insertItemTable(String task_code, String case_name,
			int item_index, int error_num, int error_code) {
		ContentValues values = new ContentValues();
		values.put(TASK_CODE, task_code);
		values.put(CASE_NAME, case_name);
		values.put(ITEM_INDEX, item_index);
		values.put(ERROR_NUM, error_num);
		values.put(ERROR_CODE, error_code);
		return mDb.insert(DB_ITEM_TABLE, null, values);
	}

	public long insertSpeedTable(String task_code, String case_name,
			int item_index, double speed) {
		ContentValues values = new ContentValues();
		values.put(TASK_CODE, task_code);
		values.put(CASE_NAME, case_name);
		values.put(ITEM_INDEX, item_index);
		values.put(DOWN_SPEED, speed);
		return mDb.insert(DB_SPEED_TABLE, null, values);
	}

	public long insertRateTable(String task_code, String case_name,
			int item_index, int rate) {
		ContentValues values = new ContentValues();
		values.put(TASK_CODE, task_code);
		values.put(CASE_NAME, case_name);
		values.put(ITEM_INDEX, item_index);
		values.put(EXEC_SUCCESS, rate);
		return mDb.insert(DB_RATE_TABLE, null, values);
	}

	public long insertTimeTable(String task_code, String case_name,
			int item_index, long time) {
		ContentValues values = new ContentValues();
		values.put(TASK_CODE, task_code);
		values.put(CASE_NAME, case_name);
		values.put(ITEM_INDEX, item_index);
		values.put(EXEC_TIME, time);
		return mDb.insert(DB_TIME_TABLE, null, values);
	}

	public long insertRequestCodeTable(String task_code, int request_code,
			int completed_num, int type) {
		ContentValues values = new ContentValues();
		values.put(TASK_CODE, task_code);
		values.put(REQUEST_CODE, request_code);
		values.put(COMPLETED_NUM, completed_num);
		values.put(TASK_EXEC_TYPE, type);
		return mDb.insert(DB_REQUEST_CODE_TABLE, null, values);
	}

	public int updateRequestCodeTable(int completed_num, int request_code) {
		ContentValues values = new ContentValues();
		values.put(COMPLETED_NUM, completed_num);
		return mDb.update(DB_REQUEST_CODE_TABLE, values, "request_code=?",
				new String[] { Integer.toString(request_code) });
	}

	public int updateTaskTable(long start_date, long end_date, String task_code) {
		ContentValues values = new ContentValues();
		values.put(TASK_START_DATE, start_date);
		values.put(TASK_END_DATE, end_date);
		return mDb.update(DB_TASK_TABLE, values, "case_name=?",
				new String[] { task_code });
	}

	/*
	 * if task record was exist and task version had not been changed delete
	 * task records from all tables
	 */
	public void deleteTaskRecords(String task_code) {
		mDb.delete(DB_TASK_TABLE, "task_code=?", new String[] { task_code });
		mDb.delete(DB_TIME_INDEX_TABLE, "task_code=?",
				new String[] { task_code });
		mDb.delete(DB_ITEM_TABLE, "task_code=?", new String[] { task_code });
		mDb.delete(DB_TIME_TABLE, "task_code=?", new String[] { task_code });
		mDb.delete(DB_SPEED_TABLE, "task_code=?", new String[] { task_code });
		mDb.delete(DB_RATE_TABLE, "task_code=?", new String[] { task_code });
		mDb.delete(DB_CASE_NUMS_TABLE, "task_code=?",
				new String[] { task_code });
		mDb.delete(DB_REQUEST_CODE_TABLE, "task_code=?",
				new String[] { task_code });
	}

	public Cursor getTask() {
		/* sql: select * from task_table where case_name=? */
		Cursor cursor = mDb.query(DB_TASK_TABLE, null, null, null, null, null,
				null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor getTaskFromTaskCode(String code) {
		/* sql: select {"end_date","task_num"} from task_table where case_name=? */
		String columns[] = { "end_date", "task_num" };
		Cursor cursor = mDb.query(DB_TASK_TABLE, columns, "task_code=?",
				new String[] { code }, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor getCaseFromTaskCode(String task_code, String case_name) {
		/* sql: select * from case_table where case_name=? */
		String columns[] = { "total_num", "total_error_num",
				"total_suessce_num", };
		Cursor cursor = mDb.query(DB_CASE_NUMS_TABLE, columns,
				"task_code=? and case_name=?", new String[] { task_code,
						case_name }, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor getCaseFromTaskCode(String task_code) {
		/* sql: select * from case_table where case_name=? */
		Cursor cursor = mDb.query(DB_CASE_NUMS_TABLE, null, "task_code=?",
				new String[] { task_code }, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor getItemIndexFromTaskCode(String task_code, String case_name) {
		/* sql: select * from case_table where case_name=? */
		Cursor cursor = mDb.query(DB_TIME_INDEX_TABLE, null,
				"task_code=? and case_name=?", new String[] { task_code,
						case_name }, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor getItemFromTaskCode(String task_code, String case_name) {
		/* sql: select * from item_table where case_name=? and case_name=? */
		Cursor cursor = mDb.query(DB_ITEM_TABLE, null,
				"task_code=? and case_name=?", new String[] { task_code,
						case_name }, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	/***************************************************************************
	 * 计算平均时延
	 * @param task_code
	 * @param case_name
	 * @param index
	 * @return
	 ***************************************************************************/
	public Cursor getExecAvgTimeFromTaskCode(String task_code,
			String case_name, int index) {
		/*
		 * sql: select avg(exec_time) from time_table where case_name=? and
		 * task_code=?
		 */
		String columns[] = { "avg(exec_time)" };
		Cursor cursor = null;
		try {
			cursor = mDb.query(DB_TIME_TABLE, columns,
					"task_code=? and case_name=? and item_index=?",
					new String[] { task_code, case_name,
							Integer.toString(index) }, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
			}
		} catch (SQLException se) {
			LOG.Log(LOG.LOG_E, se.getMessage());
		}

		return cursor;
	}

	/***************************************************************************
	 * 计算平均下载速率
	 * @param task_code
	 * @param case_name
	 * @param index
	 * @return
	 ***************************************************************************/
	public Cursor getExecAvgSpeedFromTaskCode(String task_code,
			String case_name, int index) {
		/*
		 * sql: select avg(exec_time) from time_table where case_name=? and
		 * task_code=?
		 */
		String columns[] = { "avg(down_speed)" };
		Cursor cursor = null;
		try {
			cursor = mDb.query(DB_SPEED_TABLE, columns,
					"task_code=? and case_name=?", new String[] { task_code,
							case_name }, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
			}
		} catch (SQLException se) {
			LOG.Log(LOG.LOG_E, se.getMessage());
		}

		return cursor;
	}

	/***************************************************************************
	 * 计算平均成功率
	 * @param task_code
	 * @param case_name
	 * @param index
	 * @return
	 ***************************************************************************/
	public Cursor getExecAvgRateFromTaskCode(String task_code,
			String case_name, int index) {
		/*
		 * sql: select avg(exec_time) from time_table where case_name=? and
		 * task_code=?
		 */
		String columns[] = { "avg(exec_success)" };
		Cursor cursor = null;
		try {
			cursor = mDb.query(DB_RATE_TABLE, columns,
					"task_code=? and case_name=?", new String[] { task_code,
							case_name }, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
			}
		} catch (SQLException se) {
			LOG.Log(LOG.LOG_E, se.getMessage());
		}

		return cursor;
	}

	public Cursor getRequestCode() {
		/* sql: select * from request_code_table */
		Cursor cursor = mDb.query(DB_REQUEST_CODE_TABLE, null, null, null,
				null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor getRequestCodeFromTaskCode(String task_code) {
		/* sql: select * from request_code_table where task_code=? */
		Cursor cursor = mDb.query(DB_REQUEST_CODE_TABLE, null, "task_code=?",
				new String[] { task_code }, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor getRequestCodeFromRequestCode(String task_code,
			int request_code) {
		/* sql: select * from request_code_table where request_code=? */
		Cursor cursor = mDb.query(DB_REQUEST_CODE_TABLE, null,
				"request_code=? and task_code=?", new String[] {
						Integer.toString(request_code), task_code }, null,
				null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public void deleteRequestCodeFromTaskCode(String task_code) {
		mDb.delete(DB_REQUEST_CODE_TABLE, "task_code=?",
				new String[] { task_code });
	}

	public void deleteRequestCodeFromRequestCode(int request_code) {
		mDb.delete(DB_REQUEST_CODE_TABLE, "request_code=?",
				new String[] { Integer.toString(request_code) });
	}

	/***************************************************************************
	 * 计算总时延
	 * @param task_code
	 * @param case_name
	 * @param index
	 * @return
	 ***************************************************************************/
	public Cursor getExecSumTimeFromTaskCode(String task_code,
			String case_name, int index) {
		/*
		 * sql: select avg(exec_time) from time_table where case_name=? and
		 * task_code=?
		 */
		String columns[] = { "sum(exec_time)" };
		Cursor cursor = null;
		try {
			cursor = mDb.query(DB_TIME_TABLE, columns,
					"task_code=? and case_name=? and item_index=?",
					new String[] { task_code, case_name,
							Integer.toString(index) }, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
			}
		} catch (SQLException se) {
			LOG.Log(LOG.LOG_E, se.getMessage());
		}

		return cursor;
	}

	// 修改多次执行时间累加问题
	public void deleteTimeTable(String task_code) {
		mDb.delete(DB_TIME_TABLE, "task_code=?", new String[] { task_code });
		mDb.delete(DB_TIME_INDEX_TABLE, "task_code=?",
				new String[] { task_code });
	}

}
