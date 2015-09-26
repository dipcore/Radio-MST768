package com.dipcore.radio;

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dipcore.radio.FileSave.FileSaveFragment;
import com.dipcore.radio.FileSave.FileSelectFragment;
import com.dipcore.radio.tw.FreqRange;
import com.dipcore.radio.ui.FreqBarView;
import com.dipcore.radio.ui.StationInfoView;
import com.dipcore.radio.ui.StationListView;
import com.google.gson.Gson;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class RadioActivity extends FragmentActivity implements StationEditDialog.NoticeListener,
        StationListView.NoticeListener,
        FreqBarView.NoticeListener,
        PopupMenu.OnMenuItemClickListener,
        StationInfoView.NoticeListener,
        StationAddDialog.NoticeListener,
        FileSaveFragment.Callbacks,
        FileSelectFragment.Callbacks
{
    private LinearLayout mainView;
    private RadioGroup modeRadioGroup;
    private CheckBox taCheckBox;
    private CheckBox regCheckBox;
    private CheckBox afCheckBox;
    private CheckBox locCheckBox;
    private TextView rdsMessageTextView;
    private Button prevButton;
    private Button nextButton;
    private Toast stationInfoToast;
    private Toast infoToast;

    private StationInfoView stationInfoView;
    private StationListView stationListView;
    private FreqBarView freqBarView;;

    private Gson mGson;

    private boolean activityVisible = false;
    private boolean isAmBandEnabled = true;
    private boolean isFreqBarEnabled = true;
    private boolean isRdsEnabled = true;
    private boolean isToggleFullViewEnabled = false;
    private boolean isToastNotificationsEnabled = true;
    private String toastNotificationPosition = null;
    private String toastNotificationDuration = null;
    private int toastNotificationTransparency = 75;

    RadioSharedPreferences sharedPreferences = null;
    BroadcastReceiver mBroadcastReceiver = null;
    ServiceConnection mServiceConnection = null;
    RadioService mRadioService = null;


    Handler mUIHandler = new Handler();
    private boolean UIFullViewFlag = true;
    private int mUILastClickId = 0;

    public RadioActivity(){

        mGson = new Gson();

            mServiceConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName componentName, IBinder service) {
                    System.out.println("SERVICE CONNECTED");

                    RadioService.ServiceBinder binder = (RadioService.ServiceBinder) service;
                    mRadioService = binder.getService();
                    mRadioService.init();
                    RadioActivity.this.init();
                }

                public void onServiceDisconnected(final ComponentName componentName) {
                    System.out.println("SERVICE DISCONNECTED");
                    mRadioService = null;
                }
            };

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String action = intent.getAction();

                String type = intent.getStringExtra("type");
                String vString = null;
                String vObject = null;
                int vInteger = -1;
                boolean vBoolean = false;

                if (type != null) {
                    switch (type) {
                        case "integer":
                            vInteger = intent.getIntExtra("value", -1);
                            break;
                        case "string":
                            vString = intent.getStringExtra("value");
                            break;
                        case "boolean":
                            vBoolean = intent.getBooleanExtra("value", false);
                            break;
                        case "object":
                            vObject = intent.getStringExtra("value");
                            break;
                    }
                }


                if (Constants.BROADCAST_ACTION_REFRESH_PREFERENCES.equals(action)){
                    initUIPrefs();
                } else if (Constants.BROADCAST_INFO_STARTED.equals(action)){
                    // Nothing
                } else if (Constants.BROADCAST_INFO_FREQUENCY.equals(action)){
                    UIRenderFrequency(vInteger);
                } else if (Constants.BROADCAST_INFO_STATION.equals(action)){
                    UIRenderStation(mGson.fromJson(vObject, Station.class));
                } else if (Constants.BROADCAST_INFO_STATION_LIST_SELECTION.equals(action)){
                    UIRenderStationListSelection(vInteger);
                } else if (Constants.BROADCAST_INFO_STATION_LIST.equals(action)){
                    UIRenderStationList(mGson.fromJson(vObject, Stations.class));
                } else if (Constants.BROADCAST_INFO_FREQUENCY_RANGE.equals(action)){
                    UIRenderFreqBar(mGson.fromJson(vObject, FreqRange.class));
                } else if (Constants.BROADCAST_INFO_STATION_LIST_NAME.equals(action)){
                    UIRenderListNameValue(vString);
                } else if (Constants.BROADCAST_INFO_FLAG_TA.equals(action)){
                    UIRenderFlagTAValue(vBoolean);
                } else if (Constants.BROADCAST_INFO_FLAG_REG.equals(action)){
                    UIRenderFlagREGValue(vBoolean);
                } else if (Constants.BROADCAST_INFO_FLAG_AF.equals(action)){
                    UIRenderFlagAFValue(vBoolean);
                } else if (Constants.BROADCAST_INFO_FLAG_LOC.equals(action)){
                    UIRenderFlagLOCValue(vBoolean);
                } else if (Constants.BROADCAST_INFO_FLAG_RDS_ST.equals(action)){
                    UIRenderFlagRDSSTValue(vBoolean);
                } else if (Constants.BROADCAST_INFO_FLAG_RDS_TP.equals(action)){
                    UIRenderFlagRDSTPValue(vBoolean);
                } else if (Constants.BROADCAST_INFO_FLAG_RDS_TA.equals(action)){
                    UIRenderFlagRDSTAValue(vBoolean);
                } else if (Constants.BROADCAST_INFO_RDS_PTY_ID.equals(action)){
                    // Nothing
                } else if (Constants.BROADCAST_INFO_RDS_PTY_NAME.equals(action)){
                    UIRenderFlagRDSPTYNameValue(vString);
                } else if (Constants.BROADCAST_INFO_RDS_PS.equals(action)){
                    UIRenderFlagRDSPSValue(vString);
                } else if (Constants.BROADCAST_INFO_RDS_TEXT.equals(action)){
                    UIRenderFlagRDSTextValue(vString);
                } else if (Constants.BROADCAST_INFO_FLAG_SCANNING.equals(action)){
                    UIStationFlag(vBoolean);
                } else if (Constants.BROADCAST_INFO_SCANNING_FOUND_STATION.equals(action)){
                    UIStationFound(mGson.fromJson(vObject, Station.class));
                } else if (Constants.BROADCAST_INFO_REGION_ID.equals(action)){
                    // Nothing
                }
            }
        };

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Settings.System.getInt(getBaseContext(), "test", true);

        // Init prefs
        sharedPreferences = new RadioSharedPreferences(this);

        // Wallpaper background
        String background = sharedPreferences.getBackground();
        if (background.equals("$$wallpaper")) {
            setTheme(R.style.wallpaper_bg);
        }

        // Create view
        setContentView(R.layout.activity_radio);

        // Init views
        initViews();
        initUIPrefs();

        // Connect to service
        startService(new Intent(this, RadioService.class));
        bindService(new Intent(this, RadioService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        // Intent filters
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_INFO_STARTED);
        intentFilter.addAction(Constants.BROADCAST_INFO_FREQUENCY);
        intentFilter.addAction(Constants.BROADCAST_INFO_STATION);
        intentFilter.addAction(Constants.BROADCAST_INFO_STATION_LIST_NAME);
        intentFilter.addAction(Constants.BROADCAST_INFO_STATION_LIST);
        intentFilter.addAction(Constants.BROADCAST_INFO_STATION_LIST_SELECTION);
        intentFilter.addAction(Constants.BROADCAST_INFO_FREQUENCY_RANGE);
        intentFilter.addAction(Constants.BROADCAST_INFO_FLAG_TA);
        intentFilter.addAction(Constants.BROADCAST_INFO_FLAG_REG);
        intentFilter.addAction(Constants.BROADCAST_INFO_FLAG_AF);
        intentFilter.addAction(Constants.BROADCAST_INFO_FLAG_LOC);
        intentFilter.addAction(Constants.BROADCAST_INFO_FLAG_RDS_ST);
        intentFilter.addAction(Constants.BROADCAST_INFO_FLAG_RDS_TP);
        intentFilter.addAction(Constants.BROADCAST_INFO_FLAG_RDS_TA);
        intentFilter.addAction(Constants.BROADCAST_INFO_RDS_PTY_ID);
        intentFilter.addAction(Constants.BROADCAST_INFO_RDS_PTY_NAME);
        intentFilter.addAction(Constants.BROADCAST_INFO_RDS_PS);
        intentFilter.addAction(Constants.BROADCAST_INFO_RDS_TEXT);
        intentFilter.addAction(Constants.BROADCAST_INFO_FLAG_SCANNING);
        intentFilter.addAction(Constants.BROADCAST_INFO_SCANNING_FOUND_STATION);
        intentFilter.addAction(Constants.BROADCAST_INFO_REGION_ID);
        this.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        sendBroadcast(new Intent(Constants.BROADCAST_ACTION_RADIO_RELEASE_AUDIO_FOCUS));
        unregisterReceiver(mBroadcastReceiver);
        unbindService(mServiceConnection);
        stopService(new Intent(this, RadioService.class));
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        activityVisible = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        sendBroadcast(new Intent(Constants.BROADCAST_ACTION_RADIO_QUERY_AUDIO_FOCUS));
        activityVisible = true;
        super.onResume();
        initUIPrefs();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            toggleFullView();
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Private
     */

    private void init(){
    }

    private void initViews(){

        // Views
        mainView = (LinearLayout)findViewById(R.id.main);
        modeRadioGroup = (RadioGroup)findViewById(R.id.modeSwitch);
        rdsMessageTextView = (TextView)findViewById(R.id.rdsMessageTextView);
        rdsMessageTextView.setMovementMethod(new ScrollingMovementMethod());

        // Check boxes
        taCheckBox = (CheckBox)findViewById(R.id.taToggleBtn);
        regCheckBox = (CheckBox)findViewById(R.id.regToggleBtn);
        afCheckBox = (CheckBox)findViewById(R.id.afToggleBtn);
        locCheckBox = (CheckBox)findViewById(R.id.locToggleBtn);

        // Buttons
        nextButton = (Button)findViewById(R.id.nextButton);
        prevButton = (Button)findViewById(R.id.prevButton);
        nextButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onNextStationBtnLongClick(v);
                return true;
            }
        });
        prevButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onPrevStationBtnLongClick(v);
                return true;
            }
        });

        // Station list view
        stationListView = (StationListView) findViewById(R.id.stationListView);
        // Frequency bar view
        freqBarView = (FreqBarView) findViewById(R.id.freqBar);
        // Station info view
        stationInfoView = (StationInfoView)findViewById(R.id.stationInfoView);
        stationInfoView.addListener(this);
    }

    private void UIRenderFrequency(int freq){
        freqBarView.setFreq(freq);
    }

    private void UIRenderFreqBar(FreqRange freqRange){
        freqBarView.setFreqRange(freqRange);
    }

    private void UIRenderStationList(Stations stations){
        stationListView.set(stations);
    }

    private void UIRenderStationListSelection(int index){
        stationListView.setSelection(index);
    }

    private void UIRenderStation(Station station){
        showStationInfoToast(station);
        stationInfoView.showStation(station);
    }

    private void UIRenderListNameValue(String listName){
        switch(listName){
            case "FM":
                modeRadioGroup.check(R.id.fmMode);
                break;
            case "AM":
                modeRadioGroup.check(R.id.amMode);
                break;
            case "FAV":
                modeRadioGroup.check(R.id.favMode);
                break;
        }
    }

    private void UIRenderFlagTAValue(boolean flag) {
        taCheckBox.setChecked(flag);
    }

    private void UIRenderFlagREGValue(boolean flag){
        regCheckBox.setChecked(flag);
    }

    private void UIRenderFlagAFValue(boolean flag) {
        afCheckBox.setChecked(flag);
    }

    private void UIRenderFlagLOCValue(boolean flag){
        locCheckBox.setText(flag ? R.string.loc_btn : R.string.dx_btn);
        locCheckBox.setChecked(flag);
    }

    private void UIRenderFlagRDSSTValue(boolean flag) {
        stationInfoView.setRDSSTFlag(flag);
    }

    private void UIRenderFlagRDSTPValue(boolean flag) {
        stationInfoView.setRDSTPFlag(flag);
    }

    private void UIRenderFlagRDSTAValue(boolean flag){
        stationInfoView.setRDSTAFlag(flag);
    }

    private void UIRenderFlagRDSPTYNameValue(String text){
        if (isRdsEnabled)
            stationInfoView.setRDSPTYText(text);
    }

    private void UIRenderFlagRDSPSValue(String text){
        if (isRdsEnabled)
            stationInfoView.setRDSPSText(text);
    }

    private void UIRenderFlagRDSTextValue(String text){
        //rdsMessageTextView.setText("one\ntwo\nthree");
        //rdsMessageTextView.setMovementMethod(new ScrollingMovementMethod());
        rdsMessageTextView.setText(text);
    }

    private void UIStationFound(Station station){
        stationListView.add(station);
    }

    private void UIStationFlag(boolean flag){
        if (flag)
            stationListView.clear();
    }

    private void initUIPrefs(){

        // Set background
        String background = sharedPreferences.getBackground();
        if (background.equals("$$solid")) { // Solid color
            int backgroundColor = sharedPreferences.getBackgroundColor();
            mainView.setBackgroundColor(backgroundColor);
        } else if (background.equals("$$custom")) { // Custom image
            String uri = sharedPreferences.getBackgroundImageURI();
            mainView.setBackgroundDrawable(Drawable.createFromPath(uri));
        } else { // Image resource from preset
            mainView.setBackgroundResource(getResources().getIdentifier(background, "drawable", this.getPackageName()));
        }

        // Grid view
        String gridSize = sharedPreferences.getGridSize();
        String[] parts = gridSize.split("x");
        int rowNum = Integer.valueOf(parts[0]);
        int colNum = Integer.valueOf(parts[1]);
        stationListView.setGridSize(rowNum, colNum);
        stationListView.setFontSize(sharedPreferences.getStationInfoFontSize());

        // Toasts
        isToastNotificationsEnabled = sharedPreferences.isToastNotificationsEnabled();
        toastNotificationPosition = sharedPreferences.getToastNotificationPosition();
        toastNotificationDuration = sharedPreferences.getToastNotificationDuration();
        toastNotificationTransparency = sharedPreferences.getStationInfoToastBgTransparency();

        // AM band
        isAmBandEnabled = sharedPreferences.isAmBandEnabled();
        if (isAmBandEnabled)
            findViewById(R.id.amMode).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.amMode).setVisibility(View.GONE);

        // Freq bar
        isFreqBarEnabled = sharedPreferences.isFreqBarEnabled();
        if (isFreqBarEnabled)
            findViewById(R.id.freqBarLayout).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.freqBarLayout).setVisibility(View.GONE);

        // RDS
        isRdsEnabled = sharedPreferences.isRDSEnabled();

        // Toggle full view
        isToggleFullViewEnabled = sharedPreferences.isToggleFullViewEnabled();
        toggleFullView();
    }

    private class UIToggleTask implements Runnable {

        int id = -1;

        public UIToggleTask(int UILastClickId){
            id = UILastClickId;
        }

        @Override
        public void run() {

            if (id == mUILastClickId) {
                UIFullViewFlag = false;

                // Hide freq bar
                findViewById(R.id.freqBarLayout).setVisibility(View.GONE);

                // Hide toggles bar
                findViewById(R.id.toggleBar).setVisibility(View.GONE);

                // Single row grid size
                String gridSize = sharedPreferences.getGridSize();
                String[] parts = gridSize.split("x");
                int rowNum = 1;
                int colNum = Integer.valueOf(parts[1]);
                stationListView.setGridSize(rowNum, colNum);
                stationListView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.15f));
            }
        }
    }

    private void toggleFullView(){

        if (isToggleFullViewEnabled) {
            if (UIFullViewFlag) {
                    mUILastClickId++;
                    mUIHandler.postDelayed(new UIToggleTask(mUILastClickId), 30000);
            } else {
                UIFullViewFlag = true;
                if (isFreqBarEnabled)
                    findViewById(R.id.freqBarLayout).setVisibility(View.VISIBLE);

                findViewById(R.id.toggleBar).setVisibility(View.VISIBLE);

                // Original grid size
                String gridSize = sharedPreferences.getGridSize();
                String[] parts = gridSize.split("x");
                int rowNum = Integer.valueOf(parts[0]);
                int colNum = Integer.valueOf(parts[1]);
                stationListView.setGridSize(rowNum, colNum);
                stationListView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.8f));

                toggleFullView();
            }
        } else {
            stationListView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.8f));
            if (isFreqBarEnabled)
                findViewById(R.id.freqBarLayout).setVisibility(View.VISIBLE);

            findViewById(R.id.toggleBar).setVisibility(View.VISIBLE);
        }
    }

    private void displayStationAddDialog(){
        if (mRadioService.getListName() != "FAV") {
            // Add station dialog
            StationAddDialog dialog = new StationAddDialog();
            dialog.station = mRadioService.getStation();
            dialog.freqRange = mRadioService.getFreqRange();

            // Show dialog
            dialog.show(getFragmentManager(), "StationAddDialog");
        }
    }

    private void displaySaveStationListDialog(){
        FileSaveFragment fsf = FileSaveFragment.newInstance(".json",
                R.string.alert_save,
                R.string.alert_cancel,
                R.string.save_station_list_dialog_title,
                R.string.save_station_file_name_hint,
                R.mipmap.ic_launcher);

        fsf.show(getFragmentManager(), "save_station_list");
    }

    private void displayRestoreStationListDialog(){
        FileSelectFragment fsf = FileSelectFragment.newInstance(FileSelectFragment.Mode.FileSelector,
                R.string.alert_ok,
                R.string.alert_cancel,
                R.string.restore_station_list_dialog_title,
                R.mipmap.ic_launcher,
                R.mipmap.ic_folder,
                R.mipmap.ic_file_json);

        fsf.setFilter(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".json");
                    }
        });

        // Restrict selection to *.xml files
        ArrayList<String> allowedExtensions = new ArrayList<String>();
        allowedExtensions.add(".json");
        fsf.setFilter(FileSelectFragment.FiletypeFilter(allowedExtensions));

        fsf.show(getFragmentManager(), "restore_station_list");
    }

    private void showStationInfoToast(Station station){

        if (!isToastNotificationsEnabled || activityVisible)
            return;

        String units;
        String freq;

        TextView freqValueTextView;
        TextView freqRangeUnits;
        TextView smallFreqTextView;


        // Remove old toast
        if (stationInfoToast != null)
            stationInfoToast.cancel();

        // Set layout and views
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.station_info_toast, null);

        freqValueTextView = (TextView)layout.findViewById(R.id.topTextView);
        freqRangeUnits = (TextView)layout.findViewById(R.id.freqRangeUtits);
        smallFreqTextView = (TextView)layout.findViewById(R.id.PTYName);

        units = Tools.unitsByRangeId(station.freqRangeId);
        freq = Tools.formatFrequencyValue(station.freq, units);


        freqRangeUnits.setText(units);
        if (station.name == null) {
            // Freq as station name
            freqValueTextView.setText(freq);
            // Hide small frequency text
            smallFreqTextView.setVisibility(View.INVISIBLE);
        } else {
            // Station name
            freqValueTextView.setText(station.name);
            // Small freq
            smallFreqTextView.setText(freq);
            smallFreqTextView.setVisibility(View.VISIBLE);
        }


        stationInfoToast = new Toast(getApplicationContext());
        switch (toastNotificationPosition){
            case "top_center":
                stationInfoToast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP, 0, 0);
                break;
            case "middle_center":
                stationInfoToast.setGravity(Gravity.CENTER, 0, 0);
                break;
            case "bottom_center":
                stationInfoToast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 0);
                break;
        }

        layout.findViewById(R.id.toastBox).getBackground().setAlpha(255 * (100 - toastNotificationTransparency) / 100);
        stationInfoToast.setDuration(toastNotificationDuration.equals("long") ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        stationInfoToast.setView(layout);
        stationInfoToast.show();
    }

    private void showInfoToast(int resId, int duration) {
        // Remove old toast
        if (infoToast != null)
            infoToast.cancel();

        infoToast = new Toast(getApplicationContext());
        infoToast.makeText(getApplicationContext(), resId, duration).show();
    }

    private void showInfoToast(String message, int duration){
        // Remove old toast
        if (infoToast != null)
            infoToast.cancel();

        infoToast = new Toast(getApplicationContext());
        infoToast.makeText(getApplicationContext(), message, duration).show();
    }



    /**
     *
     * Buttons
     *
     */

    public void onREGBtnClick(View view){
        mRadioService.toggleREGFlag();
    }

    public void onTABtnClick(View view){
        mRadioService.toggleTAFlag();
    }

    public void onAFBtnClick(View view){
        mRadioService.toggleAFFlag();
    }

    public void onLOCBtnClick(View view){
        mRadioService.toggleLOCFlag();
    }

    public void onFMRadioBtnClicked(View view) {
        mRadioService.setStationListName("FM");
    }

    public void onAMRadioBtnClicked(View view) {
        mRadioService.setStationListName("AM");
    }

    public void onFAVRadioBtnClicked(View view){
        mRadioService.setStationListName("FAV");
    }

    public void onHomeBtnClicked(View view) {
        Tools.switchToHome(this);
    }

    public void onMoreBtnClicked(View view){
        PopupMenu popup = new PopupMenu(this, view);
        Menu popupMenu = popup.getMenu();
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_radio, popup.getMenu());

        // Hide auto-scan item on FAV
        if (mRadioService.getListName().equals("FAV")) {
            popupMenu.findItem(R.id.action_auto_scan).setEnabled(false);
            popupMenu.findItem(R.id.action_add_station).setEnabled(false);
        } else {
            popupMenu.findItem(R.id.action_auto_scan).setEnabled(true);
            popupMenu.findItem(R.id.action_add_station).setEnabled(true);
        }

        popup.show();
        popup.setOnMenuItemClickListener(this);
    }

    public void onEqualizerBtnClicked(View view){
        Tools.switchToEqualizer(this);
    }

    public void onBackBtnClicked(View view){
        finish();
    }

    public void onPrevStationBtnClick(View view){
        mRadioService.prevStation();
    }
    public void onNextStationBtnClick(View view){
        mRadioService.nextStation();
    }

    public void onPrevStationBtnLongClick(View view){
        mRadioService.seekPrevStation();
    }
    public void onNextStationBtnLongClick(View view){
        mRadioService.seekNextStation();
    }

    /**
     *
     * Station List View Events
     *
     */

    /**
     * Click on station
     * @param index
     */
    public void onStationCellClicked(int index, Station station){
        mRadioService.setStation(index, station);
    }

    /**
     * Long click on station
     * @param index
     */
    public void onStationCellLongClicked(int index, Station station){
        StationEditDialog dialog = new StationEditDialog();
        dialog.index = index;
        dialog.station = station;

        // //
        dialog.freqRange = mRadioService.getFreqRange();
        dialog.range = mRadioService.getListName();
        // //

        // Show dialog
        dialog.show(getFragmentManager(), "StationEditDialog");
    }

    /**
     *
     * Station Edit Dialog Events
     *
     */

    /**
     * Station Edit Dialog  - Save callback
     * @param dialog
     * @param index
     * @param station
     */
    @Override
    public void onStationEditDialogSaveClick(DialogFragment dialog, int index, Station station) {
        mRadioService.updateStation(index, station);
    }

    /**
     * Station Edit Dialog  - Delete callback
     * @param dialog
     */
    @Override
    public void onStationEditDialogDeleteClick(DialogFragment dialog, int index, Station station) {
        mRadioService.deleteStation(index, station);
    }

    /**
     * Station Edit Dialog  - Cancel callback
     * @param dialog
     */
    @Override
    public void onStationEditDialogCancelClick(DialogFragment dialog, int stationId) {
    }

    /**
     *
     * Station Add Dialog Events
     *
     */

    /**
     * Station Add Dialog - Save cb
     * @param dialog
     * @param station
     */
    @Override
    public void onStationAddDialogSaveClick(DialogFragment dialog, Station station){
        mRadioService.addStation(station);
    }

    /**
     * Station Add Dialog - Cancel cb
     * @param dialog
     */
    @Override
    public void onStationAddDialogCancelClick(DialogFragment dialog){

    }

    /**
     *
     * Freq Bar View Events
     *
     */

    /**
     * Stop touch tracking (Freq bar view)
     * Render frequency value (from frequency view on touch motion event)
     * @param freq
     */
    @Override
    public void onFreqBarStopTrackingTouch(int freq, FreqRange freqRange){
        mRadioService.setFrequency(freq);
    }

    /**
     * Start touch tracking (Freq bar view)
     * Render frequency value (from frequency view on touch motion event)
     * @param freq
     */
    @Override
    public void onFreqBarStartTrackingTouch(int freq, FreqRange freqRange){
        stationInfoView.showStationFrequency(freq, freqRange.id);
    }

    /**
     * Long click on station info bar
     * @param station
     */
    public void onStationInfoViewLongClick(Station station) {
        if (station == null)
            return;
        if (mRadioService.getStationList().findIdByFreq(station.freq) == -1)
            displayStationAddDialog();
        else
            showInfoToast(R.string.station_exists_message, 1);
    }

    /**
     * Menu item click
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_station:
                displayStationAddDialog();
                return true;
            case R.id.action_auto_scan:
                mRadioService.autoScan();
                return true;
            case R.id.action_clear_list:
                mRadioService.clearStationList();
                return true;
            case R.id.action_save_station_list:
                displaySaveStationListDialog();
                return true;
            case R.id.action_restore_station_list:
                displayRestoreStationListDialog();
                return true;
            case R.id.action_settings:
                Tools.switchToSettings(this);
                return true;
            default:
                return false;
        }
    }

    /**
     *
     * Save station list callback
     *
     */

    /**
     *  On can save callback
     * @param absolutePath - Absolute path to target directory.
     * @param fileName     - Filename. Not guaranteed to have a type extension.
     *
     * @return
     */

    @Override
    public boolean onCanSave(String absolutePath, String fileName ){
        boolean canSave = true;

        // Catch the really stupid case.
        if (absolutePath == null || absolutePath.length() ==0 ||
                fileName == null || fileName.length() == 0) {
            canSave = false;
            showInfoToast(R.string.alert_supply_filename, Toast.LENGTH_SHORT);
        }

        // Do we have a filename if the extension is thrown away?
        if (canSave) {
            String copyName = FileSaveFragment.NameNoExtension(fileName);
            if (copyName == null || copyName.length() == 0 ) {
                canSave = false;
                showInfoToast(R.string.alert_supply_filename, Toast.LENGTH_SHORT);
            }
        }

        // Allow only alpha-numeric names. Simplify dealing with reserved path
        // characters.
        if (canSave) {
            if (!FileSaveFragment.IsAlphaNumeric(fileName)) {
                canSave = false;
                showInfoToast(R.string.alert_bad_filename_chars, Toast.LENGTH_SHORT);
            }
        }

        // No overwrite of an existing file.
        if (canSave) {
            if (FileSaveFragment.FileExists(absolutePath, fileName)) {
                canSave = false;
                showInfoToast(R.string.alert_file_exists, Toast.LENGTH_SHORT);
            }
        }

        return canSave;
    }

    /**
     * On confirm save
     * @param absolutePath - Absolute path to target directory.
     * @param fileName     - Filename. Not guaranteed to have a type extension.
     */
    @Override
    public void onConfirmSave(String absolutePath, String fileName){
        if (absolutePath != null && fileName  != null) {
            sharedPreferences.exportStationList(absolutePath, fileName);
        }
    }

    /**
     *
     * Restore stations callbacks
     *
     */

    /**
     * On confirm select
     * @param absolutePath - Absolute path to target directory.
     * @param fileName     - Filename. Will be null if Mode = DirectorySelector
     *
     */
    @Override
    public void onConfirmSelect(String absolutePath, String fileName) {
        if (absolutePath != null && fileName != null) {
            sharedPreferences.importStationList(absolutePath, fileName);
        }
    }

    /**
     * Is valid selection
     * @param absolutePath
     * @param fileName
     * @return
     */
    @Override
    public boolean isValid(String absolutePath, String fileName) {
        return true;
    }

}
