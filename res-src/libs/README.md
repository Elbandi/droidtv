HOWTO BUILD BINARIES
====================

* ENTER THIS DIRECTORY...

## DOWNLOAD SOURCES ##
```bash
$ wget "http://wirbel.htpc-forum.de/w_scan/w_scan-20110329.tar.bz2"
$ wget "http://download.videolan.org/pub/videolan/bitstream/1.0/bitstream-1.0.tar.bz2"
$ wget "http://downloads.videolan.org/pub/videolan/dvblast/2.1/dvblast-2.1.0.tar.bz2"
$ wget -O dvb-apps-7de0663facd9.tar.bz2 "http://linuxtv.org/hg/dvb-apps/archive/7de0663facd9.tar.bz2"
```

## BUILD W_SCAN ##
```bash
$ tar xf w_scan-20110329.tar.bz2
$ ./build_wscan.sh
```
the compiled w_scan binary will then be copied to /res/raw/


## BUILD DVBLAST ##
```bash
$ tar xf bitstream-1.0.tar.bz2
$ tar xf dvblast-2.1.0.tar.bz2
$ patch -p1 -d dvblast-2.1.0 < patches/dvblast-2.1.0.patch
$ ./build_dvblast.sh
```
the compiled dvblast binaries will then be copied to /res/raw/

## BUILD DVB APPS ##
```bash
$ tar xfj dvb-apps-7de0663facd9.tar.bz2
$ patch -p1 -d dvb-apps-7de0663facd9 < patches/dvb-apps.patch
$ ./build_dvbapps.sh
```
the compiled tzap binaries will then be copied to /res/raw/

