#******************************************************************************
#*  DroidTV, live TV on Android devices with host USB port and a DVB tuner    *
#*  Copyright (C) 2012  Christian Ulrich <chrulri@gmail.com>                  *
#*                                                                            *
#*  This program is free software: you can redistribute it and/or modify      *
#*  it under the terms of the GNU General Public License as published by      *
#*  the Free Software Foundation, either version 3 of the License, or         *
#*  (at your option) any later version.                                       *
#*                                                                            *
#*  This program is distributed in the hope that it will be useful,           *
#*  but WITHOUT ANY WARRANTY; without even the implied warranty of            *
#*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
#*  GNU General Public License for more details.                              *
#*                                                                            *
#*  You should have received a copy of the GNU General Public License         *
#*  along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
#******************************************************************************
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE     := zap
LOCAL_SRC_FILES  := dvbutil.c dvbparsers.c dvbcontrol.c zap.c
#LOCAL_CFLAGS     := -DDEBUG
LOCAL_LDLIBS     := -lm -llog
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
include $(BUILD_SHARED_LIBRARY)
