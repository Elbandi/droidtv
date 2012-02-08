#ifndef _LOG_H_
#define _LOG_H_

#include <android/log.h>

#define LOG_TAG			"LiveTvNative"
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
