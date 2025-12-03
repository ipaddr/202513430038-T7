package com.example.medzone.ui.dialog;

import android.Manifest;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.medzone.R;
import com.example.medzone.activities.NotificationSettingsActivity;
import com.example.medzone.model.Recommendation;
import com.example.medzone.reminder.ReminderScheduler;
import com.example.medzone.reminder.ReminderReceiver;
import com.example.medzone.utils.NotificationPreferences;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class MedicineReminderDialog extends DialogFragment {

    private static final String ARG_MEDICINE_NAME = "medicine_name";
    private static final String ARG_MEDICINE_DOSE = "medicine_dose";
    private static final String ARG_REMINDER_KEY = "reminder_key";
    private static final String ARG_REMINDER_EXISTING = "reminder_existing";
    private static final String ARG_REMINDER_META = "reminder_meta";
    private static final String ARG_DISEASE_NAME = "disease_name";

    public static final String RESULT_KEY = "med_reminder_result";
    public static final String RESULT_ACTION = "action"; // "saved" or "deleted"
    public static final String RESULT_REMINDER_KEY = "reminder_key";
    public static final String RESULT_REMINDER_META = "reminder_meta"; // map with times/frequency

    private String medicineName;
    private String medicineDose;
    private String reminderKey;
    private boolean isExisting;
    private String diseaseName;
    private int selectedFrequency = 3;
    private int selectedDuration = 7;
    private List<String> timeSlots = new ArrayList<>();
    private LinearLayout timeSlotsContainer;
    private TextView tvScheduleLabel;

    // temporary flag to indicate user attempted save while permission missing
    private boolean pendingSaveRequested = false;
    private boolean pendingEnablePrefs = false;
    private static final int REQ_POST_NOTIFICATIONS = 1001;

    public static MedicineReminderDialog newInstance(Recommendation recommendation, String reminderKey, boolean isExisting, java.util.Map<String, Object> meta, String diseaseName) {
        MedicineReminderDialog dialog = new MedicineReminderDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MEDICINE_NAME, recommendation.name);
        args.putString(ARG_MEDICINE_DOSE, recommendation.dosis);
        args.putString(ARG_REMINDER_KEY, reminderKey);
        args.putBoolean(ARG_REMINDER_EXISTING, isExisting);
        args.putString(ARG_DISEASE_NAME, diseaseName);
        if (meta != null) {
            args.putSerializable(ARG_REMINDER_META, new java.util.HashMap<>(meta));
        }
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);

        if (getArguments() != null) {
            medicineName = getArguments().getString(ARG_MEDICINE_NAME);
            medicineDose = getArguments().getString(ARG_MEDICINE_DOSE);
            reminderKey = getArguments().getString(ARG_REMINDER_KEY);
            isExisting = getArguments().getBoolean(ARG_REMINDER_EXISTING, false);
            diseaseName = getArguments().getString(ARG_DISEASE_NAME);
            if (getArguments().containsKey(ARG_REMINDER_META)) {
                //noinspection unchecked
                java.util.Map<String, Object> meta = (java.util.Map<String, Object>) getArguments().getSerializable(ARG_REMINDER_META);
                if (meta != null) {
                    Object freq = meta.get("frequency");
                    Object dur = meta.get("duration");
                    Object times = meta.get("times");
                    if (freq instanceof Number) selectedFrequency = ((Number) freq).intValue();
                    if (dur instanceof Number) selectedDuration = ((Number) dur).intValue();
                    if (times instanceof java.util.List) {
                        try {
                            timeSlots.clear();
                            for (Object o : (java.util.List) times) {
                                timeSlots.add(String.valueOf(o));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        // Initialize default time slots only if nothing provided via meta
        if (timeSlots.isEmpty()) {
            initializeDefaultTimeSlots();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_medicine_reminder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        TextView tvMedicineName = view.findViewById(R.id.tvMedicineName);
        TextView tvMedicineDose = view.findViewById(R.id.tvMedicineDose);
        tvScheduleLabel = view.findViewById(R.id.tvScheduleLabel);
        timeSlotsContainer = view.findViewById(R.id.timeSlotsContainer);
        ChipGroup chipGroupFrequency = view.findViewById(R.id.chipGroupFrequency);
        ChipGroup chipGroupDuration = view.findViewById(R.id.chipGroupDuration);
        AppCompatButton btnCancel = view.findViewById(R.id.btnCancel);
        AppCompatButton btnSave = view.findViewById(R.id.btnSave);
        View btnClose = view.findViewById(R.id.btnClose);

        // Set medicine info
        tvMedicineName.setText(medicineName);
        tvMedicineDose.setText(medicineDose);

        // Setup frequency chips
        // We'll set the checked chip to reflect selectedFrequency (possibly coming from meta)
        switch (selectedFrequency) {
            case 1: chipGroupFrequency.check(R.id.chip1x); break;
            case 2: chipGroupFrequency.check(R.id.chip2x); break;
            case 3: chipGroupFrequency.check(R.id.chip3x); break;
            case 4: chipGroupFrequency.check(R.id.chip4x); break;
            default: chipGroupFrequency.check(R.id.chip3x); break;
        }
        chipGroupFrequency.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip1x) selectedFrequency = 1;
                else if (checkedId == R.id.chip2x) selectedFrequency = 2;
                else if (checkedId == R.id.chip3x) selectedFrequency = 3;
                else if (checkedId == R.id.chip4x) selectedFrequency = 4;

                updateTimeSlots();
                updateScheduleLabel();
            }
        });

        // Setup duration chips
        switch (selectedDuration) {
            case 1: chipGroupDuration.check(R.id.chip1day); break;
            case 3: chipGroupDuration.check(R.id.chip3days); break;
            case 7: chipGroupDuration.check(R.id.chip7days); break;
            case 14: chipGroupDuration.check(R.id.chip14days); break;
            default: chipGroupDuration.check(R.id.chip7days); break;
        }
        chipGroupDuration.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip1day) selectedDuration = 1;
                else if (checkedId == R.id.chip3days) selectedDuration = 3;
                else if (checkedId == R.id.chip7days) selectedDuration = 7;
                else if (checkedId == R.id.chip14days) selectedDuration = 14;
            }
        });

        // Initial setup
        updateTimeSlots();
        updateScheduleLabel();

        // Close button
        btnClose.setOnClickListener(v -> dismiss());

        // Cancel/Delete button
        if (isExisting) {
            btnCancel.setText(R.string.delete);
            btnCancel.setOnClickListener(v -> {
                // delete reminder
                if (reminderKey != null) {
                    NotificationPreferences prefs = new NotificationPreferences(requireContext());
                    prefs.setReminder(reminderKey, false);

                    // cancel any scheduled alarms for current times
                    for (int i = 0; i < timeSlots.size(); i++) {
                        ReminderScheduler.cancel(requireContext(), reminderKey, i);
                    }

                    // remove stored meta
                    prefs.removeReminderMeta(reminderKey);

                    Bundle result = new Bundle();
                    result.putString(RESULT_ACTION, "deleted");
                    result.putString(RESULT_REMINDER_KEY, reminderKey);
                    getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
                }
                dismiss();
            });
        } else {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        // Save button
        btnSave.setOnClickListener(v -> {
            // Check if notifications are enabled (app-level setting)
            NotificationPreferences prefs = new NotificationPreferences(requireContext());
            if (!prefs.isNotificationsEnabled()) {
                // Redirect to notification settings and show toast
                Toast.makeText(requireContext(),
                    getString(R.string.notification_must_be_enabled),
                    Toast.LENGTH_LONG).show();

                Intent intent = new Intent(requireContext(), NotificationSettingsActivity.class);
                startActivity(intent);
                dismiss();
                return;
            }

            // For Android 13+, if permission not granted yet, request it
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    pendingSaveRequested = true;
                    // Do not set pendingEnablePrefs here because internal toggle may already be true
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
                    return;
                }
            }

            // All checks passed -> perform save actions (scheduling + persist meta + emit result)
            performSaveActions();
        });
    }

    private void initializeDefaultTimeSlots() {
        timeSlots.clear();
        timeSlots.add("08:00");
        timeSlots.add("14:00");
        timeSlots.add("20:00");
    }

    private void updateTimeSlots() {
        timeSlotsContainer.removeAllViews();

        // Adjust time slots based on frequency
        while (timeSlots.size() < selectedFrequency) {
            timeSlots.add("12:00");
        }
        while (timeSlots.size() > selectedFrequency) {
            timeSlots.remove(timeSlots.size() - 1);
        }

        // Create time slot views
        for (int i = 0; i < selectedFrequency; i++) {
            View timeSlotView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_time_slot, timeSlotsContainer, false);

            AppCompatButton btnDoseLabel = timeSlotView.findViewById(R.id.btnDoseLabel);
            EditText etTime = timeSlotView.findViewById(R.id.etTime);

            btnDoseLabel.setText(getString(R.string.dose_label, i + 1));
            etTime.setText(timeSlots.get(i));

            final int index = i;
            etTime.setOnClickListener(v -> showTimePicker(index));

            timeSlotsContainer.addView(timeSlotView);
        }
    }

    private void updateScheduleLabel() {
        tvScheduleLabel.setText(getString(R.string.schedule_label)
            .replace("3×", selectedFrequency + "×"));
    }

    private void showTimePicker(int index) {
        Calendar calendar = Calendar.getInstance();
        String currentTime = timeSlots.get(index);
        String[] parts = currentTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
            requireContext(),
            (view, selectedHour, selectedMinute) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                timeSlots.set(index, time);
                updateTimeSlotDisplay(index, time);
            },
            hour,
            minute,
            true
        );
        timePickerDialog.show();
    }

    private void updateTimeSlotDisplay(int index, String time) {
        View timeSlotView = timeSlotsContainer.getChildAt(index);
        if (timeSlotView != null) {
            EditText etTime = timeSlotView.findViewById(R.id.etTime);
            etTime.setText(time);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    // Extracted save actions so we can call them after permission is granted
    private void performSaveActions() {
        // Save reminder (persist state locally)
        if (reminderKey != null) {
            NotificationPreferences prefs = new NotificationPreferences(requireContext());
            prefs.setReminder(reminderKey, true);
        }

        // schedule alarms for each time slot
        // title/message for notification (declare once)
        String title = getString(R.string.reminder_notification_title);
        String disease = (diseaseName != null && !diseaseName.isEmpty()) ? diseaseName : getString(R.string.reminder_health_default);
        String message = getString(R.string.reminder_notification_message, medicineName, medicineDose, disease);
        for (int i = 0; i < timeSlots.size(); i++) {
            String hhmm = timeSlots.get(i);
            String[] parts = hhmm.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            // If time already passed today, schedule for next day
            // Use strict past check (<) to avoid skipping scheduling when time equals current instant
            if (cal.getTimeInMillis() < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            ReminderScheduler.scheduleExact(requireContext(), reminderKey, i, cal, title, message);
        }

        // Build metadata to persist to cloud via fragment result
        Map<String, Object> meta = new HashMap<>();
        meta.put("frequency", selectedFrequency);
        meta.put("duration", selectedDuration);
        meta.put("times", timeSlots);
        meta.put("title", title);
        meta.put("message", message);
        meta.put("startTimestamp", System.currentTimeMillis()); // Track when reminder started

        // persist meta locally so BootReceiver can reschedule
        if (reminderKey != null) {
            NotificationPreferences prefs = new NotificationPreferences(requireContext());
            prefs.setReminderMeta(reminderKey, meta);
        }

        Bundle result = new Bundle();
        result.putString(RESULT_ACTION, "saved");
        result.putString(RESULT_REMINDER_KEY, reminderKey);
        result.putSerializable(RESULT_REMINDER_META, (HashMap)meta);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);

        Toast.makeText(requireContext(),
            getString(R.string.reminder_saved),
            Toast.LENGTH_SHORT).show();
        dismiss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            NotificationPreferences prefs = new NotificationPreferences(requireContext());
            if (granted) {
                if (pendingEnablePrefs) {
                    prefs.setNotificationsEnabled(true);
                    pendingEnablePrefs = false;
                }
                if (pendingSaveRequested) {
                    pendingSaveRequested = false;
                    performSaveActions();
                    return;
                }
            }
            // permission denied or no pending save
            pendingSaveRequested = false;
            pendingEnablePrefs = false;
            Toast.makeText(requireContext(), getString(R.string.notification_permission_required), Toast.LENGTH_LONG).show();
            // optionally open app notification settings so user can enable
            Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
            startActivity(intent);
        }
    }
}
