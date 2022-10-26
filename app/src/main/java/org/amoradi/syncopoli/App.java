package org.amoradi.syncopoli;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

public class App extends Application {
    public static final String SYNC_CHANNEL_ID = "Sync notification channel";
    public static final String ERROR_CHANNEL_ID = "Sync error channel";
    public static final String SUCCESS_CHANNEL_ID = "Sync success channel";
    public static final int SYNC_NOTIF_ID = 1;
    public static final int ERROR_NOTIF_ID = 2;
    public static final int SUCCESS_NOTIF_ID = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        BackupActivity.setupSyncing(this);

        NotificationManager notifyMan = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if  (Build.VERSION.SDK_INT >= 26) {
            // >= orio, notification channels are required
            NotificationChannel progressCh = new NotificationChannel(
                    SYNC_CHANNEL_ID,
                    "Sync in progress",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationChannel errorCh = new NotificationChannel(
                    ERROR_CHANNEL_ID,
                    "Sync error",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationChannel successCh = new NotificationChannel(
                    SUCCESS_CHANNEL_ID,
                    "Sync success",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            notifyMan.createNotificationChannel(progressCh);
            notifyMan.createNotificationChannel(successCh);
            notifyMan.createNotificationChannel(errorCh);
        }

        myReceiver = new BackupReceiver();
        this.registerReceiver(myReceiver, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
        this.registerReceiver(myReceiver, new IntentFilter("android.net.wifi.STATE_CHANGE"));
        this.registerReceiver(myReceiver, new IntentFilter("android.intent.action.ACTION_POWER_CONNECTED"));
        this.registerReceiver(myReceiver, new IntentFilter("org.amoradi.syncopoli.SYNC_PROFILE"));
        this.registerReceiver(myReceiver, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
    }

    private BackupReceiver myReceiver;

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(myReceiver);
    }
}
