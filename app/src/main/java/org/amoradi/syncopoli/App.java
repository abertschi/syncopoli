package org.amoradi.syncopoli;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class App extends Application {
    public static final String SYNC_CHANNEL_ID = "Sync notification channel";
    public static final String ERROR_CHANNEL_ID = "Sync error channel";
    public static final int SYNC_NOTIF_ID = 1;
    public static final int ERROR_NOTIF_ID = 2;

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationManager notifyMan = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if  (Build.VERSION.SDK_INT >= 26) {
            // >= orio, notification channels are required
            NotificationChannel sync_notif_chan = new NotificationChannel(
                    SYNC_CHANNEL_ID,
                    "Sync in progress",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationChannel error_notif_chan = new NotificationChannel(
                    ERROR_CHANNEL_ID,
                    "Sync error",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            notifyMan.createNotificationChannel(sync_notif_chan);
            notifyMan.createNotificationChannel(error_notif_chan);
        }
    }
}
