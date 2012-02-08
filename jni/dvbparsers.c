#include "dvbutil.h"

static const Param inversion_list [] = {
	{ "INVERSION_OFF", INVERSION_OFF },
	{ "INVERSION_ON", INVERSION_ON },
	{ "INVERSION_AUTO", INVERSION_AUTO }
};

static const Param bw_list [] = {
	{ "BANDWIDTH_6_MHZ", BANDWIDTH_6_MHZ },
	{ "BANDWIDTH_7_MHZ", BANDWIDTH_7_MHZ },
	{ "BANDWIDTH_8_MHZ", BANDWIDTH_8_MHZ }
};

static const Param fec_list [] = {
	{ "FEC_1_2", FEC_1_2 },
	{ "FEC_2_3", FEC_2_3 },
	{ "FEC_3_4", FEC_3_4 },
	{ "FEC_4_5", FEC_4_5 },
	{ "FEC_5_6", FEC_5_6 },
	{ "FEC_6_7", FEC_6_7 },
	{ "FEC_7_8", FEC_7_8 },
	{ "FEC_8_9", FEC_8_9 },
	{ "FEC_AUTO", FEC_AUTO },
	{ "FEC_NONE", FEC_NONE }
};

static const Param guard_list [] = {
	{"GUARD_INTERVAL_1_16", GUARD_INTERVAL_1_16},
	{"GUARD_INTERVAL_1_32", GUARD_INTERVAL_1_32},
	{"GUARD_INTERVAL_1_4", GUARD_INTERVAL_1_4},
	{"GUARD_INTERVAL_1_8", GUARD_INTERVAL_1_8},
	{"GUARD_INTERVAL_AUTO", GUARD_INTERVAL_AUTO}
};

static const Param hierarchy_list [] = {
	{ "HIERARCHY_1", HIERARCHY_1 },
	{ "HIERARCHY_2", HIERARCHY_2 },
	{ "HIERARCHY_4", HIERARCHY_4 },
	{ "HIERARCHY_NONE", HIERARCHY_NONE },
	{ "HIERARCHY_AUTO", HIERARCHY_AUTO }
};

static const Param constellation_list [] = {
	{ "QPSK", QPSK },
	{ "QAM_128", QAM_128 },
	{ "QAM_16", QAM_16 },
	{ "QAM_256", QAM_256 },
	{ "QAM_32", QAM_32 },
	{ "QAM_64", QAM_64 },
	{ "QAM_AUTO", QAM_AUTO }
};

static const Param transmissionmode_list [] = {
	{ "TRANSMISSION_MODE_2K", TRANSMISSION_MODE_2K },
	{ "TRANSMISSION_MODE_8K", TRANSMISSION_MODE_8K },
	{ "TRANSMISSION_MODE_AUTO", TRANSMISSION_MODE_AUTO }
};

static const Param modulation_list [] = {
	{ "8VSB", VSB_8 },
	{ "16VSB", VSB_16 },
	{ "QAM_64", QAM_64 },
	{ "QAM_256", QAM_256 },
};

#define LIST_SIZE(x) sizeof(x)/sizeof(Param)

int atsc_parse(const char *fname, const char *channel,
	  struct dvb_frontend_parameters *frontend, int *vpid, int *apid,
	  int *sid)
{
	int fd;
	int err;
	int tmp;

	if ((fd = open(fname, O_RDONLY | O_NONBLOCK)) < 0) {
		LOGE("could not open file '%s'", fname);
		return -1;
	}

	if (find_channel(fd, channel) < 0) {
		LOGE("could not find channel '%s' in channel list", channel);
		return -2;
	}

	if ((err = try_parse_int(fd, &tmp, "frequency"))) {
		return -3;
	}

	frontend->frequency = tmp;

	if ((err = try_parse_param(fd,
				   modulation_list, LIST_SIZE(modulation_list),
				   &tmp, "modulation"))) {
		return -4;
	}
	frontend->u.vsb.modulation = tmp;

	if ((err = try_parse_int(fd, vpid, "Video PID"))) {
		return -5;
	}

	if ((err = try_parse_int(fd, apid, "Audio PID"))) {
		return -6;
	}

	if ((err = try_parse_int(fd, sid, "Service ID"))) {
		return -7;
	}

	close(fd);
	return 0;
}

int dvbt_parse(const char *fname, const char *channel,
	  struct dvb_frontend_parameters *frontend, int *vpid, int *apid,
	  int *sid)
{
	int fd;
	int err;
	int tmp;

	if ((fd = open(fname, O_RDONLY | O_NONBLOCK)) < 0) {
		LOGE("could not open file '%s'", fname);
		return -1;
	}

	if (find_channel(fd, channel) < 0) {
		LOGE("could not find channel '%s' in channel list", channel);
		return -2;
	}

	if ((err = try_parse_int(fd, &tmp, "frequency"))) {
		return -3;
	}
	frontend->frequency = tmp;

	if ((err = try_parse_param(fd,
				   inversion_list, LIST_SIZE(inversion_list),
				   &tmp, "inversion"))) {
		return -4;
	}
	frontend->inversion = tmp;

	if ((err = try_parse_param(fd, bw_list, LIST_SIZE(bw_list),
				   &tmp, "bandwidth"))) {
		return -5;
	}
	frontend->u.ofdm.bandwidth = tmp;

	if ((err = try_parse_param(fd, fec_list, LIST_SIZE(fec_list),
				   &tmp, "code_rate_HP"))) {
		return -6;
	}
	frontend->u.ofdm.code_rate_HP = tmp;
	if (check_fec(&frontend->u.ofdm.code_rate_HP)) {
		return -6;
	}

	if ((err = try_parse_param(fd, fec_list, LIST_SIZE(fec_list),
				   &tmp, "code_rate_LP"))) {
		return -7;
	}
	frontend->u.ofdm.code_rate_LP = tmp;
	if (check_fec(&frontend->u.ofdm.code_rate_LP)) {
		return -7;
	}

	if ((err = try_parse_param(fd, constellation_list,
				   LIST_SIZE(constellation_list),
				   &tmp, "constellation")))
		return -8;
	frontend->u.ofdm.constellation = tmp;

	if ((err = try_parse_param(fd, transmissionmode_list,
				   LIST_SIZE(transmissionmode_list),
				   &tmp, "transmission_mode")))
		return -9;
	frontend->u.ofdm.transmission_mode = tmp;

	if ((err = try_parse_param(fd, guard_list, LIST_SIZE(guard_list),
				   &tmp, "guard_interval")))
		return -10;
	frontend->u.ofdm.guard_interval = tmp;

	if ((err = try_parse_param(fd, hierarchy_list,
				   LIST_SIZE(hierarchy_list),
				   &tmp, "hierarchy_information")))
		return -11;
	frontend->u.ofdm.hierarchy_information = tmp;

	if ((err = try_parse_int(fd, vpid, "Video PID")))
		return -12;

	if ((err = try_parse_int(fd, apid, "Audio PID")))
		return -13;

	if ((err = try_parse_int(fd, sid, "Service ID")))
	    return -14;

	close(fd);
	return 0;
}
