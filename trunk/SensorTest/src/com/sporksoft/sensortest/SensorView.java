package com.sporksoft.sensortest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.SensorManager;
import android.hardware.SensorListener;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class SensorView extends View implements SensorListener {
    Paint mPaint;
    RectF mRectF;
    float[] mOrientationValues;
    
    //natural range of positions user holds the device in while looking at the screen
    float[] mNeutralOrientationMin;
    float[] mNeutralOrientationMax;
    
    int mScaleSpeed = 5;
    
    public SensorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SensorView(Context context, int size) {
        super(context);
        init();
    }
    
    private void init() {
        mPaint = new Paint();
        mRectF = new RectF();
        mOrientationValues = new float[3];
        
        mNeutralOrientationMin = new float[3];
        mNeutralOrientationMin[SensorManager.DATA_Z] = -30;
        mNeutralOrientationMin[SensorManager.DATA_Y] = -10;

        mNeutralOrientationMax = new float[3];
        mNeutralOrientationMax[SensorManager.DATA_Z] = 2;
        mNeutralOrientationMax[SensorManager.DATA_Y] = 10;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        float x = getLeft();
        float y = getTop();
        float w = getWidth();
        float h = getHeight();
        float cx = x + w / 2;
        float cy = y + h / 2;
        
        canvas.drawRGB(0xff, 0xff, 0xff);
        
        
        //draw box
        mPaint.setColor(0xffff0000);
        canvas.drawRect(mRectF, mPaint);
        
        //draw cross 
        mPaint.setColor(0xff000000);
        canvas.drawLine(cx, 0, cx, h, mPaint);
        canvas.drawLine(0, cy, w, cy, mPaint);

        canvas.drawText("(" + mOrientationValues[0] + ", " + mOrientationValues[1] +", " + mOrientationValues[2] + ")",
                x, y + 10, mPaint);

    }
    
    @Override 
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int x = getLeft();
        int y = getTop();
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        int cx = x + w / 2;
        int cy = y + h / 2;
        
        mRectF.set(cx - 10, cy - 10, cx + 10, cy + 10);
    }
    
    public void onAccuracyChanged(int sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    public void onSensorChanged(int sensor, float[] values) {
        //Log.v(SensorView.class.getName(), "sensor: " + sensor + ", x: " + values[0] + ", y: " + values[1] + ", z: " + values[2]);
        
        switch(sensor) {
            case SensorManager.SENSOR_ORIENTATION: {
                for (int i=0 ; i<3 ; i++) {
                    mOrientationValues[i] = values[i];
                    float z =  mOrientationValues[SensorManager.DATA_Z];
                    float y = -mOrientationValues[SensorManager.DATA_Y];
                    float zMin = mNeutralOrientationMin[SensorManager.DATA_Z];
                    float zMax = mNeutralOrientationMax[SensorManager.DATA_Z];
                    float yMin = mNeutralOrientationMin[SensorManager.DATA_Y];
                    float yMax = mNeutralOrientationMax[SensorManager.DATA_Y];
                    
                    
                    if (z > zMax) {
                        //move right
                        mRectF.left += (z - zMax) / mScaleSpeed;
                        mRectF.right += (z - zMax) / mScaleSpeed;
                    } else if (z < zMin) {
                        //move left
                        mRectF.left += (z - zMin) / mScaleSpeed;                        
                        mRectF.right += (z - zMin) / mScaleSpeed;                        
                    }
                    
                    if (y > yMax) {
                        //move down
                        mRectF.top += (y - yMax) / mScaleSpeed;
                        mRectF.bottom += (y - yMax) / mScaleSpeed;
                    } else if(y < yMin) {
                        //move up
                        mRectF.top += (y - yMin) / mScaleSpeed;                        
                        mRectF.bottom += (y - yMin) / mScaleSpeed;                                                
                    }
                }

                invalidate();
                break;
            }
        }
    }
}
