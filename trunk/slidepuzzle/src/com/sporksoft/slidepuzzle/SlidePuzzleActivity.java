package com.sporksoft.slidepuzzle;

import java.util.ArrayList;

import com.qwapi.adclient.android.view.QWAdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnKeyListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class SlidePuzzleActivity extends Activity implements OnKeyListener {	
	private static final int MENU_NEW = 0;
	private static final int MENU_SCORES = 1;
	private static final int MENU_SETTINGS = 2;
	
	private ImageView mCompleteView;
	private TileView mTileView;
	private Chronometer mTimerView;
    private long mTime;
    private Toast mToast;

    private class ScoresListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton ) {
            switch (whichButton) {
                case AlertDialog.BUTTON1: {
                    showConfirmDeleteDialog();
                    return;
                }
                case AlertDialog.BUTTON2: {
                    mTimerView.setBase(SystemClock.elapsedRealtime() - mTime);
                    if (!mTileView.isSolved()) {
                        mTimerView.start();
                    }
                    return;
                }
            }
        }
    }

    private class ScoresCancelListener implements OnCancelListener {
        public void onCancel(DialogInterface dialog) {
            mTimerView.setBase(SystemClock.elapsedRealtime() - mTime);
            if (!mTileView.isSolved()) {
                mTimerView.start();
            }        
        }
    }
    
    private class ConfirmDeleteListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton ) {
            if (whichButton == AlertDialog.BUTTON1) {
                ScoreUtil.getInstance(SlidePuzzleActivity.this).clearScores();                
            }
            showHighScoreListDialog();
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
                
        setContentView(R.layout.slide_puzzle);
        mCompleteView = (ImageView) findViewById(R.id.complete_view);
        mTileView = (TileView) findViewById(R.id.tile_view);
        mTileView.requestFocus();
        mTileView.setOnKeyListener(this);
        
        mTimerView = (Chronometer) findViewById(R.id.timer_view);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mTimerView.setTextColor(prefs.getInt(PuzzlePreferenceActivity.TIMER_COLOR, getResources().getColor(R.drawable.default_fg_color)));
                
        if (icicle == null) {
            int blankLoc = Integer.parseInt(prefs.getString(PuzzlePreferenceActivity.BLANK_LOCATION, String.valueOf(1)));
            mTileView.newGame(null, blankLoc, mTimerView);
            mTime = 0;
        } else {
        	Parcelable[] parcelables = icicle.getParcelableArray("tiles");
        	Tile[] tiles = null;
        	if (parcelables != null) {
        		int len = parcelables.length;
        		
        		tiles = new Tile[len];
        		for (int i = 0; i < len; i++) {
        			tiles[i] = (Tile) parcelables[i];
        		}
        	}
        	
            mTileView.newGame(tiles, icicle.getInt("blank_first"), mTimerView);
            mTime = icicle.getLong("time", 0);
        }
        
       com.qwapi.adclient.android.AdApiRegistration.registerApplication(this); 
    }
    
    @Override
    public void onResume() {
    	super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    	if (prefs.getBoolean(PuzzlePreferenceActivity.SHOW_STATUS, true)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);       	    
    	} else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);   
    	}    	

    	mTileView.updateInstantPrefs();
        mTimerView.setBase(SystemClock.elapsedRealtime() - mTime);
    	if (!mTileView.isSolved()) {
    	    mTimerView.start();
    	}    	
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if (!mTileView.isSolved()) {
            mTime = (SystemClock.elapsedRealtime() - mTimerView.getBase());
        }
        mTimerView.stop();
    }
    
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // Prevent user from moving tiles if the puzzle has been solved 
        if (mTileView.isSolved()) {
            return false;
        }
        
    	if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN: {
                	mTileView.move(TileView.DIR_DOWN);
                    break;
                }
                case KeyEvent.KEYCODE_DPAD_UP: {
                	mTileView.move(TileView.DIR_UP);
                    break;
                }
                case KeyEvent.KEYCODE_DPAD_LEFT: {
                	mTileView.move(TileView.DIR_LEFT);
                    break;
                }
                case KeyEvent.KEYCODE_DPAD_RIGHT: {
                	mTileView.move(TileView.DIR_RIGHT);
                    break;
                }
                default:
                    return false;
            }

            if (mTileView.checkSolved()) {
            	mCompleteView.setImageBitmap(mTileView.getCurrentImage());
            	mCompleteView.setVisibility(View.VISIBLE);
            	
            	Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            	mCompleteView.startAnimation(animation);
            	
                postScore();
            }
            return true;
        }
    	
        return false;
    }

    private void postScore() {
        mTime = SystemClock.elapsedRealtime() - mTimerView.getBase();
        mTimerView.stop();
        mTimerView.setBase(SystemClock.elapsedRealtime() - mTime);
        mTimerView.invalidate(); // make sure the actual final time is shown
        
        boolean isHighScore = ScoreUtil.getInstance(this).updateScores(mTime, mTileView.mSize);
        if (isHighScore) {
            mToast = Toast.makeText(this, R.string.new_high_score, Toast.LENGTH_SHORT);
            mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
        }    
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(25);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        // Prevent user from moving tiles if the puzzle has been solved 
        if (mTileView.isSolved()) {
            return false;
        }

        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
            	mTileView.grabTile(event.getX(), event.getY());
            	return true;
            }
            case MotionEvent.ACTION_MOVE: {
            	mTileView.dragTile(event.getX(), event.getY());
            	return true;
            }
            case MotionEvent.ACTION_UP: {
            	mTileView.dropTile(event.getX(), event.getY());
                if (mTileView.checkSolved()) {
                	mCompleteView.setImageBitmap(mTileView.getCurrentImage());
                	mCompleteView.setVisibility(View.VISIBLE);

                	Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
                	mCompleteView.startAnimation(animation);

                	postScore();
                }
            	return true;
            }
        }
        
        return false;
    }  
   
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	menu.clear();
    	
    	menu.add(0, MENU_NEW, 0, R.string.menu_new).setIcon(
                R.drawable.menu_new_game);;
        menu.add(0, MENU_SCORES, 0, R.string.menu_scores).setIcon(
                R.drawable.menu_high_scores);
    	menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings).setIcon(
    	        R.drawable.ic_menu_preferences);
    	
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_NEW: {
            	mCompleteView.setVisibility(View.GONE);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                int blankLoc = Integer.parseInt(prefs.getString(PuzzlePreferenceActivity.BLANK_LOCATION, String.valueOf(1)));

                mTileView.newGame(null, blankLoc, mTimerView);
                
                //reset timer
                mTime = 0;
                mTimerView.stop();
                mTimerView.setBase(SystemClock.elapsedRealtime());
                mTimerView.start();
                
                break;
            }
            case MENU_SCORES: {
                if (!mTileView.isSolved()) {
                    mTime = (SystemClock.elapsedRealtime() - mTimerView.getBase());
                }
                mTimerView.stop();

                showHighScoreListDialog();
                break;
            }
            case MENU_SETTINGS: {
            	Intent intent = new Intent(this, PuzzlePreferenceActivity.class);
                this.startActivity(intent);
            	break;
            }
        }
        
        return true;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArray("tiles", mTileView.getTiles());
        outState.putInt("blank_first", mTileView.mBlankLocation);
        outState.putLong("time", mTime);
    }
        
    private void showHighScoreListDialog() {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.high_score_list, null);
        ListView listView = (ListView) layout.findViewById(R.id.score_list);
        ScoresListener listener = new ScoresListener();
        long[] times = ScoreUtil.getInstance(this).getAllScores();
        String[] sizes = getResources().getStringArray(R.array.pref_entries_size);
        int len = sizes.length;
        
        ArrayList<ScoreItem> scores = new ArrayList<ScoreItem>();
        for (int i = 0; i < len; i++) {
            scores.add(new ScoreItem(sizes[i], times[i]));
        }
        listView.setAdapter(new HighScoreListAdapter(this, scores));
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setIcon();
        builder.setTitle(R.string.scores_title);
        builder.setCancelable(true);
        builder.setView(layout);
        //builder.setAdapter(new HighScoreListAdapter(this, scores), null);
        builder.setPositiveButton(R.string.menu_clear, listener);
        builder.setNegativeButton(R.string.dialog_close, listener);
        builder.setOnCancelListener(new ScoresCancelListener());
        builder.show();        
    }
    
    private void showConfirmDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_dialog_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.dialog_yes, new ConfirmDeleteListener());
        builder.setNegativeButton(R.string.dialog_no, new ConfirmDeleteListener());
        builder.setMessage(R.string.delete_dialog_msg);

        builder.show();
    }
}