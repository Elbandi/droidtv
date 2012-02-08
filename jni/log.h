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
#ifndef _LOG_H_
#define _LOG_H_

#include <android/log.h>

#define LOG_TAG			"DroidTVnative"
#define LOG(lvl,...)	__android_log_print(lvl,LOG_TAG,__VA_ARGS__)
#define LOGE(...)		LOG(ANDROID_LOG_ERROR,__VA_ARGS__)
#ifdef DEBUG
	#define LOGD(...)	LOG(ANDROID_LOG_DEBUG,__VA_ARGS__)
	#define LOGV(...)	LOG(ANDROID_LOG_VERBOSE,__VA_ARGS__)
#else
	#define	LOGD(...)	while(0)
	#define	LOGV(...)	while(0)
#endif

#define ERROR_UNIMPLEMENTED			-1
#define ERROR_OK					0
// TODO ERROR_...					...

#endif
