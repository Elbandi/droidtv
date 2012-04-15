#!/bin/sh
# droidtv/res-src/libs/build_dvblast.sh
# Compiles DVBlast for Android
# Make sure you have ANDROID_NDK defined in .bashrc or .bash_profile

API=8
SRC_DIR_DVBLAST="`pwd`/dvblast-2.1.0"
SRC_DIR_BITSTREAM="`pwd`/bitstream-1.0"
TARGET_FILE="`pwd`/../../res/raw/dvblast_2_1_0.bin"
TARGETCTL_FILE="`pwd`/../../res/raw/dvblastctl_2_1_0.bin"
INSTALL_DIR="`pwd`/../../bin"
INCLUDE_DIR="`pwd`/include"


export PATH="$ANDROID_NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/:$PATH"
export SYS_ROOT="$ANDROID_NDK/platforms/android-$API/arch-arm/"
export CC="arm-linux-androideabi-gcc --sysroot=$SYS_ROOT"
export LD="arm-linux-androideabi-ld"
export AR="arm-linux-androideabi-ar"
export RANLIB="arm-linux-androideabi-ranlib"
export STRIP="arm-linux-androideabi-strip"
export CFLAGS="-I$INSTALL_DIR/include -I$INCLUDE_DIR"
export LDLIBS="-lc -lgcc"
export PREFIX="$INSTALL_DIR"

mkdir -p $INSTALL_DIR

cd $SRC_DIR_BITSTREAM
make install

cd $SRC_DIR_DVBLAST
make
make install

cp $INSTALL_DIR/bin/dvblast $TARGET_FILE
cp $INSTALL_DIR/bin/dvblastctl $TARGETCTL_FILE

exit 0

