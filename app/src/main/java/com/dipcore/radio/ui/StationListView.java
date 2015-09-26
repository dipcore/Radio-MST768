package com.dipcore.radio.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dipcore.radio.R;
import com.dipcore.radio.RadioActivity;
import com.dipcore.radio.RadioSharedPreferences;
import com.dipcore.radio.Station;
import com.dipcore.radio.Stations;
import com.dipcore.radio.Tools;

public class StationListView extends RelativeLayout {

    public interface NoticeListener {
        void onStationCellClicked(int index, Station station);
        void onStationCellLongClicked(int index, Station station);
    }

    Context mContext;
    LayoutInflater mInflater;
    GridViewPager mGridViewPager;
    GridPagerAdapter mGridPagerAdapter;

    int fontSize;

    public StationListView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public StationListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public void init(){

        // Inflater
        mInflater = LayoutInflater.from(mContext);

        // Grid view pager
        mGridViewPager = new GridViewPager(mContext);

        mGridViewPager.setRowNumber(2);
        mGridViewPager.setColumnNumber(4);

        mGridViewPager.setRowMargin(0);
        mGridViewPager.setColumnMargin(0);

        mGridViewPager.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mGridPagerAdapter = new GridPagerAdapter();
        mGridViewPager.setAdapter(mGridPagerAdapter);

        // Add grid view pager as view
        addView(mGridViewPager);

    }

    public void set(Stations stations){
        mGridPagerAdapter.set(stations);
        mGridViewPager.refresh();
        mGridViewPager.notifyDataSetChanged();
    }

    public void add(Station station){
        mGridPagerAdapter.add(station);
        mGridViewPager.notifyDataSetChanged();
        mGridViewPager.setCurrentItem(mGridPagerAdapter.getCount() / mGridViewPager.getPageSize(), true);

    }

    public void clear(){
        mGridPagerAdapter.clear();
        mGridViewPager.notifyDataSetChanged();
    }

    public void setSelection(int index){
        mGridViewPager.setSelection(index);
    }

    public void setGridSize(final int rowNumber, int colNumber) {

        ShapeDrawable background = new ShapeDrawable();
        background.getPaint().setColor(getResources().getColor(R.color.station_list_bar_bg_color));


        ShapeDrawable line = new ShapeDrawable(new Shape() {
            @Override
            public void draw(Canvas canvas, Paint paint) {
                for (int i = 0; i <= getHeight(); i += getHeight() / rowNumber){
                    if (i != 0 && 1 != getHeight())
                        canvas.drawLine(0, i , getWidth(), i, paint);
                }
            }
        });
        line.getPaint().setColor(getResources().getColor(R.color.station_list_bar_divider));

        Drawable[] layers = {background, line};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        layerDrawable.setLayerInset(0, 0, 0, 0, 0);
        layerDrawable.setLayerInset(1, 0, 0, 0, 0);

        setBackground(layerDrawable);

        mGridViewPager.setRowNumber(rowNumber);
        mGridViewPager.setColumnNumber(colNumber);
        mGridViewPager.refresh();
        mGridViewPager.setSelection(mGridViewPager.getSelection());
    }

    public void setFontSize(int fontSize){
        this.fontSize = fontSize;
        mGridViewPager.refresh();
    }

    public class GridPagerAdapter extends BaseAdapter {

        Stations mStations = new Stations();

        public GridPagerAdapter(){

        }

        @Override
        public int getCount() {
            return mStations.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            RecordHolder holder = null;

            if(convertView == null) {
                convertView = mInflater.inflate(R.layout.station_list_view_item, null);
                holder = new RecordHolder();

                Station item = mStations.get(position);

                // Set view values
                TextView indexView = (TextView) convertView.findViewById(R.id.item_id);
                TextView nameView = (TextView) convertView.findViewById(R.id.item_name);
                TextView freqView = (TextView) convertView.findViewById(R.id.item_freq);
                TextView unitsView = (TextView) convertView.findViewById(R.id.item_units);
                ImageView favoriteView = (ImageView) convertView.findViewById(R.id.item_favorite);

                nameView.setTextSize(fontSize);

                holder.index = position;
                holder.station = item;

                indexView.setText(String.valueOf(holder.index + 1));

                String units = Tools.unitsByRangeId(item.freqRangeId);
                String freqString = Tools.formatFrequencyValue(item.freq, units);

                if (item.name == null || item.name == "") {
                    nameView.setText(freqString);
                    freqView.setVisibility(View.GONE);
                    unitsView.setVisibility(View.VISIBLE);
                    unitsView.setText(units);
                } else {
                    nameView.setText(item.name);
                    freqView.setVisibility(View.VISIBLE);
                    freqView.setText(freqString + " " + units);
                    unitsView.setVisibility(View.GONE);
                }

                if (item.favorite)
                    favoriteView.setVisibility(View.VISIBLE);

                convertView.setTag(holder);
            } else {
                holder = (RecordHolder) convertView.getTag();
            }

            convertView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (view.getTag() != null) {
                        RecordHolder holder = (RecordHolder) view.getTag();
                        StationListView.this.setSelection(holder.index);
                        ((RadioActivity) mContext).onStationCellClicked(holder.index, holder.station); // Weak ref
                    }
                }
            });

            convertView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(view.getTag() != null) {
                        RecordHolder holder = (RecordHolder) view.getTag();
                        ((RadioActivity) mContext).onStationCellLongClicked(holder.index, holder.station); // Weak ref
                    }
                    return true;
                }
            });

            return convertView;
        }

        private class RecordHolder {
            int index;
            Station station;
        }

        public void set(Stations stations){
            mStations = stations;
        }

        public void add(Station station){
            mStations.add(station);
        }

        public void clear(){
            mStations = new Stations();
        }
    }

}
