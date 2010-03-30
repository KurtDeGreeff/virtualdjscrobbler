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
package com.biohaz.vdjscrobbler;

import static com.biohaz.vdjscrobbler.enums.LastFMResponse.GREAT_SUCCESS;
import static com.biohaz.vdjscrobbler.enums.LastFMResponse.HARD_FAILURE_BANNED_CLIENT;
import static com.biohaz.vdjscrobbler.enums.LastFMResponse.HARD_FAILURE_ERROR_RESPONSE_CODE;
import static com.biohaz.vdjscrobbler.enums.LastFMResponse.HARD_FAILURE_FAILED;
import static com.biohaz.vdjscrobbler.enums.LastFMResponse.HARD_FAILURE_UNKNOWN;
import static com.biohaz.vdjscrobbler.enums.LastFMResponse.SOFT_FAILURE_BAD_AUTH;
import static com.biohaz.vdjscrobbler.enums.LastFMResponse.SOFT_FAILURE_BAD_SESSION;
import static com.biohaz.vdjscrobbler.enums.LastFMResponse.SOFT_FAILURE_BAD_TIME;
import static com.biohaz.vdjscrobbler.util.MD5Util.md5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.biohaz.vdjscrobbler.data.LastFMTrack;
import com.biohaz.vdjscrobbler.data.LastFMUser;
import com.biohaz.vdjscrobbler.enums.Rating;
import com.biohaz.vdjscrobbler.enums.Source;
import com.biohaz.vdjscrobbler.exceptions.LastFMHardFailureException;
import com.biohaz.vdjscrobbler.exceptions.LastFMSoftFailureException;

/** @author Magnus Tingne */
public class Scrobbler {
	private static final Logger log = org.apache.log4j.Logger
			.getLogger(Scrobbler.class);

	private String handshakeURL;
	private String sessionID;
	private String nowPlayingURL;
	private String submissionURL;
	private String clientID;
	private String clientVersion;
	private String protocolVersion;

	private static HashMap<LastFMUser, Scrobbler> scrobblers;

	private final LastFMUser user;

	public static Scrobbler getScrobblerForUser(LastFMUser user) {
		if (scrobblers == null) {
			scrobblers = new HashMap<LastFMUser, Scrobbler>();
		}
		Scrobbler scrobbler = scrobblers.get(user);
		if (scrobbler == null) {
			scrobbler = new Scrobbler(user);
			scrobblers.put(user, scrobbler);
		}
		return scrobbler;
	}

	private Scrobbler(LastFMUser user) {
		this.user = user;
		handshakeURL = "http://post.audioscrobbler.com/"; // TODO: Property?
		clientID = "tst"; // dito //If you use this code the client id and
		clientVersion = "1.0"; // dito // clientVersion and protocolVersion
		protocolVersion = "1.2.1"; // dito //needs to be changed to yours
	}

