/*
 * frontend.h
 *
 * Copyright (C) 2000 Marcus Metzler <marcus@convergence.de>
 *		    Ralph  Metzler <ralph@convergence.de>
 *		    Holger Waechtler <holger@convergence.de>
 *		    Andre Draszik <ad@convergence.de>
 *		    for convergence integrated media GmbH
 *
 * Copyright (C) Manu Abraham <abraham.manu@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

#ifndef _DVBFRONTEND_H_
#define _DVBFRONTEND_H_

#include <asm/types.h>


typedef enum fe_type {
	FE_QPSK,
	FE_QAM,
	FE_OFDM,
	FE_ATSC
} fe_type_t;


typedef enum fe_caps {
	FE_IS_STUPID			= 0,
	FE_CAN_INVERSION_AUTO		= 0x1,
	FE_CAN_FEC_1_2			= 0x2,
	FE_CAN_FEC_2_3			= 0x4,
	FE_CAN_FEC_3_4			= 0x8,
	FE_CAN_FEC_4_5			= 0x10,
	FE_CAN_FEC_5_6			= 0x20,
	FE_CAN_FEC_6_7			= 0x40,
	FE_CAN_FEC_7_8			= 0x80,
	FE_CAN_FEC_8_9			= 0x100,
	FE_CAN_FEC_AUTO			= 0x200,
	FE_CAN_QPSK			= 0x400,
	FE_CAN_QAM_16			= 0x800,
	FE_CAN_QAM_32			= 0x1000,
	FE_CAN_QAM_64			= 0x2000,
	FE_CAN_QAM_128			= 0x4000,
	FE_CAN_QAM_256			= 0x8000,
	FE_CAN_QAM_AUTO			= 0x10000,
	FE_CAN_TRANSMISSION_MODE_AUTO	= 0x20000,
	FE_CAN_BANDWIDTH_AUTO		= 0x40000,
	FE_CAN_GUARD_INTERVAL_AUTO	= 0x80000,
	FE_CAN_HIERARCHY_AUTO		= 0x100000,
	FE_CAN_8VSB			= 0x200000,
	FE_CAN_16VSB			= 0x400000,
	FE_NEEDS_BENDING		= 0x20000000, // not supported anymore, don't use (frontend requires frequency bending)
	FE_CAN_RECOVER			= 0x40000000, // frontend can recover from a cable unplug automatically
	FE_CAN_MUTE_TS			= 0x80000000  // frontend can stop spurious TS data output
} fe_caps_t;


struct dvb_frontend_info {
	char       name[128];
	fe_type_t  type;
	__u32      frequency_min;
	__u32      frequency_max;
	__u32      frequency_stepsize;
	__u32      frequency_tolerance;
	__u32      symbol_rate_min;
	__u32      symbol_rate_max;
	__u32      symbol_rate_tolerance;	/* ppm */
	__u32      notifier_delay;		/* DEPRECATED */
	fe_caps_t  caps;
};


/**
 *  Check out the DiSEqC bus spec available on http://www.eutelsat.org/ for
 *  the meaning of this struct...
 */
struct dvb_diseqc_master_cmd {
	__u8 msg [6];	/*  { framing, address, command, data [3] } */
	__u8 msg_len;	/*  valid values are 3...6  */
};


struct dvb_diseqc_slave_reply {
	__u8 msg [4];	/*  { framing, data [3] } */
	__u8 msg_len;	/*  valid values are 0...4, 0 means no msg  */
	int  timeout;	/*  return from ioctl after timeout ms with */
};			/*  errorcode when no message was received  */


typedef enum fe_sec_voltage {
	SEC_VOLTAGE_13,
	SEC_VOLTAGE_18,
	SEC_VOLTAGE_OFF
} fe_sec_voltage_t;


typedef enum fe_sec_tone_mode {
	SEC_TONE_ON,
	SEC_TONE_OFF
} fe_sec_tone_mode_t;


typedef enum fe_sec_mini_cmd {
	SEC_MINI_A,
	SEC_MINI_B
} fe_sec_mini_cmd_t;


