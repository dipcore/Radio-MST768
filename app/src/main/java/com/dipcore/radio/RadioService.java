package com.dipcore.radio;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.dipcore.radio.tw.FreqRange;
import com.dipcore.radio.tw.Radio;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class RadioService extends Service implements Radio.NoticeListener {

    private Gson mGson = null;
    private Radio mRadio = null;
    private RadioSharedPreferences mPreferences = null;
    NotifyData mNotifyData = null;

    private BroadcastReceiver mIntentReceiver = null;
    private ServiceBinder mServiceBinder;

    private HashMap<String, Stations> mStationList = new HashMap<>(); // Band - Stations
    private HashMap<String, Integer> mIndexes = null; // Band - selection
    private FreqRange mFreqRange = null;
    private Station mStation = null; // Current frequency range
    private String mListName = null; // Current station list FM, AM, FAV
    private int mFreq = -1; // Current freq
    private boolean audioFocus = false;


    private boolean mScanning = false; // Scanning flag

    // Toggle flags
    private boolean TA = false;
    private boolean REG = false;
    private boolean AF = false;
    private boolean LOC = false;

    // RDS
    private boolean RDS_ST = false;
    private boolean RDS_TP = false;
    private boolean RDS_TA = false;
    private int RDS_PTY = -1;
    private String RDS_PS = null;
    private String RDS_Text = null;

    // Settings
    private int mRegionId = -1;

    // Service vars and params
    private boolean started = false;

    // KeyCodes
    private boolean isKeyCodeDebuggingEnabled = false;
    private ArrayList<KeyCode> nextKeyCodes = null;
    private ArrayList<KeyCode> prevKeyCodes = null;
    private ArrayList<KeyCode> seekNextKeyCodes = null;
    private ArrayList<KeyCode> seekPrevKeyCodes = null;
    private ArrayList<ArrayList<KeyCode>> stationKeyCodes = new ArrayList<>();
    private ArrayList<KeyCode> autoScanKeyCodes = null;


    public RadioService() {

        mServiceBinder = new ServiceBinder();

        mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {

                final String action = intent.getAction();

                if (Constants.BROADCAST_ACTION_SET_FREQ.equals(action)) {

                } else if (Constants.BROADCAST_ACTION_PREV_STATION.equals(action)) {
                    prevStation();
                } else if (Constants.BROADCAST_ACTION_NEXT_STATION.equals(action)) {
                    nextStation();
                } else if (Constants.BROADCAST_ACTION_SET_BAND.equals(action)) {

                } else if (Constants.BROADCAST_ACTION_RADIO_QUERY_AUDIO_FOCUS.equals(action)) {
                    queryAudioFocus();
                } else if (Constants.BROADCAST_ACTION_RADIO_RELEASE_AUDIO_FOCUS.equals(action)) {
                    releaseAudioFocus();
                } else if (Constants.BROADCAST_ACTION_REFRESH_PREFERENCES.equals(action)) {
                    initPreferenceValues();
                } else if (Constants.BROADCAST_ACTION_REFRESH_STATION_LIST.equals(action)) {
                    refreshStationList();
                } else if (Constants.BROADCAST_ACTION_REFRESH_STATION.equals(action)) {
                    refreshStation();
                }
            }
        };

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServiceBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {

        // Gson serializer/deserializer //
        mGson = new Gson();

        // Preferences //
        mPreferences = new RadioSharedPreferences(this);

        // IntentFilter //
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_ACTION_SET_FREQ);
        intentFilter.addAction(Constants.BROADCAST_ACTION_PREV_STATION);
        intentFilter.addAction(Constants.BROADCAST_ACTION_NEXT_STATION);
        intentFilter.addAction(Constants.BROADCAST_ACTION_SET_BAND);
        intentFilter.addAction(Constants.BROADCAST_ACTION_REFRESH_PREFERENCES);
        intentFilter.addAction(Constants.BROADCAST_ACTION_REFRESH_STATION);
        intentFilter.addAction(Constants.BROADCAST_ACTION_REFRESH_STATION_LIST);
        intentFilter.addAction(Constants.BROADCAST_ACTION_RADIO_QUERY_AUDIO_FOCUS);
        intentFilter.addAction(Constants.BROADCAST_ACTION_RADIO_RELEASE_AUDIO_FOCUS);
        this.registerReceiver(mIntentReceiver, intentFilter);

        // Initial values //
        initPreferenceValues();

        // Service started
        started = true;
        sendBroadcast(Constants.BROADCAST_INFO_STARTED);

    }

    @Override
    public void onDestroy() {
        mRadio.stop();
        unregisterReceiver(mIntentReceiver);
        stopForeground(true);
        mNotifyData.hide();
        started = false;
        System.out.println("Destroy");
    }

    @Override
    public int onStartCommand(final Intent intent, final int n, final int n2) {

        // Notify & start foreground
        mNotifyData = new  NotifyData( getApplicationContext() );
        startForeground( NotifyData.NOTIFY_ID, mNotifyData.show());

        // Kill stock radio and service
        ActivityManager activityManager = (ActivityManager)getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.killBackgroundProcesses("com.tw.radio");
        activityManager.killBackgroundProcesses("com.tw.radio:RadioService");

        if (intent != null && started) {

            if (mRadio == null) {
                init();
            }

            final String action = intent.getAction();
            if (Constants.BROADCAST_ACTION_SET_FREQ.equals(action)) {

            } else if (Constants.BROADCAST_ACTION_PREV_STATION.equals(action)) {
                if (!audioFocus)
                    queryAudioFocus();
                prevStation();
            } else if (Constants.BROADCAST_ACTION_NEXT_STATION.equals(action)) {
                if (!audioFocus)
                    queryAudioFocus();
                nextStation();
            } else if (Constants.BROADCAST_ACTION_SET_BAND.equals(action)) {

            } else if (Constants.BROADCAST_ACTION_RADIO_QUERY_AUDIO_FOCUS.equals(action)) {
                queryAudioFocus();
            } else if (Constants.BROADCAST_ACTION_REFRESH_PREFERENCES.equals(action)) {
                initPreferenceValues();
            } else if (Constants.BROADCAST_ACTION_REFRESH_STATION_LIST.equals(action)) {
                refreshStationList();
            } else if (Constants.BROADCAST_ACTION_REFRESH_STATION.equals(action)) {
                refreshStation();
            } else if (Constants.BROADCAST_ACTION_SWITCH_STATION_LIST_TO_FM.equals(action)) {
                setStationListName("FM");
            } else if (Constants.BROADCAST_ACTION_SWITCH_STATION_LIST_TO_AM.equals(action)) {
                setStationListName("AM");
            } else if (Constants.BROADCAST_ACTION_SWITCH_STATION_LIST_TO_FAV.equals(action)) {
                setStationListName("FAV");
            }
        }
        return 1;
    }


    /**
     * Private
     */

    private void queryAudioFocus(){
        audioFocus = true;
        mRadio.queryAudioFocus();
    }

    private void releaseAudioFocus(){
        audioFocus = false;
        mRadio.releaseAudioFocus();
    }

    private void initPreferenceValues() {

        // FM
        mStationList.put("FM", mPreferences.getStationList("FM"));
        // AM
        mStationList.put("AM", mPreferences.getStationList("AM"));
        // FAV
        mStationList.put("FAV", favStationList());

        // Current list name
        mListName = mPreferences.getListName();

        // Selected station indexes
        mIndexes = mPreferences.getIndexes();

        // Key codes
        isKeyCodeDebuggingEnabled = mPreferences.isKeyCodeDebuggingEnabled();
        nextKeyCodes = mPreferences.getKeyCodes("next_station", "19");
        prevKeyCodes = mPreferences.getKeyCodes("prev_station", "21");
        seekNextKeyCodes = mPreferences.getKeyCodes("seek_next_station", "0");
        seekPrevKeyCodes = mPreferences.getKeyCodes("seek_prev_station", "0");
        stationKeyCodes.add(mPreferences.getKeyCodes("station_1", "49"));
        stationKeyCodes.add(mPreferences.getKeyCodes("station_2", "50"));
        stationKeyCodes.add(mPreferences.getKeyCodes("station_3", "51"));
        stationKeyCodes.add(mPreferences.getKeyCodes("station_4", "52"));
        stationKeyCodes.add(mPreferences.getKeyCodes("station_5", "53"));
        stationKeyCodes.add(mPreferences.getKeyCodes("station_6", "54"));
        autoScanKeyCodes = mPreferences.getKeyCodes("auto_scan", "0");
    }

    /**
     * Filter FAV list
     */
    private Stations favStationList() {
        Stations Favorites = new Stations();
        Favorites.addAll(Tools.filterFavoriteStations(mStationList.get("FM")));
        Favorites.addAll(Tools.filterFavoriteStations(mStationList.get("AM")));
        return Favorites;
    }

    /**
     * Send broadcast messages
     *
     * @param action
     * @param o,     i, s or b
     */
    private void sendBroadcast(String action, Object o) {
        if (started)
            sendBroadcast(new Intent(action).putExtra("value", mGson.toJson(o)).putExtra("type", "object"));
    }

    private void sendBroadcast(String action, Integer i) {
        sendBroadcast(action, (int) i);
    }

    private void sendBroadcast(String action, int i) {
        if (started)
            sendBroadcast(new Intent(action).putExtra("value", i).putExtra("type", "integer"));
    }

    private void sendBroadcast(String action, String s) {
        if (started)
            sendBroadcast(new Intent(action).putExtra("value", s).putExtra("type", "string"));
    }

    private void sendBroadcast(String action, boolean b) {
        if (started)
            sendBroadcast(new Intent(action).putExtra("value", b).putExtra("type", "boolean"));
    }

    private void sendBroadcast(String action) {
        if (started)
            sendBroadcast(new Intent(action));
    }


    /**
     *
     * Radio Events
     *
     */

    /**
     * Frequency changed event
     *
     * @param freq
     */
    @Override
    public void onFrequencyChanged(int freq) {
        if (mListName != null) {
            mFreq = freq;
            Stations stations = getStationList();
            int index = stations.findIdByFreq(freq);

            if (mFreqRange != null) {
                mStation = (index > -1)?stations.get(index):null;
                mStation = (mStation == null) ? new Station(null, freq, mFreqRange.id) : mStation;
                sendBroadcast(Constants.BROADCAST_INFO_STATION, mStation);
            }

            // Notify
            sendBroadcast(Constants.BROADCAST_INFO_FREQUENCY, mFreq);
            sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST_SELECTION, index);

            // Update notification
            String notification = ((index > -1) ? "[" + String.valueOf(index) + "]" : "")
                    + " " + ((mStation.name != null) ? mStation.name : "")
                    + " " + Tools.formatFrequencyValue(mStation.freq, mFreqRange.units)
                    + " " + mFreqRange.units;
            mNotifyData.setText(notification);

            // Save
            mIndexes.put(mListName, index);
            mPreferences.putIndexes(mIndexes);
            mPreferences.putFrequency(mFreq);
        }
    }


    /**
     * Frequency range changed event
     *
     * @param freqRange
     */
    @Override
    public void onFreqRangeChanged(FreqRange freqRange) {
        mFreqRange = freqRange;
        sendBroadcast(Constants.BROADCAST_INFO_FREQUENCY_RANGE, mFreqRange);
    }


    /**
     * Frequency range received
     */
    public void onFreqRangeParamsReceived() {
        mFreqRange = mRadio.freqRanges.getById(mRadio.freqRange.id);
        sendBroadcast(Constants.BROADCAST_INFO_FREQUENCY_RANGE, mFreqRange);
    }

    /**
     * TA flag was changed event
     *
     * @param flag
     */
    @Override
    public void onFlagChangedTA(boolean flag) {
        TA = flag;
        sendBroadcast(Constants.BROADCAST_INFO_FLAG_TA, flag);
    }

    /**
     * REG flag was changed event
     *
     * @param flag
     */
    @Override
    public void onFlagChangedREG(boolean flag) {
        REG = flag;
        sendBroadcast(Constants.BROADCAST_INFO_FLAG_REG, flag);
    }

    /**
     * AF flag changed
     *
     * @param flag
     */
    @Override
    public void onFlagChangedAF(boolean flag) {
        AF = flag;
        sendBroadcast(Constants.BROADCAST_INFO_FLAG_AF, flag);
    }

    /**
     * DX flag changed
     *
     * @param flag
     */
    @Override
    public void onFlagChangedLOC(boolean flag) {
        LOC = flag;
        sendBroadcast(Constants.BROADCAST_INFO_FLAG_LOC, flag);
    }

    /**
     * RDS_ST flag changed
     *
     * @param flag
     */
    @Override
    public void onFlagChangedRDSST(boolean flag) {
        RDS_ST = flag;
        sendBroadcast(Constants.BROADCAST_INFO_FLAG_RDS_ST, flag);
    }

    /**
     * RDS_TP changed
     *
     * @param flag
     */
    @Override
    public void onFlagChangedRDSTP(boolean flag) {
        RDS_TP = flag;
        sendBroadcast(Constants.BROADCAST_INFO_FLAG_RDS_TP, flag);
    }

    /**
     * RDS_TA changed
     *
     * @param flag
     */
    @Override
    public void onFlagChangedRDSTA(boolean flag) {
        RDS_TA = flag;
        sendBroadcast(Constants.BROADCAST_INFO_FLAG_RDS_TA, flag);
    }

    /**
     * PTY info found
     *
     * @param id
     * @param requestedId
     */
    @Override
    public void onFoundPTY(int id, int requestedId) {
        if (RDS_PTY != id) {
            RDS_PTY = id;
            sendBroadcast(Constants.BROADCAST_INFO_RDS_PTY_ID, id);
            sendBroadcast(Constants.BROADCAST_INFO_RDS_PTY_NAME, mRadio.PTYNames[id]);
        }
    }

    /**
     * RDS_ST text found
     *
     * @param text
     */
    @Override
    public void onFoundRDSPSText(String text) {
        RDS_PS = text;
        sendBroadcast(Constants.BROADCAST_INFO_RDS_PS, text);
    }

    /**
     * RDS message, a long one
     *
     * @param text
     */
    @Override
    public void onFoundRDSText(String text) {
        RDS_Text = text;
        sendBroadcast(Constants.BROADCAST_INFO_RDS_TEXT, text);
    }

    /**
     * Scanning flag changed
     *
     * @param flag
     */
    @Override
    public void onFlagChangedScanning(boolean flag) {

        // On radio init twUtil returns flag - false
        // We need to ignore it

        // Scanning begin
        if (flag) {
            mScanning = true;
            mStationList.get(mListName).clear();
        }

        // Scanning end
        if (mScanning && !flag) {
            mScanning = false;
            sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST, getStationList());
            if (getStationList().size() > 0) {
                setStation(0, getStationList().get(0));
            }
            // Save
            mPreferences.putStationList(mListName, getStationList());
        }

        sendBroadcast(Constants.BROADCAST_INFO_FLAG_SCANNING, mScanning);
    }


    /**
     * Station found (scanning)
     *
     * @param number
     * @param freq
     * @param name
     */
    @Override
    public void onStationFound(int number, int freq, String name) {

        // Station
        Station station = new Station(name, freq, mFreqRange.id);

        // Send broadcast
        sendBroadcast(Constants.BROADCAST_INFO_SCANNING_FOUND_STATION, station);

        // Add station to the station list
        mStationList.get(mListName).add(station);
    }

    /**
     * Region Id
     *
     * @param id
     */
    public void onSetRegionId(int id) {
        mRegionId = id;
        mRadio.getFreqRangeParams();
        sendBroadcast(Constants.BROADCAST_INFO_REGION_ID, id);
        mPreferences.putRegionId(id);
    }

    /**
     * On key pressed
     * @param code
     * @param duration
     */
    public void onKeyPress(int code, int duration) {

        String msg = Integer.toString(code) + (duration == 2?"L":"");

        // Next station
        for (KeyCode kc: nextKeyCodes){
            if (kc.code == code && kc.duration == duration) {
                nextStation();
                msg += " nextStation()";
            }
        }
        // Prev station
        for (KeyCode kc: prevKeyCodes){
            if (kc.code == code && kc.duration == duration) {
                prevStation();
                msg += " prevStation()";
            }
        }
        // Seek next station
        for (KeyCode kc: seekNextKeyCodes){
            if (kc.code == code && kc.duration == duration) {
                seekNextStation();
                msg += " seekNextStation()";
            }
        }
        // Seek prev station
        for (KeyCode kc: seekPrevKeyCodes){
            if (kc.code == code && kc.duration == duration) {
                seekPrevStation();
                msg += " seekPrevStation()";
            }
        }

        // Stations
        for (int i = 0; i < stationKeyCodes.size(); i++) {
            for (KeyCode kc : stationKeyCodes.get(i)) {
                if (kc.code == code && kc.duration == duration) {
                    int size = getStationList().size();
                    if (size >= i) {
                        setStation(i, getStationList().get(i));
                        msg += " setStation()";
                    }
                }
            }
        }

        // Auto scan
        for (KeyCode kc: autoScanKeyCodes){
            if (kc.code == code && kc.duration == duration) {
                autoScan();
                msg += " autoScan()";
            }
        }

        // Debugging
        if (isKeyCodeDebuggingEnabled) {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }

    }

    /**
     *
     * Service binder
     *
     */

    public class ServiceBinder extends Binder {
        RadioService getService() {
            return RadioService.this;
        }
    }


    /**
     * Public methods
     */


    /**
     * Init
     */

    public void init() {

        // Radio //
        if (mRadio == null) {
            mRadio = new Radio();
            mRadio.addListener(this);
        }

        // DEVEL
        mFreqRange = mRadio.freqRange;
        // End DEVEL

        mRadio.init();

        // Set current station list
        setStationListName(mListName);
    }

    /**
     * Get freq-range
     * @return
     */
    public FreqRange getFreqRange() {
        return mFreqRange;
    }

    /**
     * Get station list name
     * @return
     */
    public String getListName() {
        return mListName;
    }

    /**
     * Current station list
     * @return
     */
    public Stations getStationList(){
        return mStationList.get(mListName);
    }

    /**
     * Ger current freq
     * @return
     */
    public int getFreq() {
        return mFreq;
    }

    /**
     * Get current station
     * @return
     */
    public Station getStation() {
        return mStation;
    }

    /**
     * Refresh station list
     */
    public void refreshStationList(){
        initPreferenceValues();
        setStationListName(mListName);
    }

    /**
     * Refresh station
     */
    public void refreshStation(){
        setStation(mIndexes.get(mListName), mStation);
    }

    /**
     * Switch station list by name
     * @param name
     */
    public void setStationListName(String name){
        if (name != null && (name.equals("FM") || name.equals("AM") || name.equals("FAV"))) {

            // Station list
            mListName = name;
            sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST_NAME, mListName);
            sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST, getStationList());

            // Active station
            int index = mIndexes.get(mListName);
            int size = getStationList().size();

            if (index > -1 && size > 0){
                index = (index >= size) ? size - 1 : index;
                index = (index < 0 ) ? 0 : index;
                mStation = getStationList().get(index);
            } else {
                mFreq = mPreferences.getFrequency();
                int freqRangeId = Tools.idByListName(mListName);
                mStation = new Station(null, mFreq, freqRangeId);
            }

            mIndexes.put(mListName, index);
            sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST_SELECTION, mIndexes.get(mListName));

            // Set station
            if (mRadio.freqRange == null || mStation.freqRangeId != mRadio.freqRange.id) {
                // Change freq-range and frequency
                mRadio.setStation(mStation.freqRangeId, mStation.freq);
            } else {
                mRadio.setFreq(mStation.freq);
                // No need to change freq range, just notify about it
                sendBroadcast(Constants.BROADCAST_INFO_FREQUENCY_RANGE, mFreqRange);
            }

            // Save
            mPreferences.putListName(mListName);
            mPreferences.putIndexes(mIndexes);
        }
    }

    /**
     * Set frequency
     * @param freq
     */
    public void setFrequency(int freq){
        mFreq = freq > mFreqRange.maxFreq ? mFreqRange.maxFreq : freq;
        mFreq = freq < mFreqRange.minFreq ? mFreqRange.minFreq : freq;
        mRadio.setFreq(freq);
    }

    /**
     * Set station
     */
    public void setStation(int index, Station station){

        if (station.freqRangeId == mRadio.freqRange.id) {
            mRadio.setFreq(station.freq);
        } else {
            mRadio.setStation(station.freqRangeId, station.freq);
        }

        mIndexes.put(mListName, index);
        sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST_SELECTION, mIndexes.get(mListName));

        // Save
        mPreferences.putIndexes(mIndexes);
    }

    /**
     * Update station
     * @param index
     * @param station
     */
    public void updateStation(int index, Station station){

        // Update station list (based on station FM or AM)
        String origListName = Tools.listNameById(station.freqRangeId);
        mStationList.get(origListName).setByUUID(station.uuid, station);

        // Update FAV station list
        mStationList.put("FAV", favStationList());

        // Notify
        sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST, getStationList());
        setStation(index, getStationList().get(index));

        // Save
        mPreferences.putStationList(mListName, getStationList());
    }

    /**
     * Delete station
     * @param index
     * @param station
     */
    public void deleteStation(int index, Station station){
        // Remove
        String origListName = Tools.listNameById(station.freqRangeId);
        mStationList.get(origListName).removeByUUID(station.uuid);
        mStationList.get("FAV").removeByUUID(station.uuid);

        // Notify
        sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST, getStationList());

        // Switch to next or prev
        int size = getStationList().size();
        index = (index >= size) ? size - 1 : index;
        if (index  > -1) {
            setStation(index, getStationList().get(index));
        }

        // Save
        mPreferences.putStationList(mListName, getStationList());
    }

    /**
     * Update station
     * @param station
     */
    public void addStation(Station station){

        // Add station
        int index = mStationList.get(mListName).size();
        mStationList.get(mListName).add(station);

        // Update FAV station list
        mStationList.put("FAV", favStationList());

        // Notify
        sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST, getStationList());

        // Switch to the new station
        setStation(index, station);

        // Save
        mPreferences.putStationList(mListName, getStationList());
    }

    /**
     * Clear current station list
     */
    public void clearStationList(){
        if (mListName == "FAV") {
            mStationList.get("AM").clearFavorites();
            mStationList.get("FM").clearFavorites();
            mPreferences.putStationList("AM", mStationList.get("AM"));
            mPreferences.putStationList("FM", mStationList.get("FM"));
        }

        mStationList.get(mListName).clear();
        mPreferences.putStationList(mListName, mStationList.get(mListName));

        // Notify
        sendBroadcast(Constants.BROADCAST_INFO_STATION_LIST, getStationList());

        // Save
        mPreferences.putStationList(mListName, getStationList());
    }

    public void toggleREGFlag(){
        mRadio.toggleREGFlag();
    }
    public void toggleTAFlag(){
        mRadio.toggleTAFlag();
    }
    public void toggleAFFlag(){
        mRadio.toggleAFFlag();
    }
    public void toggleLOCFlag(){
        mRadio.toggleLOCFlag();
    }

    public void autoScan(){
        mRadio.autoScan();
    }

    /**
     * Switch to prev station
     */
    public void prevStation(){
        int size = getStationList().size();
        int index = mIndexes.get(mListName);

        if (size > 0) {
            index--;
            index = index < 0 ? size - 1 : index;
            setStation(index, getStationList().get(index));
        }

    }

    /**
     * Switch to next station
     */
    public void nextStation(){
        int size = getStationList().size();
        int index = mIndexes.get(mListName);

        if (size > 0) {
            index++;
            index = index >= size ? 0 : index;
            setStation(index, getStationList().get(index));
        }

    }

    public void seekNextStation(){
        mRadio.seekNextStation();
    }

    public void seekPrevStation(){
        mRadio.seekPrevStation();
    }

}
