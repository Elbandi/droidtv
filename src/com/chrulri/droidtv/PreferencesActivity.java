/******************************************************************************
 *  DroidTV, live TV on Android devices with host USB port and a DVB tuner    *
 *  Copyright (C) 2012  Christian Ulrich <chrulri@gmail.com>                  *
 *                                                                            *
 *  This program is free software: you can redistribute it and/or modify      *
 *  it under the terms of the GNU General Public License as published by      *
 *  the Free Software Foundation, either version 3 of the License, or         *
 *  (at your option) any later version.                                       *
 *                                                                            *
 *  This program is distributed in the hope that it will be useful,           *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *  GNU General Public License for more details.                              *
 *                                                                            *
 *  You should have received a copy of the GNU General Public License         *
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
 ******************************************************************************/
package com.chrulri.droidtv;

import static com.chrulri.droidtv.Utils.Prefs.KEY_CHANNELCONFIGS;
import static com.chrulri.droidtv.Utils.Prefs.KEY_SCANCHANNELS;

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
