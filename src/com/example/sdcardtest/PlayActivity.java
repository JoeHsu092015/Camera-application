package com.example.sdcardtest;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class PlayActivity extends Activity {

	
	private final String tag = "PlayActivity";
	private String FilePATH ;
	private VideoView videoView;
	private int fileIndex = 0;
	private String fileName;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		/*requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);*/
		videoView = (VideoView) this.findViewById(R.id.videoView1);
		MediaController mc = new MediaController(this);
		videoView.setMediaController(mc);
		FilePATH = System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/video/";

		playVideo();
	}
	
	public void playVideo(){
		
		final ArrayList<String> fileList = getDirectoryFileList();
		fileName = fileList.get(fileIndex++);
		videoView.setVideoPath(FilePATH+fileName);
		videoView.requestFocus();
		videoView.start();
		Toast.makeText(getApplicationContext(), "Video 1/"+fileList.size(), Toast.LENGTH_LONG).show();
		
		videoView.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mc) {
            	if(fileIndex==fileList.size()){
            		changeActivity();
            		return;
            	}
            	fileName = fileList.get(fileIndex);
                videoView.setVideoPath(FilePATH+fileName);
    			videoView.requestFocus();
    			videoView.start();
    			Toast.makeText(getApplicationContext(), "Video "+(fileIndex+1)+"/"+fileList.size() , Toast.LENGTH_LONG).show();
    			fileIndex++;
            }
        });  
			
		videoView.setOnErrorListener(new OnErrorListener() {
			@Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
				Toast.makeText(getApplicationContext(), "Error play Video: "+fileName, Toast.LENGTH_LONG).show();
		        return false;//continue.   true=stop
            }
        });
		
	}
	
	public ArrayList<String> getDirectoryFileList(){
		String SDcardPath = System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/video";
		File f = new File(SDcardPath);
        ArrayList<String> fileList = new ArrayList<String>();
        if(f.isDirectory()){
            String []s=f.list();
            for(int i=0;i<s.length;i++) {
                fileList.add(s[i]);
            }
        }
        return fileList;
	}
	
	public void changeActivity(){
		Intent intent = new Intent();
        intent.setClass(PlayActivity.this, PhotoActivity.class);
        finish();
        startActivity(intent);
	}
}
