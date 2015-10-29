package com.example.sdcardtest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import android.app.Activity;
import android.app.ActivityManager;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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

public class TestProcActivity extends Activity implements SurfaceHolder.Callback {

	private SurfaceView surfaceview;
	private MediaRecorder mediarecorder;
	private SurfaceHolder surfaceHolder;
	private Button startButton;
	private Button playButton;
	private TextView textView;
	private ProgressBar progressBar;
	private Camera camera;
	private Configuration phoneConfigure;
	private FileWriter logDataWriter = null;
	private static Semaphore mutex = new Semaphore(0);
	private boolean sdcardExisted = true;
	private boolean startProc = false;
	private boolean enoughSpace = true;
	private boolean handlePic = true;
	private static String SDcardPath;
	private final String tag = "VideoTest";
	private int VIDEO_LENGTH = 600000;// ms
	private int PIC_NUM = 100;
	//private int totalPicCount = 1;
	//private char prefix = 'A';
	private static int progressCount = 1;
	private int videoFileIndex;
	private int photoFileIndex;
	private int procPart = 0;
	private String failVideoFileName = null;
	private String logFileName;
	Handler handler=new Handler();
	//private boolean takeFlag = true;
	//private int testCount = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setBundleData();
		initialize();
	}

	private void initialize() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_testproc_landscape);
		surfaceview = (SurfaceView) findViewById(R.id.surfaceView1);
		surfaceHolder = surfaceview.getHolder();
		surfaceHolder.addCallback(this);
		LayoutInflater controlInflater = LayoutInflater.from(getBaseContext());
		phoneConfigure = getResources().getConfiguration();
		if (phoneConfigure.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			View viewControl = controlInflater.inflate(R.layout.control_landscape, null);
			LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			this.addContentView(viewControl, layoutParamsControl);
			startButton = (Button) this.findViewById(R.id.control2_startButton);
			playButton = (Button) this.findViewById(R.id.control2_playButton);
			progressBar = (ProgressBar) findViewById(R.id.control2_progressBar);
			textView = (TextView) this.findViewById(R.id.control2_textView);
		} else if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT) {
			View viewControl = controlInflater.inflate(R.layout.control_portrait, null);
			LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			this.addContentView(viewControl, layoutParamsControl);
			startButton = (Button) this.findViewById(R.id.control_startButton);
			playButton = (Button) this.findViewById(R.id.control_playButton);
			progressBar = (ProgressBar) findViewById(R.id.control_progressBar);
			textView = (TextView) this.findViewById(R.id.control_textView);
		}
		startButton.setOnClickListener(TestProcedureListener);
		playButton.setOnClickListener(playButtonListener);
		playButton.setVisibility(View.INVISIBLE);
		initSDcard();
	}

	@Override
	protected void onResume() {
		camera = Camera.open();
		if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT)
			camera.setDisplayOrientation(90);
		super.onResume();
	}

	@Override
	protected void onPause() {
		camera.stopPreview();
		camera.setPreviewCallback(null);
		camera.release();
		super.onPause();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!startProc)
				android.os.Process.killProcess(android.os.Process.myPid());

			new AlertDialog.Builder(TestProcActivity.this).setTitle("Exit").setMessage("Exit Application ?")
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

	private View.OnClickListener TestProcedureListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			initSDcard();
			if (sdcardExisted) {
				if (!startProc) {
					startProc = true;
					new Thread(new Runnable() {
						public void run() {
							try {
								initLogFile();
								if (procPart == 2)
									while (enoughSpace) {
										startProc = true;
										testProc();
									}
								else
									testProc();
								closeLogFile();
								changeActivity();
							} catch (Exception e) {
								logData("TestProcedureListener: "+e);
								e.printStackTrace();
							}
						}
					}).start();
				} else {
					Toast.makeText(TestProcActivity.this, "Running..", Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(TestProcActivity.this, "SD card not found", Toast.LENGTH_SHORT).show();
			}
		}
	};

	private void testProc() {
		logData("Start test part " + procPart);
		setPhoneRotation();
		setProgressBar();
		setFileIndex();
		RecordVideo();
		takePicture();
		// toast("Progress " + progressCount);
		logData("Test part "+procPart+" finish");
		finishProc();

	}

	public void RecordVideo() {
		logData("Record Video");
		String fileName = null;
		int tmpVideoLength = VIDEO_LENGTH;
		long startTime, durationTime;
		int videoWidth, videoHeight;
		List<Integer> videoSizeList = getPhoneSupportedSizes(0);
		String timeString = null;
		for (int i = 0; i < videoSizeList.size(); i += 2) {
			if (!hasFreeVideoSize()) {
				progressCount = videoSizeList.size() / 2;
				sendtoProgress(progressCount++);
				// toast("RecordVideo() "+progressCount);
				break;
			}

			videoWidth = videoSizeList.get(i);
			videoHeight = videoSizeList.get(i + 1);
			fileName = startRecord(videoWidth, videoHeight);
			Log.e(tag, "RECORD " + videoWidth + "x" + videoHeight);

			startTime = System.currentTimeMillis();
			durationTime = System.currentTimeMillis() - startTime;
			while (durationTime < VIDEO_LENGTH) {
				timeString = String.format("%02d : %02d", TimeUnit.MILLISECONDS.toMinutes(durationTime),
						TimeUnit.MILLISECONDS.toSeconds(durationTime)
								- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationTime)));
				try {

					if (!hasFreeVideoSize()) {
						failVideoFileName = fileName;
						logData("[" + videoWidth + "x" + videoHeight + "] file name " + fileName
								+ " suspend: no enough space");
						break;
					}

					Thread.sleep(1000);
					durationTime = System.currentTimeMillis() - startTime;
				} catch (Exception e) {
					logData("RecordVideo(): " + e);
					e.printStackTrace();
				}
				sendtoCurrent(videoWidth + "x" + videoHeight + "  " + timeString);
			}
			timeString = String.format("%02d : %02d", TimeUnit.MILLISECONDS.toMinutes(durationTime),
					TimeUnit.MILLISECONDS.toSeconds(durationTime)
							- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationTime)));
			logData("[" + videoWidth + "x" + videoHeight + "] record time = " + timeString);
			VIDEO_LENGTH = tmpVideoLength;
			sendtoProgress(progressCount++);
			stopRecord();

		}

		toast("Record video finish");
	}

	public String startRecord(int videoWidth, int videoHeight) {
		String fileName = null;
		int quality = 0;
		String fileExtention;
		try {
			if (videoWidth <= 176) {
				quality = CamcorderProfile.QUALITY_LOW;
				fileExtention = ".3gp";
			} else {
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
			mediarecorder.setVideoSize(videoWidth, videoHeight);
			if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT)
				mediarecorder.setOrientationHint(90);
			fileName = createVideoFilePath(fileExtention);
			mediarecorder.setOutputFile(SDcardPath + "/video/" + fileName);
			mediarecorder.setPreviewDisplay(surfaceHolder.getSurface());
			mediarecorder.prepare();
			mediarecorder.start();
		} catch (Exception e) {
			mediarecorder.stop();
			mediarecorder.release();
			e.printStackTrace();
			logData("startRecord(): " + e);
			//toast(e.getStackTrace().toString());
		}

		return fileName;
	}

	public void stopRecord() {
		try {
			if (mediarecorder != null) {
				mediarecorder.stop();
				mediarecorder.release();
				mediarecorder = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logData("stopRecord(): " + e);
			
			//toast(e.getStackTrace().toString());
		}

	}

	public void takePicture() {
		logData("Take Picture");
		int picWidth, picHeight;
		List<Integer> picSizeList = getPhoneSupportedSizes(1);
		for (int i = 0; i < picSizeList.size(); i += 2) {
			int tmp = progressCount;
			picWidth = picSizeList.get(i);
			picHeight = picSizeList.get(i + 1);
			takePictureProc(picWidth, picHeight);
			progressCount = tmp;
			sendtoProgress(progressCount++);
			// toast("takePicture() "+progressCount);
		}

		/*
		 * if(!enoughSpace) toast("SD card no space");
		 */
		toast("Take picture finish");

	}

	public void takePictureProc(int picWidth, int picHeight) {
		
		Camera.Parameters params = camera.getParameters();
		try {
			params.setPictureFormat(ImageFormat.JPEG);
			params.setPictureSize(picWidth, picHeight);
			//params.setRotation(90);
			camera.setParameters(params);
		} catch (Exception e) {
			logData("takePictureProc() Parameters: " + e);
			e.printStackTrace();
			Log.e(tag, "rotation error");
		}
		sendtoCurrent(picWidth + "x" + picHeight + ": 0");
		int takeCount = 1;
		enoughSpace = true;
		handlePic = false;///111111111111111111
		int waitAcquireCount = 0;
		while (takeCount <= PIC_NUM) {
			waitAcquireCount = 0;
			//memData();
			try {
				handlePic = true;
				camera.takePicture(null, null, jpeg);
				
				//mutex.tryAcquire(30L,  TimeUnit.SECONDS);
				//mutex.acquire();
				
				while(!mutex.tryAcquire()){
					Thread.sleep(500);
					waitAcquireCount++;
					if(waitAcquireCount>60){
						mutex.release();
						logData("[" + picWidth + "x" + picHeight + "] take " + takeCount + " timeout");
						//Log.e(tag, "[" + picWidth + "x" + picHeight + "] take " + takeCount + " timeout");
						
						resetCamera();
					}
				}
				
				if(waitAcquireCount>60)
					continue;
					
				camera.startPreview();
				/*handler.postDelayed(timerCount, 0);
				
				mutex.acquire();
				handler.removeCallbacks(timerCount);
				if(!takeFlag){
					Log.e(tag, "ss");
					camera.release();
					camera = Camera.open();
					takeFlag = true;
					continue;
				}*/
					
				// handlePic = true;
				if (!enoughSpace) {
					
					handlePic = false;
					logData("[" + picWidth + "x" + picHeight + "] take " + takeCount + " suspend: No enough space");
					// mutex.release();
					break;
				}
				// camera.takePicture(null, null, jpeg);
				sendtoCurrent(picWidth + "x" + picHeight + ": " + (takeCount++));
				//Log.e(tag, picWidth + "x" + picHeight + ": " + (takeCount-1));
			} catch (Exception e) {
				logData("takePictureProc() takePicture: " + e);
				e.printStackTrace();
			} 
		}
		if (enoughSpace)
			logData("[" + picWidth + "x" + picHeight + "] take " + PIC_NUM);
		
			
		while (handlePic)
			;// wait final takePicture procedure finish

	}
	
	private void resetCamera(){
		camera.release();
		camera = Camera.open();
		try {
			
			if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT)
				camera.setDisplayOrientation(90);
			Camera.Parameters parameters = camera.getParameters();
			parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			camera.setParameters(parameters);
			camera.setPreviewDisplay(surfaceHolder);
			camera.startPreview();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logData("resetCamera(): " + e);
		}
		
	}
	
	/*private Runnable timerCount = new Runnable()  
    {  
        @Override  
        public void run()  
        {  
        	try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	if(!mutex.tryAcquire()){
        		takeFlag = false;
        		mutex.release();
        	}
        		
        }  
    };  */

	private PictureCallback jpeg = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			//Log.e(tag,"onPictureTaken create"+testCount);
			//handlePic = true;//111111111111111111
			File file = null;
			FileOutputStream outStream = null;
			try {

				file = createPictureFilePath();
				outStream = new FileOutputStream(file);
				outStream.write(data);
				//logData("onPictureTaken(): write");
				if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT) {
					ExifInterface exifi = new ExifInterface(file.getAbsolutePath());
					exifi.setAttribute(ExifInterface.TAG_ORIENTATION,
							String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
					exifi.saveAttributes();
					//logData("onPictureTaken(): saveAttributes");
				}

				outStream.close();
				//Log.e(tag,"onPictureTaken finish"+(testCount++));

				//camera.startPreview();
				//logData("onPictureTaken(): startPreview()");
			} catch (FileNotFoundException e) {
				Log.e(tag, "TException:" + e.getCause().toString());
				if (e.getCause().toString().indexOf("ENOENT") != -1) {
					enoughSpace = false;
					file.delete();
				}
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(tag, "TException:" + e.getCause().toString());
				if (e.getCause().toString().indexOf("ENOSPC") != -1) {
					enoughSpace = false;
					file.delete();
				}
				e.printStackTrace();
			}  catch (Exception e) {
				logData("onPictureTaken(): " + e);
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
			logData("surfaceCreated(): " + e);
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
		SDcardPath = System.getenv("SECONDARY_STORAGE") + "/Android/data/com.example.sdcardtest/";
		try {
			FileWriter fw = new FileWriter(SDcardPath + "data.txt", true);

		} catch (FileNotFoundException e) {
			sdcardExisted = false;
			Toast.makeText(this, "SD card not found", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initLogFile() {
		String path = Environment.getExternalStorageDirectory() + "/SDcardTestLog";
		File file = new File(path);
		if (!file.exists())
			file.mkdir();
		long currentTime = System.currentTimeMillis();

		SimpleDateFormat formatter = new SimpleDateFormat("MM:dd:HH:mm");
		Date date = new Date(currentTime);
		logFileName = formatter.format(date);
		try {
			logDataWriter = new FileWriter(path + "/" + logFileName + ".txt", true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logData("initLogFile(): " + e);
			e.printStackTrace();
		}
	}

	private void setFileIndex(){
		File appDir = new File(SDcardPath, "video");
		if (!appDir.exists())
			appDir.mkdir();
		File appDir2 = new File(SDcardPath, "picture");
		if (!appDir2.exists())
			appDir2.mkdir();
		String[] s = appDir.list();
		String[] s2 = appDir2.list();
		
		videoFileIndex = s.length;
		photoFileIndex = s2.length;
		
	}
	
	public String createVideoFilePath(String fileExtention) {

		File appDir = new File(SDcardPath, "video");
		videoFileIndex++;
		String name = "Video" + String.valueOf(videoFileIndex) + fileExtention;
		new File(appDir, name);
		Log.e(tag, "video name " + name);
		if (!appDir.exists())
			return createVideoFilePath(fileExtention);
		return name;
	}

	public File createPictureFilePath() {
		File appDir = new File(SDcardPath, "picture");
		photoFileIndex++;
		String name = "Photo" + String.valueOf(photoFileIndex) + ".jpg";
		//totalPicCount++;
		return new File(appDir, name);
	}

	public List<Integer> getPhoneSupportedSizes(int type) {
		List<Size> a;
		List<Integer> phoneSupportedList = new ArrayList<Integer>();
		try {
			if (type == 0) {
				double size1 = 16.0 / 9.0;
				double size2 = 4.0 / 3.0;
				double size3 = 11.0 / 9.0;
				a = camera.getParameters().getSupportedVideoSizes();
				for (int i = 0; i < a.size(); i++) {
					double tmp = (double) a.get(i).width / (double) a.get(i).height;
					if ((tmp == size1) || (tmp == size2) || (tmp == size3)) {
						phoneSupportedList.add(a.get(i).width);
						phoneSupportedList.add(a.get(i).height);
					}
				}
			} else if (type == 1) {
				a = camera.getParameters().getSupportedPictureSizes();
				for (int i = 0; i < a.size(); i++) {
					phoneSupportedList.add(a.get(i).width);
					phoneSupportedList.add(a.get(i).height);
					// Log.e(tag, a.get(i).width+"x"+a.get(i).height);

				}
			} else {
				toast("Error type");
				return null;
			}
		} catch (Exception e) {
			logData("getPhoneSupportedSizes(): " + e);
			e.printStackTrace();

		}
		return phoneSupportedList;
	}

	public void sendtoCurrent(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textView.setText(" " + x + " ");
			}
		});
	}

	public void sendtoProgress(final int x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				progressBar.setProgress(x);
			}
		});
	}

	public void toast(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(TestProcActivity.this, x, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void logData(String log) {
		try {
			logDataWriter.write(log + "\n");
			logDataWriter.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean hasFreeVideoSize() {

		StatFs sdcardSpaceStat = new StatFs(System.getenv("SECONDARY_STORAGE").toString());

		float sdAvailSize = sdcardSpaceStat.getAvailableBlocks() * (sdcardSpaceStat.getBlockSize() / (1048576f));

		if (sdAvailSize <= 1) {
			toast("Video: SD card no space");
			logData("Video: SD card no space");
			return false;
		} else
			return true;
	}

	public int getDirectoryFileList() {

		File f = new File(SDcardPath + "/video");
		
		if (f.isDirectory()) {
			Log.e(tag, "filename : " + f.getName());
			String[] s = f.list();
			return s.length;
		}
		
		return 0;
	}

	private void setProgressBar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textView.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.VISIBLE);
				List<Integer> videoTotalNum = getPhoneSupportedSizes(0);
				List<Integer> pictureTotalNum = getPhoneSupportedSizes(1);
				while (videoTotalNum.size() == 0 || pictureTotalNum.size() == 0) {
					videoTotalNum = getPhoneSupportedSizes(0);
					pictureTotalNum = getPhoneSupportedSizes(1);
				}

				progressBar.setMax(videoTotalNum.size() / 2 + pictureTotalNum.size() / 2);
				progressBar.setProgress(0);
				logData("ProgressBarMAX = " + progressBar.getMax());
				// toast("MAX " + progressBar.getMax());

			}
		});
	}

	private void setPhoneRotation() {
		if (phoneConfigure.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			logData("ORIENTATION_LANDSCAPE");
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT) {
			logData("ORIENTATION_PORTRAIT");
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

	}

	private void finishProc() {
		if (!enoughSpace) {
			sendtoCurrent("SD card no space");
			logData("SD card no space");
		} else {
			sendtoCurrent("finish");
		}
		//prefix++;
		//totalPicCount = 1;
		progressCount = 1;
		
		startProc = false;
	}
	
	private void closeLogFile(){
		try {
			logDataWriter.flush();
			logDataWriter.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void setBundleData() {
		Bundle bundle = this.getIntent().getExtras();
		procPart = bundle.getInt("part");
	}

	private void changeActivity() {
		Intent intent = new Intent();
		intent.setClass(TestProcActivity.this, PlayActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("failVideoFileName",failVideoFileName);
		bundle.putString("logFileName", logFileName);
		intent.putExtras(bundle);
		startActivity(intent);
		TestProcActivity.this.finish();
	}

	private OnClickListener playButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (sdcardExisted) {
				if (getDirectoryFileList() == 0)
					Toast.makeText(TestProcActivity.this, "No video file", Toast.LENGTH_SHORT).show();
				else {
					if (!startProc) {
						changeActivity();
					} else {
						Toast.makeText(TestProcActivity.this, "Running..", Toast.LENGTH_SHORT).show();
					}
				}
			} else {
				Toast.makeText(TestProcActivity.this, "SD card not found", Toast.LENGTH_SHORT).show();
			}
		}
	};
}

/*class GoodTask extends AsyncTask<Integer, Integer, String> {
    // <肚把计, 矪瞶い穝ざ把计, 矪瞶肚把计>
    int nowCount;
    @Override
    protected String doInBackground(Integer... countTo) {
        // TODO Auto-generated method stub
        // 璉春い矪瞶
        try {
            for (int i = 0; i < countTo[0]; i++) {
                Thread.sleep(1000);

                nowCount = i + 1;
                publishProgress(nowCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "10";
    }

 
}*/
