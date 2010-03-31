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

import static se.tingne.vdjscrobbler.util.MD5Util.md5;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.PopupMenu;
import java.awt.SplashScreen;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileLock;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;

import se.tingne.vdjscrobbler.data.LastFMTrack;
import se.tingne.vdjscrobbler.data.LastFMUser;
import se.tingne.vdjscrobbler.enums.Source;
import se.tingne.vdjscrobbler.queue.QueueReaderThread;
import se.tingne.vdjscrobbler.workerthreads.NowPlayingThread;
import se.tingne.vdjscrobbler.workerthreads.ValidateUserThread;


/**
 * @author Magnus Tingne
 */
public class VirtualDJScrobbler extends Thread {
	private static final String NAME = "VirtualDJScrobbler";
	private static final String VERSION = "Beta";

	private static final String USERS_PREFERENCE = "USERS";
	private static final String TRACKLIST_FILE_PREFERENCE = "TRACKLIST";
	private static final String REFRESH_INTERVAL_PREFERENCE = "REFRESH";
	private static final String POPUPS_PREFERENCE = "POPUPS";
	private static final String SPLASH_PREFERENCE = "SPLASH";
	private static Logger log = org.apache.log4j.Logger
			.getLogger(VirtualDJScrobbler.class);
	private Calendar lastTime = Calendar.getInstance(Locale.getDefault());
	private TrayIcon trayIcon;
	private Map<String, LastFMUser> users;
	private Preferences preferences;
	private Queue<LastFMTrack> trackQueue;
	private LastFMTrack trackToScrobble;

	private JDialog validationDialog;
	private boolean validationSuccess;

	private boolean dialogShowing;

	private JFrame mainFrame;
	private File lockFile;
	private FileLock lock;
	private boolean isValidating;

