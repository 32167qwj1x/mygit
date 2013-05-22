package com.android.xmlParse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ScriptInfo {
	private String VenderCode;
	private String DevType;
	private String TestMode;
	private List<ScriptDetail> Script = null;
	
	public ScriptInfo(){
		Script = new ArrayList<ScriptDetail>();
	}
	
	public String getVenderCode() {
		return VenderCode;
	}

	public void setVenderCode(String VenderCode) {
		this.VenderCode = VenderCode;
	}
	
	public String getDevType() {
		return DevType;
	}

	public void setDevType(String DevType) {
		this.DevType = DevType;
	}
	
	public String getTestMode() {
		return TestMode;
	}

	public void setTestMode(String TestMode) {
		this.TestMode = TestMode;
	}
	
	public List<ScriptDetail> getScript() {
		return Script;
	}
	
	public void addScript(ScriptDetail Script) {
		this.Script.add(Script);
	}
	
	public ScriptDetail newScript(){
		return new ScriptDetail();
	}
	
	public class ScriptDetail {

		private String ServiceCode;
		private String TestKeyCode;
		private String ScriptName;
		private String ScriptVersion;
		private String TargetId;
		private Date Uploaddate;
		private String ScriptDesc;
	
		public ScriptDetail(){
			Uploaddate = new Date();
		}
		
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
		
		public String getTargetId() {
			return TargetId;
		}
	
		public void setTargetId(String TargetId) {
			this.TargetId = TargetId;
		}
	
		public Date getUploaddate() {
			return Uploaddate;
		}
	
		public void setUploaddate(String Uploaddate) {
			Date d = new Date();
			d.getTime();
			try{
				this.Uploaddate.setYear(Integer.parseInt(Uploaddate.substring(0,4))-1900);
				this.Uploaddate.setMonth(Integer.parseInt(Uploaddate.substring(4,6))-1);
				this.Uploaddate.setDate(Integer.parseInt(Uploaddate.substring(6,8)));
				this.Uploaddate.setHours(Integer.parseInt(Uploaddate.substring(8,10)));
				this.Uploaddate.setMinutes(Integer.parseInt(Uploaddate.substring(10,12)));
				this.Uploaddate.setSeconds(Integer.parseInt(Uploaddate.substring(12,14)));
			}catch(Exception e){
				this.Uploaddate = d;;
			}
		}	
		
		public String getScriptDesc() {
			return ScriptDesc;
		}
	
		public void setScriptDesc(String ScriptDesc) {
			this.ScriptDesc = ScriptDesc;
		}
	}
}
