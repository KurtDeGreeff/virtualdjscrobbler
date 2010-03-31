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
package se.tingne.vdjscrobbler.workerthreads;

import java.util.List;

import org.apache.log4j.Logger;

import se.tingne.vdjscrobbler.Scrobbler;
import se.tingne.vdjscrobbler.VirtualDJScrobbler;
import se.tingne.vdjscrobbler.data.LastFMTrack;
import se.tingne.vdjscrobbler.data.LastFMUser;
import se.tingne.vdjscrobbler.enums.LastFMResponse;
import se.tingne.vdjscrobbler.exceptions.LastFMHardFailureException;
import se.tingne.vdjscrobbler.exceptions.LastFMSoftFailureException;


/** @author Magnus Tingne */
public class SubmissionThread extends Thread {
	private static final Logger log = org.apache.log4j.Logger
			.getLogger(SubmissionThread.class);

	private Scrobbler scrobbler;
	private final VirtualDJScrobbler virtualDJScrobbler;
	private final List<LastFMTrack> tracks;
	private boolean success;

	private long hardFailureWait;
	private long hardFailureCount = 0;

	private final LastFMUser user;

	public SubmissionThread(VirtualDJScrobbler virtualDJScrobbler,
			LastFMUser user, List<LastFMTrack> tracks) {
		this.virtualDJScrobbler = virtualDJScrobbler;
		this.user = user;
		this.tracks = tracks;
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
				scrobbler.submitTracks(tracks);
				success = true;
				synchronized (tracks) {
					log.debug("Notifying all that tracks have been submitted");
					tracks.notifyAll();
				}
			} catch (LastFMHardFailureException e) {
				hardFailureCount++;
				try {
					if (hardFailureCount >= 3) {
						log.info("3rd hard failure, starting new handshake");
						reHandshake();
					} else {
						log.info("Hard failure, retrying in: "
								+ hardFailureWait / 1000 + "sec.");
						Thread.sleep(hardFailureWait);
					}
				} catch (InterruptedException ie) {
					log.warn("Thread sleep interrupted, trying again...", ie);
				}
			} catch (LastFMSoftFailureException e) {
				LastFMResponse reason = e.getFailureType();
				switch (reason) {
				case SOFT_FAILURE_BAD_SESSION:
					try {
						reHandshake();
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

	public boolean isSubmitted() {
		return success;
	}

	/**
	 * Starts a new handshaking process.
	 * 
	 * @return true if handshake appears to be successful, false if more than
	 *         half the track has passed.
	 * @throws InterruptedException
	 */
	private void reHandshake() throws InterruptedException {
		Object lock = new Object();
		new HandshakeThread(virtualDJScrobbler, lock, user).start();
		synchronized (lock) {
			lock.wait();
			while (!scrobbler.isSuccessfullyHandshaked()) {
				lock.wait();
			}
		}
		log.info("Handshake should now be successful, retrying");
		hardFailureCount = 0;
	}

}
