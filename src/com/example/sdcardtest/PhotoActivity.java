package com.example.sdcardtest;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.Toast;

public class PhotoActivity extends Activity {
	private ImageView image;
	private final String tag = "Photo";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photo);
		image = (ImageView) findViewById(R.id.imageView1);
		
		playPhoto();
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
		Log.e(tag, "display "+width+"x"+height);
		
		/*for(int i = 0;i<fileList.size();i++){
			bmp = BitmapFactory.decodeFile(System.getenv("SECONDARY_STORAGE")+"/Android/data/com.example.sdcardtest/picture/"+fileList.get(i));
			
			
			out = Bitmap.createScaledBitmap(bmp,(int) (bmp.getWidth()*0.8) , (int) (bmp.getHeight()*0.8), false);
			image.setImageBitmap(bmp);
			Toast.makeText(PhotoActivity.this,fileList.get(i), Toast.LENGTH_SHORT).show();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
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
