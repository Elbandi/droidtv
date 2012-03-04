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

  static final String MUMUDVB_PARAM_FREQ = "freq";

  private static int tryParseInt(String str, String paramName)
      throws IOException {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      throw new IOException(
          "error while parsing " + paramName + " (" + str + ")");
    }
  }

  private static void printConfig(PrintWriter writer, String param,
      Object... values) {
    writer.print(param);
    writer.print('=');
    for (int i = 0; i < values.length; i++) {
      if (i > 0)
        writer.print(' ');
      writer.print(values[i]);
    }
    writer.println();
  }

  public static int parse(String channelConfig, PrintWriter writer)
      throws IOException {
    // sNAME/iFREQ/iServiceID
    String[] params = channelConfig.split(":");

    // check config length
    if (params.length != 3) {
      throw new IOException("invalid DVB params count[" + params.length + "]");
    }

    // parse config
    int freq = tryParseInt(params[1], "frequency") / 1000000; // Hz -> MHz
    int sid = tryParseInt(params[2], "service ID");

    // print config
    printConfig(writer, MUMUDVB_PARAM_FREQ, freq);

    return sid;
  }

}
