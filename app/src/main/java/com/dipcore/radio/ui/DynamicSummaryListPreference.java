package com.dipcore.radio.ui;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

class DynamicSummaryListPreference extends ListPreference {
    private int index;

    public DynamicSummaryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DynamicSummaryListPreference(Context context) {
        super(context);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        setSummary(value);
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(getEntry());
    }
}