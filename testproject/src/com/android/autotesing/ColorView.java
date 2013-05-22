package com.android.autotesing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/****************************************************************************
 * @author 
 *
 /****************************************************************************/
public class ColorView extends View{
	private int mScreenWidth;
	private int mScreenHeight;
	public ColorView(Context context){
		super(context);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		//canvas.drawColor(Color.RED);
		Paint p = new Paint();
		p.setColor(Color.RED);
		
		canvas.drawRect(0,0,mScreenWidth,mScreenHeight,p);
	}
	
	public void setScreensize(int w,int h){
		mScreenWidth = w;
		mScreenHeight = h;
	}
}
