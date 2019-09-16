package com.clacksdepartment.hype;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        Preference button = findPreference("pref_db");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Drop DB
                SQLiteDatabase db = (new FeedReaderDbHelper(getActivity().getApplicationContext())).getWritableDatabase();
                db.delete(FeedReaderContract.FeedEntryReleases.TABLE_NAME, null, null);

                // Change pref_db to notify mModifiedListAdapter
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                boolean value = prefs.getBoolean("pref_db",false);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("pref_db", !value);
                editor.apply();

                return true;
            }
        });

    }
}