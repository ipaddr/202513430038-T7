package com.example.medzone.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medzone.R;
import com.example.medzone.adapters.HistoryAdapter;
import com.example.medzone.model.HistoryItem;
import com.example.medzone.model.Recommendation;
import com.example.medzone.ui.dialog.MedicineReminderDialog;
import com.example.medzone.utils.NotificationPreferences;
import com.example.medzone.viewmodel.HistoryViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;
    private View emptyStateView;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvHistory);
        emptyStateView = view.findViewById(R.id.emptyStateView);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(requireContext());
        recyclerView.setAdapter(adapter);

        // Register fragment result listener to update adapter when dialog saves/deletes reminder
        getParentFragmentManager().setFragmentResultListener(MedicineReminderDialog.RESULT_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            String action = bundle.getString(MedicineReminderDialog.RESULT_ACTION);
            String reminderKey = bundle.getString(MedicineReminderDialog.RESULT_REMINDER_KEY);
            Map<String, Object> meta = null;
            if (bundle.containsKey(MedicineReminderDialog.RESULT_REMINDER_META)) {
                //noinspection unchecked
                meta = (Map<String, Object>) bundle.getSerializable(MedicineReminderDialog.RESULT_REMINDER_META);
            }
            if (reminderKey != null) {
                // Find matching history item and recommendation index
                List<HistoryItem> current = new ArrayList<>(adapter.getCurrentList());
                for (int hi = 0; hi < current.size(); hi++) {
                    HistoryItem h = current.get(hi);
                    if (h.rekomendasi == null) continue;
                    for (int ri = 0; ri < h.rekomendasi.size(); ri++) {
                        Recommendation r = h.rekomendasi.get(ri);
                        String key = HistoryAdapter.computeReminderKey(r, ri);
                        if (key.equals(reminderKey)) {
                            boolean isSet = "saved".equals(action);
                            // Update model and notify adapter for that item
                            r.reminderSet = isSet;
                            adapter.notifyItemChanged(hi);

                            // Persist to cloud via ViewModel — if meta present, save full meta; otherwise save timestamp only
                            if (isSet) {
                                java.util.Map<String, Object> data = new java.util.HashMap<>();
                                data.put("timestamp", System.currentTimeMillis());
                                if (meta != null) {
                                    data.putAll(meta);
                                }
                                viewModel.saveReminderToCloud(h.id, reminderKey, data, null);
                            } else {
                                viewModel.deleteReminderFromCloud(h.id, reminderKey, null);
                            }

                            return; // done
                        }
                    }
                }
            }
        });

        // Set notification click listener to show reminder dialog
        adapter.setOnNotificationClickListener((recommendation, position, reminderKey, isExisting) -> {
            // Get disease name from the history item
            HistoryItem item = adapter.getCurrentList().get(position);
            String diseaseName = (item != null && item.diagnosis != null) ? item.diagnosis : "penyakit Anda";

            // Prefer local meta from NotificationPreferences first, then fall back to cloud
            NotificationPreferences prefs = new NotificationPreferences(requireContext());
            Map<String, Object> localMeta = prefs.getReminderMeta(reminderKey);
            if (localMeta != null) {
                MedicineReminderDialog dialog = MedicineReminderDialog.newInstance(recommendation, reminderKey, isExisting, localMeta, diseaseName);
                dialog.show(getParentFragmentManager(), "MedicineReminderDialog");
                return;
            }

            // Fetch from cloud if no local meta
            viewModel.fetchReminderMeta(item.id, reminderKey, meta -> {
                requireActivity().runOnUiThread(() -> {
                    MedicineReminderDialog dialog = MedicineReminderDialog.newInstance(recommendation, reminderKey, isExisting, meta, diseaseName);
                    dialog.show(getParentFragmentManager(), "MedicineReminderDialog");
                });
            });
        });

        // Use activity-scoped ViewModel so HomeFragment and HistoryFragment share the same instance
        viewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);

        // ✅ Load histories terlebih dahulu untuk inisialisasi LiveData
        viewModel.loadHistoriesForCurrentUser();

        // ✅ Observe LiveData dari Room
        viewModel.getHistories().observe(getViewLifecycleOwner(), list -> {
            if (list == null) list = new ArrayList<>();
            adapter.submitList(list);

            // Show empty state if list is empty, otherwise show recycler view
            if (list.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyStateView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        // ✅ Auto-fetch dari Firebase dan merge ke lokal apabila berbeda
        viewModel.refreshFromCloudIfChanged();
    }
}
