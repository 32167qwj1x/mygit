package com.android.autotesing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
/********************************************************************************
 * 用户信息验证
 * @author 
 *
 */
public class UserCertificateActivity extends Activity {
	//控件
	private EditText userInput;
	private EditText pwInput;
	private Button mConfirm;
	private Button mCancel;
	private Context mContext;
	
	//数据库
	private DataBaseRestore mDBRecord;
	private SharedPreferences sp;
	
	//关键字
	private String KEY_NAME = "name";
	private String KEY_PW = "pw";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.authinput);
		mContext = this;
		initialLayout();
		initialConfig();
	}

	private void finishApp() {
		finish();
	}

	/********************************************************************************
	 * 根据当前设备信息完整度，选择进入主任务列表还是设备信息编辑界面
	 ********************************************************************************/
	private void entryMainApp() {
		Intent it = new Intent();

		if (mDBRecord.isDevInfoEmpty() == false) {
			it.setClass(mContext, AutoTestingActivity.class);
		} else {
			it.setClass(mContext, DevInfoInputActivity.class);
		}
		mContext.startActivity(it);
		finishApp();
	}

	/********************************************************************************
	 * 初始化控件
	 ********************************************************************************/
	private void initialLayout() {
		userInput = (EditText) findViewById(R.id.userinput);
		pwInput = (EditText) findViewById(R.id.pwinput);
		mConfirm = (Button) findViewById(R.id.confirm);
		mCancel = (Button) findViewById(R.id.cancel);

		mConfirm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean mCertify = certifyUserInfo(userInput.getText()
						.toString().trim(), pwInput.getText().toString().trim());

				if (mCertify == true) {
					entryMainApp();
				} else {
					Toast.makeText(mContext, "用户名密码错误", Toast.LENGTH_SHORT)
							.show();

				}
			}
		});

		mCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				finishApp();
			}
		});
	}

	/********************************************************************************
	 * 目前用户名密码是写死的。
	 * user：test
	 * pwd:123
	 * 预留后面从数据库文件中读取
	 ********************************************************************************/
	private void initialConfig() {
		sp = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
		mDBRecord = DataBaseRestore.getInstance(mContext);

		String test = sp.getString(KEY_NAME, null);
		if (test == null) {
			Editor editor = sp.edit();
			editor.putString(KEY_NAME, "test");
			editor.putString(KEY_PW, "123");
			editor.commit();
		}

	}

	private boolean certifyUserInfo(String user, String pw) {
		boolean ret = false;
		String userString, pwString;
		userString = sp.getString(KEY_NAME, "");
		pwString = sp.getString(KEY_PW, "");
		if (user.equals(userString) && pw.equals(pwString)) {
			ret = true;
		}

		return ret;
	}
}
