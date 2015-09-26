package com.dipcore.radio;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.google.gson.Gson;


public class RadioWidgetExtended extends AppWidgetProvider {

    private Station station = null;
    private String PTYName = "";
    private String RDS_PS = "";

    private Gson mGson = new Gson();
    private boolean initialised = false;

    RadioSharedPreferences sharedPreferences = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        initWidget(context);

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }

    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        mGson = null;
        station = null;
        initialised = false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), RadioWidgetExtended.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        String action = intent.getAction();

        // Station
        if (Constants.BROADCAST_INFO_STATION.equals(action)) {
            String value = intent.getStringExtra("value");
            if (value != null) {
                station = mGson.fromJson(value, Station.class);
                PTYName = "";
                RDS_PS = "";
                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        } else if (Constants.BROADCAST_INFO_RDS_PTY_NAME.equals(action)){
            String value = intent.getStringExtra("value");
            if (value != null && !value.equals(PTYName)) {
                PTYName = value;
                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        } else if (Constants.BROADCAST_INFO_RDS_PS.equals(action)){
            String value = intent.getStringExtra("value");
            if (!value.equals(RDS_PS)) {
                RDS_PS = value;
                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged (Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions){

    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.radio_widget_extended);

        if (station != null){

            String freqParsed = null;
            String units = null;


            units = Tools.unitsByRangeId(station.freqRangeId);
            freqParsed = Tools.formatFrequencyValue(station.freq, units);

            views.setTextViewText(R.id.freqRangeUnits, units);
            views.setTextViewText(R.id.PTYName, PTYName);

            if (PTYName != null && PTYName != "") {
                views.setViewVisibility(R.id.PTYName, View.VISIBLE);
                views.setViewVisibility(R.id.PTYIcon, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.PTYName, View.GONE);
                views.setViewVisibility(R.id.PTYIcon, View.GONE);
            }

            if (!Tools.isEmptyString(RDS_PS)) {
                views.setTextViewText(R.id.RDS_PS, RDS_PS);
            } else {
                views.setTextViewText(R.id.RDS_PS, freqParsed);
            }

            if (station.name == null || station.name.equals("")) {
                views.setViewVisibility(R.id.freqRangeUnits, View.VISIBLE);
                views.setTextViewText(R.id.topTextView, freqParsed);
            } else {
                views.setViewVisibility(R.id.freqRangeUnits, View.GONE);
                views.setTextViewText(R.id.topTextView, station.name);
            }

        } else {
            sendBroadcast(context, Constants.BROADCAST_ACTION_REFRESH_STATION);
        }

        // Prev station
        views.setOnClickPendingIntent(R.id.widgetPrevStationBtn, sendServiceCommand(context, Constants.BROADCAST_ACTION_PREV_STATION));

        // Next station
        views.setOnClickPendingIntent(R.id.widgetNextStationBtn, sendServiceCommand(context, Constants.BROADCAST_ACTION_NEXT_STATION));

        // Open radio app on click
        views.setOnClickPendingIntent(R.id.radioWidgetExtended, PendingIntent.getActivity(context, 0, new Intent(context, (Class) RadioActivity.class), PendingIntent.FLAG_CANCEL_CURRENT));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private PendingIntent sendServiceCommand(Context context, String action) {
        ComponentName componentName = new ComponentName(context, RadioService.class);
        Intent intent = new Intent(action);
        intent.setComponent(componentName);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    private void sendBroadcast(Context context, String action){
        Context c = context.getApplicationContext();
        c.sendBroadcast(new Intent(action));
    }

    private void initWidget(Context context){
        if (!initialised) {

            // Shared prefs
            sharedPreferences = new RadioSharedPreferences(context);

            Context c = context.getApplicationContext();
            IntentFilter filter1 = new IntentFilter();
            filter1.addAction(Constants.BROADCAST_INFO_RDS_PTY_NAME);
            filter1.addAction(Constants.BROADCAST_INFO_STATION);
            filter1.addAction(Constants.BROADCAST_INFO_RDS_PS);
            c.registerReceiver(this, filter1);

            initialised = true;
        }
    }

}
