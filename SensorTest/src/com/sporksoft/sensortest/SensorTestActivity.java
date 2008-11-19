package com.sporksoft.sensortest;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;

public class SensorTestActivity extends Activity {
    SensorManager mSensorManager;
    SensorView mSensorView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorView = (SensorView) findViewById(R.id.sensor_view);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorView,
                SensorManager.SENSOR_ACCELEROMETER |
                SensorManager.SENSOR_MAGNETIC_FIELD |
                SensorManager.SENSOR_ORIENTATION,
                SensorManager.SENSOR_DELAY_GAME);
    }
   
    @Override
    protected void onStop() {
        mSensorManager.unregisterListener(mSensorView);
        super.onStop();
    }

}