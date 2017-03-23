package com.example.sdcardtest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CheckVideoResolutionActivity extends Activity {

	
	CameraController cameraContrl;
	private int procPart = 0; // test Procedure Part I/II
	String sdcardSetting;
	CharSequence[] testVideoResolutionArray;//send to TestProc activity data
	private CharSequence[] deviceDefaultCameraVideoResolution; //user selected test video resolution 
	private CharSequence[] deviceDefaultCameraPhotoResolution; //user selected test photo resolution
	ArrayList<String> videoResolutionArrayList;//video resolution dialog list
	ArrayAdapter<String> videoResolutionArrayAdapter;
	ListView videoResolutionListView;
	boolean[] videoResolutionIsChecked;//record which item be clicked
	Button nextButton;
	Button prevButton;
	int playPhotoIntervalTime;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_check_video_resolution);
		prevButton = (Button) this.findViewById(R.id.video_cancelButton);
		nextButton = (Button) this.findViewById(R.id.video_okButton);
		prevButton.setOnClickListener(ProcedureLister);
		nextButton.setOnClickListener(ProcedureLister);
		videoResolutionArrayList = new ArrayList<String>();
		Camera camera = null;
		cameraContrl = new CameraController(camera);
		getBundleData();
		//load from database
		checkPhoneVideoSupportedSizes();
		//load previous setting
		loadResolutionSetting();
		initializeVideoResolutionList();
	}
	
	private void initializeVideoResolutionList() {
		videoResolutionArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice,
				videoResolutionArrayList){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
            	View view = super.getView(position, convertView, parent);
                //If API cannot support this video resolution,set red color.
                if(testVideoResolutionArray[position]==null) {
                    TextView tv = (TextView) view.findViewById(android.R.id.text1);
                    tv.setTextColor(Color.RED);
                    tv.setTypeface(null, Typeface.BOLD);
                }
                return view;
            }
        };
		
		videoResolutionListView = (ListView)findViewById(R.id.videoResolutionListView);
		videoResolutionListView.setAdapter(videoResolutionArrayAdapter);
		//Setting API supported video resolution be clicked on listView
		for(int i=0;i<videoResolutionIsChecked.length;i++)
			if(videoResolutionIsChecked[i]==true) 
				videoResolutionListView.setItemChecked(i,true);
		videoResolutionListView.setOnItemClickListener(listViewItemListener);
	}
	
	//set video & photo revolution list data
	private OnItemClickListener listViewItemListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			boolean isChecked;
			if(testVideoResolutionArray[position]==null)
				videoResolutionListView.setItemChecked(position, false);
			else {
				isChecked = videoResolutionListView.isItemChecked(position);
				videoResolutionIsChecked[position] = isChecked;
			}
		}
	};
	
	//
	public void checkPhoneVideoSupportedSizes() {
		String resolutionType;
		String resolutionTmp = null;
		//init camera
		if(!cameraContrl.openCamera())
			return;
		List<Size> sizeList;
		try {
			//load from database
			testVideoResolutionArray = new CharSequence[deviceDefaultCameraVideoResolution.length];
			videoResolutionIsChecked = new boolean[deviceDefaultCameraVideoResolution.length];
			
			for(int i=0;i<deviceDefaultCameraVideoResolution.length;i++) {
				resolutionTmp = deviceDefaultCameraVideoResolution[i].toString();
				testVideoResolutionArray[i] = resolutionTmp;
				videoResolutionIsChecked[i] = false;
				if((resolutionType = cameraContrl.getVideoResolutionType(resolutionTmp))!=null)
					resolutionTmp += " " + resolutionType;
				videoResolutionArrayList.add(resolutionTmp);
			}
			
			//check video resolution from API
			sizeList = cameraContrl.getSupportedVideoSizes();
			int i,j;
			for (i=0; i<testVideoResolutionArray.length; i++) {
				for(j=0; j<sizeList.size(); j++) {
					if(sizeList.get(j).width<sizeList.get(j).height)
						resolutionTmp = sizeList.get(j).height + "x" + sizeList.get(j).width;
					else
						resolutionTmp = sizeList.get(j).width + "x" + sizeList.get(j).height;
					if(testVideoResolutionArray[i].toString().equals(resolutionTmp)){
						break;
					}
				}
				if(j==sizeList.size())
					testVideoResolutionArray[i] = null;
			}
			//close camera
			cameraContrl.closeCamera();
		} catch (Exception e) {
			// logData("getPhoneSupportedSizes(): " + e);
			e.printStackTrace();
		}
	}
	
	//ButtonListener
	private View.OnClickListener ProcedureLister = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//cancel button clicked
			if (v == prevButton) {
				changeActivity(false);
			}
			//ok button clicked
			if (v == nextButton) {
				saveUserChoice();
				changeActivity(true);
			}
		}
	};
	
	//save user choice on video & photo dialog & sdcardSetting checkBox
	private void saveUserChoice() {
		for(int i=0;i<videoResolutionIsChecked.length;i++)
			if(videoResolutionIsChecked[i]==false)
				testVideoResolutionArray[i] = null;
	}
	
	//load user choice on video
	private boolean loadResolutionSetting() {
		try {
			BufferedReader settingReader = new BufferedReader(new FileReader(TestProcActivity.getLogFileDirPath() + "/SettingData"));
			String strTmp;
			String[] strSplitTmp;
			
			while((strTmp = settingReader.readLine()) != null) {
				strSplitTmp = strTmp.split(" ");
				if(strSplitTmp[0].equals("video")) {
					//load video resolution
					for(int i = 0;i<strSplitTmp.length;i++)
						for(int j = 0;j<testVideoResolutionArray.length;j++)
							if(strSplitTmp[i].equals(testVideoResolutionArray[j]))
								videoResolutionIsChecked[j] = true;
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

	public void toast(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(CheckVideoResolutionActivity.this, x, Toast.LENGTH_LONG).show();
			}
		});
	}

	private void getBundleData() {
		Bundle bundle = this.getIntent().getExtras();
		procPart = bundle.getInt("part");
		playPhotoIntervalTime = bundle.getInt("playPhotoInterval");
		deviceDefaultCameraVideoResolution = bundle.getCharSequenceArray("deviceDefaultCameraVideoResolutionArray");
		deviceDefaultCameraPhotoResolution = bundle.getCharSequenceArray("deviceDefaultCameraPhotoResolutionArray");
		sdcardSetting = bundle.getString("sdcardSetting");
	}
	
	private void changeActivity(boolean nextButtonClicked) {
		Intent intent = new Intent();
		if(nextButtonClicked) {
			intent.setClass(CheckVideoResolutionActivity.this, CheckPhotoResolutionActivity.class);
			Bundle bundle = new Bundle();
			bundle.putInt("part", procPart);
			bundle.putInt("playPhotoInterval", playPhotoIntervalTime);
			bundle.putString("sdcardSetting", sdcardSetting);
			bundle.putCharSequenceArray("deviceDefaultCameraVideoResolutionArray", deviceDefaultCameraVideoResolution);
			bundle.putCharSequenceArray("deviceDefaultCameraPhotoResolutionArray", deviceDefaultCameraPhotoResolution);
			bundle.putCharSequenceArray("testVideoResolutionArray", testVideoResolutionArray);
			intent.putExtras(bundle);
			startActivity(intent);
			
		} else {
			intent.setClass(CheckVideoResolutionActivity.this, MainActivity.class);
			startActivity(intent);
		}
		CheckVideoResolutionActivity.this.finish();
	}
}
