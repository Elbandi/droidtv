HOWTO BUILD W_SCAN
==================

ENTER THIS DIRECTORY..

## DOWNLOAD SOURCES ##
```linux shell
$ wget "http://wirbel.htpc-forum.de/w_scan/w_scan-20110329.tar.bz2"
$ wget "http://gitweb.braice.net/gitweb?p=mumudvb;a=snapshot;h=fa4b8a9129d032a1942f90375c86ec0227e35be0;sf=tgz" -O mumudvb-1.6.1b.tgz
```

## BUILD W_SCAN ##
```linux shell
$ tar xf w_scan-20110329.tar.bz2
$ ./build_wscan.sh
```
the compiled w_scan binary will then be copied to /res/raw/


## BUILD MUMUDVB ##
```linux shell
$ tar xf mumudvb-1.6.1b.tgz
$ patch -p1 -d mumudvb < mumudvb.patch
$ ./build_mumudvb.sh
```
the compiled mumudvb binary will then be copied to /res/raw/

