package com.example.sdcardtest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
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
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo);
		
		initialize();
		
	}
	
	public void initialize(){
		image = (ImageView) findViewById(R.id.imageView1);
		tw = (TextView) this.findViewById(R.id.textView1);
		SdCardPath = System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/picture/";
		new Thread(new Runnable() {
            public void run() {
           	 try{
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
		
		int sampleSize = 2;
		
		/*Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;*/
		
		//Log.e(tag, "display "+width+"x"+height);
		/*bmp = BitmapFactory.decodeFile(System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/picture/"+fileList.get(0));
		image.setImageBitmap(bmp);*/
		for(int i = 0;i<fileList.size();i++){
			bmp = BitmapFactory.decodeFile(SdCardPath+fileList.get(i));
			
			out = decodeSampledBitmapFromResource(SdCardPath+fileList.get(i), sampleSize);
			
			//image.setImageBitmap(bmp);
			sendtoTextView(fileList.get(i));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sendtoUI(out);
		}
		
		sendtoTextView("Finish");
		
	}
	
	public void sendtoTextView(final String x){
		runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	tw.setText(x);
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
	
	public Bitmap decodeSampledBitmapFromResource(String filePath,int SampleSize) {
		
	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    
	    BitmapFactory.decodeFile(filePath,options);
	    
	    int rotation = getExifInfo(filePath);
        
		int width = image.getWidth();
		int height = image.getHeight();
	    // Calculate inSampleSize
	    options.inJustDecodeBounds = false;
	    options.inSampleSize = SampleSize;
	    //options.inSampleSize = calculateInSampleSize(options,width,height);
	    // Decode bitmap with inSampleSize set
	    
	    options.inPurgeable = true;
	    return RotateBitmap(BitmapFactory.decodeFile(filePath,options),rotation);
	}
	
	public Bitmap RotateBitmap(Bitmap source, float angle) {
	    Matrix matrix = new Matrix();
	    matrix.postRotate(angle);
	    source = Bitmap.createBitmap(source, 0, 0, source.getWidth(),source.getHeight(), matrix, true);
	    
	    return source;
	}
	
	public int getExifInfo(String filePath){
		ExifInterface exifi;
		try {
			exifi = new ExifInterface(filePath);
			 int orientation = exifi.getAttributeInt(ExifInterface.TAG_ORIENTATION,
		                ExifInterface.ORIENTATION_NORMAL);
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
