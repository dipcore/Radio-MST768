package com.dipcore.radio.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.dipcore.radio.Tools;
import com.dipcore.radio.tw.FreqRange;

public class FreqBarView extends View {

    public interface NoticeListener {
        void onFreqBarStartTrackingTouch(int freq, FreqRange freqRange);
        void onFreqBarStopTrackingTouch(int freq, FreqRange freqRange);
    }

    private FreqRange freqRange = null; // Frequency range
    private int freq = 0; // Current frequency
    private int x = 0; // X coordinate of slider

    int viewFreqMin = 0;
    int viewFreqMax = 0;

    private Bitmap mSliderBitmap = null;
    private Bitmap mFreqLineBitmap = null;

    private NoticeListener mListener;
    private boolean loaded = false;

    int VIEW_FREQ_MARGIN = 70;
    int FONT_SIZE = 12;
    int FONT_COLOR = Color.WHITE;
    int LINES_COLOR = Color.WHITE;


    public FreqBarView(Context context) {
        super(context);
    }

    public FreqBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setFreqRange(FreqRange aFreqRange){

        if (getVisibility() == GONE || getHeight() == 0 || aFreqRange == null)
            return;

        freqRange = aFreqRange;

        viewFreqMin = freqRange.minFreq - VIEW_FREQ_MARGIN; // Visible range is bigger than real frequency range
        viewFreqMax = freqRange.maxFreq + VIEW_FREQ_MARGIN;

        if (loaded) {
            initFreqLineBitmap();
        }

        invalidate();
    }

    public void initSliderBitmap(){
        // Slider
        mSliderBitmap = Bitmap.createBitmap(1, getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mSliderBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawLine(0, 0, 0, getHeight(), paint);
    }

    public void initFreqLineBitmap(){

        if (freqRange != null) {

            int height = getHeight();
            int width = getWidth();

            int virtualFreqMin = (viewFreqMin / 100 - 1) * 100; // To print rounded freq values
            int virtualFreqMax = (viewFreqMax / 100 + 1) * 100; // To print rounded freq values
            int virtualFreqWidth = virtualFreqMax - virtualFreqMin;

            int shortH = height * 10 / 100; // Short line
            int longH = height * 25 / 100; // Long line

            String units = Tools.unitsByRangeId(freqRange.id);

            mFreqLineBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            Bitmap mFreqLineVirtualBitmap =  Bitmap.createBitmap(fToX(virtualFreqWidth), height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mFreqLineVirtualBitmap);

            // Lines
            Paint paint = new Paint();
            paint.setColor(LINES_COLOR);

            // Text values
            Paint textPaint = new Paint();
            textPaint.setColor(FONT_COLOR);
            textPaint.setTextSize(FONT_SIZE);
            textPaint.setAntiAlias(true);

            for (int freq = virtualFreqMin; freq <= virtualFreqMax; freq += 20) {
                int x = fToX(freq - virtualFreqMin);
                if (freq % (200) == 0) {
                    // Long line
                    canvas.drawLine(x, 0, x, longH, paint);
                    // Text labels
                    String freqText = Tools.formatFrequencyValue(freq, units);
                    Rect bounds = new Rect();
                    textPaint.getTextBounds(freqText, 0, freqText.length(), bounds);
                    canvas.drawText(freqText, x - (bounds.width() / 2), longH + FONT_SIZE + 5, textPaint);
                } else {
                    // Short line
                    canvas.drawLine(x, 0, x, shortH, paint);
                }
            }

            mFreqLineBitmap = Bitmap.createBitmap(mFreqLineVirtualBitmap, fToX(viewFreqMin - virtualFreqMin), 0, width, height);
        }

    }

    public void setFreq(int aFreq) {
        if (freqRange != null) {
            freq = (aFreq - freqRange.minFreq) / freqRange.step * freqRange.step + freqRange.minFreq;

            if (freq < freqRange.minFreq) {
                freq = freqRange.minFreq;
            } else if (freq > freqRange.maxFreq) {
                freq = freqRange.maxFreq;
            }

            x = fToX(freq - viewFreqMin);

            invalidate();
        }
    }

    // Frequency to x-coordinate
    private int xToF(int x){
        return Math.round(x * (viewFreqMax - viewFreqMin) / (float)getWidth());
    }

    // X-coordinate to frequency
    private int fToX(int f){
        return Math.round(f * getWidth() / (float) (viewFreqMax - viewFreqMin));
    }

    ///////

    protected void onAttachedToWindow() {
        try {
            mListener = (FreqBarView.NoticeListener) getContext();
        } catch (ClassCastException e) {
            //throw new ClassCastException(getContext().toString() + " must implement NoticeListener");
        }
        loaded = true;
    }

    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        if(mSliderBitmap != null)
        {
            mSliderBitmap.recycle();
            mSliderBitmap = null;
        }
        if(mFreqLineBitmap != null)
        {
            mFreqLineBitmap.recycle();
            mFreqLineBitmap = null;
        }
    }

    protected void onDraw(Canvas canvas)
    {

        if (mFreqLineBitmap != null){
            canvas.drawBitmap(mFreqLineBitmap, 0.0F, 0.0F, new Paint());
        }
        if(mSliderBitmap != null) {
            canvas.drawBitmap(mSliderBitmap, x - mSliderBitmap.getWidth() / 2, 0.0F, new Paint());
        }
        super.onDraw(canvas);
    }


    protected void onSizeChanged(int w, int h, int oldW, int oldH)
    {
        if(mSliderBitmap == null || mFreqLineBitmap == null) {
            initSliderBitmap();
            initFreqLineBitmap();
        }
        if (freqRange != null ) {
            x = fToX(freq - viewFreqMin);
        }
    }

    void onStartTrackingTouch(){
        mListener.onFreqBarStartTrackingTouch(freq, freqRange);
    }

    void onStopTrackingTouch() {
        mListener.onFreqBarStopTrackingTouch(freq, freqRange);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (freqRange != null) {
            switch (motionEvent.getAction()) {
                case 0:
                case 2:
                    freq = xToF((int)motionEvent.getX()) + viewFreqMin;
                    setFreq(freq);
                    onStartTrackingTouch();
                    break;
                case 1:
                case 3:
                    onStopTrackingTouch();
                    break;
            }
        }
        return true;
    }


}
