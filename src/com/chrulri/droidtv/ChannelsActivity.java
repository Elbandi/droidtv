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
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.chrulri.droidtv.Utils.Prefs;
import com.chrulri.droidtv.Utils.ProcessUtils;
import com.chrulri.droidtv.Utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChannelsActivity extends Activity {
    private static final String TAG = ChannelsActivity.class.getSimpleName();
    private static final String PKG = ChannelsActivity.class.getPackage()
            .getName();

    public static final String ACTION_ERROR = PKG + ".ACTION_ERROR";
    public static final String ACTION_UPDATE = PKG + ".ACTION_UPDATE";

    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_UPDATES = "updates";
    public static final String EXTRA_ERRORMSG = "errormsg";

    private Spinner mSpinner;
    private ListView mListView;
    private String[] mChannelConfigs;
    private String[] mChannels;
    private AlertDialog mStreamingDialog;
    private StreamService.LocalBinder mServiceBinder;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected(" + name + ")");
            mServiceBinder = null;
            stopStreaming();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected(" + name + "," + service + ")");
            mServiceBinder = (StreamService.LocalBinder) service;
        }
    };

    // ************************************************************** //
    // * ACTIVITY *************************************************** //
    // ************************************************************** //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new CheckTask().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindStreamService();
    }

    private void onCreateImpl() {
        setContentView(R.layout.channels);

        bindStreamService();

        mStreamingDialog = new AlertDialog.Builder(this).setTitle(R.string.app_name)
                // FIXME localization
                .setMessage("streaming")
                .setNegativeButton("Cancel", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopStreaming();
                    }
                }).create();

        mSpinner = (Spinner) findViewById(R.id.spinner);
        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                loadChannels((String) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                loadChannels(null);
            }
        });

        mListView = (ListView) findViewById(R.id.listView);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                watchChannel((int) id);
            }
        });

        // start
        loadChannelLists();
        setChannelList(null);
        if (mSpinner.getAdapter().getCount() == 0) {
            Utils.openSettings(this);
        }
    }

    // ************************************************************** //
    // * STREAMING SERVICE ****************************************** //
    // ************************************************************** //

    private void bindStreamService() {
        bindService(new Intent(this, StreamService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindStreamService() {
        unbindService(mServiceConnection);
    }

    private void startStreaming(final String channelconfig) {
        final StreamService svc = mServiceBinder.getService();
        if (svc == null) {
            Log.d(TAG, "startStreaming without service attached");
            return;
        }
        String channelname = svc.startStream(channelconfig);
        setupStreamingGui(channelname);
    }

    private void stopStreaming() {
        final StreamService svc = mServiceBinder.getService();
        if (svc != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    svc.stopStream();
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    cleanupStreamingGui();
                };
            }.execute();
        }
        else {
            cleanupStreamingGui();
        }
    }

    // ************************************************************** //
    // * STREAMING GUI ********************************************** //
    // ************************************************************** //

    private void setupStreamingGui(String channelname) {
        // TODO set up GUI
        mStreamingDialog.show();
    }

    private void cleanupStreamingGui() {
        // TODO clean up GUI
        mStreamingDialog.dismiss();
    }

    // ************************************************************** //
    // * CHANNEL UTILS ********************************************** //
    // ************************************************************** //

    private void loadChannelLists() {
        // reset channel lists spinner
        mSpinner.setAdapter(null);
        mListView.setAdapter(null);

        // load channel lists
        String[] configFiles = Utils.getConfigsDir(this).list();
        mSpinner.setAdapter(Utils.createSimpleArrayAdapter(this, configFiles));
    }

    private void loadChannels(String channelListName) {
        // reset channel list view
        mListView.setAdapter(null);

        // check channel list
        File file = Utils.getConfigsFile(this, channelListName);
        if (file == null || !file.canRead() || !file.isFile()) {
            return;
        }

        // load channels
        mChannelConfigs = null;
        mChannels = null;
        try {
            List<String> channelConfigList = new ArrayList<String>();
            List<String> channelList = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                channelConfigList.add(line);
                channelList.add(line.split(":")[0]);
            }
            reader.close();
            mChannelConfigs = channelConfigList.toArray(new String[channelConfigList
                    .size()]);
            mChannels = channelList.toArray(new String[channelList.size()]);
        } catch (IOException e) {
            Utils.error(this, getText(R.string.error_invalid_channel_configuration), e);
        }

        // update channel list view
        mListView.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item,
                mChannels));
    }

    /**
     * @param channelList list name, insert null to read config from settings
     */
    private void setChannelList(String channelList) {
        if (channelList == null) {
            channelList = Prefs.get(this).getString(Prefs.KEY_CHANNELCONFIGS, null);
        }

        if (StringUtils.isNullOrEmpty(channelList) || Utils.getConfigsFile(this,
                channelList) == null) {
            String[] files = Utils.getConfigsDir(this).list();
            if (files.length == 1) {
                channelList = files[0];
            } else {
                channelList = null;
            }
        }

        if (channelList == null) {
            mSpinner.setSelection(AdapterView.INVALID_POSITION);
        } else {
            for (int i = 0; i < mSpinner.getAdapter().getCount(); i++) {
                if (channelList.equals(mSpinner.getAdapter().getItem(i))) {
                    mSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void watchChannel(int channelId) {
        Log.d(TAG, "watchChannel(" + channelId + "): " + mChannels[channelId]);
        String channelconfig = mChannelConfigs[channelId];
        startStreaming(channelconfig);
    }

    // ************************************************************** //
    // * INITIALIZATION TASK **************************************** //
    // ************************************************************** //

    private class CheckTask extends AsyncTask<Void, Integer, Integer> {

        static final int OK = 0;
        static final int CHECK_DEVICE = R.string.no_device;

        static final String FILE_DEV_DVB_ADAPTER = "/dev/dvb/adapter0";
        static final String FILE_DVB_CA = FILE_DEV_DVB_ADAPTER + "/ca0";
        static final String FILE_DVB_FRONTEND = FILE_DEV_DVB_ADAPTER + "/frontend0";
        static final String FILE_DVB_DEMUX = FILE_DEV_DVB_ADAPTER + "/demux0";
        static final String FILE_DVB_DVR = FILE_DEV_DVB_ADAPTER + "/dvr0";

        private boolean checkDeviceNode(String file, boolean checkExists) {
            File f = new File(file);
            if (checkExists) {
                return f.exists() && f.canRead() && f.canWrite();
            } else {
                return !f.exists() || f.exists() && f.canRead() && f.canWrite();
            }
        }

        private boolean checkDevice() {
            return checkDeviceNode(FILE_DVB_FRONTEND, true)
                    && checkDeviceNode(FILE_DVB_DEMUX, true)
                    && checkDeviceNode(FILE_DVB_DVR, true)
                    && checkDeviceNode(FILE_DVB_CA, false);
        }

        @Override
        protected void onPreExecute() {
            setContentView(R.layout.loading);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // kill old instance if still running
            try {
                ProcessUtils.killBinary(getApplicationContext(), StreamService.DVBLAST);
            } catch (IOException e) {
                Log.w(TAG, "kill dvblast", e);
            }
            // check for device access
            publishProgress(CHECK_DEVICE);
            if (!checkDevice())
                return CHECK_DEVICE;
            // everything fine
            return OK;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int value = values[0];
            TextView loading = (TextView) findViewById(R.id.loading);
            // update text
            switch (value) {
                case CHECK_DEVICE:
                    loading.setText(R.string.check_device);
                    break;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == OK) {
                onCreateImpl();
            } else {
                // show alert and abort application
                Utils.error(ChannelsActivity.this, getText(result), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            }
        }
    }
}
