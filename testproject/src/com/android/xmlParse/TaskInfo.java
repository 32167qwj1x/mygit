package com.android.xmlParse;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import com.android.autotesing.LOG;

import android.os.Parcel;   
import android.os.Parcelable;   

public class TaskInfo implements Parcelable{
	private String TestTaskCode;
	private String TestTaskFlag;
	private String TestTaskName;
	private String TestTaskDesc;
	private String LogLevel;
	private String TestTaskType;
	private int Priority;
	private Date StartDate;
	private Date EndDate;
	private String TestTaskVersion;
	private String NetPacketCap;
	
	private List<KeySchema> Key = null;
	
	private int ExeType;
	private Date ExeBeginTime;
	private Date ExeEndTime;
	private int IterationType;
	private int IterationNum;
	private int Interval;
	private String mDateStr;
	private String mPath;
	public int mTaskState;
	public int mRequestCode;
	public long mExecStartDate;
	public long mExecEndDate;
	
	public TaskInfo(){
		Key = new ArrayList<KeySchema>();
		StartDate = new Date();
		EndDate = new Date();
		ExeBeginTime = new Date();
		ExeEndTime = new Date();
	}

	public TaskInfo(String TestTaskCode) {
		this.TestTaskCode = TestTaskCode;
		Key = new ArrayList<KeySchema>();
		StartDate = new Date();
		EndDate = new Date();
		ExeBeginTime = new Date();
		ExeEndTime = new Date();
	}

    @Override  
    public void writeToParcel(Parcel dest, int flags) {   
        dest.writeString(TestTaskCode);
        dest.writeString(TestTaskName);
        dest.writeString(TestTaskDesc);
        dest.writeString(LogLevel);
        dest.writeInt(Priority);
        dest.writeLong(StartDate.getTime());
        dest.writeLong(EndDate.getTime());
        dest.writeTypedList(Key);
        dest.writeInt(ExeType);
        dest.writeLong(ExeBeginTime.getTime());
        dest.writeLong(ExeEndTime.getTime());
        dest.writeInt(IterationType);
        dest.writeInt(IterationNum);
        dest.writeInt(Interval);
        dest.writeString(TestTaskFlag);
        dest.writeString(TestTaskType);
        dest.writeInt(this.mTaskState);
        dest.writeString(this.mPath);
        dest.writeString(TestTaskVersion);
        dest.writeInt(mRequestCode);
        dest.writeLong(mExecStartDate);
        dest.writeLong(mExecEndDate);
        dest.writeString(NetPacketCap);
    } 	

    @Override  
    public int describeContents() {   
        return 0;   
    }   
    
    public static final Parcelable.Creator<TaskInfo> CREATOR = new Parcelable.Creator<TaskInfo>() {   
		@Override  
		public TaskInfo createFromParcel(Parcel source) {   
			TaskInfo p = new TaskInfo();   
			p.TestTaskCode = source.readString();
			p.TestTaskName = source.readString();
			p.TestTaskDesc = source.readString();
			p.LogLevel = source.readString();
			p.Priority = source.readInt();
			p.StartDate = new Date(source.readLong());
			p.EndDate = new Date (source.readLong());
			source.readTypedList(p.Key,TaskInfo.KeySchema.CREATOR);
			p.ExeType = source.readInt();
			p.ExeBeginTime = new Date(source.readLong());
			p.ExeEndTime = new Date(source.readLong());
			p.IterationType = source.readInt();
			p.IterationNum = source.readInt();
			p.Interval = source.readInt();
			p.TestTaskFlag = source.readString();
			p.TestTaskType = source.readString();
			p.mTaskState = source.readInt();
			p.mPath = source.readString();
			p.TestTaskVersion = source.readString();
			p.mRequestCode = source.readInt();
			p.mExecStartDate = source.readLong();
			p.mExecEndDate = source.readLong();
			p.NetPacketCap = source.readString();
			return p;   
		}   
		  
		@Override  
		public TaskInfo[] newArray(int size) {   
		    // TODO Auto-generated method stub   
		    return new TaskInfo[size];   
		}
	};
	
