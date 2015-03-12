# Settings #
## 1. Setting the path to the tracklist file ##

The program will assume that the tracklist file is in the place where VirtualDJ places it, for example in Windows XP with English as language:

`C:\Documents And Settings\<Username>\My Documents\VirtualDJ\Tracklisting\tracklist.txt`

if the document isn't in the "correct" place or if it doesn't appear to be valid (see FAQ) you will be asked to select the location of the tracklist.txt-file.

If you later, for some reason, need to change the location of your tracklist file you can right-click on the icon in the system tray and select "Set tracklist file" from the menu.


## 2. Setting how often the program will check for new tracks ##

The program reads the tracklist.txt file to determine what tracks you play, the default interval between these checks is 30seconds. If you want to change this you can right-click on the icon in the system tray and select "Set file checking interval". Just move the slider to the desired amount of seconds between checks.

## 3. Adding users ##

For the program to be able to scrobble anything it needs at least one user, to set a user right-click the system tray icon and choose "Add user". Enter your credentials in the fields and press enter. A validation will be made against last.fm and while this is going on you will see an indeterminate progress bar. Pressing cancel here will cancel the validation and the user won't be added. If the user is properly validated or if some error occurs (for example if the username or password was wrong) you will be notified.

Users are saved between between program reboots so you won't need to re-add your user every time.

Note: An Internet connection is needed when adding users, otherwise the program won't be able to validate the user.

## 4. Checking what users are stored and removing users ##

If you want to see what users are stored or if you want to remove one or more users right-click the system tray icon and press "Remove users". You will get a dialog where you see all users that are currently saved. Pressing the "Remove"-button next to a user will remove the users and no more song will be scrobbled for this user.

## 5. Stop popups from showing ##

If you are annoyed by the popups stating what song is beeing scrobbled just right-click the system tray icon and deselect the "Show tray popups" item in the menu.

If you later on change your mind just select it again.

## 6. Skipping the splash-screen ##

If you think the three seconds it takes for the wonderful splash-screen to disappear takes like forever just right-click the system tray icon and deselect the "Show splash-screen" item in the menu.

If you later on change your mind just select it again.

Note: A splash-screen might popup for a very short while even if the option is deselected, this is due to the fact that the splash-screen cannot be hidden before the java runtime machine has fully started the program.

## 7. VirtualDJ Options ##
### 7.1 Autostarting VirtualDJ ###
If you would like VirtualDJScrobbler to automatically start VirtualDJ when starting
up check this option.

If the path to the VirtualDJ executable hasn't been set (see section 5.7.2) you will
be requested to set the path when selecting autostart. If you cancel this popup
the autostart option will not be selected.

### 7.2 Setting the path to the VirtualDJ executable ###
To be able to use the autostart VirtualDJ function you must set the path to the
VirtualDJ executable. To do this simply press the "Set VirtualDJ executable" option
and either enter the full path to the "virtualdj.exe" file, or select browse and
locate the executable in the file explorer.

## 8. Scrobbler Options ##
### 8.1 Autoexit when VirtualDJ is closed ###
If you want VirtualDJScrobbler to automatically exit once VirtualDJ is closed
you can enable it by clicking the "Exit when VDJ is closed" option.

This option depends on the "Autostart VirtualDJ" option and will only be possible
to set when autostart is activated. The reason for this is that the only way
VirtualDJScrobbler can know about VirtualDJ is if it has been started from within
VirtualDJScrobbler, i.e. via the autostart option.

### 8.2 Updating VirtualDJScrobbler ###
You can set VirtualDJScrobbler to automatically check for updates during startup, to enable this check the "Check for updates on startup" option under the menu "Scrobbler options"

You can also manually check for updates here by clicking the "Check for updates now" option.