typedef enum fe_status {
	FE_HAS_SIGNAL	= 0x01,   /*  found something above the noise level */
	FE_HAS_CARRIER	= 0x02,   /*  found a DVB signal  */
	FE_HAS_VITERBI	= 0x04,   /*  FEC is stable  */
	FE_HAS_SYNC	= 0x08,   /*  found sync bytes  */
	FE_HAS_LOCK	= 0x10,   /*  everything's working... */
	FE_TIMEDOUT	= 0x20,   /*  no lock within the last ~2 seconds */
	FE_REINIT	= 0x40    /*  frontend was reinitialized,  */
} fe_status_t;			  /*  application is recommended to reset */
				  /*  DiSEqC, tone and parameters */

typedef enum fe_spectral_inversion {
	INVERSION_OFF,
	INVERSION_ON,
	INVERSION_AUTO
} fe_spectral_inversion_t;


typedef enum fe_code_rate {
	FEC_NONE = 0,
	FEC_1_2,
	FEC_2_3,
	FEC_3_4,
	FEC_4_5,
	FEC_5_6,
	FEC_6_7,
	FEC_7_8,
	FEC_8_9,
	FEC_AUTO
} fe_code_rate_t;


typedef enum fe_modulation {
	QPSK,
	QAM_16,
	QAM_32,
	QAM_64,
	QAM_128,
	QAM_256,
	QAM_AUTO,
	VSB_8,
	VSB_16
} fe_modulation_t;

typedef enum fe_transmit_mode {
	TRANSMISSION_MODE_2K,
	TRANSMISSION_MODE_8K,
	TRANSMISSION_MODE_AUTO
} fe_transmit_mode_t;

typedef enum fe_bandwidth {
	BANDWIDTH_8_MHZ,
	BANDWIDTH_7_MHZ,
	BANDWIDTH_6_MHZ,
	BANDWIDTH_AUTO
} fe_bandwidth_t;


typedef enum fe_guard_interval {
	GUARD_INTERVAL_1_32,
	GUARD_INTERVAL_1_16,
	GUARD_INTERVAL_1_8,
	GUARD_INTERVAL_1_4,
	GUARD_INTERVAL_AUTO
} fe_guard_interval_t;


typedef enum fe_hierarchy {
	HIERARCHY_NONE,
	HIERARCHY_1,
	HIERARCHY_2,
	HIERARCHY_4,
	HIERARCHY_AUTO
} fe_hierarchy_t;


struct dvb_qpsk_parameters {
	__u32		symbol_rate;  /* symbol rate in Symbols per second */
	fe_code_rate_t	fec_inner;    /* forward error correction (see above) */
};

struct dvb_qam_parameters {
	__u32		symbol_rate; /* symbol rate in Symbols per second */
	fe_code_rate_t	fec_inner;   /* forward error correction (see above) */
	fe_modulation_t	modulation;  /* modulation type (see above) */
};

struct dvb_vsb_parameters {
	fe_modulation_t	modulation;  /* modulation type (see above) */
};

struct dvb_ofdm_parameters {
	fe_bandwidth_t      bandwidth;
	fe_code_rate_t      code_rate_HP;  /* high priority stream code rate */
	fe_code_rate_t      code_rate_LP;  /* low priority stream code rate */
	fe_modulation_t     constellation; /* modulation type (see above) */
	fe_transmit_mode_t  transmission_mode;
	fe_guard_interval_t guard_interval;
	fe_hierarchy_t      hierarchy_information;
};


struct dvb_frontend_parameters {
	__u32 frequency;     /* (absolute) frequency in Hz for QAM/OFDM/ATSC */
			     /* intermediate frequency in kHz for QPSK */
	fe_spectral_inversion_t inversion;
	union {
		struct dvb_qpsk_parameters qpsk;
		struct dvb_qam_parameters  qam;
		struct dvb_ofdm_parameters ofdm;
		struct dvb_vsb_parameters vsb;
	} u;
};


