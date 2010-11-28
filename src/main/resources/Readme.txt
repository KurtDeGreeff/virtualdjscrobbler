--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
||                                                                            ||
||    888     888 d8b         888                      888 8888888b. 888888   ||
||    888     888 Y8P         888                      888 888  "Y88b  "88b   ||
||    888     888             888                      888 888    888   888   ||
||    Y88b   d88P 888 888d888 888888 888  888  8888b.  888 888    888   888   ||
||     Y88b d88P  888 888P"   888    888  888     "88b 888 888    888   888   ||
||      Y88o88P   888 888     888    888  888 .d888888 888 888    888   888   ||
||       Y888P    888 888     Y88b.  Y88b 888 888  888 888 888  .d88P   88P   ||
||        Y8P     888 888      "Y888  "Y88888 "Y888888 888 8888888P"    888   ||
||                                                                    .d88P   ||
||                                                                  .d88P"    ||
||                                                                 888P"      ||
||  .d8888b.                           888      888      888                  ||
|| d88P  Y88b                          888      888      888                  ||
|| Y88b.                               888      888      888                  ||
||  "Y888b.    .d8888b 888d888 .d88b.  88888b.  88888b.  888  .d88b.  888d888 ||
||     "Y88b. d88P"    888P"  d88""88b 888 "88b 888 "88b 888 d8P  Y8b 888P"   ||
||       "888 888      888    888  888 888  888 888  888 888 88888888 888     ||
|| Y88b  d88P Y88b.    888    Y88..88P 888 d88P 888 d88P 888 Y8b.     888     ||
||  "Y8888P"   "Y8888P 888     "Y88P"  88888P"  88888P"  888  "Y8888  888     ||
||                                                                            ||
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

0. Table of contents
--------------------
1 Introduction
2 Requirements
3 Install instructions
	3.1 Installing
	3.2 Uninstalling
4 Starting the program
5 Settings
	5.1 Setting the path to the tracklist file
	5.2 Setting how often the program will check for new tracks
	5.3 Adding users
	5.4 Checking what users are stored and removing users
	5.5 Stop popups from showing
	5.6 Skipping the splash-screen
	5.7 Autostarting VirtualDJ
	5.8 Setting the path to the VirtualDJ executable
	5.9 Autoexit when VirtualDJ is closed
6 Known limitations
7 FAQ
8 Licence
9 Additional information
	9.1 Contact information
	9.2 Donate

1. Introduction
---------------
VirtualDJScrobbler is a program that scrobbles the tracks you play in VirtualDJ.
It uses the fact that VirtualDJ creates a tracklist file from the tracks that 
you play. The program has the ability to scrobble for many users, for example
if you are multiple DJs or maybe even if you want to scrobble songs for your
crowd.

The program uses the last.fm submission API to submit the played tracks to up to
50 different last.fm accounts.

The program was created by Magnus Tingne, the original idea came when he was
having a small rave in his apartment and wanted the songs he played to be 
scrobbled. After seeing that there were some interest in a scrobbler for 
VirtualDJ he decided to fine tune the program and release it to the public.

Note: This program has no affiliation with Atomix or VirtualDJ it just uses the 
tracklist file stored by VirtualDJ while playing. Nor does it have any 
affiliation with last.fm or audioscrobbler, it just uses their API.

2. Requirements
---------------
VirtualDJScrobbler requires a Java Runtime Environment (JRE) of at version 6 or
later. You can get it at: http://java.sun.com.

For the songs to be scrobbled an Internet connection is needed. The program will
work without a connection and store the files but will be unable to upload these
until the connection is restored.

VirtualDJ is of course required for the program to be useful.

3. Install instructions
-----------------------
3.1 Installing
--------------
Unzip VirtualDJScrobbler.zip into a directory of your choice.

3.2 Uninstalling
----------------
Remove the dir where you extraxted the zip. 

The program will also have created a VirtualDJScrobbler in your %APPDATA% folder:
%APPDATA%\VirtualDJScrobbler
Just remove this and the program is completely gone.

4. Starting the program
-----------------------
Most computers with java installed will have .jar-files registered as runnable 
programs so double-clicking the .jar file should start the program.

If you have registered .jar files with some other program (for example WinRar or
7-Zip) you can run the program by starting a command prompt, navigating to the
folder where you have stored the VirtualDJScrobbler jar-file and run:
<full path to jre>/java -jar VirtualDJScrobbler.jar
for example (if you have stored the jar-file directly under C:): 
C:\> "C:\Program Files\Java\jre1.6.0_07\bin\java" -jar VirtualDJScrobbler.jar

5. Settings
-----------
5.1 Setting the path to the tracklist file
------------------------------------------
The program will assume that the tracklist file is in the place where VirtualDJ 
places it, for example in Windows XP with English as language: 
C:\Documents And Settings\<Username>\My Documents\VirtualDJ\Tracklisting\tracklist.txt
if the document isn't in the "correct" place or if it doesn't appear to be valid 
(see FAQ) you will be asked to select the location of the tracklist.txt-file.

