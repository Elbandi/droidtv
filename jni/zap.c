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
#include <jni.h>
#include <stdint.h>
#include "log.h"

#include <linux/dvb/frontend.h>
#include "com_chrulri_droidtv_DvbTuner.h"

JNIEXPORT jint JNICALL Java_com_chrulri_droidtv_DvbTuner_nativeZapStart
  (JNIEnv *env, jobject this, jint dvbType, jstring jChannelConf, jstring jChannelName){
	const char *channelConf = (*env)->GetStringUTFChars(env, jChannelConf, 0);
	const char *channelName = (*env)->GetStringUTFChars(env, jChannelName, 0);
	fe_type_t fetype;
	switch(dvbType){
	case com_chrulri_droidtv_DvbTuner_TYPE_ATSC:
		fetype = FE_ATSC;
		break;
//	case com_chrulri_droidtv_DvbTuner_TYPE_DVBC:
//		fetype = FE_QAM;
//		break;
//	case com_chrulri_droidtv_DvbTuner_TYPE_DVBS:
//		fetype = FE_QPSK;
//		break;
	case com_chrulri_droidtv_DvbTuner_TYPE_DVBT:
		fetype = FE_OFDM;
		break;
	default:
		return -1;
	}
	int ret = dvb_start(channelConf, channelName, fetype);
	(*env)->ReleaseStringUTFChars(env, jChannelConf, channelConf);
	(*env)->ReleaseStringUTFChars(env, jChannelName, channelName);
	return ret;
}

JNIEXPORT void JNICALL Java_com_chrulri_droidtv_DvbTuner_nativeZapStop
  (JNIEnv *env, jobject this, jint dvbType){
	dvb_stop();
}

JNIEXPORT jint JNICALL Java_com_chrulri_droidtv_DvbTuner_nativeZapStatus
  (JNIEnv *env, jobject this, jint dvbType, jobject jStatus){
	fe_status_t status = 0;
	uint16_t signal = 0, snr = 0;
	uint32_t ber = 0, unc = 0;
	int ret = dvb_status(&status, &signal, &snr, &ber, &unc);
	if(ret == 0){
		// set status
		jclass clazz = (*env)->GetObjectClass(env, jStatus);
		jmethodID methodSet = (*env)->GetMethodID(env, clazz, "set", "(IIIII)V");
		if (!methodSet){
			LOGE("failed to get DvbTuner.ZapStatus.set(...) method");
			return -10;
		}
		(*env)->CallVoidMethod(env, jStatus, methodSet, status, signal, snr, ber, unc);
	}
	return ret;
}
