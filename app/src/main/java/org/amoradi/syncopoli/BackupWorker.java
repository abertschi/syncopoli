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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Work manager to handle sync work.
 * Invoke with syncNow() to sync directly or schedule a frequency with schedulePeriodic().
 */
public class BackupWorker extends Worker {
    private static final String TAG = "Syncopoli";
    private static final String KEY_ITEMS = "KEY_ITEMS";
    private static final String KEY_FORCE = "KEY_FORCE";
    private static final String WORKMANAGER_ID = "SYNC_ID";

    private final NotificationManager notificationManager;

    public static void syncNow(Context context, BackupItem item) {
        List<BackupItem> items = new ArrayList<>();
        items.add(item);
        syncNow(context, items);
    }

    public static void syncNow(Context context, List<BackupItem> items) {
        List<String> strItems = new ArrayList<String>();
        for (BackupItem i : items) {
            strItems.add(i.toJson());
        }
        scheduleOneShot(context, strItems);
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

    public static void unschedulePeriodic(Context context) {
        WorkManager.getInstance(context)
                .cancelUniqueWork(WORKMANAGER_ID);
    }

    public static void schedulePeriodic(Context context, int hourFreq) {
        /*
         * XXX: ExistingPeriodicWorkPolicy.KEEP ensures that we dont schedule the request twice.
         * This allows us to ensure scheduling on application start.
         */
        PeriodicWorkRequest req = new PeriodicWorkRequest
                .Builder(BackupWorker.class, hourFreq, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        WORKMANAGER_ID,
                        ExistingPeriodicWorkPolicy.KEEP,
                        req);
        Log.i(TAG, "Backup Worker " + WORKMANAGER_ID +
                " scheduled with frequency (hrs): " + hourFreq);
    }

    public BackupWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters) {
        super(context, parameters);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);

        {
            Notification error_notif = getNotificationBuilder(App.SUCCESS_CHANNEL_ID)
                    .setContentTitle("Backup Worker: Instantiation")
                    .setAutoCancel(true)
                    .build();
            notificationManager.notify(Long.toString(System.currentTimeMillis()), App.SUCCESS_NOTIF_ID, error_notif);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Backup Worker started");
        {
            Notification error_notif = getNotificationBuilder(App.SUCCESS_CHANNEL_ID)
                    .setContentTitle("Backup Worker: doWork")
                    .setAutoCancel(true)
                    .build();
            notificationManager.notify(Long.toString(System.currentTimeMillis()), App.SUCCESS_NOTIF_ID, error_notif);
        }

        Data inputData = getInputData();
        BackupHandler handler = new BackupHandler(getApplicationContext());
        setForegroundAsync(createForegroundInfo(App.SYNC_CHANNEL_ID, "Sync in progress"));

        if (!canRunBackup(inputData, handler)) {
            Log.d(TAG, "Not allowed to run backup due to configuration restriction");
            return Result.success();
        }

        boolean success = true;
        List<BackupItem> backupItems = getBackupItems(inputData, handler);
        if (backupItems.isEmpty()) {
            return Result.success();
        }

        for (BackupItem i : backupItems) {
            Log.i(TAG, "Syncing item: " + i.name);
            setForegroundAsync(createForegroundInfo(App.SYNC_CHANNEL_ID, "Syncing " + i.name));
            success = success && doSync(handler, i);
        }

        return notifyUser(inputData, success);
    }

    private boolean doSync(BackupHandler h, BackupItem b) {
        boolean success = true;
        int ret = 0;
        try {
            ret = h.runBackup(b);
        } catch (Exception e) {
            // XXX: Catch all exceptions wo the scheduled worker does not
            // crash and has issues to recover on new periodic sync
            ret = BackupHandler.ERROR_GENERIC;
            e.printStackTrace();
        }
        if (ret != 0 && ret != BackupHandler.ERROR_DONOTRUN) {
            success = false;
            Log.e(TAG, "Sync failed: " + b.name + ", ret: " + ret);

            Notification error_notif = getNotificationBuilder(App.ERROR_CHANNEL_ID)
                    .setContentTitle("Sync failed")
                    .setContentText(b.name)
                    .setAutoCancel(true)
                    .build();
            notificationManager.notify(b.name, App.ERROR_NOTIF_ID, error_notif);
        }
        return success;
    }

    private List<BackupItem> getBackupItems(Data inputData, BackupHandler handler) {
        /*
         * XXX: If force=true is set, we manually attempt a sync. In this case, we specify
         *  a list of items to sync. Otherwise (force=false), the periodic task syncs all items,
         * which we load from BackupHandler.
         */
        List<BackupItem> backupItems = new ArrayList<>();
        if (isForced(inputData)) {
            Log.d(TAG, "Forced sync - ignoring configuration restrictions");
            String[] jsonItems = inputData.getStringArray(KEY_ITEMS);
            for (String json : Objects.requireNonNull(jsonItems)) {
                backupItems.add(BackupItem.fromJson(json));
            }
        } else {
            backupItems.addAll(handler.getBackups());
        }
        return backupItems;
    }

    private boolean canRunBackup(Data inputData, BackupHandler handler) {
        return isForced(inputData) || handler.canRunBackup();
    }

    private Result notifyUser(Data inputData, boolean success) {
        if (success) {
            notificationManager.notify(App.SUCCESS_NOTIF_ID, getNotificationBuilder(App.SUCCESS_CHANNEL_ID)
                    .setContentTitle("Sync completed")
                    .setAutoCancel(true)
                    .build());
            return Result.success();
        } else if (isForced(inputData)) {
            // XXX: Forced sync failed (we already show dedicated notification),
            // We do not retry
            return Result.success();
        } else {
            // Scheduled sync failed, retry later again
            return Result.retry();
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

    private NotificationCompat.Builder getNotificationBuilder(String channel) {
        int notifIcon = R.drawable.ic_action_refresh_bitmap;

        if (Build.VERSION.SDK_INT >= 21) {
            // >= lollipop, notification supports vector icons
            notifIcon = R.drawable.ic_action_refresh;
        }
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return new NotificationCompat.Builder(getApplicationContext(), channel)
                .setTicker("Syncopoli")
                .setSmallIcon(notifIcon)
                .setContentIntent(PendingIntent.getActivity(this.getApplicationContext(),
                        0,
                        new Intent(this.getApplicationContext(), BackupActivity.class),
                        flags));

    }

    private boolean isForced(Data data) {
        return data.getBoolean(KEY_FORCE, false);
    }
}