package com.dipcore.radio;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;

import android.tw.john.TWUtil;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    static final int IMAGE_SELECTOR = 5555;
    static PreferenceFragment mUISettingsFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        View bar = (View) LayoutInflater.from(this).inflate(R.layout.settings_layout, root, false);
        root.addView(bar, 0); // insert at top

    }


    public static class AboutFragment extends Fragment {

        TextView versionTextView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_about_app, container, false);
            versionTextView = (TextView) view.findViewById(R.id.versionTextView);
            versionTextView.setText(BuildConfig.VERSION_NAME);
            return view;

        }

    }

    public static class RadioSettingsFragment extends PreferenceFragment {

        TWUtil twUtil = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // TW Util
            twUtil = new TWUtil(1);
            twUtil.open(new short[]{265});

            PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_radio, true);
            addPreferencesFromResource(R.xml.preference_radio);

            init();
        }

        @Override
        public void onDestroy(){
            super.onDestroy();
            twUtil.close();
            getActivity().sendBroadcast(new Intent(Constants.BROADCAST_ACTION_REFRESH_PREFERENCES));
        }

        private void init(){
            // Region
            ListPreference regiondListPreference = (ListPreference) findPreference("pref_key_radio_region_id");
            regiondListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    twUtil.write(265, 0, Integer.valueOf(o.toString()));
                    return true;
                }
            });
        }
    }

    public static class UISettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_ui, true);
            addPreferencesFromResource(R.xml.preference_ui);

            mUISettingsFragment = this;

            init();
        }

        @Override
        public void onDestroy(){
            super.onDestroy();
            getActivity().sendBroadcast(new Intent(Constants.BROADCAST_ACTION_REFRESH_PREFERENCES));
        }

        private void init(){

            // BG prefs
            ListPreference backgroundListPreference = (ListPreference) findPreference("pref_key_ui_background");
            backgroundListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    // Color selector
                    if (o.toString().equals("$$solid")) {
                        showColorPickerDialog();
                    }

                    // Image selector
                    if (o.toString().equals("$$custom")) {
                        showImagePicker();
                    }

                    return true;
                }
            });


        }

        private void showColorPickerDialog()
        {
            AlertDialog.Builder colorDialogBuilder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View dialogView = inflater.inflate(R.layout.color_picker_popup, null);
            final ColorPicker picker = (ColorPicker) dialogView.findViewById(R.id.picker);
            SVBar svBar = (SVBar) dialogView.findViewById(R.id.svbar);
            OpacityBar opacityBar = (OpacityBar) dialogView.findViewById(R.id.opacitybar);
            picker.addSVBar(svBar);
            picker.addOpacityBar(opacityBar);
            picker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener()
            {
                @Override
                public void onColorChanged(int color) {

                }
            });
            colorDialogBuilder.setTitle(R.string.select_color);
            colorDialogBuilder.setView(dialogView);
            colorDialogBuilder.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
                            editor.putInt("pref_key_ui_background_color", picker.getColor());
                            editor.commit();
                        }
                    });
            colorDialogBuilder.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            AlertDialog colorPickerDialog = colorDialogBuilder.create();
            colorPickerDialog.show();
        }

        private void showImagePicker(){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            getActivity().startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_SELECTOR);
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == IMAGE_SELECTOR && resultCode == RESULT_OK && null != data) {
                String selectedImagePath = getRealPathFromURI(data.getData());
                SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
                editor.putString("pref_key_ui_background_image_uri", selectedImagePath);
                editor.commit();
            }
        }

        private String getRealPathFromURI(Uri contentURI) {
            Cursor cursor = getActivity().getContentResolver().query(contentURI, null, null, null, null);
            if (cursor == null) {
                return contentURI.getPath();
            } else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                return cursor.getString(idx);
            }
        }

    }

    public static class KeySettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_key, true);
            addPreferencesFromResource(R.xml.preference_key);
        }

        @Override
        public void onDestroy(){
            super.onDestroy();
            getActivity().sendBroadcast(new Intent(Constants.BROADCAST_ACTION_REFRESH_PREFERENCES));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mUISettingsFragment.onActivityResult(requestCode, resultCode, data);
    }
}