	public void handshake() throws LastFMHardFailureException,
			LastFMSoftFailureException {
		try {
			sessionID = null;
			int timestamp = (int) (Calendar.getInstance(
					TimeZone.getTimeZone("UTC")).getTimeInMillis() / 1000);
			String auth = md5(user.getMd5password() + timestamp);
			URL url = new URL(handshakeURL + "?hs=true&p=" + protocolVersion
					+ "&c=" + clientID + "&v=" + clientVersion + "&u="
					+ user.getUsername() + "&t=" + timestamp + "&a=" + auth);
			log.info("Handshake set up with parameters: \n\tclient=" + clientID
					+ " \n\tclientVersion=" + clientVersion + " \n\tusername="
					+ user.getUsername() + " \n\ttimestamp=" + timestamp);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			conn.setInstanceFollowRedirects(true);
			conn.setRequestProperty("User-agent", "spider");
			conn.connect();
			int responseCode = conn.getResponseCode();
			log.debug("Response code is: " + responseCode);
			if (responseCode != 200) {
				log.warn("handshake: Received error response code");
				throw new LastFMHardFailureException(
						"Received error response code",
						HARD_FAILURE_ERROR_RESPONSE_CODE);
			} else {
				Object content = conn.getContent();
				byte[] bytes = null;
				if (content instanceof InputStream) {
					InputStream is = (InputStream) content;
					bytes = new byte[is.available()];
					is.read(bytes);
					if (bytes != null) {
						String response = new String(bytes).trim();
						log.debug("Response is: "
								+ response.replace("\n", "\n\t"));
						if (response.startsWith(GREAT_SUCCESS.getText())) {
							String[] responseLines = response.split("\n");
							sessionID = responseLines[1];
							nowPlayingURL = responseLines[2];
							submissionURL = responseLines[3];
							log
									.info("Handshake successful, storing info:\n\tsessionId="
											+ sessionID
											+ "\n\tnowPlayingURL="
											+ nowPlayingURL
											+ "\n\tsubmissionURL="
											+ submissionURL);
						} else if (SOFT_FAILURE_BAD_AUTH.getText().equals(
								response)) {
							log
									.warn("The user could not be validated, credentials wrong");
							throw new LastFMSoftFailureException(
									"The user could not be validated, credentials wrong",
									SOFT_FAILURE_BAD_AUTH);
						} else if (SOFT_FAILURE_BAD_TIME.getText().equals(
								response)) {
							log.warn("The users time is wrong");
							throw new LastFMSoftFailureException(
									"The users time is wrong",
									SOFT_FAILURE_BAD_TIME);
						} else if (HARD_FAILURE_BANNED_CLIENT.getText().equals(
								response)) {
							log.fatal("The client has been banned!");
							throw new LastFMHardFailureException(
									"The client has been banned!",
									HARD_FAILURE_BANNED_CLIENT);
						} else if (response.startsWith(HARD_FAILURE_FAILED
								.getText())) {
							String failureReason = response.replace(
									HARD_FAILURE_FAILED.getText(), "");
							log.error("The handshake failed with the reason: "
									+ failureReason);
							throw new LastFMHardFailureException(
									"The handshake failed with the reason: "
											+ failureReason,
									HARD_FAILURE_FAILED);
						} else {
							log
									.error("The handshake failed due to some unknown reason, response is: "
											+ response);
							throw new LastFMHardFailureException(
									"The handshake failed due to some unknown reason, response is: "
											+ response, HARD_FAILURE_UNKNOWN);
						}
					} else {
						log.error("Didn't get any response body");
						throw new LastFMHardFailureException(
								"Didn't get any response body",
								HARD_FAILURE_UNKNOWN);
					}
				} else {
					log.error("Could not read response");
					throw new LastFMHardFailureException(
							"Could not read response", HARD_FAILURE_UNKNOWN);
				}
			}
		} catch (IOException e) {
			log.error("IOError during handshaking", e);
			throw new LastFMHardFailureException("IOError during handshaking",
					HARD_FAILURE_UNKNOWN, e);
		} catch (NoSuchAlgorithmException e) {
			log.error("Could not generate MD5", e);
			throw new LastFMHardFailureException("Could not generate MD5",
					HARD_FAILURE_UNKNOWN, e);
		}
	}

