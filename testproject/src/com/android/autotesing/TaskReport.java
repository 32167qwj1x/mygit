/**
 * 
 */
package com.android.autotesing;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预留
 * @author 
 *
 */
public class TaskReport {
	public String mTaskName;
	public String mTaskCode;
	public String mTaskDesc;
	public Date mStartDate;
	public Date mEndDate;
	public List<Case> mCaseList = new ArrayList<Case>();
	
	public class Case{
		public String mCaseName;
		public int mTotalNum;
		public int mErrorNum;
		public int mSuessceNum;
		public List<Item> mItemList = new ArrayList<Item>();
	}
	public class Item{
		public int mItemIndex;
		public int mCurErrorNum;
		public int mErrorCode;
	}
	
	public Case newCase(){
		return new Case();
	}
	
	public Item newItem(){
		return new Item();
	}

}
