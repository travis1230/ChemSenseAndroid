package com.chemsense.travisbrannen.chemsenseapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chemsense.travisbrannen.chemsenseapp.displays.DisplayModes;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * A fragment representing a single ChemSense detail screen.
 * This fragment is either contained in a {@link com.chemsense.travisbrannen.chemsenseapp.ChemSenseListActivity}
 * in two-pane mode (on tablets) or a {@link com.chemsense.travisbrannen.chemsenseapp.ChemSenseDetailActivity}
 * on handsets.
 */
public class AboutDetailFragment extends Fragment {
    public AboutDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chemsense_detail, container, false);
            ((TextView) rootView.findViewById(R.id.ChemSense_detail)).setText("hoi");
            return rootView;
    }
}
