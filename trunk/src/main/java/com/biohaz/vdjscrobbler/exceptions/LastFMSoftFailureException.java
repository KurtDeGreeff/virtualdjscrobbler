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
package com.biohaz.vdjscrobbler.exceptions;

import com.biohaz.vdjscrobbler.enums.LastFMResponse;

/** @author Magnus Tingne */
public class LastFMSoftFailureException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3700820006232733147L;

	private final LastFMResponse failureType;

	public LastFMSoftFailureException(String message, LastFMResponse failureType) {
		super(message);
		this.failureType = failureType;
	}

	public LastFMSoftFailureException(String message,
			LastFMResponse failureType, Throwable t) {
		super(message, t);
		this.failureType = failureType;
	}

	public LastFMResponse getFailureType() {
		return failureType;
	}
}
