package com.chemsense.travisbrannen.chemsenseapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chemsense.travisbrannen.chemsenseapp.displays.DisplayModes;
import com.chemsense.travisbrannen.chemsenseapp.ChemSenseData;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * A fragment representing a single ChemSense detail screen.
 * This fragment is either contained in a {@link ChemSenseListActivity}
 * in two-pane mode (on tablets) or a {@link ChemSenseDetailActivity}
 * on handsets.
 */
public class ChemSenseDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private DisplayModes.DisplayItem mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChemSenseDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = DisplayModes.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mItem != null) {

            View rootView = inflater.inflate(R.layout.fragment_chemsense_detail, container, false);
            ((TextView) rootView.findViewById(R.id.ChemSense_detail)).setText(mItem.content);
            return rootView;

        }
        return null;
    }
}
