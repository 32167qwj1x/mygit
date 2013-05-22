package com.android.autotesing;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.xmlParse.TaskInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author 
 *
 */
public class FileExplorerActivity extends Activity {

	private ListView lvFiles;
	private boolean is_root = false;
	// 记录当前的父文件夹
	File currentParent;
	// 记录当前路径下的所有文件夹的文件数组
	File[] currentFiles;
	//高亮文件名
	String curfilename = "file";
	// 文件类型枚举
	private final static int E_FILE_TYPE_XML= 0x30;
	private final static int E_FILE_TYPE_UNKNOW = 0x40;
	// 文件后缀名
	private final static String XMLFILE = ".xml";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filelist);
		lvFiles = (ListView) this.findViewById(R.id.files);
		// 如果SD卡存在的话
		if (checkSDcard() == true) {
			File root = Environment.getExternalStorageDirectory();
			currentParent = root;
			currentFiles = root.listFiles();
			// 使用当前目录下的全部文件、文件夹来填充ListView
			inflateListView(currentFiles);
			
		}else {
			TextView tv = (TextView)this.findViewById(R.id.tip_text);
			tv.setText(R.string.sdexist);
			is_root = true;
			//showDialogEx(R.string.sdexist);
			return;
		}
		
		lvFiles.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				//处理第一项item
				if(position == 0 && is_root == false) {
					onClickBack();
					return ;
				}
				// 如果用户单击了文件，直接返回，不做任何处理
				int file_index = position;
				if(is_root == false) {
					file_index = position - 1;
				}
				if (currentFiles[file_index].isFile()) {
					// 也可自定义扩展打开这个文件等
					setFilename(currentFiles[file_index].getPath());
					openMediafile(getFiletype(currentFiles[file_index].getName()));
					return;
				}
				// 获取用户点击的文件夹 下的所有文件
				File[] tem = currentFiles[file_index].listFiles();
				if (tem == null || tem.length == 0) {

					Toast.makeText(FileExplorerActivity.this,
							"当前路径不可访问或者该路径下没有文件", Toast.LENGTH_SHORT).show();
				} else {
					// 获取用户单击的列表项对应的文件夹，设为当前的父文件夹
					currentParent = currentFiles[file_index];
					// 保存当前的父文件夹内的全部文件和文件夹
					currentFiles = tem;
					// 再次更新ListView
					inflateListView(currentFiles);
				}

			}
		});
	}

	/**
	 * 根据文件夹填充ListView
	 * 
	 * @param files
	 */
	private void inflateListView(File[] files){
		//sort
		sortFiles(files);
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		try {
			//添加返回item
			if (!currentParent.getCanonicalPath().equals("/mnt/sdcard")) {
				Map<String, Object> item = new HashMap<String, Object>();
				item.put("icon", R.drawable.cartoon_back_big);
				// 添加一个文件名称
				item.put("filename", "...");
				item.put("modify", "返回上一目录");
				listItems.add(item);
				is_root = false;
			}
			else {
				is_root = true;
			}
		}catch (Exception e) {
			// TODO: handle exception
		}
		for (int i = 0; i < files.length; i++) {
			Map<String, Object> listItem = new HashMap<String, Object>();
			if (files[i].isDirectory()) {
				// 如果是文件夹就显示的图片为文件夹的图片
				listItem.put("icon", R.drawable.directory);
			} else {
				listItem.put("icon", R.drawable.file_doc);
			}
			// 添加一个文件名称
			listItem.put("filename", files[i].getName());

//			File myFile = new File(files[i].getName());

//			// 获取文件的最后修改日期
//			long modTime = myFile.lastModified();
//			SimpleDateFormat dateFormat = new SimpleDateFormat(
//					"yyyy-MM-dd HH:mm:ss");
//			System.out.println(dateFormat.format(new Date(modTime)));
//			// 添加一个最后修改日期
//			listItem.put("modify",
//					"修改日期：" + dateFormat.format(new Date(modTime)));
			listItems.add(listItem);
		} 
		// 定义一个SimpleAdapter
		SimpleAdapter adapter = new SimpleAdapter(
				FileExplorerActivity.this, listItems, R.layout.file_item,
				new String[] { "filename", "icon"}, new int[] {
						R.id.file_name, R.id.icon});
		
		// 填充数据集
		lvFiles.setAdapter(adapter);

		try {
			//tvpath.setText("当前路径为:" + currentParent.getCanonicalPath());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private void onClickBack() {
		try {

			if (!currentParent.getCanonicalPath().equals("/mnt/sdcard")) {

				// 获取上一级目录
				currentParent = currentParent.getParentFile();
				// 列出当前目录下的所有文件
				currentFiles = currentParent.listFiles();
				// 再次更新ListView
				inflateListView(currentFiles);
			}else { 
				Toast.makeText(FileExplorerActivity.this,
						"已经是根目录.", Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	/**
	 * 根据文件类型调用不同的接口显示内容
	 * @param type
	 */
	private void openMediafile(int type) {
		Intent it = new Intent();
		switch(type) {
		case E_FILE_TYPE_XML:
			it.setClass(this, AutoTestingActivity.class);
			it.putExtra("xmlname", getFilename());
//			this.setResult(1, it);
			this.startActivity(it);
	        finish();
			break;
		default:
			showDialogEx(R.string.unsupportfile);
			break;
		}
	}
	
	/**
	 * 不支持文件open提示dialog
	 */
	private void showDialogEx(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(FileExplorerActivity.this);
		builder.setIcon(R.drawable.alert_dialog_icon);
		builder.setTitle(id);
//		builder.setMessage(R.string.unsupportfile);
		builder.show();
	}
	
	private int getFiletype(String filename) {
		if(filename.endsWith(XMLFILE)) {
			return E_FILE_TYPE_XML;
		}
		return E_FILE_TYPE_UNKNOW;
	} 
	
	/**
	 * 设置文件名
	 * @param name
	 */
	private void setFilename(String name) {
		curfilename = name;
	}

	/**
	 * 获取文件名	
	 * @return
	 */
	private String getFilename() {
		return curfilename;
	}
	
	private boolean checkSDcard() { 
		if (android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED)) {
			return true;
		}
		return false;
	}
	
	/**
	 * @Override
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if(is_root) {
				Intent it = new Intent();
				it.setClass(this, AutoTestingActivity.class);
				this.setResult(1, it);
				finish();
				//System.exit(0);
			}else {
				onClickBack();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void sortFiles(File[] files) { 
		Arrays.sort(files, new Comparator<File>() { 
			public int compare(File file1, File file2) {
				if (file1.isDirectory() && file2.isDirectory())
					return 1; 
				if (file2.isDirectory()) 
					return 1; 
				return -1; 
				} 
			}); 
		}
}