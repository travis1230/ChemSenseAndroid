package com.chemsense.travisbrannen.chemsenseapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
/**
 * A fragment representing a single ChemSense detail screen.
 * This fragment is either contained in a {@link com.chemsense.travisbrannen.chemsenseapp.ChemSenseListActivity}
 * in two-pane mode (on tablets) or a {@link com.chemsense.travisbrannen.chemsenseapp.ChemSenseDetailActivity}
 * on handsets.
 */
public class OptionsDetailFragment extends Fragment {

    EditText mEditText;
    ChemSenseData mData;
    Spinner mDutySpinner;
    Spinner mFreqSpinner;
    EditText mAttenuationText;
    EditText mNameText;
    public OptionsDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mData = ((ChemSenseData) getActivity().getApplication());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.optionsmenu, container, false);
        mNameText = (EditText) rootView.findViewById(R.id.title);
        mNameText.setText(mData.getChemicalName());
        mEditText = (EditText) rootView.findViewById(R.id.alarmThreshold);
        mEditText.setText(Double.toString(mData.getAlarmPoint()));
        mAttenuationText = (EditText) rootView.findViewById(R.id.attenuationCoefficient);
        mAttenuationText.setText(Integer.toString(mData.getAttenuationCoefficient()));
        mDutySpinner = (Spinner) rootView.findViewById(R.id.dutySpinner);
        Integer[] oneThroughOneHundred = new Integer[100];
        for (int i=1; i<101; i++){
            oneThroughOneHundred[i-1]=i;
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_spinner_item, oneThroughOneHundred);
        mDutySpinner.setAdapter(adapter);
        mDutySpinner.setSelection(mData.getDutyPercent()-1);//1% is in position 0, etc.
        mFreqSpinner = (Spinner) rootView.findViewById(R.id.freqSpinner);
        Integer[] freqRange = new Integer[31];
        int minFreq = 20;
        for (int i=0; i<=30; i++){

            freqRange[i]=minFreq+i;
        }
        ArrayAdapter<Integer> adapter2 = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_spinner_item, freqRange);
        mFreqSpinner.setAdapter(adapter2);
        mFreqSpinner.setSelection(mData.getFreqKhz()-minFreq);
        return rootView;
    }
    @Override
    public void onPause() {
        mData.setAlarmPoint(Double.parseDouble(mEditText.getText().toString()));
        mData.setDutyCycle((int)mDutySpinner.getSelectedItem());
        mData.setFreqKhz((int)mFreqSpinner.getSelectedItem());
        mData.setAttenuationCoefficient((int)Float.parseFloat(mAttenuationText.getText().toString()));
        mData.setChemicalName(mNameText.getText().toString());
        super.onPause();
    }
}
