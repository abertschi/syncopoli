package org.amoradi.syncopoli;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.NotificationCompat;

public class BackupBackgroundService extends IntentService {
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

        Parcelable[] ps = bundle.getParcelableArray("items");
        for (Parcelable x : ps) {
            BackupItem y = (BackupItem) x;
            runTask(y);
        }

	}

    private void runTask(BackupItem b) {

		BackupHandler h = new BackupHandler(getApplicationContext());
		int ret = h.runBackup(b);

		int notif_icon = R.drawable.ic_action_refresh;

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			// < lollipop, notification doesn't support vector icons
			notif_icon = R.drawable.ic_action_refresh_bitmap;
		}

		if (ret != 0 && ret != BackupHandler.ERROR_DONOTRUN) {
			Notification notif = new NotificationCompat.Builder(getApplicationContext())
				.setContentTitle("Syncopoli")
				.setContentText("Syncing " + b.name + " failed.")
				.setTicker("Notification!")
				.setWhen(System.currentTimeMillis())
				.setSound(null)
				.setVibrate(null)
				.setAutoCancel(true)
				.setSmallIcon(notif_icon)
				.build();

			NotificationManager notifyMan = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

			// int m = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
			notifyMan.notify(b.name, 1, notif);
		} else {
			NotificationManager notifyMan = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			notifyMan.cancel(b.name, 1);
		}
    }
}
