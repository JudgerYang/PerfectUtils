package com.perfectcorp.utility.testbed;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.perfectcorp.pfutiltestbed.PhotoViewerActivity;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void onTestPanZoomImageViewClick(View v) {
		Intent intent = new Intent(this, PhotoViewerActivity.class);
		startActivity(intent);
	}
}
