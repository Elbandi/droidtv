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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.chrulri.droidtv.StreamActivity.DvbType;
import com.chrulri.droidtv.utils.ParallelTask;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class Utils {
    static final String TAG = Utils.class.getName();

    static final int BUFFER_SIZE = 10240;

    public static final String NEWLINE = System.getProperty("line.separator");

    private static Method METHOD_FileUtils_setPermissions;

    static {
        try {
            Class<?> clazz;
            clazz = Class.forName("android.os.FileUtils");
            METHOD_FileUtils_setPermissions = clazz.getMethod("setPermissions",
                    String.class, int.class, int.class, int.class);
        } catch (Exception e) {
            Log.wtf(TAG, e);
        }
    }

    private static Object invoke(Method method, Object receiver, Object... args) {
        try {
            return method.invoke(receiver, args);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, null, e);
            throw new AndroidRuntimeException(e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, null, e);
            throw new AndroidRuntimeException(e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, null, e);
            throw new AndroidRuntimeException(e);
        }
    }

    public static String getStackTrace(final Throwable t) {
        if (t == null)
            return "null";
        final StringWriter buf = new StringWriter();
        final PrintWriter writer = new PrintWriter(buf);
        t.printStackTrace(writer);
        return buf.toString();
    }

    public static class FileUtils {

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

    public static class ProcessUtils {
        public static Process run(String executable, String... args) throws IOException {
            return run(executable, null, null, args);
        }

        public static Process run(String executable, File workingDirectory, String[] envp,
                String... args) throws IOException {
            if (args.length > 0) {
                String[] pargs = new String[args.length + 1];
                System.arraycopy(args, 0, pargs, 1, args.length);
                pargs[0] = executable;
                args = pargs;
            } else {
                args = new String[] { executable };
            }
            return Runtime.getRuntime().exec(args, envp, workingDirectory);
        }

        public static Process runAsRoot(String... args) throws IOException {
            if (args == null || args.length == 0)
                return null;
            Process su = run("su");
            DataOutputStream out = new DataOutputStream(su.getOutputStream());
            for (String arg : args) {
                out.writeBytes(" ");
                out.writeBytes(arg);
            }
            out.writeBytes("\n");
            out.writeBytes("exit\n");
            out.flush();
            return su;
        }

        public static String readStdOut(Process proc) throws IOException {
            InputStream pin = proc.getInputStream();
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
            }
            return readAll(pin);
        }

        public static String readErrOut(Process proc) throws IOException {
            InputStream pin = proc.getErrorStream();
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
            }
            return readAll(pin);
        }

        public static void printLine(Process proc, String line) throws IOException {
            OutputStream pout = proc.getOutputStream();
            Writer out = new OutputStreamWriter(pout);
            out.write(line);
            out.write(NEWLINE);
            out.flush();
        }

        public static Integer checkExitCode(Process proc) {
            try {
                return proc.exitValue();
            } catch (IllegalThreadStateException e) {
                return null;
            }
        }

        public static void terminate(Process proc) {
            try {
                Class<?> cls = proc.getClass();
                Field fid;
                try {
                    fid = cls.getDeclaredField("id");
                } catch (NoSuchFieldException e) {
                    // ICS-compatible
                    fid = cls.getDeclaredField("pid");
                }
                fid.setAccessible(true);
                int pid = (Integer) fid.get(proc);
                terminate(pid);
            } catch (Throwable t) {
                Log.wtf(TAG, "terminate", t);
            }
        }

        public static void terminate(int pid) {
            try {
                Log.d(TAG, "terminate(" + pid + ")");
                run("kill", "" + pid);
                while (checkPid(pid)) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // nop
                    }
                }
            } catch (Throwable t) {
                Log.wtf(TAG, "terminate", t);
            }
        }

        public static boolean checkPid(int pid) {
            return new File("/proc", "" + pid).exists();
        }

        public static void finishTask(ParallelTask task, boolean interrupt) {
            if (task == null) {
                return;
            }
            // cancel task and wait for it to finish
            task.cancel(interrupt);
            while (task.getState() != Thread.State.TERMINATED) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // nop
                }
            }
        }

        private static File getBinaryExecutableFile(Context ctx, int rawId) {
            return new File(ctx.getCacheDir(), Integer.toHexString(rawId) + ".bin");
        }

        public static Process runBinary(Context ctx, int rawId, String... args)
                throws IOException {
            return runBinary(ctx, rawId, null, args);
        }

        public static Process runBinary(Context ctx, int rawId, String[] envp, String... args)
                throws IOException {
            ProcessUtils.killBinary(ctx, rawId);
            File bin = getBinaryExecutableFile(ctx, rawId);
            if (!FileUtils.copyToFile(ctx.getResources().openRawResource(rawId), bin))
                throw new IOException("copying file failed");
            if (FileUtils.setPermissions(bin.toString(), FileUtils.S_IRWXU) != 0)
                throw new IOException("setting permission failed");
            return ProcessUtils.run(bin.toString(), ctx.getCacheDir(), envp, args);
        }

        public static void killBinary(Context ctx, int rawId) throws IOException {
            File exe = getBinaryExecutableFile(ctx, rawId);
            String exePath = exe.getCanonicalPath();
            File[] procs = new File("/proc").listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    try {
                        Integer.parseInt(filename);
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            });
            for (File proc : procs) {
                File pexe = new File(proc, "exe");
                if (pexe.canRead()) {
                    if (exePath.equals(pexe.getCanonicalPath())) {
                        int pid = Integer.parseInt(proc.getName());
                        terminate(pid);
                    }
                }
            }
        }
    }

    public static class Prefs {
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
    }

    public static class StringUtils {
        public static boolean isNullOrEmpty(String str) {
            return str == null || str.trim().length() == 0;
        }
    }

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

    private static class ErrorDialog extends Dialog {

        public ErrorDialog(Context context, CharSequence msg, Throwable t,
                final View.OnClickListener onClickListener) {
            super(context);
            setContentView(R.layout.error);
            StringBuilder buf = new StringBuilder();
            buf.append(msg);
            buf.append(NEWLINE);
            if (t != null) {
                buf.append(getStackTrace(t));
            }
            TextView textView = (TextView) findViewById(R.id.error_textView);
            textView.setText(buf.toString());
            Button button = (Button) findViewById(R.id.error_reportButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    if (onClickListener != null) {
                        onClickListener.onClick(v);
                    }
                }
            });
        }
    }

    public static void error(Activity ctx, CharSequence msg) {
        error(ctx, msg, null, null);
    }

    public static void error(Activity ctx, CharSequence msg, Throwable t) {
        error(ctx, msg, t, null);
    }

    public static void error(Activity ctx, CharSequence msg, OnClickListener onClickListener) {
        error(ctx, msg, null, onClickListener);
    }

    public static void error(final Activity ctx, final CharSequence msg,
            final Throwable t, final OnClickListener onClickListener) {
        ctx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ErrorDialog dlg = new ErrorDialog(ctx, msg, t, onClickListener);
                dlg.show();
            }
        });
    }

    public static void openSettings(Context context) {
        Intent settingsActivity = new Intent(context, PreferencesActivity.class);
        context.startActivity(settingsActivity);
    }
}
