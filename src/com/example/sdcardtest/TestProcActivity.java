package com.example.sdcardtest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
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
import android.os.Build;
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

public class TestProcActivity extends Activity implements SurfaceHolder.Callback {

	private SurfaceView surfaceview;
	private MediaRecorder mediarecorder;
	private SurfaceHolder surfaceHolder;
	private Button startButton;
	private Button playButton;
	private TextView testProcStatusTextView;
	private TextView availableSpaceTextView;
	private TextView totalSpaceTextView;
	private ProgressBar progressBar; // Test procedure progress bar
	private Camera camera;
	private Configuration phoneConfigure;
	private FileWriter logDataWriter = null; // Test procedure log writer
	private final int VIDEO_TYPE = 0;
	private final int PHOTO_TYPE = 1;
	/*
	* when takePicture API processing,mutex will be locked until
	* takePicture API process finish
	 */
	private static Semaphore mutex = new Semaphore(0);//takePicture API mutex
	private static Semaphore mutexSuspendTestProc = new Semaphore(0);//user click suspend test proc mutex
	private boolean sdcardExisted = true; // check sdcard exist flag
	private boolean startProc = false; // start Test procedure flag
	private boolean enoughSpace = true; // check sdcard capacity flag
	private boolean handlePic = true; // taking photo flag
	private boolean testErrorOccurs = false;
	private boolean suspendTestProc = false;
	public static String SDcardPath; // SDcard's system path
	private final String tag = "TestProc"; // debug tag
	private int VIDEO_LENGTH = 600000;// ms // record video length
	private int PIC_NUM = 100; // take photo number
	private static int progressCount = 1; // show current process on progressBar
	private int videoFileIndex; // video file name format:("video"+videoFileIndex).mp4/3gp
	private int photoFileIndex; // photo file name format:("photo"+photoFileIndex).jpg
	private int procPart = 0; // test Procedure Part I/II
	private int testPattenType = 0;//test pattern type
	private String sdcardSetting;// sd card is internal storage or not
	private CharSequence[] userChoiceVideoResolutionArray; // user selected test's video resolution
	private CharSequence[] userChoicePhotoResolutionArray; // user selected test's photo resolution
	private String failVideoFileName = null; // store failure video File name when recording video failure happened.
											//The failure reason is SD card capacity not enough.
	private String logFileName; // Test procedure log file name
	private String notTestedVideoResolutionString;// not tested video resolution list
	private String notTestedPhotoResolutionString;// not tested photo resolution list
    private int currentPhotoWidth;//current test photo width resolution
    private int currentPhotoHeight;//current test photo height resolution
    private DecimalFormat doubleFormat;//show sd card storage space format
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getBundleData();
		initialize();
		initSDcard();

