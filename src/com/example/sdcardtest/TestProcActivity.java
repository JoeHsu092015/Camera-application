package com.example.sdcardtest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class TestProcActivity extends Activity implements SurfaceHolder.Callback{

	private SurfaceView surfaceview;
	private MediaRecorder mediarecorder;
	private SurfaceHolder surfaceHolder;
	private Button startButton;
	private Button playButton;
	private TextView textView;
	private ProgressBar progressBar;
	private Camera camera;
	private Configuration phoneConfigure;
	private static Semaphore mutex = new Semaphore(1);
	private boolean sdcardExisted = true; 
	private boolean startProc = false;
	private boolean enoughSpace = true;
	private boolean handlePic = true;
	private static String SDcardPath;
	private final String tag = "Video";
	private int VIDEO_LENGTH = 120000;//ms
	private int PIC_NUM = 10;
	private int totalPicCount = 1;
	private static int progressCount = 1;
	private char prefix = 'B';
	private int procPart = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setBundleData();
		initialize();
	}
	
	private void initialize(){
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_testproc_landscape);
		surfaceview = (SurfaceView)findViewById(R.id.surfaceView1);
	    surfaceHolder = surfaceview.getHolder();
	    surfaceHolder.addCallback(this); 
	    LayoutInflater controlInflater = LayoutInflater.from(getBaseContext());
	    phoneConfigure=getResources().getConfiguration();  
		if (phoneConfigure.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			View viewControl = controlInflater.inflate(R.layout.control_landscape, null);
			LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			this.addContentView(viewControl, layoutParamsControl);
			startButton = (Button) this.findViewById(R.id.control2_startButton);
			playButton = (Button) this.findViewById(R.id.control2_playButton);
			progressBar = (ProgressBar)findViewById(R.id.control2_progressBar);
			textView = (TextView) this.findViewById(R.id.control2_textView);
		} else if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT){
			View viewControl = controlInflater.inflate(R.layout.control_portrait, null);
			LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			this.addContentView(viewControl, layoutParamsControl);
			startButton = (Button) this.findViewById(R.id.control_startButton);
			playButton = (Button) this.findViewById(R.id.control_playButton);
			progressBar = (ProgressBar)findViewById(R.id.control_progressBar);
			textView = (TextView) this.findViewById(R.id.control_textView);
		}
		startButton.setOnClickListener(TestProcedureLister);
		playButton.setOnClickListener(playButtonListener);
	    initSDcard();
	}
		
	@Override
	protected void onResume(){
		camera = Camera.open();
		if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT)
			camera.setDisplayOrientation(90);
		super.onResume();
	}
	
	@Override
	protected void onPause(){
		camera.stopPreview();
		camera.setPreviewCallback(null);
		camera.release();
		super.onPause();
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(TestProcActivity.this)
	            .setTitle("Exit")
	            .setMessage("Exit Application ?")
	            .setIcon(R.drawable.ic_launcher)
	            .setPositiveButton("Yes",
	                    new DialogInterface.OnClickListener() {
	            	@Override
                    public void onClick(DialogInterface dialog,
                            int which) {
                    	android.os.Process.killProcess(android.os.Process.myPid());
                    }
	            })
	            .setNegativeButton("No",
	            		new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int which) {
                    }
	            }).show();
        }
        return true;
    }
	
	private View.OnClickListener TestProcedureLister = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			initSDcard();
			if(sdcardExisted){
				if(!startProc){
					startProc = true;
					new Thread(new Runnable() {
			             public void run() {
			            	 try{
			            		 if(procPart==2)
				            		 while(enoughSpace)
				 		         		testProc();
			            		 else
			 		         		testProc();
			            	 } catch (Exception e){
			     				e.printStackTrace();
			     			 }
			             }
					}).start();
				}else{
					Toast.makeText(TestProcActivity.this,"Running..", Toast.LENGTH_SHORT).show();
				}
			}else{
				Toast.makeText(TestProcActivity.this,"SD card not found", Toast.LENGTH_SHORT).show();
			}	
		}
	};
	
	private void testProc(){
		setPhoneRotation();
		setProgressBar();
		RecordVideo();
		takePicture();
		//toast("ProcGress"+progressCount);
		finishProc();
	}
	
	public void RecordVideo(){
		
		int tmpVideoLength = VIDEO_LENGTH;
		long startTime,durationTime;
		int videoWidth,videoHeight;
		List<Integer> videoSizeList = getPhoneSupportedSizes(0);
		String timeString;
		for(int i = 0;i< videoSizeList.size(); i+=2){
			if(!hasFreeVideoSize()){
				progressCount = videoSizeList.size()/2;
				sendtoProgress(progressCount++);
				//toast("RecordVideo() "+progressCount);
				break;
			}
			
			videoWidth = videoSizeList.get(i);
			videoHeight = videoSizeList.get(i+1);
			startRecord(videoWidth,videoHeight);
			Log.e(tag, "RECORD "+videoWidth+"x"+videoHeight);
			startTime = System.currentTimeMillis();
			durationTime = System.currentTimeMillis() - startTime;
			
			while(durationTime<VIDEO_LENGTH){
				timeString = String.format("%d : %d", 
					    TimeUnit.MILLISECONDS.toMinutes(durationTime),
					    TimeUnit.MILLISECONDS.toSeconds(durationTime) - 
					    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationTime)));
				try {
					
					if(!hasFreeVideoSize())
						break;
					Thread.sleep(1000);
					durationTime = System.currentTimeMillis() - startTime;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				sendtoCurrent(videoWidth+"x"+videoHeight+"  "+timeString);
			}
			VIDEO_LENGTH = tmpVideoLength;
			sendtoProgress(progressCount++);
			stopRecord();
			
		}
		
		toast("Record video finish");
	}
	
	public void startRecord(int videoWidth,int videoHeight){
		int quality = 0;
		String fileExtention;
		if(videoWidth<=176){
			quality = CamcorderProfile.QUALITY_LOW;
			fileExtention = ".3gp";
		}else{
			quality = CamcorderProfile.QUALITY_HIGH;
			fileExtention = ".mp4";
		}
		mediarecorder = new MediaRecorder();
		camera.stopPreview();
		camera.unlock();
		mediarecorder.setCamera(camera);
		mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mediarecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediarecorder.setProfile(CamcorderProfile.get(quality));
		mediarecorder.setVideoSize(videoWidth,videoHeight);
		if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT)
			mediarecorder.setOrientationHint(90);
		mediarecorder.setOutputFile(createVideoFilePath(fileExtention));
		mediarecorder.setPreviewDisplay(surfaceHolder.getSurface());
		try {
			mediarecorder.prepare();
			mediarecorder.start();
		}catch (Exception e) {
			mediarecorder.stop();
			mediarecorder.release();
			e.printStackTrace();
			toast(e.getCause().toString());
		}
	}
	
	public void stopRecord(){
		
		if (mediarecorder != null) {
			mediarecorder.stop();
			mediarecorder.release();
			mediarecorder = null;
		}
	
	}
	
	public void takePicture(){
		
		int picWidth,picHeight;
		List<Integer> picSizeList = getPhoneSupportedSizes(1);
		for(int i = 0;i< picSizeList.size(); i+=2){
			int A = progressCount;
			picWidth = picSizeList.get(i);
			picHeight = picSizeList.get(i+1);
			takePictureProc(picWidth,picHeight);
			progressCount = A;
			sendtoProgress(progressCount++);
			//toast("takePicture() "+progressCount);
		}
		
		if(!enoughSpace)
			toast("SD card no space");
		toast("Take picture finish");
		
	}
	
	public void takePictureProc(int picWidth,int picHeight){
		
		Camera.Parameters params =camera.getParameters();
		try{
			params.setPictureFormat(ImageFormat.JPEG);
			params.setPictureSize(picWidth,picHeight);
			camera.setParameters(params);
		} catch (IllegalArgumentException e){
			Log.e(tag, "rotation error");
		}
		
		int takeCount = 1;
		enoughSpace = true;
		
		while(takeCount<=PIC_NUM){
			try {
				mutex.acquire();
				handlePic = true;
				if(!enoughSpace) {
				
					handlePic = false;
					mutex.release();
					break;
				}
				camera.takePicture(null,null,jpeg);
			
				sendtoCurrent(picWidth+"x"+picHeight+": "+(takeCount++));
			} catch (RuntimeException e){
				e.printStackTrace();
			} catch (InterruptedException e) {
				Log.e(tag, "281: InterruptedException");
				e.printStackTrace();
			}
		}
		
		while(handlePic); //wait final takePicture procedure finish
		
	}
	
	private PictureCallback jpeg = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
		
			File file = null;
			FileOutputStream outStream = null;
			try {
				
				file  = createPictureFilePath();
				outStream = new FileOutputStream(file);
				outStream.write(data);
				outStream.close();
				if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT){
					ExifInterface exifi = new ExifInterface(file.getAbsolutePath());
					exifi.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
					exifi.saveAttributes();
					
				}
				camera.startPreview();
			
			} catch (FileNotFoundException e) {
				Log.e(tag,"TException:" +e.getCause().toString());
				if(e.getCause().toString().indexOf("ENOENT")!=-1){
					enoughSpace = false;
					file.delete();
				}
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(tag,"TException:" +e.getCause().toString());
				if(e.getCause().toString().indexOf("ENOSPC")!=-1){
					enoughSpace = false;
					file.delete();
				}
				e.printStackTrace();
			} catch (RuntimeException e){
				e.printStackTrace();
			} catch (Exception e){
				Log.e(tag,"TException:" +e.getCause().toString());
				e.printStackTrace();
			} finally {
				handlePic = false;
				mutex.release();
				
			}
		}
	};
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Camera.Parameters parameters = camera.getParameters();
		parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO); 
		camera.setParameters(parameters);
		camera.startPreview();
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			camera.setPreviewDisplay(surfaceHolder);
		} catch (IOException e) {
			camera.release();
			camera = null;
			e.printStackTrace();
		}
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}	 
	
	public void initSDcard() {
		sdcardExisted = true;
		getExternalFilesDir(null);
		SDcardPath = System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/";
		try {
			FileWriter fw = new FileWriter(SDcardPath+"data.txt",true);
		} catch (FileNotFoundException e) {
			sdcardExisted = false;
			Toast.makeText(this,"SD card not found", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String createVideoFilePath(String fileExtention){
		File appDir = new File(SDcardPath, "video");
		if(!appDir.exists()) appDir.mkdir();
		String name = prefix+ String.valueOf(progressCount) +fileExtention; 
		Log.e(tag, "video name "+name);
		if(!appDir.exists()) 
			return createVideoFilePath(fileExtention);
		return new File(appDir, name).getAbsolutePath();
	}
	
	public File createPictureFilePath(){
		File appDir = new File(SDcardPath, "picture"); 
		if(!appDir.exists()) appDir.mkdir();
		String name = prefix+ String.valueOf(totalPicCount) +".jpg";
		totalPicCount++;
		return new File(appDir, name);
	}
	
	public List<Integer> getPhoneSupportedSizes(int type){
		List<Size> a;
		List<Integer> phoneSupportedList = new ArrayList<Integer>();
		try {
			if(type==0){
				double size1 = 16.0/9.0;
				double size2 = 4.0/3.0;
				double size3 = 11.0/9.0;
				a = camera.getParameters().getSupportedVideoSizes();
				for(int i = 0;i< a.size();i++){
					double tmp = (double)a.get(i).width/(double)a.get(i).height;
					if((tmp==size1)||(tmp==size2)||(tmp==size3)){
						phoneSupportedList.add(a.get(i).width);
						phoneSupportedList.add(a.get(i).height);
					}
				}
			}else if(type==1){
				a = camera.getParameters().getSupportedPictureSizes();
				for(int i = 0;i< a.size();i++){
					phoneSupportedList.add(a.get(i).width);
					phoneSupportedList.add(a.get(i).height);
				}
			}else{
				toast("Error type");
				return null;
			}
		} catch (Exception e){
			e.getStackTrace();
		
		}
		return phoneSupportedList;
	}
	
	public void sendtoCurrent(final String x){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	textView.setText(" "+x+" ");
            }
        });
    }
	
	public void sendtoProgress(final int x){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	progressBar.setProgress(x);
            }
        });
    }
	
	public void toast(final String x){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	Toast.makeText(TestProcActivity.this,x, Toast.LENGTH_SHORT).show();
            }
        });
    }
	
	public boolean hasFreeVideoSize(){
		
		StatFs sdcardSpaceStat = new StatFs(System.getenv("SECONDARY_STORAGE").toString());
		
		float sdAvailSize = sdcardSpaceStat.getAvailableBlocks()* (sdcardSpaceStat.getBlockSize() / (1048576f));
		
	    if(sdAvailSize<=1){
	    	toast("Video: SD card no space");
	    	return false;
	    }else
	    	return true;
	}
	
	public ArrayList<String> getDirectoryFileList(){
	
		File f = new File(SDcardPath+"/video");
        ArrayList<String> fileList = new ArrayList<String>();
        if(f.isDirectory()) {
            Log.e(tag,"filename : "+f.getName()); 
            String []s=f.list();
            for(int i=0;i<s.length;i++){
                fileList.add(s[i]);
            }
        }
        return fileList;
	}
	
	private void setProgressBar(){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	textView.setVisibility(View.VISIBLE);
            	progressBar.setVisibility(View.VISIBLE);
        		List<Integer> videoTotalNum = getPhoneSupportedSizes(0);
        		List<Integer> pictureTotalNum = getPhoneSupportedSizes(1);
        		
        		progressBar.setMax(videoTotalNum.size()/2+pictureTotalNum.size()/2);
        		progressBar.setProgress(0);
        		progressCount = 1;
        		//toast("MAX " +progressBar.getMax());
        		
            }
		});
	}
	
	private void setPhoneRotation(){
		if (phoneConfigure.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		
	}
	
	private void finishProc(){
		sendtoCurrent("finish");
		prefix++;
		totalPicCount = 1;
		startProc = false;
	}
	
	private void setBundleData(){
		 Bundle bundle =this.getIntent().getExtras();
		 procPart = bundle.getInt("part");
	}
	
	private OnClickListener playButtonListener = new OnClickListener() {
	    @Override
	    public void onClick(View v) {
	    	if(sdcardExisted){
		    	if(getDirectoryFileList().size()==0)
		    		Toast.makeText(TestProcActivity.this,"No video file", Toast.LENGTH_SHORT).show();
		    	else{
		    		if(!startProc){
		    			Intent intent = new Intent();
				        intent.setClass(TestProcActivity.this, PlayActivity.class);
				        startActivity(intent);
				        TestProcActivity.this.finish();
		    		}else{
		    			Toast.makeText(TestProcActivity.this,"Running..", Toast.LENGTH_SHORT).show();
		    		}
		    	}
	    	}else{
				Toast.makeText(TestProcActivity.this,"SD card not found", Toast.LENGTH_SHORT).show();
			}
	    }
	};
}
