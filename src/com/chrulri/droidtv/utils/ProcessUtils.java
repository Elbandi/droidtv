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
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;

public final class ProcessUtils {
    static final String TAG = ProcessUtils.class.getName();

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
            args = new String[] {
                    executable
            };
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
        return StringUtils.readAll(pin);
    }

    public static String readErrOut(Process proc) throws IOException {
        InputStream pin = proc.getErrorStream();
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
        }
        return StringUtils.readAll(pin);
    }

    public static void printLine(Process proc, String line) throws IOException {
        OutputStream pout = proc.getOutputStream();
        Writer out = new OutputStreamWriter(pout);
        out.write(line);
        out.write(StringUtils.NEWLINE);
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
