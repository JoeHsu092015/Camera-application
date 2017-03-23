package com.example.sdcardtest;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.R.bool;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class CheckPhotoResolutionActivity extends Activity {

	CameraController cameraContrl;
	private int procPart = 0; // test Procedure Part I/II
	String sdcardSetting;
	CharSequence[] testPhotoResolutionArray;//send to TestProc activity data
	CharSequence[] testVideoResolutionArray;//send to TestProc activity data
	private CharSequence[] deviceDefaultCameraVideoResolution; //user selected test video resolution
	private CharSequence[] deviceDefaultCameraPhotoResolution; //user selected test photo resolution
	ArrayList<String> photoResolutionArrayList;//video resolution dialog list
	ArrayAdapter<String> photoResolutionArrayAdapter;
	ListView photoResolutionListView;
	boolean[] photoResolutionIsChecked;//record which item be clicked
	Button nextButton;
	Button prevButton;
	int playPhotoIntervalTime;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_check_photo_resolution);
		prevButton = (Button) this.findViewById(R.id.photo_cancelButton);
		nextButton = (Button) this.findViewById(R.id.photo_okButton);
		prevButton.setOnClickListener(ProcedureLister);
		nextButton.setOnClickListener(ProcedureLister);
		photoResolutionArrayList = new ArrayList<String>();
		Camera camera = null;
		cameraContrl = new CameraController(camera);
		getBundleData();
		//load database data
		checkPhonePhotoSupportedSizes();
		//load previous setting
		loadResolutionSetting();
		initializeVideoResolutionList();
		
	}
	
	private void initializeVideoResolutionList() {
		photoResolutionArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice,
				photoResolutionArrayList){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);
                if(testPhotoResolutionArray[position]==null) {
                	 // Initialize a TextView for ListView each Item
                    TextView tv = (TextView) view.findViewById(android.R.id.text1);
                    // Set the text color of TextView (ListView Item)
                    tv.setTextColor(Color.RED);
                    tv.setTypeface(null, Typeface.BOLD);
                    // Generate ListView Item using TextView
                }
                return view;
            }
        };
		
        photoResolutionListView = (ListView)findViewById(R.id.photoResolutionListView);
        photoResolutionListView.setAdapter(photoResolutionArrayAdapter);
        
        //Setting API supported video resolution be clicked on listView
        for(int i=0;i<photoResolutionIsChecked.length;i++)
  			if(photoResolutionIsChecked[i]==true)
  				photoResolutionListView.setItemChecked(i,true);
        photoResolutionListView.setOnItemClickListener(listViewItemListener);
	}
	
	//set video & photo revolution list data
	private OnItemClickListener listViewItemListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			boolean isChecked;
			if(testPhotoResolutionArray[position]==null)
				photoResolutionListView.setItemChecked(position, false);
			else {
				isChecked = photoResolutionListView.isItemChecked(position);
				photoResolutionIsChecked[position] = isChecked;
			}
			/*if (videoDialogSelectAllClicked && (!isChecked)) {
				videoResolutionArrayAdapter.notifyDataSetChanged();
				videoDialogSelectAllClicked = false;
			}*/
		}
	};	
	
	//
	public void checkPhonePhotoSupportedSizes() {
	
		String resolutionTmp = null;

		if(!cameraContrl.openCamera())
			return;
		List<Size> sizeList;
		try {
			testPhotoResolutionArray = new CharSequence[deviceDefaultCameraPhotoResolution.length];
			photoResolutionIsChecked = new boolean[deviceDefaultCameraPhotoResolution.length];
			
			for(int i=0;i<deviceDefaultCameraPhotoResolution.length;i++) {
				resolutionTmp = deviceDefaultCameraPhotoResolution[i].toString();
				testPhotoResolutionArray[i] = resolutionTmp;
				photoResolutionIsChecked[i] = false;
				photoResolutionArrayList.add(resolutionTmp);
			}
			
			sizeList = cameraContrl.getSupportedPhotoSizes();
			for(int j=0; j<sizeList.size(); j++) 
				MainActivity.showLogMessage("API photo "+sizeList.get(j).width + "x" + sizeList.get(j).height);
			//checking photo resolution from API.
			int i,j;
			for (i=0; i<testPhotoResolutionArray.length; i++) {
				for(j=0; j<sizeList.size(); j++) {
					if(sizeList.get(j).width < sizeList.get(j).height)
						resolutionTmp = sizeList.get(j).height + "x" + sizeList.get(j).width;
					else
						resolutionTmp = sizeList.get(j).width + "x" + sizeList.get(j).height;
					if(testPhotoResolutionArray[i].toString().equals(resolutionTmp)){
						break;
					}
				}
				if(j==sizeList.size()){
					MainActivity.showLogMessage("remove "+testPhotoResolutionArray[i]);
					testPhotoResolutionArray[i] = null;
				}
			}
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
		for(int i=0;i<photoResolutionIsChecked.length;i++)
			if(photoResolutionIsChecked[i]==false)
				testPhotoResolutionArray[i] = null;
	}	
	
	//load user choice on video checkBox
	private boolean loadResolutionSetting() {
		try {
			BufferedReader settingReader = new BufferedReader(new FileReader(TestProcActivity.getLogFileDirPath() + "/SettingData"));
			String strTmp;
			String[] strSplitTmp;
			int i,j;
			while((strTmp = settingReader.readLine()) != null) {
				strSplitTmp = strTmp.split(" ");
				if(strSplitTmp[0].equals("photo")){
					//load video resolution
					for(i = 0;i<strSplitTmp.length;i++)
						for(j = 0;j<testPhotoResolutionArray.length;j++) 
							if(strSplitTmp[i].equals(testPhotoResolutionArray[j])) 
								photoResolutionIsChecked[j] = true;
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
	
	private void changeActivity(boolean nextButtonClicked) {
		Intent intent = new Intent();
		if(nextButtonClicked) {
			intent.setClass(CheckPhotoResolutionActivity.this, CheckTestTypeActivity.class);
			Bundle bundle = new Bundle();
			bundle.putInt("part", procPart);
			bundle.putInt("playPhotoInterval", playPhotoIntervalTime);
			bundle.putString("sdcardSetting", sdcardSetting);
			bundle.putCharSequenceArray("deviceDefaultCameraVideoResolutionArray", deviceDefaultCameraVideoResolution);
			bundle.putCharSequenceArray("deviceDefaultCameraPhotoResolutionArray", deviceDefaultCameraPhotoResolution);
			bundle.putCharSequenceArray("testVideoResolutionArray", testVideoResolutionArray);
			bundle.putCharSequenceArray("testPhotoResolutionArray", testPhotoResolutionArray);
			intent.putExtras(bundle);
			startActivity(intent);
		}else {
			intent.setClass(CheckPhotoResolutionActivity.this, CheckVideoResolutionActivity.class);
			Bundle bundle = new Bundle();
			bundle.putInt("part", procPart);
			bundle.putInt("playPhotoInterval", playPhotoIntervalTime);
			bundle.putString("sdcardSetting", sdcardSetting);
			bundle.putCharSequenceArray("deviceDefaultCameraVideoResolutionArray", deviceDefaultCameraVideoResolution);
			bundle.putCharSequenceArray("deviceDefaultCameraPhotoResolutionArray", deviceDefaultCameraPhotoResolution);
			intent.putExtras(bundle);
			startActivity(intent);
		}
		CheckPhotoResolutionActivity.this.finish();
	}
	
	public void toast(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(CheckPhotoResolutionActivity.this, x, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	private void getBundleData() {
		Bundle bundle = this.getIntent().getExtras();
		procPart = bundle.getInt("part");
		playPhotoIntervalTime = bundle.getInt("playPhotoInterval");
		deviceDefaultCameraVideoResolution = bundle.getCharSequenceArray("deviceDefaultCameraVideoResolutionArray");
		deviceDefaultCameraPhotoResolution = bundle.getCharSequenceArray("deviceDefaultCameraPhotoResolutionArray");
		testVideoResolutionArray = bundle.getCharSequenceArray("testVideoResolutionArray");
		sdcardSetting = bundle.getString("sdcardSetting");
		/*for(int i = 0; i < deviceDefaultCameraVideoResolution.length; i++)
			MainActivity.showLogMessage("video "+deviceDefaultCameraVideoResolution[i].toString());
		for(int i = 0; i < deviceDefaultCameraPhotoResolution.length; i++)
			MainActivity.showLogMessage("photo "+deviceDefaultCameraPhotoResolution[i].toString());
		for(int i = 0; i < testVideoResolutionArray.length; i++)
			if(testVideoResolutionArray[i]!=null)
				MainActivity.showLogMessage("test video "+testVideoResolutionArray[i].toString());*/
		
	}
}
