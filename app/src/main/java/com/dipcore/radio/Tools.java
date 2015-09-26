package com.dipcore.radio;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.Log;

import com.dipcore.radio.tw.FreqRange;

import java.util.HashMap;
import java.util.Locale;

public class Tools {

    public static void printInfo1028(int val){
            System.out.println("0x1    ??        " + ((val & 0x1) == 0x1));
            System.out.println("0x2    RDS_TA    " + ((val & 0x2) == 0x2));
            System.out.println("0x4    REG       " + ((val & 0x4) == 0x4));
            System.out.println("0x8    ???       " + ((val & 0x8) == 0x8));
            System.out.println("0x10   ???       " + ((val & 0x10) == 0x10));
            System.out.println("0x20   TA        " + ((val & 0x20) == 0x20));
            System.out.println("0x40   AF        " + ((val & 0x40) == 0x40));
            System.out.println("0x80   RDS_TP    " + ((val & 0x80) == 0x80));
    }

    public static void printInfo1025(int val){
            System.out.println("0x1    ??        " + ((val & 0x1) == 0x1));
            System.out.println("0x2    ??        " + ((val & 0x2) == 0x2));
            System.out.println("0x4    ??        " + ((val & 0x4) == 0x4));
            System.out.println("0x8    LOC       " + ((val & 0x8) == 0x8));
            System.out.println("0x10   RDS_ST    " + ((val & 0x10) == 0x10));
            System.out.println("0x20   ??        " + ((val & 0x20) == 0x20));
            System.out.println("0x40   Seek stat " + ((val & 0x40) == 0x40));
            System.out.println("0x80   Search    " + ((val & 0x80) == 0x80));
    }

    public static String formatFrequencyValue(int aFreq, String aUnits){
        Locale locale = Locale.US;
        String freqString = "";
        switch (aUnits) {
            case "MHz":
                freqString = String.format(locale, "%.2f", (float) aFreq / 100F);
                break;
            case "KHz":
                freqString = String.format(locale, "%d", aFreq);
                break;
        }
        return freqString;
    }

    public static String unitsByRangeId(int id){
        String v = "MHz";
        switch (id) {
            case 0:
            case 1:
                v = "MHz";
                break;
            case 2:
                v = "KHz";
                break;
        }
        return v;
    }

    public static String listNameById(int id) {
        String v = "FM";
        switch (id) {
            case 0:
            case 1:
                v = "FM";
                break;
            case 2:
                v = "AM";
                break;
            case 255:
                v = "FAV";
        }
        return v;
    }


    public static int idByListName(String range) {
        int id = 0;
        switch (range) {
            case "FM":
                id = 0;
                break;
            case "AM":
                id = 2;
                break;
            case "FAV":
                id = 0;
        }
        return id;
    }






    public static void switchToHome(Activity a){
        try {
            final Intent intent = new Intent("android.intent.action.MAIN");
            intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.addCategory("android.intent.category.HOME");
            a.startActivity(intent);
            //this.condensed = true;
        }
        catch (Exception ex) {
            Log.e("RadioActivity", Log.getStackTraceString((Throwable) ex));
        }
    }

    public static void switchToEqualizer(Activity a) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.tw.eq", "com.tw.eq.EQActivity");
            intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            a.startActivity(intent);
            //this.condensed = true;
        } catch (Exception ex) {
            Log.e("RadioActivity", Log.getStackTraceString((Throwable) ex));
        }
    }

    public static void switchToSettings(Activity a) {
        try {
            Intent intent = new Intent(a, SettingsActivity.class);

            a.startActivity(intent);
        } catch (Exception ex) {
            Log.e("RadioActivity", Log.getStackTraceString((Throwable) ex));
        }
    }

    public static Stations filterFavoriteStations(Stations stations){
        Stations result = new Stations();
        for (Station station : stations) {
            if (station.favorite){
                result.add(new Station(station.name, station.freq, station.freqRangeId, station.uuid));
            }
        }
        return result;
    }

    public static int testFrequency(int freq, FreqRange freqRange){
        freq = (freq > freqRange.maxFreq) ? freqRange.maxFreq : freq;
        freq = (freq < freqRange.minFreq) ? freqRange.minFreq : freq;
        return freq;
    }

    public static Bitmap createTextBitmap(final String text, final Typeface typeface, final float textSizePixels, final int textColour)
    {
        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setColor(textColour);
        textPaint.setTextAlign(Paint.Align.LEFT);

        Bitmap myBitmap = Bitmap.createBitmap((int) textPaint.measureText(text), (int) textSizePixels, Bitmap.Config.ARGB_8888);
        Canvas myCanvas = new Canvas(myBitmap);
        myCanvas.drawText(text, 0, myBitmap.getHeight(), textPaint);

        return myBitmap;
    }

    public static boolean isEmptyString(String str){
        final int len = str.length();
        boolean empty = true;
        if (str != null)
            for (int i = 0; i < len; i++) {
                if (str.charAt(i) != ' ') {
                    empty = false;
                    break;
                }
            }
        else
            empty = false;
        return empty;
    }

}
