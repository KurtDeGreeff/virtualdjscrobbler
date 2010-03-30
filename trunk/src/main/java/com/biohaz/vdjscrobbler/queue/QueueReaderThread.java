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
package com.biohaz.vdjscrobbler.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;

import com.biohaz.vdjscrobbler.VirtualDJScrobbler;
import com.biohaz.vdjscrobbler.data.LastFMTrack;
import com.biohaz.vdjscrobbler.data.LastFMUser;
import com.biohaz.vdjscrobbler.workerthreads.SubmissionThread;

/** @author Magnus Tingne */
public class QueueReaderThread extends Thread {
	private static final Logger log = org.apache.log4j.Logger
			.getLogger(QueueReaderThread.class);

	private final Queue<LastFMTrack> trackQueue;
	private final VirtualDJScrobbler virtualDJScrobbler;
	private final LastFMUser user;

	public QueueReaderThread(Queue<LastFMTrack> trackQueue,
			VirtualDJScrobbler virtualDJScrobbler, LastFMUser user) {
		this.trackQueue = trackQueue;
		this.virtualDJScrobbler = virtualDJScrobbler;
		this.user = user;
		this.setName(user.getUsername());
	}

	@Override
	public void run() {
		while (true) {
			try {
				while (trackQueue.isEmpty()) {
					log.debug("Queue is empty, waiting for notification");
					synchronized (trackQueue) {
						trackQueue.wait();
					}
				}
				int toIndex = trackQueue.size();
				if (toIndex > 50) {
					toIndex = 50;
				}
				List<LastFMTrack> tracksToSubmit = new ArrayList<LastFMTrack>(
						trackQueue).subList(0, toIndex);
				SubmissionThread submissionThread = new SubmissionThread(
						virtualDJScrobbler, user, tracksToSubmit);
				log.debug("Starting thread that will submit tracks: "
						+ tracksToSubmit);
				submissionThread.start();
				while (submissionThread.isAlive()
						&& !submissionThread.isSubmitted()) {
					log.debug("Waiting for tracks to be submitted");
					synchronized (tracksToSubmit) {
						tracksToSubmit.wait();
					}
				}
				log.debug("Tracks submitted, removing tracks from queue");
				trackQueue.removeAll(tracksToSubmit);
			} catch (InterruptedException e) {
				log
						.warn("Thread was interrupted, continuing checking the queue");
			}
		}
	}
}