If you later, for some reason, need to change the location of your tracklist file
you can right-click on the icon in the system tray and select 
"Set tracklist file" from the menu.

5.2 Setting how often the program will check for new tracks
-----------------------------------------------------------
The program reads the tracklist.txt file to determine what tracks you play, the 
default interval between these checks is 30seconds. If you want to change this
you can right-click on the icon in the system tray and select 
"Set file checking interval". Just move the slider to the desired amount of 
seconds between checks.

5.3 Adding users
----------------
For the program to be able to scrobble anything it needs at least one user, to 
set a user right-click the system tray icon and choose "Add user". Enter your
credentials in the fields and press enter. A validation will be made against 
last.fm and while this is going on you will see an indeterminate progress bar. 
Pressing cancel here will cancel the validation and the user won't be added. If
the user is properly validated or if some error occurs (for example if the 
username or password was wrong) you will be notified.

Users are saved between between program reboots so you won't need to re-add your
user every time.

Note: An Internet connection is needed when adding users, otherwise the program
won't be able to validate the user.

5.4 Checking what users are stored and removing users
-----------------------------------------------------
If you want to see what users are stored or if you want to remove one or more
users right-click the system tray icon and press "Remove users". You will get a
dialog where you see all users that are currently saved. Pressing the "Remove"-
button next to a user will remove the users and no more song will be scrobbled
for this user.

5.5 Stop popups from showing
----------------------------
If you are annoyed by the popups stating what song is beeing scrobbled just
right-click the system tray icon and deselect the "Show tray popups" item in the
menu.

If you later on change your mind just select it again.

5.6 Skipping the splash-screen
------------------------------
If you think the three seconds it takes for the wonderful splash-screen to 
disappear takes like forever just right-click the system tray icon and deselect the
"Show splash-screen" item in the menu.

If you later on change your mind just select it again.

Note: A splash-screen might popup for a very short while even if the option is 
deselected, this is due to the fact that the splash-screen cannot be hidden
before the java runtime machine has fully started the program.

5.7 VirtualDJ options
---------------------
5.7.1 Autostarting VirtualDJ
--------------------------
If you would like VirtualDJScrobbler to automatically start VirtualDJ when starting
up check this option.

If the path to the VirtualDJ executable hasn't been set (see section 5.7.2) you will
be requested to set the path when selecting autostart. If you cancel this popup
the autostart option will not be selected.

5.7.2 Setting the path to the VirtualDJ executable
------------------------------------------------
To be able to use the autostart VirtualDJ function you must set the path to the
VirtualDJ executable. To do this simply press the "Set VirtualDJ executable" option 
and either enter the full path to the "virtualdj.exe" file, or select browse and
locate the executable in the file explorer.

5.7.3 Autoexit when VirtualDJ is closed
-------------------------------------
If you want VirtualDJScrobbler to automatically exit once VirtualDJ is closed
you can enable it by clicking the "Exit when VDJ is closed" option.

This option depends on the "Autostart VirtualDJ" option and will only be possible
to set when autostart is activated. The reason for this is that the only way 
VirtualDJScrobbler can know about VirtualDJ is if it has been started from within
VirtualDJScrobbler, i.e. via the autostart option. 

6. Known limitations
--------------------
The program has only been tested on Windows XP and Windows 7 x64 with VirtualDJ 
v6.0, it might, for example, be that other versions of VirtualDJ stores the
tracklist file in another format.

The error handling of the program hasn't been thoroughly tested since most error 
responses from last.fm doesn't occur very often.

If you start the program before ever playing a track in VirtualDJ or have 
emptied the tracklist file the program won't accept the tracklist file as valid,
just play a song and wait until the song is listed in the tracklist.txt-file and 
try again.

Program hasn't been tested with more than 2 users since I only have 2 accounts.

7. FAQ
------
Q1. What does a valid tracklist file look like?
A1. VirtualDJ v6.0 stores the files on the format:

	VirtualDJ History - yyyy/mm/dd
	------------------------------
	
	hh:mm : <Artist> - <Title>
	
At least one song has to be have been played for the tracklist-file to be 
accepted as valid (see section 6).

Note: If VirtualDJ has stored a song without artist the track will be scrobbled
as Unknown artist - Title.


Q2. Why aren't my tracks that I played yesterday scrobbled?
A2. The program only accepts songs that are played after the program has started.

8. License
----------
Copyright 2009 Magnus Tingne (vdjscrobbler@gmail.com)

This file is part of VirtualDJScrobbler.

VirtualDJScrobbler is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option) any
later version.

VirtualDJScrobbler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.
 
You should have received a copy of the GNU General Public License along with
VirtualDJScrobbler. If not, see <http://www.gnu.org/licenses/>.

9. Additional information
-------------------------
9.1 Contact information
-----------------------
Magnus Tingne
vdjscrobbler@gmail.com

9.2 Donate
----------
If you like the program and think it is worth at least some money please feel
free to donate at the link below :D

https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=5NRMM5NEBFQPW&lc=SE&item_name=VirtualDJScrobbler&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted