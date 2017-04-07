Syncopoli - Rsync for Android
=============================

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/org.amoradi.syncopoli)

Options
-------
* Server address - The IP address of your server
* Protocol - Rsync or SSH
* Port - Self explanatory
* User - This should be your rsync user or ssh user (depending on protocol)
* Additional Options - Any additional options you want to give rsync
* Private key - Should be your dropbear-compatible ssh key (see below)
* Rsync Password - password used to authenticate with the Rsync daemon
* Frequency - How often you want to run all the sync profiles (in hours)

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

Q: Why is Syncopoli making connections to <someservername>?

A: Syncopoli needs `rsync` and `ssh` (dropbearssh) to operate. Since F-Droid does not allow bundling of binary files, I have hosted them on <someservername> and download those two when you first run the program. If you don't trust the binaries and have your phone rooted, just put `rsync` and `ssh` binaries that you have compiled yourself in `/data/data/org.amoradi.topoli` and Syncopoli will use those ones instead.

Credits
=======

Translators
-----------
* Spanish - [Andrés Hernández](https://gitlab.com/u/auroszx)
* Japanese - [naofum](https://gitlab.com/u/naofumi)
* Russian - [ashed](https://gitlab.com/u/ashed)
* German - [Christian](https://gitlab.com/u/epinez)
