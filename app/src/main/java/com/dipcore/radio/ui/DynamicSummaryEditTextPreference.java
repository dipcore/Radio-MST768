package com.dipcore.radio.ui;

import android.content.Context;
import android.util.AttributeSet;

class DynamicSummaryEditTextPreference extends android.preference.EditTextPreference {

    public DynamicSummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        return this.getText();
    }
}