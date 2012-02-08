package chrulri.livetv;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

class Utils {
	static final String TAG = Utils.class.getName();

	static final int BUFFER_SIZE = 10240;

	public static final String NEWLINE = System.getProperty("line.separator");

	private static Method METHOD_FileUtils_copyFile;
	private static Method METHOD_FileUtils_copyToFile;
	private static Method METHOD_FileUtils_setPermissions;

	static {
		try {
			Class<?> clazz;
			clazz = Class.forName("android.os.FileUtils");
			METHOD_FileUtils_copyFile = clazz.getMethod("copyFile", File.class,
					File.class);
			METHOD_FileUtils_copyToFile = clazz.getMethod("copyToFile",
					InputStream.class, File.class);
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

		public static boolean copyFile(File srcFile, File destFile) {
			return (Boolean) invoke(METHOD_FileUtils_copyFile, null, srcFile,
					destFile);
		}

		public static boolean copyToFile(InputStream inputStream, File destFile) {
			return (Boolean) invoke(METHOD_FileUtils_copyToFile, null, inputStream,
					destFile);
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

		public static Process run(String executable, String... args)
				throws IOException {
			if (args.length > 0) {
				String[] pargs = new String[args.length + 1];
				pargs[0] = executable;
				for (int i = 0; i < args.length; i++)
					pargs[i + 1] = args[i];
				args = pargs;
			}
			return Runtime.getRuntime().exec(args);
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
	}

	public static class Prefs {
		public static final String KEY_PLAYLASTCHANNELONSTARTUP = "playLastChannelOnStartup";
		public static final String KEY_DVBTYPE = "dvbType";
		public static final String KEY_CHANNELCONFIGS = "channelConfigs";
		public static final String KEY_SCANCHANNELS = "scanChannels";

		public static SharedPreferences get(Context ctx) {
			return PreferenceManager.getDefaultSharedPreferences(ctx
					.getApplicationContext());
		}

		/***
		 * @return read dvbType from preferences or return default ({@link DvbTuner.TYPE_DVBT})
		 */
		public static int getDvbType(Context ctx) {
			String dvbType = get(ctx).getString(KEY_DVBTYPE, null);
			return parseDvbType(ctx, dvbType);
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

	public static Process runBinary(Context ctx, int rawId, String... args)
			throws IOException {
		File bin = new File(ctx.getCacheDir(), Integer.toHexString(rawId) + ".bin");
		bin.deleteOnExit();
		if (!FileUtils.copyToFile(ctx.getResources().openRawResource(rawId), bin))
			throw new IOException("copying file failed");
		if (FileUtils.setPermissions(bin.toString(), FileUtils.S_IRWXU) != 0)
			throw new IOException("setting permission failed");
		return ProcessUtils.run(bin.toString(), args);
	}

	public static int parseDvbType(Context ctx, String dvbType) {
		if ("atsc".equals(dvbType))
			return DvbTuner.TYPE_ATSC;
		if ("dvbt".equals(dvbType))
			return DvbTuner.TYPE_DVBT;
		return DvbTuner.TYPE_DVBT;
	}

	public static File getConfigsDir(Context ctx) {
		return ctx.getDir("configs", 0);
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
		try {
			Writer out = new StringWriter();
			char[] buf = new char[BUFFER_SIZE];
			Reader reader = new InputStreamReader(in);
			int len;
			while ((len = reader.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
			return out.toString();
		} finally {
			in.close();
		}
	}

	private static class ErrorDialog extends Dialog {

		public ErrorDialog(Context context, CharSequence msg, Throwable t) {
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
		}
	}

	public static void error(Activity ctx, CharSequence msg) {
		error(ctx, msg, null);
	}

	public static void error(final Activity ctx, final CharSequence msg,
			final Throwable t) {
		ctx.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ErrorDialog dlg = new ErrorDialog(ctx, msg, t);
				dlg.show();
			}
		});
	}
}
