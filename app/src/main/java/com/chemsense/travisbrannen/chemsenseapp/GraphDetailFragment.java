package com.chemsense.travisbrannen.chemsenseapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.DefaultLabelFormatter;
import android.widget.Toast;
import android.widget.ImageButton;
import android.content.Intent;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * A fragment representing a single ChemSense detail screen.
 * This fragment is either contained in a {@link com.chemsense.travisbrannen.chemsenseapp.ChemSenseListActivity}
 * in two-pane mode (on tablets) or a {@link com.chemsense.travisbrannen.chemsenseapp.ChemSenseDetailActivity}
 * on handsets.
 */
public class GraphDetailFragment extends Fragment implements BluetoothLeUart.Callback{

    private final Handler mHandler = new Handler();
    private Runnable mTimer1;
    LineGraphSeries<DataPoint> mAlarmLine;
    PointsGraphSeries<DataPoint> mSelectedPoint;
    ChemSenseData mData;
    GraphView mGraph;
    GraphView mGraph2;
    int mSelectedPointCounter = 0;
    int mToastCounter = 25;
    View mRootView;
    public GraphDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mData = ((ChemSenseData) getActivity().getApplication());
        mSelectedPoint = new PointsGraphSeries<DataPoint>();
        mSelectedPoint.setColor(mData.getAvgSeries().getColor());
        mSelectedPoint.setShape(PointsGraphSeries.Shape.POINT);
        mSelectedPoint.setSize(12);
        mData.mAlarming = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_chemsense_detail_graph, container, false);
        double alarmPt = mData.getAlarmPoint();
        mAlarmLine = new LineGraphSeries<DataPoint>(new DataPoint[]{new DataPoint(0, alarmPt), new DataPoint(mData.getNumSamplesDisplayed(), alarmPt)});
        mAlarmLine.setColor(0xffdd0000);
        mAlarmLine.setTitle("Alarm Concentration");
        final ImageButton button = (ImageButton) mRootView.findViewById(R.id.email);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Copying Data to Email...", Toast.LENGTH_SHORT).show();
                Intent i = mData.sendDataAsEmail();
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        int textColor  = 0xffffffff;
        double alarmPoint = mData.getAlarmPoint();
        mGraph = (GraphView) mRootView.findViewById(R.id.graph);
        mGraph2 = (GraphView) mRootView.findViewById(R.id.graph2);
        mGraph.setTitle(mData.getChemicalName() +
                " Concentration over Time");
        //mGraph.setTitleColor(textColor);
        mGraph.setTitleTextSize(45);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        mGraph.getViewport().setMaxX(mData.getSecondsDisplayed());
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinY(0);
        mGraph.getViewport().setMaxY(1.25*alarmPoint);
        mGraph2.getViewport().setXAxisBoundsManual(true);
        mGraph2.getViewport().setMinX(mData.getStartTime());
        mGraph2.getViewport().setMaxX(mData.getEndTime());
        mGraph2.getViewport().setYAxisBoundsManual(true);
        mGraph2.getViewport().setMinY(0);
        mGraph2.getViewport().setMaxY(1.25*alarmPoint);
        mGraph.getLegendRenderer().setVisible(true);
        mGraph.getLegendRenderer().setBackgroundColor(0x22222222);
        //mGraph.getLegendRenderer().setTextColor(textColor);
        //mGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        //graph.getViewport().setScalable(true);
        //mGraph.getViewport().setScrollable(true);
        GridLabelRenderer r = mGraph.getGridLabelRenderer();
        r.setPadding(20);
        r.setVerticalAxisTitle("Concentration (% absorbed)");
        //r.setVerticalAxisTitleColor(textColor);
        //r.setVerticalLabelsColor(textColor);
        //r.setHorizontalLabelsVisible(false);
        r.setHorizontalAxisTitle(" \nTime");
        //r.setHorizontalAxisTitleColor(textColor);
        r.setHorizontalLabelsColor(mData.getSeries().getColor());
        //r.setGridColor(0xffffffff);
        r.setGridStyle(GridLabelRenderer.GridStyle.NONE);
        r.setNumHorizontalLabels(4);
        // custom label formatter to show currency "EUR"
        r.setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    return ":" + super.formatLabel(value, isValueX);
                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });
        mGraph2.getGridLabelRenderer().setNumHorizontalLabels(3);
        mGraph2.getGridLabelRenderer().setPadding(r.getPadding());
        mGraph2.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        mGraph2.getGridLabelRenderer().setVerticalLabelsColor(0x00000000);
        mGraph2.getGridLabelRenderer().setVerticalAxisTitle(" ");
        mGraph2.getGridLabelRenderer().setHorizontalLabelsColor(mData.getAvgSeries().getColor());
        mGraph2.getGridLabelRenderer().setHorizontalAxisTitleTextSize(6);
        mGraph2.getGridLabelRenderer().setHorizontalAxisTitle(" ");
        SimpleDateFormat df = new SimpleDateFormat("hh:mm");
        mGraph2.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity(), df));
        LineGraphSeries<DataPoint> dummySeries = new LineGraphSeries<DataPoint>(new DataPoint[]{});
        dummySeries.setTitle(mData.getAvgSeries().getTitle());
        dummySeries.setColor(mData.getAvgSeries().getColor());
        mGraph.addSeries(mAlarmLine);
        mGraph.addSeries(mData.getSeries());
        mGraph2.addSeries(mData.getAvgSeries());
        mGraph.addSeries(dummySeries); //just used to draw legend
        mGraph2.addSeries(mSelectedPoint);
        mData.getAvgSeries().setOnDataPointTapListener(mListener);
        return mRootView;
    }

    public void timedUpdate(){
        mData.connectionUpdateToasts(getActivity());
        if(!mData.hasNewPoint()){
            mData.FAKE_X_VALUE_ITER += 1.0*mData.FAKE_INTERRUPT_TIME/1000;
        }
        while(mData.hasNewPoint()){
            if (mData.getSeries().getHighestValueX()>=mData.getSecondsDisplayed()){
                mData.resetSeries();
                mSelectedPoint.resetData(new DataPoint[] {});
            }
            if (mData.getAvgSeries().getHighestValueX()>mData.getEndTime()){
                mData.resetAvgSeries();
                mGraph2.getViewport().setMinX(mData.getStartTime());
                mGraph2.getViewport().setMaxX(mData.getEndTime());
            }
            mData.getSeries().appendData(mData.updatePlot(), false, mData.getNumSamplesDisplayed() + 1);
            if (mSelectedPointCounter!=0){
                mSelectedPointCounter -= 1;
                if (mSelectedPointCounter==0){
                    mSelectedPoint.resetData(new DataPoint[] {});
                }
            }
            if (mData.getSeries().isEmpty()){return;}
            if (mData.getLatestY() > mData.alarmPoint && !mData.mAlarming){
                mData.alarmRaised(mData.getLatestY(), getActivity());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mData.resumeGraphing(this);
        mTimer1 = new Runnable() {
            @Override
            public void run() {
                timedUpdate();
                mHandler.postDelayed(this, mData.FAKE_INTERRUPT_TIME);
            }
        };
        mHandler.postDelayed(mTimer1, mData.FAKE_INTERRUPT_TIME);

    }
    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTimer1);
    }
    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    public void onStop() {
        super.onStop();
        mData.stopGraphing(this);
    }
    // UART Callback event handlers.
    @Override
    public void onConnected(BluetoothLeUart uart) {
        // Called when UART device is connected and ready to send/receive data.
    }

    @Override
    public void onConnectFailed(BluetoothLeUart uart) {
        // Called when some error occured which prevented UART connection from completing.
        Toast.makeText(getActivity(), "Failed to connect", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected(BluetoothLeUart uart) {
        // Called when the UART device disconnected.
        mHandler.removeCallbacks(mTimer1);
    }

    @Override
    public void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
        // Called when data is received by the UART.
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        // Called when a UART device is discovered (after calling startScan).
    }
    @Override
    public void onDeviceInfoAvailable() {
    }

    OnDataPointTapListener mListener = new OnDataPointTapListener() {
        @Override
        public void onTap(Series series, DataPointInterface dataPoint) {
            selectPoint(dataPoint);
        }
    };

    public void selectPoint(DataPointInterface dataPoint){
        Date date = new Date((long) dataPoint.getX());
        SimpleDateFormat format = new SimpleDateFormat("hh:mm");
        String dateToStr = format.format(date);
        String msg = "Time: " + dateToStr + "\n" + "Concentration: " + (int) dataPoint.getY() + "% absorbed";
        mSelectedPoint.resetData(new DataPoint[] {new DataPoint(dataPoint.getX(), dataPoint.getY())});
        mSelectedPointCounter = mToastCounter;
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }
}
