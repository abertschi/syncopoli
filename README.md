Syncopoli - Rsync for Android
=============================

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/org.amoradi.syncopoli)

Global Options
--------------
* Server address - The IP address of your server
* Protocol - Rsync or SSH
* Port - Port where rsync or ssh daemon is listening
* User - This should be your rsync user or ssh user (depending on protocol)
* Additional Options - Any additional options you want to give rsync. This is applied to all sync profiles.
* Private key - Should be your dropbear-compatible ssh key (see below)
* Rsync Password - password used to authenticate with the Rsync daemon
* SSH Password - password used to authenticate using ssh protocol
* Frequency - How often you want to run all the sync profiles (in hours)
* Wifi only - whether to sync over wifi only
* SSIDs to sync - sync only when connected to the specified SSIDs, e.g. mynetwork;yournetwork;somenetwork

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
`rsync` and `ssh` (dropbear ssh) binaries are built from source on f-droid servers. Cloning this project will include ALL sources used (and their modifications).

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

Building
--------
See `app/build.gradle` for dependencies (requires android `sdk` and `ndk`, make sure to properly set up both).

```
$ git clone --recursive # clones sources for rsync and ssh
$ gradle assembleRelease
```

You have to have `gradle` installed. See [gradle.org](https://gradle.org) or your local fresh repo.

Starting with v0.4.5.4, you need `gradle 4.4.1`, `build-tools 27.0.3`, and `ndk-tools r15c`.

How to get the latest debug build
---------------------------------
`gitlab.com/fengshaun/syncopoli` -> CI/CD -> Pipelines -> on the right side, download artifacts. There is a file name apk-debug.apk.

How to install apk
------------------
`adb install <apk>`

How to get logcat output for bug report
---------------------------------------
Run syncopoli on your phone, then run the following commands:
```
# to get the pid of syncopoli
$ adb shell "pidof org.amoradi.syncopoli"
# to get the logcat for syncopoli
$ adb logcat --pid=<the number you got from above command>
```

Paste the output in the bug report

FAQ
---

Q: Syncopoli fails! I get: "ssh: Exited: String too long"!

A: Syncopoli needs an ssh key in dropbear format. See "SSH Key" section above.

Q: How do I use this...thing?

A: Set your global options by going to `settings` (see `Global Options` above), then press the plus button to create individual profiles. The play button on top bar runs all sync tasks. Hold your finger over each profile to edit/delete them.

Q: Typing paths is tedious.

A: I know. It's in the works.

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
