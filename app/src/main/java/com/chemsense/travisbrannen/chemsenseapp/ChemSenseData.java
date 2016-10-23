package com.chemsense.travisbrannen.chemsenseapp;

import android.app.AlertDialog;
import android.app.Application;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.content.Context;
import java.util.Date;
import java.util.Calendar;
/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ChemSenseData extends Application{

    LineGraphSeries<DataPoint> mSeries;
    Double FAKE_X_VALUE_ITER = 0.0;
    Double mYValue = 0.0;
    List<Double> mAllX = new ArrayList<Double>();
    List<Double> mAllY = new ArrayList<Double>();
    int FAKE_INTERRUPT_TIME = 100;  // IN MS
    int mNumSamplesDisplayed = 60*1000/FAKE_INTERRUPT_TIME;
    int mNumSamplesAveraged = 60*1000/FAKE_INTERRUPT_TIME;
    double alarmPoint = 1;
    String chemicalName = "Chemical";
    LineGraphSeries<DataPoint> mAverageSeries;
    DataPoint mLastPoint;
    boolean alarmPtSet = false;
    boolean audibleAlarm = true;
    boolean mAlarming = false;
    Ringtone mR;
    int mDutyPercent = 1;
    int mFreqKhz = 25;
    private BluetoothLeUart mUart;
    private int mAttenuationCoefficient = 1000;
    Date mStartTime;
    Date mEndTime;
    Date mLittleStart;
    public enum DataSource{
        BLUETOOTH, ACCELEROMETER
    }
    DataSource mDataSource = DataSource.BLUETOOTH;
    AccelerometerInput mInput;
    
    public void InitData(android.app.Activity activity){
        mSeries = new LineGraphSeries<DataPoint>();
        mSeries.setDrawBackground(true);
        mSeries.setBackgroundColor(0x55000077);
        mSeries.setTitle("Avg per " + round(FAKE_INTERRUPT_TIME/1000d, 1) + " sec for last min");
        mAverageSeries = new LineGraphSeries<DataPoint>();
        mAverageSeries.setColor(0xff0c9a65);
        mAverageSeries.setBackgroundColor(0x00000000);
        mAverageSeries.setDrawBackground(true);
        mAverageSeries.setTitle("Avg per min for last hr");
        mAverageSeries.setThickness(12);
        mUart = new BluetoothLeUart(this);
        mInput = new AccelerometerInput(activity);
        mInput.registerListener();
        Calendar calendar = Calendar.getInstance();
        mStartTime = calendar.getTime();
        calendar.roll(Calendar.HOUR_OF_DAY, true);
        mEndTime = calendar.getTime();
        mLittleStart = mStartTime;
    }
    private int mRingCounter;
    public long getStartTime(){
        return mStartTime.getTime();
    }
    public long getEndTime(){
        return mEndTime.getTime();
    }
    public LineGraphSeries<DataPoint> getSeries(){
        return mSeries;
    }
    public String getChemicalName(){
        return chemicalName;
    }
    public void setChemicalName(String s){
        chemicalName=s;
    }

    public int getNumSamplesDisplayed() {
        return mNumSamplesDisplayed;
    }

    public int getSecondsDisplayed() {
        return mNumSamplesDisplayed*FAKE_INTERRUPT_TIME/1000;
    }

    public LineGraphSeries<DataPoint> getAvgSeries(){
        return mAverageSeries;
    }

    public int getNumSamplesAveraged(){
        return mNumSamplesAveraged;
    }

    public LineGraphSeries<DataPoint> resetSeries(){
        double sumY = 0;
        int counter = 0;
        for (Iterator<DataPoint> dataPoint=mSeries.getValues(0, mNumSamplesAveraged); dataPoint.hasNext();){
            sumY += dataPoint.next().getY();
            counter += 1;
        }
        mAverageSeries.appendData(new DataPoint(mLittleStart, sumY/counter), false, 61);
        mSeries.resetData(new DataPoint[]{new DataPoint(0, mLastPoint.getY())});
        Calendar calendar = Calendar.getInstance();
        mLittleStart = calendar.getTime();
        FAKE_X_VALUE_ITER = 1.0*FAKE_INTERRUPT_TIME/1000;
        return mSeries;
    }

    public LineGraphSeries<DataPoint> resetAvgSeries(){
        mAverageSeries.resetData(new DataPoint[]{});
        Calendar calendar = Calendar.getInstance();
        mStartTime = calendar.getTime();
        calendar.roll(Calendar.HOUR_OF_DAY, 1);
        mEndTime = calendar.getTime();
        return mAverageSeries;
    }

    public double getAlarmPoint(){
        alarmPtSet = true;
        return alarmPoint;
    }
    public void setAlarmPoint(double alarmPt){
        if (mDataSource==DataSource.BLUETOOTH){
            if (alarmPoint != alarmPt){
                sendToUart("At"+(short)alarmPt);
            }
        }
        alarmPoint = alarmPt;
    }
    public void setDutyCycle(int dutyPercent){
        if (mDataSource==DataSource.BLUETOOTH){
            if (mDutyPercent!=dutyPercent){
                mDutyPercent = dutyPercent;
                sendToUart("Pd" + (short)mDutyPercent);
            }
        }
    }
    public int getDutyPercent(){
        return mDutyPercent;
    }
    public void setFreqKhz(int freqKhz){
        if (mDataSource==DataSource.BLUETOOTH){
            if (mFreqKhz!=freqKhz){
                mFreqKhz=freqKhz;
                sendToUart("Pf" + (short)mFreqKhz);
            }
        }
    }
    public int getAttenuationCoefficient(){
        return mAttenuationCoefficient;
    }
    public void setAttenuationCoefficient(int attenuationCoefficient){
        if (mDataSource==DataSource.BLUETOOTH){
            if (mAttenuationCoefficient!=attenuationCoefficient){
                mAttenuationCoefficient=attenuationCoefficient;
                sendToUart("Ac" + (short)mAttenuationCoefficient);
            }
        }
    }
    public int getFreqKhz(){
        return mFreqKhz;
    }
    public DataPoint updatePlot(){
        /**
         * returns com.jjoe64.graphview.mSeries.DataPoint object, initialized DataPoint(x,y)
         */

        mYValue = (double) newPoint();
        mAllX.add(FAKE_X_VALUE_ITER);
        mAllY.add(mYValue);
        if (mAllX.size()>10000){
            mAllX.clear();
            mAllY.clear();
        }
        DataPoint dp = new DataPoint(FAKE_X_VALUE_ITER, mYValue);
        mLastPoint = dp;
        FAKE_X_VALUE_ITER += 1d*FAKE_INTERRUPT_TIME/1000;
        return dp;
    }

    public Intent sendDataAsEmail(){
        String data = "";
        data = data.concat("Num Packets:" + mUart.mGottenPackets + "  Missed Packets:" + mUart.mMissedPackets + "\n");
        for (int i=0; i < mAllX.size(); i++){
            data = data.concat(Double.toString(mAllX.get(i))).concat(" ");
            data = data.concat(Double.toString(mAllY.get(i))).concat("\n");
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_TEXT, data);
        intent.putExtra(Intent.EXTRA_SUBJECT, "ChemSense Data Stream");
        return intent;
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
    public void alarmRaised(double y, Context activity){
        mAlarming = true;
        if (audibleAlarm) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                mR = RingtoneManager.getRingtone(activity, notification);
                mR.play();
            } catch (java.lang.NullPointerException e) {
                e.printStackTrace();
            }
        }
        final AlertDialog.Builder builder=new AlertDialog.Builder(activity);
        builder.setTitle("High Chemical Concentration Alert");
        builder.setMessage(getChemicalName() + " concentration: " + ChemSenseData.round(y, 2) + " mols/m³");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mAlarming = false;
                if (audibleAlarm){
                    mR.stop();
                }
            }
        });
        builder.show();
    }
    public BluetoothLeUart getUart(){
        return mUart;
    }

    public boolean hasNewPoint(){
        if (mDataSource==DataSource.BLUETOOTH){
            return mUart.hasNewPoint();
        }
        else if (mDataSource==DataSource.ACCELEROMETER){
            return mInput.hasNewPoint();
        }
        return false;
    }

    private float newPoint(){
        if (mDataSource==DataSource.BLUETOOTH){
            return mUart.newPoint()%1000;
        }
        else if (mDataSource==DataSource.ACCELEROMETER){
            return mInput.newPoint();
        }
        return 0;
    }

    public void resumeGraphing(GraphDetailFragment frag){
        if (mDataSource==DataSource.BLUETOOTH){
            mUart.registerCallback(frag);
            mUart.connectIfNeeded(frag.getActivity());
        }
    }

    public void stopGraphing(GraphDetailFragment frag){
        if (mDataSource==DataSource.BLUETOOTH){
            mUart.unregisterCallback(frag);
            //mData.getUart().disconnect();
        }
        if (mDataSource==DataSource.ACCELEROMETER){
            //mInput.unregisterListener();
        }
    }

    public void connectionUpdateToasts(Context activity){
        if (mDataSource==DataSource.BLUETOOTH){
            if (mUart.justConnected()){
                mUart.showConnectedToast(activity);
            }
            if (mUart.justDisconnected()){
                mUart.showDisconnectedToast(activity);
            }
            if (mUart.justFoundDevice()){
                mUart.showFoundDeviceToast(activity);
            }
            if (mUart.justGotDeviceInfo()){
                mUart.showGotDeviceInfoToast(activity);
            }
            if (mUart.justFailedConnection()){
                mUart.showConnectionFailedToast(activity);
            }
        }

    }
    public void setDataSource(DataSource d){
        mDataSource = d;
    }

    private void sendToUart(String message){
        StringBuilder stringBuilder = new StringBuilder();
        // We can only send 20 bytes per packet, so break longer messages
        // up into 20 byte payloads
        int len = message.length();
        int pos = 0;
        while(len != 0) {
            stringBuilder.setLength(0);
            if (len>=20) {
                stringBuilder.append(message.toCharArray(), pos, 20 );
                len-=20;
                pos+=20;
            }
            else {
                stringBuilder.append(message.toCharArray(), pos, len);
                len = 0;
            }
            mUart.send(stringBuilder.toString());
        }
    }

    public double getLatestY(){
        return mYValue;
    }
}
