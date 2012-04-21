#!/bin/sh
# droidtv/res-src/libs/build_wscan.sh
# Compiles tzap for Android
# Make sure you have ANDROID_NDK defined in .bashrc or .bash_profile

API=8
SRC_DIR="`pwd`/dvb-apps-7de0663facd9"
#TARGET_FILE_SCAN="`pwd`/../../res/raw/scan_1355.bin"
TARGET_FILE_TZAP="`pwd`/../../res/raw/tzap_1355.bin"
#TARGET_FILE_FEMON="`pwd`/../../res/raw/femon_1355.bin"
#TARGET_FILE_GNUTV="`pwd`/../../res/raw/gnutv_1355.bin"

INSTALL_DIR="`pwd`/../../bin"
INCLUDE_DIR="`pwd`/include"

cd $SRC_DIR

export PATH="$ANDROID_NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/:$PATH"
export SYS_ROOT="$ANDROID_NDK/platforms/android-$API/arch-arm/"
export CC="arm-linux-androideabi-gcc --sysroot=$SYS_ROOT"
export LD="arm-linux-androideabi-ld"
export AR="arm-linux-androideabi-ar"
export RANLIB="arm-linux-androideabi-ranlib"
export STRIP="arm-linux-androideabi-strip"
export CFLAGS="-I$INCLUDE_DIR"
export LIBS="-lc -lgcc"

mkdir -p $INSTALL_DIR

make clean
make static=1
make install DESTDIR=$INSTALL_DIR

#cp $INSTALL_DIR/usr/bin/scan $TARGET_FILE_SCAN
cp $INSTALL_DIR/usr/bin/tzap $TARGET_FILE_TZAP
#cp $INSTALL_DIR/usr/bin/femon $TARGET_FILE_FEMON
#cp $INSTALL_DIR/usr/bin/gnutv $TARGET_FILE_GNUTV


exit 0

