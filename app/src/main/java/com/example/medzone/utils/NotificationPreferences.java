package com.example.medzone.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NotificationPreferences {
    private static final String PREF_NAME = "MedZoneNotificationPrefs";
    private static final String KEY_ENABLED = "notifications_enabled";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_RINGTONE = "selected_ringtone";

    private static final String KEY_REMINDER_KEYS = "reminder_keys_set";
    private static final String META_PREFIX = "reminder_meta_";
    private static final String KEY_POST_NOTIF_REQUESTED = "post_notifications_requested";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public NotificationPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Clear any previously stored custom ringtone to migrate to device default sound
        prefs.edit().remove(KEY_RINGTONE).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_VIBRATION, true);
    }

    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply();
    }

    // Ringtone selection is intentionally disabled. Always return null so callers fall back
    // to the device default notification sound. setSelectedRingtone is a no-op.
    public String getSelectedRingtone() {
        return null;
    }

    public void setSelectedRingtone(String uriString) {
        // no-op: we intentionally do not persist a custom ringtone. Device default will be used.
    }

    // --- per-recommendation reminder helpers ---
    // key should be unique per recommendation; caller is responsible to provide a stable key
    public boolean isReminderSet(String reminderKey) {
        if (reminderKey == null) return false;
        return prefs.getBoolean(reminderKey, false);
    }

    public void setReminder(String reminderKey, boolean enabled) {
        if (reminderKey == null) return;
        prefs.edit().putBoolean(reminderKey, enabled).apply();
        // maintain keys set
        Set<String> keys = getReminderKeys();
        if (enabled) {
            keys.add(reminderKey);
        } else {
            keys.remove(reminderKey);
        }
        prefs.edit().putStringSet(KEY_REMINDER_KEYS, keys).apply();
    }

    private Set<String> getReminderKeys() {
        Set<String> keys = prefs.getStringSet(KEY_REMINDER_KEYS, null);
        if (keys == null) return new HashSet<>();
        return new HashSet<>(keys);
    }

    // store metadata map (frequency, duration, times list, optional title/message)
    public void setReminderMeta(String reminderKey, Map<String, Object> meta) {
        if (reminderKey == null || meta == null) return;
        String json = gson.toJson(meta);
        prefs.edit().putString(META_PREFIX + reminderKey, json).apply();
        // ensure key is present in keys set
        Set<String> keys = getReminderKeys();
        keys.add(reminderKey);
        prefs.edit().putStringSet(KEY_REMINDER_KEYS, keys).apply();
    }

    public Map<String, Object> getReminderMeta(String reminderKey) {
        if (reminderKey == null) return null;
        String json = prefs.getString(META_PREFIX + reminderKey, null);
        if (json == null) return null;
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        try {
            Map<String, Object> map = gson.fromJson(json, type);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            return null;
        }
    }

    public Set<String> getAllReminderKeys() {
        return getReminderKeys();
    }

    public void removeReminderMeta(String reminderKey) {
        if (reminderKey == null) return;
        prefs.edit().remove(META_PREFIX + reminderKey).apply();
        Set<String> keys = getReminderKeys();
        keys.remove(reminderKey);
        prefs.edit().putStringSet(KEY_REMINDER_KEYS, keys).apply();
    }

    // Track whether we've requested POST_NOTIFICATIONS runtime permission before
    public boolean hasRequestedPostNotifications() {
        return prefs.getBoolean(KEY_POST_NOTIF_REQUESTED, false);
    }

    public void setRequestedPostNotifications(boolean requested) {
        prefs.edit().putBoolean(KEY_POST_NOTIF_REQUESTED, requested).apply();
    }
}
