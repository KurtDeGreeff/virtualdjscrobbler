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
package se.tingne.vdjscrobbler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;

/** @author Magnus Tingne */
public class TracklistFileWatcher {
	private static TracklistFileWatcher instance;
	private static Logger log = org.apache.log4j.Logger.getLogger(TracklistFileWatcher.class);

	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private Path trackListPath;
	private String lastLineHandled;
	private VirtualDJScrobbler virtualDJScrobbler;

	public static TracklistFileWatcher getInstance(VirtualDJScrobbler virtualDJScrobbler) throws IOException {
		if (instance == null) {
			instance = new TracklistFileWatcher(virtualDJScrobbler);
		}
		return instance;
	}

	private TracklistFileWatcher(VirtualDJScrobbler virtualDJScrobbler) throws IOException {
		this.virtualDJScrobbler = virtualDJScrobbler;
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();
		updateTrackListPath();
	}

	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
		keys.put(key, dir);
	}

	public void updateTrackListPath() throws IOException {
		trackListPath = FileSystems.getDefault().getPath(
				Preferences.userNodeForPackage(VirtualDJScrobbler.class).get(VirtualDJScrobbler.TRACKLIST_FILE_PREFERENCE, ""));
		keys.clear();
		register(trackListPath.getParent());
		log.debug("Updated watched directory to: " + trackListPath.getParent());
	}

	@SuppressWarnings("unchecked")
	public void processEvents() {
		while (true) {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException e) {
				log.error("Waiting for watcher interrupted", e);
				continue;
			}
			Path dir = keys.get(key);
			if (dir == null) {
				log.error("WatchKey not recognized");
				continue;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<Path> kind = (Kind<Path>) event.kind();
				if (kind != StandardWatchEventKinds.ENTRY_MODIFY) {
					continue;
				}
				Path name = (Path) event.context();
				Path child = dir.resolve(name);
				if (!child.equals(trackListPath)) {
					continue;
				}
				processFileChange();
			}
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);
				if (keys.isEmpty()) {
					break;
				}
			}
		}
	}

	private void processFileChange() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(trackListPath.toFile()));
			String lastNonEmptyLine = null;
			String lastDateLine = "";
			String line = br.readLine();
			while (line != null) {
				if (!"".equals(line)) {
					lastNonEmptyLine = line;
				}
				if (line.startsWith("VirtualDJ History - ")) {
					lastDateLine = line;
				}
				line = br.readLine();
			}
			if (lastNonEmptyLine != null && !lastNonEmptyLine.equals(lastLineHandled) && !lastNonEmptyLine.startsWith("VirtualDJ History")
					&& !lastNonEmptyLine.startsWith("-----------------")) {
				log.debug("Last line is: " + lastNonEmptyLine);
				log.debug("Last date line is:" + lastDateLine);
				lastLineHandled = lastNonEmptyLine;
				String[] dateArray = lastDateLine.replace("VirtualDJ History - ", "").split("/");
				virtualDJScrobbler.handleNewLineInTrackListFile(lastNonEmptyLine, dateArray);
			}
		} catch (FileNotFoundException e) {
			log.error("Tracklist file doesn't exist", e);
		} catch (IOException e) {
			log.error("Tracklist file could not be read", e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				log.debug("Couldn't close reader", e);
			}
		}
	}
}
