package com.example.sdcardtest;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {

	/*
	 * Button part1Button: sdcard test procedure run 1 time 
	 * part2Button: sdcard test procedure run until sdcard capacity full 
	 * videoResolutionButton: user selects test procedure video resolutions from phone support resolutions
	 * photoResolutionButton: user selects test procedure photo resolutions from phone support resolutions
	 */
	Button part1Button;
	Button part2Button;
	Button videoResolutionButton;
	Button photoResolutionButton;
	Camera camera;
	final int VIDEO_TYPE = 0;
	final int PHOTO_TYPE = 1;
	CharSequence[] videoSizeArray;
	CharSequence[] photoSizeArray;
	boolean[] videoResolutionIsChecked;
	boolean[] photoResolutionIsChecked;
	boolean setVideoResolution = false;
	boolean setPhotoResolution = false;
	boolean videoDialogSelectAllClicked = false;
	boolean photoDialogSelectAllClicked = false;
	ArrayList<String> videoResolutionArrayList;
	ArrayList<String> photoResolutionArrayList;
	ArrayAdapter<String> videoResolutionArrayAdapter;
	ArrayAdapter<String> photoResolutionArrayAdapter;
	AlertDialog.Builder resolutionDialogBuilder;
	AlertDialog resolutionDialog;
	ListView videoResolutionListView;
	ListView photoResolutionListView;
	Button selectAllButton;
	int currentDialog = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initialize();
		getPhoneSupportedSizes(VIDEO_TYPE);
		getPhoneSupportedSizes(PHOTO_TYPE);
	}

	private void initialize() {

		part1Button = (Button) this.findViewById(R.id.part1Button);
		part2Button = (Button) this.findViewById(R.id.part2Button);
		videoResolutionButton = (Button) this.findViewById(R.id.videoResolutionButton);
		photoResolutionButton = (Button) this.findViewById(R.id.photoResolutionButton);

		part1Button.setOnClickListener(ProcedureLister);
		part2Button.setOnClickListener(ProcedureLister);
		videoResolutionButton.setOnClickListener(ProcedureLister);
		photoResolutionButton.setOnClickListener(ProcedureLister);
		videoResolutionArrayList = new ArrayList<String>();
		photoResolutionArrayList = new ArrayList<String>();
		resolutionDialogBuilder = new AlertDialog.Builder(MainActivity.this);
		videoResolutionArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice,
				videoResolutionArrayList);
		photoResolutionArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice,
				photoResolutionArrayList);
	}

	/*
	 * ButtonListener
	 */
	private View.OnClickListener ProcedureLister = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int part = 0;
			if (v == part1Button) {
				part = 1;
				if ((setVideoResolution == true) && (setPhotoResolution == true)) {
					checkUserChoice();
					changeActivity(part);
				} else {
					toast("Please set video/photo resolution");
				}
			}
			if (v == part2Button) {
				part = 2;
				if ((setVideoResolution == true) && (setPhotoResolution == true)) {
					checkUserChoice();
					changeActivity(part);
				} else {
					toast("Please set video/photo resolution");
				}
			}
			if (v == videoResolutionButton) {
				currentDialog = VIDEO_TYPE;
				setVideoResolution = true;
				setAlertDialog(currentDialog);
			}
			if (v == photoResolutionButton) {
				currentDialog = PHOTO_TYPE;
				setPhotoResolution = true;
				setAlertDialog(currentDialog);
			}
		}
	};

	private void setAlertDialog(int dialog) {

		if ((dialog != VIDEO_TYPE) && (dialog != PHOTO_TYPE)) {
			toast("Dialog Type Error");
			return;
		}
		resolutionDialogBuilder.setTitle("Support resolution");
		if (dialog == VIDEO_TYPE)
			resolutionDialogBuilder.setMultiChoiceItems(R.array.video_resolution_items, null, null);
		else if (dialog == PHOTO_TYPE) 
			resolutionDialogBuilder.setMultiChoiceItems(R.array.photo_resolution_items, null, null);
		
		resolutionDialogBuilder.setNegativeButton("Select/Deselect All", null);
		resolutionDialogBuilder.setPositiveButton("OK", null);
		resolutionDialog = resolutionDialogBuilder.create();
		resolutionDialog.show();

		if (dialog == VIDEO_TYPE) {
			videoResolutionListView = resolutionDialog.getListView();
			videoResolutionListView.setAdapter(videoResolutionArrayAdapter);
			for (int i = 0; i < videoResolutionIsChecked.length; i++)
				if (videoResolutionIsChecked[i] == true)
					videoResolutionListView.setItemChecked(i, true);
		} else if (dialog == PHOTO_TYPE) {
			photoResolutionListView = resolutionDialog.getListView();
			photoResolutionListView.setAdapter(photoResolutionArrayAdapter);
			for (int i = 0; i < photoResolutionIsChecked.length; i++)
				if (photoResolutionIsChecked[i] == true)
					photoResolutionListView.setItemChecked(i, true);
		}

		Button okButton = resolutionDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		okButton.setText("OK");
		okButton.setOnClickListener(clickOKButton);
		selectAllButton = resolutionDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (((dialog == VIDEO_TYPE) && videoDialogSelectAllClicked)
				|| ((dialog == PHOTO_TYPE) && photoDialogSelectAllClicked))
			selectAllButton.setText("Deselect All");
		else
			selectAllButton.setText("Select All");

		selectAllButton.setOnClickListener(clickSelectAllButton);

		if (dialog == VIDEO_TYPE) {
			videoResolutionListView.setOnItemClickListener(listViewItemListener);
		} else if (dialog == PHOTO_TYPE) {
			photoResolutionListView.setOnItemClickListener(listViewItemListener);
		}

	}

	private OnItemClickListener listViewItemListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			boolean isChecked;

			if (currentDialog == VIDEO_TYPE) {
				isChecked = videoResolutionListView.isItemChecked(position);
				videoResolutionIsChecked[position] = isChecked;
				if (videoDialogSelectAllClicked && (!isChecked)) {
					changeButtonText("Select all");
					videoResolutionArrayAdapter.notifyDataSetChanged();
					videoDialogSelectAllClicked = false;
				}
			} else if (currentDialog == PHOTO_TYPE) {
				isChecked = photoResolutionListView.isItemChecked(position);
				photoResolutionIsChecked[position] = isChecked;
				if (photoDialogSelectAllClicked && (!isChecked)) {
					changeButtonText("Select all");
					photoResolutionArrayAdapter.notifyDataSetChanged();
					photoDialogSelectAllClicked = false;
				}
			}
		}
	};

	private View.OnClickListener clickOKButton = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			resolutionDialog.dismiss();
		}
	};

	private View.OnClickListener clickSelectAllButton = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (currentDialog == VIDEO_TYPE) {
				if (!videoDialogSelectAllClicked) {
					videoDialogSelectAllClicked = true;
					changeButtonText("Deselect all");
					for (int i = 0; i < videoResolutionIsChecked.length; i++) {
						videoResolutionListView.setItemChecked(i, true);
						videoResolutionIsChecked[i] = true;
					}
				} else {
					videoDialogSelectAllClicked = false;
					changeButtonText("Select all");
					for (int i = 0; i < videoResolutionIsChecked.length; i++) {
						videoResolutionListView.setItemChecked(i, false);
						videoResolutionIsChecked[i] = false;
					}
				}
			} else if (currentDialog == PHOTO_TYPE) {
				if (!photoDialogSelectAllClicked) {
					photoDialogSelectAllClicked = true;
					changeButtonText("Deselect all");
					for (int i = 0; i < photoResolutionIsChecked.length; i++) {
						photoResolutionListView.setItemChecked(i, true);
						photoResolutionIsChecked[i] = true;
					}
				} else {
					photoDialogSelectAllClicked = false;
					changeButtonText("Select all");
					for (int i = 0; i < photoResolutionIsChecked.length; i++) {
						photoResolutionListView.setItemChecked(i, false);
						photoResolutionIsChecked[i] = false;
					}
				}
			} else {
				toast("Dialog Type Error");
			}
		}
	};

	private void checkUserChoice() {
		for (int i = 0; i < videoResolutionIsChecked.length; i++) {
			if (videoResolutionIsChecked[i] == false)
				videoSizeArray[i] = null;
		}

		for (int i = 0; i < photoResolutionIsChecked.length; i++) {
			if (photoResolutionIsChecked[i] == false)
				photoSizeArray[i] = null;
		}
	}

	public void getPhoneSupportedSizes(int type) {
		if ((type != VIDEO_TYPE) && (type != PHOTO_TYPE)) {
			toast("Error type : " + type);
			return;
		}

		List<Size> sizeList;
		try {
			camera = Camera.open();
			if (type == VIDEO_TYPE) {
				sizeList = camera.getParameters().getSupportedVideoSizes();
				videoSizeArray = new CharSequence[sizeList.size()];
				videoResolutionIsChecked = new boolean[sizeList.size()];
				for (int i = 0; i < sizeList.size(); i++) {
					videoSizeArray[i] = sizeList.get(i).width + "x" + sizeList.get(i).height;
					videoResolutionArrayList.add(sizeList.get(i).width + "x" + sizeList.get(i).height);
				}
			} else if (type == PHOTO_TYPE) {
				sizeList = camera.getParameters().getSupportedPictureSizes();
				photoSizeArray = new CharSequence[sizeList.size()];
				photoResolutionIsChecked = new boolean[sizeList.size()];
				for (int i = 0; i < sizeList.size(); i++) {
					photoSizeArray[i] = sizeList.get(i).width + "x" + sizeList.get(i).height;
					photoResolutionArrayList.add(sizeList.get(i).width + "x" + sizeList.get(i).height);
				}
			}
			camera.release();
		} catch (Exception e) {
			// logData("getPhoneSupportedSizes(): " + e);
			toast("" + e);
			e.printStackTrace();
		}
	}

	public void toast(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainActivity.this, x, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void changeButtonText(final String x) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				selectAllButton.setText(x);
			}
		});
	}

	private void changeActivity(int part) {
		Intent intent = new Intent();
		intent.setClass(MainActivity.this, TestProcActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt("part", part);
		bundle.putCharSequenceArray("videoSizeArray", videoSizeArray);
		bundle.putCharSequenceArray("photoSizeArray", photoSizeArray);
		intent.putExtras(bundle);
		startActivity(intent);
		MainActivity.this.finish();
	}

}
