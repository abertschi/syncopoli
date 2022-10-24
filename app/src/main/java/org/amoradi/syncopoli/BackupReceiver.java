package org.amoradi.syncopoli;


import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class BackupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED") ||
            intent.getAction().equals("android.net.wifi.STATE_CHANGE") ||
                intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED"))  {
            BackupHandler h = new BackupHandler(ctx);
            if (h.getRunOnWifi() && h.canRunBackup()) {
                h.setRunOnWifi(false);
                BackupWorker.syncNow(ctx, h.getBackups());
            }
        }

        if (intent.getAction().equals("org.amoradi.syncopoli.SYNC_PROFILE")) {
            BackupHandler bh = new BackupHandler(ctx);
            BackupItem b = bh.findBackup(intent.getStringExtra("profile_name"));

            if (b == null) {
                return;
            }
            BackupWorker.syncNow(ctx, b);
        }
    }
}
