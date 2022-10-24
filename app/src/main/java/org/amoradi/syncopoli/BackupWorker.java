package org.amoradi.syncopoli;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Work manager to handle sync work
 */
public class BackupWorker extends Worker {
    private static final String TAG = "Syncopoli";
    private static final String KEY_ITEMS = "KEY_ITEMS";
    private static final String KEY_FORCE = "KEY_FORCE";
    private static final String WORKMANAGER_ID = "SYNC_ID";

    private NotificationManager notificationManager;

    public BackupWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters) {
        super(context, parameters);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
    }

    public static void syncNow(Context context, List<BackupItem> items) {
        List<String> strItems = new ArrayList<String>();
        for (BackupItem i : items) {
            strItems.add(i.toJson());
        }
        scheduleOneShot(context, strItems);
    }

    public static void syncNow(Context context, BackupItem item) {
        List<BackupItem> items = new ArrayList<>();
        items.add(item);
        syncNow(context, items);
    }

    private static void scheduleOneShot(Context context, List<String> jsonItems) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setInputData(new Data.Builder()
                        .putBoolean(KEY_FORCE, true)
                        .putStringArray(KEY_ITEMS, jsonItems.toArray(new String[0]))
                        .build())
                .build();
        WorkManager.getInstance(context).enqueue(request);
    }

    /*
     * XXX: ExistingPeriodicWorkPolicy.KEEP ensures that we dont schedule the request twice.
     * This allows us to ensure scheduling on application start.
     */
    public static void schedulePeriodic(Context context, int hourFreq) {
        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(BackupWorker.class, hourFreq, TimeUnit.HOURS)
                        .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORKMANAGER_ID,
                ExistingPeriodicWorkPolicy.KEEP,
                req);
    }

    @NonNull
    @Override
    public Result doWork() {
        setForegroundAsync(createForegroundInfo(App.SYNC_CHANNEL_ID, "Sync in progress"));

        Data inputData = getInputData();
        boolean force = inputData.getBoolean(KEY_FORCE, false);
        BackupHandler h = new BackupHandler(getApplicationContext());
        List<BackupItem> backupItems = new ArrayList<>();

        /*
         * XXX: If force=true is set, we manually attempt a sync. In this case, we specify
         *  a list of items to sync. Otherwise (force=false), the periodic task syncs all items.
         */
        if (force) {
            Log.d(TAG, "Forced sync - ignoring configuration restrictions");
            List<String> jsonItems = Arrays.asList(inputData.getStringArray(KEY_ITEMS));
            for (String json : jsonItems) {
                backupItems.add(BackupItem.fromJson(json));
            }
        } else {
            if (!h.canRunBackup()) {
                Log.d(TAG, "Not allowed to run backup due to configuration restriction");
                return Result.success();
            }
            BackupHandler backupHandler = new BackupHandler(this.getApplicationContext());
            backupItems.addAll(backupHandler.getBackups());
        }

        for (BackupItem i : backupItems) {
            Log.i(TAG, "Syncing item: " + i.name);
            setForegroundAsync(createForegroundInfo(App.SYNC_CHANNEL_ID, "Syncing " + i.name));
            doSync(h, i);
        }
        return Result.success();
    }

    private void doSync(BackupHandler h, BackupItem b) {
        int ret = h.runBackup(b);
        if (ret != 0 && ret != BackupHandler.ERROR_DONOTRUN) {
            Log.e(TAG, "Sync failed: " + b.name + ", ret: " + ret);

            Notification error_notif = getNotificationBuilder(App.ERROR_CHANNEL_ID)
                    .setContentTitle("Sync failed: " + b.name)
                    .setContentText(b.name)
                    .setAutoCancel(true)
                    .build();
            notificationManager.notify(b.name, App.ERROR_NOTIF_ID, error_notif);
        }
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(String id, String message) {
        Context context = getApplicationContext();
        String cancel = "Cancel";

        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());
        Notification notification = getNotificationBuilder(id)
                .setContentTitle(message)
                .setSmallIcon(R.drawable.ic_action_refresh_bitmap)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_delete, cancel, intent)
                .build();

        return new ForegroundInfo(App.SYNC_NOTIF_ID, notification);
    }

    private NotificationCompat.Builder getNotificationBuilder(String id) {
        int notifIcon = R.drawable.ic_action_refresh_bitmap;

        if (Build.VERSION.SDK_INT >= 21) {
            // >= lollipop, notification supports vector icons
            notifIcon = R.drawable.ic_action_refresh;
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return new NotificationCompat.Builder(getApplicationContext(), id)
                .setTicker("Syncopoli")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(notifIcon)
                .setContentIntent(PendingIntent.getActivity(this.getApplicationContext(),
                        0,
                        new Intent(this.getApplicationContext(), BackupActivity.class),
                        flags));

    }
}