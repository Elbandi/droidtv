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

/**
 * MuMuDVB wrapper service <br/>
 * <br/>
 * {@link http://mumudvb.braice.net/mumudvb/doc/mumudvb-1.7/README.html}
 * {@link http://mumudvb.braice.net/mumudvb/doc/mumudvb-1.7/README_CONF.html}
 */
public class StreamService extends Service {
  private static final String TAG = StreamService.class.getSimpleName();

  static final int MUMUDVB = R.raw.mumudvb_1_7;

  public static final String EXTRA_CHANNELCONFIG = "channelconfig";

  public enum DvbType {
    ATSC, DVBT,
  }

  static final String MUMUDVB_IP = "0.0.0.0";// "127.0.0.1";
  static final int MUMUDVB_PORT = 4242;
  static final String MUMUDVB_CONFIG_FILENAME = "mumudvb.conf";
  static final String[] MUMUDVB_CONFIG_HEADER = {
      "autoconfiguration=full",
      "unicast=1",
      "multicast=0",
      "ip_http=" + MUMUDVB_IP,
      "port_http=" + MUMUDVB_PORT,
      "sap=0",
      "timeout_no_diff=0",
      "check_status=1",
      "log_type=console",
      //"tuning_timeout=10",
  };

  public static Uri getServiceUri(int serviceId) {
    return Uri.parse(String.format("http://localhost:%i/bysid/%i",
        MUMUDVB_PORT, serviceId));
  }

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
    Log.d(TAG, "onDestroy");
    asyncTask.cancel(false);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void startMuMuDvb(DvbType dvbType, String channelConfig)
      throws IOException {
    Log.d(TAG, "startMuMuDvb(" + channelConfig + ")");
    File configFile = new File(getCacheDir(), MUMUDVB_CONFIG_FILENAME);
    PrintWriter writer = new PrintWriter(configFile);
    for (String headerLine : MUMUDVB_CONFIG_HEADER) {
      writer.println(headerLine);
    }
    int sid;
    switch (dvbType) {
    case ATSC:
    case DVBT:
      sid = DvbUtils.parse(channelConfig, writer);
      break;
    default:
      Log.wtf(TAG, "unknown dvbType[" + dvbType + "]");
      return;
    }
    writer.close();
    runMuMuDvb(configFile, sid);
  }

  private void runMuMuDvb(File configFile, int sid) throws IOException {
    Log.d(TAG, "runMuMuDvb(" + configFile + "," + sid + ")");

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
        Process mumudvb = Utils.runBinary(StreamService.this, MUMUDVB, "-d",
            "-s", "-t", "-c", configFile.toString()
        // , "-vvvvvvvvv"
            );
        Reader input = new InputStreamReader(mumudvb.getErrorStream());
        BufferedReader reader = new BufferedReader(input);
        Integer exitCode = null;
        String line;
        while (!isCancelled() && ((line = reader.readLine()) != null || (exitCode = ProcessUtils
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
          Log.d(TAG, "mumudvb destroying");
          mumudvb.waitFor();
          Log.d(TAG, "mumudvb destroyed");
        } else if (exitCode != 0) {
          // TODO localization
          Log.e(TAG, "mumudvb failed (" + exitCode + ")");
          Log.d(TAG, ProcessUtils.readStdOut(mumudvb));
          Log.d(TAG, ProcessUtils.readErrOut(mumudvb));
          sendError("mumudvb failed (" + exitCode + ")");
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
