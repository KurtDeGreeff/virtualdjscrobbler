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

import org.apache.log4j.Logger;

import se.tingne.vdjscrobbler.VirtualDJScrobbler;
import se.tingne.vdjscrobbler.data.LastFMUser;


/** @author Magnus Tingne */
public class ValidateUserThread extends Thread {
	private static final Logger log = org.apache.log4j.Logger
			.getLogger(ValidateUserThread.class);

	private final VirtualDJScrobbler virtualDJScrobbler;

	private final LastFMUser user;

	public ValidateUserThread(VirtualDJScrobbler virtualDJScrobbler,
			LastFMUser user) {
		this.user = user;
		this.setName(user.getUsername());
		this.virtualDJScrobbler = virtualDJScrobbler;
	}

	@Override
	public void run() {
		Object lock = new Object();
		HandshakeThread handshakeThread = new HandshakeThread(
				virtualDJScrobbler, lock, user);
		handshakeThread.start();
		synchronized (lock) {
			try {
				lock.wait();
				virtualDJScrobbler.reportValidationSuccessful();
			} catch (InterruptedException e) {
				log
						.debug("Waiting for handshake interrupted, sending stop request");
				handshakeThread.requestStop();
			}
		}
	}
}
