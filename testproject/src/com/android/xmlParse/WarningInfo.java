package com.android.xmlParse;



public class WarningInfo {
	//电池温度
	private int mBatteryTemp = -1;
	//cpu使用率
	private float mCpuUsate = -1;
	//内存剩余
	private int mRamMemLeft = -1;
	//SD卡剩余
	private int mRomMemLeft = -1;
	//电池电量
	private int mBatterylevel = -1;
	//信号强度
	private int mSingnal = -1;
	//设备温度
	private int mDeviceTemp = -1;
	//网络流量
	private int mNetworkCost = -1;
	//持续执行错误次数
	private int mContinuFailNum = -1;
	//告警时间间隔
	private int AlarmInterval = -1;
	
	
	/********************************************************************************
	 * 初始化各个告警阀值
	 ********************************************************************************/
	public WarningInfo() {
		mBatteryTemp = -1;
		mCpuUsate = -1;
		mRamMemLeft = -1;
		mRomMemLeft = -1;
		mBatterylevel = -1;
		mSingnal = -1;
		mDeviceTemp = -1;
		mNetworkCost = -1;
		AlarmInterval =-1;
	}
	
	/********************************设置和获取各个阀值信息**********************************/
	public void SetBatteryTemp(int temp){
		this.mBatteryTemp = temp;
	}
	
	public int GetBatteryTemp(){
		return this.mBatteryTemp;
	}

	public void SetCpuUsage(float usage){
		this.mCpuUsate = usage;
	}
	
	public float GetCpuUsage(){
		return this.mCpuUsate;
	}
	
	public void SetRamMemoryLeft(int ram){
		this.mRamMemLeft = ram;
	}
	
	public float GetRamMemoryLeft(){
		return this.mRamMemLeft;
	}
	
	public void SetRomMemoryLeft(int rom){
		this.mRomMemLeft = rom;
	}
	
	public float GetRomMemoryLeft(){
		return this.mRomMemLeft;
	}
	
	public void SetBatteryLevelLeft(int level){
		this.mBatterylevel = level;
	}
	
	public float GetBatteryLevelLeft(){
		return this.mBatterylevel;
	}
	
	public void SetSignalKey(int signal){
		this.mSingnal = signal;
	}
	
	public float GetSignalKey(){
		return this.mSingnal;
	}
	
	public void SetDeviceTemp(int temp){
		this.mDeviceTemp = temp;
	}
	
	public float GetDeviceTemp(){
		return this.mDeviceTemp;
	}
	
	public void SetNetworkCost(int cost){
		this.mNetworkCost = cost;
	}
	
	public int GetNetworkCost(){
		return this.mNetworkCost;
	}
	
	public void SetContinuFailNum(int Num){
		this.mContinuFailNum = Num;
	}
	
	public int GetContinuFailNum(){
		return this.mContinuFailNum;
	}
	
	public void SetAlarmInterval(int interval){
		this.AlarmInterval = interval;
	}
	
	public int GetAlarmInterval(){
		return this.AlarmInterval;
	}
	/************************************************************************************/
}
