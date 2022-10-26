package org.amoradi.syncopoli;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

/**
 * Manager to schedule recurring jobs
 */
public class ScheduleManager {
    private Context context;

    public static String ALARM_ACTION = "org.amoradi.syncopoli.ALARM_INTERVAL";

    public ScheduleManager(Context context) {
        this.context = context;
    }

    public int getScheduleInterval() { /* in hrs */
        return SettingsFragment.getFrequency(context);
    }

    public void enableAlarmScheduler(boolean enable) {
        SettingsFragment.setAlarmSchedulerEnabled(context, enable);
    }

    public boolean isAlarmSchedulerEnabled() {
        return SettingsFragment.isAlarmSchedulerEnabled(context);
    }

    public void scheduleWithJob() {
        BackupWorker.schedulePeriodic(context, getScheduleInterval());
    }

    public void unscheduleJob() {
        BackupWorker.unschedulePeriodic(context);
    }

    public void unscheduleAlarm() {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(buildIntent());
    }

    private PendingIntent buildIntent() {
        Intent intent = new Intent(context, BackupReceiver.class);
        intent.setAction(ALARM_ACTION);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pIntent;
    }

    public void scheduleWithAlarm() {
        unscheduleAlarm();
        int freq = getScheduleInterval();
        long startMs = SystemClock.elapsedRealtime() + freq * AlarmManager.INTERVAL_HOUR;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        /*
         * XXX: We raise an Intent which our intent receiver will receive.
         * This ensures that our app is woken up from sleep and the workmanager
         * can schedule a pending task. If this approach is not aggressive enough,
         * we can explicitly launch a BackupWorker sync with this intent, but so far
         * we leave it as is.
         */
        am.setRepeating(AlarmManager.ELAPSED_REALTIME, startMs, freq * AlarmManager.INTERVAL_HOUR, buildIntent());
    }

}
