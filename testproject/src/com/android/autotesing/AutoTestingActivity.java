package com.android.autotesing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.android.ftp.AutoTestFtpIO;
import com.android.io.AutoTestIO;
import com.android.io.Report;
import com.android.xmlParse.TaskInfo;
import com.android.xmlParse.xmlParse;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class AutoTestingActivity extends ListActivity {
	/*APK端版本号信息*/
	private static String VERSION = "1.3.0-4.0";
	
	/*log输出过滤*/
	private  static String TAG = "AutoTestingActivity";
	
	/*任务执行服务进程启动命令*/
	private static final String SERVICE_ACTION = "com.android.autotesing.START_TASK_SERVICE";
	
	/*数据服务进程启动命令*/
	private static final String DATAUPDATE_SERVICE = "com.android.autotesing.DATA_UPDATE_SERVICE_START_ACTION";
	
	/*当自动更新启动时，更新完成后数据服务进程会发送此广播来更新界面*/
	private static final String REFRESH_FROM_SERVICE = "REFRESH_ACTIVITY_FROM_DATA_SERVICE";
	
	/*用户设备状态*/
	private final String DEV_STATE_BUSY = "01";
	
	/*任务执行状态*/
	private final int TASK_READY = -1;
	private final int TASK_RUNNING = 0;
	private final int TASK_PENDING = 1;
	private final int TASK_BGRUNNING = 2;
	private final int TASK_FINISHED = 3;
	
	/*更新进度Dialog类型*/
	private final int DIALOG_TYPE_UPDATESETTING = 0;
	private final int DIALOG_TYPE_BLOCKNUMBERINPUT = 1;
	
	//是否手动选择任务程序
	private final boolean select_byuser = false;
	
	/*重置设备信息菜单*/
	private static final int OPTMENUID_RESETDEVINFO =Menu.FIRST;
	/*更新任务菜单*/
	private static final int OPTMENUID_REFRESHTASK = Menu.FIRST+1;
	/*设置任务更新时间间隔菜单*/
	private static final int OPTMENUID_SETDURATION =Menu.FIRST+2;
	/*获取对比色值*/
	public static final int ITEM0 = Menu.FIRST+3;
	/*设备状态更新时间间隔设置菜单*/
	private static final int OPTMENUID_DEVUPDATESETTING = Menu.FIRST+4;
	/*退出应用程序菜单*/
	private static final int OPTMENUID_QUITAPP = Menu.FIRST+5;
	/*关闭底层执行.jar程序菜单*/
	private static final int OPTMENUID_KILL_EXE = Menu.FIRST+6;
	/*手动上传测试结果菜单*/
	private static final int OPTMENUID_UPLOADREPORT =Menu.FIRST+7;
	/*预留*/
	private static final int OPTMENUID_BLOCKINCOMINGCALL =Menu.FIRST+8;
	/*彻底清除数据库菜单*/
	private static final int OPTMENUID_DELETEDB =Menu.FIRST+9;
	/*版本信息菜单*/
	public static final int ITEM3 = Menu.FIRST+10;
	
	/*进度线程控制标记*/
	private static final int DIALOG_DISMISS_ACTION = 1;
	private static final int DIALOG_UPDATE_TASK_FAIL = -1;
	private static final int DIALOG_UPDATE_SCRIPT_FAIL = -2;
	private static final int DIALOG_UPLOAD_SUCCESS = -3;
    /** Called when the activity is first created. */
	
	
	/*列表控件*/
	private ListView mListview;
	/*解析文件名称*/
	private TextView mTaskFile;
	/*添加button空间列表*/
	private Button mAddTaskBnt;
	private Button mAllBnt;
	private Context mCxt;
	private String mTaskfilename = null;
	private View mFooterView;
	private View mHeaderView;
	private TaskService mTaskService;
	private ArrayList<TaskInfo> mTestTaskList = new ArrayList<TaskInfo>();
	private List<Map<String, String>> mUIlist = new ArrayList<Map<String, String>>();
	private List<ToggleButton> mTogglenList = new ArrayList<ToggleButton>();
	private ArrayList<TaskInfo> mRunningList = new ArrayList<TaskInfo>();
	private TaskRecord mTaskRecord;
	private Intent mIntent;
	private Dialoghandler mDialoghandler;  
	private TextView mTip;
	private int mW = 0;
	private Dialog mProgressDialog = null;
	private int dialogType = -1;
	private int count = 0;
	private boolean mIsshow = true;
	private boolean mVerget = false;
	private ProgressDialog mChecProgressDialog;
	private VersionHandle mReceiver;
	
	/*************************************************
     * 创建Activity，初始化控件，接收器以及数据库
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCxt = this;
        resetList();
        
        /*初始化数据库*/
        mTaskRecord = new TaskRecord(this);
        mTaskRecord.getSharedPreferences("TASK_INFO");
        setContentView(R.layout.main);
        
        /*初始化列表控件*/
    	mListview = this.getListView();
        LayoutInflater inflater = LayoutInflater.from(this);
        mFooterView = inflater.inflate(R.layout.tailview, mListview, false);
        mHeaderView = inflater.inflate(R.layout.headview, mListview, false);
        mListview.addHeaderView(mHeaderView, null, true);
        TaskListAdapter adapter = new TaskListAdapter(this);
        setListAdapter(adapter); 
        mTaskFile = (TextView)this.findViewById(R.id.filename);
        mAddTaskBnt = (Button)this.findViewById(R.id.select_bnt);
        mTip = (TextView)this.findViewById(R.id.tip_text);
        mDialoghandler = new Dialoghandler();
        
        mAddTaskBnt.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View view){
        		if(select_byuser){
            		Intent it = new Intent();
            		it.setClass(mCxt, FileExplorerActivity.class);
            		mCxt.startActivity(it);//startActivityForResult(it,11);
        			finish();	
        		}else{
        			//连接服务器
        			startGetNewTask();
        		}

        	}
        });
        
		/*如已经获取屏幕信息，不再显示提示*/
		if(mTaskRecord.getInt("SCREEN_WIDTH") > 0){
			if(mTip != null){
				mTip.setVisibility(View.GONE);
			}
    	}
		
		/*调试函数*/
		/******************************************************/
        if(!select_byuser){
             TaskFileParse();   	
        }
        if(select_byuser){
    	Bundle bundle = getIntent().getExtras();
    	if(bundle != null){
    		mTaskfilename = bundle.getString("xmlname");
	    	if(mTaskfilename.length() > 0){
	    		mTaskRecord.putString("TASKNAME", mTaskfilename);
//	    		mTaskRecord.saveTaskFilename(mTaskfilename);
	    		resetList();
	    		if(parseXmlFile(mTaskfilename)){
		    		mTaskFile.setText(mTaskfilename);
		    		mAddTaskBnt.setText(mCxt.getString(R.string.replacestr));
		    		appendFooterView();
	    			testTaskListSort();
	    			//Print();
					mTip.setVisibility(View.GONE);
	    		}
	    	}
    	}
        }
        /******************************************************/

        /*初始化广播接收器*/
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.autotesing.EXEC_TO_TASK");
        filter.addAction(REFRESH_FROM_SERVICE);
        mReceiver = new VersionHandle();
        this.registerReceiver(mReceiver, filter);
        
        /*进入主任务列表，与底层执行程序握手，如未启动底层程序，则不允许执行任务*/
		Intent it = new Intent("com.android.autotesing.TASK_TO_EXEC");
		it.putExtra("TASK_SEND_ACK", "TASK_SEND_ACK_STATE_REQ");
		this.sendBroadcast(it);
        initProgressDialog();
        new Thread(){   
            public void run(){   
                try{   
                	//重试50次，如50次未启动成功则退出
                    while(count <= 50 && mIsshow){
                    	StartJarService(mCxt);
                        count ++;
                        Thread.sleep(100);
                    }
        			Message message = new Message();
        			message.what = 1;
        			handler.sendMessage(message);
                }catch(Exception ex){
                	ex.printStackTrace();
                	mChecProgressDialog.dismiss();   
                }   
            }
        }.start();
        if(mIsshow){
        	mChecProgressDialog.show();
        }
    }
    	
    
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if(mReceiver != null && mCxt != null){
			mCxt.unregisterReceiver(mReceiver);
		}
		
		super.onDestroy();
	}
	/*************************************************
     * 进度弹出窗口处理Hanle
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
		// TODO Auto-generated method stub
			switch(msg.what){
				case 1:{
					mChecProgressDialog.dismiss();
					if(mIsshow){
						showDialogEx(0);
					}
				}
				break;
				case 2:{
					  mChecProgressDialog.cancel();
					  if(mVerget){
						  return;
					  }
					  Intent it = new Intent(AutoTestingActivity.this,AboutActivity.class);
					  it.putExtra("VERSION","");
					  it.putExtra("TV", VERSION);
					  it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					  startActivity(it);
				}
				break;
			}
			super.handleMessage(msg);
		}
	};

    /*************************************************
     * 初始化底层服务程序检查进度查询
     * 
     *************************************************/
	private void initProgressDialog(){
        mChecProgressDialog = new ProgressDialog(this);   
        mChecProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);//设置风格为圆形进度条   
        mChecProgressDialog.setTitle("正在开启后台服务");//设置标题   
        mChecProgressDialog.setMessage("请稍侯...");   
        mChecProgressDialog.setIndeterminate(false);//设置进度条是否为不明确   
        mChecProgressDialog.setCancelable(false);//设置进度条是否可以按退回键取消  
	}
	
	
    /*************************************************
     * 重置任务列表
     * 
     *************************************************/
    private void resetList(){
    	mTestTaskList.clear();
    	mUIlist.clear();
    	mRunningList.clear();
    }
    
    
	/*************************************************
     * 描述：进度窗口按键处理
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
	class DialogOnKeyListener implements OnKeyListener {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_UP
					&& keyCode == KeyEvent.KEYCODE_BACK) {
				finish();
				return true;
			}
			if (event.getAction() == KeyEvent.ACTION_UP
					&& keyCode == KeyEvent.KEYCODE_HOME) {
				return false;
			}
			return false;
		}
	}
    
	/*************************************************
     * 描述：退出窗口显示
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
	private void showDialogEx(int id) {
		AlertDialog builder = new AlertDialog.Builder(AutoTestingActivity.this)
				.setIcon(R.drawable.alert_dialog_icon).setTitle("提示")
				.setMessage("请开启执行端进行任务操作！").setOnKeyListener(
						new DialogOnKeyListener()).create();
		builder.show();
		builder.getWindow().setType(
				WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
	}
    
	/*************************************************
     * 描述：任务列表排序
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
	private void testTaskListSort() {
		Iterator<TaskInfo> it = mTestTaskList.iterator();
		while (it.hasNext()) {
			Map<String, String> item = new HashMap<String, String>();
			TaskInfo task = it.next();
			item.put("title", task.getTestTaskName());
			item.put("detail", task.getTestTaskDesc());
			mUIlist.add(item);
		}
	}
	/*************************************************
     * 描述：任务开始按钮处理
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
	private void appendFooterView() {
		mListview.removeFooterView(mFooterView);
		mListview.addFooterView(mFooterView, null, true);
		mAllBnt = (Button) this.findViewById(R.id.allbnt);
		if (mAllBnt != null) {
			mAllBnt.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					if (mRunningList.size() > 0) {
						if (mTaskRecord != null) {
							int with = mTaskRecord.getInt("SCREEN_WIDTH");
							int height = mTaskRecord.getInt("SCREEN_HEIGHT");
							if (with == 0 && height == 0) {
								Toast.makeText(mCxt, R.string.chedkdevicealarm,
										Toast.LENGTH_SHORT).show();
								return;
							}
						}
						/*任务为空，或case不存在时不允许启动*/
						if (CheckTasksIsCanUse() == false) {
							Toast.makeText(mCxt, R.string.EmptyCaseInclude,
									Toast.LENGTH_SHORT).show();
							return;
						}

						mAllBnt.setText(R.string.allstop);
						if (IsServiceRunning("TaskService")) {
							StopTaskService();
						}
						StartTaskService();
						finish();
					} else {
						mTaskRecord.putString("TASKLIST", "");
						Toast.makeText(mCxt,
								mCxt.getString(R.string.taskstarttip),
								Toast.LENGTH_LONG).show();
					}
				}
			});
		}
	}
        
	/*************************************************
     * 描述：解析任务列表文件
     * 出参：
     * 入参：Task列表文件全路径
     * 返回值：解析结果
     *************************************************/
    private boolean parseXmlFile(String filename){
		File tskfile = new File(filename);
		xmlParse p = new xmlParse();
		InputStream in = null;
		if(!tskfile.exists()){
			showInfo(mCxt.getString(R.string.taskfileerror));
			return false;
		}
		try{
			in = new BufferedInputStream(new FileInputStream(tskfile));
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
		try{
			String list[] = mTaskRecord.getString("TASKLIST").split("\\|");
			List<TaskInfo> tskall;
			tskall = AutoTestIO.getTask();
			
    		Iterator<TaskInfo> it = tskall.iterator();
    		while (it.hasNext()) {
    			TaskInfo task = it.next();
    			task.setTaskPath(tskfile.getParent());
    			Log.i(TAG, "  ");
    			task.mTaskState = TASK_READY;
    			for(int i = 0; i < list.length; i ++){
    				if(task.getTestTaskCode().equals(list[i].split("\\#")[0])){
    					//设置后台启动状态，以便用户知道哪些任务已经处于开启状态
    					task.mTaskState = TASK_BGRUNNING;
    					addRunningTaskList(task);
    					break;
    				}
    			}
    			mTestTaskList.add(task);
    		}
		}catch(Exception e){
			Log.i(TAG, "Exception");
			e.printStackTrace();
			showInfo(mCxt.getString(R.string.parseerror));
		}
		if(mTestTaskList.size() > 0){
			return true;
		}
		
		return false;
    }
    
    
    
    public void showInfo(String str){
    	new AlertDialog.Builder(this).setTitle(mCxt.getString(R.string.app_name)).setMessage(str).
    		setPositiveButton(mCxt.getString(R.string.ok), new DialogInterface.OnClickListener() {
    	@Override
    	public void onClick(DialogInterface dialog, int which) {
    	}
    	}).show();
    } 
    
    
    /*************************************************
     * 描述：获取Button信息
     * 出参：
     * 入参：button控件
     * 返回值：index
     *************************************************/
    private int getTogglenBntIndex(CompoundButton bnt){
    	int index = 0;
		Iterator<ToggleButton> it = mTogglenList.iterator();
		Log.i(TAG, "bnt.text = "+bnt.getText());
		while (it.hasNext()){
			ToggleButton togbnts = it.next();
			Log.i(TAG, "ogbnts.togbnt.text = "+togbnts.getText());
			if(togbnts == bnt){
				Log.i(TAG, "getTogglenBntIndex index = "+index);
				return index;
			}
			index ++;
		}
		Log.i(TAG, "getTogglenBntIndex return -1");
    	return -1;
    }
    
    private TaskInfo getRunningTask(int index){
    	if(index >= 0){
    		return mTestTaskList.get(index);
    	}
    	return null;
    }
    
    /*************************************************
     * 描述：启动后台执行服务，将用户选择的任务列表通过“TASKLIST”
     * 		传入
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
    private void StartTaskService(){
		String taskList="";
		int index = 0;
		while(index < mRunningList.size()){
			TaskInfo task = mRunningList.get(index);
			taskList += (task.getTestTaskCode()+"#" + task.getTestTaskVersion() + "|");
			index ++;
		}
		if(taskList.length() > 1){
			mTaskRecord.putString("TASKLIST",taskList);
		}
		Intent it = new Intent(SERVICE_ACTION);
		it.putParcelableArrayListExtra("RUNNINGLIST",mRunningList);
        startService(it);
    }
    
    /*************************************************
     * 描述：停止后台执行服务
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
    private void StopTaskService(){
    	Intent it = new Intent(SERVICE_ACTION);//SERVICE_ACTION
    	this.stopService(it);
    }
    
    
    private void addRunningTaskList(TaskInfo task){
    	if(task != null){
    		Log.i(TAG,"##addRunningTaskList : "+task.getTestTaskName());
    		Log.i(TAG,"##mRunningList.size() before add: "+mRunningList.size());
    		if(mRunningList.contains(task) == false){
    			mRunningList.add(task);
    		}else{
    			Log.i(TAG,"##addRunningTaskList "+task.getTestTaskName()+"already exist");
    		}
    		Log.i(TAG,"##mRunningList.size() after add: "+mRunningList.size());
    		
    	}
    }
    
    private void removeTasktoRuningList(TaskInfo task){
    	if(task != null){
    		Log.i(TAG,"##removeTasktoRuningList : "+task.getTestTaskName());
    		Log.i(TAG,"##mRunningList.size() before remove: "+mRunningList.size());
    		if(mRunningList.contains(task) == true){
    			mRunningList.remove(task);
    		}else{
    			Log.i(TAG,"##removeTasktoRuningList "+task.getTestTaskName()+"not exist");
    		}
    		Log.i(TAG,"##mRunningList.size() after remove: "+mRunningList.size());
    	}
    }
    
    
    /*************************************************
     * 描述：列表控件处理内部类，负责列表控件的处理和初始化工作
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
    public class TaskListAdapter extends BaseAdapter{
    	private LayoutInflater mInflater;
    	public boolean mChecked = false;
    	
    	public TaskListAdapter(Context context){
    		this.mInflater = LayoutInflater.from(context);
    	}

    	@Override
    	public int getCount() {
    		// TODO Auto-generated method stub
    		return mUIlist.size();
    	}

    	@Override
    	public Object getItem(int arg0) {
    		// TODO Auto-generated method stub
    		return null;
    	}

    	@Override
    	public long getItemId(int arg0) {
    		// TODO Auto-generated method stub
    		return 0;
    	}
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		ListItem holder = null;
    		
    	if (convertView == null) {
	    	holder=new ListItem();
	    	convertView = mInflater.inflate(R.layout.listitem, null);
	    	holder.text1 = (TextView)convertView.findViewById(R.id.text1111);
	    	holder.text2 = (TextView)convertView.findViewById(R.id.text2222);
	    	holder.toggleBtn = (ToggleButton)convertView.findViewById(R.id.togglebnt);

	    	Log.i(TAG,"## mTestTaskList.get(position).mTaskState = "+mTestTaskList.get(position).mTaskState
	    			+ "position = "+position);
	    	if(mTestTaskList.get(position).mTaskState == TASK_BGRUNNING){
	    		Log.i(TAG,"## toggle button check true");
	    		holder.toggleBtn.setChecked(true);
	    	}else{
	    		Log.i(TAG,"## toggle button check false");
	    		holder.toggleBtn.setChecked(false);
	    	}
	    	
	    	if(mTogglenList.size() < mUIlist.size()){
	    		mTogglenList.add(holder.toggleBtn);
	    		Log.i(TAG,"## mTogglenList.add(tog)");
	    	}
	    	convertView.setTag(holder);
	    	
    	}else {
    		holder = (ListItem)convertView.getTag();
    	}
    	String title = (String)mUIlist.get(position).get("title");
    	String detail = (String)mUIlist.get(position).get("detail");
    	if(title != null){
    		holder.text1.setText((String)mUIlist.get(position).get("title"));
    	}
    	if(detail != null && detail.equals("") == false){
    		holder.text2.setText((String)mUIlist.get(position).get("detail"));
    	}
    	final int p = position;  
    	Log.i(TAG,"##p = "+p);
    	
    	//初始化Tag button控件
    	holder.toggleBtn.setOnCheckedChangeListener(new OnCheckedChangeListener(){
    	   @Override  		
    	   public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
    		   TaskInfo item = getRunningTask(getTogglenBntIndex(buttonView));
    		   if(item != null){
	    		   if(isChecked){
	    			   //add
	    			   Log.i(TAG,"##addRunningTaskList ");
	    			   addRunningTaskList(item);
	    		   }else {
	    			   //cancel
	    			   Log.i(TAG,"##removeTasktoRuningList ");
	    			   item.mTaskState = TASK_FINISHED;
	    			   removeTasktoRuningList(item);
	    		   }
    		   }
    	   }
    	});

    	return convertView;
    	} 
    }
    
    public static class ComparatorTestTask implements Comparator<TaskInfo> {
    	
    	@Override
        public int compare(TaskInfo task1, TaskInfo task2) {
    		int result=0;
			 if(task1 != null && task2 != null)
			 {
				 boolean d = (task1.getStartDate().before(task2.getStartDate()));
				 
				 if(d){
					 result = 1;
				 }else {
					 result = -1;
				 }
			 }            
            return result;
        } 
    } 
    
    
	//list item 
	public final class ListItem{
		public TextView text1;
		public TextView text2;
		public ToggleButton toggleBtn;
	}

	
    private void log(String msg){
    	Log.i(TAG, msg);
    }
    @Override
	protected void onActivityResult(int requestCode, int resultCode,Intent intent){
		Bundle bundle = intent.getExtras();
		if(requestCode == 10){
			if(resultCode == RESULT_OK){
				if(intent != null){
					if(mTip != null){
						mTip.setVisibility(View.GONE);
					}
					boolean rgb = bundle.getBoolean("ISRGB");
					int width = bundle.getInt("SCREEN_WIDTH");
					int height = bundle.getInt("SCREEN_HEIGHT");
					mTaskRecord.putBoolean("ISRGB",rgb);
					mTaskRecord.putInt("SCREEN_WIDTH", width);
					mTaskRecord.putInt("SCREEN_HEIGHT", height);
				}
			}
		}else if(requestCode == 11){
			if(resultCode == 1){
		    	if(bundle != null){
		    		mTaskfilename = bundle.getString("xmlname");
			    	if(mTaskfilename.length() > 0){
			    		mTaskRecord.putString("TASKNAME", mTaskfilename);
			    		if(parseXmlFile(mTaskfilename)){
				    		mTaskFile.setText(mTaskfilename);
				    		mAddTaskBnt.setText(mCxt.getString(R.string.replacestr));
				    		appendFooterView();
			    			testTaskListSort();
//			    			Print();
			    			mTip.setVisibility(View.GONE);
			    		}
			    	}
		    	}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}
    
    
    /*************************************************
     * 描述：广播接收器，接收来自底层执行端的握手消息和其他服务的
     * 		界面更新广播。
     * 出参：
     * 入参：
     * 返回值：
     *************************************************/
	public class VersionHandle extends BroadcastReceiver{
		  @Override
		  public void onReceive(Context context, Intent intent){
			  if(intent.getAction().equals("com.android.autotesing.EXEC_TO_TASK")){
				  if(intent.getStringExtra("TASK_SEND_ACK").equals("EXEC_VERSION_RSP")){
					  mChecProgressDialog.cancel();
					  mVerget = true;
					  String version = intent.getStringExtra("EXEC_VERSION_DATA");
					  Intent it = new Intent(context,AboutActivity.class);
					  it.putExtra("VERSION",version);
					  it.putExtra("TV", VERSION);
					  it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					  context.startActivity(it);
				  }
				  if(intent.getStringExtra("TASK_SEND_ACK").equals("TASK_SEND_ACK_STATE_RSP")){
					  mChecProgressDialog.cancel();
					  mIsshow = false;
				  }
			  }else if(intent.getAction().equals(REFRESH_FROM_SERVICE)){
				  //刷新列表
				  if(ParseDownloadTask()){
						appendFooterView();
						if(mListview != null){
							  LOG.Log(LOG.LOG_I,"mListview.invalidate();");
							  mListview.invalidate(); 
						  }
					}
				  LOG.Log(LOG.LOG_I,"restartActivity update UI");
			  }
		  }
	}

	/*************************************************
     * 描述：创建Menu菜单
     * 出参：
     * 入参：Menu
     * 返回值：
     *************************************************/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		
		menu.add(1, OPTMENUID_RESETDEVINFO, 0, getString(R.string.resetdev));
		menu.add(1, OPTMENUID_REFRESHTASK, 0, getString(R.string.refreshtask));
		menu.add(2, OPTMENUID_SETDURATION, 0, getString(R.string.setduration));
		menu.add(2, OPTMENUID_DEVUPDATESETTING, 0, getString(R.string.stupsetting));
		menu.add(2, OPTMENUID_QUITAPP, 0, getString(R.string.quitapplication));
		menu.add(0, OPTMENUID_KILL_EXE, 0 , R.string.closeexecution);
		menu.add(0, ITEM0, 0, "获取对比色值");
		menu.add(2, OPTMENUID_UPLOADREPORT, 0, getString(R.string.reportupload));
		menu.add(2, OPTMENUID_DELETEDB, 0, getString(R.string.deletedatabse));
		//如需block电话号码，可以打开此菜单
//		menu.add(2, OPTMENUID_BLOCKINCOMINGCALL, 0, getString(R.string.blockincomingcall));
	    menu.add(0, ITEM3, 0, "关于");
		return super.onCreateOptionsMenu(menu);
	}

	
	/*************************************************
     * 描述：Menu菜单Item选择处理
     * 出参：
     * 入参：MenuItem
     * 返回值：
     *************************************************/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case OPTMENUID_RESETDEVINFO: {
			Intent it = new Intent();
			it.setClass(mCxt, DevInfoInputActivity.class);
			mCxt.startActivity(it);
			finish();
		}
			break;
		case OPTMENUID_REFRESHTASK: {
			DataBaseRestore DB = DataBaseRestore.getInstance(mCxt);
			boolean updating = DB.getTaskUpdatingState();
			if (updating) {
				Toast
						.makeText(mCxt, R.string.taskisupdating,
								Toast.LENGTH_LONG).show();
				return true;
			}
			String DevState = DB.getDevStateValue();
			if (DEV_STATE_BUSY.equals(DevState)) {
				Toast.makeText(mCxt, R.string.devicebusy, Toast.LENGTH_LONG)
						.show();
				DB.SetTaskHaveUpdateState(true);
			} else {
				startUpdateTask();
			}

		}
			break;
		case OPTMENUID_SETDURATION: {
			Intent it = new Intent();
			it.setClass(mCxt, TaskUpdateSetting.class);
			mCxt.startActivity(it);
		}
			break;
		case ITEM0: {
			Intent it = new Intent(this, ColorActivity.class);
			this.startActivityForResult(it, 10);
		}
			break;
		case OPTMENUID_DEVUPDATESETTING: {
			showInputDialog(DIALOG_TYPE_UPDATESETTING);
			break;
		}
		case OPTMENUID_QUITAPP: {
			StopTaskService();
			finish();
			break;
		}
		case OPTMENUID_KILL_EXE: {
			Intent it = new Intent("com.android.autotesing.TASK_TO_EXEC");
			it.putExtra("TASK_SEND_ACK", "TASK_SEND_STOP_EXEC_SERVICE");
			this.sendBroadcast(it);
			Toast.makeText(this, R.string.executionhaseclosed,
					Toast.LENGTH_LONG).show();
			break;
		}
		case OPTMENUID_UPLOADREPORT: {
			upLoadReport();

			break;
		}
		case OPTMENUID_DELETEDB: {
			if (DeleteUserDB()) {
				finish();
				Toast.makeText(this, "数据库清除成功", Toast.LENGTH_LONG).show();
				StopTaskService();
			} else {
				Toast.makeText(this, "数据库清除失败，请重试", Toast.LENGTH_LONG).show();
			}
			break;
		}
		case OPTMENUID_BLOCKINCOMINGCALL: {
			showInputDialog(DIALOG_TYPE_BLOCKNUMBERINPUT);
			break;
		}
		case ITEM3: {
			initProgressDialog();
			this.mChecProgressDialog.show();
			Intent it = new Intent("com.android.autotesing.TASK_TO_EXEC");
			it.putExtra("TASK_SEND_ACK", "EXEC_VERSION_REQ");
			this.sendBroadcast(it);
			new Thread() {
				public void run() {
					try {
						while (count <= 50 && !mVerget) {
							count++;
							Thread.sleep(100);
						}
						Message message = new Message();
						message.what = 2;
						handler.sendMessage(message);
					} catch (Exception ex) {
						ex.printStackTrace();
						mChecProgressDialog.dismiss();
					}
				}
			}.start();

		}
			break;
		}
		return true;
	}
	
	/*************************************************
     * 描述：更新输入窗口
     * 出参：
     * 入参：type
     * 返回值：
     *************************************************/
   private void showInputDialog(int type){
	   LayoutInflater inflater = LayoutInflater.from(this); 
       final View textEntryView = inflater.inflate(  
               R.layout.dialoginput, null);  
       final EditText edtInput=(EditText)textEntryView.findViewById(R.id.edtInput); 
       final DataBaseRestore DB = DataBaseRestore.getInstance(mCxt);
       dialogType = type;
       if(dialogType == DIALOG_TYPE_UPDATESETTING){
    	 edtInput.setText(DB.getDevUpdateTimeValue());  
       }else if(dialogType == DIALOG_TYPE_BLOCKNUMBERINPUT){
    	   
       }
       
	   AlertDialog.Builder builder = new AlertDialog.Builder(mCxt);  
	   builder.setCancelable(false);  
	   builder.setTitle(R.string.dialogtitle); 
	   builder.setView(textEntryView);  
	   builder.setPositiveButton("确定",  
               new DialogInterface.OnClickListener() {  
                   public void onClick(DialogInterface dialog, int whichButton) { 
                	   if(dialogType == -1){
                		   return;
                	   }
                	   if(dialogType == DIALOG_TYPE_UPDATESETTING){
                	       DB.SetStUpdateDBValue(edtInput.getText().toString());
                	       //启动更新服务
                	       DataUpdateServiceStart();	   
                	   }else if(dialogType == DIALOG_TYPE_BLOCKNUMBERINPUT){
                		   String input = edtInput.getText().toString();
                		   if(" ".equals(input) == false){
                			   DB.SetCallblockNumValue(edtInput.getText().toString());
                			   
                		   }
                		   
                	   }

                   }  
               });  
	   builder.setNegativeButton("取消",  
               new DialogInterface.OnClickListener() {  
                   public void onClick(DialogInterface dialog, int whichButton) {  
                   }  
               });  
       builder.show(); 
   }
	
   private void showProgressbar(){
	   if (mProgressDialog == null) {
           mProgressDialog = new Dialog(this);
           mProgressDialog.setContentView(R.layout.loadprogressbar);
           mProgressDialog.setCancelable(false);
           mProgressDialog.show();
       }else{
    	   mProgressDialog.show();
       }
   }
   
   private void showUploadProgressbar(){
	   if (mProgressDialog == null) {
           mProgressDialog = new Dialog(this);
           mProgressDialog.setContentView(R.layout.loadprogressbar);
           mProgressDialog.setCancelable(false);
           TextView tv=(TextView)mProgressDialog.findViewById(R.id.text); 
           tv.setText(R.string.reportuploading); 
           
           mProgressDialog.show();
       }else{
    	   mProgressDialog.show();
       }
   }
   
   
   /*************************************************
    * 描述：任务脚本更新线程
    * 出参：
    * 入参：
    * 返回值：
    *************************************************/
	private class taskHandle extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			int ret = 0;
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//获取新任务以及脚本
			ret = getNewTaskScriptProgress();

			//获取结束后，停止progress控件，返回为0代表成功
			Message msg = new Message();
			msg.what = DIALOG_DISMISS_ACTION;
			if (ret == 0) {
				msg.arg1 = 0;
			} else {
				msg.arg1 = ret;
			}
			mDialoghandler.sendMessage(msg);

		}
	}
	
	 /*************************************************
	    * 描述：测试结果手动上传线程
	    * 出参：
	    * 入参：
	    * 返回值：
	    *************************************************/
	private class ReportUploadHandle extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			try {
				Thread.sleep(100);
				Report.UpLoadReport(mCxt);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Message msg = new Message();
			msg.arg1 = DIALOG_UPLOAD_SUCCESS;
			msg.what = DIALOG_DISMISS_ACTION;
			mDialoghandler.sendMessage(msg);

		}
	}
	
	/*************************************************
	 * 描述：获取任务及脚本 
	 * 出参： 
	 * 入参： 返回值：
	 *************************************************/
	private int getNewTaskScriptProgress() {
		boolean up_ret = false;
		DataBaseRestore DB = DataBaseRestore.getInstance(mCxt);
		TelephonyManager tm = (TelephonyManager) this
				.getSystemService(TELEPHONY_SERVICE);
		String DevType = DB.getDevTypeValue();
		int provCode = Integer.parseInt(DB.getPovCodeValue());
		String DevID = tm.getDeviceId();
		if (DevID == null || "".equals(DevID)) {
			DevID = "11111111111119";
		}
		String TestCodes = "";
		List<TaskInfo> tskall;
		tskall = AutoTestIO.getTask();
		if (tskall != null && tskall.size() > 0) {
			for (int i = 0; i < tskall.size(); i++) {
				if (TestCodes.equals("") == false) {
					TestCodes += ",";
				}
				TestCodes = TestCodes + tskall.get(i).getTestTaskCode();
			}
		}
		LOG.Log(LOG.LOG_D, "TestCodes = " + TestCodes);
		DB.SetTaskUpdatingState(true);
		List<TaskInfo> task = AutoTestIO.getNewTask(DevID, DevType, provCode,
				TestCodes, mCxt);
		if (task == null) {
			DB.SetTaskUpdatingState(false);
			return DIALOG_UPDATE_TASK_FAIL;
		}
		up_ret = AutoTestFtpIO.getAllScriptFile(DevType, DB.getPovCodeValue(),
				mCxt);
		DB.SetTaskUpdatingState(false);
		if (up_ret == true) {
			return 0;
		} else {
			return DIALOG_UPDATE_SCRIPT_FAIL;
		}

	}
	
	//线程开启
	private void taskUpdateStart() {
		new taskHandle().start();
	}
	
	//线程开启
	private void startUpdateTask(){
		   showProgressbar();
		   taskUpdateStart();
	   }
	
	private void TaskFileParse(){
		File tskfile = new File(AutoTestIO.getSDPath() + AutoTestIO.TaskFilePath);
		File scripfile = new File(AutoTestIO.getSDPath() + AutoTestIO.ScriptXMLPath);
		if(tskfile.exists() &&
				scripfile.exists()){
			if(ParseDownloadTask()){
				appendFooterView();
			}
			
		}
	}
	
   private void startGetNewTask(){
	   showProgressbar();
	   taskUpdateStart();
   }
   
   
   //解析已下载任务列表文件
   private boolean ParseDownloadTask(){
   		mTaskfilename = AutoTestIO.getSDPath() + AutoTestIO.TaskFilePath;
	    	if(mTaskfilename.length() > 0){
	    		mTaskRecord.getSharedPreferences("TASK_INFO");
	    		mTaskRecord.putString("TASKNAME", mTaskfilename);
	    		resetList();
	    		if(parseXmlFile(mTaskfilename)){
		    		mTaskFile.setText(mTaskfilename);
		    		mAddTaskBnt.setText(mCxt.getString(R.string.refreshtask));
	    			testTaskListSort();
	    			if(mTip != null){
	    				mTip.setVisibility(View.GONE);
	    			}
	    			return true;
	    		}else{
	    			mListview.removeFooterView(mFooterView);
	    			return false;
	    		}
	    	}
	    	return false;
   	
   }
   
   private class Dialoghandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case DIALOG_DISMISS_ACTION: {
				if(mProgressDialog != null){
					mProgressDialog.dismiss();	
				}
				if(msg.arg1 == DIALOG_UPDATE_TASK_FAIL){
					Toast.makeText(mCxt,R.string.gettaskfilefail,
	       					 Toast.LENGTH_SHORT).show();
				}else if(msg.arg1 == DIALOG_UPDATE_SCRIPT_FAIL){
					Toast.makeText(mCxt,R.string.getscriptfilefail,
	       					 Toast.LENGTH_SHORT).show();
				}else if(msg.arg1 == DIALOG_UPLOAD_SUCCESS){
					Toast.makeText(mCxt,
							"Report Upload Success", 
							Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(mCxt,R.string.getfilesucess,
	       					 Toast.LENGTH_SHORT).show();
//					if(ParseDownloadTask()){
//						appendFooterView();
//					}
//					if(mListview != null){
//						  mListview.requestLayout(); 
//					  }
					  if(ParseDownloadTask()){
							appendFooterView();
							if(mListview != null){
								  LOG.Log(LOG.LOG_I,"mListview.invalidate();");
								  mListview.invalidate(); 
							  }
						}
					  LOG.Log(LOG.LOG_I,"restartActivity update UI");
					
				}
				break;
			}
			default:
				break;
			}
		}

	}
   
   /*************************************************
	 * 描述：如更新服务未启动，则启动更新服务
	 * 出参： 
	 * 入参： 返回值：
	 *************************************************/
	private void DataUpdateServiceStart() {
		if (IsServiceRunning("DataUpdateService")) {
			Intent intent;
			intent = new Intent("android.intent.action.UPDATE_DEV_ALARM");
			sendBroadcast(intent);
		}

	}
   
   private void DataUpdateServiceStop(){
	   Intent intent;
	   intent = new Intent(DATAUPDATE_SERVICE);
	   this.stopService(intent);
   }
   
   
	/*************************************************
	 * 描述：检查服务是否已启动 
	 * 出参： 
	 * 入参： ServiceName 
	 * 返回值：
	 *************************************************/
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

	/*************************************************
	 * 描述：手动上传执行日志 
	 * 出参： 
	 * 入参：  
	 * 返回值：
	 *************************************************/
	private boolean upLoadReport() {
		TaskDB mTaskDB = new TaskDB(mCxt);
		int totalcase = 0;
		if (mTaskDB == null)
			return false;
		log("upLoadReport start...");
		mTaskDB.openDB();
		Cursor taskCursor = mTaskDB.getTask();
		if (taskCursor.getCount() > 0) {
			taskCursor.moveToFirst();
			String taskCode = taskCursor.getString(0);
			while (!taskCursor.isAfterLast()) {
				Cursor caseCursor = mTaskDB.getCaseFromTaskCode(taskCode);
				if (caseCursor.getCount() > 0) {
					caseCursor.moveToFirst();
					while (!caseCursor.isAfterLast()) {
						String caseName = caseCursor.getString(1);
						int Num = caseCursor.getInt(2);
						Report.addReportCaseNum(caseName, Num);
						totalcase += Num;
						caseCursor.moveToNext();
					}

				}

				caseCursor.close();
				taskCursor.moveToNext();
			}

		}
		taskCursor.close();
		mTaskDB.closeDB();
		if (totalcase != 0) {
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				Toast.makeText(mCxt, "正在上传中。。请稍候", Toast.LENGTH_LONG).show();
			} else {
				showUploadProgressbar();
				Report.setReportTotalNum(totalcase);
				new ReportUploadHandle().start();
			}

		} else {
			Toast.makeText(mCxt, "日志信息为空", Toast.LENGTH_LONG).show();
			return false;
		}
		log("upLoadReport end...");
		return true;
	}
   
   public boolean CheckTasksCasesExist(TaskInfo task){
	   
	   for(int ii = 0 ; ii < task.getKey().size() ; ++ ii){
		   String casename = task.getTaskPath() + "/" + task.getKey().get(ii).getScriptName() + "/" + task.getKey().get(ii).getScriptName() + ".lua";
		   File casefile = new File(casename);
		   log(casename);
		   if(casefile.exists() == false || casefile.length() <= 0){
			   return false;
		   }
		   
	   }
	   return true;
   }
   
   public boolean CheckTasksIsCanUse(){
	   int index = 0;
		while(index < mRunningList.size()){
			TaskInfo task = mRunningList.get(index);
			if(CheckTasksCasesExist(task) == false){
				
				return false;
			}
			index ++;
		}
		return true;
   }
   
   /*************************************************
	 * 描述：启动底层执行端，通过shell命令开启
	 * 出参： 
	 * 入参：  
	 * 返回值：
	 *************************************************/
	public boolean StartJarService(Context Cxt) {
		AutoTestIO.execCommandAsRoot("/system/bin/TestStartMain &");
		Intent it = new Intent("com.android.autotesing.TASK_TO_EXEC");
		it.putExtra("TASK_SEND_ACK", "TASK_SEND_ACK_STATE_REQ");
		Cxt.sendBroadcast(it);
		return true;
	}
	
	 /*************************************************
	 * 描述：删除数据库以及执行日志和生成的report信息
	 * 出参： 
	 * 入参：  
	 * 返回值：
	 *************************************************/
	public boolean DeleteUserDB() {
		if (mTaskRecord.DeleteDB()) {
			TaskDB mTaskDB = new TaskDB(this);
			mTaskDB.openDB();
			mTaskDB.deleteAll();
			mTaskDB.closeDB();
			if (GetSDCardStates()) {
				File sdDir = null;
				sdDir = Environment.getExternalStorageDirectory();
				File resultDir = new File(sdDir.toString() + "/AutoTest/Result");
				File file = new File(sdDir.toString() + Report.reportFilePath);
				if (file.exists()) {
					if (!file.delete()) {
						return false;
					}
				}
				if (resultDir.exists()) {
					if (delResultDir(resultDir) == false) {
						return false;
					}
				}
			}
		} else {
			return false;
		}

		for (int i = 0; i < mTestTaskList.size(); i++) {
			mTestTaskList.get(i).mTaskState = TASK_READY;
		}
		return true;
	}
   
   
   public boolean delResultDir(File dir) {  
       if (dir == null || !dir.exists() || dir.isFile()) {  
           return false;  
       }  
       for (File file : dir.listFiles()) {  
           if (file.isFile()) {  
               file.delete();  
           } else if (file.isDirectory()) {  
        	   delResultDir(file);// 递归  
           }  
       }  
       dir.delete();  
       return true;  
   }  
   
   public static boolean GetSDCardStates(){
       String status = Environment.getExternalStorageState();
       if (status.equals(android.os.Environment.MEDIA_MOUNTED)) {
           return true;
       } else {
           return false;
       }        
   }
   
}


