package com.example.sdcardtest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

public class CheckTestTypeActivity extends Activity {

	Button nextButton;
	Button prevButton;
	RadioGroup radioGroup;
	int procPart = 0; // test Procedure Part I/II
	String sdcardSetting;
	int testPattenType = 0;
	CharSequence[] testPhotoResolutionArray;//send to TestProc activity data
	CharSequence[] testVideoResolutionArray;//send to TestProc activity data
	private CharSequence[] deviceDefaultCameraVideoResolution; //user selected test video resolution
	private CharSequence[] deviceDefaultCameraPhotoResolution; //user selected test photo resolution
	int playPhotoIntervalTime;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_check_test_type);
		prevButton = (Button) this.findViewById(R.id.video_cancelButton);
		nextButton = (Button) this.findViewById(R.id.video_okButton);
		radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);

		prevButton.setOnClickListener(ProcedureLister);
		nextButton.setOnClickListener(ProcedureLister);
		getBundleData();
		loadTestTypeSetting();
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
				switch(radioGroup.getCheckedRadioButtonId()){
	                case R.id.radio0://sequence test(P1P2..V1V2..)
	                	testPattenType = 0;
	                	break;
	                case R.id.radio1://photo test(P1P2P3..)
	                	testPattenType = 1;
	                	testVideoResolutionArray = new CharSequence[0];
	                	break;
	                case R.id.radio2://video test(V1V2V3..)
	                	testPattenType = 2;
	                	testPhotoResolutionArray = new CharSequence[0];
	                	break;
	                case R.id.radio3://interaction test(P1V1P2V2..)
	                	testPattenType = 3;
	                	break;
				}
				
				saveUserChoice();
				changeActivity(true);
			}
		}
	};
	
	//save user choice on video & photo dialog & sdcardSetting checkBox
	private void saveUserChoice() {
		try {
			BufferedWriter	settingWriter = new BufferedWriter(new FileWriter(TestProcActivity.getLogFileDirPath() + "/SettingData", false));
			settingWriter.write("video ");
			for (int i = 0; i < testVideoResolutionArray.length; i++) {
				if (testVideoResolutionArray[i]!=null)
					settingWriter.write(testVideoResolutionArray[i].toString()+" ");
			}
			settingWriter.write("\nphoto ");
			for (int i = 0; i < testPhotoResolutionArray.length; i++) {
				if (testPhotoResolutionArray[i]!=null)
					settingWriter.write(testPhotoResolutionArray[i].toString()+" ");
			}
			
			settingWriter.write("\nsdcardSetting "+sdcardSetting);
			settingWriter.write("\ntestPattenType "+testPattenType);
			settingWriter.write("\nplayPhotoInterval "+playPhotoIntervalTime);
			
			settingWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//load user choice on video checkBox
	private boolean loadTestTypeSetting() {
		try {
			BufferedReader settingReader = new BufferedReader(new FileReader(TestProcActivity.getLogFileDirPath() + "/SettingData"));
			String strTmp;
			String[] strSplitTmp;
			
			while((strTmp = settingReader.readLine()) != null) {
				strSplitTmp = strTmp.split(" ");
				if(strSplitTmp[0].equals("testPattenType")) {
					//load test type
					for(int i = 0;i<strSplitTmp.length;i++)
						for(int j = 0;j<testVideoResolutionArray.length;j++) {
							switch (Integer.parseInt(strSplitTmp[1])) {
							case 0:
								radioGroup.check(R.id.radio0);
								break;
							case 1:
								radioGroup.check(R.id.radio1);
								break;
							case 2:
								radioGroup.check(R.id.radio2);
								break;
							case 3:
								radioGroup.check(R.id.radio3);
								break;
							default:
								break;
							}
						}
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
	
	
	private void getBundleData() {
		Bundle bundle = this.getIntent().getExtras();
		procPart = bundle.getInt("part");
		playPhotoIntervalTime = bundle.getInt("playPhotoInterval");
		deviceDefaultCameraVideoResolution = bundle.getCharSequenceArray("deviceDefaultCameraVideoResolutionArray");
		deviceDefaultCameraPhotoResolution = bundle.getCharSequenceArray("deviceDefaultCameraPhotoResolutionArray");
		testVideoResolutionArray = bundle.getCharSequenceArray("testVideoResolutionArray");
		testPhotoResolutionArray = bundle.getCharSequenceArray("testPhotoResolutionArray");
		sdcardSetting = bundle.getString("sdcardSetting");
		/*for(int i = 0; i < deviceDefaultCameraVideoResolution.length; i++)
			MainActivity.showLogMessage("video "+deviceDefaultCameraVideoResolution[i].toString());
		for(int i = 0; i < deviceDefaultCameraPhotoResolution.length; i++)
			MainActivity.showLogMessage("photo "+deviceDefaultCameraPhotoResolution[i].toString());
		for(int i = 0; i < testVideoResolutionArray.length; i++)
			if(testVideoResolutionArray[i]!=null)
				MainActivity.showLogMessage("test video "+testVideoResolutionArray[i].toString());
		for(int i = 0; i < testPhotoResolutionArray.length; i++)
			if(testPhotoResolutionArray[i]!=null)
				MainActivity.showLogMessage("test photo "+testPhotoResolutionArray[i].toString());*/
		
	}
	
	private void changeActivity(boolean nextButtonClicked) {
		Intent intent = new Intent();
		if(nextButtonClicked) {
			intent.setClass(CheckTestTypeActivity.this, TestProcActivity.class);
			Bundle bundle = new Bundle();
			bundle.putInt("part", procPart);
			bundle.putInt("testPattenType", testPattenType);
			bundle.putCharSequenceArray("testVideoResolutionArray", testVideoResolutionArray);
			bundle.putCharSequenceArray("testPhotoResolutionArray", testPhotoResolutionArray);
			bundle.putString("sdcardSetting", sdcardSetting);
			intent.putExtras(bundle);
			startActivity(intent);
			
		} else {
			intent.setClass(CheckTestTypeActivity.this, CheckPhotoResolutionActivity.class);
			Bundle bundle = new Bundle();
			bundle.putInt("part", procPart);
			bundle.putInt("playPhotoInterval",playPhotoIntervalTime);
			bundle.putCharSequenceArray("deviceDefaultCameraVideoResolutionArray", deviceDefaultCameraVideoResolution);
			bundle.putCharSequenceArray("deviceDefaultCameraPhotoResolutionArray", deviceDefaultCameraPhotoResolution);
			bundle.putCharSequenceArray("testVideoResolutionArray", testVideoResolutionArray);
			intent.putExtras(bundle);
			startActivity(intent);
			
		}
		CheckTestTypeActivity.this.finish();
		
	}
	
}
