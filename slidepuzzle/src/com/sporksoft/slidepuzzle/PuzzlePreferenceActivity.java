package com.sporksoft.slidepuzzle;

import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

public class PuzzlePreferenceActivity extends PreferenceActivity {
	final static int REQUEST_CODE_LOAD_IMAGE = 1;
	
	// Symbolic names for the keys used for preference lookup
    public static final String BLANK_LOCATION = "pref_key_blank_loc";
    public static final String PUZZLE_SIZE = "pref_key_size";
    public static final String RANDOM_PUZZLE_IMAGE = "pref_key_random_image";
    public static final String CUSTOM_PUZZLE_IMAGE = "pref_key_image";
    public static final String SHOW_IMAGE = "pref_key_show_image";
    public static final String IMAGE_SOURCE = "pref_key_image_source";
    public static final String USE_CUSTOM_IMAGE = "pref_key_custom_image";
    public static final String SHOW_NUMBERS = "pref_key_show_numbers";
    public static final String SHOW_BORDERS = "pref_key_show_borders";
    public static final String SHOW_TIMER = "pref_key_show_timer";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle); 
        addPreferencesFromResource(R.xml.preferences);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode) {
            case REQUEST_CODE_LOAD_IMAGE:
                if (data != null) {
                    ((SelectImagePreference) findPreference(IMAGE_SOURCE)).setCustomLocation(data.getData());
                }
                break;
        }
    }    
}
