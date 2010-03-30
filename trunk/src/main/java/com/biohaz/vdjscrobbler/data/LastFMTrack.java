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

import java.io.Serializable;

import com.biohaz.vdjscrobbler.enums.Rating;
import com.biohaz.vdjscrobbler.enums.Source;

/** @author Magnus Tingne */
public class LastFMTrack implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9114668406643651314L;

	private String artist;
	private String title;
	private int startTime;
	private int length;
	private Source source;
	private Rating rating;
	private String albumTitle;
	private String trackNbr;
	private String musicBrainzTrackID;

	public LastFMTrack(String artist, String title, int startTime, int length,
			Source source, Rating rating, String albumTitle, String trackNbr,
			String musicBrainzTrackID) {
		super();
		this.artist = artist;
		this.title = title;
		this.startTime = startTime;
		this.length = length;
		this.source = source;
		this.rating = rating;
		this.albumTitle = albumTitle;
		this.trackNbr = trackNbr;
		this.musicBrainzTrackID = musicBrainzTrackID;
	}

	public LastFMTrack(String artist, String title, int startTime, int length,
			Source source) {
		this(artist, title, startTime, length, source, Rating.NA, "", "", "");
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public Rating getRating() {
		return rating;
	}

	public void setRating(Rating rating) {
		this.rating = rating;
	}

	public String getAlbumTitle() {
		return albumTitle;
	}

	public void setAlbumTitle(String albumTitle) {
		this.albumTitle = albumTitle;
	}

	public String getTrackNbr() {
		return trackNbr;
	}

	public void setTrackNbr(String trackNbr) {
		this.trackNbr = trackNbr;
	}

	public String getMusicBrainzTrackID() {
		return musicBrainzTrackID;
	}

	public void setMusicBrainzTrackID(String musicBrainzTrackID) {
		this.musicBrainzTrackID = musicBrainzTrackID;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LastFMTrack other = (LastFMTrack) obj;
		if (albumTitle == null) {
			if (other.albumTitle != null) {
				return false;
			}
		} else if (!albumTitle.equals(other.albumTitle)) {
			return false;
		}
		if (artist == null) {
			if (other.artist != null) {
				return false;
			}
		} else if (!artist.equals(other.artist)) {
			return false;
		}
		if (length != other.length) {
			return false;
		}
		if (musicBrainzTrackID == null) {
			if (other.musicBrainzTrackID != null) {
				return false;
			}
		} else if (!musicBrainzTrackID.equals(other.musicBrainzTrackID)) {
			return false;
		}
		if (rating == null) {
			if (other.rating != null) {
				return false;
			}
		} else if (!rating.equals(other.rating)) {
			return false;
		}
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
			return false;
		}
		if (startTime != other.startTime) {
			return false;
		}
		if (title == null) {
			if (other.title != null) {
				return false;
			}
		} else if (!title.equals(other.title)) {
			return false;
		}
		if (trackNbr == null) {
			if (other.trackNbr != null) {
				return false;
			}
		} else if (!trackNbr.equals(other.trackNbr)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Artist: " + artist + ", Title: " + title + ", Start time: "
				+ startTime + ", Length: " + length + ", Source: " + source
				+ ", Rating: " + rating + ", Album title: " + albumTitle
				+ ", Track nbr: " + trackNbr + ", Music Brainz ID: "
				+ musicBrainzTrackID;
	}
}
