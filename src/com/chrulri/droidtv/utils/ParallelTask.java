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

import android.util.Log;

public abstract class ParallelTask {
    private static final String TAG = ParallelTask.class.getSimpleName();

    private volatile boolean mCancelled;
    private final Thread mThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                doInBackground();
            } catch (Throwable t) {
                Log.e(TAG, "doInBackground", t);
            }
        }
    });

    public final Thread.State getState() {
        return mThread.getState();
    }

    protected abstract void doInBackground();

    protected final boolean isCancelled() {
        return mCancelled;
    }

    public final void execute() {
        mCancelled = false;
        mThread.start();
    }

    public final void cancel(boolean interrupt) {
        mCancelled = true;
        if (interrupt) {
            mThread.interrupt();
        }
    }
}
