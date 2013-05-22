/**
 * 
 */
package com.android.autotesing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

/**
 * @author Activity
 *
 */
public class AboutActivity extends Activity{
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.about);
		TextView tv = (TextView)this.findViewById(R.id.task_version);
		TextView jarv = (TextView)this.findViewById(R.id.jar_version);
		Bundle bundle = getIntent().getExtras();
		if(bundle != null){
			tv.setText("任务端版本:" + bundle.getString("TV"));
			String str = bundle.getString("VERSION");
			if(str != null && str.length() > 0){
				jarv.setText("执行端版本:" + str);
			}else {
				jarv.setText("执行端版本:" + this.getString(R.string.startexec));
			}
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
	    	finish();
	    	return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
