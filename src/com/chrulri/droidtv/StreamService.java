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

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.chrulri.droidtv.Utils.ProcessUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import static com.chrulri.droidtv.Utils.NEWLINE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * DVBlast wrapper service <br/>
 * <br/>
 * README: {@link http://git.videolan.org/?p=dvblast.git;a=blob;f=README;h=
 * cda01aa2e0cf0999478a7dcb1d60305e5c8a7a7f
 * ;hb=350557c669ce3670b7ea1e252b11f261c0610239}
 */
public class StreamService extends Service {
    private static final String TAG = StreamService.class.getSimpleName();

    static final int DVBLAST = R.raw.dvblast_2_1_0;
    static final int DVBLASTCTL = R.raw.dvblastctl_2_1_0;

    public static final String EXTRA_CHANNELCONFIG = "channelconfig";

    public enum DvbType {
        ATSC, DVBT, DVBC, DVBS
    }

    public class FrontendStatus {

        public static final int HAS_SIGNAL = 0x001;
        public static final int HAS_CARRIER = 0x02;
        public static final int HAS_VITERBI = 0x04;
        public static final int HAS_SYNC = 0x08;
        public static final int HAS_LOCK = 0x0F;
        public static final int REINIT = 0x10;

        public int status;
        public int ber;
        public int signal;
        public int snr;
    }

    static final int BUFFER_LENGTH = 512 * 1024;

    static final String UDP_IP = "127.0.0.1";
    static final int UDP_PORT = 1555;

    static final String HTTP_IP = "0.0.0.0";// "127.0.0.1";
    static final int HTTP_PORT = 1666;
    static final String HTTP_HEADER = "HTTP/1.1 200 OK" + NEWLINE +
            "Content-Type: video/mp2t" + NEWLINE +
            "Connection: keep-alive" + NEWLINE + NEWLINE;

    public static final Uri SERVICE_URI = Uri.parse(String.format("http://127.0.0.1:%d/tv.ts",
            HTTP_PORT));

    /**
     * ip:port 1 serviceid
     */
    static final String DVBLAST_CONFIG_CONTENT = UDP_IP + ":" + UDP_PORT + " 1 %d";
    static final String DVBLAST_CONFIG_FILENAME = "dvblast.conf";
    static final String DVBLAST_SOCKET = "droidtv.socket";
    static final int DVBLAST_CHECKDELAY = 2500;

