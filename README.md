DroidTV
=======

Based on my [previous project for Archos Gen8 devices only][1] this app will provide [DVB][4] scanning, tuning and watching (e.g. local streaming) capabilities.

As soon as it hits a stable version, it will be available on the Android Market for free: [DroidTV][2]


Feel free to fork, create issues, [buy me a beer][3] or just having fun watching some live tv!

If you fork it, feel free to send me pull requests for your awesome new features or bug fixes so they'll make it on the market.

LICENSE
=======
 ******************************************************************************
	DroidTV, live TV on Android devices with host USB port and a DVB tuner
	Copyright (C) 2012  Christian Ulrich <chrulri@gmail.com>

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************

HOW DOES IT WORK?
=================
DroidTV uses basic linux functionality as Android runs on top of a linux kernel. So the prerequirements are:

* kernel modules for your dvb device are loaded and working
* dvb device has the firmware loaded and is in 'warm state'
* dvb device nodes in /dev/dvb/... are set with the correct permissions (e.g. at least 666)

These prerequirements given the app itself does not need any superuser privileges.

Then DroidTV uses [w_scan][5] to scan for channels on all frequencies.
This has to be done everytime you're moving to a new broadcasting area (e.g. moving to another country).

Once you've scanned for the channels the channel list will be stored on your Android device and you can
simply switch between those channel lists and tune your favored channel.
DroidTV then starts [DVBlast][6] - an open source, simple and powerful MPEG-2/TS demux and streaming application - which streams all the media streams via UDP back to droidtv which in turn streams it via HTTP to the media player rendering engine on your Android device.

[1]: http://code.google.com/p/archos-gen8-dvb/
[2]: https://market.android.com/details?id=com.chrulri.droidtv
[3]: https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=chrulri@gmail.com&item_name=droidtv
[4]: http://en.wikipedia.org/wiki/Digital_Video_Broadcasting
[5]: http://wirbel.htpc-forum.de/w_scan/index_en.html
[6]: http://www.videolan.org/projects/dvblast.html