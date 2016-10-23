package com.chemsense.travisbrannen.chemsenseapp;
import java.lang.Thread;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
public class Splash extends Activity {

    /**
     * Duration of wait *
     */
    private final int SPLASH_DISPLAY_LENGTH = 1000;
    private Handler uiHandler;
    Timer timer;
    int seconds = 1;
    Context context;
    Intent listIntent;
    ChemSenseData mData;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mData = ((ChemSenseData) getApplication());
        mData.InitData(this);
        setContentView(R.layout.splash_screen);
        context = getApplicationContext();
        mData.connectionUpdateToasts(this);
        final AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Choose Data Source");
        builder.setMessage("Would you like to connect to ChemSense or use your accelerometer for simulated data?");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setPositiveButton(R.string.bluetooth, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mData.setDataSource(ChemSenseData.DataSource.BLUETOOTH);
                        new Reminder(seconds);
                    }
        });
        builder.setNegativeButton(R.string.accelerometer, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mData.setDataSource(ChemSenseData.DataSource.ACCELEROMETER);
                    new Reminder(seconds);
                }
        });
        builder.show();
    }

    public class Reminder {

        Timer timer;

        public Reminder(int seconds) {
            timer = new Timer();
            timer.schedule(new TimedTask(), seconds * 1000);
            listIntent = new Intent(context, ChemSenseListActivity.class);
        }

        public class TimedTask extends TimerTask {
            public void run() {
                timer.cancel(); //Not necessary because we call System.exit
                startActivity(listIntent);
            }
        }
    }
}

