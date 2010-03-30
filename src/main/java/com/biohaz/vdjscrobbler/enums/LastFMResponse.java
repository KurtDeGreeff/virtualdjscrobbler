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
package com.biohaz.vdjscrobbler.enums;

/**
 * @author Magnus Tingne
 */
public enum LastFMResponse {
	GREAT_SUCCESS("OK"), HARD_FAILURE_ERROR_RESPONSE_CODE("RESPONSECODE"), HARD_FAILURE_BANNED_CLIENT(
			"BANNED"), HARD_FAILURE_FAILED("FAILED"), SOFT_FAILURE_BAD_AUTH(
			"BADAUTH"), SOFT_FAILURE_BAD_TIME("BADTIME"), SOFT_FAILURE_BAD_SESSION(
			"BADSESSION"), HARD_FAILURE_UNKNOWN("UNKNOWN");

	private final String text;

	private LastFMResponse(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
