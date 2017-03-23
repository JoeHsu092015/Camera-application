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
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoActivity extends Activity {

	final int PLAY_NEXT = 1;
	final int PLAY_PREV = 2;
	private final String tag = "PlayActivity";
	private FileWriter logDataWriter = null;
	private String SdCardPath;
	private VideoView videoView;
	private int fileIndex = 0;
	private String fileName;
	private TextView videoNametextView;
	private String failVideoFileName;
	private String logFileName;
	private ArrayList<String> videofileList;
	private int failFile = 0;
	private int currentPlayStatus = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
		setBundleData();
		initLogFile();
		SdCardPath = TestProcActivity.SDcardPath + "/video/"+logFileName+"/";
		//checking APP be rotated or not.  
		if (savedInstanceState != null) {
			fileIndex = savedInstanceState.getInt("fileIndex");
	    }else
	    	logData("\n\nPlay videos");
		
		if((videofileList = getDirectoryFileList())!=null) {
			playVideo(fileIndex);
		}else {
			logData("Total videos = 0, error = 0");
			logData("Play videos finish");
    		closeWriter();
    		changeActivity();
		}
	}

	@Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        outState.putInt("fileIndex", fileIndex);
        closeWriter();
    }
	
	private void initialize() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_video);
		setControlView();
		setVideoPlayer();
		setVideoViewListener();
		
		videoNametextView = (TextView) this.findViewById(R.id.play_textView);
	}

	private void setVideoPlayer() {
		videoView = (VideoView) this.findViewById(R.id.videoView1);
		MediaController mediaPlayer = new MediaController(this);
		
		mediaPlayer.setPrevNextListeners(new View.OnClickListener() {
			  @Override
			  public void onClick(View v) {//next button clicked
				  currentPlayStatus = PLAY_NEXT;
				  int currentFileIndex = fileIndex;
				  if((++currentFileIndex)>=videofileList.size()) {
					  Toast.makeText(getApplicationContext(), "This is final video", Toast.LENGTH_SHORT).show();
				  }else{
					  videoView.stopPlayback();
					  playVideo(++fileIndex);
				  }
			  }
			}, new View.OnClickListener() {
			  @Override
			  public void onClick(View v) {//previous button clicked
				  currentPlayStatus = PLAY_PREV;
				  int currentFileIndex = fileIndex;
				  if(--currentFileIndex<0) {
					  Toast.makeText(getApplicationContext(), "This is first video", Toast.LENGTH_SHORT).show();
				  }else {
					  videoView.stopPlayback();
					  playVideo(--fileIndex);
				  }
			  }
		});
		videoView.setMediaController(mediaPlayer);
		currentPlayStatus = PLAY_NEXT;
	}
	
	private void setVideoViewListener() {
		videoView.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mc) {
				currentPlayStatus = PLAY_NEXT;
            	if(fileIndex==(videofileList.size()-1)) {
            		logData("Total videos = " + videofileList.size() + ", error = " + failFile);
					logData("Play videos finish");
            		closeWriter();
            		changeActivity();
            		return;
            	}
            	playVideo(++fileIndex);
            }
        });  
		
		videoView.setOnErrorListener(new OnErrorListener() {
			@Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
				Toast.makeText(getApplicationContext(), "Error play Video: " + fileName, Toast.LENGTH_SHORT).show();
				failFile++;
				logData("Error play Video: " + fileName);
				
				videoView.stopPlayback();
				if(fileIndex==(videofileList.size()-1)) {
					logData("Total videos = " + videofileList.size() + ", error = " + failFile);
					logData("Play videos finish");
            		closeWriter();
            		changeActivity();
				}else if(fileIndex==0) {
					currentPlayStatus = PLAY_NEXT;
					playVideo(++fileIndex);
				}else {
					switch(currentPlayStatus) {
					case PLAY_NEXT:
						playVideo(++fileIndex);
						break;
					case PLAY_PREV:
						playVideo(--fileIndex);
						break;
					}
				}
				return true;
            }
        });
	}
	
	private void playVideo(int index) {
		if(index<0||index==videofileList.size()) {
			Toast.makeText(getApplicationContext(), "Error video file index", Toast.LENGTH_SHORT).show();
			return;
		}
		fileName = videofileList.get(index);
		videoView.setVideoPath(SdCardPath+fileName);
		videoView.requestFocus();
		videoView.start();
		sendtoTextView(fileName);
		logData("Video: "+fileName);
	}
	
	private ArrayList<String> getDirectoryFileList() {
		File f = new File(SdCardPath);
		ArrayList<String> fileList = new ArrayList<String>();
		if (f.isDirectory()) {
			String[] s = f.list();
			if(s.length==0)
				return null;
			for (int i = 0; i < s.length; i++) {
				if(s[i].equals(failVideoFileName)) continue;
				fileList.add(s[i]);
			}
		}
		
		if(fileList.size()==0) 
			return null;
		return fileList;
	}
	
	private void sendtoTextView(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				videoNametextView.setText(" " + x);
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
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
			Date date = new Date(System.currentTimeMillis());
			
			logDataWriter.write("["+formatter.format(date)+"] "+log + "\n");
			logDataWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void closeWriter() {
		try {
			logDataWriter.flush();
			logDataWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initLogFile() {
		String path = TestProcActivity.getLogFileDirPath();
		try {
			logDataWriter = new FileWriter(path + "/" + logFileName + ".txt", true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void setControlView() {
		LayoutInflater controlInflater = LayoutInflater.from(getBaseContext());
		View viewControl = controlInflater.inflate(R.layout.control_video, null);
		LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		this.addContentView(viewControl, layoutParamsControl);
	}

	private void changeActivity() {
		Intent intent = new Intent();
		intent.setClass(VideoActivity.this, PhotoActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("logFileName", logFileName);
		intent.putExtras(bundle);
		finish();
		startActivity(intent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(VideoActivity.this).setTitle("Exit").setMessage("Return APP Home screen ?")
					.setIcon(R.drawable.ic_launcher).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//android.os.Process.killProcess(android.os.Process.myPid());
					Intent intent = new Intent();
					intent.setClass(VideoActivity.this, MainActivity.class);
					startActivity(intent);
					VideoActivity.this.finish();

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