	public void sendNowPlayingNotification(LastFMTrack track)
			throws LastFMSoftFailureException, LastFMHardFailureException {
		if (isSuccessfullyHandshaked()) {
			OutputStreamWriter outputStreamWriter = null;
			BufferedReader bufferedReader = null;
			try {
				StringBuffer request = new StringBuffer();
				request.append(URLEncoder.encode("s", "UTF-8") + "="
						+ URLEncoder.encode(sessionID, "UTF-8"));
				request.append("&" + URLEncoder.encode("a", "UTF-8") + "="
						+ URLEncoder.encode(track.getArtist(), "UTF-8"));
				request.append("&" + URLEncoder.encode("t", "UTF-8") + "="
						+ URLEncoder.encode(track.getTitle(), "UTF-8"));
				request.append("&" + URLEncoder.encode("b", "UTF-8") + "="
						+ URLEncoder.encode(track.getAlbumTitle(), "UTF-8"));
				request.append("&" + URLEncoder.encode("l", "UTF-8") + "=");
				if (track.getLength() > 0) {
					request.append(URLEncoder.encode("" + track.getLength(),
							"UTF-8"));
				}
				request.append("&" + URLEncoder.encode("n", "UTF-8") + "="
						+ URLEncoder.encode(track.getTrackNbr(), "UTF-8"));
				request.append("&"
						+ URLEncoder.encode("m", "UTF-8")
						+ "="
						+ URLEncoder.encode(track.getMusicBrainzTrackID(),
								"UTF-8"));
				log.info("Now playing set up with parameters: \n\tsessionID="
						+ sessionID + " \n\ttrack=" + track);
				log.debug("Now playing URL is: " + submissionURL
						+ URLDecoder.decode(request.toString(), "UTF-8"));
				URL url = new URL(nowPlayingURL);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setDoOutput(true);
				outputStreamWriter = new OutputStreamWriter(conn
						.getOutputStream());
				outputStreamWriter.write(request.toString());
				outputStreamWriter.flush();

				int responseCode = conn.getResponseCode();
				log.debug("Response code is: " + responseCode);
				if (responseCode != 200) {
					log.warn("sendNowPlaying: Received error response code");
					throw new LastFMHardFailureException(
							"Received error response code",
							HARD_FAILURE_ERROR_RESPONSE_CODE);
				} else {
					bufferedReader = new BufferedReader(new InputStreamReader(
							conn.getInputStream()));
					String response;
					if ((response = bufferedReader.readLine()) != null) {
						log.debug("Response is: "
								+ response.replace("\n", "\n\t"));
						if (response.startsWith(GREAT_SUCCESS.getText())) {
							log.info("Now playing submission succesful!");
						} else if (SOFT_FAILURE_BAD_SESSION.getText().equals(
								response)) {
							sessionID = null;
							log
									.warn("The now playing submission failed because there was something wrong with the session, new handshake needed");
							throw new LastFMSoftFailureException(
									"The now playing submission failed because there was something wrong with the session, new handshake needed",
									SOFT_FAILURE_BAD_SESSION);
						} else {
							log
									.error("The now playing submission failed due to some unknown reason, response is: "
											+ response);
							throw new LastFMHardFailureException(
									"The now playing submission failed due to some unknown reason, response is: "
											+ response, HARD_FAILURE_UNKNOWN);
						}
					} else {

					}
					bufferedReader.close();
				}
			} catch (MalformedURLException e) {
				log.error("The now playing URL was malformed", e);
				throw new LastFMHardFailureException(
						"The now playing URL was malformed",
						HARD_FAILURE_UNKNOWN, e);
			} catch (IOException e) {
				log.error("IOError during now playing submission", e);
				throw new LastFMHardFailureException(
						"IOError during now playing submission",
						HARD_FAILURE_UNKNOWN, e);
			} finally {
				if (outputStreamWriter != null) {
					try {
						outputStreamWriter.close();
					} catch (IOException e) {
					}
				}
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			log
					.warn("sendNowPlaying: No current handshake, new handshake needed");
			throw new LastFMSoftFailureException(
					"sendNowPlaying: No current handshake, new handshake needed",
					SOFT_FAILURE_BAD_SESSION);
		}
	}

	public void submitTracks(List<LastFMTrack> tracks)
			throws LastFMSoftFailureException, LastFMHardFailureException {
		if (isSuccessfullyHandshaked()) {
			OutputStreamWriter outputStreamWriter = null;
			BufferedReader bufferedReader = null;
			try {
				StringBuffer request = new StringBuffer();
				request.append(URLEncoder.encode("s", "UTF-8") + "="
						+ URLEncoder.encode(sessionID, "UTF-8"));
				int i = 0;
				for (LastFMTrack track : tracks) {
					request.append("&"
							+ URLEncoder.encode("a[" + i + "]", "UTF-8") + "="
							+ URLEncoder.encode(track.getArtist(), "UTF-8"));
					request.append("&"
							+ URLEncoder.encode("t[" + i + "]", "UTF-8") + "="
							+ URLEncoder.encode(track.getTitle(), "UTF-8"));
					request.append("&"
							+ URLEncoder.encode("i[" + i + "]", "UTF-8")
							+ "="
							+ URLEncoder.encode("" + track.getStartTime(),
									"UTF-8"));
					request.append("&"
							+ URLEncoder.encode("o[" + i + "]", "UTF-8")
							+ "="
							+ URLEncoder
									.encode("" + track.getSource(), "UTF-8"));
					request.append("&"
							+ URLEncoder.encode("r[" + i + "]", "UTF-8") + "=");
					if (track.getRating() == Rating.NA) {
						request.append(URLEncoder.encode(track.getRating()
								.getStringRepresentation(), "UTF-8"));
					}
					request.append("&"
							+ URLEncoder.encode("l[" + i + "]", "UTF-8") + "=");
					if (track.getLength() > 0) {
						request.append(URLEncoder.encode(
								"" + track.getLength(), "UTF-8"));
					} else if (track.getSource() == Source.P) {
						request.append(URLEncoder.encode("30", "UTF-8"));
					}
					request
							.append("&"
									+ URLEncoder
											.encode("b[" + i + "]", "UTF-8")
									+ "="
									+ URLEncoder.encode(track.getAlbumTitle(),
											"UTF-8"));
					request.append("&"
							+ URLEncoder.encode("n[" + i + "]", "UTF-8") + "="
							+ URLEncoder.encode(track.getTrackNbr(), "UTF-8"));
					request.append("&"
							+ URLEncoder.encode("m[" + i + "]", "UTF-8")
							+ "="
							+ URLEncoder.encode(track.getMusicBrainzTrackID(),
									"UTF-8"));
					i++;
					log
							.info("Submission track added with parameters: \n\tsessionID="
									+ sessionID + " \n\ttrack=" + track);
				}
				log.debug("Submission URL is: " + submissionURL
						+ URLDecoder.decode(request.toString(), "UTF-8"));
				URL url = new URL(submissionURL);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setDoOutput(true);
				outputStreamWriter = new OutputStreamWriter(conn
						.getOutputStream());
				outputStreamWriter.write(request.toString());
				outputStreamWriter.flush();

				int responseCode = conn.getResponseCode();
				log.debug("Response code is: " + responseCode);
				if (responseCode != 200) {
					log.warn("submitTracks: Received error response code");
					throw new LastFMHardFailureException(
							"Received error response code",
							HARD_FAILURE_ERROR_RESPONSE_CODE);
				} else {
					bufferedReader = new BufferedReader(new InputStreamReader(
							conn.getInputStream()));
					String response;
					if ((response = bufferedReader.readLine()) != null) {
						log.debug("Response is: "
								+ response.replace("\n", "\n\t"));
						if (response.startsWith(GREAT_SUCCESS.getText())) {
							log.info("Submission succesful!");
						} else if (SOFT_FAILURE_BAD_SESSION.getText().equals(
								response)) {
							sessionID = null;
							log
									.warn("The submission failed because there was something wrong with the session, new handshake needed");
							throw new LastFMSoftFailureException(
									"The submission failed because there was something wrong with the session, new handshake needed",
									SOFT_FAILURE_BAD_SESSION);
						} else if (response.startsWith(HARD_FAILURE_FAILED
								.getText())) {
							String failureReason = response.replace(
									HARD_FAILURE_FAILED.getText(), "");
							log.error("The submission failed with the reason: "
									+ failureReason);
							throw new LastFMHardFailureException(
									"The submission failed with the reason: "
											+ failureReason,
									HARD_FAILURE_FAILED);
						} else {
							log
									.error("The submission failed due to some unknown reason, response is: "
											+ response);
							throw new LastFMHardFailureException(
									"The submission failed due to some unknown reason, response is: "
											+ response, HARD_FAILURE_UNKNOWN);
						}
					} else {

					}
					bufferedReader.close();
				}
			} catch (MalformedURLException e) {
				log.error("The submission URL was malformed", e);
				throw new LastFMHardFailureException(
						"The submission URL was malformed",
						HARD_FAILURE_UNKNOWN, e);
			} catch (IOException e) {
				log.error("IOError during submission", e);
				throw new LastFMHardFailureException(
						"IOError during submission", HARD_FAILURE_UNKNOWN, e);
			} finally {
				if (outputStreamWriter != null) {
					try {
						outputStreamWriter.close();
					} catch (IOException e) {
					}
				}
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			log
					.warn("submitTracks: No current handshake, new handshake needed");
			throw new LastFMSoftFailureException(
					"submitTracks: No current handshake, new handshake needed",
					SOFT_FAILURE_BAD_SESSION);
		}
	}

	public boolean isSuccessfullyHandshaked() {
		return sessionID != null;
	}

	public String getSessionID() {
		return sessionID;
	}

	public String getNowPlayingURL() {
		return nowPlayingURL;
	}

	public String getSubmissionURL() {
		return submissionURL;
	}

}
