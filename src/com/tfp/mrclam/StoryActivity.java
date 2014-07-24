package com.tfp.mrclam;

import android.media.AudioManager;
import android.os.Bundle;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;



public class StoryActivity extends Activity {

	private ImageSwitcher storySwitcher;
	private int[] imgs;
	private int c;
	private boolean mIsMusic;
	private boolean mIsSound;
	private boolean started;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_story);
		
		Intent i = getIntent();
		
		mIsMusic = i.getBooleanExtra(MenuActivity.MUSIC_TAG, false);
		mIsSound = i.getBooleanExtra(MenuActivity.SOUND_TAG, false);
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		storySwitcher = (ImageSwitcher)findViewById(R.id.imageSwitcher);

		storySwitcher.setFactory(new ViewFactory(){
			@Override
			public View makeView(){
				ImageView myView = new ImageView(getApplicationContext());
				myView.setScaleType(ImageView.ScaleType.FIT_CENTER);
				myView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
				
				return myView;
			}
		});
		
		//Animation fadeIn = AnimationUtils.loadAnimation(this,  android.R.anim.fade_in);
		//Animation fadeOut = AnimationUtils.loadAnimation(this,  android.R.anim.fade_out);
		
		//storySwitcher.setInAnimation(fadeIn);
		//storySwitcher.setOutAnimation(fadeOut);
		
		imgs = new int[10];
		imgs[0] = R.drawable.story1;
		imgs[1] = R.drawable.story2;
		imgs[2] = R.drawable.story3;
		imgs[3] = R.drawable.story4;
		imgs[4] = R.drawable.story5;
		imgs[5] = R.drawable.story6;
		imgs[6] = R.drawable.story7;
		imgs[7] = R.drawable.story8;
		imgs[8] = R.drawable.story9;
		imgs[9] = R.drawable.story10;
		
		c = 0;
		
		storySwitcher.setImageResource(imgs[c]);
		
		started = false;
		
		Toast.makeText(this, "Touch to progress.", Toast.LENGTH_SHORT).show();
	}
	
	public void onTouch(View v){
		if (started) return;
		c++;
		if (c<imgs.length){
			storySwitcher.setImageResource(imgs[c]);
		}else{
			started = true;
			startGame();
		}		
	}
	
	public void onSkip(View v){
		if (started) return;
		started = true;
		startGame();
	}
	
	private void startGame(){
		imgs = null;
		storySwitcher.setImageResource(R.drawable.loading);
		System.gc();
		
		Intent intent = new Intent(StoryActivity.this, GameActivity.class);
		
		intent.putExtra(MenuActivity.MUSIC_TAG, mIsMusic);
		intent.putExtra(MenuActivity.SOUND_TAG, mIsSound);
		
		StoryActivity.this.startActivity(intent);
		
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.story, menu);
		return true;
	}

}
