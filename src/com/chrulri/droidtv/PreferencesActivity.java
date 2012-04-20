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

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.chrulri.droidtv.utils.PreferenceUtils;
import com.chrulri.droidtv.utils.PreferenceUtils.DvbType;

public class PreferencesActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        // setup scan button
        Preference scanButton = findPreference(PreferenceUtils.KEY_SCANCHANNELS);
        scanButton.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent settingsActivity = new Intent(getBaseContext(),
                        ScanActivity.class);
                startActivity(settingsActivity);
                return true;
            }
        });
        ListPreference dvbtypesList = (ListPreference)findPreference(PreferenceUtils.KEY_DVBTYPE);
        dvbtypesList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference arg0, Object arg1) {
                PreferenceUtils.setDvbType(PreferencesActivity.this, Enum.valueOf(DvbType.class, (String)arg1));
                return false;
            }
        });
        dvbtypesList.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                ((ListPreference)arg0).setDefaultValue(PreferenceUtils.getDvbType(PreferencesActivity.this).name().toUpperCase());
                return false;
            }
        });
    }
}
