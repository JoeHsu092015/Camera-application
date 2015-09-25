package com.example.sdcardtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Button part1Button;
	private Button part2Button;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initialize();

	}

	private void initialize() {

		part1Button = (Button) this.findViewById(R.id.part1Button);
		part2Button = (Button) this.findViewById(R.id.part2Button);

		part1Button.setOnClickListener(ProcedureLister);
		part2Button.setOnClickListener(ProcedureLister);
	}

	private View.OnClickListener ProcedureLister = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int part = 0;
			if (v == part1Button)
				part = 1;
			if (v == part2Button)
				part = 2;
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, TestProcActivity.class);
			Bundle bundle = new Bundle();
			bundle.putInt("part", part);
			intent.putExtras(bundle);
			startActivity(intent);
			MainActivity.this.finish();

		}
	};

}
