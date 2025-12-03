package com.example.medzone.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.medzone.R;
import com.example.medzone.utils.NotificationPreferences;
import androidx.appcompat.widget.SwitchCompat;

import androidx.core.content.ContextCompat;

public class NotificationSettingsActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 2001;

    private SwitchCompat switchEnableNotifications;

    private boolean suppressSwitchCallback = false;

    public static Intent createIntent(Context context) {
        return new Intent(context, NotificationSettingsActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // header icon
        switchEnableNotifications = findViewById(R.id.switch_enable_notifications);

        NotificationPreferences prefs = new NotificationPreferences(this);
        boolean enabled = prefs.isNotificationsEnabled();

        Log.d("NotificationSettings", "onCreate - Notification settings loaded");

        // Respect user's stored preference without auto-syncing to system permission state
        // This allows users to disable notifications even when system permission is granted

        switchEnableNotifications.setChecked(enabled);

        // set initial header icon state

        switchEnableNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallback) return;

            if (isChecked) {
                // If Android 13+ require runtime POST_NOTIFICATIONS permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        // If we've previously asked and user selected 'Don't ask again', shouldShowRequestPermissionRationale returns false.
                        boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS);
                        if (!shouldShowRationale) {
                            // Show dialog explaining that user must enable in Settings
                            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
                            b.setTitle(R.string.allow_notifications_title)
                             .setMessage(R.string.notification_permission_required)
                             .setPositiveButton(R.string.btn_masuk, (d, which) -> {
                                 Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                         .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                                 startActivity(intent);
                             })
                             .setNegativeButton(android.R.string.cancel, null)
                             .show();
                            // revert switch visually
                            suppressSwitchCallback = true;
                            switchEnableNotifications.setChecked(false);
                            suppressSwitchCallback = false;
                            return;
                        } else {
                            // request permission and handle result; don't change prefs until granted
                            pendingRequestForToggle();
                            NotificationPreferences npref = new NotificationPreferences(this);
                            npref.setRequestedPostNotifications(true);
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
                            return;
                        }
                    }
                }
                // Permission already granted or not required: auto-save
                autoSaveNotificationSettings();
                Toast.makeText(this, getString(R.string.notification_enabled_prompt), Toast.LENGTH_SHORT).show();
            } else {
                // turned off: auto-save
                autoSaveNotificationSettings();
                Toast.makeText(this, getString(R.string.notification_disabled), Toast.LENGTH_SHORT).show();
            }
        });

        // Ringtone preview button
        findViewById(R.id.btnPreviewRingtone).setOnClickListener(v -> {
            playNotificationSound();
        });
    }

    /**
     * Auto-save notification settings whenever any toggle changes
     */
    private void autoSaveNotificationSettings() {
        NotificationPreferences prefs = new NotificationPreferences(this);
        prefs.setNotificationsEnabled(switchEnableNotifications.isChecked());
        // Vibration always disabled (removed from UI)
        prefs.setVibrationEnabled(false);
    }

    private void pendingRequestForToggle() {
        // show brief message explaining why we're asking
        Toast.makeText(this, getString(R.string.notification_enabled_prompt), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            boolean granted = grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            NotificationPreferences prefs = new NotificationPreferences(this);
            if (granted) {
                // user granted notifications permission -> persist
                prefs.setNotificationsEnabled(true);
                suppressSwitchCallback = true;
                switchEnableNotifications.setChecked(true);
                suppressSwitchCallback = false;
                Toast.makeText(this, getString(R.string.notification_settings_saved), Toast.LENGTH_SHORT).show();
            } else {
                // denied -> revert switch and open app notification settings so user can manually enable
                suppressSwitchCallback = true;
                switchEnableNotifications.setChecked(false);
                suppressSwitchCallback = false;
                prefs.setNotificationsEnabled(false);
                Toast.makeText(this, getString(R.string.notification_permission_required), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Reload preferences when returning to this activity to reflect any changes
        NotificationPreferences prefs = new NotificationPreferences(this);
        boolean enabled = prefs.isNotificationsEnabled();

        Log.d("NotificationSettings", "onResume - Notification settings loaded");

        // Update UI to reflect current stored preferences
        suppressSwitchCallback = true;
        switchEnableNotifications.setChecked(enabled);
        suppressSwitchCallback = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // nothing to cleanup
    }

    /**
     * Play default notification sound as preview
     */
    private void playNotificationSound() {
        try {
            android.net.Uri notificationUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
            android.media.Ringtone ringtone = android.media.RingtoneManager.getRingtone(this, notificationUri);
            if (ringtone != null) {
                ringtone.play();
                Toast.makeText(this, "Memutar nada dering default", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("NotificationSettings", "Failed to play ringtone: " + e.getMessage());
            Toast.makeText(this, "Gagal memutar nada dering", Toast.LENGTH_SHORT).show();
        }
    }
}
