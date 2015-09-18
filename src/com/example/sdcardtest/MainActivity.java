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
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements SurfaceHolder.Callback{

	private SurfaceView surfaceview;// 顯示視頻的控制項
	private MediaRecorder mediarecorder;// 錄製視頻的類
	private SurfaceHolder surfaceHolder;
	private Button startButton;// 開始錄製按鈕
	private Button playButton;//
	private TextView testView;
	private TextView testView2;
	private Camera camera;
	private final String tag = "Video";
	private boolean sdcardExisted = true; 
	private boolean startProc = false;
	private boolean enoughSpace = true;
	private boolean takeFlag = true;
	
	private static String SDcardPath;
	private int VIDEO_LENGTH = 600000;//ms
	private int PIC_NUM = 100;
	private int takeCount=1;  
	private static Semaphore mutex = new Semaphore(1);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.initialize();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		/*requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);*/
		
	   
	}
	
	private void initialize(){
		
		startButton = (Button) this.findViewById(R.id.startButton);
		playButton = (Button) this.findViewById(R.id.playButton);
		surfaceview = (SurfaceView)findViewById(R.id.surfaceViewB);
		testView = (TextView) this.findViewById(R.id.currentTextView);
		testView2 = (TextView) this.findViewById(R.id.totalTextView);
		startButton.setOnClickListener(TestProcedureLister);
		playButton.setOnClickListener(playButtonListener);
	    surfaceHolder = surfaceview.getHolder();
	    surfaceHolder.addCallback(this); 
	    initSDcard();
	}
		

	@Override
	protected void onResume(){
		camera = Camera.open();
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
	
	/*@Override
	protected void onDestroy(){
		if(camera!=null){
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
		}
		super.onDestroy();
	}*/
	
	private View.OnClickListener TestProcedureLister = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			
				if(sdcardExisted){
					if(!startProc){
						startProc = true;
						new Thread(new Runnable() {
				             public void run() {
				            	 try{
				            		 //RecordVideo();
						         	 takePicture();
						         	 startProc = false;
				            	 } catch (Exception e){
				     				// TODO Auto-generated catch block
				            		 //Toast.makeText(MainActivity.this,e.getCause().toString(), Toast.LENGTH_SHORT).show();
				     				e.printStackTrace();
				     			 }
				             }
						}).start();
					}else{
						Toast.makeText(MainActivity.this,"Running..", Toast.LENGTH_SHORT).show();
					}
				}else{
					Toast.makeText(MainActivity.this,"SD card not found", Toast.LENGTH_SHORT).show();
				}
		}
	};
	
	public void RecordVideo(){
		long startTime,durationTime;
		int videoWidth,videoHeight;
		List<Integer> videoSizeList = getPhoneSupportedSizes(0);
		String timeString;
		int count = 1;
		int totalVideoNum = videoSizeList.size()/2;
		for(int i = 0;i< videoSizeList.size(); i+=2){
			if(!hasFreeSize()){
				Log.e(tag, "STOP ");
				break;
			}
			
			videoWidth = videoSizeList.get(i);
			videoHeight = videoSizeList.get(i+1);
			
			if(videoWidth>1280)
				continue;
			startRecord(videoWidth,videoHeight);
			Log.e(tag, "RECORD "+videoWidth+"x"+videoHeight);
			startTime = System.currentTimeMillis();
			durationTime = System.currentTimeMillis() - startTime;
			while(durationTime<VIDEO_LENGTH){
				timeString = String.format("%d min, %d sec", 
					    TimeUnit.MILLISECONDS.toMinutes(durationTime),
					    TimeUnit.MILLISECONDS.toSeconds(durationTime) - 
					    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationTime)));
				try {
					Thread.sleep(1000);
					durationTime = System.currentTimeMillis() - startTime;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				sendtoCurrent("video "+count+"/"+totalVideoNum + ": "+ timeString);
			}
			
			count++;
			stopRecord();
		}
		sendtoStatus("Record video finish");
		Log.e(tag, "Record video finish");
		/*Log.e(tag,"audioBitRate "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).audioBitRate);
		Log.e(tag,"audioChannels "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).audioChannels);
		Log.e(tag,"audioCodec "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).audioCodec);
		Log.e(tag,"audioSampleRate "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).audioSampleRate);
		Log.e(tag,"duration "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).duration);
		Log.e(tag,"fileFormat "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).fileFormat);
		Log.e(tag,"quality "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).quality);
		Log.e(tag,"videoBitRate "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).videoBitRate);
		Log.e(tag,"videoCodec "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).videoCodec);
		Log.e(tag,"videoCodec "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).videoFrameHeight);
		Log.e(tag,"videoFrameHeight "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).videoFrameRate);
		Log.e(tag,"videoCodec "+ CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).videoFrameWidth);*/
		
		//sendtoUI("Finish");
		//Log.e(tag, "Finish");
	}
	
	public void startRecord(int videoWidth,int videoHeight){
		int quality = 0;
		if(videoWidth<720)
			quality = CamcorderProfile.QUALITY_LOW;
		else
			quality = CamcorderProfile.QUALITY_HIGH;
		
		StatFs sdcardSpaceStat = new StatFs(System.getenv("SECONDARY_STORAGE").toString());
		//StatFs sdcardSpaceStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		mediarecorder = new MediaRecorder();// 創建mediarecorder物件
		
		camera.stopPreview();
		camera.unlock();
		mediarecorder.setCamera(camera);
		mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		//Log.e(tag, "setVideoSource");
		mediarecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediarecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mediarecorder.setVideoFrameRate(CamcorderProfile.get(quality).videoFrameRate);
        mediarecorder.setVideoEncodingBitRate(CamcorderProfile.get(quality).videoBitRate);
        mediarecorder.setAudioEncodingBitRate(CamcorderProfile.get(quality).audioBitRate);
        mediarecorder.setAudioChannels(CamcorderProfile.get(quality).audioChannels);
        mediarecorder.setAudioSamplingRate(CamcorderProfile.get(quality).audioSampleRate);
        mediarecorder.setMaxFileSize(sdcardSpaceStat.getAvailableBlocks()* sdcardSpaceStat.getBlockSize());
        mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mediarecorder.setVideoSize(videoWidth,videoHeight);
		mediarecorder.setOrientationHint(90);
		/*mediarecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QCIF));
		 * QUALITY_HIGH > QUALITY_CIF = QUALITY_QVGA > QUALITY_QCIF = QUALITY_LOW
		 */
		// HTC M9(1-5) OK, SONY (1,4,5) OK
	
		//Log.e(tag, "setVideoEncoder");
		mediarecorder.setOutputFile(createVideoFilePath());
		//Log.e(tag, "setOutputFile");
		mediarecorder.setPreviewDisplay(surfaceHolder.getSurface());
		//Log.e(tag, "setPreviewDisplay");
		try {
			// 準備錄製
			mediarecorder.prepare();
			//Log.e(tag, "prepare()");
			// 開始錄製
			mediarecorder.start();
			//Log.e(tag, "start()");
		}catch (Exception e) {
			// TODO Auto-generated catch block
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
		
		//int count = 1;
		
		//int totalpicResolutionNum = picSizeList.length/2;
		for(int i = 0;i< picSizeList.size(); i+=2){
			
			/*if(!hasFreeSize()){
				break;
			}*/
			
			picWidth = picSizeList.get(i);
			picHeight = picSizeList.get(i+1);
			Log.e(tag,picWidth+"x"+ picHeight);
			
			//sendtoCurrent("Resolution: "+picWidth+"x"+picHeight);
			takePictureProc(picWidth,picHeight);
				
				
		}
		sendtoStatus("Take picture finish");
		//count++;
		
	}
	
	public void takePictureProc(int picWidth,int picHeight){
		
		if(camera==null)
			return;
		
		Camera.Parameters params =camera.getParameters();
		params.setPictureFormat(ImageFormat.JPEG);
		params.setPictureSize(picWidth,picHeight);
		params.setRotation(90);
		camera.setParameters(params);
		takeCount = 1;
		enoughSpace = true;
		
		
		//sendtoUI("Resolution "+picWidth+"x"+ picHeight);
		while(takeCount<=PIC_NUM){
			try {
				//Log.e(tag, "lock()");
				mutex.acquire();
				takeFlag = true;
				if(!enoughSpace) break;
				
				sendtoCurrent(picWidth+"x"+picHeight+" "+(takeCount++));
				camera.setPreviewCallback(null);
				camera.takePicture(null,null,jpeg);
				
				//Log.e(tag, "Lsew "+takeFlag);
			} catch (RuntimeException e){
				//Toast.makeText(MainActivity.this,"Camera Failed", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				Log.e(tag, "ACQUIRE  InterruptedException");
				
				e.printStackTrace();
			}
			
			
				
		}
		
		while(takeFlag);
		
		
		Log.e(tag, "finish");
	}
	
	private PictureCallback jpeg = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			File file = null;
			FileOutputStream outStream = null;
			/*Bitmap bmTmp = BitmapFactory.decodeByteArray(data, 0, data.length);
			Matrix matrix=new Matrix();
			matrix.reset();
			matrix.postRotate(90);
			Bitmap bm = Bitmap.createBitmap(bmTmp, 0, 0, bmTmp.getWidth(), bmTmp.getHeight(), matrix, true);*/
			try {
				file  = createPictureFile();
				outStream = new FileOutputStream(file);
				outStream.write(data);
				outStream.close();
				//BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));  
				//bm.compress(Bitmap.CompressFormat.JPEG,80,bos);
				//bos.flush();
				//bos.close();
				//Log.e(tag, "startPreview");
				camera.startPreview();
				//Log.e(tag, "take "+takeCount);
				//camera.setPreviewCallback(pc);
				
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				
				if(e.getCause().toString().indexOf("ENOSPC")!=-1){
					enoughSpace = false;
					file.delete();
					Log.e(tag, "pic: SD card no space ");
					//sendtoStatus("SD card no space");
				}
				
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RuntimeException e){
				//Toast.makeText(MainActivity.this,"Camera Failed", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			} catch (Exception e){
				Log.e(tag,"Exception:" +e.getCause().toString());
				e.printStackTrace();
			} finally {
				//Log.e(tag, "unlock()");
				mutex.release();
				takeFlag = false;
			}
		}
	};
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		  // TODO Auto-generated method stub
		Camera.Parameters parameters = camera.getParameters();
		parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO); 
		
		camera.setParameters(parameters);
		camera.startPreview();
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	
	  // TODO Auto-generated method stub
		
		try {
			camera.setPreviewDisplay(surfaceHolder);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	  // TODO Auto-generated method stub 
	}	 
	
	public void initSDcard() {
		getExternalFilesDir(null);
		SDcardPath = System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/";
		/*File appDir = new File(SDcardPath);
		if(!appDir.exists()) {
			sdcardExisted = false;
			Toast.makeText(this,"SD card not found", Toast.LENGTH_SHORT).show();
			return;
		}*/
		
		try {
			FileWriter fw = new FileWriter(SDcardPath+"data.txt",true);
		} catch (FileNotFoundException e) {
			sdcardExisted = false;
			Toast.makeText(this,"SD card not found", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Log.e(tag,"final "+SDcardPath);
		/*try {
			FileWriter fw = new FileWriter(SDcardPath+"xxqqaa.txt",true);
			fw.write("hello");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
	}
	
	public String createVideoFilePath(){
		File appDir = new File(SDcardPath, "video"); //指定資料夾
		if(!appDir.exists()) appDir.mkdir();
		String name = System.currentTimeMillis() +".mp4"; //檔案以系統時間來命名
		
		if(!appDir.exists()) 
			return createVideoFilePath();
		
		return new File(appDir, name).getAbsolutePath();
	}
	
	public File createPictureFile(){
		
		File appDir = new File(SDcardPath, "picture"); //指定資料夾
		if(!appDir.exists()) appDir.mkdir();
		String name = System.currentTimeMillis() +".jpg"; //檔案以系統時間來命名
		return new File(appDir, name);
	}
	
	public List<Integer> getPhoneSupportedSizes(int type){
		List<Size> a;
		double size1 = 16.0/9.0;
		double size2 = 4.0/3.0;
		double size3 = 11.0/9.0;
		List<Integer> intList = new ArrayList<Integer>();
		if(type==0){
			a = camera.getParameters().getSupportedVideoSizes();
			for(int i = 0;i< a.size();i++){
				double tmp = (double)a.get(i).width/(double)a.get(i).height;
				//Log.e(tag, "tmp "+tmp +"size1 "+size1+"size2 "+size2+"size3 "+size3);
				if((tmp==size1)||(tmp==size2)||(tmp==size3)){
					intList.add(a.get(i).width);
					intList.add(a.get(i).height);
					//Log.e(tag, "SupportSize "+a.get(i).width+"x"+a.get(i).height);
				}
			}
		}else{
			a = camera.getParameters().getSupportedPictureSizes();
			for(int i = 0;i< a.size();i++){
				intList.add(a.get(i).width);
				intList.add(a.get(i).height);
			}
		}
				
			//Log.e(tag, "videoSize = " +a.get(i).width +"x"+a.get(i).height);
		
		return intList;
	}
	
	public void sendtoCurrent(final String x){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	testView.setText(x);
            }
        });
    }
	
	public void sendtoStatus(final String x){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	testView2.append(x+"\n");
            }
        });
    }
	
	public void toast(final String x){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	Toast.makeText(MainActivity.this,x, Toast.LENGTH_SHORT).show();
            }
        });
    }
	
	public boolean hasFreeSize(){
		
		StatFs sdcardSpaceStat = new StatFs(System.getenv("SECONDARY_STORAGE").toString());
		float sdAvailSize = sdcardSpaceStat.getAvailableBlocks()* (sdcardSpaceStat.getBlockSize() / (1048576f));
	   // long sdAvailSize = (long)stat.getAvailableBlocksLong(); //1048576 MB
	    Log.e(tag, "NOW "+ sdAvailSize);
	    if(sdAvailSize<=1){
	    	sendtoStatus("SD card no space");
	    	return false;
	    }else
	    	return true;
	    
	}
	
	public ArrayList<String> getDirectoryFileList(){
		String SDcardPath = System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/video";
		File f = new File(SDcardPath);
        ArrayList<String> fileList = new ArrayList<String>();
        if(f.isDirectory()) {
            Log.e(tag,"filename : "+f.getName()); 
            String []s=f.list();
          
            for(int i=0;i<s.length;i++){
                //System.out.println(s[i]);
                fileList.add(s[i]);
            }
        }
        
        return fileList;
	}
	
	private OnClickListener playButtonListener = new OnClickListener() {
	    @Override
	    public void onClick(View v) {
	        // TODO Auto-generated method stub
	        //Switch to config page
	    	if(sdcardExisted){
		    	if(getDirectoryFileList().size()==0)
		    		Toast.makeText(MainActivity.this,"No video file", Toast.LENGTH_SHORT).show();
		    	else{
		    		if(!startProc){
		    			Toast.makeText(MainActivity.this,"Running..", Toast.LENGTH_SHORT).show();
		    			Intent intent = new Intent();
				        intent.setClass(MainActivity.this, PlayActivity.class);
				        startActivity(intent);
		    			
		    		}else{
		    			Toast.makeText(MainActivity.this,"Running..", Toast.LENGTH_SHORT).show();
		    		}
		    	}
	    	}else{
				Toast.makeText(MainActivity.this,"SD card not found", Toast.LENGTH_SHORT).show();
			}
	    }
	};
}
