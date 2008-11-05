package com.sporksoft.slidepuzzle;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnKeyListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Chronometer;
import android.widget.Toast;

public class SlidePuzzleActivity extends Activity implements OnKeyListener {	
	private static final int MENU_NEW = 0;
	private static final int MENU_SCORES = 1;
	private static final int MENU_SETTINGS = 2;
	
	private TileView mTileView;
	private Chronometer mTimerView;
    private long mTime;
    private Toast mToast;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.slide_puzzle);
        mTileView = (TileView) findViewById(R.id.tile_view);
        mTileView.requestFocus();
        mTileView.setOnKeyListener(this);
        
        mTimerView = (Chronometer) findViewById(R.id.timer_view);

        if (icicle == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            mTileView.newGame(null, prefs.getBoolean(PuzzlePreferenceActivity.BLANK_LOCATION, false), mTimerView);
            mTime = 0;
        } else {
            mTileView.newGame((Tile[]) icicle.getParcelableArray("tiles"), icicle.getBoolean("blank_first"), mTimerView);
            mTime = icicle.getLong("time", 0);
        }
    }
    
    @Override
    public void onResume() {
    	super.onResume();

    	mTileView.updateInstantPrefs();
        mTimerView.setBase(SystemClock.elapsedRealtime() - mTime);
    	if (!mTileView.mSolved) {
    	    mTimerView.start();
    	}
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if (!mTileView.mSolved) {
            mTime = (SystemClock.elapsedRealtime() - mTimerView.getBase());
        }
        mTimerView.stop();
    }
    
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // Prevent user from moving tiles if the puzzle has been solved 
        if (mTileView.mSolved) {
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
                postScore();
            }
            return true;
        }
    	
        return false;
    }

    private void postScore() {
        long time = SystemClock.elapsedRealtime() - mTimerView.getBase();
        mTimerView.stop();
        
        boolean isHighScore = ScoreUtil.getInstance(this).updateScores(time, mTileView.mSize);
        if (isHighScore) {
            mToast = Toast.makeText(this, R.string.new_high_score, Toast.LENGTH_SHORT);
            mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
        }    
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(25);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        // Prevent user from moving tiles if the puzzle has been solved 
        if (mTileView.mSolved) {
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
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                mTileView.newGame(null, prefs.getBoolean(PuzzlePreferenceActivity.BLANK_LOCATION, false), mTimerView);
                
                //reset timer
                mTime = 0;
                mTimerView.stop();
                mTimerView.setBase(SystemClock.elapsedRealtime());
                mTimerView.start();
                break;
            }
            case MENU_SCORES: {
                Intent intent = new Intent(this, HighScoreListActivity.class);
                this.startActivity(intent);                
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
        outState.putBoolean("blank_first", mTileView.mBlankFirst);
        outState.putLong("time", mTime);
    }
}