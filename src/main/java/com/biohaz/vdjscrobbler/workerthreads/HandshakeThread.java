/** 
 * Copyright 2009 Magnus Tingne (vdjscrobbler@gmail.com)
 * 
 * This file is part of VirtualDJScrobbler.
 * 
 * VirtualDJScrobbler is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * VirtualDJScrobbler is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * VirtualDJScrobbler. If not, see <http://www.gnu.org/licenses/>.
 */
package com.biohaz.vdjscrobbler.workerthreads;

import org.apache.log4j.Logger;

import com.biohaz.vdjscrobbler.Scrobbler;
import com.biohaz.vdjscrobbler.VirtualDJScrobbler;
import com.biohaz.vdjscrobbler.data.LastFMUser;
import com.biohaz.vdjscrobbler.enums.LastFMResponse;
import com.biohaz.vdjscrobbler.exceptions.LastFMHardFailureException;
import com.biohaz.vdjscrobbler.exceptions.LastFMSoftFailureException;

/** @author Magnus Tingne */
public class HandshakeThread extends Thread {
	private static final Logger log = org.apache.log4j.Logger
			.getLogger(HandshakeThread.class);

	private Scrobbler scrobbler;
	private final VirtualDJScrobbler virtualDJScrobbler;
	private boolean success;
	private boolean stopRequested;

	private long hardFailureWait;

	private final Object lock;

	private final LastFMUser user;

	public HandshakeThread(VirtualDJScrobbler virtualDJScrobbler, Object lock,
			LastFMUser user) {
		this.virtualDJScrobbler = virtualDJScrobbler;
		this.lock = lock;
		this.user = user;
		this.scrobbler = Scrobbler.getScrobblerForUser(user);
		success = false;
		stopRequested = false;
		hardFailureWait = 60000;
	}

	@Override
	public synchronized void run() {
		while (!success && !stopRequested) {
			try {
				scrobbler.handshake();
				success = true;
				synchronized (lock) {
					lock.notifyAll();
				}
			} catch (LastFMHardFailureException e) {
				if (e.getFailureType() == LastFMResponse.HARD_FAILURE_BANNED_CLIENT) {
					virtualDJScrobbler.reportClientBanned();
					break;
				}
				try {
					log.info("Hard failure, retrying in: " + hardFailureWait
							/ 1000 / 60 + "min.");
					Thread.sleep(hardFailureWait);
				} catch (InterruptedException ie) {
					log.warn("Thread sleep interrupted", ie);
				}
				hardFailureWait = hardFailureWait * 2;
				if (hardFailureWait > 7200000) {
					hardFailureWait = 7200000;
				}
			} catch (LastFMSoftFailureException e) {
				LastFMResponse reason = e.getFailureType();
				try {
					switch (reason) {
					case SOFT_FAILURE_BAD_AUTH:
						virtualDJScrobbler.reportBadAuth(user);
						this.join();
						break;
					case SOFT_FAILURE_BAD_TIME:
						virtualDJScrobbler.reportBadTime();
						break;
					default:
						log.error("Unknown soft failure occurred");
						break;
					}
				} catch (InterruptedException e1) {
				}
			}
		}
	}

	public void requestStop() {
		stopRequested = true;
	}
}