/**
 * When set, this flag will disable any zigzagging or other "normal" tuning
 * behaviour. Additionally, there will be no automatic monitoring of the lock
 * status, and hence no frontend events will be generated. If a frontend device
 * is closed, this flag will be automatically turned off when the device is
 * reopened read-write.
 */
#define FE_TUNE_MODE_ONESHOT 0x01


#define FE_GET_INFO		   _IOR('o', 61, struct dvb_frontend_info)

#define FE_DISEQC_RESET_OVERLOAD   _IO('o', 62)
#define FE_DISEQC_SEND_MASTER_CMD  _IOW('o', 63, struct dvb_diseqc_master_cmd)
#define FE_DISEQC_RECV_SLAVE_REPLY _IOR('o', 64, struct dvb_diseqc_slave_reply)
#define FE_DISEQC_SEND_BURST       _IO('o', 65)  /* fe_sec_mini_cmd_t */

#define FE_SET_TONE		   _IO('o', 66)  /* fe_sec_tone_mode_t */
#define FE_SET_VOLTAGE		   _IO('o', 67)  /* fe_sec_voltage_t */
#define FE_ENABLE_HIGH_LNB_VOLTAGE _IO('o', 68)  /* int */

#define FE_READ_STATUS		   _IOR('o', 69, fe_status_t)
#define FE_READ_BER		   _IOR('o', 70, __u32)
#define FE_READ_SIGNAL_STRENGTH    _IOR('o', 71, __u16)
#define FE_READ_SNR		   _IOR('o', 72, __u16)
#define FE_READ_UNCORRECTED_BLOCKS _IOR('o', 73, __u32)

#define FE_SET_FRONTEND		   _IOW('o', 76, struct dvb_frontend_parameters)
#define FE_GET_FRONTEND		   _IOR('o', 77, struct dvb_frontend_parameters)
#define FE_SET_FRONTEND_TUNE_MODE  _IO('o', 81) /* unsigned int */

#define FE_DISHNETWORK_SEND_LEGACY_CMD _IO('o', 80) /* unsigned int */

/*
 * References:
 * DVB-S : EN 300 421
 * DVB-S2: EN 302 307, TR 102 376, EN 301 210
 * DVB-C : EN 300 429
 * DVB-T : EN 300 744
 * DVB-H : EN 300 304
 * ATSC  : A/53A
 */

/*
 * Delivery Systems
 * needs to set/queried for multistandard frontends
 */
enum dvbfe_delsys {
	DVBFE_DELSYS_DVBS		= (1 <<  0),
	DVBFE_DELSYS_DSS		= (1 <<  1),
	DVBFE_DELSYS_DVBS2		= (1 <<  2),
	DVBFE_DELSYS_DVBC		= (1 <<  3),
	DVBFE_DELSYS_DVBT		= (1 <<  4),
	DVBFE_DELSYS_DVBH		= (1 <<  5),
	DVBFE_DELSYS_ATSC		= (1 <<  6),
	DVBFE_DELSYS_DUMMY		= (1 << 31)
};
#define DVBFE_GET_DELSYS		_IOR('o', 82, enum dvbfe_delsys)
#define DVBFE_SET_DELSYS		_IOW('o', 87, enum dvbfe_delsys)

/* Modulation types			*/
enum dvbfe_modulation {
	DVBFE_MOD_NONE			= (0 <<  0),
	DVBFE_MOD_BPSK			= (1 <<  0),
	DVBFE_MOD_QPSK			= (1 <<  1),
	DVBFE_MOD_OQPSK			= (1 <<  2),
	DVBFE_MOD_8PSK			= (1 <<  3),
	DVBFE_MOD_16APSK		= (1 <<  4),
	DVBFE_MOD_32APSK		= (1 <<  5),
	DVBFE_MOD_QAM4			= (1 <<  6),
	DVBFE_MOD_QAM16			= (1 <<  7),
	DVBFE_MOD_QAM32			= (1 <<  8),
	DVBFE_MOD_QAM64			= (1 <<  9),
	DVBFE_MOD_QAM128		= (1 << 10),
	DVBFE_MOD_QAM256		= (1 << 11),
	DVBFE_MOD_QAM512		= (1 << 12),
	DVBFE_MOD_QAM1024		= (1 << 13),
	DVBFE_MOD_QAMAUTO		= (1 << 14),
	DVBFE_MOD_OFDM			= (1 << 15),
	DVBFE_MOD_COFDM			= (1 << 16),
	DVBFE_MOD_VSB8			= (1 << 17),
	DVBFE_MOD_VSB16			= (1 << 18),
	DVBFE_MOD_AUTO			= (1 << 31)
};

