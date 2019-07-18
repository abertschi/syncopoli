package org.amoradi.syncopoli;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class BackupHandler implements IBackupHandler {
    private static final String TAG = "Syncopoli";

    private List<BackupItem> mBackupItems;
    Context mContext;

    public static final int ERROR_DONOTRUN = -2;
    public static final int ERROR_BACKUP_EXISTS = -3;
    public static final int ERROR_BACKUP_MISSING = -4;
    public static final int ERROR_TOO_MANY_RESULTS = -5;
	public static final int ERROR_RSYNC_MISSING = -6;
	public static final int ERROR_SSH_MISSING = -7;

    public BackupHandler(Context ctx) {
        mContext = ctx;
        updateBackupList();
    }

    public int addBackup(BackupItem item) {
        Log.d(TAG, "Adding backup: " + item);
        
        if (item.sources[0].equals("") || item.name.equals("")) {
            return -1;
        }

        if (findBackup(item.name) != null) {
            return ERROR_BACKUP_EXISTS;
        }

        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BackupSyncSchema.COLUMN_TYPE, "backup");
        values.put(BackupSyncSchema.COLUMN_NAME, item.name);
        values.put(BackupSyncSchema.COLUMN_SOURCES, item.getSourcesAsString());
        values.put(BackupSyncSchema.COLUMN_DESTINATION, item.destination);
        values.put(BackupSyncSchema.COLUMN_RSYNC_OPTIONS, item.rsync_options);
        values.put(BackupSyncSchema.COLUMN_LAST_UPDATE, "");

        if (item.direction == BackupItem.Direction.INCOMING) {
            values.put(BackupSyncSchema.COLUMN_DIRECTION, "INCOMING");
        } else {
            values.put(BackupSyncSchema.COLUMN_DIRECTION, "OUTGOING");
        }

        db.insert(BackupSyncSchema.TABLE_NAME, null, values);
        db.close();
        dbHelper.close();

        updateBackupList();
        Log.d(TAG, "Adding backup succeeded");
        return 0;
    }

    public int copyBackup(BackupItem item) {
        BackupItem item_copy = new BackupItem(item);

        int n = 1;
        String name;
        do {
            name = item.name + " - copy " + Integer.toString(n++);
        } while (findBackup(name) != null);

        item_copy.name = name;
        return addBackup(item_copy);
    }

    public int removeBackup(BackupItem item) {
        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query(
                BackupSyncSchema.TABLE_NAME,
                null,
                "name = '" + item.name + "'",
                null,
                null,
                null,
                BackupSyncSchema.COLUMN_NAME + " DESC",
                null
        );

        if (c.getCount() <= 0) {
            return BackupHandler.ERROR_BACKUP_MISSING;
        }

        if (c.getCount() > 1) {
            return BackupHandler.ERROR_TOO_MANY_RESULTS;
        }

        db.delete(BackupSyncSchema.TABLE_NAME, "name = '" + item.name + "'", null);
        return 0;
    }

    public List<BackupItem> getBackups() {
        return mBackupItems;
    }

    public BackupItem findBackup(String name) {
        for (BackupItem b : mBackupItems) {
            if (b.name.equals(name)) {
                return b;
            }
        }

        return null;
    }

    public void updateBackupList() {
        List<BackupItem> bl = new ArrayList<>();

        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.query(
                BackupSyncSchema.TABLE_NAME,
                null, //proj
                "type = 'backup'",
                null,
                null,
                null,
                BackupSyncSchema.COLUMN_NAME + " DESC",
                null
        );

        if (c.getCount() <= 0) {
            c.close();
            db.close();
            dbHelper.close();
            mBackupItems = bl;
            return;
        }

        c.moveToFirst();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

        do {
            BackupItem x = new BackupItem();
            x.name = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_NAME));
            x.sources = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_SOURCES)).split("\n");
            x.destination = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_DESTINATION));
            x.rsync_options = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_RSYNC_OPTIONS));

            String dir = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_DIRECTION));
            if (dir.equals("INCOMING")) {
                x.direction = BackupItem.Direction.INCOMING;
            } else {
                x.direction = BackupItem.Direction.OUTGOING;
            }

            try {
                x.lastUpdate = df.parse(c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_LAST_UPDATE)));
            } catch (ParseException e) {
                x.lastUpdate = null;
            }

            bl.add(x);
        } while(c.moveToNext());

        c.close();
        db.close();
        dbHelper.close();

        mBackupItems = bl;
    }

    public void updateBackupTimestamp(BackupItem b) {
        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BackupSyncSchema.COLUMN_TYPE, "backup");
        values.put(BackupSyncSchema.COLUMN_NAME, b.name);
        values.put(BackupSyncSchema.COLUMN_SOURCES, b.getSourcesAsString());
        values.put(BackupSyncSchema.COLUMN_DESTINATION, b.destination);
        values.put(BackupSyncSchema.COLUMN_RSYNC_OPTIONS, b.rsync_options);

        b.lastUpdate = new Date();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        values.put(BackupSyncSchema.COLUMN_LAST_UPDATE, df.format(b.lastUpdate));

        db.update(BackupSyncSchema.TABLE_NAME, values, "name='" + b.name + "'", null);
        db.close();
        dbHelper.close();
    }

    public int updateBackup(String old_name, BackupItem b) {
        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BackupSyncSchema.COLUMN_TYPE, "backup");
        values.put(BackupSyncSchema.COLUMN_NAME, b.name);
        values.put(BackupSyncSchema.COLUMN_SOURCES, b.getSourcesAsString());
        values.put(BackupSyncSchema.COLUMN_DESTINATION, b.destination);
        values.put(BackupSyncSchema.COLUMN_LAST_UPDATE, "");
        values.put(BackupSyncSchema.COLUMN_RSYNC_OPTIONS, b.rsync_options);

        if (b.direction == BackupItem.Direction.INCOMING) {
            values.put(BackupSyncSchema.COLUMN_DIRECTION, "INCOMING");
        } else {
            values.put(BackupSyncSchema.COLUMN_DIRECTION, "OUTGOING");
        }

        db.update(BackupSyncSchema.TABLE_NAME, values, "name='" + old_name + "'", null);
        db.close();
        dbHelper.close();

        return 0;
    }

    public int runBackup(BackupItem b) {
        try {
            String rsyncPath = new File(mContext.getFilesDir(), "rsync").getAbsolutePath();
            Log.d(TAG, "rsyncPath: " + rsyncPath);
            String sshPath = new File(mContext.getFilesDir(), "ssh").getAbsolutePath();
            Log.d(TAG, "sshPath: " + sshPath);

            FileOutputStream logFile = mContext.openFileOutput(b.getLogFileName(), Context.MODE_PRIVATE);

            updateBackupTimestamp(b);
            logFile.write((b.lastUpdate.toString() + " \n\n").getBytes());

            File f = new File(rsyncPath);

            if (!f.exists()) {
                logFile.write(("ERROR: Missing rsync binary. Please submit a bug report and include logcat output along with the following info:\n\n").getBytes());

                String[] abis = {Build.CPU_ABI, Build.CPU_ABI2};
                if (Build.VERSION.SDK_INT >= 21) {
                    abis = Build.SUPPORTED_ABIS;
                }

                logFile.write(("SUPPORTED ABIS: " + Arrays.toString(abis) + "\n").getBytes());

                return ERROR_RSYNC_MISSING;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            String rsync_username = prefs.getString(SettingsFragment.KEY_RSYNC_USERNAME, "");

            if (rsync_username.equals("")) {
                logFile.write("ERROR: Username not specified. Please set username in settings.".getBytes());
                return -1;
            }

            String rsync_options = prefs.getString(SettingsFragment.KEY_RSYNC_OPTIONS, "");
            String rsync_password = prefs.getString(SettingsFragment.KEY_RSYNC_PASSWORD, "");
            String ssh_password = prefs.getString(SettingsFragment.KEY_SSH_PASSWORD, "");
            boolean use_ssh_password = false;
            boolean as_root = prefs.getBoolean(SettingsFragment.KEY_AS_ROOT, false);

            String server_address = prefs.getString(SettingsFragment.KEY_SERVER_ADDRESS, "");

            if (server_address.equals("")) {
                logFile.write("ERROR: Server address not specified. Please set Server address in settings.".getBytes());
                return -1;
            }

            String protocol = prefs.getString(SettingsFragment.KEY_PROTOCOL, "SSH");
            String private_key = prefs.getString(SettingsFragment.KEY_PRIVATE_KEY, "");
            String port = prefs.getString(SettingsFragment.KEY_PORT, "22");

            if (port.equals("")) {
                logFile.write("ERROR: Port not specified. Please set Port in settings.".getBytes());
                return -1;
            }

            if (protocol.equals("SSH")) {
                if (private_key.equals("")) {
                    use_ssh_password = true;
                } else {
                    File pkey_file = new File(private_key);
                    if (!pkey_file.canRead()) {
                        logFile.write(("ERROR: Cannot read specified private key file: '" + private_key + "'").getBytes());
                        return -1;
                    }
                    use_ssh_password = false;
                }

                if (ssh_password.equals("") && use_ssh_password) {
                    logFile.write(("ERROR: attempting to use password, but no password specified for SSH").getBytes());
                    return -1;
                }
            }

            /*
             * BUILD ARGUMENTS
             */

            ArrayList<String> args = new ArrayList<>();

            args.add(f.getAbsolutePath());

            if (!rsync_options.equals("")) {
				args.addAll(ArgumentTokenizer.tokenize(rsync_options));
            }

            if (!b.rsync_options.equals("")) {
				args.addAll(ArgumentTokenizer.tokenize(b.rsync_options));
            }

            if (protocol.equals("SSH")) {
                args.add("-e");
                String ssh_cmd = sshPath + " -p " + port;

                if (!use_ssh_password) {
                    ssh_cmd += " -i " + private_key;
                }

                if (as_root) {
                    args.add("'" + ssh_cmd + "'");
                } else {
                    args.add(ssh_cmd);
                }

                if (b.direction == BackupItem.Direction.OUTGOING) {
                    args.addAll(Arrays.asList(b.sources));
                    args.add(rsync_username + "@" + server_address + ":" + b.destination);
                } else {
                    for (String s : b.sources) {
                        args.add(rsync_username + "@" + server_address + ":" + s);
                    }
                    args.add(b.destination);
                }

            } else if (protocol.equals("Rsync")) {
				args.add("--port=" + port);
				
                if (b.direction == BackupItem.Direction.OUTGOING) {
                    args.addAll(Arrays.asList(b.sources));
                    args.add(rsync_username + "@" + server_address + "::" + b.destination);
                } else {
                    for (String s : b.sources) {
                        args.add(rsync_username + "@" + server_address + "::" + s);
                    }
                    args.add(b.destination);
                }
            }

            Log.d(TAG, "rsync exec: " + args.toString());

            /*
             * AS ROOT
             */
            ArrayList<String> final_cmd = new ArrayList<String>();

            if (as_root) {
                StringBuilder sb = new StringBuilder();
                for (String s : args) {
                    sb.append(s);
                    sb.append(" ");
                }

                final_cmd.add("su");
                final_cmd.add("--preserve-environment");
                final_cmd.add("--command");
                final_cmd.add(sb.toString());

                Log.d(TAG, "with su: " + final_cmd.toString());
            } else {
                final_cmd = args;
            }

            /*
             * BUILD PROCESS
             */

            ProcessBuilder pb = new ProcessBuilder(final_cmd);
            pb.directory(mContext.getFilesDir());
            pb.redirectErrorStream(true);

            // Set environment (make sure we have reasonable $HOME, so ssh can store keys)
            Map<String, String> env = pb.environment();
            env.put("HOME", mContext.getFilesDir().getAbsolutePath());

            if (protocol.equals("Rsync") && !rsync_password.equals("")) {
                env.put("RSYNC_PASSWORD", rsync_password);
            }

            if (protocol.equals("SSH") && use_ssh_password) {
                env.put("DROPBEAR_PASSWORD", ssh_password);
            }

            /*
             * RUN PROCESS
             */

            Process process = pb.start();

            /*
             * GET STDOUT/STDERR
             */

            String temp = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            /* Read STDOUT & STDERR */
            while ((temp = reader.readLine()) != null) {
                Log.v(TAG, temp + "\n");
                logFile.write((temp + "\n").getBytes());
            }
            reader.close();

            // Wait for the command to finish.
            process.waitFor();

            // Show message how it ended.
            int errno = process.exitValue();
            if (errno != 0) {
                logFile.write(("\nSync FAILED (error code " + errno + ").\n").getBytes());
            } else {
                logFile.write("\nSync complete.\n".getBytes());
            }

            logFile.close();

            return errno;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean canRunBackup() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean wifi_only = prefs.getBoolean(SettingsFragment.KEY_WIFI_ONLY, false);
        String wifi_name = prefs.getString(SettingsFragment.KEY_WIFI_NAME, "");

        if (!wifi_only) {
            return true;
        }

        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Log.d(TAG, "Wifi not enabled");
            return false;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            Log.e(TAG, "Cannot get Wifi info from WifiManager");
            return false;
        }

        SupplicantState state = wifiInfo.getSupplicantState();
        if (state != SupplicantState.COMPLETED) {
            Log.d(TAG, "SupplicantState.COMPLETED is false");
            return false;
        }

        if (wifi_name.equals("")) {
	       return true;
        }

        String ssid = wifiInfo.getSSID();
        if (ssid.startsWith("\"")) {
            ssid = ssid.replaceAll("^\"|\"$", "");
        }

        Log.d(TAG, "ssid: " + ssid);
        for (String name : wifi_name.split(";")) {
            if (ssid.equals(name)) {
                Log.d(TAG, ssid + " matches " + name);
                return true;
            }
        }

        return false;
    }

    public void setRunOnWifi(boolean run) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putBoolean("RunOnWifi", true);
        editor.apply();
    }

    public boolean getRunOnWifi() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getBoolean("RunOnWifi", false);
    }

    public void syncBackups() {}
    public void showLog(BackupItem b) {}
    public int editBackup(BackupItem b) {return 0;}
}
