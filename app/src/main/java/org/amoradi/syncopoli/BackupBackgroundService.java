package org.amoradi.syncopoli;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import android.util.Log;

public class BackupBackgroundService extends JobIntentService {
    public final static String TAG = "Syncopoli";
    public static final int JOB_ID = 1;

    public BackupBackgroundService() {
        super();
    }

    static void enqueueWork(Context ctx, Intent i) {
        JobIntentService.enqueueWork(ctx, BackupBackgroundService.class, JOB_ID, i);
    }

	@Override
	protected void onHandleWork(Intent work) {
		Bundle bundle = work.getExtras();
		Boolean force = bundle.getBoolean("force");

		BackupHandler h = new BackupHandler(getApplicationContext());

		if (!force) {
			if(!h.canRunBackup()) {
				Log.d(TAG, "Not allowed to run backup due to configuration restriction");
				return;
			}
		} else {
			Log.d(TAG, "Forced sync - ignoring configuration restrictions");
		}

		BackupItem b = bundle.getParcelable("item");

        if (b != null) {
            runTask(h, b);
            return;
        }

        Parcelable[] ps = bundle.getParcelableArray("items");
        for (Parcelable x : ps) {
            BackupItem y = (BackupItem) x;
            runTask(h, y);
        }
	}

    private void runTask(BackupHandler h, BackupItem b) {
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