    private AsyncDvblastTask mDvblastTask;
    private AsyncDvblastCtlTask mDvblastCtlTask;
    private AsyncStreamTask mStreamTask;
    private DatagramSocket mUdpSocket;
    private ServerSocket mHttpSocket;

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "onStart");
        super.onStart(intent, startId);
        Bundle bundle = intent.getExtras();
        String channelConfig = bundle.getString(EXTRA_CHANNELCONFIG);
        DvbType dvbType = DvbType.DVBT;
        try {
            startDvblast(dvbType, channelConfig);
        } catch (IOException e) {
            Log.e(TAG, "starting streaming failed", e);
            sendError(e);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mDvblastCtlTask != null) {
            mDvblastCtlTask.cancel(true);
        }
        if (mDvblastTask != null) {
            mDvblastTask.cancel(true);
        }
        if (mStreamTask != null) {
            mStreamTask.cancel(true);
        }
        if (mUdpSocket != null) {
            mUdpSocket.close();
        }
        if (mHttpSocket != null) {
            try {
                mHttpSocket.close();
            } catch (IOException e) {
                // nop
            }
        }
        Log.d(TAG, "onDestroy - end");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startDvblast(DvbType dvbType, String channelConfig)
            throws IOException {
        Log.d(TAG, "startDvblast(" + channelConfig + ")");
        File configFile = new File(getCacheDir(), DVBLAST_CONFIG_FILENAME);
        PrintWriter writer = new PrintWriter(configFile);
        // sNAME/iFREQ/iServiceID
        String[] params = channelConfig.split(":");
        // check config length
        if (params.length != 3) {
            throw new IOException("invalid DVB params count[" + params.length + "]");
        }
        // parse config
        int freq = tryParseInt(params[1], "frequency");
        int sid = tryParseInt(params[2], "service ID");
        // print config
        writer.println(String.format(DVBLAST_CONFIG_CONTENT, sid));
        writer.close();
        // run dvblast
        Log.d(TAG, "dvblast(" + configFile + "," + freq + ")");
        mUdpSocket = new DatagramSocket(UDP_PORT, InetAddress.getByName(null));
        mHttpSocket = new ServerSocket(HTTP_PORT, 0/* FIXME */);
        mStreamTask = new AsyncStreamTask();
        mStreamTask.execute();
        mDvblastTask = new AsyncDvblastTask(configFile, freq);
        mDvblastTask.execute();
        mDvblastCtlTask = new AsyncDvblastCtlTask();
        mDvblastCtlTask.execute();
    }

    class AsyncStreamTask extends AsyncTask<Void, Void, Void> {

        final String TAG = StreamService.TAG + "." + AsyncStreamTask.class.getSimpleName();

        @Override
        protected void onCancelled() {
            sendUpdates(null);
            stopSelf();
        }

        @Override
        protected void onPostExecute(Void result) {
            sendUpdates(null);
            stopSelf();
        }

        @Override
        protected Void doInBackground(Void... params) {
            byte[] data = new byte[BUFFER_LENGTH];
            DatagramPacket dataPacket = new DatagramPacket(data, data.length);
            while (!isCancelled()) {
                Socket client;
                try {
                    client = mHttpSocket.accept();
                } catch (IOException e) {
                    continue;
                }
                // FIXME parse HTTP request
                try {
                    OutputStream out = client.getOutputStream();
                    out.write(HTTP_HEADER.getBytes());
                    out.flush();
                    while (!isCancelled()) {
                        try {
                            mUdpSocket.receive(dataPacket);
                            out.write(dataPacket.getData(), dataPacket.getOffset(),
                                    dataPacket.getLength());
                        } catch (SocketException e) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "STREAM", e);
                }
            }
            return null;
        }
    }

    class AsyncDvblastCtlTask extends AsyncTask<Void, FrontendStatus, Void> {

        final String TAG = StreamService.TAG + "." + AsyncDvblastCtlTask.class.getSimpleName();

        @Override
        protected Void doInBackground(Void... params) {
            while (!isCancelled()) {
                try {
                    Thread.sleep(DVBLAST_CHECKDELAY);
                } catch (InterruptedException e) {
                    continue;
                }
                try {
                    Process dvblastctl = Utils.runBinary(StreamService.this, DVBLASTCTL,
                            "-r", DVBLAST_SOCKET, "-x", "xml", "fe_status");
                    int exitCode = dvblastctl.waitFor();
                    if (exitCode != 0) {
                        Log.w(TAG, "exited with " + exitCode);
                        continue;
                    }
                    Document dom = getDomElement(dvblastctl.getInputStream());
                    FrontendStatus status = new FrontendStatus();
                    NodeList statusList = dom.getElementsByTagName("STATUS");
                    for (int i = 0; i < statusList.getLength(); i++) {
                        Node node = statusList.item(i);
                        String statusName = node.getAttributes().getNamedItem("status")
                                .getNodeValue();
                        if ("HAS_SIGNAL".equals(statusName)) {
                            status.status |= FrontendStatus.HAS_SIGNAL;
                        } else if ("HAS_CARRIER".equals(statusName)) {
                            status.status |= FrontendStatus.HAS_CARRIER;
                        } else if ("HAS_VITERBI".equals(statusName)) {
                            status.status |= FrontendStatus.HAS_VITERBI;
                        } else if ("HAS_SYNC".equals(statusName)) {
                            status.status |= FrontendStatus.HAS_SYNC;
                        } else if ("HAS_LOCK".equals(statusName)) {
                            status.status |= FrontendStatus.HAS_LOCK;
                        } else if ("REINIT".equals(statusName)) {
                            status.status |= FrontendStatus.REINIT;
                        }
                    }
                    NodeList valueList = dom.getElementsByTagName("VALUE");
                    for (int i = 0; i < valueList.getLength(); i++) {
                        Node node = valueList.item(i);
                        Node valueNode = node.getAttributes().item(0);
                        String valueName = valueNode.getNodeName();
                        int value = Integer.parseInt(valueNode.getNodeValue());
                        if ("bit_error_rate".equalsIgnoreCase(valueName)) {
                            status.ber = value;
                        } else if ("signal_strength".equalsIgnoreCase(valueName)) {
                            status.signal = value;
                        } else if ("snr".equalsIgnoreCase(valueName)) {
                            status.snr = value;
                        }
                    }
                    publishProgress(status);
                } catch (Throwable t) {
                    Log.w(TAG, "dvblastctl", t);
                }
            }
            return null;
        }

        private Document getDomElement(InputStream xmlStream) {
            Document doc = null;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                is.setByteStream(xmlStream);
                doc = db.parse(is);
            } catch (ParserConfigurationException e) {
                Log.e(TAG, e.getMessage());
                return null;
            } catch (SAXException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            } catch (IOException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            }
            return doc;
        }

    }

    class AsyncDvblastTask extends AsyncTask<Void, CharSequence, Void> {

        final String TAG = StreamService.TAG + "." + AsyncDvblastTask.class.getSimpleName();

        private File mConfigFile;
        private int mFreq;

        public AsyncDvblastTask(File configFile, int freq) {
            mConfigFile = configFile;
            mFreq = freq;
        }

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
        protected Void doInBackground(Void... params) {
            try {
                Process dvblast = Utils.runBinary(StreamService.this, DVBLAST, "-U", "-O", "5000",
                        "-r", DVBLAST_SOCKET, "-x", "xml", "-c", mConfigFile.getAbsolutePath(),
                        "-f", "" + mFreq, "-q");
                Reader input = new InputStreamReader(dvblast.getInputStream());
                BufferedReader reader = new BufferedReader(input);
                Integer exitCode = null;
                String line;
                while (!isCancelled()
                        && ((line = reader.readLine()) != null || (exitCode = ProcessUtils
                                .checkExitCode(dvblast)) == null)) {
                    if (line == null) {
                        Thread.sleep(250);
                    } else {
                        Log.d(TAG, "#" + line);
                        publishProgress(line);
                    }
                }
                if (exitCode == null) {
                    ProcessUtils.terminate(dvblast);
                    Log.d(TAG, "dvblast destroying");
                    // seriously wait until it's done!
                    while (true) {
                        try {
                            dvblast.waitFor();
                            break;
                        } catch (InterruptedException e) {
                            continue;
                        }
                    }
                    Log.d(TAG, "dvblast destroyed");
                } else if (exitCode != 0) {
                    // FIXME localization
                    Log.e(TAG, "dvblast failed (" + exitCode + ")");
                    Log.d(TAG, ProcessUtils.readStdOut(dvblast));
                    Log.d(TAG, ProcessUtils.readErrOut(dvblast));
                    sendError("dvblast failed (" + exitCode + ")");
                }
            } catch (Throwable t) {
                Log.e(TAG, "dvblast", t);
                sendError("dvblast", t);
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

    private static int tryParseInt(String str, String paramName)
            throws IOException {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new IOException(
                    "error while parsing " + paramName + " (" + str + ")");
        }
    }
}
