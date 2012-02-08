package chrulri.livetv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import chrulri.livetv.Utils.Prefs;
import chrulri.livetv.Utils.StringUtils;

public class LiveActivity extends Activity implements OnBufferingUpdateListener, OnCompletionListener, OnErrorListener, OnInfoListener, OnSeekCompleteListener, OnVideoSizeChangedListener, Callback {
	static final String TAG = "LiveTv";

	static final int DIALOG_PROGRESS_BUFFER = 0x101;
	static final int DIALOG_PROGRESS_TUNE = 0x102;
	static final int DIALOG_LIST_CHANNEL = 0x301;

	static final String BUNDLE_CHANNELS_LIST = "channels_list";
	static final String BUNDLE_CHANNELS_INDEX = "channels_index";

	static final String FILE_CHANNELS_CONF = "channels.conf";
	static final String FILE_IMPORT_CHANNELS_CONF = "/sdcard/channels.conf";

	static final String PREF_CURRENT_CHANNEL = "current_channel";

	private MediaPlayer mPlayer;
	private StreamServer mServer;
	private DvbTuner mTuner;
	private SurfaceHolder mSurfaceHolder;
	private ProgressDialog mDialogProgressBuffer;
	private boolean mIsOnAir;

	private void openSettings() {
		Intent settingsActivity = new Intent(getBaseContext(),
				PreferencesActivity.class);
		startActivity(settingsActivity);
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_LIST_CHANNEL:
			final String[] channels = args.getStringArray(BUNDLE_CHANNELS_LIST);
			int channelIndex = args.getInt(BUNDLE_CHANNELS_INDEX);
			return new AlertDialog.Builder(this)
					.setTitle(R.string.choose_channel)
					.setSingleChoiceItems(channels, channelIndex,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {
									dialog.dismiss();
									final String channel = channels[item];
									new Thread() {
										public void run() {
											runOnUiThread(new Runnable() {
												public void run() {
													stopLiveTv();
													startLiveTv(channel);
												}
											});
										}
									}.start();
								}
							}).create();

		case DIALOG_PROGRESS_BUFFER:
			mDialogProgressBuffer = new ProgressDialog(this);
			mDialogProgressBuffer.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mDialogProgressBuffer.setMessage(getText(R.string.buffering));
			return mDialogProgressBuffer;

