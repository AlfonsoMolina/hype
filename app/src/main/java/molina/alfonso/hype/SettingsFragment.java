package molina.alfonso.hype;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

/**
 * Created by Usuario on 31/07/2017.
 */

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
                //Se vacía la bbdd
                SQLiteDatabase db = (new FeedReaderDbHelper(getActivity().getApplicationContext())).getWritableDatabase();
                db.delete(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, null, null);
                db.delete(FeedReaderContract.FeedEntryCartelera.TABLE_NAME, null, null);

                //Se cambia el valor para notificar a la lista
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                Boolean valor = prefs.getBoolean("pref_db",false);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("pref_db", !valor);
                editor.apply();

                return true;
            }
        });
    }
}