package com.example.medzone.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class ReminderScheduler {
    private static final String TAG = "ReminderScheduler";

    public static void scheduleExact(Context context, String reminderKey, int index, Calendar when, String title, String message) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra(ReminderReceiver.EXTRA_REMINDER_KEY, reminderKey);
            intent.putExtra(ReminderReceiver.EXTRA_INDEX, index);
            intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
            intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, message);
            // Attach the scheduled trigger time (ms since epoch) so receiver can log expected vs actual
            intent.putExtra("extra_scheduled_time", when.getTimeInMillis());

            int reqCode = (reminderKey != null ? reminderKey.hashCode() : 0) ^ index;

            PendingIntent pi = PendingIntent.getBroadcast(context, reqCode, intent, pendingFlags());

            try {
                // Use setAlarmClock for medication reminders - guarantees exact timing even in Doze mode
                // This bypasses all battery optimization delays and ensures vibration works
                // Available since API 21 (Lollipop), which is the minimum SDK for this app
                AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
                    when.getTimeInMillis(),
                    pi // Show intent when user taps the alarm icon
                );
                am.setAlarmClock(alarmClockInfo, pi);
                Log.d(TAG, "Scheduled ALARM_CLOCK (guaranteed exact) " + reminderKey + " at " + when.getTime());
            } catch (SecurityException se) {
                // Exact alarm permission missing on newer Android versions (S+). Fall back to inexact alarm.
                Log.w(TAG, "Exact alarm security exception, falling back to inexact set(): " + se.getMessage());
                am.set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pi);
                Log.d(TAG, "Scheduled inexact alarm (fallback) " + reminderKey + " at " + when.getTime());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed scheduleExact: " + e.getMessage(), e);
        }
    }

    public static void cancel(Context context, String reminderKey, int index) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent intent = new Intent(context, ReminderReceiver.class);
            int reqCode = (reminderKey != null ? reminderKey.hashCode() : 0) ^ index;
            PendingIntent pi = PendingIntent.getBroadcast(context, reqCode, intent, pendingFlags());
            am.cancel(pi);
        } catch (Exception e) {
            Log.e(TAG, "Failed cancel: " + e.getMessage(), e);
        }
    }

    private static int pendingFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.FLAG_UPDATE_CURRENT;
    }
}
