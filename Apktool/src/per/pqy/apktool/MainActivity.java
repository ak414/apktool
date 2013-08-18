package per.pqy.apktool;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {
	static int count=0;
	MyHandler myHandler = new MyHandler();
	ProgressDialog myDialog;
	private TextView tvpath;
	private ListView lvFiles;
	PowerManager powerManager = null; 
    WakeLock wakeLock = null; 				
    String apicode = String.valueOf(android.os.Build.VERSION.SDK_INT);
    String shell = new String();
	private static final int DECODE = 1;
	private static final int COMPILE = 2;
	private static final int DEODEX = 3;
	private static final int DECDEX = 4;
	private static final int LONGPRESS = 5;
	private static final int UNPACKIMG = 6;
	private static final int REPACKIMG = 7;
	private static final int TASK = 8;
	enum fileType {FOLDER,NFILE,APKFILE,ODEXFILE};
	
	boolean tasks[] = new boolean[]{false,false,false,false};
	ProgressDialog dialogs[] = new ProgressDialog[4];
	
	public String uri;
	File currentParent;
	File[] currentFiles;	
	class MyHandler extends Handler {	
		public void doWork(String str,final Bundle b){
			if(b.getBoolean("isTemp")){
				myDialog.setMessage(b.getString("op"));
			}else{
			SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE);
			if(settings.getInt("Vib", 0 ) != 0){
				Vibrator v = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
				v.vibrate(new long[]{0,200,100,200},-1);
			}
			if(settings.getInt("Noti", 0 ) != 0){
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				Notification notification = new Notification(R.drawable.ic_launcher,getString(R.string.op_done),System.currentTimeMillis());
				Context context = getApplicationContext(); 
				CharSequence contentTitle = b.getString("filename"); 
				CharSequence contentText =  getString(R.string.op_done); 
				Intent notificationIntent = MainActivity.this.getIntent();
				PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this,0,notificationIntent,0);
				notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);	
				notification.flags |= Notification.FLAG_AUTO_CANCEL;				
				mNotificationManager.notify(count++,notification);				
			} 	
			myDialog.dismiss();
			int num = b.getInt("tasknum");
			tasks[num] = false;
			Toast.makeText(MainActivity.this, str,Toast.LENGTH_LONG).show();
			AlertDialog.Builder b1 = new AlertDialog.Builder(
					MainActivity.this);
			String tmp_str = b.getString("filename")+"\n"+ getString(R.string.cost_time);
			
			long time = (System.currentTimeMillis() - b.getLong("time"))/1000;
			if(time > 3600){
				tmp_str += Integer.toString((int) (time/3600)) + getString(R.string.hour) + Integer.toString((int) (time%3600)/60) +
						getString(R.string.minute) + Integer.toString((int) (time%60)) + getString(R.string.second);
			}
			else if(time > 60){
				tmp_str +=  Integer.toString((int) (time%3600)/60) +
						getString(R.string.minute) + Integer.toString((int) (time%60)) + getString(R.string.second);
			}
			else{
				tmp_str +=  Integer.toString((int) time) + getString(R.string.second);
			}
			b1.setTitle(tmp_str)
			.setMessage(b.getString("output"))
			.setPositiveButton(getString(R.string.ok), null)
			.setNeutralButton((getString(R.string.copy)),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					// TODO Auto-generated method stub
					ClipboardManager cmb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					cmb.setText(b.getString("output"));
				}
			}).create().show();
			currentFiles = currentParent.listFiles();
			inflateListView(currentFiles);
		}
		}
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			final Bundle b = msg.getData();
			switch (b.getInt("what")) {
			case 0:
				doWork(getString(R.string.decompile_all_finish),b);
				break;
			case 1:
				doWork(getString(R.string.sign_finish),b);
				break;
			case 2:
				doWork(getString(R.string.recompile_finish),b);
				break;
			case 3:
				doWork(getString(R.string.decompile_dex_finish),b);
				break;
			case 4:
				doWork(getString(R.string.decompile_res_finish),b);
				break;
			case 5:
				doWork(getString(R.string.decompile_odex_finish),b);
				break;
			case 6:
				doWork(getString(R.string.op_done),b);
				break;
			case 7:
				doWork(getString(R.string.import_finish),b);
				break;
			case 8:
				doWork(getString(R.string.align_finish),b);
				break;
			case 9:
				doWork(getString(R.string.add_finish),b);
				break;
			case 10:
				doWork(getString(R.string.delete_finish),b);
				break;
			}
		}
	}
	public void threadWork(Context context,String message,final String command,final int what){
		 int freeTask=-1;
		 if(!tasks[0]) freeTask=0;
		 else if(!tasks[1]) freeTask=1;
		 else if(!tasks[2]) freeTask=2;
		 else if(!tasks[3]) freeTask=3; 	
		 if(freeTask==-1){
			 Toast.makeText(MainActivity.this, getString(R.string.nofreetask), Toast.LENGTH_SHORT).show();
			 return;
		 }
		 Thread thread = new myThread(freeTask){						
			public void run(){
				java.lang.Process process = null;
				DataOutputStream os = null;
				InputStream proerr = null; 
				InputStream proin = null;
				try {					
					Bundle tb = new Bundle();
					tb.putString("filename", new File(uri).getName());
					tb.putInt("what", what);					
					tb.putLong("time",System.currentTimeMillis());					
					tb.putBoolean("isTemp", false);	
					process = Runtime.getRuntime().exec(shell);
					os = new DataOutputStream(process.getOutputStream());
					proerr = process.getErrorStream();
					proin = process.getInputStream();
					os.writeBytes(new String("LD_LIBRARY_PATH=/data/data/per.pqy.apktool/lix:$LD_LIBRARY_PATH ")+command + "\n");
					os.writeBytes("exit\n");
					os.flush();					
					BufferedReader br1 = new BufferedReader(new InputStreamReader(proerr));
					String s = "";
					String totals="";					
					while((s=br1.readLine())!=null){
						Message mess = new Message();
						Bundle b = new Bundle();
						totals+=s+"\n";						
						b.putString("op", s);
						b.putInt("what", what);
						b.putBoolean("isTemp", true);
						b.putInt("tasknum", tasknum);
						mess.setData(b);
						myHandler.sendMessage(mess);
					}					
					process.waitFor();
					Message tmess = new Message();					
					tb.putString("output",totals+RunExec.inputStream2String(proin, "utf-8"));												
					tmess.setData(tb);
					myHandler.sendMessage(tmess);
				} catch (Exception e) {
					Log.d("*** DEBUG ***",
							"ROOT REE" + e.getMessage());
				} finally {
					try {
						if (os != null) {
							os.close();
						}
						process.destroy();
					} catch (Exception e) {
					}
				}			
			}		
		};
		
		thread.start();
		myDialog = new ProgressDialog(context);
		myDialog.setMessage(message);
		myDialog.setIndeterminate(true);
		myDialog.setCancelable(false);
		myDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.put_background), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        myDialog.dismiss();
		    }
		});
		/*
		myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		       dialog.dismiss();
		       
		    	
		    }
		});
		*/
		dialogs[freeTask] = myDialog;
		tasks[freeTask] = true;
		myDialog.show();
	}
	protected Dialog onCreateDialog(int id) {
		
		switch (id) {
		case DECODE:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.dec_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {						
							switch (which) {							
							case 0:
								final	String command = new String(" sh /data/data/per.pqy.apktool/mydata/apktool.sh d -f ") 
								+ "'" + uri + "' '" + uri.substring(0, uri.length()-4) + "_src'";
								threadWork(MainActivity.this,getString(R.string.decompiling),command,0);
								break;									
							case 1:
								final	String command1 = new String(" sh /data/data/per.pqy.apktool/mydata/apktool.sh d -f -r ") 
								+ "'" + uri + "' '" + uri.substring(0, uri.length()-4) + "_src'";
								threadWork(MainActivity.this,getString(R.string.decompiling),command1,3);
								break;	
							case 2:
								final String command2 = new String(" sh /data/data/per.pqy.apktool/mydata/apktool.sh d -f -s ") 
								+ "'" + uri + "' '" + uri.substring(0, uri.length()-4) + "_src'";
								threadWork(MainActivity.this,getString(R.string.decompiling),command2,4);								
								break;							
							case 3:		
								final String command3 = new String(" sh /data/data/per.pqy.apktool/mydata/signapk.sh ") 
								+ "'" + uri + "' '" + uri.substring(0, uri.length()-4) + "_sign.apk'";
								threadWork(MainActivity.this,getString(R.string.signing),command3,1);					
								break;
							case 4:								
									final String command4 = new String(" /data/data/per.pqy.apktool/lix/dexopt-wrapper ") 
									+"'" + uri + "' '" + uri.substring(0, uri.length()-3) + "odex'";
									threadWork(MainActivity.this,getString(R.string.making_odex),command4,6);	
									break;
								
							case 5:
								final String command5 = new String(" /data/data/per.pqy.apktool/lix/zipalign -f -v 4 ") + "'" + uri + "' '" + uri.substring(0, uri.length()-4)+"_zipalign.apk'";
								threadWork(MainActivity.this,getString(R.string.aligning),command5,8);
								break;
							case 6:
								Intent intent = new Intent(Intent.ACTION_VIEW);  
						        final Uri apkuri = Uri.fromFile(new File(uri));  
						        intent.setDataAndType(apkuri, "application/vnd.android.package-archive");  
						        startActivity(intent);
						        break;
							case 7:
								final String command6 = new String("/data/data/per.pqy.apktool/lix/7z d -tzip '") + uri + "' classes.dex";
								threadWork(MainActivity.this,getString(R.string.deleting),command6,10);
								break;
							case 8:
								File f = new File(uri);
								if(!new File(f.getParent()+"/META-INF").exists()){
									final String command7 = new String("sh /data/data/per.pqy.apktool/mydata/tool.sh ")+"'"+f.getParent() +"' '" + f.getName()+"'";
									threadWork(MainActivity.this,getString(R.string.extracting),command7,6);
								}
								else
									Toast.makeText(MainActivity.this, getString(R.string.dir_exist), Toast.LENGTH_LONG).show();
								break;
							case 9:
								final String command8 = new String("/data/data/per.pqy.apktool/lix/7z d -tzip ") +"'"+ uri +"'"+ " META-INF";
								threadWork(MainActivity.this,getString(R.string.deleting),command8,10);
								break;
							case 10:
								String str = new File(uri).getParent();
								if(new File(str+"/META-INF").exists()){
									str = new File(uri).getParent();
									final String command9 = new String("/data/data/per.pqy.apktool/lix/7z a -tzip ") +"'"+ uri + "' '"+ str+"/META-INF'";
									threadWork(MainActivity.this,getString(R.string.adding),command9,8);}
								else
									Toast.makeText(MainActivity.this, getString(R.string.dir_not_exist),Toast.LENGTH_LONG).show();
								break;
							case 11:
								final String command10 = new String(" sh /data/data/per.pqy.apktool/mydata/apktool.sh if ")+"'"+uri+"'";								
								threadWork(MainActivity.this,getString(R.string.importing_framework),command10,7);
							case 12:
								return;								
							}
						}
					}).create();
		case COMPILE:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.comp_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								if(uri.endsWith("_src")){
								final String command = new String(" sh /data/data/per.pqy.apktool/mydata/apktool.sh b -f -a /data/data/per.pqy.apktool/lix/aapt '")
								+ uri + "' '" + uri + ".apk'";								
								threadWork(MainActivity.this,getString(R.string.recompiling),command,2);			
								}else if(uri.endsWith("_odex")){
									final String command = new String(" sh /data/data/per.pqy.apktool/mydata/smali.sh -a ")
									+apicode +" '" + uri + "' -o '" + uri.substring(0, uri.length()-5) + ".dex'";									
									threadWork(MainActivity.this,getString(R.string.recompiling),command,2);
								}else if(uri.endsWith("_dex")){
									final String command = new String(" sh /data/data/per.pqy.apktool/mydata/smali.sh -a ")
									+apicode +" '"+ uri + "' -o '" + uri.substring(0, uri.length()-4) + ".dex'";									
									threadWork(MainActivity.this,getString(R.string.recompiling),command,2);
								}
								break;
							case 1:
								currentParent = new File(uri);
								currentFiles = currentParent.listFiles();
								inflateListView(currentFiles);
							case 2:								
								return;
							}
						}
					}).create();
		case DEODEX:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.deodex_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:								
								final String command = new String(" sh /data/data/per.pqy.apktool/mydata/baksmali.sh -x -a ")+apicode
								+" '"+uri+"' -o '"+uri.substring(0, uri.length()-5)+"_odex'";
								threadWork(MainActivity.this,getString(R.string.decompiling),command,5);
								break;												
							case 1:
								return;
							}
						}
					}).create();
		case DECDEX:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.decdex_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								final String command = new String(" sh /data/data/per.pqy.apktool/mydata/baksmali.sh '")+
								uri+"' -o '"+uri.substring(0, uri.length()-4)+"_dex'";
								threadWork(MainActivity.this,getString(R.string.decompiling),command,3);
								break;
							case 1:
								String apkFile = uri.substring(0,uri.length()-3)+"apk";
								if(new File(apkFile).exists()){
									apkFile = uri.substring(0,uri.length()-3)+"apk";
									RunExec.Cmd(shell,new String(" mv '")+uri+"' '"+new File(uri).getParent()+"/classes.dex'");
									final String command1 = new String(" /data/data/per.pqy.apktool/lix/7z a -tzip '"+apkFile+"' '"+new File(uri).getParent()+"/classes.dex'");
									threadWork(MainActivity.this,getString(R.string.adding),command1,9);
								}
								else
									Toast.makeText(MainActivity.this, getString(R.string.apk_not_exist), Toast.LENGTH_LONG).show();
								break;
							case 2:
								String jarFile = uri.substring(0,uri.length()-3)+"jar";
								if(new File(jarFile).exists()){
									jarFile = uri.substring(0,uri.length()-3)+"jar";
									RunExec.Cmd(shell,new String(" mv '")+uri+"' '"+new File(uri).getParent()+"/classes.dex'");
									final String command2 = new String(" /data/data/per.pqy.apktool/lix/7z a -tzip '"+jarFile+"' '"+new File(uri).getParent()+"/classes.dex'");
									threadWork(MainActivity.this,getString(R.string.adding),command2,9);
								}
								else
									Toast.makeText(MainActivity.this, getString(R.string.jar_not_exist), Toast.LENGTH_LONG).show();
							case 3:
								return;
							}
						}
					}).create();
		case LONGPRESS:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.longpress_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								final EditText et = new EditText(MainActivity.this);
								et.setText(new File(uri).getName());
								new AlertDialog.Builder(MainActivity.this)
								.setTitle(getString(R.string.new_name))
								.setView(et)
								.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,int which) {
								// TODO Auto-generated method stub
									String newName = et.getText().toString();									
									newName = currentParent + "/" + newName;
									RunExec.Cmd(shell," chmod 777 "+currentParent);
									new File(uri).renameTo(new File(newName));
									currentFiles = currentParent.listFiles();
									inflateListView(currentFiles);
								}														
								})
								.setNegativeButton(getString(R.string.cancel), null).show();
								break;
							case 1:
								new AlertDialog.Builder(MainActivity.this)
								.setTitle(getString(R.string.want_to_delete))
								.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
								@Override
									public void onClick(DialogInterface dialog,int which) {
									// TODO Auto-generated method stub																		
										final String command = new String(" rm -r '")+uri+"'";
										threadWork(MainActivity.this,getString(R.string.deleting),command,10);																	
									}														
									})
								.setNegativeButton(getString(R.string.cancel), null).show();
								break;
							case 2:
								RunExec.Cmd(shell,new String(" chmod 777 '")+uri+"'");
								break;
							case 3:
								//RunExec.Cmd(new String("rm /data/data/per.pqy.apktool/mydata"));
								File file = new File("/data/data/per.pqy.apktool/mydata");
								file.delete();
								RunExec.Cmd(shell,new String(" ln -s '")+uri+"' /data/data/per.pqy.apktool/mydata");
								extractData();
								break;
							case 4:
								return ;
		}
						}
					}).create();
		case UNPACKIMG:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.unpackimg, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								if(uri.endsWith("boot.img")){
									File tmp = new File(uri);								
									final String command = new String(" sh /data/data/per.pqy.apktool/mydata/unpackimg.sh '") + 
											tmp.getParent() + "' boot.img new.img mt65xx"; 
									threadWork(MainActivity.this, getString(R.string.extracting), command, 6);
								}
								else{
									File tmp = new File(uri);								
									final String command = new String(" sh /data/data/per.pqy.apktool/mydata/unpackimg.sh '") + 
											tmp.getParent() + "' recovery.img new.img mt65xx"; 
									threadWork(MainActivity.this, getString(R.string.extracting), command, 6);
								}
								break;
							case 1:
								if(uri.endsWith("boot.img")){
									File tmp = new File(uri);								
									final String command = new String(" sh /data/data/per.pqy.apktool/mydata/unpackimg.sh '") + 
											tmp.getParent() + "' boot.img new.img"; 
									threadWork(MainActivity.this, getString(R.string.extracting), command, 6);
								}
								else{
									File tmp = new File(uri);								
									final String command = new String(" sh /data/data/per.pqy.apktool/mydata/unpackimg.sh '") + 
											tmp.getParent() + "' recovery.img new.img"; 
									threadWork(MainActivity.this, getString(R.string.extracting), command, 6);
								}
								break;
							case 2:
								return;
							}
						}
					}).create();
									
		case REPACKIMG:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.repackimg, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								
									File tmp = new File(uri);								
									final String command = new String(" sh /data/data/per.pqy.apktool/mydata/repackimg.sh '") + 
											tmp.getParent() + "' new.img mtk"; 
									threadWork(MainActivity.this, getString(R.string.compressing), command, 6);
								
								break;
							case 1:
								
									File tmp1 = new File(uri);								
									final String command1 = new String(" sh /data/data/per.pqy.apktool/mydata/repackimg.sh '") + 
											tmp1.getParent() + "' new.img"; 
									threadWork(MainActivity.this, getString(R.string.compressing), command1, 6);
								
								
								break;
							case 2:
								currentParent = new File(uri);
								currentFiles = currentParent.listFiles();
								inflateListView(currentFiles);
							case 3:
								return;
							}
						}
					}).create();
		case TASK:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.Task, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								if(tasks[0]){
									dialogs[0].show();
								}
								else {
									Toast.makeText(MainActivity.this, getString(R.string.cur_task_not_run),Toast.LENGTH_SHORT).show();
								}
								break;
							case 1:
								if(tasks[1]){
									dialogs[1].show();
								}
								else {
									Toast.makeText(MainActivity.this, getString(R.string.cur_task_not_run),Toast.LENGTH_SHORT).show();
								}
								break;
							case 2:
								if(tasks[2]){
									dialogs[2].show();
								}
								else {
									Toast.makeText(MainActivity.this, getString(R.string.cur_task_not_run),Toast.LENGTH_SHORT).show();
								}
								break;
							case 3:
								if(tasks[3]){
									dialogs[3].show();
								}
								else {
									Toast.makeText(MainActivity.this, getString(R.string.cur_task_not_run),Toast.LENGTH_SHORT).show();
								}
							}
						}
					}).create();
		}
		
		return null;
	} 

	public void onCreate(Bundle savedInstanceState) {
		if(new File("/system/bin/su").exists()||new File("/system/xbin/su").exists())
			shell = "su ";
		else {
			shell = "sh ";
		}
	//	shell += new String("LANG=") + getResources().getConfiguration().locale.toString() + ".UTF-8 ";
		super.onCreate(savedInstanceState);
		myHandler = new MyHandler();
		 this.powerManager = (PowerManager) this 
	                .getSystemService(Context.POWER_SERVICE); 
	        this.wakeLock = this.powerManager.newWakeLock( 
	                PowerManager.FULL_WAKE_LOCK, "My Lock"); 
	    
		if (!(new File("/data/data/per.pqy.apktool/tag").exists())) {
			AlertDialog.Builder b1 = new AlertDialog.Builder(MainActivity.this);
				b1.setTitle(getString(R.string.declaration)).setMessage(getString(R.string.agreement));
				b1.setPositiveButton(getString(R.string.ok), null);
				b1.setNeutralButton((getString(R.string.never_remind)),
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						// TODO Auto-generated method stub
						RunExec.Cmd(shell," mkdir  /data/data/per.pqy.apktool/tag");
					}
				});
				b1.create().show();			
		} 
		if(!new File("/data/data/per.pqy.apktool/mydata").exists()){
			if(new File("/sdcard/apktool").exists()){
				RunExec.Cmd(shell, "rm /data/data/per.pqy.apktool/mydata");
				RunExec.Cmd(shell,"ln -s /sdcard/apktool /data/data/per.pqy.apktool/mydata");		
				extractData();
			}
			else{
				AlertDialog.Builder b1 = new AlertDialog.Builder(MainActivity.this);
				b1.setTitle(getString(R.string.warning)).setMessage(getString(R.string.data_not_in_sdcard));
				b1.setPositiveButton(getString(R.string.ok), null);
				b1.create().show();
			}
		}
		setContentView(R.layout.main);
		lvFiles = (ListView) this.findViewById(R.id.files);
		tvpath = (TextView) this.findViewById(R.id.tvpath);
		SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE);
		if(settings.getInt("bg", 0 ) == 0){
			lvFiles.setBackgroundColor(Color.BLACK);
			tvpath.setBackgroundColor(Color.BLACK);
		}
		File root = new File(settings.getString("parent", "/"));
		if(!root.canRead())
			root = new File("/");
		currentParent = root;
		currentFiles = currentParent.listFiles();
		inflateListView(currentFiles);
		
		
		lvFiles.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				
				uri = currentFiles[position].getPath();
				
				if(uri.contains("//"))
					uri = RunExec.removeRepeatedChar(uri);
				if (currentFiles[position].isFile()) {									
					if (uri.endsWith(".apk")|| uri.endsWith("jar"))
						showDialog(DECODE);
					else if(uri.endsWith(".odex"))
						showDialog(DEODEX);
					else if(uri.endsWith(".dex"))
						showDialog(DECDEX);
					else if(uri.endsWith("boot.img")||uri.endsWith("recovery.img")){
						showDialog(UNPACKIMG);
					}
					else{
						Intent intent = new Intent(Intent.ACTION_VIEW);  
				        final Uri apkuri = Uri.fromFile(new File(uri));  
				        intent.setDataAndType(apkuri, "*/*");  
				        startActivity(intent);
					}
					return;
				} else if (currentFiles[position].isDirectory()
						&& (currentFiles[position].getName().endsWith("_src")||
								currentFiles[position].getName().endsWith("_odex")||
								currentFiles[position].getName().endsWith("_dex"))) {					
					showDialog(COMPILE);
					return;
				}else if(currentFiles[position].isDirectory()
						&& (currentFiles[position].getName().equals("ramdisk"))){
					showDialog(REPACKIMG);
					return;
				}
				
				
				File[] tem = currentFiles[position].listFiles();
				if (tem == null) {
					Toast.makeText(MainActivity.this, getString(R.string.directory_no_permission),
							Toast.LENGTH_LONG).show();
				} else {
					
					currentParent = currentFiles[position];
					
					currentFiles = tem;
					
					inflateListView(currentFiles);
				}
			}
		});
		
		lvFiles.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO 自动生成的方法存根
				uri = currentFiles[position].getPath();
				if(uri.contains("//"))
					uri = RunExec.removeRepeatedChar(uri);				
				showDialog(LONGPRESS);
				return false;
			}			
		}); 
	}

	@SuppressWarnings("unchecked")
	@SuppressLint("SimpleDateFormat")
	private void inflateListView(File[] files) {
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();		
		Arrays.sort(files,new FileComparator());
			
		for (int i = 0; i < files.length; i++) {
			Map<String, Object> listItem = new HashMap<String, Object>();
			if (files[i].isDirectory()) {
				listItem.put("icon", getFileIcon(MainActivity.this, null,fileType.FOLDER ));			
			}else if(files[i].getName().endsWith(".apk")){
				listItem.put("icon", getFileIcon(MainActivity.this,files[i].getAbsolutePath(),fileType.APKFILE));
			
			}else if(files[i].getName().endsWith(".odex")){
				listItem.put("icon", getFileIcon(MainActivity.this,null,fileType.ODEXFILE));
			
			}else{
				listItem.put("icon", getFileIcon(MainActivity.this,null,fileType.NFILE));
			}
			listItem.put("filename", files[i].getName());
			File myFile = new File(files[i].getAbsolutePath());
			long modTime = myFile.lastModified();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			long size = myFile.length();
			double fileSize;
			String strSize = null;
			java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00");
			if(size>=1073741824){
				fileSize = (double)size/1073741824.0;				
				strSize = df.format(fileSize)+"G";				
			}else if(size>=1048576){
				fileSize = (double)size/1048576.0;				
				strSize = df.format(fileSize)+"M";				
			}else if(size>=1024){
				fileSize = (double)size/1024;			
				strSize = df.format(fileSize)+"K";
			}else{
				strSize = Long.toString(size)+"B";
			}
			if(myFile.isFile()&&myFile.canRead())
				listItem.put("modify",dateFormat.format(new Date(modTime))+"   " + strSize);
			else
				listItem.put("modify",dateFormat.format(new Date(modTime)));
			
			listItems.add(listItem);
		}

		Adapter adapter = new Adapter(this, listItems, R.layout.list_item, new String[] { "filename", "icon", "modify" }, new int[] {
				R.id.file_name, R.id.icon, R.id.file_modify });
		
		lvFiles.setAdapter(adapter);
		tvpath.setText(currentParent.getAbsolutePath());

	}

	public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent) {
		if (paramInt == KeyEvent.KEYCODE_BACK)
			try {
				if (!currentParent.getCanonicalPath().equals("/")) {
					currentParent = currentParent.getParentFile();
					currentFiles = currentParent.listFiles();
					inflateListView(currentFiles);
				} else {
					AlertDialog.Builder localBuilder = new AlertDialog.Builder(
							this);
					localBuilder.setTitle(getString(R.string.want_to_exit));
					localBuilder.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface paramAnonymousDialogInterface,int paramAnonymousInt) {
							finish();
						}
					});
					localBuilder.setNegativeButton(getString(R.string.no),null);
					localBuilder.create().show();
				}
			} catch (Exception localException) {
			}
		return false;
	}

	public boolean onCreateOptionsMenu(Menu paramMenu) {
		getMenuInflater().inflate(R.menu.menu, paramMenu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
		switch (paramMenuItem.getItemId()) {
		default:
			return false;
		case R.id.about:
			AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
						localBuilder.setTitle(getString(R.string.about)).setMessage(
								"refer to https://code.google.com/p/apktool");
						localBuilder.setPositiveButton(getString(R.string.ok), null);
						localBuilder.create().show();
						
			return false;
		case R.id.exit:
			finish();
			return false;
		case R.id.task:
			showDialog(TASK);
			return false;
		case R.id.donate:
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			//以下链接未经作者同意，不要修改。
			intent.setData(Uri.parse("https://me.alipay.com/pangqingyuan"));
			startActivity(intent);
			return false;
		case R.id.refresh:
			currentFiles = currentParent.listFiles();
			inflateListView(currentFiles);
			return false;	
		case R.id.setting:
			SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE); 
			new AlertDialog.Builder(this).setTitle(getString(R.string.setting))
			.setMultiChoiceItems(new String[] { getString(R.string.vibration), getString(R.string.notify) ,getString(R.string.white_background),getString(R.string.keep_screen_on)},
									new boolean[] {settings.getInt("Vib", 0)==1,settings.getInt("Noti", 0)==1,
									settings.getInt("bg", 0)==1,settings.getInt("bl", 0)==1},
									new DialogInterface.OnMultiChoiceClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					// TODO 自动生成的方法存根
					SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE); 
					SharedPreferences.Editor editor = settings.edit();  					 
					if(isChecked){
						switch (which){
							case 0:
								editor.putInt("Vib", 1);
								editor.commit();
								break;
							case 1:
								editor.putInt("Noti", 1);
								editor.commit();
								break;
							case 2:
								editor.putInt("bg", 1);
								lvFiles.setBackgroundColor(Color.WHITE);
								tvpath.setBackgroundColor(Color.WHITE);
								editor.commit();						
								break;
							case 3:
								editor.putInt("bl",1);
								wakeLock.acquire();
								editor.commit();
								break;
						 					
					}}
					else{
						switch (which){
						case 0:
							editor.putInt("Vib", 0);
							editor.commit(); 
							break;
						case 1:
							editor.putInt("Noti", 0);
							editor.commit(); 
							break;
						case 2:
							editor.putInt("bg", 0);
							lvFiles.setBackgroundColor(Color.BLACK);
							tvpath.setBackgroundColor(Color.BLACK);
							editor.commit(); 						
							break;
						case 3:
							editor.putInt("bl",0);
							wakeLock.release();
							editor.commit();
							break;								
					}}
					
				}
			})
			.setPositiveButton(getString(R.string.ok),null)
			.show();
			return false;
		}
		
	}
	protected void onResume() { 
        super.onResume(); 
        SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE);
	 	 if(settings.getInt("bl", 0 ) != 0)
          this.wakeLock.acquire(); 
		
    } 
	protected void onPause() { 
        super.onPause(); 
        SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE);
        if(settings.getInt("bl", 0 ) != 0)
          this.wakeLock.release(); 
        
    }
	protected void onDestroy() {
		SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("parent", currentParent.toString());
		editor.commit();
		super.onDestroy();
		System.exit(0);
	}

    public  Drawable getFileIcon(Context context,String apkPath,fileType type) {
    	switch(type){
    	case FOLDER:
    		return context.getResources().getDrawable(R.drawable.folder);
    		
    	case NFILE:
    		return context.getResources().getDrawable(R.drawable.file);
    		
    	case ODEXFILE:
    		return context.getResources().getDrawable(R.drawable.odex);
    		
    	case APKFILE:
    		PackageManager pm = MainActivity.this.getPackageManager();
    		PackageInfo info = pm.getPackageArchiveInfo(apkPath,
                PackageManager.GET_ACTIVITIES);
    		if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                return appInfo.loadIcon(pm);
            } catch (OutOfMemoryError e) {
          //      Log.e("ApkIconLoader", e.toString());
            	}
    		}
    		else 
    			return context.getResources().getDrawable(R.drawable.file);
    	}
        return null;
    }
    public void extractData(){
		new Thread(){
			public void run(){						
				if (!(new File("/data/data/per.pqy.apktool/lix").exists())) {
					RunExec.Cmd(shell,"dd if=/data/data/per.pqy.apktool/mydata/busybox of=/data/data/per.pqy.apktool/tar");
					RunExec.Cmd(shell,"chmod 777 /data/data/per.pqy.apktool/tar");
					RunExec.Cmd(shell,"/data/data/per.pqy.apktool/tar xf /data/data/per.pqy.apktool/mydata/jvm.tar --directory=/data/data/per.pqy.apktool");	
					RunExec.Cmd(shell,"chmod -R 755 /data/data/per.pqy.apktool/lix");
					RunExec.Cmd(shell," rm /data/data/per.pqy.apktool/tar");
				}
			}
		}.start();
    }
}


