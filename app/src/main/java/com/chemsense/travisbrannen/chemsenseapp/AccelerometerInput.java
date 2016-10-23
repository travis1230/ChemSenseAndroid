package com.chemsense.travisbrannen.chemsenseapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.lang.Math;
import java.util.ArrayList;

/**
 * Created by travisbrannen on 4/18/15.
 */
public class AccelerometerInput implements SensorEventListener{

    public ArrayList mNewPoints;
    int numAccelEvents = 20;
    float mX=0; float mY=0; float mZ=0;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    android.app.Activity mActivity;

    public AccelerometerInput(android.app.Activity activity){
        mActivity = activity;
        mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mNewPoints = new ArrayList();
    }
    public void registerListener(){
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterListener(){
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        mX = Math.abs((mX + event.values[0])/numAccelEvents);
        mY = Math.abs((mY + event.values[1])/numAccelEvents);
        mZ = Math.abs((mY + event.values[2])/numAccelEvents);
        mNewPoints.add((float)(mX)*2.5);
    }

    public float newPoint(){
        Double d = new Double((double) mNewPoints.remove(0));
        return d.floatValue();
    }
    public boolean hasNewPoint(){
        return mNewPoints.size()!=0;
    }
}
