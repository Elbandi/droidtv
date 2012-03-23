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
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chrulri.droidtv.StreamService.DvbType;
import com.chrulri.droidtv.Utils.ProcessUtils;
import com.chrulri.droidtv.Utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Date;
import java.util.Locale;

public class ScanActivity extends Activity implements OnClickListener,
        OnLongClickListener {
    static final String TAG = ScanActivity.class.getName();

    static final int WSCAN = R.raw.wscan_20110329;
    static final String[][] WSCAN_COUNTRIES = {
            {
                    "AF", "AFGHANISTAN"
            },
            {
                    "AX", "ÅLAND ISLANDS"
            },
            {
                    "AL", "ALBANIA"
            },
            {
                    "DZ", "ALGERIA"
            },
            {
                    "AS", "AMERICAN SAMOA"
            },
            {
                    "AD", "ANDORRA"
            },
            {
                    "AO", "ANGOLA"
            },
            {
                    "AI", "ANGUILLA"
            },
            {
                    "AQ", "ANTARCTICA"
            },
            {
                    "AG", "ANTIGUA AND BARBUDA"
            },
            {
                    "AR", "ARGENTINA"
            },
            {
                    "AM", "ARMENIA"
            },
            {
                    "AW", "ARUBA"
            },
            {
                    "AU", "AUSTRALIA"
            },
            {
                    "AT", "AUSTRIA"
            },
            {
                    "AZ", "AZERBAIJAN"
            },
            {
                    "BS", "BAHAMAS"
            },
            {
                    "BH", "BAHRAIN"
            },
            {
                    "BD", "BANGLADESH"
            },
            {
                    "BB", "BARBADOS"
            },
            {
                    "BY", "BELARUS"
            },
            {
                    "BE", "BELGIUM"
            },
            {
                    "BZ", "BELIZE"
            },
            {
                    "BJ", "BENIN"
            },
            {
                    "BM", "BERMUDA"
            },
            {
                    "BT", "BHUTAN"
            },
            {
                    "BO", "BOLIVIA"
            },
            {
                    "BA", "BOSNIA AND HERZEGOVINA"
            },
            {
                    "BW", "BOTSWANA"
            },
            {
                    "BV", "BOUVET ISLAND"
            },
            {
                    "BR", "BRAZIL"
            },
            {
                    "IO", "BRITISH INDIAN OCEAN TERRITORY"
            },
            {
                    "BN", "BRUNEI DARUSSALAM"
            },
            {
                    "BG", "BULGARIA"
            },
            {
                    "BF", "BURKINA FASO"
            },
            {
                    "BI", "BURUNDI"
            },
            {
                    "KH", "CAMBODIA"
            },
            {
                    "CM", "CAMEROON"
            },
            {
                    "CA", "CANADA"
            },
            {
                    "CV", "CAPE VERDE"
            },
            {
                    "KY", "CAYMAN ISLANDS"
            },
            {
                    "CF", "CENTRAL AFRICAN REPUBLIC"
            },
            {
                    "TD", "CHAD"
            },
            {
                    "CL", "CHILE"
            },
            {
                    "CN", "CHINA"
            },
            {
                    "CX", "CHRISTMAS ISLAND"
            },
            {
                    "CC", "COCOS (KEELING) ISLANDS"
            },
            {
                    "CO", "COLOMBIA"
            },
            {
                    "KM", "COMOROS"
            },
            {
                    "CG", "CONGO"
            },
            {
                    "CD", "CONGO, THE DEMOCRATIC REPUBLIC OF THE"
            },
            {
                    "CK", "COOK ISLANDS"
            },
            {
                    "CR", "COSTA RICA"
            },
            {
                    "CI", "CÔTE D'IVOIRE"
            },
            {
                    "HR", "CROATIA"
            },
            {
                    "CU", "CUBA"
            },
            {
                    "CY", "CYPRUS"
            },
            {
                    "CZ", "CZECH REPUBLIC"
            },
            {
                    "DK", "DENMARK"
            },
            {
                    "DJ", "DJIBOUTI"
            },
            {
                    "DM", "DOMINICA"
            },
            {
                    "DO", "DOMINICAN REPUBLIC"
            },
            {
                    "EC", "ECUADOR"
            },
            {
                    "EG", "EGYPT"
            },
            {
                    "SV", "EL SALVADOR"
            },
            {
                    "GQ", "EQUATORIAL GUINEA"
            },
            {
                    "ER", "ERITREA"
            },
            {
                    "EE", "ESTONIA"
            },
            {
                    "ET", "ETHIOPIA"
            },
            {
                    "FK", "FALKLAND ISLANDS (MALVINAS)"
            },
            {
                    "FO", "FAROE ISLANDS"
            },
            {
                    "FJ", "FIJI"
            },
            {
                    "FI", "FINLAND"
            },
            {
                    "FR", "FRANCE"
            },
            {
                    "GF", "FRENCH GUIANA"
            },
            {
                    "PF", "FRENCH POLYNESIA"
            },
            {
                    "TF", "FRENCH SOUTHERN TERRITORIES"
            },
            {
                    "GA", "GABON"
            },
            {
                    "GM", "GAMBIA"
            },
            {
                    "GE", "GEORGIA"
            },
            {
                    "DE", "GERMANY"
            },
            {
                    "GH", "GHANA"
            },
            {
                    "GI", "GIBRALTAR"
            },
            {
                    "GR", "GREECE"
            },
            {
                    "GL", "GREENLAND"
            },
            {
                    "GD", "GRENADA"
            },
            {
                    "GP", "GUADELOUPE"
            },
            {
                    "GU", "GUAM"
            },
            {
                    "GT", "GUATEMALA"
            },
            {
                    "GG", "GUERNSEY"
            },
            {
                    "GN", "GUINEA"
            },
            {
                    "GW", "GUINEA-BISSAU"
            },
            {
                    "GY", "GUYANA"
            },
            {
                    "HT", "HAITI"
            },
            {
                    "HM", "HEARD ISLAND AND MCDONALD ISLANDS"
            },
            {
                    "VA", "HOLY SEE (VATICAN CITY STATE)"
            },
            {
                    "HN", "HONDURAS"
            },
            {
                    "HK", "HONG KONG"
            },
            {
                    "HU", "HUNGARY"
            },
            {
                    "IS", "ICELAND"
            },
            {
                    "IN", "INDIA"
            },
            {
                    "ID", "INDONESIA"
            },
            {
                    "IR", "IRAN, ISLAMIC REPUBLIC OF"
            },
            {
                    "IQ", "IRAQ"
            },
            {
                    "IE", "IRELAND"
            },
            {
                    "IM", "ISLE OF MAN"
            },
            {
                    "IL", "ISRAEL"
            },
            {
                    "IT", "ITALY"
            },
            {
                    "JM", "JAMAICA"
            },
            {
                    "JP", "JAPAN"
            },
            {
                    "JE", "JERSEY"
            },
            {
                    "JO", "JORDAN"
            },
            {
                    "KZ", "KAZAKHSTAN"
            },
            {
                    "KE", "KENYA"
            },
            {
                    "KI", "KIRIBATI"
            },
            {
                    "KP", "KOREA, DEMOCRATIC PEOPLE'S REPUBLIC OF"
            },
            {
                    "KR", "KOREA, REPUBLIC OF"
            },
            {
                    "KW", "KUWAIT"
            },
            {
                    "KG", "KYRGYZSTAN"
            },
            {
                    "LA", "LAO PEOPLE'S DEMOCRATIC REPUBLIC"
            },
            {
                    "LV", "LATVIA"
            },
            {
                    "LB", "LEBANON"
            },
            {
                    "LS", "LESOTHO"
            },
            {
                    "LR", "LIBERIA"
            },
            {
                    "LY", "LIBYAN ARAB JAMAHIRIYA"
            },
            {
                    "LI", "LIECHTENSTEIN"
            },
            {
                    "LT", "LITHUANIA"
            },
            {
                    "LU", "LUXEMBOURG"
            },
            {
                    "MO", "MACAO"
            },
            {
                    "MK", "MACEDONIA, THE FORMER YUGOSLAV REPUBLIC OF"
            },
            {
                    "MG", "MADAGASCAR"
            },
            {
                    "MW", "MALAWI"
            },
            {
                    "MY", "MALAYSIA"
            },
            {
                    "MV", "MALDIVES"
            },
            {
                    "ML", "MALI"
            },
            {
                    "MT", "MALTA"
            },
            {
                    "MH", "MARSHALL ISLANDS"
            },
            {
                    "MQ", "MARTINIQUE"
            },
            {
                    "MR", "MAURITANIA"
            },
            {
                    "MU", "MAURITIUS"
            },
            {
                    "YT", "MAYOTTE"
            },
            {
                    "MX", "MEXICO"
            },
            {
                    "FM", "MICRONESIA, FEDERATED STATES OF"
            },
            {
                    "MD", "MOLDOVA"
            },
            {
                    "MC", "MONACO"
            },
            {
                    "MN", "MONGOLIA"
            },
            {
                    "ME", "MONTENEGRO"
            },
            {
                    "MS", "MONTSERRAT"
            },
            {
                    "MA", "MOROCCO"
            },
            {
                    "MZ", "MOZAMBIQUE"
            },
            {
                    "MM", "MYANMAR"
            },
            {
                    "NA", "NAMIBIA"
            },
            {
                    "NR", "NAURU"
            },
            {
                    "NP", "NEPAL"
            },
            {
                    "NL", "NETHERLANDS"
            },
            {
                    "AN", "NETHERLANDS ANTILLES"
            },
            {
                    "NC", "NEW CALEDONIA"
            },
            {
                    "NZ", "NEW ZEALAND"
            },
            {
                    "NI", "NICARAGUA"
            },
            {
                    "NE", "NIGER"
            },
            {
                    "NG", "NIGERIA"
            },
            {
                    "NU", "NIUE"
            },
            {
                    "NF", "NORFOLK ISLAND"
            },
            {
                    "MP", "NORTHERN MARIANA ISLANDS"
            },
            {
                    "NO", "NORWAY"
            },
            {
                    "OM", "OMAN"
            },
            {
                    "PK", "PAKISTAN"
            },
            {
                    "PW", "PALAU"
            },
            {
                    "PS", "PALESTINIAN TERRITORY, OCCUPIED"
            },
            {
                    "PA", "PANAMA"
            },
            {
                    "PG", "PAPUA NEW GUINEA"
            },
            {
                    "PY", "PARAGUAY"
            },
            {
                    "PE", "PERU"
            },
            {
                    "PH", "PHILIPPINES"
            },
            {
                    "PN", "PITCAIRN"
            },
            {
                    "PL", "POLAND"
            },
            {
                    "PT", "PORTUGAL"
            },
            {
                    "PR", "PUERTO RICO"
            },
            {
                    "QA", "QATA"
            },
            {
                    "RE", "RÉUNION"
            },
            {
                    "RO", "ROMANIA"
            },
            {
                    "RU", "RUSSIAN FEDERATION"
            },
            {
                    "RW", "RWANDA"
            },
            {
                    "BL", "SAINT BARTHÉLEMY"
            },
            {
                    "SH", "SAINT HELENA"
            },
            {
                    "KN", "SAINT KITTS AND NEVIS"
            },
            {
                    "LC", "SAINT LUCIA"
            },
            {
                    "MF", "SAINT MARTIN"
            },
            {
                    "PM", "SAINT PIERRE AND MIQUELON"
            },
            {
                    "VC", "SAINT VINCENT AND THE GRENADINES"
            },
            {
                    "WS", "SAMOA"
            },
            {
                    "SM", "SAN MARINO"
            },
            {
                    "ST", "SAO TOME AND PRINCIPE"
            },
            {
                    "SA", "SAUDI ARABIA"
            },
            {
                    "SN", "SENEGAL"
            },
            {
                    "RS", "SERBIA"
            },
            {
                    "SC", "SEYCHELLES"
            },
            {
                    "SL", "SIERRA LEONE"
            },
            {
                    "SG", "SINGAPORE"
            },
            {
                    "SK", "SLOVAKIA"
            },
            {
                    "SI", "SLOVENIA"
            },
            {
                    "SB", "SOLOMON ISLANDS"
            },
            {
                    "SO", "SOMALIA"
            },
            {
                    "ZA", "SOUTH AFRICA"
            },
            {
                    "GS", "SOUTH GEORGIA AND THE SOUTH SANDWICH ISLANDS"
            },
            {
                    "ES", "SPAIN"
            },
            {
                    "LK", "SRI LANKA"
            },
            {
                    "SD", "SUDAN"
            },
            {
                    "SR", "SURINAME"
            },
            {
                    "SJ", "SVALBARD AND JAN MAYEN"
            },
            {
                    "SZ", "SWAZILAND"
            },
            {
                    "SE", "SWEDEN"
            },
            {
                    "CH", "SWITZERLAND"
            },
            {
                    "SY", "SYRIAN ARAB REPUBLIC"
            },
            {
                    "TW", "TAIWAN"
            },
            {
                    "TJ", "TAJIKISTAN"
            },
            {
                    "TZ", "TANZANIA, UNITED REPUBLIC OF"
            },
            {
                    "TH", "THAILAND"
            },
            {
                    "TL", "TIMOR-LESTE"
            },
            {
                    "TG", "TOGO"
            },
            {
                    "TK", "TOKELAU"
            },
            {
                    "TO", "TONGA"
            },
            {
                    "TT", "TRINIDAD AND TOBAGO"
            },
            {
                    "TN", "TUNISIA"
            },
            {
                    "TR", "TURKEY"
            },
            {
                    "TM", "TURKMENISTAN"
            },
            {
                    "TC", "TURKS AND CAICOS ISLANDS"
            },
            {
                    "TV", "TUVALU"
            },
            {
                    "UG", "UGANDA"
            },
            {
                    "UA", "UKRAINE"
            },
            {
                    "AE", "UNITED ARAB EMIRATES"
            },
            {
                    "GB", "UNITED KINGDOM"
            },
            {
                    "US", "UNITED STATES"
            },
            {
                    "UM", "UNITED STATES MINOR OUTLYING ISLANDS"
            },
            {
                    "UY", "URUGUAY"
            },
            {
                    "UZ", "UZBEKISTAN"
            },
            {
                    "VU", "VANUATU"
            },
            {
                    "VE", "VENEZUELA"
            },
            {
                    "VN", "VIET NAM"
            },
            {
                    "VG", "VIRGIN ISLANDS, BRITISH"
            },
            {
                    "VI", "VIRGIN ISLANDS, U.S."
            },
            {
                    "WF", "WALLIS AND FUTUNA"
            },
            {
                    "EH", "WESTERN SAHARA"
            },
            {
                    "YE", "YEMEN"
            },
            {
                    "ZM", "ZAMBIA"
            },
    };

    static final String SPINNER_COLUMN_ISO = "iso";
    static final String SPINNER_COLUMN_COUNTRY = "country";

    private Spinner _countryList;
    private Button _scanButton;
    private TextView _textView;
    private ScrollView _scrollView;
    private EditText _fileNameView;
    private AsyncScanTask _scanTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);
        // country list
        _countryList = (Spinner) findViewById(R.id.scan_countrySpinner);
        String[] countries = new String[WSCAN_COUNTRIES.length];
        for (int i = 0; i < WSCAN_COUNTRIES.length; i++) {
            String[] wscanCountry = WSCAN_COUNTRIES[i];
            countries[i] = wscanCountry[1] + " (" + wscanCountry[0] + ")";
        }
        _countryList.setAdapter(Utils.createSimpleArrayAdapter(this, countries));
        // scan button
        _scanButton = (Button) findViewById(R.id.scan_scanButton);
        _scanButton.setOnClickListener(this);
        // text views
        _textView = (TextView) findViewById(R.id.scan_textView);
        _textView.setOnLongClickListener(this);
        _scrollView = (ScrollView) findViewById(R.id.scan_scrollView);
        _fileNameView = (EditText) findViewById(R.id.scan_fileNameView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _countryList = null;
        _scanButton = null;
        _textView = null;
        _scrollView = null;
        _fileNameView = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        _fileNameView.setText(DateFormat.format("yyyyMMddhhmm", new Date()));
        for (int i = 0; i < WSCAN_COUNTRIES.length; i++) {
            if (Locale.getDefault().getCountry()
                    .equalsIgnoreCase(WSCAN_COUNTRIES[i][0])) {
                _countryList.setSelection(i);
                break;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (_scanTask != null) {
            _scanTask.cancel(true);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == _scanButton) {
            _scanTask = new AsyncScanTask();
            _scanTask.execute();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == _textView) {
            CharSequence txt = _textView.getText();
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setText(txt);
            // TODO localization
            Toast.makeText(this, "Log copied to clipboard", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    protected void setEnabled(boolean enabled) {
        if (isFinishing())
            return;
        _countryList.setEnabled(enabled);
        _fileNameView.setEnabled(enabled);
        _scanButton.setEnabled(enabled);
    }

    class AsyncScanTask extends AsyncTask<Void, CharSequence, String> {
        private DvbType _dvbType;
        private String _type;
        private String _country;
        private File _outFile;

        @Override
        protected void onPreExecute() {
            setEnabled(false);
            _textView.setText(null);
            // dvbType
            _dvbType = DvbType.DVBT;
            // FIXME _dvbType = Prefs.getDvbType(ScanActivity.this);
            _type = (String) Utils.decode(_dvbType, DvbType.ATSC, "a",
                    DvbType.DVBC, "c",
                    DvbType.DVBS, "s",
                    DvbType.DVBT, "t");
            // country
            int countryIdx = _countryList.getSelectedItemPosition();
            if (countryIdx == AdapterView.INVALID_POSITION) {
                // TODO localize error
                Utils.error(ScanActivity.this, "no country selected");
                cancel(true);
                return;
            }
            _country = WSCAN_COUNTRIES[countryIdx][0];
            // fileName
            String fileName = _fileNameView.getText().toString();
            if (StringUtils.isNullOrEmpty(fileName)) {
                // TODO localize errror
                Utils.error(ScanActivity.this,
                        "please enter file name for channels file");
                cancel(true);
                return;
            }
            _outFile = Utils.getConfigsFile(ScanActivity.this, fileName + ".conf");
            // TODO redesign print of type and country
            _textView.append(_type + "/" + _country);
            _textView.append(Utils.NEWLINE);
        }

        @Override
        protected void onCancelled() {
            setEnabled(true);
        }

        @Override
        protected void onProgressUpdate(CharSequence... values) {
            if (isCancelled())
                return;
            _textView.append(values[0]);
            _textView.append(Utils.NEWLINE);
            _scrollView.post(new Runnable() {
                public void run() {
                    _scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                if (result != null) {
                    try {
                        StringBuilder buf = new StringBuilder();
                        BufferedReader scanned = new BufferedReader(
                                new StringReader(result));
                        String line;
                        while ((line = scanned.readLine()) != null) {
                            String[] params = line.split(":");
                            switch (_dvbType) {
                                case ATSC:
                                    if (params.length != 6) {
                                        throw new IOException(
                                                "invalid ATSC params count[" + params.length + "]");
                                    }
                                    buf.append(params[0]); // name
                                    buf.append(':');
                                    buf.append(params[1]); // freq
                                    buf.append(':');
                                    buf.append(params[5]); // sid
                                    break;
                                case DVBT:
                                    if (params.length != 13) {
                                        throw new IOException(
                                                "invalid DVB-T params count[" + params.length + "]");
                                    }
                                    buf.append(params[0]); // name
                                    buf.append(':');
                                    buf.append(params[1]); // freq
                                    buf.append(':');
                                    buf.append(params[12]); // sid
                                    break;
                            }
                            buf.append('\n');
                        }
                        FileOutputStream out = new FileOutputStream(_outFile);
                        out.write(buf.toString().getBytes());
                    } catch (IOException e) {
                        // TODO localize error
                        Utils.error(ScanActivity.this, "failed to save channels file", e);
                    }
                }
            } finally {
                setEnabled(true);
                _scanTask = null;
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Process wscan = ProcessUtils.runBinary(ScanActivity.this, WSCAN, "-f", _type,
                        "-c", _country, "-X");
                Reader input = new InputStreamReader(wscan.getErrorStream());
                BufferedReader reader = new BufferedReader(input);
                Integer exitCode = null;
                String line;
                while (!isCancelled()
                        && ((line = reader.readLine()) != null || (exitCode = ProcessUtils
                                .checkExitCode(wscan)) == null)) {
                    if (line == null) {
                        Thread.sleep(250);
                    } else {
                        publishProgress(line);
                    }
                }
                if (exitCode == null) {
                    wscan.destroy();
                } else if (exitCode == 0) {
                    return ProcessUtils.readStdOut(wscan);
                } else {
                    // TODO localization
                    Log.e(TAG, "wscan failed (" + exitCode + ")");
                    Utils.error(ScanActivity.this, "wscan failed (" + exitCode + ")");
                }
            } catch (Throwable t) {
                Log.e(TAG, "wscan", t);
                Utils.error(ScanActivity.this, "wscan", t);
            }
            return null;
        }
    }
}
