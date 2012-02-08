LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE     := zap
LOCAL_SRC_FILES  := dvbutil.c dvbparsers.c dvbcontrol.c zap.c
#LOCAL_CFLAGS     := -DDEBUG
LOCAL_LDLIBS     := -lm -llog
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
include $(BUILD_SHARED_LIBRARY)
