package com.example.medzone.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.medzone.utils.NotificationPreferences;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.d(TAG, "BootReceiver triggered: " + action + ", rescheduling reminders");
            rescheduleAll(context);
        }
    }

    private void rescheduleAll(Context context) {
        try {
            NotificationPreferences prefs = new NotificationPreferences(context);
            Set<String> keys = prefs.getAllReminderKeys();
            if (keys == null || keys.isEmpty()) {
                Log.d(TAG, "No reminders to reschedule");
                return;
            }

            for (String key : keys) {
                Map<String, Object> meta = prefs.getReminderMeta(key);
                if (meta == null) continue;

                // meta expected to contain "times" (List<String> HH:mm), "title", "message"
                Object timesObj = meta.get("times");
                String title = meta.get("title") == null ? null : String.valueOf(meta.get("title"));
                String message = meta.get("message") == null ? null : String.valueOf(meta.get("message"));

                if (!(timesObj instanceof List)) continue;
                List<?> times = (List<?>) timesObj;
                int idx = 0;
                for (Object t : times) {
                    try {
                        String hhmm = String.valueOf(t);
                        String[] parts = hhmm.split(":");
                        int hour = Integer.parseInt(parts[0]);
                        int minute = Integer.parseInt(parts[1]);

                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        cal.set(Calendar.HOUR_OF_DAY, hour);
                        cal.set(Calendar.MINUTE, minute);
                        // Use strict past check (<) so exact-equal times schedule for now
                        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
                            cal.add(Calendar.DAY_OF_YEAR, 1);
                        }

                        ReminderScheduler.scheduleExact(context, key, idx, cal, title, message);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to reschedule key=" + key + " item=" + t + " err=" + e.getMessage());
                    }
                    idx++;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Reschedule all failed: " + e.getMessage(), e);
        }
    }
}
