package com.example.sdcardtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PhotoActivity extends Activity {
	private static ImageView image;
	private final String tag = "Photo";
	private TextView photoNametextView;
	private ImageButton playButton;
	private ImageButton prevButton;
	private ImageButton nextButton;
	private String SdCardPath;
	private String logFileName;
	private FileWriter logDataWriter = null;
	private Configuration phoneConfigure;
	private int intervalTime = 1000;// ms
	private int failFile = 0;
	private int playPhotoIndex = 0;
	private ArrayList<String> photofileList;
	private boolean stopPLAY = false;
	private boolean checkedFinalPhoto = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
		setBundleData();
		initLogFile();
		SdCardPath = TestProcActivity.SDcardPath + "/picture/"+logFileName+"/";
		loadPlayPhotoIntervalTime();
		logData("\n\nPlay photos");
		setPhoneRotation();
		if((photofileList = getDirectoryFileList())!=null){
			playPhoto();
		}else{
			sendtoTextView("No photo file");
			logData("No photo file");
			logData("Play photos finish");
			closeWriter();
		}
	}

	private void initialize() {
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo);

		phoneConfigure = getResources().getConfiguration();
		image = (ImageView) findViewById(R.id.imageView1);
		playButton = (ImageButton) this.findViewById(R.id.playPhotoButton);
		prevButton = (ImageButton) this.findViewById(R.id.prevButton);
		nextButton = (ImageButton) this.findViewById(R.id.nextButton);
		prevButton.setVisibility(View.INVISIBLE);
		nextButton.setVisibility(View.INVISIBLE);
		setControlView();
		
		
		photoNametextView = (TextView) this.findViewById(R.id.photoName_textView);
		playButton.setOnClickListener(playPhotoListener);
		prevButton.setOnClickListener(playPhotoListener);
		nextButton.setOnClickListener(playPhotoListener);
		
	}
	
	private View.OnClickListener playPhotoListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//auto play photo
			if(v == playButton){
				if((photofileList==null)||(photofileList.size()==0))
					return;
				if(!stopPLAY){
					prevButton.setVisibility(View.VISIBLE);
					nextButton.setVisibility(View.VISIBLE);
					playButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
					stopPLAY = true;
				}else{
					prevButton.setVisibility(View.INVISIBLE);
					nextButton.setVisibility(View.INVISIBLE);
					playButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
					stopPLAY = false;
					playPhoto();
				}
			}
			//clicking PREV to select prev photo
			if(v == prevButton){
				if((--playPhotoIndex)<0){
					playPhotoIndex = 0;
					toast("This is first photo");
				}else
					selectPhoto(playPhotoIndex);
			}
			//clicking NEXT to select next photo
			if(v == nextButton){
				if((++playPhotoIndex)==photofileList.size()){
					if(!checkedFinalPhoto)
						finishProc();
					checkedFinalPhoto = true;
					playPhotoIndex--;
					toast("This is last photo");
				}else
					selectPhoto(playPhotoIndex);
			}
		}
	};
	
	public void playPhoto() {
		new Thread(new Runnable() {
			public void run() {
				try {
					int sampleSize = 2;
					for (int i = playPhotoIndex; i < photofileList.size(); i++) {
						if(stopPLAY) return;
						if(!loadPhoto(photofileList.get(i), sampleSize)){
							toast(photofileList.get(i) + " can't read");
							logData(photofileList.get(i) + " can't read");
							failFile++;
						}
						playPhotoIndex = i;	
						Thread.sleep(intervalTime);
					}
				}catch (Exception e) {
					logData("playPhoto(): " + e);
					e.printStackTrace();
				}
				sendtoTextView("Finish");
				if(!checkedFinalPhoto)
					finishProc();
				checkedFinalPhoto = true;
			}
		}).start();	
	}
	//user select specific photo
	public void selectPhoto(int photoIndex) {
		int sampleSize = 2;//photo shrink 2   
		
		if(!loadPhoto(photofileList.get(photoIndex), sampleSize)){
			toast(photofileList.get(photoIndex) + " can't read");
			logData(photofileList.get(photoIndex) + " can't read");
			failFile++;
		}
	}
	
	public boolean loadPhoto(String photoName,int sampleSize){
		Bitmap outImage = null;
		
		try {
			outImage = decodeSampledBitmapFromResource(SdCardPath + photoName, sampleSize);
			if(outImage==null) return false;
			
			sendtoImageView(outImage);
			sendtoTextView(photoName);
			//logData("Photo: " + photoName);
		} catch (Exception e) {
			logData("loadPhoto(): " + e);
			e.printStackTrace();
		}
		return true;
	}
	
	public void sendtoTextView(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				photoNametextView.setText(x);
			}
		});
	}

	public void sendtoImageView(final Bitmap x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				image.setImageBitmap(x);
			}
		});
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	public Bitmap decodeSampledBitmapFromResource(String filePath, int SampleSize) {

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);
		int rotation = getExifInfo(filePath);

		options.inJustDecodeBounds = false;
		options.inSampleSize = SampleSize;
		options.inPurgeable = true;
		return RotateBitmap(BitmapFactory.decodeFile(filePath, options), rotation);
	}

	public Bitmap RotateBitmap(Bitmap source, float angle) {
		if(source==null)
			return null;
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		source = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
		return source;
	}
	//get photo exif data to rotate photo
	public int getExifInfo(String filePath) {
		ExifInterface exifi;
		try {
			exifi = new ExifInterface(filePath);
			int orientation = exifi.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				return 90;
			case ExifInterface.ORIENTATION_ROTATE_180:
				return 180;
			case ExifInterface.ORIENTATION_ROTATE_270:
				return 270;
			default:
				return 0;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logData("getExifInfo(): " + e);
			e.printStackTrace();
		}
		return 0;
	}

	public void toast(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(PhotoActivity.this, x, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public ArrayList<String> getDirectoryFileList() {
		File f = new File(SdCardPath);
		ArrayList<String> fileList = new ArrayList<String>();
		if (f.isDirectory()) {
			String[] s = f.list();
			if(s.length==0)
				return null;
			for (int i = 0; i < s.length; i++) {
				fileList.add(s[i]);
			}
		}
		
		if (fileList.size() == 0) {
			sendtoTextView("No photo file");
			logData("No photo file");
			logData("Play photos finish");
			closeWriter();
			return null;
		}
		
		return fileList;
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

	
	public void logData(String log) {
		if(checkedFinalPhoto) return;
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

	public void initLogFile() {
		String path = TestProcActivity.getLogFileDirPath();
		try {
			logDataWriter = new FileWriter(path + "/" + logFileName + ".txt", true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeWriter() {
		if(checkedFinalPhoto) return;
		try {
			logDataWriter.flush();
			logDataWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//loading play photo interval time
	private boolean loadPlayPhotoIntervalTime() {
		try {
			BufferedReader settingReader = new BufferedReader(new FileReader(TestProcActivity.getLogFileDirPath() + "/SettingData"));
			String strTmp;
			String[] strSplitTmp;
			int i,j;
			while((strTmp = settingReader.readLine()) != null) {
				strSplitTmp = strTmp.split(" ");
				if(strSplitTmp[0].equals("playPhotoInterval")){
					intervalTime = Integer.parseInt(strSplitTmp[1]);
				}
			}
			settingReader.close();
			//toast("Load setting success");
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}		

	public void finishProc(){
		logData("Total photos = " + photofileList.size() + ", error = " + failFile);
		logData("Play photos finish");
		closeWriter();
	}
	
	private void setBundleData() {
		Bundle bundle = this.getIntent().getExtras();
		logFileName = bundle.getString("logFileName");
	}
	
	private void setControlView(){
		LayoutInflater controlInflater = LayoutInflater.from(getBaseContext());
		View viewControl = controlInflater.inflate(R.layout.control_photo, null);
		LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		this.addContentView(viewControl, layoutParamsControl);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(PhotoActivity.this).setTitle("Exit").setMessage("Return APP Home screen ?")
					.setIcon(R.drawable.ic_launcher).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent();
							intent.setClass(PhotoActivity.this, MainActivity.class);
							startActivity(intent);
							PhotoActivity.this.finish();
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