	public VirtualDJScrobbler() {
		log.debug("--------------");
		log.debug("Starting up...");
		log.debug("--------------");
		setErrorStream();
		preferences = Preferences.userNodeForPackage(this.getClass());
		boolean locked = true;
		try {
			locked = obtainLock();
		} catch (IOException e) {
			log.error("Error in obtaining lock", e);
			locked = false;
		}
		if (!locked) {
			log.warn("Could not obtain lock");
			JOptionPane
					.showMessageDialog(
							mainFrame,
							"Only one instance of "
									+ NAME
									+ " can be run at a time, please exit all other instances.\nIf no other instances are running try going to program directory and delete the \".lock\" file",
							"Multiple instances disallowed",
							JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		long splashTime = 3000;
		final SplashScreen splash = SplashScreen.getSplashScreen();
		if (splash == null) {
			log.warn("No splash-screen available");
		} else {
			if (preferences.getBoolean(SPLASH_PREFERENCE, true)) {
				Graphics2D g = splash.createGraphics();
				if (g == null) {
					splashTime = 0;
					log.warn("Couldn't get graphics for splash screen");
				}
				try {
					Thread.sleep(splashTime);
					splash.close();
				} catch (InterruptedException e) {
				}
			} else {
				splash.close();
			}
		}
		validationSuccess = false;
		dialogShowing = false;
		mainFrame = new JFrame();
		mainFrame.setUndecorated(true);
		mainFrame.pack();
		mainFrame.setLocationRelativeTo(null);
		this.setName("VirtualDJScrobbler");
		loadQueue();
		if (preferences.get(TRACKLIST_FILE_PREFERENCE, "").equals("")) {
			preferences.put(TRACKLIST_FILE_PREFERENCE, new JFileChooser()
					.getFileSystemView().getDefaultDirectory()
					+ "/VirtualDJ/Tracklisting/tracklist.txt");
		}
		lastTime.setTimeInMillis(System.currentTimeMillis());
		users = getUsersPreference();
		createQueueReaderThreads();
		SystemTray systemTray = SystemTray.getSystemTray();
		URL url = ClassLoader.getSystemResource("tray.png");
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		trayIcon = new TrayIcon(image);
		trayIcon.setToolTip("VirtualDJScrobbler " + VERSION);
		trayIcon.setImageAutoSize(true);
		PopupMenu menu = new PopupMenu();
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit(0);
			}
		});
		final CheckboxMenuItem popupItem = new CheckboxMenuItem(
				"Show tray popups", preferences.getBoolean(POPUPS_PREFERENCE,
						true));
		popupItem.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				preferences.putBoolean(POPUPS_PREFERENCE, popupItem.getState());
				log.info("Popups are now: " + popupItem.getState());
			}
		});
		final CheckboxMenuItem splashScreenItem = new CheckboxMenuItem(
				"Show splash-screen", preferences.getBoolean(SPLASH_PREFERENCE,
						true));
		splashScreenItem.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				preferences.putBoolean(SPLASH_PREFERENCE, splashScreenItem
						.getState());
				log
						.info("Splash screen is now: "
								+ splashScreenItem.getState());
			}
		});
		final MenuItem addUserItem = new MenuItem("Add user");
		addUserItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!dialogShowing) {
					dialogShowing = true;
					addUser();
					dialogShowing = false;
				} else {
					mainFrame.toFront();
				}
			}
		});
		final MenuItem removeUsersItem = new MenuItem("Remove users");
		removeUsersItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!dialogShowing) {
					dialogShowing = true;
					removeUser();
					dialogShowing = false;
				} else {
					mainFrame.toFront();
				}
			}
		});
		MenuItem tracklistFileItem = new MenuItem("Set tracklist file");
		tracklistFileItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!dialogShowing) {
					dialogShowing = true;
					preferences.put(TRACKLIST_FILE_PREFERENCE,
							showTracklistFileChooser(preferences.get(
									TRACKLIST_FILE_PREFERENCE, "")));
					log.debug("Path to tracklist file set to: "
							+ preferences.get(TRACKLIST_FILE_PREFERENCE,
									"Couldn't get preference"));
					dialogShowing = false;
				} else {
					mainFrame.toFront();
				}
			}
		});
		MenuItem refreshIntervalItem = new MenuItem(
				"Set file checking interval");
		refreshIntervalItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!dialogShowing) {
					dialogShowing = true;
					setRefreshInterval();
					dialogShowing = false;
				} else {
					mainFrame.toFront();
				}
			}
		});
		MenuItem aboutItem = new MenuItem("About");
		final JEditorPane editorPane = createEditorPane(generateAboutHTML());
		aboutItem.setShortcut(new MenuShortcut(KeyEvent.VK_A, false));
		aboutItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!dialogShowing) {
					dialogShowing = true;
					about(editorPane);
					dialogShowing = false;
				} else {
					mainFrame.toFront();
				}
			}

		});
		menu.add(aboutItem);
		menu.addSeparator();
		menu.add(refreshIntervalItem);
		menu.add(tracklistFileItem);
		menu.addSeparator();
		menu.add(removeUsersItem);
		menu.add(addUserItem);
		menu.addSeparator();
		menu.add(popupItem);
		menu.add(splashScreenItem);
		menu.addSeparator();
		menu.add(exitItem);
		trayIcon.setPopupMenu(menu);
		try {
			systemTray.add(trayIcon);
			if (preferences.getBoolean(POPUPS_PREFERENCE, true)) {
				trayIcon
						.displayMessage(
								NAME + " started",
								NAME
										+ " "
										+ VERSION
										+ " is now running in the background, right click the system tray icon for options.",
								MessageType.INFO);
			}
		} catch (AWTException e) {
			log.error("Couldn't add tray icon", e);
		}
	}

	private boolean obtainLock() throws IOException {
		lockFile = new File(".lock");
		if (!lockFile.exists()) {
			boolean createNewFile = lockFile.createNewFile();
			if (!createNewFile) {
				return false;
			}
		}
		lock = new FileOutputStream(lockFile).getChannel().tryLock();
		return lock != null;
	}

	@Override
	public void run() {
		Calendar calendar = Calendar.getInstance(Locale.getDefault());
		// ArrayList<TrackInfo> tracks = new ArrayList<TrackInfo>();
		// ArrayList<TrackInfo> latestTracks = new ArrayList<TrackInfo>();
		BufferedReader br = null;
		trackToScrobble = null;

		while (true) {
			// log.debug("Reading");
			// latestTracks.addAll(tracks);
			// tracks = new ArrayList<TrackInfo>();
			String filePath = preferences.get(TRACKLIST_FILE_PREFERENCE, "");
			File f = new File(filePath);
			String validationResult;
			while (!(validationResult = validateTrackListFile(f)).equals("")) {
				log.warn(validationResult);
				mainFrame.setVisible(true);
				mainFrame.setTitle(NAME + " - Invalid tracklist file");
				JOptionPane.showMessageDialog(mainFrame, validationResult
						+ ".\n\rPlease choose another file.",
						"Invalid tracklist file", JOptionPane.WARNING_MESSAGE);
				mainFrame.setVisible(false);
				filePath = showTracklistFileChooser(filePath);
				if (filePath != null) {
					f = new File(filePath);
				}
			}
			preferences.put(TRACKLIST_FILE_PREFERENCE, filePath);

			try {
				br = new BufferedReader(new FileReader(f));
				String line = br.readLine();
				String dateString = "";
				while (line != null
						&& (line.startsWith("VirtualDJ History")
								|| line.startsWith("-----------------") || line
								.length() == 0)) {
					if (line.startsWith("VirtualDJ History")) {
						dateString = line.replace("VirtualDJ History - ", "");
					}
					line = br.readLine();
				}
				String[] dateArray = dateString.split("/");
				while (line != null) {
					calendar.set(Integer.parseInt(dateArray[0]), Integer
							.parseInt(dateArray[1]) - 1, Integer
							.parseInt(dateArray[2]), Integer.parseInt(line
							.substring(0, 2)), Integer.parseInt(line.substring(
							3, 5)));
					if (calendar.after(lastTime)) {
						lastTime.setTimeInMillis(calendar.getTimeInMillis());
						log.debug("Added " + line.substring(7));
						String artist, title;
						if (line.indexOf(" - ") == -1) {
							artist = "Unknown artist";
							title = line.substring(line.indexOf(" : ") + 3);
						} else {
							artist = line.substring(line.indexOf(" : ") + 3,
									line.indexOf(" - "));
							title = line.substring(line.indexOf(" - ") + 3);
						}
						scrobbleTrack();
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							log
									.error(
											"Sleep interrupted while waiting to submit now playing track",
											e);
						}
						LastFMTrack nowPlayingTrack = new LastFMTrack(artist,
								title,
								(int) (calendar.getTimeInMillis() / 1000), 240,
								Source.P);
						for (String user : users.keySet()) {
							new NowPlayingThread(this, users.get(user),
									nowPlayingTrack).start();
						}
						trackToScrobble = nowPlayingTrack;
					} else {
						if (trackToScrobble != null
								&& (int) (System.currentTimeMillis() / 1000)
										- trackToScrobble.getStartTime() >= 240) {
							scrobbleTrack();
						}
						line = br.readLine();
					}
					while (line != null
							&& (line.startsWith("VirtualDJ History")
									|| line.startsWith("-----------------") || line
									.length() == 0)) {
						if (line.startsWith("VirtualDJ History")) {
							dateString = line.replace("VirtualDJ History - ",
									"");
						}
						line = br.readLine();
					}
					dateArray = dateString.split("/");
				}

				// if (tracks.size() != 0) {
				// uploadTracks(tracks);
				// } else {
				// log.debug("No new tracks to scrobble");
				// }
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
			try {
				Thread.sleep(preferences
						.getInt(REFRESH_INTERVAL_PREFERENCE, 30) * 1000);
			} catch (InterruptedException e) {
				log.debug("Thread sleep interrupted", e);
			}
		}
	}

	private void setErrorStream() {
		File file = new File("./errorstream.log");
		try {
			PrintStream printStream = new PrintStream(new FileOutputStream(
					file, true));
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
					"yyMMDD HH:mm:ss");
			printStream
					.write("-------------------------------------------------------------\r\n"
							.getBytes());
			printStream.write(new String(simpleDateFormat.format(new Date(
					System.currentTimeMillis()))
					+ " - Starting new vdjscrobbler\r\n").getBytes());
			printStream
					.write("-------------------------------------------------------------\r\n"
							.getBytes());
			System.setErr(printStream);
		} catch (FileNotFoundException e) {
			log.error("Couldn't set error stream to file", e);
		} catch (IOException e) {
			log.error("Couldn't set error stream to file", e);
		}
	}

	private void scrobbleTrack() {
		if (trackToScrobble != null) {
			int length = (int) (System.currentTimeMillis() / 1000 - trackToScrobble
					.getStartTime());
			if (length >= 30) {
				trackToScrobble
						.setLength((int) (System.currentTimeMillis() / 1000 - trackToScrobble
								.getStartTime()));
				trackQueue.offer(trackToScrobble);
				synchronized (trackQueue) {
					trackQueue.notifyAll();
				}
				log.info("Putting " + trackToScrobble + " in the queue.");
				if (preferences.getBoolean(POPUPS_PREFERENCE, true)) {
					trayIcon.displayMessage("Scrobbling", "Putting "
							+ trackToScrobble.getArtist() + " - "
							+ trackToScrobble.getTitle() + " in the queue.",
							MessageType.INFO);
				}
			} else {
				log.info("Track: " + trackToScrobble + " to short (length: "
						+ length + "s), skipping scrobbling");
			}
		}
	}

	private void createQueueReaderThreads() {
		if (users != null) {
			for (String user : users.keySet()) {
				new QueueReaderThread(trackQueue, this, users.get(user))
						.start();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void loadQueue() {
		File queueFile = new File("./trackQueue");
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					queueFile));
			trackQueue = (LinkedList<LastFMTrack>) ois.readObject();
			log.debug("Track queue loaded: " + trackQueue);
			try {
				ois.close();
			} catch (Exception e) {
			}
		} catch (FileNotFoundException e) {
			log
					.warn(
							"Could not find tracksQueue file, creating a new queue",
							e);
		} catch (IOException e) {
			log
					.warn(
							"Could not read tracksQueue file, creating a new queue",
							e);
		} catch (ClassNotFoundException e) {
			log
					.warn(
							"Could not read tracksQueue file, creating a new queue",
							e);
		}
		if (trackQueue == null) {
			trackQueue = new LinkedList<LastFMTrack>();
		}
	}

	protected String showTracklistFileChooser(String originalPath) {
		final JFileChooser fileChooser = new JFileChooser();
		// FileSystemView fw = fileChooser.getFileSystemView();
		// fw.getDefaultDirectory();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.getName().equals("tracklist.txt") || f.isDirectory();
			}

			@Override
			public String getDescription() {
				return "tracklist.txt";
			}
		});
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileHidingEnabled(true);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setCurrentDirectory(new File(fileChooser
				.getFileSystemView().getDefaultDirectory()
				+ "/VirtualDJ/Tracklisting/"));

		final JTextField textField = new JTextField(originalPath);
		textField.setColumns(100);
		JButton button = new JButton("Browse...");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = null;
				int openDialog = fileChooser.showOpenDialog(null);
				if (openDialog == JFileChooser.APPROVE_OPTION) {
					selectedFile = fileChooser.getSelectedFile();
					String validatationResult = validateTrackListFile(selectedFile);
					if (validatationResult.equals("")) {
						textField.setText(fileChooser.getSelectedFile()
								.getAbsolutePath());
					} else {
						mainFrame.setVisible(true);
						mainFrame.setTitle(NAME + " - Invalid tracklist file");
						JOptionPane.showMessageDialog(mainFrame,
								validatationResult
										+ ".\n\rPlease choose another file.",
								"Invalid tracklist file",
								JOptionPane.WARNING_MESSAGE);
						mainFrame.setVisible(false);
					}
				}
			}
		});

		JPanel panel = new JPanel();
		panel.add(textField);
		panel.add(button);

		mainFrame.setVisible(true);
		mainFrame.setTitle(NAME + " - Set path to tracklist file");
		JOptionPane.showMessageDialog(mainFrame, panel,
				"Set path to tracklist file", JOptionPane.OK_CANCEL_OPTION);
		mainFrame.setVisible(false);

		return textField.getText();
	}

	protected String validateTrackListFile(File selectedFile) {
		if (selectedFile == null) {
			return "File is null";
		} else {
			try {
				BufferedReader bufferedReader = new BufferedReader(
						new FileReader(selectedFile));
				String[] firstThreeLines = new String[3];
				for (int i = 0; i < 3; i++) {
					firstThreeLines[i] = bufferedReader.readLine();
				}
				Pattern pattern = Pattern
						.compile("VirtualDJ History - (19|20)\\d\\d[- /.](0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])");
				if (firstThreeLines[0] == null
						|| firstThreeLines[0].length() != 0) {
					log
							.debug("File: "
									+ selectedFile.getAbsolutePath()
									+ " is not a valid tracklist file since it doesn't start with an empty line");
					return "File: "
							+ selectedFile.getAbsolutePath()
							+ " is not a valid tracklist file since it doesn't start with an empty line";
				} else if (firstThreeLines[1] == null
						|| !pattern.matcher(firstThreeLines[1]).matches()) {
					log
							.debug("File: "
									+ selectedFile.getAbsolutePath()
									+ " is not a valid tracklist file since the second line doesn't start with \"VirtualDJ History - YYYY/MM/DD\"");
					return "File: "
							+ selectedFile.getAbsolutePath()
							+ " is not a valid tracklist file since the second line doesn't start with \"VirtualDJ History - YYYY/MM/DD\"";
				} else if (firstThreeLines[2] == null
						|| !firstThreeLines[2]
								.equals("------------------------------")) {
					log
							.debug("File: "
									+ selectedFile.getAbsolutePath()
									+ " is not a valid tracklist file since the third line doesn't equal \"------------------------------\"");
					return "File: "
							+ selectedFile.getAbsolutePath()
							+ " is not a valid tracklist file since the third line doesn't equal \"------------------------------\"";
				} else {
					log
							.debug("File: "
									+ selectedFile.getAbsolutePath()
									+ " appears to be a valid virtual dj tracklist file");
					return "";
				}
			} catch (FileNotFoundException e) {
				log
						.debug("File: "
								+ selectedFile.getAbsolutePath()
								+ " is not a valid tracklist file since it doesn't exist");
				return "File: "
						+ selectedFile.getAbsolutePath()
						+ " is not a valid tracklist file since it doesn't exist";
			} catch (IOException e) {
				log
						.debug("File: "
								+ selectedFile.getAbsolutePath()
								+ " is not a valid tracklist file since it cannot be read");
				return "File: "
						+ selectedFile.getAbsolutePath()
						+ " is not a valid tracklist file since it cannot be read";
			}
		}
	}

	private void setUsersPreference(Map<String, LastFMUser> users) {
		String value = "";
		for (String key : users.keySet()) {
			value += "<user>" + key + "=" + users.get(key).getMd5password()
					+ "</user>";
		}
		preferences.put(USERS_PREFERENCE, value);
	}

	private Map<String, LastFMUser> getUsersPreference() {
		Map<String, LastFMUser> users = new HashMap<String, LastFMUser>();
		String value = preferences.get(USERS_PREFERENCE, "");
		if (value.length() != 0) {
			List<String> usersStrings = Arrays.asList(value.split("<user>"));
			usersStrings = usersStrings.subList(1, usersStrings.size());
			int i = 0;
			for (String user : usersStrings) {
				user = user.replace("</user>", "");
				String[] split = user.split("=");
				users
						.put(split[0], new LastFMUser(split[0], split[1],
								i * 250));
				i++;
			}
		}
		return users;
	}

	public void reportClientBanned() {
		mainFrame.setVisible(true);
		mainFrame.setTitle(NAME + " - Client Banned");
		dialogShowing = true;
		JOptionPane.showMessageDialog(mainFrame,
				createEditorPane(generateBannedHTML()), "Client Banned",
				JOptionPane.ERROR_MESSAGE);
		mainFrame.setVisible(false);
		log.fatal("The client has been reported as banned");
		System.exit(1);
	}

	public void reportBadAuth(LastFMUser user) {
		if (validationDialog != null && isValidating) {
			isValidating = false;
			validationDialog.setVisible(false);
			validationSuccess = false;
			log.info("Bad auth reported on user validation");
		} else {
			mainFrame.setVisible(true);
			mainFrame.setTitle(NAME + " - Bad Auth");
			dialogShowing = true;
			JOptionPane
					.showMessageDialog(
							mainFrame,
							"The credentials of user: "
									+ user.getUsername()
									+ " is wrong, please re-add the user in the next dialog if you want to keep it.",
							"Bad Auth", JOptionPane.ERROR_MESSAGE);
			users.remove(user.getUsername());
			setUsersPreference(users);
			log
					.warn("User "
							+ user.getUsername()
							+ " has wrong credentials has been removed and re-add popup will be shown.");
			mainFrame.setVisible(false);
			addUser();
			dialogShowing = false;
		}
	}

	public void reportBadTime() {
		mainFrame.setVisible(true);
		mainFrame.setTitle(NAME + " - Bad Time");
		dialogShowing = true;
		JOptionPane
				.showMessageDialog(
						mainFrame,
						"Your computer clock is to much of the actual time, please adjust and restart program",
						"Bad Time", JOptionPane.ERROR_MESSAGE);
		mainFrame.setVisible(false);
		log
				.fatal("Users computer clock is to much of the actual time, exiting");
		exit(1);
	}

	private void exit(int status) {
		int length;
		if (trackToScrobble != null
				&& (length = (int) (System.currentTimeMillis() / 1000)
						- trackToScrobble.getStartTime()) >= 30) {
			trackToScrobble.setLength(length);
			trackQueue.offer(trackToScrobble);
			synchronized (trackQueue) {
				trackQueue.notifyAll();
			}
			try {
				Thread.sleep(500); // give reader a little time to scrobble
			} catch (InterruptedException e) {
				log.error("Waiting for last track to be submited interrupted",
						e);
			}
		}
		storeQueue();
		try {
			lock.release();
		} catch (IOException e) {
			log.error("Error when releasing lock", e);
		}
		log.debug("Exiting");
		System.exit(status);
	}

	private void storeQueue() {
		try {
			FileOutputStream fos = new FileOutputStream(
					new File("./trackQueue"));

			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(trackQueue);
			oos.flush();
			oos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			log
					.error("Couldn't save tracks queue, new queue will be created next time application is run");
		} catch (IOException e) {
			log
					.error("Couldn't save tracks queue, new queue will be created next time application is run");
		}
	}

	private void addUser() {
		final JPanel addUserPanel = new JPanel(new GridLayout(2, 1));
		final JTextField userName = new JTextField("");
		final JPasswordField password = new JPasswordField("");
		addUserPanel.add(new JLabel("Username: "), 0);
		addUserPanel.add(userName, 1);
		addUserPanel.add(new JLabel("Password: "), 2);
		addUserPanel.add(password, 3);
		boolean validated = false;
		int confirmed = Integer.MAX_VALUE;
		while (!validated && confirmed != JOptionPane.CANCEL_OPTION) {
			mainFrame.setVisible(true);
			mainFrame.setTitle(NAME + " - Add user");
			confirmed = JOptionPane.showConfirmDialog(mainFrame, addUserPanel,
					"Add user", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);
			mainFrame.setVisible(false);
			if (confirmed == JOptionPane.OK_OPTION
					&& userName.getText().length() > 0
					&& password.getPassword().length > 0) {
				try {
					LastFMUser user = new LastFMUser(userName.getText(),
							md5(new String(password.getPassword())), users
									.size() * 250);
					if (users.get(user.getUsername()) != null) {
						mainFrame.setVisible(true);
						mainFrame.setTitle(NAME + " - Didn't add user");
						JOptionPane.showMessageDialog(mainFrame, "User: "
								+ user.getUsername() + " already added",
								"Didn't add user", JOptionPane.WARNING_MESSAGE);
						mainFrame.setVisible(false);
						log.warn("User: " + user.getUsername()
								+ " already added");
						validated = true;
					} else if (users.size() == 50) {
						mainFrame.setVisible(true);
						mainFrame.setTitle(NAME + " - Didn't add user");
						JOptionPane
								.showMessageDialog(
										mainFrame,
										"User: "
												+ user.getUsername()
												+ " couldn't be added, maximum allowed number of users is 50.",
										"Didn't add user",
										JOptionPane.WARNING_MESSAGE);
						mainFrame.setVisible(false);
						log
								.warn("User: "
										+ user.getUsername()
										+ " couldn't be added, maximum allowed number of users is 50.");
						validated = true;
					} else {
						validated = validateUser(user);
						if (validated) {
							users.put(userName.getText(), user);
							new QueueReaderThread(trackQueue, this, user)
									.start();
							log.info("Adding user: " + userName.getText());
							mainFrame.setVisible(true);
							mainFrame.setTitle(NAME + " - User added");
							JOptionPane.showMessageDialog(mainFrame, "User: "
									+ user.getUsername()
									+ " successfully added", "User added",
									JOptionPane.PLAIN_MESSAGE);
							mainFrame.setVisible(false);
							setUsersPreference(users);
						} else {
							mainFrame.setVisible(true);
							mainFrame.setTitle(NAME + " - Bad auth");
							JOptionPane
									.showMessageDialog(
											mainFrame,
											"Bad username/password, please change and try again",
											"Bad auth",
											JOptionPane.WARNING_MESSAGE);
							mainFrame.setVisible(false);
						}
					}
				} catch (NoSuchAlgorithmException e1) {
					log.error("MD5 failed", e1);
				}
			}
		}
	}

	public void reportValidationSuccessful() {
		validationDialog.setVisible(false);
		validationSuccess = true;
		log.info("Reporting successful validation");
	}

	private boolean validateUser(LastFMUser user) {
		isValidating = true;
		validationSuccess = false;
		ValidateUserThread validateUserThread = new ValidateUserThread(this,
				user);
		validateUserThread.start();
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		JOptionPane optionPane = new JOptionPane(progressBar,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
				null, new Object[] { "Cancel" }, null);
		validationDialog = optionPane.createDialog("Validating user");
		if (isValidating) {
			validationDialog.setVisible(true);
		}
		if (!validationSuccess) {
			validateUserThread.interrupt();
		}
		return validationSuccess;
	}

	private void removeUser() {
		Map<String, LastFMUser> savedUsers = getUsersPreference();
		final JPanel removeUsersPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.ipadx = 5;
		c.ipady = 5;
		for (final String username : savedUsers.keySet()) {
			final JLabel userLabel = new JLabel(username);
			removeUsersPanel.add(userLabel, c);
			JButton removeButton = new JButton("Remove");
			removeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					mainFrame.setTitle(NAME + " - Confirm removal");
					int showConfirmDialog = JOptionPane.showConfirmDialog(
							mainFrame, "Are you sure you want to remove: "
									+ username, "Confirm removal",
							JOptionPane.YES_NO_OPTION);
					mainFrame.setTitle(NAME + " - Remove users");
					if (showConfirmDialog == JOptionPane.YES_OPTION) {
						users.remove(username);
						((JButton) e.getSource()).setEnabled(false);
						userLabel.setEnabled(false);
						log.debug("Removing user: " + username);
					}
				}
			});
			c.gridx = 1;
			removeUsersPanel.add(removeButton, c);
			c.gridx = 0;
			c.gridy++;
			removeUsersPanel.add(Box.createVerticalStrut(5), c);
			c.gridy++;
		}
		mainFrame.setVisible(true);
		mainFrame.setTitle(NAME + " - Remove users");
		JOptionPane.showMessageDialog(mainFrame, removeUsersPanel,
				"Remove users", JOptionPane.PLAIN_MESSAGE);
		mainFrame.setVisible(false);
		setUsersPreference(users);
	}

	private void setRefreshInterval() {
		JSlider slider = new JSlider(20, 120);
		slider.setValue(preferences.getInt(REFRESH_INTERVAL_PREFERENCE, 30));
		Hashtable<Integer, JLabel> sliderTable = new Hashtable<Integer, JLabel>();
		for (int i = 0; i <= 120; i += 20) {
			sliderTable.put(i, new JLabel(i + "s"));
		}
		slider.setLabelTable(sliderTable);
		slider.setMajorTickSpacing(20);
		slider.setMinorTickSpacing(05);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setPaintTrack(true);
		slider.setSnapToTicks(true);
		slider.setMinimumSize(new Dimension(300, 20));

		mainFrame.setVisible(true);
		mainFrame.setTitle(NAME + " - Set file checking interval");
		JOptionPane.showMessageDialog(mainFrame, slider,
				"Set file checking interval", JOptionPane.PLAIN_MESSAGE);
		mainFrame.setVisible(false);
		preferences.putInt(REFRESH_INTERVAL_PREFERENCE, slider.getValue());
		log.debug("Refresh interval now set to: " + slider.getValue());
	}

	private JEditorPane createEditorPane(String html) {
		JEditorPane editorPane = new JEditorPane("text/html", html);
		editorPane.setMinimumSize(new Dimension(500, 200));
		editorPane.setEditable(false);
		editorPane.setBackground(new Color(0, 0, 0, 0));
		editorPane.addHyperlinkListener(new VDJHyperLinkListener());
		return editorPane;
	}

	private String generateBannedHTML() {
		StringBuffer html = new StringBuffer();
		html.append("<html>");
		html.append("<body>");
		html.append("<p style=\"font-family:tahoma;font-size:11\">");
		html.append("This client has sadly been banned, please send email to ");
		html
				.append("<a href=mailto:vdjscrobbler@gmail.com?subject=Client%20(version:%20"
						+ VERSION + ")%20banned>vdjscrobbler@gmail.com</a>");
		html.append("</p>");
		html.append("</body>");
		html.append("</html>");
		return html.toString();
	}

	private String generateAboutHTML() {
		StringBuffer html = new StringBuffer();
		html.append("<html>");
		html.append("<body>");
		html.append("<p style=\"font-family:tahoma;font-size:11\">");
		html.append("<b>Version:</b>&nbsp;" + VERSION);
		html.append("<br/><b>Created by:</b>&nbsp;Magnus Tingne 2009");
		html.append("<br/><b>Copyright:</b>&nbsp;Magnus Tingne 2009");
		URL gplUrl = ClassLoader.getSystemResource("licence.txt");
		html
				.append("<br/><b>Licence:</b>&nbsp;This software is released under the");
		html
				.append("<br/><a href=http://www.gnu.org/licenses/gpl.txt>GNU General Public License v3</a>");
		URL poweredByUrl = ClassLoader.getSystemResource("poweredby.png");
		html
				.append("<br/><b>Support:</b>&nbsp;<a href=mailto:vdjscrobbler@gmail.com?subject=support>vdjscrobbler@gmail.com</a>");
		URL donateUrl = ClassLoader.getSystemResource("donate.gif");
		html
				.append("<br/><br/><a href=https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=5NRMM5NEBFQPW&lc=SE&item_name=VirtualDJScrobbler&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted><img alt=\"Donate\" src="
						+ donateUrl + " border=\"0\" align=\"middle\"/></a>");
		html
				.append("<br/><br/><a href=http://www.lastfm.com><img alt=\"Powered by Audioscrobbler\" src="
						+ poweredByUrl + " border=\"0\" align=\"middle\"/></a>");
		html.append("</p>");
		html.append("</body>");
		html.append("</html>");

		return html.toString();
	}

	private void about(final JEditorPane editorPane) {
		log.debug("Showing about screen");
		URL url = ClassLoader.getSystemResource("logo.png");
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		ImageIcon imageIcon = new ImageIcon(image);
		mainFrame.setTitle(NAME + " - About");
		mainFrame.setVisible(true);
		JOptionPane.showMessageDialog(mainFrame, editorPane, "About",
				JOptionPane.PLAIN_MESSAGE, imageIcon);
		mainFrame.setVisible(false);
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			log.warn("Could not set system look and feel", e);
		} catch (InstantiationException e) {
			log.warn("Could not set system look and feel", e);
		} catch (IllegalAccessException e) {
			log.warn("Could not set system look and feel", e);
		} catch (UnsupportedLookAndFeelException e) {
			log.warn("Could not set system look and feel", e);
		}
		VirtualDJScrobbler vdjs = new VirtualDJScrobbler();
		vdjs.start();
	}

	private class VDJHyperLinkListener implements HyperlinkListener {
		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType() == EventType.ACTIVATED) {
				boolean desktopSupported = Desktop.isDesktopSupported();
				if (desktopSupported) {
					if (e.getDescription().startsWith(
							"mailto:vdjscrobbler@gmail.com")) {
						try {
							log.debug("Starting mail program");
							Desktop.getDesktop().mail(
									new URI(e.getDescription()));
						} catch (IOException e1) {
							log.error("Could not start mail program", e1);
						} catch (URISyntaxException e1) {
							log.error("Could not start mail program", e1);
						}
					} else {
						try {
							log.debug("Browsing to " + e.getURL());
							Desktop.getDesktop().browse(e.getURL().toURI());
						} catch (IOException e1) {
							log.error("Could not open browser", e1);
						} catch (URISyntaxException e1) {
							log.error("Could not open browser", e1);
						}
					}
				} else {
					log
							.info("Desktop API not supported, browser/mail program cannot be opened");
				}
			}
		}

	}
}