	public void setTaskPath(String path){
		mPath = path;
	}
	public String getTaskPath(){
		return mPath;
	}
	
	public String getTestTaskCode() {
		return TestTaskCode;
	}

	public String getTestTaskVersion() {
		return TestTaskVersion;
	}

	public String getNetPacketCap() {
		return NetPacketCap;
	}
	
	public void setTestTaskVersion(String TestTaskVersion) {
		this.TestTaskVersion = TestTaskVersion;
	}
	
	public void setNetPacketCap(String NetPacketCap) {
			this.NetPacketCap = NetPacketCap;
	}
	
	public void setTestTaskCode(String TestTaskCode) {
		this.TestTaskCode = TestTaskCode;
	}
	
	public String getTestTaskFlag() {
		return TestTaskFlag;
	}

	public void setTestTaskFlag(String TestTaskFlag) {
		this.TestTaskFlag = TestTaskFlag;
	}
	
	public String getTestTaskType() {
		return TestTaskType;
	}

	public void setTestTaskType(String TestTaskType) {
		this.TestTaskType = TestTaskType;
	}
	
	public String getTestTaskName() {
		return TestTaskName;
	}

	public void setTestTaskName(String TestTaskName) {
		this.TestTaskName = TestTaskName;
	}

	public String getTestTaskDesc() {
		return TestTaskDesc;
	}

	public void setTestTaskDesc(String TestTaskDesc) {
		this.TestTaskDesc = TestTaskDesc;
	}
	
	public String getLogLevel() {
		return LogLevel;
	}

	public void setLogLevel(String LogLevel) {
		this.LogLevel = LogLevel;
	}
	
	public int getPriority() {
		return Priority;
	}

	public void setPriority(int Priority) {
		this.Priority = Priority;
	}
	
	public Date getStartDate() {
		return StartDate;
	}

	public void setStartDate(String StartDate) {
		mDateStr = StartDate;
		Date d = new Date();
		d.getTime();
		try{
			this.StartDate.setHours(Integer.parseInt(StartDate.substring(8,10)));
			this.StartDate.setMinutes(Integer.parseInt(StartDate.substring(10,12)));
			this.StartDate.setSeconds(Integer.parseInt(StartDate.substring(12,14)));
			this.StartDate.setDate(Integer.parseInt(StartDate.substring(6,8)));
			this.StartDate.setMonth(Integer.parseInt(StartDate.substring(4,6)) - 1);
			this.StartDate.setYear(Integer.parseInt(StartDate.substring(0,4))-1900);
//			LOG.Log(LOG.LOG_I,"parse task start date "+this.StartDate.toLocaleString());
		}catch(Exception e){
			this.StartDate = d;
		}
	}

	public Date getEndDate() {
		return EndDate;
	}

	public void setEndDate(String EndDate) {
		Date d = new Date();
		d.getTime();
		try{
			this.EndDate.setYear(Integer.parseInt(EndDate.substring(0,4))-1900);
			this.EndDate.setMonth(Integer.parseInt(EndDate.substring(4,6)) - 1);
			this.EndDate.setDate(Integer.parseInt(EndDate.substring(6,8)));
			this.EndDate.setHours(Integer.parseInt(EndDate.substring(8,10)));
			this.EndDate.setMinutes(Integer.parseInt(EndDate.substring(10,12)));
			this.EndDate.setSeconds(Integer.parseInt(EndDate.substring(12,14)));
		}catch(Exception e){
			this.EndDate = d;;
		}
	}
	
	public List<KeySchema> getKey() {
		return Key;
		
	}

	public void addKey(KeySchema Key) {
		this.Key.add(Key);
	}

	public KeySchema newKey(){
		return new KeySchema();
	}
	
	public void setKey(List<KeySchema> key) {
		this.Key = key;
	}
	
	public int getExeType() {
		return ExeType;
	}

	public void setExeType(int ExeType) {
		this.ExeType = ExeType;
	}
	
	public Date getExeBeginTime() {
		return ExeBeginTime;
	}

