package molina.alfonso.hype;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by Usuario on 31/07/2017.
 */

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.fragmento, new SettingsFragment())
                .commit();


    }
}
