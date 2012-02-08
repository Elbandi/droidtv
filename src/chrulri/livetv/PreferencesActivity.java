package chrulri.livetv;

import static chrulri.livetv.Utils.Prefs.KEY_CHANNELCONFIGS;
import static chrulri.livetv.Utils.Prefs.KEY_SCANCHANNELS;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
		// setup scan button
		Preference scanButton = findPreference(KEY_SCANCHANNELS);
		scanButton.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent settingsActivity = new Intent(getBaseContext(),
						ScanActivity.class);
				startActivity(settingsActivity);
				return true;
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		// read channel configs from filesystem
		ListPreference channelConfigs = (ListPreference) findPreference(KEY_CHANNELCONFIGS);
		File configsDir = Utils.getConfigsDir(this);
		String[] configFiles = configsDir.list();
		String[] configs = new String[configFiles.length];
		for (int i = 0; i < configFiles.length; i++) {
			String fileName = configFiles[i];
			int pos = fileName.lastIndexOf(".conf");
			configs[i] = pos == -1 ? fileName : fileName.substring(0, pos);
		}
		channelConfigs.setEntries(configs);
		channelConfigs.setEntryValues(configFiles);
	}
}
