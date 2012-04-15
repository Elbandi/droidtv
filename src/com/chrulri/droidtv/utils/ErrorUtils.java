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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.chrulri.droidtv.R;

public final class ErrorUtils {
    static final String TAG = ErrorUtils.class.getName();

    private static class ErrorDialog extends Dialog {

        public ErrorDialog(Context context, CharSequence msg, Throwable t,
                final View.OnClickListener onClickListener) {
            super(context);
            setContentView(R.layout.error);
            StringBuilder buf = new StringBuilder();
            buf.append(msg);
            buf.append(StringUtils.NEWLINE);
            if (t != null) {
                buf.append(StringUtils.getStackTrace(t));
            }
            TextView textView = (TextView) findViewById(R.id.error_textView);
            textView.setText(buf.toString());
            Button button = (Button) findViewById(R.id.error_reportButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    if (onClickListener != null) {
                        onClickListener.onClick(v);
                    }
                }
            });
        }
    }

    public static void error(Activity ctx, CharSequence msg) {
        error(ctx, msg, null, null);
    }

    public static void error(Activity ctx, CharSequence msg, Throwable t) {
        error(ctx, msg, t, null);
    }

    public static void error(Activity ctx, CharSequence msg, OnClickListener onClickListener) {
        error(ctx, msg, null, onClickListener);
    }

    public static void error(final Activity ctx, final CharSequence msg,
            final Throwable t, final OnClickListener onClickListener) {
        ctx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ErrorDialog dlg = new ErrorDialog(ctx, msg, t, onClickListener);
                dlg.show();
            }
        });
    }
}
