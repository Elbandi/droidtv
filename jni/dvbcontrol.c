#include "dvbutil.h"

static int frontend_fd;
static int video_fd;
static int audio_fd;
static int pmt_fd;
static int pat_fd;

int dvb_start(const char *confname, const char *channel, fe_type_t fe_type){
	const char const *FRONTEND_DEV = "/dev/dvb/adapter0/frontend0";
	const char const *DEMUX_DEV = "/dev/dvb/adapter0/demux0";
	const char const *DVR_DEV = "/dev/dvb/adapter0/dvr0";

	struct dvb_frontend_parameters frontend_param;
	int vpid, apid, sid, pmtpid = 0;
	video_fd = 0;
	audio_fd = 0;
	int dvr_fd, file_fd;
	int dvr = 1;

	LOGD("reading channels from file '%s'\n", confname);

	memset(&frontend_param, 0, sizeof(struct dvb_frontend_parameters));

	switch(fe_type){
	case FE_OFDM:
		if (dvbt_parse (confname, channel, &frontend_param, &vpid, &apid, &sid))
			return -1;
		break;
	case FE_ATSC:
		if (atsc_parse (confname, channel, &frontend_param, &vpid, &apid, &sid))
			return -1;
		break;
	default:
		return -2;
	}

	if ((frontend_fd = open(FRONTEND_DEV, O_RDWR)) < 0) {
		LOGE("failed opening '%s'", FRONTEND_DEV);
		return -1;
	}

	if (setup_frontend (frontend_fd, &frontend_param, fe_type) < 0)
		return -1;

	pmtpid = get_pmt_pid(DEMUX_DEV, sid);
	if (pmtpid <= 0) {
		LOGE("couldn't find pmt-pid for sid %04x\n", sid);
		return -1;
	}

	if ((pat_fd = open(DEMUX_DEV, O_RDWR)) < 0) {
		LOGE("opening pat demux failed");
		return -1;
	}
	if (set_pesfilter(pat_fd, 0, DMX_PES_OTHER, dvr) < 0)
		return -1;

	if ((pmt_fd = open(DEMUX_DEV, O_RDWR)) < 0) {
		LOGE("opening pmt demux failed");
		return -1;
	}
	if (set_pesfilter(pmt_fd, pmtpid, DMX_PES_OTHER, dvr) < 0)
		return -1;

	LOGV("video pid 0x%04x, audio pid 0x%04x\n", vpid, apid);

	if ((video_fd = open(DEMUX_DEV, O_RDWR)) < 0) {
		LOGE("failed opening '%s'", DEMUX_DEV);
		return -1;
	}

	if (set_pesfilter (video_fd, vpid, DMX_PES_VIDEO, dvr) < 0)
		return -1;

	if ((audio_fd = open(DEMUX_DEV, O_RDWR)) < 0) {
		LOGE("failed opening '%s'", DEMUX_DEV);
		return -1;
	}

	if (set_pesfilter (audio_fd, apid, DMX_PES_AUDIO, dvr) < 0)
		return -1;

	return 0;
}

void dvb_stop(){
	close(pat_fd);
	close(pmt_fd);
	close(audio_fd);
	close(video_fd);
	close(frontend_fd);
}

int dvb_status(fe_status_t *status, uint16_t *signal, uint16_t *snr, uint32_t *ber, uint32_t *unc){
	if(ioctl(frontend_fd, FE_READ_STATUS, status) < 0)
		return -1;
	if(ioctl(frontend_fd, FE_READ_SIGNAL_STRENGTH, signal) < 0)
		return -2;
	if(ioctl(frontend_fd, FE_READ_SNR, snr) < 0)
		return -3;
	if(ioctl(frontend_fd, FE_READ_BER, ber) < 0)
		return -4;
	if(ioctl(frontend_fd, FE_READ_UNCORRECTED_BLOCKS, unc) < 0)
		return -5;
	return 0;
}