/*
 * Convolution Code Rate (Viterbi Inner Code Rate)
 * DVB-S2 uses LDPC. Information on LDPC can be found at
 * http://www.ldpc-codes.com
 */
enum dvbfe_fec {
	DVBFE_FEC_NONE			= (0 <<  0),
	DVBFE_FEC_1_4			= (1 <<  0),
	DVBFE_FEC_1_3			= (1 <<  1),
	DVBFE_FEC_2_5			= (1 <<  2),
	DVBFE_FEC_1_2			= (1 <<  3),
	DVBFE_FEC_3_5			= (1 <<  4),
	DVBFE_FEC_2_3			= (1 <<  5),
	DVBFE_FEC_3_4			= (1 <<  6),
	DVBFE_FEC_4_5			= (1 <<  7),
	DVBFE_FEC_5_6			= (1 <<  8),
	DVBFE_FEC_6_7			= (1 <<  9),
	DVBFE_FEC_7_8			= (1 << 10),
	DVBFE_FEC_8_9			= (1 << 11),
	DVBFE_FEC_9_10			= (1 << 12),
	DVBFE_FEC_AUTO			= (1 << 31)
};

/* Frontend Inversion (I/Q Swap)	*/
enum dvbfe_inversion {
	DVBFE_INVERSION_OFF		= 0,
	DVBFE_INVERSION_ON		= 1,
	DVBFE_INVERSION_AUTO		= 2
};

/* DVB-S parameters			*/
struct dvbs_params {
	__u32				symbol_rate;

	enum dvbfe_modulation		modulation;
	enum dvbfe_fec			fec;
};

/* DSS parameters			*/
struct dss_params {
	__u32				symbol_rate;

	enum dvbfe_modulation		modulation;
	enum dvbfe_fec			fec;
};

/*
 * Rolloff Rate (Nyquist Filter Rolloff)
 * NOTE: DVB-S2 has rates of 0.20, 0.25, 0.35
 * Values are x100
 * Applies to DVB-S2
 */
enum dvbfe_rolloff {
	DVBFE_ROLLOFF_35		= 0,
	DVBFE_ROLLOFF_25		= 1,
	DVBFE_ROLLOFF_20		= 2,
	DVBFE_ROLLOFF_UNKNOWN		= 3
};

/* DVB-S2 parameters			*/
struct dvbs2_params {
	__u32				symbol_rate;

	enum dvbfe_modulation		modulation;
	enum dvbfe_fec			fec;

	/* Informational fields only	*/
	enum dvbfe_rolloff		rolloff;

	__u8				matype_1;
	__u8				matype_2;
	__u8				upl_1;
	__u8				upl_2;
	__u8				dfl_1;
	__u8				dfl_2;
	__u8				sync;
	__u8				syncd_1;
	__u8				syncd_2;

	__u8				pad[32];
};

/* DVB-C parameters			*/
struct dvbc_params {
	__u32				symbol_rate;
	enum dvbfe_modulation		modulation;
	enum dvbfe_fec			fec;
};

/* DVB-T Channel bandwidth		*/
enum dvbfe_bandwidth {
	DVBFE_BANDWIDTH_8_MHZ		= (1 <<  0),
	DVBFE_BANDWIDTH_7_MHZ		= (1 <<  1),
	DVBFE_BANDWIDTH_6_MHZ		= (1 <<  2),
	DVBFE_BANDWIDTH_5_MHZ		= (1 <<  3),
	DVBFE_BANDWIDTH_AUTO		= (1 << 31)
};

