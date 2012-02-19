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

import java.io.IOException;
import java.io.PrintWriter;


class DvbUtils {
	static final String TAG = DvbUtils.class.getName();

	/**
	 * See {@link http://mumudvb.braice.net/mumudvb/doc/mumudvb-1.6.1/README_CONF.html#_parameters_specific_to_terrestrial_dvb_t}
	 */
	static final String PARAMS_BW_LIST [][] = {
		{ "BANDWIDTH_6_MHZ", "6MHz" },
		{ "BANDWIDTH_7_MHZ", "7MHz" },
		{ "BANDWIDTH_8_MHZ", "8MHz" },
	};

	/**
	 * See {@link http://mumudvb.braice.net/mumudvb/doc/mumudvb-1.6.1/README_CONF.html#_parameters_specific_to_terrestrial_dvb_t}
	 */
	static final String PARAMS_FEC_LIST [][] = {
		{ "FEC_1_2",	"1/2"	},
		{ "FEC_2_3",	"2/3"	},
		{ "FEC_3_4",	"3/4"	},
		{ "FEC_4_5",	"4/5"	},
		{ "FEC_5_6",	"5/6"	},
		{ "FEC_6_7",	"6/7"	},
		{ "FEC_7_8",	"7/8"	},
		{ "FEC_8_9",	"8/9"	},
		{ "FEC_AUTO",	"auto"	},
		{ "FEC_NONE",	"none"	},
	};

	/**
	 * See {@link http://mumudvb.braice.net/mumudvb/doc/mumudvb-1.6.1/README_CONF.html#_parameters_specific_to_terrestrial_dvb_t}
	 */
	static final String PARAMS_GUARD_LIST [][] = {
		{"GUARD_INTERVAL_1_16",	"1/16"	},
		{"GUARD_INTERVAL_1_32", "1/32"	},
		{"GUARD_INTERVAL_1_4",	"1/4"	},
		{"GUARD_INTERVAL_1_8",	"1/8"	},
		{"GUARD_INTERVAL_AUTO",	"auto"	},
	};

	static final String PARAMS_CONSTELLATION_LIST [][] = {
		{ "QPSK",		"QPSK"		},
		{ "QAM_128",	"QAM128"	},
		{ "QAM_16",		"QAM16"		},
		{ "QAM_256",	"QAM256"	},
		{ "QAM_32",		"QAM32"		},
		{ "QAM_64",		"QAM64"		},
		{ "QAM_AUTO",	"QAMAUTO"	},
	};

	/**
	 * See {@link http://mumudvb.braice.net/mumudvb/doc/mumudvb-1.6.1/README_CONF.html#_parameters_specific_to_terrestrial_dvb_t}
	 */
	static final String PARAMS_TRANSMISSIONMODE_LIST [][] = {
		{ "TRANSMISSION_MODE_2K",	"2k"	},
		{ "TRANSMISSION_MODE_8K",	"8k"	},
		{ "TRANSMISSION_MODE_AUTO",	"auto"	},
	};

	/**
	 * See {@link http://mumudvb.braice.net/mumudvb/doc/mumudvb-1.6.1/README_CONF.html#_parameters_specific_to_atsc_cable_or_terrestrial}
	 */
	static final String PARAMS_MODULATION_LIST [][] = {
		{ "8VSB",	"vsb8"	},
		{ "16VSB",	"vsb16"	},
		{ "QAM_64",	"qam64"	},
		{ "QAM_256","qam256"},
	};

	static final String MUMUDVB_PARAM_CHANNEL_NEXT = "channel_next";
	static final String MUMUDVB_PARAM_NAME = "name";
	static final String MUMUDVB_PARAM_PIDS = "pids";
	static final String MUMUDVB_PARAM_SID = "service_id";
	static final String MUMUDVB_PARAM_FREQ = "freq";
	static final String MUMUDVB_PARAM_MODULATION = "modulation";
	static final String MUMUDVB_PARAM_BANDWIDTH = "bandwidth";
	static final String MUMUDVB_PARAM_TRANSMISSIONMODE = "trans_mode";
	static final String MUMUDVB_PARAM_GUARDINTERVAL = "guardinterval";
	static final String MUMUDVB_PARAM_CODERATE = "coderate";

