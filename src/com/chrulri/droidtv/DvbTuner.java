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

import java.io.File;

import android.os.SystemClock;
import android.util.Log;

class DvbTuner {
	static final String TAG = LiveActivity.TAG + ".DvbTuner";

	static {
		System.loadLibrary("zap");
	}

	static final int TYPE_ATSC = 0x1;
	static final int TYPE_DVBC = 0x2;
	static final int TYPE_DVBS = 0x3;
	static final int TYPE_DVBT = 0x4;

	private native int nativeZapStart(int dvbType, String channelsFilePath,
			String channelName);

	private native void nativeZapStop(int dvbType);

	private native int nativeZapStatus(int dvbType, ZapStatus status);

	static final int TUNE_TIMEOUT_MS = 2500;
	static final int TUNE_DELAY_MS = 250;

	private volatile boolean mRunning;
	private int mDvbType;

	public boolean start(int dvbType, File channelsFile, String channelName) {
		if (mRunning)
			throw new IllegalStateException();
		if (!channelsFile.exists())
			throw new IllegalArgumentException("channelsFile");
		if (channelName == null || channelName.trim().length() == 0)
			throw new IllegalArgumentException("channelName");
		mRunning = true;
		mDvbType = dvbType;
		int ret = nativeZapStart(mDvbType, channelsFile.getAbsolutePath(),
				channelName);
		if (ret != 0) {
			Log.e(TAG, "nativeZapStart: " + ret);
			stop();
			return false;
		}

		long timer = System.currentTimeMillis();
		ZapStatus status;
		while ((status = getStatus()) != null
				&& timer > System.currentTimeMillis() - TUNE_TIMEOUT_MS) {
			boolean hasLock = (status.getStatus() & ZapStatus.FE_HAS_LOCK) != 0;
			Log.d(TAG, String.format(
					"status %02x | signal %3d%% | snr %3d%% | ber %d | unc %d | %s",
					status.getStatus(), (status.getSignal() * 100) / 0xffff,
					(status.getSNR() * 100) / 0xffff, status.getBER(), status.getUNC(),
					hasLock ? "FE_HAS_LOCK" : ""));
			if (hasLock) {
				Log.v(TAG, "frontend has lock");
				return true;
			}
			SystemClock.sleep(TUNE_DELAY_MS);
		}
		Log.w(TAG, "frontend failed to get lock");
		stop();
		return false;
	}

	public ZapStatus getStatus() {
		if (!mRunning)
			throw new IllegalStateException();
		ZapStatus status = new ZapStatus();
		int ret = nativeZapStatus(mDvbType, status);
		if (ret != 0) {
			Log.e(TAG, "getStatus: " + ret);
			return null;
		}
		return status;
	}

	public void stop() {
		nativeZapStop(mDvbType);
		mRunning = false;
	}

	public class ZapStatus {

		public static final int FE_HAS_SIGNAL = 0x01; /* found something above the noise level */
		public static final int FE_HAS_CARRIER = 0x02; /* found a DVB signal */
		public static final int FE_HAS_VITERBI = 0x04; /* FEC is stable */
		public static final int FE_HAS_SYNC = 0x08; /* found sync bytes */
		public static final int FE_HAS_LOCK = 0x10; /* everything's working... */
		public static final int FE_TIMEDOUT = 0x20; /* no lock within the last ~2 seconds */
		public static final int FE_REINIT = 0x40; /* frontend was reinitialized, */

		private int mStatus;
		private int mSignal;
		private int mSNR;
		private int mBER;
		private int mUNC;

		public void set(int status, int signal, int snr, int ber, int unc) {
			mStatus = status;
			mSignal = signal;
			mSNR = snr;
			mBER = ber;
			mUNC = unc;
		}

		public int getStatus() {
			return mStatus;
		}

		public int getSignal() {
			return mSignal;
		}

		public int getSNR() {
			return mSNR;
		}

		public int getBER() {
			return mBER;
		}

		public int getUNC() {
			return mUNC;
		}
	}
}