/* DVB-T/DVB-H transmission mode	*/
enum dvbfe_transmission_mode {
	DVBFE_TRANSMISSION_MODE_2K	= (1 <<  0),
	DVBFE_TRANSMISSION_MODE_4K	= (1 <<  1),
	DVBFE_TRANSMISSION_MODE_8K	= (1 <<  2),
	DVBFE_TRANSMISSION_MODE_AUTO	= (1 << 31)
};

/* DVB-T/DVB-H Guard interval		*/
enum dvbfe_guard_interval {
	DVBFE_GUARD_INTERVAL_1_32	= (1 <<  1),
	DVBFE_GUARD_INTERVAL_1_16	= (1 <<  2),
	DVBFE_GUARD_INTERVAL_1_8	= (1 <<  3),
	DVBFE_GUARD_INTERVAL_1_4	= (1 <<  4),
	DVBFE_GUARD_INTERVAL_AUTO	= (1 << 31)
};

/* DVB-T/DVB-H Hierarchial modulation	*/
enum dvbfe_hierarchy {
	DVBFE_HIERARCHY_OFF		= (1 <<  0),
	DVBFE_HIERARCHY_ON		= (1 <<  1),
	DVBFE_HIERARCHY_AUTO		= (1 <<  2)
};

/* DVB-T/DVB-H Rolloff's		*/
enum dvbfe_alpha {
	DVBFE_ALPHA_1			= (1 <<  0),
	DVBFE_ALPHA_2			= (1 <<  1),
	DVBFE_ALPHA_4			= (1 <<  2)
};

/* Stream priority (Hierachial coding)	*/
enum dvbfe_stream_priority {
	DVBFE_STREAM_PRIORITY_HP	= (0 << 0),
	DVBFE_STREAM_PRIORITY_LP	= (1 << 0)
};

/* DVB-T parameters			*/
struct dvbt_params {
	enum dvbfe_modulation		constellation;
	enum dvbfe_bandwidth		bandwidth;
	enum dvbfe_fec			code_rate_HP;
	enum dvbfe_fec			code_rate_LP;
	enum dvbfe_transmission_mode	transmission_mode;
	enum dvbfe_guard_interval	guard_interval;
	enum dvbfe_hierarchy		hierarchy;
	enum dvbfe_alpha		alpha;
	enum dvbfe_stream_priority	priority;

	__u8				pad[32];
};

/* DVB-H Interleaver type		*/
enum dvbfe_interleaver {
	DVBFE_INTERLEAVER_NATIVE	= (1 <<  0),
	DVBFE_INTERLEAVER_INDEPTH	= (1 <<  1),
	DVBFE_INTERLEAVER_AUTO		= (1 << 31)
};

/* DVB-H MPE-FEC Indicator		*/
enum dvbfe_mpefec {
	DVBFE_MPEFEC_OFF		= (1 <<  0),
	DVBFE_MPEFEC_ON			= (1 <<  1)
};

/* DVB-H Timeslicing Indicator		*/
enum dvbfe_timeslicing {
	DVBFE_TIMESLICING_OFF		= (1 <<  0),
	DVBFE_TIMESLICING_ON		= (1 <<  1)
};

/* DVB-H parameters			*/
struct dvbh_params {
	enum dvbfe_modulation		constellation;
	enum dvbfe_fec			code_rate_HP;
	enum dvbfe_fec			code_rate_LP;
	enum dvbfe_transmission_mode	transmission_mode;
	enum dvbfe_guard_interval	guard_interval;
	enum dvbfe_hierarchy		hierarchy;
	enum dvbfe_alpha		alpha;
	enum dvbfe_interleaver		interleaver;
	enum dvbfe_mpefec		mpefec;
	enum dvbfe_timeslicing		timeslicing;
	enum dvbfe_stream_priority	priority;

	__u32				bandwidth;
	__u8				pad[32];
};

/* ATSC parameters			*/
struct atsc_params {
	enum dvbfe_modulation		modulation;

	__u8				pad[32];
};

/* DVB Frontend Tuning Parameters	*/
struct dvbfe_params {
	__u32				frequency;
	enum fe_spectral_inversion	inversion;
	enum dvbfe_delsys		delivery;

