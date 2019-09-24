package com.clacksdepartment.hype;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragmentCompat  {
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the Preferences from the XML file
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preference button = findPreference("pref_db");
        if (button != null)
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(preference.getContext())
                            .setTitle(getResources().getString(R.string.dialog_db_title))
                            .setMessage(getResources().getString(R.string.dialog_db_text))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Drop DB
                                    SQLiteDatabase db;
                                    db = (new FeedReaderDbHelper(getActivity().getApplicationContext())).getWritableDatabase();
                                    db.delete(FeedReaderContract.FeedEntryReleases.TABLE_NAME, null, null);

                                    // Change pref_db to notify mModifiedListAdapter
                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                                    boolean value = prefs.getBoolean("pref_db",false);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean("pref_db", !value);
                                    editor.apply();

                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                    return true;
                }
            });

    }
}