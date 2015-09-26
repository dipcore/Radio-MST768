package com.dipcore.radio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.dipcore.radio.tw.FreqRange;

public class StationEditDialog extends DialogFragment {

    public interface NoticeListener {
        public void onStationEditDialogSaveClick(DialogFragment dialog, int index, Station station);
        public void onStationEditDialogCancelClick(DialogFragment dialog, int index);
        public void onStationEditDialogDeleteClick(DialogFragment dialog, int index, Station station);
    }

    // Dialog params
    public int index;
    public Station station;
    public FreqRange freqRange;
    public String range;
    private String units;

    // Views
    EditText freqEditView;
    EditText nameEditView;
    TextView unitsEditView;
    TextView freqLabelTextView;
    Switch favoriteSwitch;

    // Use this instance of the interface to deliver action events
    NoticeListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.edit_station_dialog, null);

        freqLabelTextView = (TextView)view.findViewById(R.id.station_list_grid_item_dialog_label_freq);
        freqEditView = (EditText)view.findViewById(R.id.station_list_grid_item_dialog_freq);
        nameEditView = (EditText)view.findViewById(R.id.station_list_grid_item_dialog_name);
        unitsEditView = (TextView)view.findViewById(R.id.station_list_grid_item_dialog_units);
        favoriteSwitch = (Switch)view.findViewById(R.id.station_list_grid_item_dialog_favorite);

        // Build
        builder.setView(view);

        // Set title
        builder.setTitle(R.string.edit_station_dialog_title);

        // Buttons
        builder.setPositiveButton(R.string.alert_save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String name = nameEditView.getText().toString();
                int freq = Integer.valueOf(freqEditView.getText().toString());
                int freqRangeId = station.freqRangeId;
                boolean favorite = favoriteSwitch.isChecked() || range == "FAV";
                String uuid = station.uuid;

                freq = (freq > freqRange.maxFreq) ? freqRange.maxFreq : freq;
                freq = (freq < freqRange.minFreq) ? freqRange.minFreq : freq;

                if (name != null && (name.equals("") || name.equals(Tools.formatFrequencyValue(freq, units)))){
                    name = null;
                }

                Station station = new Station(name, freq, freqRangeId, uuid);
                station.setFavorite(favorite);
                mListener.onStationEditDialogSaveClick(StationEditDialog.this, index, station);
            }
        });

        builder.setNeutralButton(R.string.alert_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onStationEditDialogDeleteClick(StationEditDialog.this, index, station);
            }
        });

        builder.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onStationEditDialogCancelClick(StationEditDialog.this, index);
            }
        });

        // Hide fav toggle if it's Favorite list
        if (range == "FAV")
            favoriteSwitch.setVisibility(View.GONE);


        // Set initial values
        units = Tools.unitsByRangeId(station.freqRangeId);
        freqEditView.setText(String.valueOf(station.freq));
        nameEditView.requestFocus();
        nameEditView.setText((station.name == "" || station.name == null) ? Tools.formatFrequencyValue(station.freq, units) : station.name);
        unitsEditView.setText(units);
        freqLabelTextView.setText(getText(R.string.station_dialog_label_station_freq) + " (" + freqRange.minFreq + " - " + freqRange.maxFreq + ")");
        favoriteSwitch.setChecked(station.favorite);

        // Create the AlertDialog object and return it
        return builder.create();
    }

}