		if(sdcardExisted) {
			//get total SD card size
			StatFs sdcardSpaceStat = new StatFs(SDcardPath.substring(0, SDcardPath.indexOf("/Android/")));
			sendtoTextView(totalSpaceTextView," total: "+
					doubleFormat.format((sdcardSpaceStat.getBlockCount() * (sdcardSpaceStat.getBlockSize() / (1048576f)))/1024f)+
			" GB ");
			//get current available SD card size
			hasFreeRemainSpace();
		}
	}
		
	//Test procedure widget setting
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
			//phone landscape orientation control widget setting
			View viewControl = controlInflater.inflate(R.layout.control_testproc_landscape, null);
			LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			this.addContentView(viewControl, layoutParamsControl);
			startButton = (Button) this.findViewById(R.id.control2_startButton);
			playButton = (Button) this.findViewById(R.id.control2_playButton);
			progressBar = (ProgressBar) findViewById(R.id.control2_progressBar);
			testProcStatusTextView = (TextView) this.findViewById(R.id.control2_textView);
			availableSpaceTextView = (TextView) this.findViewById(R.id.control2_availableSpaceTextView);
			totalSpaceTextView = (TextView) this.findViewById(R.id.control2_totalSpaceTextView);
		} else if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT) {
			//phone portrait orientation control widget setting
			View viewControl = controlInflater.inflate(R.layout.control_testproc_portrait, null);
			LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			this.addContentView(viewControl, layoutParamsControl);
			startButton = (Button) this.findViewById(R.id.control_startButton);
			playButton = (Button) this.findViewById(R.id.control_playButton);
			progressBar = (ProgressBar) findViewById(R.id.control_progressBar);
			testProcStatusTextView = (TextView) this.findViewById(R.id.control_textView);
			availableSpaceTextView = (TextView) this.findViewById(R.id.control_availableSpaceTextView);
			totalSpaceTextView = (TextView) this.findViewById(R.id.control_totalSpaceTextView);
		}
		startButton.setOnClickListener(TestProcedureListener);
		playButton.setOnClickListener(playButtonListener);
		playButton.setVisibility(View.INVISIBLE);
		doubleFormat = new DecimalFormat("#.##");
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
	
	//when user click keyboard back or exit
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(TestProcActivity.this).setTitle("Exit").setMessage("Return APP Home screen ?")
					.setIcon(R.drawable.ic_launcher).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent();
							intent.setClass(TestProcActivity.this, MainActivity.class);
							startActivity(intent);
							TestProcActivity.this.finish();
						}
					}).setNegativeButton("No", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}).show();
		}
		return true;
	}
	
	//"Start" button listener,start test procedure
	private View.OnClickListener TestProcedureListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			initSDcard();
			if (sdcardExisted) {//check SD card
				if (!startProc) {//check test procedure start
					startProc = true;
					new Thread(new Runnable() {
						public void run() {
							try {
								initLogFile();
								if (procPart == 2)//PART II test
									while (enoughSpace) {
										startProc = true;
										testProc();
									}
								else
									testProc();
								closeLogFile();
								openTestFinishAlertDialog();
							} catch (Exception e) {
								logData("TestProcedureListener: " + e);
								e.printStackTrace();
							}
						}
					}).start();
					startButton.setText("PAUSE");
				} else {
					//Toast.makeText(TestProcActivity.this, "Running..", Toast.LENGTH_SHORT).show();
					if(!suspendTestProc) {
						suspendTestProc = true;
						startButton.setText("START");
					}else {
						startButton.setText("PAUSE");
						suspendTestProc = false;
						mutexSuspendTestProc.release();
					}
				}
			} else {
				Toast.makeText(TestProcActivity.this, "SD card not found", Toast.LENGTH_SHORT).show();
			}
		}
	};

	/*
	 * test process items order
	 */
	private void testProc() {
		getAPPVersion();
		setPhoneRotation(); // lock phone's screen orientation
		setProgressBar(); 	// set percentage of Test Procedure process
		setFileIndex(); 	// set video / photo file name
		logData("SD card PATH: " + SDcardPath);
		logData("Start test part " + procPart);
		switch(testPattenType) {
		case 0:
			logData("Sequence test");
			recordVideo();	// start testing record videos
			resetCamera();	// init camera parameters
			takePicture();	// start testing take pictures
			break;
		case 1:
			logData("Photo test only");
			takePicture();	// start testing take pictures
			resetCamera();	// init camera parameters
			break;
		case 2:
			logData("Video test only");
			recordVideo();	// start testing record videos
			resetCamera();	// init camera parameters
			break;
		case 3:
			logData("Interaction test");
			InteractionTest();
			break;
		}
		
		logData("Test part " + procPart + " finish");
		finishProc();
	}
	
	public void InteractionTest() {
		List<Integer> videoSizeList = getUserChoiceSizes(VIDEO_TYPE);
		List<Integer> picSizeList = getUserChoiceSizes(PHOTO_TYPE);
		int recordVideoRunTime = videoSizeList.size()>>1;
		int takePhotoRunTime = picSizeList.size()>>1;
		notTestedPhotoResolutionString = "not tested photo:\n";
		notTestedVideoResolutionString = "not tested video:\n";
		String fileName = null;
		long startTime, durationTime,offsetTime;
		int videoWidth, videoHeight;
		int sizeListIndex = 0,tmp = 0;
		while((recordVideoRunTime>0)||(takePhotoRunTime>0)) {
			if(recordVideoRunTime>0) {
				if (!hasFreeRemainSpace()) {
					//SD card full
					progressCount = videoSizeList.size() / 2;
					sendtoProgress(progressCount++);
					notTestedVideoResolutionString += videoSizeList.get(sizeListIndex) + "x" + videoSizeList.get(sizeListIndex + 1) + "\n";
				}else {
					//SD card not full
					videoWidth = videoSizeList.get(sizeListIndex);
					videoHeight = videoSizeList.get(sizeListIndex + 1);
	
					// If width is smaller than height , mediaRecorder would failed
					if (videoWidth < videoHeight) { // exchange width and height value
						videoWidth  = videoWidth ^ videoHeight;
						videoHeight = videoWidth ^ videoHeight;
						videoWidth  = videoWidth ^ videoHeight;
					}
					fileName = startRecord(videoWidth,videoHeight);
					if (fileName == null) {
						//test error occurs
						while((recordVideoRunTime>0)||(takePhotoRunTime>0)) {
							//record not tested resolution
							if(takePhotoRunTime>0) {
								//record not tested photo resolution
								notTestedPhotoResolutionString += notTestedPhotoResolutionString += picSizeList.get(sizeListIndex) + "x" + picSizeList.get(sizeListIndex+1) + "\n";
							}
							sizeListIndex+=2;
							recordVideoRunTime--;
							takePhotoRunTime--;
							if(recordVideoRunTime>0) {
								//record not tested video resolution
								notTestedVideoResolutionString += videoSizeList.get(sizeListIndex) + "x" + videoSizeList.get((sizeListIndex) + 1) + "\n";
							}
						}
						//end test procedure
						break;
					}else {
						startTime = System.currentTimeMillis();
						durationTime = 0;
						offsetTime = 0;
						while (durationTime < VIDEO_LENGTH) {
							try {
								if(suspendTestProc) {
									stopRecord();
									logData("[" + videoWidth + "x" + videoHeight + "] suspend TestProc");
									durationTime = System.currentTimeMillis();
									mutexSuspendTestProc.acquire();
									startRecord(videoWidth,videoHeight);
									offsetTime += System.currentTimeMillis() - durationTime;
								}

								if (!hasFreeRemainSpace()) {
									failVideoFileName = fileName;
									logData("[" + videoWidth + "x" + videoHeight + "] file name " + fileName
											+ " suspend: no enough space");
									notTestedVideoResolutionString += videoWidth + "x" + videoHeight+"(record suspend: no enough space)" + "\n";
									break;
								}
								
								durationTime = System.currentTimeMillis() - startTime - offsetTime;
								sendtoTextView(testProcStatusTextView," "+videoWidth + "x" + videoHeight + "  " + transTimeFormat(durationTime)+" ");
								Thread.sleep(1000);
							} catch (Exception e) {
								logData("RecordVideo(): " + e);
								e.printStackTrace();
							}
						}
						logData("[" + videoWidth + "x" + videoHeight + "] record time = " + transTimeFormat(durationTime));
						sendtoProgress(progressCount++);
						stopRecord();
					}
					resetCamera();
				}
			}
			
			if(takePhotoRunTime>0) {
				tmp = progressCount;
				currentPhotoWidth = picSizeList.get(sizeListIndex);
				currentPhotoHeight = picSizeList.get(sizeListIndex+1);
				if(!takePictureProc(currentPhotoWidth, currentPhotoHeight)) {
					//test error occurs
					while((recordVideoRunTime>0)||(takePhotoRunTime>0)) {
						//record not tested resolution
						sizeListIndex+=2;
						recordVideoRunTime--;
						takePhotoRunTime--;
						if(recordVideoRunTime>0) {
							//record not tested video resolution
							notTestedVideoResolutionString += videoSizeList.get(sizeListIndex) + "x" + videoSizeList.get((sizeListIndex) + 1) + "\n";
						}
						if(takePhotoRunTime>0) {
							//record not tested photo resolution
							notTestedPhotoResolutionString += notTestedPhotoResolutionString += picSizeList.get(sizeListIndex) + "x" + picSizeList.get(sizeListIndex+1) + "\n";
						}
					}
					//end test procedure
					break;
				}
				progressCount = tmp;
				sendtoProgress(progressCount++);
			}
			sizeListIndex+=2;
			recordVideoRunTime--;
			takePhotoRunTime--;
		}
	}
	//record video procedure
	public void recordVideo() {
		logData("Record Video");
		notTestedVideoResolutionString = "not tested video:\n";
		String fileName = null;
		long startTime, durationTime,offsetTime;
		int videoWidth, videoHeight;
		List<Integer> videoSizeList = getUserChoiceSizes(VIDEO_TYPE);

		for (int i = 0; i < videoSizeList.size(); i += 2) {
			if ((!hasFreeRemainSpace())||testErrorOccurs) {
				progressCount = videoSizeList.size() / 2;
				sendtoProgress(progressCount++);
				// toast("RecordVideo() "+progressCount);
				while(i<videoSizeList.size()) {
					notTestedVideoResolutionString += videoSizeList.get(i) + "x" + videoSizeList.get(i + 1) + "\n";
					i+=2;
				}
				break;
			}

			videoWidth = videoSizeList.get(i);
			videoHeight = videoSizeList.get(i + 1);

			// If width is smaller than height , mediaRecorder would failed
			if (videoWidth < videoHeight) { // exchange width and height value
				videoWidth  = videoWidth ^ videoHeight;
				videoHeight = videoWidth ^ videoHeight;
				videoWidth  = videoWidth ^ videoHeight;
			}
			Log.e("TestProc", "record "+videoWidth + "x" + videoHeight);
			fileName = startRecord(videoWidth,videoHeight);
			//if test error occurs
			if (fileName == null) continue;

			startTime = System.currentTimeMillis();
			durationTime = 0;
			offsetTime = 0;
			//recording video with VIDEO_LENGTH time
			while (durationTime < VIDEO_LENGTH) {
				try {
					if(suspendTestProc) {
						stopRecord();
						logData("[" + videoWidth + "x" + videoHeight + "] suspend TestProc");
						durationTime = System.currentTimeMillis();
						mutexSuspendTestProc.acquire();
						startRecord(videoWidth,videoHeight);
						//record suspend time
						offsetTime += System.currentTimeMillis() - durationTime;
					}
			
					if (!hasFreeRemainSpace()) {
						toast("Video: SD card no space");
						failVideoFileName = fileName;
						logData("[" + videoWidth + "x" + videoHeight + "] file name " + fileName
								+ " suspend: no enough space");
						notTestedVideoResolutionString += videoWidth + "x" + videoHeight +"(record suspend: no enough space)"+ "\n";
						break;
					}
					//offsetTime is suspend time
					durationTime = System.currentTimeMillis() - startTime - offsetTime;
					//update status view with 1 second
					sendtoTextView(testProcStatusTextView," "+videoWidth + "x" + videoHeight + "  " + transTimeFormat(durationTime)+" ");
					Thread.sleep(1000);
				} catch (Exception e) {
					logData("RecordVideo(): " + e);
					e.printStackTrace();
				}
			}
			logData("[" + videoWidth + "x" + videoHeight + "] record time = " + transTimeFormat(durationTime));
			sendtoProgress(progressCount++);
			stopRecord();
			
			/*if(suspendTestProc) {
				try {
					mutexSuspendTestProc.acquire();
					i-=2;
					recordRemainTimeVideo(durationTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}*/
		}
		toast("Record video finish");
	}

	//record video setting
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

			fileName = getVideoFilePath(videoWidth+"x"+videoHeight,fileExtention);
			mediarecorder.setOutputFile(SDcardPath + "video/"+logFileName+"/" + fileName);
			mediarecorder.setPreviewDisplay(surfaceHolder.getSurface());
			mediarecorder.prepare();
			mediarecorder.start();
		} catch (RuntimeException e) {
			e.printStackTrace();
			logData("startRecord(): " + e);
			deleteFailFile(SDcardPath + "video/"+logFileName+"/" + fileName);
			mediarecorder.reset();
			mediarecorder.release();
			mediarecorder = null;
			reconnectCamera();
			showAlertDialog(0,"Alert",videoWidth + "x" + videoHeight+" video failed");
			logData(videoWidth + "x" + videoHeight+" video failed");
			testErrorOccurs = true;
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			logData("startRecord(): " + e);
			showAlertDialog(0,"Alert",videoWidth + "x" + videoHeight+" video failed");
			logData(videoWidth + "x" + videoHeight+" video failed");
			testErrorOccurs = true;
			return null;
		}
		return fileName;
	}

	//stop record video setting
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
		}
	}

	//set take picture resolution
	public void takePicture() {
		logData("Take Picture");
		notTestedPhotoResolutionString = "not tested photo:\n";
		int picWidth, picHeight,i = 0;
		List<Integer> picSizeList = getUserChoiceSizes(PHOTO_TYPE);

		//if test error occurs
		if(testErrorOccurs) {
			while(i < picSizeList.size()) {
				//record not tested photo resolution
				picWidth = picSizeList.get(i);
				picHeight = picSizeList.get(i + 1);
				notTestedPhotoResolutionString += picWidth + "x" + picHeight + "\n";
				i+=2;
			}
			return;
		}

		for (i = 0; i < picSizeList.size(); i += 2) {
			//progressCount would be changed after calling camera.takePicture API in some case 
			int tmp = progressCount;
			picWidth = picSizeList.get(i);
			picHeight = picSizeList.get(i + 1);
			currentPhotoWidth = picWidth;
			currentPhotoHeight = picHeight;
			if(!takePictureProc(currentPhotoWidth, currentPhotoHeight)) {
				//test error occurs
				i+=2;
				while(i < picSizeList.size()) {
					//record not tested photo resolution
					picWidth = picSizeList.get(i);
					picHeight = picSizeList.get(i + 1);
					notTestedPhotoResolutionString += picWidth + "x" + picHeight + "\n";
					i+=2;
				}
				return;
			}
			progressCount = tmp;
			sendtoProgress(progressCount++);
		}

		toast("Take picture finish");
	}

	//test take picture resolution
	public boolean takePictureProc(int picWidth, int picHeight) {
		Camera.Parameters params = camera.getParameters();
		int waitAcquireCount = 0;
		int waitInterval = 40;
		try {
			camera.stopPreview();
			params.setPictureFormat(ImageFormat.JPEG);
			params.setPictureSize(picWidth, picHeight);
			params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			camera.setParameters(params);
			camera.setPreviewDisplay(surfaceHolder);
			camera.startPreview();
			//For HTC One V T320E,it must has some delay time with open camera.
			Thread.sleep(2000);
		} catch (Exception e) {
			logData("takePictureProc() Parameters: " + e);
			e.printStackTrace();
			Log.e(tag, "rotation error");
		}
		
		enoughSpace = true;
		handlePic = false;
		//takePhotoFail = false;
		sendtoTextView(testProcStatusTextView," "+currentPhotoWidth + "x" + currentPhotoHeight + ": 0 ");
		int takeCount = 1;
		//start take photo
		while (takeCount <= PIC_NUM) {
			if(suspendTestProc)
				try {
					mutexSuspendTestProc.acquire();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			waitAcquireCount = 0;
			try {
				handlePic = true;
				//takePhotoFail = false;
				camera.takePicture(null, null, jpeg);//take photo callback API
				while (!mutex.tryAcquire()) {//wait takePicture finish
					Thread.sleep(500);
					waitAcquireCount++;
					if (waitAcquireCount > waitInterval) {//if takePicture API time out (failed)
						mutex.release();
						logData("[" + picWidth + "x" + picHeight + "] take " + takeCount + " timeout");
						//Log.e(tag, "[" + picWidth + "x" + picHeight + "] take" + takeCount + " timeout");
						resetCamera();
						//////////////////////////////////////////////
						camera.stopPreview();
						params = camera.getParameters();
						params.setPictureFormat(ImageFormat.JPEG);
						params.setPictureSize(picWidth, picHeight);
						camera.setParameters(params);
						camera.setPreviewDisplay(surfaceHolder);
						camera.startPreview();
						/////////////////////////////////////////////
					}
				}
				if(testErrorOccurs) {
					showAlertDialog(0,"Alert","[" + picWidth + "x" + picHeight + "] take " + takeCount +" failed");
					logData("[" + picWidth + "x" + picHeight + "] take " + takeCount +" failed");
					return false;
				}
				if (waitAcquireCount > waitInterval) continue;
				camera.startPreview();
				if (!enoughSpace) {
					handlePic = false;
					logData("[" + picWidth + "x" + picHeight + "] take " + takeCount + " suspend: No enough space");
					if(takeCount>1) {
						notTestedPhotoResolutionString += picWidth + "x" + picHeight +"(take  "+ takeCount + " suspend: No enough space)"+ "\n";
					}else {
						notTestedPhotoResolutionString += picWidth + "x" + picHeight + "\n";
					}
					
					break;
				}
				sendtoTextView(testProcStatusTextView," "+currentPhotoWidth + "x" + currentPhotoHeight + ": " + (takeCount++)+" ");
				// Log.e(tag, picWidth + "x" + picHeight + ": " +
				// (takeCount-1));
			} catch (Exception e) {
				logData("takePictureProc() takePicture: " + e);
				e.printStackTrace();
			}
		}
		if (enoughSpace)
			logData("[" + picWidth + "x" + picHeight + "] take " + PIC_NUM);
		hasFreeRemainSpace();
		//wait final takePicture procedure finish
		while (handlePic);
		return true;
	}
	
	//take picture finish callback
	private PictureCallback jpeg = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			File file = null;
			FileOutputStream outStream = null;
			try {
				file = getPictureFilePath(currentPhotoWidth+"x"+currentPhotoHeight);
				outStream = new FileOutputStream(file);
				outStream.write(data);
				//rotate photo
				if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT) {
					ExifInterface exifi = new ExifInterface(file.getAbsolutePath());
					exifi.setAttribute(ExifInterface.TAG_ORIENTATION,
							String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
					exifi.saveAttributes();
				}
				outStream.close();
			} catch (FileNotFoundException e) {
				Log.e(tag, "onPictureTaken:" + e.getCause().toString());
				if (e.getCause().toString().indexOf("ENOENT") != -1) {//No such file
					enoughSpace = false;
					file.delete();
				}
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(tag, "onPictureTaken:" + e.getCause().toString());
				if (e.getCause().toString().indexOf("ENOSPC") != -1) {//No space
					Log.e("TestProc", "no space");
					enoughSpace = false;
					file.delete();
				}else {
					logData("onPictureTaken(): " + e);
					testErrorOccurs = true;
				}
				e.printStackTrace();
			} catch (Exception e) {
				logData("onPictureTaken(): " + e);
				testErrorOccurs = true;
				e.printStackTrace();
			} finally {
				handlePic = false;
				mutex.release();
			}
		}
	};

	//show alert dialog API
	private void showAlertDialog(final int dialogType,final String title,final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog alertDialog = new AlertDialog.Builder(TestProcActivity.this).create();
				alertDialog.setTitle(title);
				alertDialog.setMessage(message);
				alertDialog.setIcon(android.R.drawable.ic_menu_info_details);
				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
					    new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			        	dialog.dismiss();
			        	if(dialogType==2)
			        		changeActivity();
			        }
			    });
				alertDialog.show();
				alertDialog.setCancelable(false);//Dialog didn't close when clicked exceed dialog region
			}
		});
	}
	
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

	//check SD card exist
	public void initSDcard() {
		sdcardExisted = true;
		setSDcardPath();
		if(SDcardPath==null) {
			sdcardExisted = false;
			Toast.makeText(this, "SD card not found", Toast.LENGTH_SHORT).show();
			return;
		}
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

	//check SD card path
	public void setSDcardPath() {
		String rootPath = null;
		File file = null;
		getExternalFilesDir(null);
		//android 6.0
		if(Build.VERSION.SDK_INT>=23) {
			if(androidMarshmallowSDcardPath()!=null) {
				//user not format SD card to internal storage
				SDcardPath = androidMarshmallowSDcardPath() +"/Android/data/com.phison.sdcardtest/";
			}else {
				if(sdcardSetting.equals("1")) {
					//user set SD card to internal storage
					SDcardPath = "/storage/emulated/0/Android/data/com.phison.sdcardtest/";
				} else {
					//user not insert SD card
					SDcardPath = null;
				}
			}
			return;
		}
		//SONY Xperia Miro
		if(android.os.Build.MODEL.matches(".*ST23a+.*")) {
			SDcardPath = "/mnt/ext_card" +"/Android/data/com.phison.sdcardtest/";
			file = new File(SDcardPath);
			file.mkdirs();
			return;
		}
		
		if((rootPath = System.getenv("SECONDARY_STORAGE"))==null) {
			rootPath = System.getenv("EXTERNAL_STORAGE");
		}
		
		//For Samsung S3 device sdcard Path
		String[] pathTmp = rootPath.split(":");
		rootPath = pathTmp[0];
		SDcardPath = rootPath +"/Android/data/com.phison.sdcardtest/";
		file = new File(SDcardPath);
		if(!file.exists()) {
			SDcardPath = rootPath +"/Android/data/com.phison.sdcardtest/";
			file = new File(SDcardPath);
			file.mkdirs();
		}
		if(!file.exists()) {
			SDcardPath = null;
			toast("create dir fail");
		}
	}

	//get android 6.0 SD card path
	private String androidMarshmallowSDcardPath() {
		String rootPath = null;
		File f = new File("/storage");
		if (f.isDirectory()) {
			String[] s = f.list();
			for (int i = 0; i < s.length; i++) {
				if(s[i].matches(".*-+.*")) {
					rootPath ="/storage/" + s[i];
					break;
				}
			}
		}
		return rootPath;
	}

	//get log file path
	public static String getLogFileDirPath() {
		String root = "/mnt/emmc";
		String path = root + "/SDcardTestLog";
		try{
			File file = new File(path);
			file.mkdir();
			if(!file.exists()) {
				root = Environment.getExternalStorageDirectory().toString();
				path = root + "/SDcardTestLog";
				file = new File(path);
				file.mkdirs();
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return path;
	}

	//reconnect camera when camera failed
	private void reconnectCamera() {
		try {
			camera.reconnect();
			setPreview();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logData("reconnectCamera(): " + e);
			e.printStackTrace();
		}
	}

	//reset camera when camera reuse
	private void resetCamera() {
		try {
			camera.release();
			camera = Camera.open();
			setPreview();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logData("resetCamera(): " + e);
			e.printStackTrace();
		}
	}

	//set camera preview parameter
	private void setPreview() {
		try {
			camera.stopPreview();
			if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT)
				camera.setDisplayOrientation(90);
			Camera.Parameters parameters = camera.getParameters();
			parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			camera.setParameters(parameters);
			camera.setPreviewDisplay(surfaceHolder);
			camera.startPreview();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logData("setPreview(): " + e);
			e.printStackTrace();
		}
	}

	//set record video time format
	private String transTimeFormat(long durationTime) {
		long minuteTime = TimeUnit.MILLISECONDS.toMinutes(durationTime);
		return String.format("%02d : %02d",minuteTime ,
				TimeUnit.MILLISECONDS.toSeconds(durationTime)
						- TimeUnit.MINUTES.toSeconds(minuteTime));
	}
	
	private void deleteFailFile(String selectedFilePath) {
		try {
			File file = new File(selectedFilePath);
			boolean deleted = file.delete();
			if (deleted)
				logData("deleteFailFile:[success] " + selectedFilePath);
			else
				logData("deleteFailFile:[fail] " + selectedFilePath);
		} catch (Exception e) {
			e.printStackTrace();
			logData("deleteFailFile(): " + e);
		}
	}

	public void initLogFile() {
		long currentTime = System.currentTimeMillis();
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-HH-mm-ss");
		Date date = new Date(currentTime);
		logFileName = formatter.format(date);
		try {
			logDataWriter = new FileWriter(getLogFileDirPath() + "/" + logFileName + ".txt", true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logData("initLogFile(): " + e);
			e.printStackTrace();
		}
	}

	//set files & set index
	public void setFileIndex() {
		File videoDir = new File(SDcardPath,"video/"+logFileName);
		if (!videoDir.exists())
			videoDir.mkdirs();
		File photoDir = new File(SDcardPath,"picture/"+logFileName);
		if (!photoDir.exists())
			photoDir.mkdirs();

		videoFileIndex = 0;
		photoFileIndex = 0;
	}

	public String getVideoFilePath(String videoResolution,String fileExtension) {
		File appDir = new File(SDcardPath,"video/"+logFileName);

		String name = "["+videoResolution+"]"+ fileExtension;
		new File(appDir, name);
		//Log.e(tag, "video name " + name);
		if (!appDir.exists())
			return getVideoFilePath(videoResolution,fileExtension);
		return name;
	}

	public File getPictureFilePath(String photoResolution) {
		File appDir = new File(SDcardPath,"picture/"+logFileName);
		photoFileIndex++;
		String name = "["+photoResolution+"]_" + String.valueOf(photoFileIndex) + ".jpg";
		if(photoFileIndex==PIC_NUM)
			photoFileIndex = 0;
		return new File(appDir, name);
	}

	public List<Integer> getPhoneSupportedSizes(int type) {
		if((type !=VIDEO_TYPE)&&(type != PHOTO_TYPE)) { 
			toast("Error type : "+type); 
			return null; 
		} 
		List<Size> sizeList; 
		List<Integer> phoneSupportedList = new ArrayList<Integer>(); 
		try { 
			if (type == VIDEO_TYPE) {
				sizeList = camera.getParameters().getSupportedVideoSizes(); 
				for (int i = 0; i < sizeList.size(); i++) {
					//Log.e(tag, "video: " +sizeList.get(i).width+"x"+sizeList.get(i).height); 
						phoneSupportedList.add(sizeList.get(i).width);
						phoneSupportedList.add(sizeList.get(i).height);
			  	} 
			} else if (type == PHOTO_TYPE) {
				sizeList = camera.getParameters().getSupportedPictureSizes();
				
				if((sizeList.get(0).width<=sizeList.get(1).width)&&(sizeList.get(0).height<=sizeList.get(1).height))
					for (int i = sizeList.size()-1; i >= 0; i--) {
						phoneSupportedList.add(sizeList.get(i).width);
						phoneSupportedList.add(sizeList.get(i).height);
					}
				else 
					for (int i = 0; i < sizeList.size(); i++) {
						phoneSupportedList.add(sizeList.get(i).width);
						phoneSupportedList.add(sizeList.get(i).height);
						//Log.e("TestProc", "phone photo resolution: " +sizeList.get(i).width+"x"+sizeList.get(i).height); 
					} 
			} else { 
				toast("Error type"); 
				return null;
			} 
		} catch (Exception e) {
				logData("getPhoneSupportedSizes(): " + e); e.printStackTrace();
		} 
		return phoneSupportedList; 
	}

	public List<Integer> getUserChoiceSizes(int type) {
		
		if ((type != VIDEO_TYPE) && (type != PHOTO_TYPE)) {
			toast("Error type : " + type);
			return null;
		}
		List<Integer> phoneSupportedList = new ArrayList<Integer>();
		String[] strTmp;
		if (type == VIDEO_TYPE) {
			for (int i = 0; i < userChoiceVideoResolutionArray.length; i++) {
				if (userChoiceVideoResolutionArray[i] == null)
					continue;
				strTmp = userChoiceVideoResolutionArray[i].toString().split("x");
				phoneSupportedList.add(Integer.parseInt(strTmp[0]));
				phoneSupportedList.add(Integer.parseInt(strTmp[1]));
				Log.e(tag, "video: " +strTmp[0]+"x"+strTmp[1]); 
			}
		} else if (type == PHOTO_TYPE) {
			for (int i = 0; i < userChoicePhotoResolutionArray.length; i++) {
				if (userChoicePhotoResolutionArray[i] == null)
					continue;
				strTmp = userChoicePhotoResolutionArray[i].toString().split("x");
				phoneSupportedList.add(Integer.parseInt(strTmp[0]));
				phoneSupportedList.add(Integer.parseInt(strTmp[1]));
			}
		} else {
			toast("Error type");
			return null;
		}
		return phoneSupportedList;
	}

	public void sendtoTextView(final TextView view, final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setText(x);
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

	//show toast on screen
	public void toast(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(TestProcActivity.this, x, Toast.LENGTH_SHORT).show();
			}
		});
	}

	//write log data to log file
	public void logData(String log) {
		try {
			//writing log time
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
			Date date = new Date(System.currentTimeMillis());
			//write log string
			logDataWriter.write("["+formatter.format(date)+"] "+log + "\n");
			logDataWriter.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//check current SD card available storage space
	public boolean hasFreeRemainSpace() {
		StatFs sdcardSpaceStat = new StatFs(SDcardPath.substring(0, SDcardPath.indexOf("/Android/")));
		float sdAvailSize = sdcardSpaceStat.getAvailableBlocks() * (sdcardSpaceStat.getBlockSize() / (1048576f));//1024*1024
		String spaceString;
		//show GB or MB
		if(sdAvailSize>=1024)
			spaceString = doubleFormat.format(sdAvailSize/1024f)+" GB ";
		else
			spaceString = doubleFormat.format(sdAvailSize)+" MB ";
		sendtoTextView(availableSpaceTextView," available: " +spaceString);
		//if available space less than 5 MB,show SD card is full
		if (sdAvailSize <= 5) {//5MB
			return false;
		} else
			return true;
	}

	//check video dir contains file or not
	public int getDirectoryFileList() {
		File f = new File(SDcardPath + "/video");
		if (f.isDirectory()) {
			Log.e(tag, "filename : " + f.getName());
			String[] s = f.list();
			return s.length;
		}
		return 0;
	}

	//init progress bar
	private void setProgressBar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				testProcStatusTextView.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.VISIBLE);
				List<Integer> videoTotalNum = getUserChoiceSizes(VIDEO_TYPE);
				List<Integer> pictureTotalNum = getUserChoiceSizes(PHOTO_TYPE);

				progressBar.setMax(videoTotalNum.size() / 2 + pictureTotalNum.size() / 2);
				progressBar.setProgress(0);
				logData("ProgressBarMAX = " + progressBar.getMax());
				// toast("MAX " + progressBar.getMax());
			}
		});
	}

	//change screen's orientation when detecting rotation.
	private void setPhoneRotation() {
		if (phoneConfigure.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			logData("ORIENTATION_LANDSCAPE");
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (phoneConfigure.orientation == Configuration.ORIENTATION_PORTRAIT) {
			logData("ORIENTATION_PORTRAIT");
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}

	//show some message when finish the test
	private void finishProc() {
		if (!enoughSpace) {
			sendtoTextView(testProcStatusTextView," SD card no space ");
			logData("SD card no space");
		} else if(testErrorOccurs) {
			sendtoTextView(testProcStatusTextView," test error occurs ");
			logData("test error occurs");
		}else {
			sendtoTextView(testProcStatusTextView," finish ");
		}
		progressCount = 1;
		startProc = false;
	}

	//close the log file when finish the test
	private void closeLogFile() {
		try {
			logDataWriter.flush();
			logDataWriter.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//get version from AndroidManifest.xml
	public void getAPPVersion() {
		try {
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(),0);
		    String strVersionName = "Version: "+ packageInfo.versionName;
		    logData(strVersionName);
		} catch (NameNotFoundException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	}

	//get MainActiviy bundle data
	private void getBundleData() {
		Bundle bundle = this.getIntent().getExtras();
		procPart = bundle.getInt("part");
		testPattenType = bundle.getInt("testPattenType");
		sdcardSetting = bundle.getString("sdcardSetting");
		userChoiceVideoResolutionArray = bundle.getCharSequenceArray("testVideoResolutionArray");
		userChoicePhotoResolutionArray = bundle.getCharSequenceArray("testPhotoResolutionArray");
	}

	//decide which alert dialog to show when finish test procedure
	private void openTestFinishAlertDialog() {
		if(enoughSpace) 
			showAlertDialog(2, "Test finish", "Click OK to playback");
		else if(testErrorOccurs) {
			String showMessage = notTestedVideoResolutionString==null? "" : notTestedVideoResolutionString+"\n";
			showMessage += notTestedPhotoResolutionString==null? "" : notTestedPhotoResolutionString;
			showAlertDialog(0, "Test error",showMessage);
		} else {
			String showMessage = notTestedVideoResolutionString==null? "" : notTestedVideoResolutionString+"\n";
			showMessage += notTestedPhotoResolutionString==null? "" : notTestedPhotoResolutionString;
			showAlertDialog(2, "SD card full",showMessage+"\nClick OK to playback");
		}
	}

	//change to VideoActivity
	private void changeActivity() {
		Intent intent = new Intent();
		intent.setClass(TestProcActivity.this, VideoActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("failVideoFileName", failVideoFileName);
		bundle.putString("logFileName", logFileName);
		intent.putExtras(bundle);
		startActivity(intent);
		TestProcActivity.this.finish();
	}

	//display TestProc video and photo
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
