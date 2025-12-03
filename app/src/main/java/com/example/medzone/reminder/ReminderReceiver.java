package com.example.medzone.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.medzone.R;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_REMINDER_KEY = "extra_reminder_key";
    public static final String EXTRA_INDEX = "extra_index";

    private static final String CHANNEL_ID = "medzone_reminders_channel";
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String reminderKey = intent.getStringExtra(EXTRA_REMINDER_KEY);
        int index = intent.getIntExtra(EXTRA_INDEX, 0);

        Log.d(TAG, "onReceive called for reminderKey=" + reminderKey + " index=" + index + " title=" + title + " message=" + message);


        // Diagnostic logging: record scheduled time vs actual delivery time
        try {
            long scheduled = intent.getLongExtra("extra_scheduled_time", -1);
            long now = System.currentTimeMillis();
            android.os.SystemClock.elapsedRealtime();
            Log.d(TAG, "Diagnostic: scheduled_time=" + (scheduled > 0 ? scheduled : "<none>") + ", now=" + now + ", delta_ms=" + (scheduled > 0 ? (now - scheduled) : 0));
        } catch (Throwable t) {
            Log.w(TAG, "Failed to log diagnostic scheduled time: " + t.getMessage());
        }

        // Get preferences first
        com.example.medzone.utils.NotificationPreferences prefs = new com.example.medzone.utils.NotificationPreferences(context);

        // CHECK if notifications are enabled in app settings
        if (!prefs.isNotificationsEnabled()) {
            Log.d(TAG, "Notifications are DISABLED in app settings - skipping notification");
            // Still schedule next reminder so when user re-enables, it continues
            scheduleNextReminder(context, reminderKey, index, title, message);
            return;
        }

        Log.d(TAG, "Notifications ENABLED - proceeding to show notification");

        createChannelIfNeeded(context);

        // Previously we used a stored custom ringtone; now we intentionally use device default by not
        // overriding sound on NotificationCompat.Builder or NotificationChannel. This lets the system
        // play the user's default notification sound.

        int notifId = (reminderKey != null ? reminderKey.hashCode() : 0) ^ index;

        // Create intent for tapping notification
        Intent notificationIntent = new Intent(context, com.example.medzone.MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
            context,
            notifId,
            notificationIntent,
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                ? android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                : android.app.PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_obat)
                .setContentTitle(title != null ? title : context.getString(R.string.set_medicine_reminder))
                .setContentText(message != null ? message : context.getString(R.string.reminder_saved))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message != null ? message : context.getString(R.string.reminder_saved)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL);  // Use all defaults (sound, lights)

        // Post notification and rely on system to handle sound and vibration based on the
        // NotificationChannel settings (no manual vibrator call here).
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted, notification will not be shown");
                    return;
                }
            }
            notificationManager.notify(notifId, builder.build());
            Log.d(TAG, "Notification shown with ID: " + notifId);

            // Note: Notification uses system defaults (sound + lights).
            // Vibration removed from settings as it only works when app is in foreground.
        } catch (Exception e) {
            Log.e(TAG, "Failed to show notification: " + e.getMessage(), e);
        }

        // Schedule next alarm for tomorrow at same time (daily recurring)
        scheduleNextReminder(context, reminderKey, index, title, message);
    }

    private void scheduleNextReminder(Context context, String reminderKey, int index, String title, String message) {
        try {
            // Get reminder metadata to check duration
            com.example.medzone.utils.NotificationPreferences prefs = new com.example.medzone.utils.NotificationPreferences(context);
            java.util.Map<String, Object> meta = prefs.getReminderMeta(reminderKey);
            if (meta == null) return;

            // Check if duration has expired
            Object durationObj = meta.get("duration");
            Object startTimeObj = meta.get("startTimestamp");
            if (durationObj instanceof Number && startTimeObj instanceof Number) {
                int durationDays = ((Number) durationObj).intValue();
                long startTime = ((Number) startTimeObj).longValue();
                long currentTime = System.currentTimeMillis();
                long daysPassed = (currentTime - startTime) / (24 * 60 * 60 * 1000);

                if (daysPassed >= durationDays) {
                    // Duration expired, stop scheduling and clean up
                    Log.d(TAG, "Reminder " + reminderKey + " duration expired (" + durationDays + " days). Stopping.");
                    prefs.setReminder(reminderKey, false);
                    prefs.removeReminderMeta(reminderKey);
                    return;
                }
            }

            Object timesObj = meta.get("times");
            if (!(timesObj instanceof java.util.List)) return;
            java.util.List<?> times = (java.util.List<?>) timesObj;
            if (index >= times.size()) return;

            String hhmm = String.valueOf(times.get(index));
            String[] parts = hhmm.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            // Schedule for next day at same time
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
            cal.set(java.util.Calendar.MINUTE, minute);
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);

            ReminderScheduler.scheduleExact(context, reminderKey, index, cal, title, message);
            Log.d(TAG, "Scheduled next reminder for " + reminderKey + " at " + cal.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule next reminder: " + e.getMessage(), e);
        }
    }

    private void createChannelIfNeeded(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            // Check if channel already exists
            NotificationChannel existingChannel = nm.getNotificationChannel(CHANNEL_ID);

            if (existingChannel != null) {
                Log.d(TAG, "Notification channel already exists");
                return;
            }

            // Create new channel
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Pengingat Penggunaan Obat",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Pengingat untuk menggunakan obat sesuai jadwal dari MedZone");
            channel.enableLights(true);
            channel.setLightColor(androidx.core.content.ContextCompat.getColor(context, R.color.primary_blue));

            // Vibration disabled - removed from settings as it only works when app is foreground
            channel.enableVibration(false);

            // Use device default notification sound
            channel.setBypassDnd(false); // Respect Do Not Disturb mode
            channel.setShowBadge(true); // Show badge on app icon

            nm.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created successfully (vibration disabled)");
        }
    }
}
