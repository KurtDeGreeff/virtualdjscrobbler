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
import com.biohaz.vdjscrobbler.data.LastFMTrack;
import com.biohaz.vdjscrobbler.data.LastFMUser;
import com.biohaz.vdjscrobbler.enums.LastFMResponse;
import com.biohaz.vdjscrobbler.exceptions.LastFMHardFailureException;
import com.biohaz.vdjscrobbler.exceptions.LastFMSoftFailureException;

/** @author Magnus Tingne */
public class NowPlayingThread extends Thread {
	private static final Logger log = org.apache.log4j.Logger
			.getLogger(NowPlayingThread.class);

	private Scrobbler scrobbler;
	private final VirtualDJScrobbler virtualDJScrobbler;
	private final LastFMTrack track;
	private boolean success;

	private long hardFailureWait;
	private long hardFailureCount = 0;

	private final LastFMUser user;

	public NowPlayingThread(VirtualDJScrobbler virtualDJScrobbler,
			LastFMUser user, LastFMTrack track) {
		this.virtualDJScrobbler = virtualDJScrobbler;
		this.user = user;
		this.track = track;
		scrobbler = Scrobbler.getScrobblerForUser(user);
		success = false;
		hardFailureWait = 10000;
		this.setName(user.getUsername());
	}

	@Override
	public synchronized void run() {
		while (!success) {
			try {
				// waiting a preset amount of time to try to make sure we don't
				// submit to many tracks to last.fm at the same time
				try {
					Thread.sleep(user.getWaitTime());
				} catch (InterruptedException e) {
					log.info("Submission thread random sleep interrupted");
				}
				scrobbler.sendNowPlayingNotification(track);
				success = true;
			} catch (LastFMHardFailureException e) {
				if (System.currentTimeMillis() / 1000 - track.getStartTime() > track
						.getLength() / 2) {
					log
							.info("More than half of the song has passed, no more now playing retries will be made");
					return;
				}
				hardFailureCount++;
				try {
					if (hardFailureCount >= 3) {
						log.info("3rd hard failure, starting new handshake");
						boolean reHandshaked = reHandshake();
						if (!reHandshaked) {
							return;
						}
					} else {
						log.info("Hard failure, retrying in: "
								+ hardFailureWait / 1000 + "sec.");
						Thread.sleep(hardFailureWait);
					}
				} catch (InterruptedException ie) {
					log.warn("Thread sleep interrupted", ie);
				}
			} catch (LastFMSoftFailureException e) {
				LastFMResponse reason = e.getFailureType();
				switch (reason) {
				case SOFT_FAILURE_BAD_SESSION:
					try {
						boolean reHandshaked = reHandshake();
						if (!reHandshaked) {
							return;
						}
					} catch (InterruptedException ie) {
						log.warn("Thread sleep interrupted", ie);
					}
					break;
				default:
					log.error("Unknown soft failure occurred");
					break;
				}
			}
		}
	}

	/**
	 * Starts a new handshaking process.
	 * 
	 * @return true if handshake appears to be successful, false if more than
	 *         half the track has passed.
	 * @throws InterruptedException
	 */
	private boolean reHandshake() throws InterruptedException {
		Object lock = new Object();
		new HandshakeThread(virtualDJScrobbler, lock, user).start();
		synchronized (lock) {
			lock.wait((track.getLength() / 2) * 1000);
			while (!scrobbler.isSuccessfullyHandshaked()) {
				if (System.currentTimeMillis() / 1000 - track.getStartTime() > track
						.getLength() / 2) {
					log
							.info("More than half of the song has passed, no more now playing retries will be made");
					return false;
				}
				lock.wait((track.getLength() / 2) * 1000);
			}
		}
		log.info("Handshake should now be successful, retrying");
		hardFailureCount = 0;
		return true;
	}

	// public static void main(String[] args) {
	// try {
	// new NowPlayingThread(new VirtualDJScrobbler(), new LastFMUser(
	// "vdjscrobbler", MD5Util.md5("vdjtest")), new LastFMTrack(
	// "testArtist", "testTitle", (int) (System
	// .currentTimeMillis() / 1000), 60, Source.P))
	// .start();
	// } catch (NoSuchAlgorithmException e) {
	// e.printStackTrace();
	// }
	// }
}
