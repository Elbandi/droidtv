
#define _FILE_OFFSET_BITS 64
#define _LARGEFILE_SOURCE 1
#define _LARGEFILE64_SOURCE 1

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <stdio.h>
#include <fcntl.h>
#include <ctype.h>
#include <errno.h>
#include <signal.h>

#include <linux/dvb/frontend.h>
#include <linux/dvb/dmx.h>

#include "log.h"

typedef struct {
	char *name;
	int value;
} Param;
