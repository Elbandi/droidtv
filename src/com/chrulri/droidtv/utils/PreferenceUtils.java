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

package com.chrulri.droidtv.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.chrulri.droidtv.PreferencesActivity;
import com.chrulri.droidtv.StreamActivity.DvbType;

public final class PreferenceUtils {
    static final String TAG = PreferenceUtils.class.getName();

    public static final String KEY_DVBTYPE = "dvbType";
    public static final String KEY_CHANNELCONFIGS = "channelConfigs";
    public static final String KEY_SCANCHANNELS = "scanChannels";

    public static SharedPreferences get(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx
                .getApplicationContext());
    }

    /***
     * @return read dvbType from preferences or return default (
     *         {@link DvbTuner.TYPE_DVBT})
     */
    public static DvbType getDvbType(Context ctx) {
        String dvbType = get(ctx).getString(KEY_DVBTYPE, null);
        return Enum.valueOf(DvbType.class, dvbType);
    }

    public static void openSettings(Context context) {
        Intent settingsActivity = new Intent(context, PreferencesActivity.class);
        context.startActivity(settingsActivity);
    }
}
