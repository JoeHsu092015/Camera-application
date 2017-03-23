package com.example.sdcardtest;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	/*
	 * part1Button: sdcard test procedure run 1 time
	 * part2Button: sdcard test procedure run until sdcard capacity full
	 */
	Button part1Button;
	Button part2Button;
	CharSequence[] deviceDefaultCameraVideoResolution;//send to TestProc activity data
	CharSequence[] deviceDefaultCameraPhotoResolution;//send to TestProc activity data
	CheckBox sdcardPositionCheckBox;
	TextView databaseStatusTextView;	//show database status
	TextView sdcardNameTextView;		//show SD card name
	TextView sdcardCIDTextView;		//show SD card CID
	TextView sdcardCSDTextView;		//show SD card CSD
	TextView deviceModelNameTextView;	//show device model name
	TextView platPhotoSpeedSeekBarValueTextView;
	SeekBar  playPhotoSpeedSeekBar;
	int playPhotoIntervalTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initialize();
		setAppTitle();
		
		playPhotoSpeedSeekBar.setMax(8);//setMax( (max - min) / step ); max:1000,min:200,step:100
		playPhotoSpeedSeekBar.setProgress(8);
		
		loadSDcardSetting();
		new Thread(new Runnable() {
			public void run() {
				try {
					//show SD card info
					showSDcardInfo();
					
					//connect database
					switch (loadRevolutionFromDatabase()) {
					case 1:
						sendtoStatusTextView(databaseStatusTextView,"connection failed",false);
						loadRevolutionFromAPI();
						break;
					case 2:
						sendtoStatusTextView(databaseStatusTextView,"no device data",false);
						loadRevolutionFromAPI();
						break;
					default:
						sendtoStatusTextView(databaseStatusTextView,"connected",true);
						break;
					}
				} catch (Exception e) {
					//logData("TestProcedureListener: " + e);
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void initialize() {
		databaseStatusTextView = (TextView) this.findViewById(R.id.databaseStatusTextView);
		sdcardNameTextView = (TextView) this.findViewById(R.id.sdCardNameTextView);
		sdcardCIDTextView = (TextView) this.findViewById(R.id.sdCardCIDTextView);
		sdcardCSDTextView = (TextView) this.findViewById(R.id.sdCardCSDTextView);
		deviceModelNameTextView = (TextView) this.findViewById(R.id.deviceModelNameTextView);
		platPhotoSpeedSeekBarValueTextView = (TextView) this.findViewById(R.id.platPhotoSpeedSeekBarValueTextView);
		part1Button = (Button) this.findViewById(R.id.part1Button);
		part2Button = (Button) this.findViewById(R.id.part2Button);
		sdcardPositionCheckBox = (CheckBox) this.findViewById(R.id.SDcardPositionCheckBox);
		playPhotoSpeedSeekBar = (SeekBar) this.findViewById(R.id.platPhotoSpeedSeekBar);
		part1Button.setOnClickListener(ProcedureLister);
		part2Button.setOnClickListener(ProcedureLister);
		playPhotoSpeedSeekBar.setOnSeekBarChangeListener(SeekBarLister);
		if(Build.VERSION.SDK_INT<23)
			sdcardPositionCheckBox.setVisibility(View.INVISIBLE);
		deviceModelNameTextView.setText(android.os.Build.MODEL);
		
	}
	
	//setting app's title include version name
	private void setAppTitle() {
		try {
			PackageInfo pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			setTitle(getResources().getString(R.string.app_name)+"( v."+pkgInfo.versionName+" )");
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//ButtonListener
	private View.OnClickListener ProcedureLister = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			saveSDcardSetting();
			if (v == part1Button) {
				changeActivity(1);
			}
			//part2 button clicked
			if (v == part2Button) {
				changeActivity(2);
			}
		}
	};
	
	//SeekBarListener
	private SeekBar.OnSeekBarChangeListener SeekBarLister = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
       
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //description.setText("ss");
        }
        
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromUser) {
        	//min:200,step:100,max:1000
        	playPhotoIntervalTime = 200 + (progress * 100);
        	platPhotoSpeedSeekBarValueTextView.setText(playPhotoIntervalTime+" ms");
        }
    };

	//load video and photo resolution from database
	private int loadRevolutionFromDatabase(){
		String deviceModel = android.os.Build.MODEL;
		try {
			JSONObject jsonData;
			String result = connectDatabase("SELECT Video_Pixel,Pic_Pixel FROM pixel WHERE Name=\'"+deviceModel+"\'");
			if(result==null)
				return 1;	//connection failed
			else if(result.equals("-1"))
				return 2;	//no device data
			//json parser
			JSONArray jsonArray = new JSONArray(result);
			for(int i = 0; i < jsonArray.length(); i++) {
				jsonData = jsonArray.getJSONObject(i);
				deviceDefaultCameraVideoResolution = jsonData.getString("Video_Pixel").split(" */ *");
				deviceDefaultCameraPhotoResolution = jsonData.getString("Pic_Pixel").split(" */ *");
	        }
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	//connect database
	private String connectDatabase(String queryString) {
		String result = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://127.0.0.1/android.php");
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("query_string", queryString));
            httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpPost);
     
            //check connection status 200=OK
            if(httpResponse.getStatusLine().getStatusCode()!=200)
            	return null;
            
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);
            StringBuilder builder = new StringBuilder();
            String line = null;
            while((line = bufReader.readLine()) != null) {
            	if(line.equals("null"))
            		return "-1";
                builder.append(line + "\n");
            }
            inputStream.close();
            result = builder.toString();
        } catch(Exception e) {
        	e.printStackTrace();
            Log.e("TestProc", e.toString());
        }
        return result;
	}
	//if database connection failed,load resolution from API
	private int loadRevolutionFromAPI() {
		Camera camera = null;
		CameraController cameraContrl = new CameraController(camera);
		String resolutionTmp = null;
		if(!cameraContrl.openCamera())
			return 1;
		//load video resolution
		List<Size> sizeList = cameraContrl.getSupportedVideoSizes();
		deviceDefaultCameraVideoResolution = new CharSequence[sizeList.size()];
		for(int i=0; i<sizeList.size(); i++) {
			if(sizeList.get(i).width<sizeList.get(i).height)
				resolutionTmp = sizeList.get(i).height + "x" + sizeList.get(i).width;
			else
				resolutionTmp = sizeList.get(i).width + "x" + sizeList.get(i).height;
			deviceDefaultCameraVideoResolution[i] = resolutionTmp;
		}
		//load photo resolution
		sizeList = cameraContrl.getSupportedPhotoSizes();
		deviceDefaultCameraPhotoResolution = new CharSequence[sizeList.size()];
		for(int i=0; i<sizeList.size(); i++) {
			if(sizeList.get(i).width<sizeList.get(i).height)
				resolutionTmp = sizeList.get(i).height + "x" + sizeList.get(i).width;
			else
				resolutionTmp = sizeList.get(i).width + "x" + sizeList.get(i).height;
			deviceDefaultCameraPhotoResolution[i] = resolutionTmp;
		}
		cameraContrl.closeCamera();
		return 0;
	}
	
	//load SD card checkBox
	private boolean loadSDcardSetting() {
		try {
			BufferedReader settingReader = new BufferedReader(new FileReader(TestProcActivity.getLogFileDirPath() + "/SettingData"));
			String strTmp;
			String[] strSplitTmp;
			while((strTmp = settingReader.readLine()) != null) {
				strSplitTmp = strTmp.split(" ");
				if(strSplitTmp[0].equals("sdcardSetting")){
					if(strSplitTmp[1].equals("1"))
						sdcardPositionCheckBox.setChecked(true);
					else
						sdcardPositionCheckBox.setChecked(false);
				}

				if(strSplitTmp[0].equals("playPhotoInterval")){
					playPhotoIntervalTime = Integer.parseInt(strSplitTmp[1]);
					playPhotoSpeedSeekBar.setProgress((playPhotoIntervalTime-200)/100);
				}
			}
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	//save widget data
	private boolean saveSDcardSetting() {
		SharedPreferences settings = getSharedPreferences(getString(R.string.SettingData),0);
		settings.edit().putString("sdcardSetting", sdcardPositionCheckBox.isChecked() ? "1":"0").commit();
		settings.edit().putString("playPhotoInterval", Integer.toString(playPhotoIntervalTime)).commit();
		return true;
	}

	//show SD card detail Info
	private void showSDcardInfo() {
		Object localObject;
		String sdCardData;
		String sdcardInfoPath = "/sys/block/mmcblk1/device/";
		try {
			//SD card name
			localObject = new FileReader(sdcardInfoPath + "name");
			sdCardData = new BufferedReader((Reader)localObject).readLine();
	        sendtoStatusTextView(sdcardNameTextView,sdCardData,true);
	        //SD card CID
	        localObject = new FileReader(sdcardInfoPath + "cid");
			sdCardData = new BufferedReader((Reader)localObject).readLine();
			sendtoStatusTextView(sdcardCIDTextView,"0x"+sdCardData.toUpperCase(),true);
			//SD card CSD
	        localObject = new FileReader(sdcardInfoPath + "csd");
			sdCardData = new BufferedReader((Reader)localObject).readLine();
			sendtoStatusTextView(sdcardCSDTextView,"0x"+sdCardData.toUpperCase(),true);
			//SD card SCR
	        localObject = new FileReader(sdcardInfoPath + "scr");
			sdCardData = new BufferedReader((Reader)localObject).readLine();
	        Log.d("TestProc", "SD card SCR = " + sdCardData);
	       
	    } catch (Exception e) {
	    	// TODO Auto-generated catch block
	    	e.printStackTrace();
	    }
	}
	
	private void changeActivity(int part) {
		Intent intent = new Intent();
		intent.setClass(MainActivity.this, CheckVideoResolutionActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt("part", part);
		bundle.putInt("playPhotoInterval", playPhotoIntervalTime);
		bundle.putString("sdcardSetting", sdcardPositionCheckBox.isChecked() ? "1":"0");
		bundle.putCharSequenceArray("deviceDefaultCameraVideoResolutionArray", deviceDefaultCameraVideoResolution);
		bundle.putCharSequenceArray("deviceDefaultCameraPhotoResolutionArray", deviceDefaultCameraPhotoResolution);
		intent.putExtras(bundle);
		startActivity(intent);
		MainActivity.this.finish();
	}

	public void toast(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainActivity.this, x, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public static void showLogMessage(String message) {
		Log.e("TestProc", message);
	}
	
	public void sendtoStatusTextView(final TextView textView,final String x,final Boolean status) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textView.setText(x);
				if(status)
					textView.setTextColor(Color.parseColor("#008000"));
				else
					textView.setTextColor(Color.parseColor("#FF0000"));
			}
		});
	}

}
