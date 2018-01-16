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
import com.jcraft.jsch.UserInfo;

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
    private JSch mJsch;
    private HostKey mRemoteHostKey;

    SSHManager(Context ctx) {
        mContext = ctx;
        mJsch = new JSch();
        mRemoteHostKey = null;

        try {
            mJsch.setKnownHosts(mContext.getFilesDir().getAbsolutePath() + "/.ssh/known_hosts");
        } catch (JSchException e) {
            Log.e("Syncopoli.SSHManager", "Could not load " + mContext.getFilesDir().getAbsolutePath() + "/.ssh/known_hosts: " + e.toString());
        }
    }

    public HostKey getRemoteHostKey(String username, String password, String host, int port) {
        Session session = null;
        String fp = "";

        try {
            session = mJsch.getSession(username, host, port);
        } catch (JSchException e) {
            Log.e("syncopoli.HostFingerPri", e.toString());
            return null;
        }

        Properties props = new Properties();
        props.put("StrictHostKeyChecking", "yes");
        session.setConfig(props);

        if (!password.equals("")) {
            session.setPassword(password);
        }

        try {
            session.connect();
        } catch (JSchException e) {
            mRemoteHostKey = session.getHostKey();
            Log.e("syncopoli.HostFingerPri", "FINGERPRINT: " + mRemoteHostKey.getFingerPrint(mJsch));
        }

        session.disconnect();
        return mRemoteHostKey;
    }

    public void acceptHostKey(HostKey hk) {
        mJsch.getHostKeyRepository().add(hk, null);
    }

    public boolean matchKey(String host, HostKey hk) {
        HostKey[] hks = mJsch.getHostKeyRepository().getHostKey(host, null);
        if (hks.length <= 0) {
            return false;
        }

        return hks[0].getKey().equals(hk.getKey());
    }

}
