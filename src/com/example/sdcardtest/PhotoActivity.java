package com.example.sdcardtest;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class PhotoActivity extends Activity {
	private ImageView image;
	private final String tag = "Photo";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo);
		image = (ImageView) findViewById(R.id.imageView1);
		new Thread(new Runnable() {
            public void run() {
           	 try{
           		 //RecordVideo();
           		 
           		playPhoto();
           	 } catch (Exception e){
    				// TODO Auto-generated catch block
           		 //Toast.makeText(MainActivity.this,e.getCause().toString(), Toast.LENGTH_SHORT).show();
    				e.printStackTrace();
    			 }
            }
		}).start();
		
		
	}
	
	public void playPhoto(){
		final ArrayList<String> fileList = getDirectoryFileList();
		Bitmap bmp = null;
		Bitmap out = null;
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		//int width = image.getMeasuredWidth();
		//int height = image.getMeasuredHeight();
		Log.e(tag, "display "+width+"x"+height);
		/*bmp = BitmapFactory.decodeFile(System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/picture/"+fileList.get(0));
		image.setImageBitmap(bmp);*/
		for(int i = 0;i<fileList.size();i++){
			bmp = BitmapFactory.decodeFile(System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/picture/"+fileList.get(i));
			
			
			//out = Bitmap.createScaledBitmap(bmp, (bmp.getWidth()/2) ,  (bmp.getHeight()/2), false);
			//image.setImageBitmap(bmp);
			sendtoUI(decodeSampledBitmapFromResource(System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/picture/"+fileList.get(i), 100, 100));
			//image.setImageBitmap(bmp);
			toast(fileList.get(i));
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		toast("Finish");
		
	}
	
	public void toast(final String x){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	Toast.makeText(PhotoActivity.this,x, Toast.LENGTH_SHORT).show();
            }
        });
    }
	
	public void sendtoUI(final Bitmap x){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	image.setImageBitmap(x);
            }
        });
    }
	
	public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        final int halfHeight = height / 2;
        final int halfWidth = width / 2;

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
    }

    return inSampleSize;
	}
	
	public static Bitmap decodeSampledBitmapFromResource(String filePath,
	        int reqWidth, int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    
	    BitmapFactory.decodeFile(filePath,options);
	    
	    int a = options.outWidth;
	    int b = options.outHeight;
	    // Calculate inSampleSize
	    options.inJustDecodeBounds = false;
	    options.inSampleSize = 2;

	    // Decode bitmap with inSampleSize set
	    
	    options.inPurgeable = true;
	    return BitmapFactory.decodeFile(filePath,options);
	}
	
	
	public ArrayList<String> getDirectoryFileList(){
		String SDcardPath = System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/picture";
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
}
