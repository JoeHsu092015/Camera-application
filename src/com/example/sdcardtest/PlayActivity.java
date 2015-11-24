package com.example.sdcardtest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class PlayActivity extends Activity {

	private final String tag = "PlayActivity";
	private FileWriter logDataWriter = null;
	private String FilePATH;
	private VideoView videoView;
	private int fileIndex = 0;
	private String fileName;
	private TextView tw;
	private String failVideoFileName;
	private String logFileName;
	private int failFile = 0;
	private ArrayList<String> fileList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_play);
		videoView = (VideoView) this.findViewById(R.id.videoView1);

		MediaController mc = new MediaController(this);
		videoView.setMediaController(mc);
		FilePATH = System.getenv("SECONDARY_STORAGE") + "/Android/data/com.example.sdcardtest/video/";

		LayoutInflater controlInflater = LayoutInflater.from(getBaseContext());
		View viewControl = controlInflater.inflate(R.layout.control_play, null);
		LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		this.addContentView(viewControl, layoutParamsControl);
		tw = (TextView) this.findViewById(R.id.play_textView);
		Toast.makeText(getApplicationContext(), "Play videos", Toast.LENGTH_LONG).show();
		setBundleData();
		initLogFile();
		logData("\n\nPlay videos");
		fileList = getDirectoryFileList();
		playVideo();

	}

public void playVideo(){
		
		final ArrayList<String> fileList = getDirectoryFileList();
		fileName = fileList.get(fileIndex++);
		videoView.setVideoPath(FilePATH+fileName);
		videoView.requestFocus();
		videoView.start();
		sendtoUI("Video : "+fileName);
		logData("Video : "+fileName);
		videoView.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mc) {
            	if(fileIndex==fileList.size()){
            		//logData("Total videos = " + fileList.size() + ", error = " + failFile);
					logData("Play videos finish");
            		closeWriter();
            		changeActivity();
            		return;
            	}
            	fileName = fileList.get(fileIndex);
                videoView.setVideoPath(FilePATH+fileName);
    			videoView.requestFocus();
    			videoView.start();
    			sendtoUI("Video : "+fileName);
    			logData("Video : "+fileName);
    			fileIndex++;
            }
        });  
		
		videoView.setOnErrorListener(new OnErrorListener() {
			@Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
				
					//Toast.makeText(getApplicationContext(), "Error play Video: " + fileName, Toast.LENGTH_LONG).show();
					
					sendtoUI("Error play Video: " + fileName);
					logData("Error play Video: " + fileName);
				return false;
            }
        });
		
		
		
	}

	/*public void setCompletionListener() {
		videoView.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mc) {
				if (fileIndex == fileList.size()) {
					logData("Total videos = " + fileList.size() + ", error = " + failFile);
					logData("Play videos finish");
					closeWriter();
					changeActivity();
					return;
				}
				playVideo();
			}
		});
	}*/

	/*public void setErrorListener() {
		videoView.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {

				if (fileName.equals(failVideoFileName)) {
					playVideo();
					return true;
				} else {
					//Toast.makeText(getApplicationContext(), "Error play Video: " + fileName, Toast.LENGTH_LONG).show();
					failFile++;
					sendtoUI("Error play Video: " + fileName);
					logData("Error play Video: " + fileName);
					playVideo();
					return false;
				}
			}
		});

	}*/

	public ArrayList<String> getDirectoryFileList() {
		String SDcardPath = System.getenv("SECONDARY_STORAGE") + "/Android/data/com.example.sdcardtest/video";
		File f = new File(SDcardPath);
		ArrayList<String> fileList = new ArrayList<String>();
		if (f.isDirectory()) {
			String[] s = f.list();
			for (int i = 0; i < s.length; i++) {
				if(s[i].equals(failVideoFileName)) continue;
				fileList.add(s[i]);
			}
		}
		return fileList;
	}

	public void sendtoUI(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tw.setText(" " + x);
			}
		});
	}

	private void setBundleData() {
		Bundle bundle = this.getIntent().getExtras();
		failVideoFileName = bundle.getString("failVideoFileName");
		logFileName = bundle.getString("logFileName");
	}

	public void logData(String log) {
		try {
			logDataWriter.write(log + "\n");
			logDataWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeWriter() {
		try {
			logDataWriter.flush();
			logDataWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void initLogFile() {
		String path = Environment.getExternalStorageDirectory() + "/SDcardTestLog";
		File file = new File(path);
		if (!file.exists())
			file.mkdir();
		try {
			logDataWriter = new FileWriter(path + "/" + logFileName + ".txt", true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void changeActivity() {
		Intent intent = new Intent();
		intent.setClass(PlayActivity.this, PhotoActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("logFileName", logFileName);
		intent.putExtras(bundle);
		finish();
		startActivity(intent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(PlayActivity.this).setTitle("Exit").setMessage("Exit Application ?")
					.setIcon(R.drawable.ic_launcher).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							android.os.Process.killProcess(android.os.Process.myPid());
						}
					}).setNegativeButton("No", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}).show();
		}
		return true;
	}
}
