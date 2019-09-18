package com.clacksdepartment.hype;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
                    // Drop DB
                    SQLiteDatabase db;
                    if (getActivity() != null){
                        db = (new FeedReaderDbHelper(getActivity().getApplicationContext())).getWritableDatabase();
                        db.delete(FeedReaderContract.FeedEntryReleases.TABLE_NAME, null, null);

                        // Change pref_db to notify mModifiedListAdapter
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                        boolean value = prefs.getBoolean("pref_db",false);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("pref_db", !value);
                        editor.apply();

                        return true;
                    } else {
                        return false;
                    }
                }
            });

    }
}