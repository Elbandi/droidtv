#include "dvbutil.h"

static
int parse_param (int fd, const Param * plist, int list_size, int *param)
{
	char c;
	int character = 0;
	int _index = 0;

	while (1) {
		if (read(fd, &c, 1) < 1)
			return -1;	/*  EOF? */

		if ((c == ':' || c == '\n')
		    && plist->name[character] == '\0')
			break;

		while (toupper(c) != plist->name[character]) {
			_index++;
			plist++;
			if (_index >= list_size)	 /*  parse error, no valid */
				return -2;	 /*  parameter name found  */
		}
		character++;
	}
	*param = plist->value;
	return 0;
}


static
int parse_int(int fd, int *val)
{
	char number[11];	/* 2^32 needs 10 digits... */
	int character = 0;

	while (1) {
		if (read(fd, &number[character], 1) < 1)
			return -1;	/*  EOF? */

		if (number[character] == ':' || number[character] == '\n') {
			number[character] = '\0';
			break;
		}

		if (!isdigit(number[character]))
			return -2;	/*  parse error, not a digit... */

		character++;

		if (character > 10)	/*  overflow, number too big */
			return -3;	/*  to fit in 32 bit */
	};

	errno = 0;
	*val = strtol(number, NULL, 10);
	if (errno == ERANGE)
		return -4;

	return 0;
}

int find_channel(int fd, const char *channel)
{
	int character = 0;

	while (1) {
		char c;

		if (read(fd, &c, 1) < 1)
			return -1;	/*  EOF! */

		if ( '\n' == c ) /* start of line */
			character = 0;
		else if ( character >= 0 ) { /* we are in the namefield */

			if (c == ':' && channel[character] == '\0')
				break;

			if (toupper(c) == toupper(channel[character]))
				character++;
			else
				character = -1;
		}
	};

	return 0;
}

int try_parse_int(int fd, int *val, const char *pname)
{
	int err;

	err = parse_int(fd, val);

	if (err)
		LOGE("error while parsing %s (%s)", pname,
		      err == -1 ? "end of file" :
		      err == -2 ? "not a number" : "number too big");

	return err;
}

int try_parse_param(int fd, const Param * plist, int list_size, int *param,
		    const char *pname)
{
	int err;

	err = parse_param(fd, plist, list_size, param);

	if (err)
		LOGE("error while parsing %s (%s)", pname,
		      err == -1 ? "end of file" : "syntax error");

	return err;
}

int check_fec(fe_code_rate_t *fec)
{
	switch (*fec)
	{
	case FEC_NONE:
		*fec = FEC_AUTO;
	case FEC_AUTO:
	case FEC_1_2:
	case FEC_2_3:
	case FEC_3_4:
	case FEC_5_6:
	case FEC_7_8:
		return 0;
	default:
		;
	}
	return 1;
}

int setup_frontend (int fe_fd, struct dvb_frontend_parameters *frontend, fe_type_t type)
{
	struct dvb_frontend_info fe_info;

	if (ioctl(fe_fd, FE_GET_INFO, &fe_info) < 0) {
		LOGE("ioctl FE_GET_INFO failed");
		return -1;
	}

	if (fe_info.type != type) {
		return -2;
	}

	LOGD("tuning to %i Hz\n", frontend->frequency);

	if (ioctl(fe_fd, FE_SET_FRONTEND, frontend) < 0) {
		LOGE("ioctl FE_SET_FRONTEND failed");
		return -3;
	}

	return 0;
}

int set_pesfilter(int dmxfd, int pid, int pes_type, int dvr)
{
    struct dmx_pes_filter_params pesfilter;

    /* ignore this pid to allow radio services */
    if (pid < 0 || pid >= 0x1fff ||
		(pid == 0 && pes_type != DMX_PES_OTHER))
    	return 0;

    if (dvr) {
		int buffersize = 64 * 1024;
		if (ioctl(dmxfd, DMX_SET_BUFFER_SIZE, buffersize) == -1)
			LOGE("DMX_SET_BUFFER_SIZE failed");
    }

    pesfilter.pid = pid;
    pesfilter.input = DMX_IN_FRONTEND;
    pesfilter.output = dvr ? DMX_OUT_TS_TAP : DMX_OUT_DECODER;
    pesfilter.pes_type = pes_type;
    pesfilter.flags = DMX_IMMEDIATE_START;

    if (ioctl(dmxfd, DMX_SET_PES_FILTER, &pesfilter) == -1) {
    	LOGE("DMX_SET_PES_FILTER failed "
    			"(PID = 0x%04x): %d %m\n", pid, errno);
		return -1;
    }

    return 0;
}

int get_pmt_pid(const char *dmxdev, int sid)
{
    int patfd, count;
    int pmt_pid = 0;
    int patread = 0;
    int section_length;
    unsigned char buft[4096];
    unsigned char *buf = buft;
    struct dmx_sct_filter_params f;

    memset(&f, 0, sizeof(f));
    f.pid = 0;
    f.filter.filter[0] = 0x00;
    f.filter.mask[0] = 0xff;
    f.timeout = 0;
    f.flags = DMX_IMMEDIATE_START | DMX_CHECK_CRC;

    if ((patfd = open(dmxdev, O_RDWR)) < 0) {
		LOGE("openening pat demux failed");
		return -1;
    }

    if (ioctl(patfd, DMX_SET_FILTER, &f) == -1) {
    	LOGE("ioctl DMX_SET_FILTER failed");
		close(patfd);
		return -1;
    }

    while (!patread){
		if (((count = read(patfd, buf, sizeof(buft))) < 0) && errno == EOVERFLOW)
			count = read(patfd, buf, sizeof(buft));
		if (count < 0) {
			LOGE("read_sections: read error");
			close(patfd);
			return -1;
		}

		section_length = ((buf[1] & 0x0f) << 8) | buf[2];
		if (count != section_length + 3)
			continue;

		buf += 8;
		section_length -= 8;

		patread = 1; /* assumes one section contains the whole pat */
		while (section_length > 0) {
			int service_id = (buf[0] << 8) | buf[1];
			if (service_id == sid) {
				pmt_pid = ((buf[2] & 0x1f) << 8) | buf[3];
				section_length = 0;
			}
			buf += 4;
			section_length -= 4;
		}
    }

    close(patfd);
    return pmt_pid;
}
