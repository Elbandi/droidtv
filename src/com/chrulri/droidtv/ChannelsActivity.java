package com.chrulri.droidtv;

import static com.chrulri.droidtv.Utils.error;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.chrulri.droidtv.Utils.Prefs;
import com.chrulri.droidtv.Utils.StringUtils;

public class ChannelsActivity extends Activity {
	private static final String TAG = ChannelsActivity.class.getSimpleName();
	private static final String PKG = ChannelsActivity.class.getPackage().getName();

	public static final String ACTION_ERROR = PKG + ".ACTION_ERROR";
	public static final String ACTION_UPDATE = PKG + ".ACTION_UPDATE";

	public static final String EXTRA_ERROR = "error";
	public static final String EXTRA_UPDATES = "updates";
	public static final String EXTRA_ERRORMSG = "errormsg";

	private Spinner spinner;
	private ListView listView;
	private String[] channelConfigs;
	private String[] channels;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.channels);

		spinner = (Spinner) findViewById(R.id.spinner);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
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

		listView = (ListView) findViewById(R.id.listView);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				watchChannel((int) id);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		loadChannelLists();
		setChannelList(null);
		if (spinner.getAdapter().getCount() == 0) {
			Utils.openSettings(this);
		}
	}

	private void loadChannelLists() {
		// reset channel lists spinner
		spinner.setAdapter(null);
		listView.setAdapter(null);

		// load channel lists
		String[] configFiles = Utils.getConfigsDir(this).list();
		spinner.setAdapter(Utils.createSimpleArrayAdapter(this, configFiles));
	}

	private void loadChannels(String channelListName) {
		// reset channel list view
		listView.setAdapter(null);

		// check channel list
		File file = Utils.getConfigsFile(this, channelListName);
		if (file == null || !file.canRead() || !file.isFile()) {
			return;
		}

		// load channels
		channelConfigs = null;
		channels = null;
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
			channelConfigs = channelConfigList
					.toArray(new String[channelConfigList.size()]);
			channels = channelList.toArray(new String[channelList.size()]);
		} catch (IOException e) {
			error(this, getText(R.string.error_invalid_channel_configuration),
					e);
		}

		// update channel list view
		listView.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item,
				channels));
	}

	/**
	 * @param channelList
	 *            list name, insert null to read config from settings
	 */
	private void setChannelList(String channelList) {
		if (channelList == null) {
			channelList = Prefs.get(this).getString(Prefs.KEY_CHANNELCONFIGS,
					null);
		}

		if (StringUtils.isNullOrEmpty(channelList)
				|| Utils.getConfigsFile(this, channelList) == null) {
			String[] files = Utils.getConfigsDir(this).list();
			if (files.length == 1) {
				channelList = files[0];
			} else {
				channelList = null;
			}
		}

		if (channelList == null) {
			spinner.setSelection(AdapterView.INVALID_POSITION);
		} else {
			for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
				if (channelList.equals(spinner.getAdapter().getItem(i))) {
					spinner.setSelection(i);
					break;
				}
			}
		}
	}

	private void watchChannel(int channelId) {
		Log.d(TAG, "watchChannel(" + channelId + "): " + channels[channelId]);
		String channelConfig = channelConfigs[channelId];
		Intent intent = new Intent(this, StreamService.class);
		intent.putExtra(StreamService.EXTRA_CHANNELCONFIG, channelConfig);
		startService(intent);
	}
}
