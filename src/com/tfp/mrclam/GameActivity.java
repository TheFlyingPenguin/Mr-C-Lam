package com.tfp.mrclam;

import com.tfp.mrclam.ClamView.ClamThread;

import android.media.AudioManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.view.Menu;
import android.widget.Toast;

public class GameActivity extends Activity {
	
	private ClamThread mClamThread;
	private ClamView mClamView;
	private int mScore;
	private long mTime;
	private static final String PREFS_NAME = "HighScoreFile";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// get handles to the ClamView from XML, and its ClamThread
        mClamView = (ClamView) findViewById(R.id.clamView);
        mClamThread = mClamView.getThread();

        mClamThread.setState(ClamThread.STATE_READY);
 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
		return true;
	}
	
	public void endgame(int score){
		mScore = score;
		showDialog();
	}
	
	public int getScore(){
		return mScore;
	}
	
	/**
	 * Get current high score, persistent between sessions.
	 * @return
	 */
	public int getHighscore(){

		SharedPreferences highScorePref = getSharedPreferences(PREFS_NAME, 0);
		int highscore = highScorePref.getInt("Highscore", 0);
		return highscore;
	}
	
	/**
	 * Set a high score, persistent between sessions.
	 * @param highscore
	 */
	public void setHighscore(int highscore){
		
		SharedPreferences highScorePref = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = highScorePref.edit();
		editor.putInt("Highscore", highscore);
		
		editor.commit();		
	}
	
	public boolean getMusic(){
		return getIntent().getBooleanExtra(MenuActivity.MUSIC_TAG, false);
	}
	
	public boolean getSound(){
		return getIntent().getBooleanExtra(MenuActivity.SOUND_TAG, false);
	}
	
	public void showDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new EndgameDialogFragment();
        dialog.show(getFragmentManager(), "NoticeDialogFragment");
    }


    public void reset() {
        // User touched the dialog's positive button
    	mClamView.reset();
    }

    public void exit() {
        // User touched the dialog's negative button
    	finish();
    }
    
    @Override
    public void onBackPressed(){
    	long time = System.currentTimeMillis();
    	if (time < mTime+300){
    		finish();
    	}else{
    		mTime = System.currentTimeMillis();
    		
    		Toast.makeText(getApplicationContext(), "Press back again to exit.", Toast.LENGTH_SHORT).show();
    	}
    	
    }
    

}
