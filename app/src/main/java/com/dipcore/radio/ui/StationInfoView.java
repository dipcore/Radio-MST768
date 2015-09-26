package com.dipcore.radio.ui;


import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dipcore.radio.R;
import com.dipcore.radio.Station;
import com.dipcore.radio.Tools;

import java.util.ArrayList;
import java.util.List;

public class StationInfoView extends LinearLayout {

    public interface NoticeListener {
        void onStationInfoViewLongClick(Station station);
    }

    private List<NoticeListener> listeners = new ArrayList<>();

    private Station mStation;

    private TextView mainText;
    private TextView freqValue;
    private TextView unitsValue;
    private TextView tp;
    private TextView ta;
    private TextView st;
    private TextView ptyName;
    private TextView nameValue;
    private ImageView ptyIcon;
    private ImageView nameIcon;
    private LinearLayout mainTextLayout;

    private Boolean rdsEnabled = true;

    public StationInfoView(Context context) {
        super(context);
        init();
    }

    public StationInfoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    /**
     * Add listener
     * @param listener
     */
    public void addListener(NoticeListener listener) {
        listeners.add(listener);
    }

    /**
     * Init view
     */
    public void init() {

        inflate(getContext(), R.layout.station_info_view, this);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                for (NoticeListener listener : listeners) {
                    listener.onStationInfoViewLongClick(mStation);
                }
                return false;
            }
        });


        mainText = (TextView) findViewById(R.id.mainText);
        freqValue = (TextView) findViewById(R.id.freqValue);
        unitsValue = (TextView) findViewById(R.id.unitsValue);
        st = (TextView) findViewById(R.id.st);
        ta = (TextView) findViewById(R.id.ta);
        tp = (TextView) findViewById(R.id.tp);
        ptyName = (TextView) findViewById(R.id.ptyName);
        nameValue = (TextView) findViewById(R.id.nameValue);
        ptyIcon = (ImageView) findViewById(R.id.ptyIcon);
        nameIcon = (ImageView) findViewById(R.id.nameIcon);
        mainTextLayout = (LinearLayout) findViewById(R.id.mainTextLayout);

        initTextAutoScale();
    }

    /**
     * Show station info
     * @param station
     */
    public void showStation(Station station) {

        mStation = station;

        clear();

        String units = Tools.unitsByRangeId(station.freqRangeId);
        String freq = Tools.formatFrequencyValue(station.freq, units);

        freqValue.setText(freq);
        unitsValue.setText(units);

        if (station.name == null || station.name.equals("")) {
            mainText.setText(freq);
            nameValue.setVisibility(GONE);
            nameIcon.setVisibility(GONE);
        } else {
            mainText.setText(station.name);
            nameValue.setText(station.name);
            nameIcon.setVisibility(VISIBLE);
            nameValue.setVisibility(VISIBLE);
        }

    }

    /**
     * Show station frequency and unit
     * @param aFreq
     * @param rangeId
     */
    public void showStationFrequency(int aFreq, int rangeId){

        clear();

        String units = Tools.unitsByRangeId(rangeId);
        String freq = Tools.formatFrequencyValue(aFreq, units);
        freqValue.setText(freq);
        unitsValue.setText(units);

        mainText.setText(freq);
    }

    /**
     * Show station name
     * @param aName
     */
    private void showStationName(String aName){
        String name = aName;
    }

    /**
     * Set PTY text
     * @param value
     */
    public void setRDSPTYText(String value){
        ptyName.setText(value);
        if (value != null && value != "") {
            ptyName.setVisibility(View.VISIBLE);
            ptyIcon.setVisibility(View.VISIBLE);
        } else {
            ptyName.setVisibility(View.GONE);
            ptyIcon.setVisibility(View.GONE);
        }
    }

    /**
     * Set short RDS text
     * @param value
     */
    public void setRDSPSText(String value){
        if (!Tools.isEmptyString(value)) {
            mainText.setText(value);
        }
    }

    /**
     * Set ST flag
     * @param flag
     */
    public void setRDSSTFlag(boolean flag){
        int color = getResources().getColor(flag ? R.color.text_additional_color_2 : R.color.text_additional_color_4);
        st.setTextColor(color);
    }

    /**
     * Set TP flag
     * @param flag
     */
    public void setRDSTPFlag(boolean flag){
        int color = getResources().getColor(flag ? R.color.text_additional_color_2 : R.color.text_additional_color_4);
        tp.setTextColor(color);
    }

    /**
     * Set TA flag
     * @param flag
     */
    public void setRDSTAFlag(boolean flag){
        int color = getResources().getColor(flag ? R.color.text_additional_color_2 : R.color.text_additional_color_4);
        ta.setTextColor(color);
    }

    /**
     * Enable - disable RDS
     * @param flag
     */
    public void setRDSState(Boolean flag){
        rdsEnabled = flag;
    }

    private void clear(){
        st.setTextColor(getResources().getColor(R.color.text_additional_color_4));
        tp.setTextColor(getResources().getColor( R.color.text_additional_color_4));
        ta.setTextColor(getResources().getColor( R.color.text_additional_color_4));
        mainText.setText("");
        ptyName.setVisibility(View.GONE);
        ptyIcon.setVisibility(View.GONE);
    }


    float scaleFactor = -1;
    private void initTextAutoScale(){

        mainTextLayout.addOnLayoutChangeListener(new OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (scaleFactor < 0) {
                    float textSize = mainText.getTextSize();
                    float height = getHeight();
                    scaleFactor = textSize / height;

                    System.out.println("textSize " + textSize);
                    System.out.println("height " + height);
                    System.out.println("scaleFactor " + scaleFactor);
                } else {
                    float currentHeight = getHeight();
                    mainText.setTextSize(currentHeight * scaleFactor);
                    System.out.println("textSize " + mainText.getTextSize());
                    System.out.println("currentHeight " + currentHeight);
                }
            }
        });

    }

}
