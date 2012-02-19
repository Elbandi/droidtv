package com.chrulri.droidtv;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.chrulri.droidtv.Utils.ProcessUtils;

public class StreamService extends Service {
	private static final String TAG = StreamService.class.getSimpleName();

	public static final String EXTRA_CHANNELCONFIG = "channelconfig";

	public enum DvbType {
		ATSC, DVBT,
	}

	static final String MUMUDVB_IP = "0.0.0.0";//"127.0.0.1";
	static final int MUMUDVB_PORT = 1234;
	static final Uri MUMUDVB_URI = Uri.parse("http://" + MUMUDVB_IP + ":"
			+ MUMUDVB_PORT + "/");

	static final String[] MUMUDVB_CONFIG_HEADER = { "autoconfiguration=0",
			"unicast=1", "multicast=0", "ip_http=" + MUMUDVB_IP,
			"port_http=" + MUMUDVB_PORT, "sap=0", "timeout_no_diff=0",
			"check_status=1", "log_type=console", "tuning_timeout=10", };

	private AsyncStreamTask asyncTask;

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "onStart");
		super.onStart(intent, startId);
		Bundle bundle = intent.getExtras();
		String channelConfig = bundle.getString(EXTRA_CHANNELCONFIG);
		DvbType dvbType = DvbType.DVBT;
		try {
			startMuMuDvb(dvbType, channelConfig);
		} catch (IOException e) {
			Log.e(TAG, "startMuMuDvb failed", e);
			sendError(e);
			stopSelf();
		}
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestory");
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void startMuMuDvb(DvbType dvbType, String channelConfig)
			throws IOException {
		Log.d(TAG, "startMuMuDvb(" + channelConfig + ")");
		File configFile = new File(getCacheDir(), "mumudvb.conf");
		PrintWriter writer = new PrintWriter(configFile);
		for (String headerLine : MUMUDVB_CONFIG_HEADER) {
			writer.println(headerLine);
		}
		switch (dvbType) {
		case ATSC:
			DvbUtils.parseATSC(channelConfig, writer);
			break;
		case DVBT:
			DvbUtils.parseDVBT(channelConfig, writer);
			break;
		}
		writer.close();
		runMuMuDvb(configFile);
	}

	private void runMuMuDvb(File configFile) throws IOException {
		Log.d(TAG, "runMuMuDvb(" + configFile + ")");

		asyncTask = new AsyncStreamTask();
		asyncTask.execute(configFile);
	}

	class AsyncStreamTask extends AsyncTask<File, CharSequence, Void> {

		@Override
		protected void onCancelled() {
			sendUpdates(null);
			stopSelf();
		}

		@Override
		protected void onProgressUpdate(CharSequence... values) {
			if (isCancelled())
				return;
			sendUpdates(values);
		}

		@Override
		protected void onPostExecute(Void result) {
			sendUpdates(null);
			stopSelf();
		}

		@Override
		protected Void doInBackground(File... params) {
			File configFile = params[0];
			try {
				Process mumudvb = Utils.runBinary(StreamService.this,
						R.raw.mumudvb_1_6_1b, "-d", "-s", "-t", "-vvvvvv",
						"-c", configFile.toString());
				Reader input = new InputStreamReader(mumudvb.getErrorStream());
				BufferedReader reader = new BufferedReader(input);
				Integer exitCode = null;
				String line;
				while (!isCancelled()
						&& ((line = reader.readLine()) != null || (exitCode = ProcessUtils
								.checkExitCode(mumudvb)) == null)) {
					if (line == null) {
						Thread.sleep(250);
					} else {
						Log.d(TAG, "AsyncStreamTask: " + line);
						publishProgress(line);
					}
				}
				if (exitCode == null) {
					mumudvb.destroy();
				} else if (exitCode != 0) {
					// TODO localization
					Log.e(TAG, "mumudvb failed (" + exitCode + ")");
					Log.d(TAG, ProcessUtils.readStdOut(mumudvb));
					Log.d(TAG, ProcessUtils.readErrOut(mumudvb));
					sendError("wscan failed (" + exitCode + ")");
				}
			} catch (Throwable t) {
				Log.e(TAG, "mumudvb", t);
				sendError("wscan", t);
			}
			return null;
		}
	}

	private void sendError(String message) {
		Intent intent = new Intent(ChannelsActivity.ACTION_ERROR);
		intent.putExtra(ChannelsActivity.EXTRA_ERRORMSG, message);
		sendBroadcast(intent);
	}

	private void sendError(String message, Throwable t) {
		Intent intent = new Intent(ChannelsActivity.ACTION_ERROR);
		intent.putExtra(ChannelsActivity.EXTRA_ERRORMSG, message);
		intent.putExtra(ChannelsActivity.EXTRA_ERROR, t);
		sendBroadcast(intent);
	}

	private void sendError(Throwable t) {
		Intent intent = new Intent(ChannelsActivity.ACTION_ERROR);
		intent.putExtra(ChannelsActivity.EXTRA_ERROR, t);
		sendBroadcast(intent);
	}

	private void sendUpdates(CharSequence[] values) {
		Intent intent = new Intent(ChannelsActivity.ACTION_UPDATE);
		intent.putExtra(ChannelsActivity.EXTRA_UPDATES, values);
		sendBroadcast(intent);
	}
}
