package com.android.xmlParse;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class CaseInfo implements Parcelable{
	private String TestEventType;
	private String TestResultType;
	private String TestID;
	private String TestFailedGotoID;
	private String TestSuccessGotoID;
	private String TestValuetype;
	private String TestLooper;
	private int TestFailedValue;
	private boolean TestNeedPacket = false;
	private boolean ExecuteTime = false;
	
	private List<KeyInfo> Key = null;
	private List<TouchInfo> Touch = null;
	private List<String> Msg = null;
	private List<ResultPic> Pic = null;
	private List<ResultLog> Log = null;
	
	public CaseInfo(){
		Key = new ArrayList<KeyInfo>();
		Msg = new ArrayList<String>();
		Touch = new ArrayList<TouchInfo>();
		Pic = new ArrayList<ResultPic>();
		Log = new ArrayList<ResultLog>();
	}
    

	@Override  
    public void writeToParcel(Parcel dest, int flags) {   
        dest.writeString(TestEventType);
        dest.writeString(TestResultType);
        dest.writeString(TestID);
        dest.writeString(TestFailedGotoID);
        dest.writeTypedList(Key);
        dest.writeTypedList(Touch);
        dest.writeStringList(Msg);
        dest.writeTypedList(Pic);
        dest.writeTypedList(Log);
        dest.writeString(TestSuccessGotoID);
        dest.writeString(TestValuetype);
        dest.writeInt(TestFailedValue);
        dest.writeString(TestLooper);
    } 	

    @Override  
    public int describeContents() {   
        return 0;   
    }   
    
    public final Parcelable.Creator<CaseInfo> CREATOR = new Parcelable.Creator<CaseInfo>() {   
		@Override  
		public CaseInfo createFromParcel(Parcel source) {
			CaseInfo p = new CaseInfo();
			p.TestEventType = source.readString();
			p.TestResultType = source.readString();
			p.TestID = source.readString();
			p.TestFailedGotoID = source.readString();
			p.TestSuccessGotoID = source.readString();
			p.TestValuetype = source.readString();
			p.TestFailedValue = source.readInt();
			p.TestLooper = source.readString();
			source.readTypedList(p.Key,CaseInfo.KeyInfo.CREATOR);		
			source.readTypedList(p.Touch,CaseInfo.TouchInfo.CREATOR);
			source.readStringList(p.Msg);
			source.readTypedList(p.Pic,CaseInfo.ResultPic.CREATOR);
			source.readTypedList(p.Log,CaseInfo.ResultLog.CREATOR);
			return p;   
		}   
		  
		@Override  
		public CaseInfo[] newArray(int size) {   
		    // TODO Auto-generated method stub   
		    return new CaseInfo[size];   
		}
	};
	
	public String getTestEventType() {
		return TestEventType;
	}

	public void setTestEventType(String TestEventType) {
		this.TestEventType = TestEventType;
	}
	
	public String getTestResultType() {
		return TestResultType;
	}
	
	public void setTestResultType(String TestResultType) {
		this.TestResultType = TestResultType;
	}
	
	public String getTestID() {
		return TestID;
	}
	
	public void setTestID(String TestID) {
		this.TestID = TestID;
	}
	
	public String getTestFailedGotoID() {
		return TestFailedGotoID;
	}
	
	public String getTestSuccessGotoID() {
		return TestSuccessGotoID;
	}
	
	public String getTestLooper() {
		return TestLooper;
	}
	
	public String getTestValueType() {
		return TestValuetype;
	}
	
	public int getTestFailedValue(){
		return TestFailedValue;
	}
	
	public boolean getTestNeedPacket(){
		return TestNeedPacket;
	}
	
	public void setTestFailedGotoID(String TestFailedGotoID) {
		this.TestFailedGotoID = TestFailedGotoID;
	}

	public void setTestSuccessGotoID(String TestSuccessGotoID) {
		this.TestSuccessGotoID = TestSuccessGotoID;
	}

	public void setTestLooper(String TestLooper) {
		this.TestLooper = TestLooper;
	}
	
	public void setTestValueType(String TestValuetype) {
		this.TestValuetype = TestValuetype;
	}
	
	public void setTestFailedValue(int TestFailedValue){
		this.TestFailedValue = TestFailedValue;
	}
	
	public void setTestNeedPacket(String TestNeedPacket){
		if(TestNeedPacket != null && TestNeedPacket.equals("T")){
			this.TestNeedPacket = true;
		}
	}
	
	public boolean getExecuteTime() {
		return ExecuteTime;
	}
	
	public void setExecuteTime(String ExecuteTime) {
		if(ExecuteTime != null && ExecuteTime.equals("T")){
			this.ExecuteTime = true;
		}
	}
	
	public List<ResultPic> getPic() {
		return Pic;
	}

	public void addPic(ResultPic Pic) {
		this.Pic.add(Pic);
	}
	
	public ResultPic newPic(){
		return new ResultPic();
	}
	
	public List<TouchInfo> getTouch() {
		return Touch;
	}

	public void addTouch(TouchInfo Touch) {
		this.Touch.add(Touch);
	}

	public TouchInfo newTouch(){
		return new TouchInfo();
	}
	
	public List<String> getMsg() {
		return Msg;
	}

	public void addMsg(String Msg) {
		this.Msg.add(Msg);
	}

	public List<ResultLog> getLog() {
		return Log;
	}

	public void addLog(ResultLog Log) {
		this.Log.add(Log);
	}
	
	public ResultLog newLog(){
		return new ResultLog();
	}
	
	public List<KeyInfo> getKey() {
		return Key;
	}

	public void addKey(KeyInfo Key) {
		this.Key.add(Key);
	}
	
	public KeyInfo newKey(){
		return new KeyInfo();
	}
	
	public static class KeyInfo implements Parcelable{
		private String KeyCode;
              private int KeyValue;
		private String KeyType;
		private int HoldTime;
		private String Log;

	    @Override  
	    public void writeToParcel(Parcel dest, int flags) {   
	        dest.writeString(KeyCode);
               dest.writeInt(KeyValue);
	        dest.writeString(KeyType);
	        dest.writeInt(HoldTime);
	        dest.writeString(Log);
	    } 	

	    @Override  
	    public int describeContents() {   
	        return 0;   
	    }   
	    
	    public static final Parcelable.Creator<KeyInfo> CREATOR = new Parcelable.Creator<KeyInfo>() {   
			@Override  
			public KeyInfo createFromParcel(Parcel source) {   
				KeyInfo p = new KeyInfo();   			
				p.KeyCode = source.readString();
                            p.KeyValue = source.readInt();
				p.KeyType = source.readString();
				p.HoldTime = source.readInt();
				p.Log = source.readString();
				return p;   
			}   
			  
			@Override  
			public KeyInfo[] newArray(int size) {   
			    // TODO Auto-generated method stub   
			    return new KeyInfo[size];   
			}
		};
		
		public String getKeyCode() {
			return KeyCode;
		}

		public void setKeyCode(String KeyCode) {
			this.KeyCode = KeyCode;
		}

		public int getKeyValue() {
			return KeyValue;
		}

		public void setKeyValue(int KeyValue) {
			this.KeyValue = KeyValue;
		}        
		
		public String getKeyType() {
			return KeyType;
		}
		
		public void setKeyType(String KeyType) {
			this.KeyType = KeyType;
		}
		
		public int getHoldTime() {
			return HoldTime;
		}
		
		public void setHoldTime(int HoldTime) {
			this.HoldTime = HoldTime;
		}
		
		public String getLog() {
			return Log;
		}
		
		public void setLog(String Log) {
			this.Log = Log;
		}
	}

	public static class TouchInfo implements Parcelable{
		private int TouchX;
		private int TouchY;
		private String TouchType;
		private int HoldTime;
		private String Log;

	    @Override  
	    public void writeToParcel(Parcel dest, int flags) {   
	    	dest.writeInt(TouchX);
	    	dest.writeInt(TouchY);
	        dest.writeString(TouchType);
	        dest.writeInt(HoldTime);
	        dest.writeString(Log);
	    } 	

	    @Override  
	    public int describeContents() {   
	        return 0;   
	    }   
	    
	    public static final Parcelable.Creator<TouchInfo> CREATOR = new Parcelable.Creator<TouchInfo>() {   
			@Override  
			public TouchInfo createFromParcel(Parcel source) {   
				TouchInfo p = new TouchInfo();   			
				p.TouchX = source.readInt();
				p.TouchY = source.readInt();
				p.TouchType = source.readString();
				p.HoldTime = source.readInt();
				p.Log = source.readString();
				return p;   
			}   
			  
			@Override  
			public TouchInfo[] newArray(int size) {   
			    // TODO Auto-generated method stub   
			    return new TouchInfo[size];   
			}
		};
		
		public int getTouchX() {
			return TouchX;
		}

		public void setTouchX(int TouchX) {
			this.TouchX = TouchX;
		}

		public int getTouchY() {
			return TouchY;
		}

		public void setTouchY(int TouchY) {
			this.TouchY = TouchY;
		}
		
		public String getTouchType() {
			return TouchType;
		}
		
		public void setTouchType(String TouchType) {
			this.TouchType = TouchType;
		}
		
		public int getHoldTime() {
			return HoldTime;
		}
		
		public void setHoldTime(int HoldTime) {
			this.HoldTime = HoldTime;
		}
		
		public String getLog() {
			return Log;
		}
		
		public void setLog(String Log) {
			this.Log = Log;
		}
	}

	public static class ResultPic implements Parcelable{
		private String Path;
		private int PicX;
		private int PicY;
		private int PicW;
		private int PicH;

	    @Override  
	    public void writeToParcel(Parcel dest, int flags) {   
	        dest.writeString(Path);
	    	dest.writeInt(PicX);
	    	dest.writeInt(PicY);
	        dest.writeInt(PicW);
	        dest.writeInt(PicH);
	    } 	

	    @Override  
	    public int describeContents() {   
	        return 0;   
	    }   
	    
	    public static final Parcelable.Creator<ResultPic> CREATOR = new Parcelable.Creator<ResultPic>() {   
			@Override  
			public ResultPic createFromParcel(Parcel source) {   
				ResultPic p = new ResultPic();   			
				p.Path = source.readString();
				p.PicX = source.readInt();
				p.PicY = source.readInt();
				p.PicW = source.readInt();
				p.PicH = source.readInt();
				return p;   
			}   
			  
			@Override  
			public ResultPic[] newArray(int size) {   
			    // TODO Auto-generated method stub   
			    return new ResultPic[size];   
			}
		};
		
		public String getPath() {
			return Path;
		}

		public void setPath(String Path) {
			this.Path = Path;
		}

		public int getPicX() {
			return PicX;
		}

		public void setPicX(int PicX) {
			this.PicX = PicX;
		}
		
		public int getPicY() {
			return PicY;
		}
		
		public void setPicY(int PicY) {
			this.PicY = PicY;
		}
		
		public int getPicW() {
			return PicW;
		}
		
		public void setPicW(int PicW) {
			this.PicW = PicW;
		}
		
		public int getPicH() {
			return PicH;
		}
		
		public void setPicH(int PicH) {
			this.PicH = PicH;
		}
	}
	
	public static class ResultLog implements Parcelable{
		private String LogInfo;
		private String Filter;

	    @Override  
	    public void writeToParcel(Parcel dest, int flags) {   
	        dest.writeString(LogInfo);
	    	dest.writeString(Filter);
	    } 	

	    @Override  
	    public int describeContents() {   
	        return 0;   
	    }   
	    
	    public static final Parcelable.Creator<ResultLog> CREATOR = new Parcelable.Creator<ResultLog>() {   
			@Override  
			public ResultLog createFromParcel(Parcel source) {   
				ResultLog p = new ResultLog();   			
				p.LogInfo = source.readString();
				p.Filter = source.readString();
				return p;   
			}   
			  
			@Override  
			public ResultLog[] newArray(int size) {   
			    return new ResultLog[size];   
			}
		};
		
		public String getLogInfo() {
			return LogInfo;
		}

		public void setLogInfo(String LogInfo) {
			this.LogInfo = LogInfo;
		}

		public String getFilter() {
			return Filter;
		}

		public void setFilter(String Filter) {
			this.Filter = Filter;
		}
	}
	
}
