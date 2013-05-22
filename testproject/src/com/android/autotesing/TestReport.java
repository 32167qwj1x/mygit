/**
 * 
 */
package com.android.autotesing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.Cursor;
import android.os.Build;


/**
 * 此类为预留类，暂时无用
 * @author 
 *
 */


public class TestReport {
	private final static String PROJECT_CODE = "W";
	private final static String PROJECT_VERSION = "2.3.5";
	//test report separator informations
	private final static String filelog = "";
	private final static String filelog1 = "";
	private final static String filelog2 = "";
	private final static String filelog3 = "";
	private final static String filelog4 = "";
	private final static String fileprix = "";
	public final static String TASK_REPORT_START = "/****************************************************Tasks Report start********************************************/";
	public final static String TASK_DESCRIPTION = "========================================================Task Description============================================";
	public final static String TASK_TEST_INFO = "=========================================================Task Test info=============================================";
	public final static String TASK_ERROR_REPORT = "=======================================================Task Error Report============================================";
	public final static String TASK_END = "====================================================================================================================\n\n";
	public final static String TASK_CASE_SEPARATOR = "--------------------------------------------------------------------------------------------------------------------";
	public final static String TASK_REPORT_END = "/****************************************************Tasks Report end******************************************/";
	//test report detail informations
	public final static String TestTaskCode = "TestTaskCode:";
	public final static String TestTaskName = "TestTaskName:";
	public final static String TestTaskDesc = "TestTaskDesc:";
	public final static String StartDate = "StartDate:";
	public final static String EndDate = "EndDate:";
	public final static String TestMobileNum = "TestMobileNum:";
	public final static String TaskTotalNum = "TaskTotalNum:";
	//test case informations
	public final static String TestCaseName = "TestCaseName:";
	public final static String TotalNum = "TotalNum:";
	public final static String SuessceNum = "SuessceNum:";
	public final static String ErrorNum = "ErrorNum:";
	//error code table
	/*
	 * 对比结果错误码定义：
	0 表示对比正确
	pic类型：
	-1 对比图片尺寸不对
	-2 没有找到对比图片
	-3 截图不成功
	-4 超时没有返回对比结果
	msg类型：
	-5 超时消息没有完全收到
	log类型：
	-6 log对比失败
	 * */
	private final static String ErrorCode = "对比结果错误码定义：\n" + "0\t\t表示对比正确\n" + 
		"pic类型：\n" + "-1\t\t对比图片尺寸不对\n" + "-2\t\t没有找到对比图片\n" + 
		"-3\t\t截图不成功\n" + "-4\t\t超时没有返回对比结果\n" + "msg类型：\n" + "-5\t\t超时消息没有完全收到\n" + 
		"log类型：\n" + "-6 log对比失败\n";
	//test item iformations
	public final static String ErrorItem = "ErrorItem(num(index/code)):";
	public final static String ExecTime = "ExecTime(index/time(ms)):";
	private final int TASK_TIME_OUT = 3;
	private TaskDB mDB;
//	private FileService mFile;
	
	public TestReport(TaskDB db){
		mDB = db;
//		mFile = file;
	}
	
    public void prepareFileHead(){
    	Calendar calendar = Calendar.getInstance();
    	FileService.writeAppend(filelog);
    	FileService.writeAppend(filelog1);
    	FileService.writeAppend(filelog2+calendar.get(Calendar.YEAR)+"."+(calendar.get(Calendar.MONTH)+1)+"."+calendar.get(Calendar.DAY_OF_MONTH)+
    			fileprix);
    	FileService.writeAppend(filelog3 + Build.MODEL + fileprix);
    	FileService.writeAppend(filelog4 + Build.VERSION.RELEASE + fileprix);
    	FileService.writeAppend(filelog);
    	FileService.writeAppend(ErrorCode);
    	FileService.writeAppend(TASK_REPORT_START);
    }
	
	public void createReport(){
		if(mDB != null)
		{
			//write report head
			prepareFileHead();
			//write task;
			Cursor taskCursor = mDB.getTask();
			if(taskCursor.getCount() > 0){
				taskCursor.moveToFirst();
				while(!taskCursor.isAfterLast()){
					FileService.writeAppend(TASK_DESCRIPTION);
					String taskCode = taskCursor.getString(0);
					FileService.writeAppend(TestTaskCode + taskCode);
					FileService.writeAppend(TestTaskName + taskCursor.getString(1));
					FileService.writeAppend(TestTaskDesc + taskCursor.getString(2));
					FileService.writeAppend(StartDate + new Date(taskCursor.getLong(3)).toLocaleString());
					FileService.writeAppend(EndDate + new Date(taskCursor.getLong(4)).toLocaleString());
					FileService.writeAppend(TASK_TEST_INFO);
					FileService.writeAppend(TestMobileNum + taskCursor.getString(5));
					FileService.writeAppend(TaskTotalNum + taskCursor.getInt(7));
					FileService.writeAppend(TASK_ERROR_REPORT);
					if(taskCursor.getInt(8) == TASK_TIME_OUT){
						FileService.writeAppend(taskCode + ": 测试时间设置有误！！！");
						taskCursor.moveToNext();
						continue;
					}
					//case code cursor
					Cursor caseCursor = mDB.getCaseFromTaskCode(taskCode);
					if(caseCursor.getCount() > 0){
						caseCursor.moveToFirst();
						while(!caseCursor.isAfterLast()){
							String caseName = caseCursor.getString(1);
							FileService.writeAppend(TASK_CASE_SEPARATOR);
							FileService.writeAppend(TestCaseName + caseName);
							FileService.writeAppend(TotalNum + caseCursor.getInt(2));
							FileService.writeAppend(SuessceNum + caseCursor.getInt(3));
							FileService.writeAppend(ErrorNum + caseCursor.getInt(4));
							FileService.writeAppend(ErrorItem);
							//item cusor
							Cursor itemCursor = mDB.getItemFromTaskCode(taskCode, caseName);
							if(itemCursor.getCount() > 0){
								itemCursor.moveToFirst();
								while(!itemCursor.isAfterLast()){
									String itemStr = "";//111(001/-3)
									itemStr = itemCursor.getInt(3) + "(" + 
									itemCursor.getInt(2) + "/" + itemCursor.getInt(4) + ")";
									FileService.writeAppend(itemStr);
									itemCursor.moveToNext();
								}
							}
							itemCursor.close();
							//time index cursor
							FileService.writeAppend(ExecTime);
							Cursor indexCursor = mDB.getItemIndexFromTaskCode(taskCode,caseName);
							int count = indexCursor.getCount();
							if(count > 0){
								indexCursor.moveToFirst();
								while(!indexCursor.isAfterLast()){
									int index = indexCursor.getInt(2);
									// time cursor
									Cursor timeCursor = mDB.getExecAvgTimeFromTaskCode(taskCode, caseName, index);
									LOG.Log(LOG.LOG_I, "AVG cursor count :" + timeCursor.getCount());
									if(timeCursor.getCount() > 0){
										timeCursor.moveToFirst();
										while(!timeCursor.isAfterLast()){
											long time = timeCursor.getLong(0);
											FileService.writeAppend(index + "/" + time);
											timeCursor.moveToNext();
										}
									}
									timeCursor.close();
									indexCursor.moveToNext();
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
			taskCursor.close();
		}
	}
}