		case DIALOG_PROGRESS_TUNE:
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setMessage(getText(R.string.tuning));
			return dialog;
		}
		return super.onCreateDialog(id, args);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTuner = new DvbTuner();
		mServer = new StreamServer();
		setContentView(R.layout.live);
		// get surface
		SurfaceView surface = (SurfaceView) findViewById(R.id.live_surface);
		surface.setZOrderOnTop(false);
		surface.setClickable(true);
		surface.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openOptionsMenu();
			}
		});
		mSurfaceHolder = surface.getHolder();
		mSurfaceHolder.addCallback(LiveActivity.this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mSurfaceHolder = null;
		mServer = null;
		mTuner = null;
	}

	@Override
	protected void onStart() {
		super.onStart();
		boolean playLastChannel = Prefs.get(this).getBoolean(
				Prefs.KEY_PLAYLASTCHANNELONSTARTUP, false);
		chooseChannel(!playLastChannel);
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopLiveTv();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.channels:
			chooseChannel(true);
			return true;
		case R.id.settings:
			openSettings();
			return true;
		case R.id.quit:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private File getChannelsFile() {
		String fileName = Prefs.get(this).getString(Prefs.KEY_CHANNELCONFIGS, null);
		File file;
		if (StringUtils.isNullOrEmpty(fileName)
				|| (file = Utils.getConfigsFile(this, fileName)) == null) {
			File[] files = Utils.getConfigsDir(this).listFiles();
			if (files.length == 0)
				return null;
			file = files[0];
			Prefs.get(this).edit()
					.putString(Prefs.KEY_CHANNELCONFIGS, file.getName()).commit();
		}
		return file;
	}

	private String[] loadChannels() {
		// determine channels.conf file
		File f = getChannelsFile();
		if (f == null || !f.exists()) {
			// Utils.error(this, getText(R.string.error_no_channel_configuration));
			// start settings/scan instead of showing useless error message
			openSettings();
			return null;
		}
		// load channels
		try {
			List<String> channels = new ArrayList<String>();
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;
			while ((line = reader.readLine()) != null)
				channels.add(line.split(":")[0]);
			reader.close();
			return channels.toArray(new String[0]);
		} catch (IOException e) {
			Utils.error(this, getText(R.string.error_invalid_channel_configuration),
					e);
			return null;
		}
	}

	private void chooseChannel(boolean forceDialog) {
		String[] channels = loadChannels();
		if (channels == null)
			return;
		String cc = getPreferences(0).getString(PREF_CURRENT_CHANNEL, null);
		int index = -1;
		for (int i = 0; i < channels.length; i++) {
			if (channels[i].equals(cc)) {
				index = i;
				break;
			}
		}
		if (index == -1 || forceDialog) {
			// list channels
			Bundle channelsBundle = new Bundle();
			channelsBundle.putStringArray(BUNDLE_CHANNELS_LIST, channels);
			channelsBundle.putInt(BUNDLE_CHANNELS_INDEX, index);
			showDialog(DIALOG_LIST_CHANNEL, channelsBundle);
		} else {
			stopLiveTv();
			startLiveTv(channels[index]);
		}
	}

	private void startLiveTv(final String channelName) {
		new AsyncLiveTask().execute(channelName);
	}

	private void stopLiveTv() {
		mServer.stop();
		mTuner.stop();
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
		mIsOnAir = false;
	}

	class AsyncLiveTask extends AsyncTask<String, Integer, Void> {

		static final int PROGRESS_OPEN_DIALOG_PROGRESS_TUNE = 1;
		static final int PROGRESS_CLOSE_DIALOG_PROGRESS_TUNE = 2;
		static final int PROGRESS_OPEN_DIALOG_PROGRESS_BUFFER = 3;
		static final int PROGRESS_CLOSE_DIALOG_PROGRESS_BUFFER = 4;

		@Override
		protected void onPreExecute() {
			if (mIsOnAir) {
				Log.w(TAG, "is already on air", new Throwable().fillInStackTrace());
				cancel(true);
				return;
			}
			mIsOnAir = true;
		}

		@Override
		protected Void doInBackground(String... params) {
			String channelName = params[0];
			// start tune server
			try {
				publishProgress(PROGRESS_OPEN_DIALOG_PROGRESS_TUNE);
				if (!mTuner.start(Prefs.getDvbType(LiveActivity.this),
						getChannelsFile(), channelName)) {
					cancel("failed to tune to '" + channelName + "'", null);
					return null;
				}
				// store current channel
				Prefs.get(LiveActivity.this).edit()
						.putString(PREF_CURRENT_CHANNEL, channelName).commit();
			} catch (Exception e) {
				cancel("failed to tune to '" + channelName + "'", e);
				return null;
			} finally {
				publishProgress(PROGRESS_CLOSE_DIALOG_PROGRESS_TUNE);
			}
			// start stream server
			try {
				publishProgress(PROGRESS_OPEN_DIALOG_PROGRESS_BUFFER);
				mServer.start();
				String url = mServer.getURL();
				Log.d(TAG, url);
				// prepare player
				mPlayer = new MediaPlayer();
				mPlayer.setScreenOnWhilePlaying(true);
				mPlayer.setOnBufferingUpdateListener(LiveActivity.this);
				mPlayer.setOnCompletionListener(LiveActivity.this);
				mPlayer.setOnErrorListener(LiveActivity.this);
				mPlayer.setOnInfoListener(LiveActivity.this);
				mPlayer.setOnSeekCompleteListener(LiveActivity.this);
				mPlayer.setOnVideoSizeChangedListener(LiveActivity.this);
				mPlayer.setDisplay(mSurfaceHolder);
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setDataSource(url);
				mPlayer.prepare();
				Log.d(TAG, "MediaPlayer prepared");
			} catch (Exception e) {
				cancel("startLiveTv.play", e);
			} finally {
				publishProgress(PROGRESS_CLOSE_DIALOG_PROGRESS_BUFFER);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Log.d(TAG, "MediaPlayer starting");
			mPlayer.start();
			Log.d(TAG, "MediaPlayer started");
		}
		
		@Override
		protected void onCancelled() {
			stopLiveTv();
		}

		private void cancel(String msg, Exception e) {
			Log.e(TAG, msg, e);
			Utils.error(LiveActivity.this, msg, e);
			cancel(false);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			switch (values[0]) {
			case PROGRESS_OPEN_DIALOG_PROGRESS_TUNE:
				showDialog(DIALOG_PROGRESS_TUNE);
				break;
			case PROGRESS_CLOSE_DIALOG_PROGRESS_TUNE:
				dismissDialog(DIALOG_PROGRESS_TUNE);
				break;
			case PROGRESS_OPEN_DIALOG_PROGRESS_BUFFER:
				showDialog(DIALOG_PROGRESS_BUFFER);
				mDialogProgressBuffer.setProgress(0);
				break;
			case PROGRESS_CLOSE_DIALOG_PROGRESS_BUFFER:
				dismissDialog(DIALOG_PROGRESS_BUFFER);
				break;
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG + "." + SurfaceHolder.class.getSimpleName(), "surface destroyed");
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG + "." + SurfaceHolder.class.getSimpleName(), "surface created");
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG + "." + SurfaceHolder.class.getSimpleName(), String.format("surface changed (%d / %d x %d)", format, width, height));
	}
	
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.d(TAG + "." + MediaPlayer.class.getSimpleName(),
				String.format("MediaPlayer buffering update: %d", percent));
		mDialogProgressBuffer.setProgress(percent);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.d(TAG + "." + MediaPlayer.class.getSimpleName(), "MediaPlayer completion");
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		String msg = getString(R.string.error_mediaplayer, what, extra);
		Log.e(TAG + "." + MediaPlayer.class.getSimpleName(), msg);
		Utils.error(LiveActivity.this, msg);
		return true;
	}
	
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		Log.i(TAG + "." + MediaPlayer.class.getSimpleName(), String.format("MediaPlayer info: what=%d, extra=%d",
				what, extra));
		return true;
	}
	
	@Override
	public void onSeekComplete(MediaPlayer mp) {
		Log.d(TAG + "." + MediaPlayer.class.getSimpleName(), "MediaPlayer seek complete");
	}
	
	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		Log.d(TAG + "." + MediaPlayer.class.getSimpleName(), String.format("MediaPlayer video size changed (%dx%d)",
				width, height));
		mSurfaceHolder.setFixedSize(width, height);
	}
}