	private static int tryParseInt(String str, String paramName) throws IOException {
		try {
			return Integer.parseInt(str);
		} catch(NumberFormatException e) {
			throw new IOException("error while parsing " + paramName + " (" + str + ")");
		}
	}

	private static String tryParseParam(String str, String paramName, String[][] paramList) throws IOException {
		for(String[] param : paramList) {
			if(param[0].equals(str)) {
				return param[1];
			}
		}
		throw new IOException("error while parsing " + paramName + " (" + str + ")");
	}

	private static void printConfig(PrintWriter writer,
			String param, Object... values) {
		writer.print(param);
		writer.print('=');
		for(int i = 0; i < values.length; i++) {
			if(i > 0) writer.print(' ');
			writer.print(values[i]);
		}
		writer.println();
	}

	private static void printChannel(PrintWriter writer, String name, int vpid, int apid, int sid) {
		writer.println(MUMUDVB_PARAM_CHANNEL_NEXT);
		printConfig(writer, MUMUDVB_PARAM_NAME, name);
		printConfig(writer, MUMUDVB_PARAM_PIDS, vpid, apid);
		printConfig(writer, MUMUDVB_PARAM_SID, sid);
	}

	public static void parseATSC(String channelConfig, PrintWriter writer) throws IOException {
		// ATSC: sNAME/iFREQ/modulation_list/iVideoPID/iAudioPID/iServiceID
		String[] params = channelConfig.split(":");
		// check config length
		if(params.length != 6) {
			throw new IllegalArgumentException("channelConfig params: 6 != " + params.length);
		}
		int i = 0;
		// parse config
		String name = params[i++];
		int freq = tryParseInt(params[i++], "frequency") / 1000000; // Hz -> MHz
		String mod = tryParseParam(params[i++], "modulation", PARAMS_MODULATION_LIST);
		int vpid = tryParseInt(params[i++], "Video PID");
		int apid = tryParseInt(params[i++], "Audio PID");
		int sid = tryParseInt(params[i++], "Service ID");
		// print config
		printConfig(writer, MUMUDVB_PARAM_FREQ, freq);
		printConfig(writer, MUMUDVB_PARAM_MODULATION, mod);
		printChannel(writer, name, vpid, apid, sid);
	}

	public static void parseDVBT(String channelConfig, PrintWriter writer) throws IOException {
		// DVBT: sNAME/iFREQ/inversion_list/bw_list/fec_list/fec_list/constellation_list/transmissionmode_list/guard_list/hierarchy_list/iVideoPID/iAudioPID/iServiceID
		String[] params = channelConfig.split(":");
		// check config length
		if(params.length != 13) {
			throw new IllegalArgumentException("channelConfig params: 13 != " + params.length);
		}
		int i = 0;
		// parse config
		String name = params[i++];
		int freq = tryParseInt(params[i++], "frequency") / 1000000; // Hz -> MHz
		i++; // ignore inversion
		String bw = tryParseParam(params[i++], "bandwidth", PARAMS_BW_LIST);
		String fecHP = tryParseParam(params[i++], "code_rate_HP", PARAMS_FEC_LIST);
		i++; // ignore fecLP // String fecLP = tryParseParam(params[i++], "code_rate_LP", PARAMS_FEC_LIST);
		String mod = tryParseParam(params[i++], "constellation", PARAMS_CONSTELLATION_LIST);
		String trans = tryParseParam(params[i++], "transmission_mode", PARAMS_TRANSMISSIONMODE_LIST);
		String guard = tryParseParam(params[i++], "guard_interval", PARAMS_GUARD_LIST);
		i++; // ignore hierarchy information
		int vpid = tryParseInt(params[i++], "Video PID");
		int apid = tryParseInt(params[i++], "Audio PID");
		int sid = tryParseInt(params[i++], "Service ID");
		// print config
		printConfig(writer, MUMUDVB_PARAM_FREQ, freq);
		printConfig(writer, MUMUDVB_PARAM_MODULATION, mod);
		printConfig(writer, MUMUDVB_PARAM_BANDWIDTH, bw);
		printConfig(writer, MUMUDVB_PARAM_TRANSMISSIONMODE, trans);
		printConfig(writer, MUMUDVB_PARAM_GUARDINTERVAL, guard);
		printConfig(writer, MUMUDVB_PARAM_CODERATE, fecHP);
		printChannel(writer, name, vpid, apid, sid);
	}
}
