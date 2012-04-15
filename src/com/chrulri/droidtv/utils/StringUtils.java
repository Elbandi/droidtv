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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

public final class StringUtils {
    static final String TAG = StringUtils.class.getName();

    static final int BUFFER_SIZE = 10240;

    public static final String NEWLINE = System.getProperty("line.separator");

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static String readAll(InputStream in) throws IOException {
        Writer out = new StringWriter();
        char[] buf = new char[BUFFER_SIZE];
        int todo = in.available();
        Reader reader = new InputStreamReader(in);
        int len;
        if (todo == 0) {
            // nop
        } else if (todo < 0) {
            while ((len = reader.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        } else {
            for (int read = 0; todo > read
                    && (len = reader.read(buf, 0, Math.min(todo - read, buf.length))) != -1; read += len) {
                out.write(buf, 0, len);
            }
        }
        return out.toString();
    }

    public static String getStackTrace(final Throwable t) {
        if (t == null)
            return "null";
        final StringWriter buf = new StringWriter();
        final PrintWriter writer = new PrintWriter(buf);
        t.printStackTrace(writer);
        return buf.toString();
    }
}
