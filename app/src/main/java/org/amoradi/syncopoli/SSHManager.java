package org.amoradi.syncopoli;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

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
