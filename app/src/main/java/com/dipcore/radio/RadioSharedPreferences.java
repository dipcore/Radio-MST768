package com.dipcore.radio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class RadioSharedPreferences {

    SharedPreferences sharedPreferences;
    Gson gson;
    Context mContext;

    RadioSharedPreferences(Context context){
        mContext = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public class ImportExportStationList {
        HashMap<String, Integer> indexes = null;
        Stations FM = null;
        Stations AM = null;

        ImportExportStationList(HashMap<String, Integer> indexes, Stations fm_list, Stations am_list){
            this.indexes = indexes;
            this.FM = fm_list;
            this.AM = am_list;
        }
    }

    public int getFrequency(){
        return sharedPreferences.getInt("frequency", -1);
    }

    public void putFrequency(int freq){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("frequency", freq);
        editor.commit();
    }

    public void putListName(String range){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("range", range);
        editor.commit();
    }

    public String getListName(){
        return sharedPreferences.getString("range", "FM");
    }

    public void putStationList(String rangeName, Stations stations){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("stations_" + rangeName, gson.toJson(stations));
        editor.commit();
    }

    public Stations getStationList(String rangeName){
        Stations stations;
        String jsonText = sharedPreferences.getString("stations_" + rangeName, null);
        if (jsonText == null) {
            stations = new Stations();
        } else {
            stations = gson.fromJson(jsonText, Stations.class);
        }
        return (stations == null)?new Stations():(Stations) stations;
    }

    public void putIndexes(HashMap<String, Integer> indexes){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("index_fm", indexes.get("FM"));
        editor.putInt("index_am", indexes.get("AM"));
        editor.putInt("index_fav", indexes.get("FAV"));
        editor.commit();
    }

    public HashMap<String, Integer> getIndexes(){
        HashMap<String, Integer> result = new HashMap<>();
        try {
            result.put("FM", sharedPreferences.getInt("index_fm", -1));
            result.put("AM", sharedPreferences.getInt("index_am", -1));
            result.put("FAV", sharedPreferences.getInt("index_fav", -1));
        } catch (Error e){
            result.put("FM", -1);
            result.put("AM", -1);
            result.put("FAV", -1);
        }
        return result;
    }

    public String getBackground(){
        return sharedPreferences.getString("pref_key_ui_background", "abstract1");
    }
    public int getBackgroundColor(){
        return sharedPreferences.getInt("pref_key_ui_background_color", 2131427435);
    }
    public String getBackgroundImageURI(){
        return sharedPreferences.getString("pref_key_ui_background_image_uri", null);
    }

    public void putRegionId(int id){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pref_key_radio_region_id", String.valueOf(id));
        editor.commit();
    }

    public String getGridSize(){
        return sharedPreferences.getString("pref_key_ui_grid_view", "3x4");
    }

    public int getStationInfoFontSize(){
        return sharedPreferences.getInt("pref_title_ui_grid_view_font_size", 20);
    }

    public ArrayList<KeyCode> getKeyCodes(String name, String defaults) {
        String keyCodesString =  sharedPreferences.getString("pref_key_" + name, defaults);
        ArrayList<KeyCode> result = new ArrayList<>();
        try {
            for (String str : keyCodesString.split(",")) {
                String[] parts = str.split("L");
                int duration = str.toLowerCase().contains("l") ? 2 : 1; // Long = 1, short =1
                result.add(new KeyCode(Integer.valueOf(parts[0]), duration));
            }
        } catch (Error e) {

        }
        return result;
    }

    public boolean isTopBarNotificationsEnabled(){
        return sharedPreferences.getBoolean("pref_key_ui_display_top_bar_notification", true);
    }

    public boolean isToastNotificationsEnabled(){
        return sharedPreferences.getBoolean("pref_key_ui_display_station_info_toasts", true);
    }

    public String getToastNotificationPosition(){
        return sharedPreferences.getString("pref_key_ui_station_info_toasts_position", "top_center");
    }

    public String getToastNotificationDuration(){
        return sharedPreferences.getString("pref_key_ui_station_info_toasts_duration", "long");
    }

    public int getStationInfoToastBgTransparency(){
        return sharedPreferences.getInt("pref_key_ui_station_info_toasts_transparency", 15);
    }

    public boolean isAmBandEnabled(){
        return sharedPreferences.getBoolean("pref_key_radio_enable_am", true);
    }

    public boolean isFreqBarEnabled(){
        return sharedPreferences.getBoolean("pref_key_ui_display_freq_bar", true);
    }

    public boolean isRDSEnabled(){
        return sharedPreferences.getBoolean("pref_key_radio_enable_rds", true);
    }

    public boolean isToggleFullViewEnabled(){
        return sharedPreferences.getBoolean("pref_key_radio_enable_toggle_full_view", false);
    }

    public void exportStationList(String absolutePath, String fileName){
        ImportExportStationList stations = new ImportExportStationList(getIndexes(), getStationList("FM"), getStationList("AM"));
        String content = gson.toJson(stations);
        saveFile(absolutePath, fileName + ".json", content);
    }

    public void importStationList(String absolutePath, String fileName){

        // Get backup
        String json = readFileFile(absolutePath, fileName);
        ImportExportStationList stations = gson.fromJson(json, ImportExportStationList.class);

        // Store it
        putIndexes(stations.indexes);
        putStationList("FM", stations.FM);
        putStationList("AM", stations.AM);

        // Refresh prefs
        mContext.sendBroadcast(new Intent(Constants.BROADCAST_ACTION_REFRESH_STATION_LIST));
    }

    /**
     * PRIVATES
     */

    public void saveFile(String absolutePath, String fileName, String content){
        try {
            File file = new File(absolutePath, fileName);
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(content);
            myOutWriter.close();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readFileFile(String absolutePath, String fileName){
        try {
            File file = new File(absolutePath, fileName);
            FileInputStream fIn = new FileInputStream(file);
            String ret = convertStreamToString(fIn);
            fIn.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
}
