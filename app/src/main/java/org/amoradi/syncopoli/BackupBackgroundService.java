package org.amoradi.syncopoli;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;

public class BackupBackgroundService extends JobIntentService {
    public final static String TAG = "Syncopoli";
    private static final int JOB_ID = 1234;

	@Override
	public void onCreate() {
		super.onCreate();
		Notification notif = getNotification(App.SYNC_CHANNEL_ID)
				.setTicker("Syncopoli")
				.setContentTitle("Syncopoli")
				.setContentText("Sync in progress...")
				.build();

		startForeground(App.SYNC_NOTIF_ID, notif);
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
				.setContentIntent(PendingIntent.getActivity(this,
						0,
						new Intent(this, BackupActivity.class),
						PendingIntent.FLAG_UPDATE_CURRENT))
				.setSmallIcon(notif_icon);
	}

	static void enqueueWork(Context ctx, Intent work) {
    	enqueueWork(ctx, BackupBackgroundService.class, JOB_ID, work);
	}

    @Override
    protected void onHandleWork(@NonNull Intent work) {
        try {
			Notification notif = getNotification(App.SYNC_CHANNEL_ID)
					.setTicker("Syncopoli")
					.setContentTitle("Syncopoli")
					.setContentText("Sync in progress...")
					.build();

			startForeground(App.SYNC_NOTIF_ID, notif);
            executeWork(work);
        } finally {
			stopForeground(true);
        }
    }

	private void executeWork(Intent work) {
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
        if (ps != null) {
			for (Parcelable x : ps) {
				BackupItem y = (BackupItem) x;
				runTask(h, y);
			}
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