	public void setExeBeginTime(String ExeBeginTime) {
		Date d = new Date();
		d.getTime();
		try{
			int hour = Integer.parseInt(ExeBeginTime.substring(0,2));
			int minute = Integer.parseInt(ExeBeginTime.substring(2,4));
			int second = Integer.parseInt(ExeBeginTime.substring(4,6));
			
			this.ExeBeginTime.setYear(Integer.parseInt(mDateStr.substring(0,4))-1900);
			this.ExeBeginTime.setMonth(Integer.parseInt(mDateStr.substring(4,6)) - 1);
			this.ExeBeginTime.setDate(Integer.parseInt(mDateStr.substring(6,8)));
			
			this.ExeBeginTime.setHours(hour);
			this.ExeBeginTime.setMinutes(minute);
			this.ExeBeginTime.setSeconds(second);
		}catch(Exception e){
			this.ExeBeginTime = d;
		}
	}

	public Date getExeEndTime() {
		return ExeEndTime;
	}

	public void setExeEndTime(String ExeEndTime) {
		Date d = new Date();
		d.getTime();
		try{
			this.ExeEndTime.setYear(Integer.parseInt(mDateStr.substring(0,4))-1900);
			this.ExeEndTime.setMonth(Integer.parseInt(mDateStr.substring(4,6)) - 1);
			this.ExeEndTime.setDate(Integer.parseInt(mDateStr.substring(6,8)));
			this.ExeEndTime.setHours(Integer.parseInt(ExeEndTime.substring(0,2)));
			this.ExeEndTime.setMinutes(Integer.parseInt(ExeEndTime.substring(2,4)));
			this.ExeEndTime.setSeconds(Integer.parseInt(ExeEndTime.substring(4,6)));
		}catch(Exception e){
			this.ExeEndTime = d;;
		}
	}
	
	public int getIterationType() {
		return IterationType;
	}

	public void setIterationType(int IterationType) {
		this.IterationType = IterationType;
	}
	
	public int getIterationNum() {
		return IterationNum;
	}

	public void setIterationNum(int IterationNum) {
		this.IterationNum = IterationNum;
	}
	
	public int getInterval() {
		return Interval;
	}

	public void setInterval(int Interval) {
		this.Interval = Interval;
	}
	
	public  static class KeySchema implements Parcelable{
		private String ServiceCode;
		private String TestKeyCode;
		private String DevId;
		private String ScriptName;
		private String ScriptVersion;
		
	    @Override  
	    public void writeToParcel(Parcel dest, int flags) {   
	        dest.writeString(ServiceCode);
	    	dest.writeString(TestKeyCode);
	    	dest.writeString(DevId);
	    	dest.writeString(ScriptName);
	    	dest.writeString(ScriptVersion);
	    } 	

	    @Override  
	    public int describeContents() {   
	        return 0;   
	    }   
	    
	    public static final Parcelable.Creator<KeySchema> CREATOR = new Parcelable.Creator<KeySchema>() {   
			@Override  
			public KeySchema createFromParcel(Parcel source) {   
				KeySchema p = new KeySchema();   			
				p.ServiceCode = source.readString();
				p.TestKeyCode = source.readString();
				p.DevId = source.readString();
				p.ScriptName = source.readString();
				p.ScriptVersion = source.readString();
				return p;   
			}   
			  
			@Override  
			public KeySchema[] newArray(int size) {   
			    return new KeySchema[size];   
			}
		};
		

		public String getServiceCode() {
			return ServiceCode;
		}

		public void setServiceCode(String ServiceCode) {
			this.ServiceCode = ServiceCode;
		}
		
		public String getTestKeyCode() {
			return TestKeyCode;
		}

		public void setTestKeyCode(String TestKeyCode) {
			this.TestKeyCode = TestKeyCode;
		}
		
		public String getDevId() {
			return DevId;
		}

		public void setDevId(String DevId) {
			this.DevId = DevId;
		}		
		
		public String getScriptName() {
			return ScriptName;
		}

		public void setScriptName(String ScriptName) {
			this.ScriptName = ScriptName;
		}	
		
		public String getScriptVersion() {
			return ScriptVersion;
		}

		public void setScriptVersion(String ScriptVersion) {
			this.ScriptVersion = ScriptVersion;
		}
		
	}
	
}
