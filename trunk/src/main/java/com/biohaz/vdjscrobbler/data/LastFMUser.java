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
package com.biohaz.vdjscrobbler.data;

/** @author Magnus Tingne */
public class LastFMUser {
	private String username;
	private String md5password;
	private long waitTime;

	public LastFMUser(String username, String md5password, long waitTime) {
		this.username = username;
		this.md5password = md5password;
		this.waitTime = waitTime;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getMd5password() {
		return md5password;
	}

	public void setMd5password(String md5password) {
		this.md5password = md5password;
	}

	public long getWaitTime() {
		return waitTime;
	}

	public void setWaitTime(long scrobblingWaitTime) {
		this.waitTime = scrobblingWaitTime;
	}

	@Override
	public String toString() {
		return "Username: " + username + ", MD5password: " + md5password
				+ ", waitTime: " + waitTime;
	}
}
