package com.example.sdcardtest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PhotoActivity extends Activity {
	private static ImageView image;
	private final String tag = "Photo";
	private TextView tw;
	private String SdCardPath;
	private String logFileName;
	private FileWriter logDataWriter = null;
	private int intervalTime = 1000;// ms
	private int failFile = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo);
		Toast.makeText(getApplicationContext(), "Play photos", Toast.LENGTH_SHORT).show();

		setBundleData();
		initLogFile();
		initialize();

	}

	// System.getenv("SECONDARY_STORAGE")
	private void initialize() {
		logData("\n\nPlay photos");
		image = (ImageView) findViewById(R.id.imageView1);
		tw = (TextView) this.findViewById(R.id.textView1);
		SdCardPath = System.getenv("SECONDARY_STORAGE") + "/Android/data/com.example.sdcardtest/picture/";
		if (getDirectoryFileList().size() == 0) {
			sendtoTextView("No photo file");
			logData("No photo file");
			logData("Play photos finish");
			closeWriter();
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				try {

					playPhoto();
				} catch (Exception e) {
					logData("initialize(): " + e.getCause().toString());
					Toast.makeText(PhotoActivity.this, e.getCause().toString(), Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void playPhoto() {

		final ArrayList<String> fileList = getDirectoryFileList();
		Bitmap bmp = null;
		Bitmap out = null;

		int sampleSize = 2;
		for (int i = 0; i < fileList.size(); i++) {
			bmp = BitmapFactory.decodeFile(SdCardPath + fileList.get(i));
			if (bmp == null) {
				toast(fileList.get(i) + " can't read");
				logData(fileList.get(i) + " can't read");
				failFile++;
				continue;
			}

			out = decodeSampledBitmapFromResource(SdCardPath + fileList.get(i), sampleSize);
			try {
				Thread.sleep(intervalTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			sendtoUI(out);
			sendtoTextView(fileList.get(i));
			logData("Photo :" + fileList.get(i));
		}
		logData("Total photos = " + fileList.size() + ", error = " + failFile);
		sendtoTextView("Finish");
		logData("Play photos finish");
		closeWriter();
	}

	public void sendtoTextView(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tw.setText(x);
			}
		});
	}

	public void sendtoUI(final Bitmap x) {
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
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		source = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
		return source;
	}

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
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		String SDcardPath = System.getenv("SECONDARY_STORAGE") + "/Android/data/com.example.sdcardtest/picture";
		File f = new File(SDcardPath);
		ArrayList<String> fileList = new ArrayList<String>();
		if (f.isDirectory()) {
			String[] s = f.list();
			for (int i = 0; i < s.length; i++) {
				fileList.add(s[i]);
			}
		}
		return fileList;
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

	public void closeWriter() {
		try {
			logDataWriter.flush();
			logDataWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setBundleData() {
		Bundle bundle = this.getIntent().getExtras();
		logFileName = bundle.getString("logFileName");
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(PhotoActivity.this).setTitle("Exit").setMessage("Exit Application ?")
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