	__u8				pad[32];

	union {
		struct dvbs_params	dvbs;
		struct dss_params	dss;
		struct dvbs2_params	dvbs2;
		struct dvbc_params	dvbc;
		struct dvbt_params	dvbt;
		struct dvbh_params	dvbh;
		struct atsc_params	atsc;

		__u8			pad[128];
	} delsys;
};
#define DVBFE_SET_PARAMS		_IOW('o', 83, struct dvbfe_params)
#define DVBFE_GET_PARAMS		_IOWR('o', 84, struct dvbfe_params)

/* DVB-S capability bitfields		*/
struct dvbfe_dvbs_info {
	enum dvbfe_modulation		modulation;
	enum dvbfe_fec			fec;
};

/* DSS capability bitfields		*/
struct dvbfe_dss_info {
	enum dvbfe_modulation		modulation;
	enum dvbfe_fec			fec;
};

/* DVB-S2 capability bitfields		*/
struct dvbfe_dvbs2_info {
	enum dvbfe_modulation		modulation;
	enum dvbfe_fec			fec;

	__u8				pad[32];
};

/* DVB-C capability bitfields		*/
struct dvbfe_dvbc_info {
	enum dvbfe_modulation		modulation;
};

/* DVB-T capability bitfields		*/
struct dvbfe_dvbt_info {
	enum dvbfe_modulation		modulation;
	enum dvbfe_stream_priority	stream_priority;

	__u8				pad[32];
};

/* DVB-H capability bitfields		*/
struct dvbfe_dvbh_info {
	enum dvbfe_modulation		modulation;
	enum dvbfe_stream_priority	stream_priority;

	__u8				pad[32];
};

/* ATSC capability bitfields		*/
struct dvbfe_atsc_info {
	enum dvbfe_modulation		modulation;

	__u8				pad[32];
};

/* DVB Frontend related Information	*/
struct dvbfe_info {
	char				name[128];

	union {
		struct dvbfe_dvbs_info	dvbs;
		struct dvbfe_dss_info	dss;
		struct dvbfe_dvbs2_info	dvbs2;
		struct dvbfe_dvbc_info	dvbc;
		struct dvbfe_dvbt_info	dvbt;
		struct dvbfe_dvbh_info	dvbh;
		struct dvbfe_atsc_info	atsc;

		__u8			pad[128];
	} delsys;

	__u32				frequency_min;
	__u32				frequency_max;
	__u32				frequency_step;
	__u32				frequency_tolerance;
	__u32				symbol_rate_min;
	__u32				symbol_rate_max;
	__u32				symbol_rate_tolerance;

	enum fe_spectral_inversion	inversion;

	__u8				pad[128];
};
#define DVBFE_GET_INFO			_IOR('o', 85, struct dvbfe_info)

enum dvbfe_status {
	DVBFE_HAS_SIGNAL		= (1 <<  0),	/*  something above noise floor	*/
	DVBFE_HAS_CARRIER		= (1 <<  1),	/*  Signal found		*/
	DVBFE_HAS_VITERBI		= (1 <<  2),	/*  FEC is stable		*/
	DVBFE_HAS_SYNC			= (1 <<  3),	/*  SYNC found			*/
	DVBFE_HAS_LOCK			= (1 <<  4),	/*  OK ..			*/
	DVBFE_TIMEDOUT			= (1 <<  5),	/*  no lock in last ~2 s	*/
	DVBFE_STATUS_DUMMY		= (1 << 31)
};

/* DVB Frontend events			*/
struct dvbfe_events {
	enum dvbfe_status		status;

	__u8				pad[32];
};

struct dvb_frontend_event {
	fe_status_t status;
	struct dvb_frontend_parameters parameters;
};
#define FE_GET_EVENT		   _IOR('o', 78, struct dvb_frontend_event)

struct dvbfe_event {
	struct dvbfe_events fe_events;
	struct dvbfe_params fe_params;
};
#define DVBFE_GET_EVENT			_IOR('o', 86, struct dvbfe_event)

#endif /*_DVBFRONTEND_H_*/
