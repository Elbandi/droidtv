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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import java.io.File;

public final class Utils {
    static final String TAG = Utils.class.getName();

    public static BaseAdapter createSimpleArrayAdapter(Context ctx, String[] array) {
        ArrayAdapter<String> ad = new ArrayAdapter<String>(ctx,
                android.R.layout.simple_spinner_item, array);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return ad;
    }

    public static File getConfigsDir(Context ctx) {
        return ctx.getDir("configs", Context.MODE_PRIVATE);
    }

    public static File getConfigsFile(Context ctx, String fileName) {
        if (fileName == null || fileName.trim().length() == 0)
            return null;
        return new File(getConfigsDir(ctx), fileName);
    }

    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null)
            return true;
        if (obj1 == null || obj2 == null)
            return false;
        return obj1.equals(obj2);
    }

    public static Object decode(Object obj, Object... vars) {
        if (vars.length == 0)
            throw new IllegalArgumentException();
        else if (vars.length == 1)
            return vars[0];
        else {
            int i;
            for (i = 0; i + 1 < vars.length; i += 2) {
                if (equals(obj, vars[i]))
                    return vars[i + 1];
            }
            return (i < vars.length) ? vars[i] : null;
        }
    }
}
