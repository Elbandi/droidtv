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

import android.util.AndroidRuntimeException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public final class FileUtils {
    static final String TAG = FileUtils.class.getName();

    public static final int S_IRWXU = 00700;
    public static final int S_IRUSR = 00400;
    public static final int S_IWUSR = 00200;
    public static final int S_IXUSR = 00100;

    public static final int S_IRWXG = 00070;
    public static final int S_IRGRP = 00040;
    public static final int S_IWGRP = 00020;
    public static final int S_IXGRP = 00010;

    public static final int S_IRWXO = 00007;
    public static final int S_IROTH = 00004;
    public static final int S_IWOTH = 00002;
    public static final int S_IXOTH = 00001;

    private static Method METHOD_FileUtils_setPermissions;

    static {
        try {
            Class<?> clazz;
            clazz = Class.forName("android.os.FileUtils");
            METHOD_FileUtils_setPermissions = clazz.getMethod("setPermissions",
                    String.class, int.class, int.class, int.class);
        } catch (Throwable t) {
            Log.wtf(TAG, t);
        }
    }

    private static Object invoke(Method method, Object receiver, Object... args) {
        try {
            return method.invoke(receiver, args);
        } catch (Exception e) {
            Log.e(TAG, null, e);
            throw new AndroidRuntimeException(e);
        }
    }

    /**
     * Copy data from a source stream to destFile. Return true if succeed,
     * return false if failed.
     */
    public static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static int setPermissions(String file, int mode) {
        return setPermissions(file, mode, -1, -1);
    }

    public static int setPermissions(String file, int mode, int uid, int gid) {
        return (Integer) invoke(METHOD_FileUtils_setPermissions, null, file,
                mode, uid, gid);
    }
}
