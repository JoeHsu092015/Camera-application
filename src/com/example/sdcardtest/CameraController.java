package com.example.sdcardtest;

import java.io.File;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.content.Context;
public class CameraController {
	public Camera cameraObject;
	public String sdCardPath;
	public int videoFileIndex;
	public int photoFileIndex;
	
	public CameraController(Camera cma) {
		this.cameraObject = cma;
	}
	
	//set video resolution type
	public String getVideoResolutionType(String resolution) {
		if(resolution.equals("176x144")) {
			return "[QCIF]";
		}else if(resolution.equals("320x240")) {
			return "[QVGA]";
		}else if(resolution.equals("352x258")) {
			return "[CIF]";
		}else if(resolution.equals("640x480")) {
			return "[VGA]";
		}else if(resolution.equals("720x480")) {
			return "[SD]";
		}else if(resolution.equals("1280x720")) {
			return "[HD]";
		}else if(resolution.equals("1920x1080")) {
			return "[FHD]";
		}else if(resolution.equals("2560x1440")) {
			return "[QHD]";
		}else if(resolution.equals("3840x2160")) {
			return "[UHD]";
		}else if(resolution.equals("4096x2160")) {
			return "[4K]";
		}else {
			return null;
		}
	}
	
	public boolean openCamera(){
		try{
			for(int i = 0; i < Camera.getNumberOfCameras(); i++) {
				CameraInfo info = new CameraInfo();
				Camera.getCameraInfo(i, info);
				if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
					cameraObject = Camera.open(i);
					return true;
				}
			}
		}catch(Exception e){
			//toast("" + e);
		}
		return false;
	}
	
	public boolean closeCamera() {
		cameraObject.release();
		return true;
	}
	
	public List<Size> getSupportedVideoSizes() {
		List<Size> sizeList;
		if((sizeList = cameraObject.getParameters().getSupportedVideoSizes())==null)
			sizeList = cameraObject.getParameters().getSupportedPreviewSizes();
		return sizeList;
	}
	
	public List<Size> getSupportedPhotoSizes() {
		return cameraObject.getParameters().getSupportedPictureSizes();
	}
	
	//load files & set index
	public void setFileIndex() {
		File videoDir = new File(sdCardPath, "video");
		if (!videoDir.exists())
			videoDir.mkdir();
		File photoDir = new File(sdCardPath, "picture");
		if (!photoDir.exists())
			photoDir.mkdir();
		String[] videoDirFileList = videoDir.list();
		String[] photoDirFileList = photoDir.list();

		videoFileIndex = videoDirFileList.length;
		photoFileIndex = photoDirFileList.length;
	}

	public String getVideoFilePath(String fileExtention) {
		File appDir = new File(sdCardPath, "video");
		videoFileIndex++;
		String name = "Video" + String.valueOf(videoFileIndex) + fileExtention;
		new File(appDir, name);
		//Log.e(tag, "video name " + name);
		if (!appDir.exists())
			return getVideoFilePath(fileExtention);
		return name;
	}

	public File getPictureFilePath() {
		File appDir = new File(sdCardPath, "picture");
		photoFileIndex++;
		String name = "Photo" + String.valueOf(photoFileIndex) + ".jpg";
		return new File(appDir, name);
	}
	
	public String getSDcardPath() {
		return this.sdCardPath;
	}
	
	public void setSDcardPath() {
		String rootPath = null;
		File file = null;
		//getExternalFilesDir(null);
		//android 6.0
		if(Build.VERSION.SDK_INT>=23) {
			if(androidMarshmallowSDcardPath()!=null) {
				sdCardPath = androidMarshmallowSDcardPath() +"/Android/data/com.example.sdcardtest/";
			}else {
				//user set SD card to internal storage
				sdCardPath = "/storage/emulated/0/Android/data/com.example.sdcardtest/";
			}
			return;
		}
		//SONY Xperia Miro
		if(android.os.Build.MODEL.matches(".*ST23a+.*")) {
			sdCardPath = "/mnt/ext_card" +"/Android/data/com.example.sdcardtest/";
			file = new File(sdCardPath);
			file.mkdirs();
			return;
		}
		
		if((rootPath = System.getenv("SECONDARY_STORAGE"))==null) {
			rootPath = System.getenv("EXTERNAL_STORAGE");
		}
		
		//For Samsung S3 device sdcard Path
		String[] pathTmp = rootPath.split(":");
		rootPath = pathTmp[0];
		sdCardPath = rootPath +"/Android/data/com.example.sdcardtest/";
		file = new File(sdCardPath);
		if(!file.exists()) {
			sdCardPath = rootPath +"/Android/data/com.example.sdcardtest/";
			file = new File(sdCardPath);
			file.mkdirs();
		}
		if(!file.exists()) {
			sdCardPath = null;
			//toast("create dir fail");
		}
	}
	
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
	
}
