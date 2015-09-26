package com.dipcore.radio.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.tw.john.TWUtil;
import android.util.AttributeSet;
import android.view.View;

import com.dipcore.radio.R;

/**
 * Created by xXx on 9/11/2015.
 */
public class BrightnessToggle extends View implements View.OnClickListener {

    TWUtil twUtil;
    int brightnessMode = 0;

    public BrightnessToggle(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);

        twUtil = new TWUtil(1);
        twUtil.open(new short[] { 258 });
        twUtil.start();

        twUtil.addHandler("brightnessToggle", new BHandler());
        twUtil.write(258, 255);

    }

    @Override
    public void onClick(View view) {
        int newBrightnessMode = brightnessMode - 1 < 0 ? 2 : brightnessMode - 1;
        twUtil.write(258, 1, newBrightnessMode);
    }

    private class BHandler extends Handler {
        public void handleMessage(final Message message) {
            switch (message.what) {
                case 258: {
                    switch (brightnessMode = message.arg2) {
                        case 2: {
                            // Dark
                            // Dark Icon Here
                            //setBackgroundResource(R.drawable.ic_settings);
                            return;
                        }
                        case 1: {
                            // Light
                            // Light Icon Here
                            //setBackgroundResource(R.drawable.ic_info);
                            return;
                        }
                        case 0: {
                            // Auto
                            // Auto Icon Here
                            //setBackgroundResource(R.drawable.ic_keyboard);
                            return;
                        }
                    }
                }
            }
        }
    }


}
