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

import java.util.UUID;

public class StationAddDialog extends DialogFragment {

    public interface NoticeListener {
        public void onStationAddDialogSaveClick(DialogFragment dialog, Station station);
        public void onStationAddDialogCancelClick(DialogFragment dialog);
    }

    EditText freqEditView;
    EditText nameEditView;
    TextView unitsEditView;
    TextView freqLabelTextView;
    Switch favoriteSwitch;

    public Station station;
    public FreqRange freqRange;

    NoticeListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (NoticeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement NoticeListener");
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
        View view = inflater.inflate(R.layout.add_station_dialog, null);

        freqLabelTextView = (TextView)view.findViewById(R.id.station_list_grid_item_dialog_label_freq);
        freqEditView = (EditText)view.findViewById(R.id.station_list_grid_item_dialog_freq);
        nameEditView = (EditText)view.findViewById(R.id.station_list_grid_item_dialog_name);
        unitsEditView = (TextView)view.findViewById(R.id.station_list_grid_item_dialog_units);
        favoriteSwitch = (Switch)view.findViewById(R.id.station_list_grid_item_dialog_favorite);

        //
        // Initial values
        //

        // Frequency
        int freq = Tools.testFrequency(station.freq,freqRange);
        freqEditView.setText(String.valueOf(freq));

        // Units
        String units = freqRange.units;
        unitsEditView.setText(units);
        unitsEditView.setText(units);

        // Station name
        nameEditView.requestFocus();
        nameEditView.setText((station.name == "" || station.name == null) ? Tools.formatFrequencyValue(station.freq, units) : station.name);

        // Frequency label text
        freqLabelTextView.setText(getText(R.string.station_dialog_label_station_freq) + " (" + freqRange.minFreq + " - " + freqRange.maxFreq + ")");

        // Build
        builder.setView(view);

        // Set title
        builder.setTitle(R.string.add_station_dialog_title);

        // Buttons
        builder.setPositiveButton(R.string.alert_save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String name = nameEditView.getText().toString();

                int freqRangeId = station.freqRangeId;
                boolean favorite = favoriteSwitch.isChecked();
                String uuid = UUID.randomUUID().toString();
                String units = freqRange.units;
                int freq = Integer.valueOf(freqEditView.getText().toString());

                if (name != null && (name.equals("") || name.equals(Tools.formatFrequencyValue(freq, units)))) {
                    name = null;
                }

                Station station = new Station(name, freq, freqRangeId, uuid);
                station.setFavorite(favorite);
                mListener.onStationAddDialogSaveClick(StationAddDialog.this, station);
            }
        });

        builder.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onStationAddDialogCancelClick(StationAddDialog.this);
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }


}
