package com.android.autotesing;

import java.util.List;

import com.android.ftp.AutoTestFtpIO;
import com.android.http.AutoTestHttpIO;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DevInfoInputActivity extends Activity {
	private Context mContext;
	//数据库
	private DataBaseRestore mDBRecord;
	
	//输入框控件
	private EditText mDevNameInput;
	private EditText mCityCodeInput;
	private EditText mProvCodeInput;
	private EditText mDeviceTypeInput;
	private EditText mServerAdressInput;
	private EditText mServerPasswordInput;
	private EditText mPhoneNumberInput;
	private EditText mSmsFilterInput;
	
	//按钮控件
	private Button mConfirm;
	private Button mCancel;
	
	//对话框
	private Dialog mProgressDialog = null;
	private Dialoghandler mDialoghandler;
	
	//字串关键值
	String sDevN;
	String sCCode;
	String sPCode;
	String sDevT;
	String sServerAdress;
	String sServerPassword;
	String sPhoneNumber;
	String sSmsFilter;

	//关闭对话框
	private static final int DIALOG_DISMISS_ACTION = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.deviceinfoinput);
		mContext = this;
		//初始化控件
		initLayout();
		mDialoghandler = new Dialoghandler();
	}
	
	/********************************************************************************
	 * 控件初始化
	 * @return
	 ********************************************************************************/
	private boolean initLayout() {

		mDevNameInput = (EditText) findViewById(R.id.dNameInput);
		mCityCodeInput = (EditText) findViewById(R.id.dCityCodeinput);
		mProvCodeInput = (EditText) findViewById(R.id.dProvCodeInput);
		mDeviceTypeInput = (EditText) findViewById(R.id.dDevTypeInput);
		mServerAdressInput = (EditText) findViewById(R.id.dServerAdressInput);
		mServerPasswordInput = (EditText) findViewById(R.id.dServerPassword);
		mPhoneNumberInput = (EditText) findViewById(R.id.dPhoneNumberInput);
		mSmsFilterInput = (EditText) findViewById(R.id.dSmsFilterInput);
		mConfirm = (Button) findViewById(R.id.DevConfirm);
		mCancel = (Button) findViewById(R.id.DevCancel);

		mDBRecord = DataBaseRestore.getInstance(mContext);
		// success = DataBaseRestore.InitializeDevDB(mContext);

		if ("".equals(mDBRecord.mDevName) == false) {
			mDevNameInput.setText(mDBRecord.mDevName);
		}
		if ("".equals(mDBRecord.mCityCode) == false) {
			mCityCodeInput.setText(mDBRecord.mCityCode);
		}
		if ("".equals(mDBRecord.mProvCode) == false) {
			mProvCodeInput.setText(mDBRecord.mProvCode);
		}
		if ("".equals(mDBRecord.mDevType) == false) {
			mDeviceTypeInput.setText(mDBRecord.mDevType);
		}
		if ("".equals(mDBRecord.mServerAdress) == false) {
			mServerAdressInput.setText(mDBRecord.mServerAdress);
		} else {
			mServerAdressInput.setText(AutoTestFtpIO.FTPURL);
		}
		if ("".equals(mDBRecord.mServerPassWord) == false) {
			mServerPasswordInput.setText(mDBRecord.mServerPassWord);
		} else {
			mServerPasswordInput.setText(AutoTestFtpIO.FTPPass);
		}
		
		//如未初始化本机号码，则读取Telephony内手机号码
		if ("".equals(mDBRecord.mPhoneNumber) == false) {
			mPhoneNumberInput.setText(mDBRecord.mPhoneNumber);
		} else {
			String MSIDN;
			TelephonyManager tm = (TelephonyManager) this
					.getSystemService(TELEPHONY_SERVICE);
			MSIDN = tm.getLine1Number();
			mPhoneNumberInput.setText(MSIDN);
		}

		if ("".equals(mDBRecord.mSmsFilter) == false) {
			mSmsFilterInput.setText(mDBRecord.mSmsFilter);
		}

		//点击确认，将设备信息发送给服务器注册
		mConfirm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				sDevN = mDevNameInput.getText().toString();
				sCCode = mCityCodeInput.getText().toString();
				sPCode = mProvCodeInput.getText().toString();
				sDevT = mDeviceTypeInput.getText().toString();
				sPhoneNumber = mPhoneNumberInput.getText().toString();
				sServerAdress = mServerAdressInput.getText().toString();
				sServerPassword = mServerPasswordInput.getText().toString();
				sSmsFilter = mSmsFilterInput.getText().toString();

				//如信息不全，不让注册
				if ("".equals(sPCode) || "".equals(sCCode) || "".equals(sDevT)
						|| "".equals(sServerAdress)
						|| "".equals(sServerPassword)
						|| "".equals(sPhoneNumber)) {
					Toast.makeText(mContext, R.string.nofullinput,
							Toast.LENGTH_SHORT).show();
				} else {
					mDBRecord.SetDevNameDBValue(sDevN);
					mDBRecord.SetCityCodeDBValue(sCCode);
					mDBRecord.SetProvCodeDBValue(sPCode);
					mDBRecord.SetDevTypeDBValue(sDevT);
					mDBRecord.SetServerAdressValue(sServerAdress);
					mDBRecord.SetServerPasswordValue(sServerPassword);
					mDBRecord.SetPhoneNumValue(sPhoneNumber);
					mDBRecord.SetSmsFilterNumValue(sSmsFilter);
					showProgressbar();
					//异步注册
					new resThread().start();

				}

			}
		});
		mCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				finish();
			}
		});

		return true;
	}

	/********************************************************************************
	 * 开始注册设备信息
	 * @param DevType
	 * @param DevName
	 * @param ProvCode
	 * @param CityCode
	 * @param Phonenum
	 * @return
	 ********************************************************************************/
	private boolean StartRegistDevice(String DevType, String DevName,
			String ProvCode, String CityCode, String Phonenum) {
		String ret = "";
		String DevID;
		String SfVersion;

		TelephonyManager tm = (TelephonyManager) this
				.getSystemService(TELEPHONY_SERVICE);
		DevID = tm.getDeviceId();
		SfVersion = tm.getDeviceSoftwareVersion();

		if (DevID == null || "".equals(DevID)) {
			DevID = "11111111111119";
		}

		if (SfVersion == null) {
			SfVersion = "";
		}
		//用户名默认“nbpt”
		ret = AutoTestHttpIO.DeviceRegister(DevID, DevType, DevName, ProvCode,
				CityCode, SfVersion, "nbpt", "00", Phonenum);
		if ("0000".equals(ret)) {
			if (IsServiceRunning("TaskService") == false) {
				mDBRecord.SetDevStateDBValue(mContext, "03");
			}
			return true;
		} else {
			return false;
		}
	}

	/********************************************************************************
	 *注册成功后进入主任务列表
	 */
	private void entryMainApp() {
		Intent it = new Intent();
		it.setClass(mContext, AutoTestingActivity.class);
		mContext.startActivity(it);
		finish();
	}

	private void showProgressbar() {
		if (mProgressDialog == null) {
			mProgressDialog = new Dialog(this);
			mProgressDialog.setContentView(R.layout.loadprogressbar);
			mProgressDialog.setCancelable(false);
			TextView tv = (TextView) mProgressDialog.findViewById(R.id.text);
			tv.setText(R.string.devreging);

			mProgressDialog.show();
		}
	}

	private class resThread extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			boolean ResSucess;
			int arg;
			ResSucess = StartRegistDevice(sDevT, sDevN, sPCode, sCCode,
					sPhoneNumber);
			if (ResSucess) {
				arg = 1;
			} else {
				arg = 0;
			}
			Message msg = new Message();
			msg.what = DIALOG_DISMISS_ACTION;
			msg.arg1 = arg;
			mDialoghandler.sendMessage(msg);

		}
	}

	private class Dialoghandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case DIALOG_DISMISS_ACTION: {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
				}
				if (msg.arg1 == 1) {
					Toast.makeText(mContext, R.string.devregsucess,
							Toast.LENGTH_SHORT).show();
					entryMainApp();
				} else {
					Toast.makeText(mContext, R.string.devregfail,
							Toast.LENGTH_SHORT).show();
				}

				break;
			}
			default:
				break;
			}
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
}
