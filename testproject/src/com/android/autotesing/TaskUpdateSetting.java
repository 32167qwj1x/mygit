package com.android.autotesing;



import java.util.List;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
/***************************************************************************
 * 任务更新时间设置
 * @author 
 *
 ***************************************************************************/
public class TaskUpdateSetting extends Activity{
	
	private Context mCxt;
	
	//更新设置输入框
	private EditText UpdateTimeInput;
	//是否启用更新
	private CheckBox UpdateCheck;
	//按钮控件
	private Button mConfirm;
	private Button mCancel;
	//数据库
	private DataBaseRestore DB;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mCxt = this;
		setContentView(R.layout.updatesetlayout);
		DB = DataBaseRestore.getInstance(mCxt);
		initialLayout();
	}
	
	/***************************************************************************
	 * 初始化控件
	 ***************************************************************************/
	private void initialLayout(){
		UpdateCheck = (CheckBox)findViewById(R.id.updatecheck);
		UpdateTimeInput = (EditText)findViewById(R.id.updatetimer);
		mConfirm = (Button)findViewById(R.id.Confirm);
		mCancel = (Button)findViewById(R.id.Cancel);
		
		String Updatetime = DB.getTaskUpdateTimeValue();
		
		if(Updatetime != null && "".equals(Updatetime) == false){
			UpdateCheck.setChecked(true);
		}else{
			UpdateCheck.setChecked(false);
		}
		if(UpdateCheck.isChecked()){
			UpdateTimeInput.setText(Updatetime);
		}
		UpdateCheck.setOnClickListener(new CheckBox.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
//				if(UpdateCheck.isChecked()){
//					is_checked = true;
////					DB.SetTaskUpdateTimeValue(UpdateTimeInput.getText().toString());
//				}else{
//					is_checked = false;
////					DB.SetTaskUpdateTimeValue("");
//				}
			}
				// TODO Auto-generated method stub
			});
			
		mConfirm.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View view){
        		if(UpdateCheck.isChecked()){
        			DB.SetTaskUpdateTimeValue(UpdateTimeInput.getText().toString());
        		}else{
        			DB.SetTaskUpdateTimeValue("");
        		}
        		StartUpdateService(mCxt);
        		//test程序
//        		if(CheckIfAppExistInSystem("autotest") == true){
//        			Toast.makeText(mCxt,"APP exist",
//     					 Toast.LENGTH_SHORT).show();
//        		}else{
//        			Toast.makeText(mCxt,"APP not exist===========",
//        					 Toast.LENGTH_SHORT).show();
//        		}
        		finish();
        	}
        });
		mCancel.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View view){
        		finish();
        	}
        });
	}
	
	 private void StartUpdateService(Context context){
		 	if(IsServiceRunning("DataUpdateService")){
		 		Intent intent;
			    intent = new Intent("android.intent.action.UPDATE_TASK_ALARM");
			    sendBroadcast(intent);
		 	}
			
	    }
	 
	 private  boolean IsServiceRunning(String ServiceName){
			ActivityManager mActivityManager = 
				(ActivityManager)getSystemService(ACTIVITY_SERVICE);  
			List<ActivityManager.RunningServiceInfo> serviceList  
	       = mActivityManager.getRunningServices(30); 
			if ((serviceList.size()>0)) { 
				 for (int i=0; i<serviceList.size(); i++) { 
		               if (serviceList.get(i).service.getClassName().contains(ServiceName) == true) { 
		            	   return true; 
		               } 
		           } 
	        }
			return false;
		}
}
