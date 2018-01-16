package org.amoradi.syncopoli;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SSHManager {
    private Context mContext;

    private String mUsername;
    private String mHost;
    private int mPort;
    private String mLocalFingerprint;

    SSHManager(Context ctx) {
        mContext = ctx;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mUsername = prefs.getString(SettingsFragment.KEY_RSYNC_USERNAME, "");
        mHost = prefs.getString(SettingsFragment.KEY_SERVER_ADDRESS, "");
        mPort = Integer.parseInt(prefs.getString(SettingsFragment.KEY_PORT, "22"));
        mLocalFingerprint = prefs.getString(SettingsFragment.KEY_HOST_KEY_FINGERPRINT, "");
    }

    public String getRemoteHostFingerPrint() {
        JSch jsch = new JSch();
        Session session = null;
        String fp = "";

        try {
            session = jsch.getSession(mUsername, mHost, mPort);
        } catch (JSchException e) {
            Log.e("syncopoli.HostFingerPri", e.toString());
            return "";
        }

        Properties props = new Properties();
        props.put("StrictHostKeyChecking", "yes");
        session.setConfig(props);

        try {
            session.connect();
            // always expecting an exception with host key rejection as host key repo is empty for jsch
        } catch (JSchException e) {
            fp = session.getHostKey().getFingerPrint(jsch);
            Log.e("syncopoli.HostFingerPri", "FINGERPRINT: " + fp);
        }

        session.disconnect();
        return fp;
    }

    public HostKey getRemoteHostKey() {
        JSch jsch = new JSch();
        Session session = null;
        HostKey key = null;

        try {
            session = jsch.getSession(mUsername, mHost, mPort);
        } catch (JSchException e) {
            Log.e("syncopoli.HostFingerPri", e.toString());
            return "";
        }

        Properties props = new Properties();
        props.put("StrictHostKeyChecking", "yes");
        session.setConfig(props);

        try {
            session.connect();
            // always expecting an exception with host key rejection as host key repo is empty for jsch
        } catch (JSchException e) {
            key = session.getHostKey();
        }

        session.disconnect();
        return key;
    }

    public List<String> getLocalHostKeys() {
        File known_hosts = new File(mContext.getFilesDir() + "/.ssh/known_hosts");
        List<String> res = new ArrayList<String>;

        try {
            BufferedReader br = new BufferedReader(new FileReader(known_hosts));
            String line;

            while ((line = br.readLine()) != null) {
                res.add(line);
            }
        } catch (IOException e) {
            Log.e("syncopoli.SSHManager", e.toString());
            return null;
        }

        return res;
    }

    public int saveHostKey(HostKey k) {
        File known_hosts = new File(mContext.getFilesDir() + "/.ssh/known_hosts");

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(known_hosts, true));
            bw.write(/*TODO*/)
        } catch (IOException e) {
            Log.e("syncopoli.SSHManager", e.toString());
            return -1;
        }
    }

    public String getLocalHostFingerPrint() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getString(SettingsFragment.KEY_HOST_KEY_FINGERPRINT, "");
    }

    public int saveRemoteHostFingerPrint() {
        String fp = getRemoteHostFingerPrint();
        return saveRemoteHostFingerPrint(fp);
    }

    public int saveRemoteHostFingerPrint(String fp) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SettingsFragment.KEY_HOST_KEY_FINGERPRINT, fp);
        editor.apply();
        return 0;
    }

    public boolean checkHostKeyFingerPrint() {
        return getRemoteHostFingerPrint().equals(getLocalHostFingerPrint());
    }
}
