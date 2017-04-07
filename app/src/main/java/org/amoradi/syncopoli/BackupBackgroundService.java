package org.amoradi.syncopoli;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;

public class BackupBackgroundService extends IntentService {
	private int NOTIFICATION_ID = 2;

    public BackupBackgroundService() {
        super("BackupBackgroundService");
        setIntentRedelivery(true);
    }

	@Override
	protected void onHandleIntent(Intent work) {
		Bundle bundle = work.getExtras();
		BackupItem b = bundle.getParcelable("item");

        if (b != null) {
            runTask(b);
            return;
        }

        BackupItem[] bs = (BackupItem[]) bundle.getParcelableArray("items");
        for (BackupItem x : bs) {
            runTask(x);
        }

	}

    private void runTask(BackupItem b) {
        Notification notif = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Syncopoli")
                .setContentText("Syncing " + b.name)
                .setTicker("Notification!")
                .setWhen(System.currentTimeMillis())
                .setSound(null)
                .setVibrate(null)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_action_refresh)
                .build();

        NotificationManager notifyMan = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notifyMan.notify(NOTIFICATION_ID, notif);

        BackupHandler h = new BackupHandler(getApplicationContext());
        h.runBackup(b);

        notifyMan.cancel(NOTIFICATION_ID);
    }
}
