package com.tfp.mrclam;

import android.media.AudioManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class MenuActivity extends Activity {
	
	private boolean mIsSound;
	private boolean mIsMusic;
	private boolean mFirst;
	public static final String MUSIC_TAG = "com.tfp.mrclam.music";
	public static final String SOUND_TAG = "com.tfp.mrclam.sound";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		
		mFirst = true;	
		mIsMusic = true;
		mIsSound = true;
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	
	public void startListener (View v){
		Intent intent;
		if (mFirst){
			intent = new Intent(MenuActivity.this, StoryActivity.class);
			mFirst = false;
		}else{
			intent = new Intent(MenuActivity.this, GameActivity.class);
		}
			intent.putExtra(MUSIC_TAG, mIsMusic);
			intent.putExtra(SOUND_TAG, mIsSound);
			MenuActivity.this.startActivity(intent);
	}

	//Button allows user to stop and exit application from menu screen
	public void exitListener (View v){
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onTouchMusic(View v){
		mIsMusic = !mIsMusic;
		if (mIsMusic){
			v.setBackground(getResources().getDrawable(R.drawable.music_on));
		}else{
			v.setBackground(getResources().getDrawable(R.drawable.music_off));
		}
	}
	
	public void onTouchSound(View v){
		mIsSound = !mIsSound;
		if (mIsSound){
			v.setBackground(getResources().getDrawable(R.drawable.sound_on));
		}else{
			v.setBackground(getResources().getDrawable(R.drawable.sound_off));
		}		
	}

}
