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

import static com.chrulri.droidtv.Utils.NEWLINE;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedByInterruptException;


import android.util.Log;

class StreamServer {
	static final String TAG = LiveActivity.TAG + ".StreamServer";
	static final String TAG_REQ = TAG + ".RequestHandler";

	static final String INPUT_FILE = "/dev/dvb/adapter0/dvr0";

	static final int BUFFER_READ_SIZE = 4 * 1024; // 4 KByte

	static final String HTTP_URL = "http://localhost:%d/livetv.mpeg";
	static final String HTTP_HEADER = "HTTP/1.1 200 OK" + NEWLINE
			+ "Content-Type: video/mpeg" + NEWLINE + "Connection: keep-alive"
			+ NEWLINE + NEWLINE;

	private FileInputStream mFile;
	private ServerSocket mServer;
	private Thread mServerThread;
	private volatile boolean mServerRunning;
	private final Object mLock = new Object();

	public String getURL() {
		if (!mServerRunning)
			throw new IllegalStateException();
		return String.format(HTTP_URL, mServer.getLocalPort());
	}

	public void start() throws IOException {
		synchronized (mLock) {
			if (mServerRunning)
				throw new IllegalStateException();
			mServerRunning = true;
		}
		// initialize file handle, server socket and thread
		mFile = new FileInputStream(INPUT_FILE);
		mServer = new ServerSocket(0, 0, InetAddress.getByName(null));
		mServer.setSoTimeout(250);
		mServerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (mServerRunning) {
					try {
						Socket client = mServer.accept();
						handleRequest(client);
					} catch (SocketTimeoutException e) {
						continue;
					} catch (Throwable t) {
						Log.e(TAG, "mServerThread", t);
					}
				}
			}
		}, TAG + ".Worker");
		// start socket thread
		mServerThread.start();
	}

	private void handleRequest(final Socket client) {
		if (!mServerRunning)
			return;
		try {
			Log.d(
					TAG_REQ,
					String.format(Thread.currentThread().getName()
							+ " - client accepted (%s)", client.getInetAddress().toString()));
			// ----------------------------------------------------------------------
			// {
			// InputStreamReader reader = new
			// InputStreamReader(client.getInputStream());
			// StringBuilder buf = new StringBuilder();
			// char[] b = new char[1024];
			// while (reader.ready()) {
			// int len = reader.read(b);
			// buf.append(b, 0, len);
			// }
			// String input = buf.toString();
			// Log.d(TAG_REQ, String.format(Thread.currentThread().getName()
			// + " - received:\n%s", input));
			// }
			// ----------------------------------------------------------------------
			// write header
			OutputStream out = client.getOutputStream();
			out.write(HTTP_HEADER.getBytes());
			// write native input in a loop
			byte[] buf = new byte[BUFFER_READ_SIZE];
			while (mServerRunning) {
				int ret = mFile.read(buf);
				if (ret < BUFFER_READ_SIZE) {
					Log.d(
							TAG_REQ,
							String.format(Thread.currentThread().getName()
									+ " - retrieved %d bytes", ret));
				}
				if (ret > 0) {
					out.write(buf, 0, ret);
				} else if (ret < 0) {
					Log.e(
							TAG_REQ,
							String.format(Thread.currentThread().getName()
									+ " - nativeRead failed: %d", ret));
					return;
				}
			}
		} catch(InterruptedIOException e) {
			// nop
		} catch(ClosedByInterruptException e) {
			// nop
		} catch (Throwable t) {
			Log.d(TAG_REQ, Thread.currentThread().getName(), t);
		} finally {
			try {
				Log.d(TAG_REQ, Thread.currentThread().getName() + " - exiting");
				client.close();
			} catch (IOException e) {
				// nop
			}
		}
	}

	public void stop() {
		synchronized (mLock) {
			mServerRunning = false;
			try {
				if (mServerThread != null) {
					mServerThread.interrupt();
					mServerThread = null;
				}
				if (mServer != null) {
					mServer.close();
					mServer = null;
				}
				if (mFile != null) {
					mFile.close();
					mFile = null;
				}
			} catch (Throwable t) {
				Log.e(TAG, "stop", t);
			}
		}
	}
}
