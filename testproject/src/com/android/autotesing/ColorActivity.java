/**
 * 
 */
package com.android.autotesing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.android.io.AutoTestIO;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/****************************************************************************
 * @author
 * 
 ****************************************************************************/
public class ColorActivity extends Activity {
	private boolean mIsRgb = false;
	private int mScreenWidth;
	private int mScreenHeight;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.color);
		DisplayMetrics dm;
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		mScreenWidth = dm.widthPixels; // 屏幕宽（px，如：480px）
		mScreenHeight = dm.heightPixels; // 屏幕高（px，如：800px）
		// 修改fb0权限
		AutoTestIO.execCommandAsRoot("chmod 777 /dev/graphics/fb0");

		// 初始化获取色值按钮
		Button button = (Button) this.findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mIsRgb = testRGB();
				Intent intent = new Intent();
				intent.putExtra("ISRGB", mIsRgb);
				intent.putExtra("SCREEN_WIDTH", mScreenWidth);
				intent.putExtra("SCREEN_HEIGHT", mScreenHeight);
				setResult(RESULT_OK, intent);
				finish();
			}
		});

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// mIsRgb = testRGB();
	}
	
	
	/****************************************************************************
	 * 获取手机当前状态，RGB、GBR以及屏幕分辨率等信息
	 * @return
	 ****************************************************************************/
	private boolean testRGB() {
		Bitmap btiMap = Bitmap.createBitmap(mScreenWidth, mScreenHeight,
				Bitmap.Config.ARGB_8888);
		FileInputStream inputStream = null;
		File file = new File("/dev/graphics/fb0");
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Toast.makeText(this, this.getString(R.string.retryinstall),
					Toast.LENGTH_LONG).show();
			return false;
		}
		try {
			byte[] byteArray = readInputStream(inputStream);

			ByteBuffer buffer = ByteBuffer.wrap(byteArray);
			btiMap.copyPixelsFromBuffer(buffer);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int pixel = btiMap.getPixel(mScreenWidth / 2, mScreenHeight / 2);

		int p = pixel;
		if (p == 0 || p == 0xff000000) {
			return false;
		} else {
			if (0x000000ff == (p & 0x000000ff)) {
				return false;
			} else {
				return true;
			}
		}
	}

	public byte[] readInputStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		while ((len = inStream.read(buffer)) != -1) {
			outSteam.write(buffer, 0, len);
		}
		outSteam.close();
		inStream.close();
		return outSteam.toByteArray();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
