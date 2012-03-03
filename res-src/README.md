HOWTO BUILD W_SCAN
==================

* ENTER THIS DIRECTORY...

## DOWNLOAD SOURCES ##
```bash
$ wget "http://wirbel.htpc-forum.de/w_scan/w_scan-20110329.tar.bz2"
$ wget "http://gitweb.braice.net/gitweb?p=mumudvb;a=snapshot;h=ee9615c9c8cbcb3523e4f8d9d688b1c2354a0e0a;sf=tgz" -O mumudvb-1.7.tgz
```

## BUILD W_SCAN ##
```bash
$ tar xf w_scan-20110329.tar.bz2
$ ./build_wscan.sh
```
the compiled w_scan binary will then be copied to /res/raw/


## BUILD MUMUDVB ##
```bash
$ tar xf mumudvb-1.7.tgz
$ patch -p1 -d mumudvb < mumudvb.patch
$ ./build_mumudvb.sh
```
the compiled mumudvb binary will then be copied to /res/raw/

