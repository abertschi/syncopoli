package org.amoradi.syncopoli;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SSHManager {
    private static final String TAG = "Syncopoli";

    private Context mContext;

    /* This needs the patched version of dropbear!
     * see gitlab.com/fengshaun/android-dropbear commit f49af1902d3d683c59a7445746fa3a35cd07ef33
     * Format: Fingerprint: md5 ab:cd:ef:...
     */
    private Pattern mFingerprintPattern = Pattern.compile("^Fingerprint: [\\w\\d]+ ([\\w:]+)$");
    private Pattern mAcceptedPattern = Pattern.compile("^Accepted fingerprint$");

    private String host;
    private String port;

    SSHManager(Context ctx) throws NumberFormatException {
        mContext = ctx;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        host = sp.getString(SettingsFragment.KEY_SERVER_ADDRESS, "");
        port = sp.getString(SettingsFragment.KEY_PORT, "22");

        try {
            Integer.parseInt(port);
        } catch (java.lang.NumberFormatException e) {
            Log.e(TAG, "Could not convert port to integer: " + e.toString());
            throw e;
        }
    }

    public String getRemoteHostFingerprint() {
        List<String> args = new ArrayList<>();

        File f = new File(mContext.getFilesDir(), "ssh");
        args.add(f.getAbsolutePath());

        args.add("-p");
        args.add(port);
        /* this option is added in patched version of dropbear
         * -C make dropbear print remote host fingerprint and exit, which is what we want
         */
        args.add("-C");
        args.add(host);

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(mContext.getFilesDir());
        pb.redirectErrorStream(true);

        // Set environment (make sure we have reasonable $HOME, so ssh can store keys)
        Map<String, String> env = pb.environment();
        env.put("HOME", mContext.getFilesDir().getAbsolutePath());

        /*
         * RUN PROCESS
         */

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            Log.e(TAG, "Could not run ssh: " + e.toString());
            return null;
        }

        /*
         * GET STDOUT/STDERR
         */

        String temp;
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        /* Read STDOUT & STDERR */
        try {
            while ((temp = reader.readLine()) != null) {
                Log.e(TAG, temp + "\n");

                Matcher m = mFingerprintPattern.matcher(temp);
                if (m.matches()) {
                    Log.e(TAG, "MATCHES FINGERPRINT: " + m.group(1));
                    String fp = m.group(1);

                    try {
                        process.waitFor();
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    }

                    return fp;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not read/write from ssh process");
            return null;
        }

        Log.e(TAG, "Unknown error occurred when trying to communicate with ssh process");
        return null;
    }

    public boolean acceptHostKeyFingerprint(String fingerprint) {
        List<String> args = new ArrayList<>();

        File f = new File(mContext.getFilesDir(), "ssh");
        args.add(f.getAbsolutePath());

        args.add("-p");
        args.add(port);
        /* this option is added in patched version of dropbear
         * -C make dropbear print remote host fingerprint and exit, which is what we want
         */
        args.add("-Z");
        args.add(fingerprint);
        args.add(host);

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(mContext.getFilesDir());
        pb.redirectErrorStream(true);

        // Set environment (make sure we have reasonable $HOME, so ssh can store keys)
        Map<String, String> env = pb.environment();
        env.put("HOME", mContext.getFilesDir().getAbsolutePath());

        /*
         * RUN PROCESS
         */

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            Log.e(TAG, "Could not run ssh: " + e.toString());
            return false;
        }

        /*
         * GET STDOUT/STDERR
         */

        String temp;
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        /* Read STDOUT & STDERR */
        try {
            while ((temp = reader.readLine()) != null) {
                Log.e(TAG, temp + "\n");

                Matcher m = mAcceptedPattern.matcher(temp);
                if (m.matches()) {
                    Log.e(TAG, "Fingerprint accepted");

                    try {
                        process.waitFor();
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    }

                    return true;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not read/write from ssh process");
            return false;
        }

        Log.e(TAG, "Unknown error occurred when trying to communicate with ssh process");
        return false;
    }

    public boolean clearAcceptedHostKeyFingerprints() {
        String filename = "known_hosts";
        File acceptedFingerprintsFile = new File(mContext.getFilesDir().getAbsolutePath() + "/.ssh/",
                                                 filename);

        if (!acceptedFingerprintsFile.delete()) {
            Log.e(TAG, "Failed to delete " + acceptedFingerprintsFile.getAbsolutePath());
            return false;
        }

        try {
            if (!acceptedFingerprintsFile.createNewFile()) {
                Log.e(TAG, "Failed to create new " + filename + " file: file already exists after being deleted: " + acceptedFingerprintsFile.getAbsolutePath());
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to create new " + filename + " file: " + e.toString());
            return false;
        }

        return true;
    }
}
