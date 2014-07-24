package com.tfp.mrclam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class EndgameDialogFragment extends DialogFragment{
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		final GameActivity activity = (GameActivity)getActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		LayoutInflater inflater = activity.getLayoutInflater();
		
		final View endgameView = inflater.inflate(R.layout.dialog_endgame, null);
		
		this.setCancelable(false);
		
		builder.setMessage(R.string.dialog_endgame)
			.setView(endgameView)
			.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
				public void onClick (DialogInterface dialog, int id){
					activity.reset();
				}
			})
			.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id){
					activity.exit();
				}
			});
		
		int score = activity.getScore();
		int highscore = activity.getHighscore();
		
		if (score > highscore){
			highscore = score;
			activity.setHighscore(score);
		}
		
		TextView scoreText = (TextView)endgameView.findViewById(R.id.scoreText);
		scoreText.setText(Integer.toString(score));
		
		//TODO implement high score
		TextView highscoreText = (TextView)endgameView.findViewById(R.id.highscoreText);
		highscoreText.setText(Integer.toString(highscore));
		
		
		return builder.create();
	}
	
}
