package molina.alfonso.hype;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Usuario on 31/07/2017.
 */

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction()
                .replace(R.id.fragmento, new SettingsFragment())
                .commit();


    }
}
