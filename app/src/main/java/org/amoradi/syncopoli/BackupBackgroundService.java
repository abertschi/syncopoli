package org.amoradi.syncopoli;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import android.app.IntentService;
import androidx.core.app.NotificationCompat;

import android.os.PowerManager;
import android.util.Log;

public class BackupBackgroundService extends IntentService {
    public final static String TAG = "Syncopoli";
    private PowerManager.WakeLock wakeLock;

    public BackupBackgroundService() {
        super("BackupBackgroundService");
        setIntentRedelivery(true);
    }

    private NotificationCompat.Builder getNotification(String id) {
		int notif_icon = R.drawable.ic_action_refresh_bitmap;

		if (Build.VERSION.SDK_INT >= 21) {
			// >= lollipop, notification supports vector icons
			notif_icon = R.drawable.ic_action_refresh;
		}

		return new NotificationCompat.Builder(getApplicationContext(), id)
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(true)
				.setSmallIcon(notif_icon);
	}

    @Override
	public void onCreate() {
		Log.e(TAG, "onCreate!!!!!!!!");
    	super.onCreate();

		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Syncopoli: Sync wakelock");
		wakeLock.acquire(20 * 60 * 1000); // timeout in millis

		Notification notif = getNotification(App.SYNC_CHANNEL_ID)
				.setTicker("Syncopoli")
				.setContentTitle("Syncopoli")
				.setContentText("Sync in progress...")
				.build();

		startForeground(App.SYNC_NOTIF_ID, notif);
	}

    @Override
	public void onDestroy() {
    	Log.e(TAG, "onDestroy!!!!!!!!!");
		wakeLock.release();
		stopForeground(true);
		super.onDestroy();
    }

	@Override
	protected void onHandleIntent(Intent work) {
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

		// handle errors with new notification if necessary
		if (ret != 0 && ret != BackupHandler.ERROR_DONOTRUN) {
			int notif_icon = R.drawable.ic_action_refresh_bitmap;

			if (Build.VERSION.SDK_INT >= 21) {
				// >= lollipop, notification supports vector icons
				notif_icon = R.drawable.ic_action_refresh;
			}

			Notification error_notif = getNotification(App.ERROR_CHANNEL_ID)
					.setTicker("Syncopoli")
					.setContentTitle("Sync failed")
					.setContentText(b.name)
					.build();

			NotificationManager notifyMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notifyMan.notify(b.name, App.ERROR_NOTIF_ID, error_notif);
		}
    }
}
