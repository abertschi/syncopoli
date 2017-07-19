Syncopoli - Rsync for Android
=============================

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/org.amoradi.syncopoli)

Global Options
--------------
* Server address - The IP address of your server
* Protocol - Rsync or SSH
* Port - Port where rsync or ssh daemon is listening
* User - This should be your rsync user or ssh user (depending on protocol)
* Additional Options - Any additional options you want to give rsync
* Private key - Should be your dropbear-compatible ssh key (see below)
* Rsync Password - password used to authenticate with the Rsync daemon
* Frequency - How often you want to run all the sync profiles (in hours)

Profile Options
---------------
* Direction - `local` means phone, `remote` means whatever server is on the internet side
* Profile name - This is for your own recognition, set it to whatever you like
* Origin - The source directory/file
* Destination - The destination directory
* Additional Options - Set additional rsync options if this profile needs additional options

SSH Key
-------
Syncopoli requires a dropbear-compatible ssh key. You can use `dropbearconvert` to convert your openssh key to dropbear key.

External binaries
-----------------
`rsync` and `ssh` (dropbear ssh) binaries are included here and are themselves open source projects. The sources for these binaries have not been modified.

Interoperability
----------------
As of a01af010, syncopoli can receive broadcast intents from other applications and runs the specified sync service. The application needs to know the name of the sync profile to run. Approximately, the code would look like:

```java
Intent intent = new Intent();
intent.setAction("org.amoradi.syncopoli.SYNC_PROFILE");
intent.putExtra("profile_name", "my profile name");
sendBroadcast(intent);
```

The following intents are supported (may expand in the future) (merge requests welcome):

```
org.amoradi.syncopoli.SYNC_PROFILE
```

I expect the user of your application will set the profile name as has been setup in syncopoli.

FAQ
---

Q: Why is Syncopoli making connections to `<someservername>`?

A: Syncopoli needs `rsync` and `ssh` (dropbearssh) to operate. Since F-Droid does not allow bundling of binary files, I have hosted them on `<someservername>` and download those two when you first run the program. If you don't trust the binaries and have your phone rooted, just put `rsync` and `ssh` binaries that you have compiled yourself in `/data/data/org.amoradi.syncopoli/files` and Syncopoli will use those ones instead (make sure to set them as executables).

Q: How do I use this...thing?

A: Set your global options by going to `settings` (see `Global Options` above), then press the plus button to create individual profiles. The play button on top bar runs all sync tasks. Hold your finger over each profile to edit/delete them.

Q: Typing paths is tedious.

A: I know. Just long-press on Origin path. Make sure you have OI File Manager installed.

Credits
=======

Translators
-----------
* Spanish - [Andrés Hernández](https://gitlab.com/u/auroszx)
* Japanese - [naofum](https://gitlab.com/u/naofumi)
* Russian - [ashed](https://gitlab.com/u/ashed)
* German - [Christian](https://gitlab.com/u/epinez)
* Italian - [Claudio Arseni](https://gitlab.com/Claudinux)
* Dutch - [Nathan van Beelen](https://gitlab.com/nvbln)
